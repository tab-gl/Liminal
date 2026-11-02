#!/usr/bin/env python3
"""
Repository checks that run in CI before anything is built.

These cover the failure modes this repo has actually hit, so they are guards
rather than generic linting:

  secrets      a live credential committed by accident
  rules        Firestore rules that are malformed or over-permissive, and
               collections the app uses but the rules do not define
  icons        HugeIcons constants and ic_hg_*.xml drifting apart
  kotlin       an import that no longer resolves, or unbalanced braces
  build        applicationId vs google-services.json, ABI filters leaking into
               defaultConfig, missing version fields, release not minified

Usage:
    python3 tools/checks/check_repo.py            # all checks
    python3 tools/checks/check_repo.py rules      # one check
Exit code 1 if anything fails.
"""
from __future__ import annotations

import glob
import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
APP = os.path.join(ROOT, "app", "src", "main")
JAVA = os.path.join(APP, "java")

SKIP_DIRS = {".git", "build", "node_modules", "lib", ".gradle", "__pycache__", ".kotlin"}
TEXT_EXT = {".kt", ".kts", ".xml", ".json", ".rules", ".md", ".ts", ".js", ".yml", ".yaml",
            ".pro", ".properties", ".toml", ".py", ".sh", ".mjs", ".example"}


def walk_files(root: str, exts: set[str] | None = None):
    for dirpath, dirnames, filenames in os.walk(root):
        dirnames[:] = [d for d in dirnames if d not in SKIP_DIRS]
        for name in filenames:
            path = os.path.join(dirpath, name)
            if exts is None or os.path.splitext(name)[1] in exts:
                yield path


def read(path: str) -> str:
    with open(path, encoding="utf-8", errors="ignore") as handle:
        return handle.read()


def rel(path: str) -> str:
    return os.path.relpath(path, ROOT)


def strip_code(src: str) -> str:
    """Remove strings and comments, in that order (comment-first breaks URLs)."""
    src = re.sub(r'"""(.*?)"""', '""', src, flags=re.S)
    src = re.sub(r'"(\\.|[^"\\])*"', '""', src)
    src = re.sub(r'/\*.*?\*/', '', src, flags=re.S)
    src = re.sub(r'//[^\n]*', '', src)
    return src


# --------------------------------------------------------------------------- secrets
SECRET_PATTERNS = [
    (re.compile(r"cloudinary://\d{10,}:[A-Za-z0-9_\-]{12,}@"), "live Cloudinary URL"),
    (re.compile(r"-----BEGIN (RSA |EC |)PRIVATE KEY-----"), "private key"),
    (re.compile(r"(?i)(api[_-]?secret|client[_-]?secret)\s*[:=]\s*['\"]?[A-Za-z0-9_\-]{16,}"),
     "hardcoded api secret"),
    (re.compile(r"(?i)aws_secret_access_key\s*[:=]\s*['\"]?[A-Za-z0-9/+=]{20,}"),
     "aws secret"),
]
# The Firebase Android client config is public by design and is meant to be shipped.
SECRET_ALLOWLIST = {
    os.path.join("app", "google-services.json"),
    os.path.join("functions", ".env.example"),
}
BINARY_SECRETS = {".jks": "keystore", ".keystore": "keystore", ".p12": "keystore"}
# Local config files that must never be committed.
FORBIDDEN_FILES = {".env", "keystore.properties", "service-account.json"}


def check_secrets() -> list[str]:
    problems: list[str] = []
    for path in walk_files(ROOT):
        relpath = rel(path)
        ext = os.path.splitext(path)[1].lower()
        if os.path.basename(path) in FORBIDDEN_FILES:
            problems.append(f"{relpath}: local config committed — it is git-ignored for a reason")
            continue
        if ext in BINARY_SECRETS:
            problems.append(f"{relpath}: {BINARY_SECRETS[ext]} committed")
            continue
        if ext not in TEXT_EXT and not path.endswith(".example"):
            continue
        if any(relpath.endswith(a) for a in SECRET_ALLOWLIST):
            continue
        body = read(path)
        for pattern, label in SECRET_PATTERNS:
            if pattern.search(body):
                problems.append(f"{relpath}: {label}")
    return problems


# ----------------------------------------------------------------------------- rules
APP_COLLECTIONS = re.compile(r'const val (\w+) = "(\w+)"')


def check_rules() -> list[str]:
    problems: list[str] = []
    rules_path = os.path.join(ROOT, "firestore.rules")
    if not os.path.exists(rules_path):
        return ["firestore.rules missing"]
    src = read(rules_path)
    code = strip_code(src)

    if code.count("{") != code.count("}"):
        problems.append(f"firestore.rules: unbalanced braces "
                        f"({code.count('{')} vs {code.count('}')})")

    depth = 0
    for number, line in enumerate(code.splitlines(), start=1):
        stripped = line.strip()
        if stripped.startswith("allow") and depth == 0:
            problems.append(f"firestore.rules:{number}: allow outside a match block")
        # Public reads of the catalogue are intentional; an unconditional
        # write never is.
        write_statement = re.match(r"allow\s+([a-z, ]+):", stripped)
        if write_statement and re.search(r"if\s+true\s*;", stripped):
            ops = {op.strip() for op in write_statement.group(1).split(",")}
            if ops & {"write", "create", "update", "delete"}:
                problems.append(
                    f"firestore.rules:{number}: unconditional write "
                    f"({', '.join(sorted(ops))})"
                )
        depth += line.count("{") - line.count("}")

    # The catch-all must deny, or everything above it is pointless.
    catch_all = re.search(r"match\s+/\{document=\*\*\}\s*\{(.*?)\n\s*\}", code, re.S)
    if catch_all and "allow read, write: if false" not in catch_all.group(1):
        problems.append("firestore.rules: catch-all block does not deny everything")

    # Every collection the app addresses must have a match block.
    paths_file = os.path.join(JAVA, "app", "tab", "store", "data", "remote", "FirestorePaths.kt")
    if os.path.exists(paths_file):
        declared = {m.group(2) for m in APP_COLLECTIONS.finditer(read(paths_file))}
        # Sub-collection names are not top-level collections.
        sub = {"lines", "items", "tokens", "config", "stats"}
        for collection in sorted(declared - sub):
            if not re.search(rf"match\s+/{collection}\b", code):
                problems.append(f"firestore.rules: no match block for '{collection}'")
    return problems


# ----------------------------------------------------------------------------- icons
def check_icons() -> list[str]:
    problems: list[str] = []
    icons_kt = os.path.join(JAVA, "app", "tab", "store", "core", "designsystem", "HugeIcons.kt")
    if not os.path.exists(icons_kt):
        return ["HugeIcons.kt missing"]

    body = "\n".join(
        line for line in read(icons_kt).splitlines() if not line.strip().startswith("//")
    )
    referenced = set(re.findall(r"R\.drawable\.(ic_hg_\w+)", body))
    on_disk = {
        os.path.splitext(os.path.basename(p))[0]
        for p in glob.glob(os.path.join(APP, "res", "drawable", "ic_hg_*.xml"))
    }

    for name in sorted(referenced - on_disk):
        problems.append(f"HugeIcons: {name} referenced but no drawable "
                        f"(run tools/hugeicons.py)")
    for name in sorted(on_disk - referenced):
        problems.append(f"drawable {name}.xml exists but HugeIcons never exposes it")
    return problems


# ---------------------------------------------------------------------------- kotlin
DECL = re.compile(
    r"^(?:@\w+(?:\([^)]*\))?\s*)*"
    r"(?:public |private |internal |abstract |open |sealed |data |value |enum |annotation "
    r"|suspend |inline |operator |override )*"
    r"(class|interface|object|fun|val|var|typealias)\s+"
    r"(?:<[^>]*>\s*)?(?:[\w.]+(?:<[^>]*>)?\.)?([A-Za-z_]\w*)",
    re.M,
)


def check_kotlin() -> list[str]:
    problems: list[str] = []
    files = glob.glob(os.path.join(JAVA, "**", "*.kt"), recursive=True)
    if not files:
        return ["no Kotlin sources found"]

    declarations: dict[str, set[str]] = {}
    packages: dict[str, str] = {}
    for path in files:
        src = read(path)
        pkg = re.search(r"^package\s+([\w.]+)", src, re.M)
        package = pkg.group(1) if pkg else ""
        packages[path] = package
        names = declarations.setdefault(package, set())
        for match in DECL.finditer(src):
            names.add(match.group(2))
        for match in re.finditer(r"^\s+(?:suspend\s+)?fun\s+([A-Za-z_]\w*)", src, re.M):
            names.add(match.group(1))
        for match in re.finditer(r"^\s+(?:val|var)\s+([A-Za-z_]\w*)", src, re.M):
            names.add(match.group(1))

    for path in files:
        src = read(path)
        code = strip_code(src)
        if code.count("{") != code.count("}"):
            problems.append(f"{rel(path)}: unbalanced braces")
        for line in src.splitlines():
            if not line.startswith("import app.tab.store."):
                continue
            symbol = line.split(".")[-1].strip()
            if symbol in {"R", "BuildConfig"}:
                continue
            target = line[len("import "):].rsplit(".", 1)[0]
            if target not in declarations:
                problems.append(f"{rel(path)}: imports missing package {target}")
            elif symbol not in declarations[target]:
                problems.append(f"{rel(path)}: import {symbol} not found in {target}")
    return problems


# ----------------------------------------------------------------------------- build
def check_build() -> list[str]:
    problems: list[str] = []
    gradle_path = os.path.join(ROOT, "app", "build.gradle.kts")
    if not os.path.exists(gradle_path):
        return ["app/build.gradle.kts missing"]
    gradle = read(gradle_path)

    application_id = re.search(r'applicationId\s*=\s*"([^"]+)"', gradle)
    if not application_id:
        problems.append("build.gradle.kts: no applicationId")
    else:
        services = os.path.join(ROOT, "app", "google-services.json")
        if os.path.exists(services):
            body = read(services)
            if f'"package_name": "{application_id.group(1)}"' not in body:
                problems.append(
                    "google-services.json has no client for "
                    f"applicationId '{application_id.group(1)}'"
                )
        else:
            problems.append("app/google-services.json missing")

    for field in ("versionCode", "versionName"):
        if not re.search(rf"{field}\s*=", gradle):
            problems.append(f"build.gradle.kts: no {field}")

    # ABI filters belong on flavours only; on defaultConfig they leak into every
    # variant (that shipped arm64 into the arm32 APK once).
    default_config = re.search(r"defaultConfig\s*\{(.*?)\n\s{4}\}", gradle, re.S)
    if default_config and "abiFilters" in default_config.group(1):
        problems.append("build.gradle.kts: abiFilters inside defaultConfig "
                        "(must be per flavour)")

    if not re.search(r"isMinifyEnabled\s*=\s*true", gradle):
        problems.append("build.gradle.kts: release build is not minified")
    return problems


def check_indexes() -> list[str]:
    """Firestore rejects a declared index with a single field.

    Single fields are indexed automatically, so declaring one fails the whole
    deploy with "this index is not necessary, configure using single field
    index controls" — which is exactly what happened once.
    """
    import json

    problems: list[str] = []
    path = os.path.join(ROOT, "firestore.indexes.json")
    if not os.path.exists(path):
        return ["firestore.indexes.json missing"]
    data = json.loads(read(path))
    for index in data.get("indexes", []):
        fields = index.get("fields", [])
        if len(fields) < 2:
            names = ", ".join(f.get("fieldPath", "?") for f in fields) or "none"
            problems.append(
                f"firestore.indexes.json: single-field index on "
                f"'{index.get('collectionGroup')}' ({names}) — Firestore "
                f"indexes single fields automatically; remove it"
            )
    return problems


CHECKS = {
    "indexes": check_indexes,
    "secrets": check_secrets,
    "rules": check_rules,
    "icons": check_icons,
    "kotlin": check_kotlin,
    "build": check_build,
}


def main(argv: list[str]) -> int:
    selected = argv[1:] or list(CHECKS)
    unknown = [name for name in selected if name not in CHECKS]
    if unknown:
        print(f"unknown check(s): {', '.join(unknown)}")
        print(f"available: {', '.join(CHECKS)}")
        return 2

    failed = 0
    for name in selected:
        problems = CHECKS[name]()
        if problems:
            failed += 1
            print(f"\n✗ {name}")
            for problem in problems:
                print(f"    {problem}")
        else:
            print(f"✓ {name}")

    print()
    if failed:
        print(f"{failed} check(s) failed")
        return 1
    print(f"all {len(selected)} checks passed")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
