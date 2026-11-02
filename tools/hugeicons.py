#!/usr/bin/env python3
"""
Regenerates res/drawable/ic_hg_*.xml from the Hugeicons free set.

Hugeicons ships as SVG/stroke data with no Android artifact, so the icons the
app uses are compiled into vector drawables once and committed. Run this only
when adding an icon to NAME_MAP.

    python3 tools/hugeicons.py

Requires network access to cdn.jsdelivr.net and node (to read the CJS bundle).
"""
import json
import os
import subprocess
import tempfile
import urllib.request

VERSION = "4.4.0"
CDN = f"https://cdn.jsdelivr.net/npm/@hugeicons/core-free-icons@{VERSION}/dist/cjs/index.min.js"

NAME_MAP = {
    "Store01Icon": "ic_hg_store",
    "Home01Icon": "ic_hg_home",
    "Search01Icon": "ic_hg_search",
    "UserIcon": "ic_hg_user",
    "Download04Icon": "ic_hg_download",
    "Download01Icon": "ic_hg_download_alt",
    "Books02Icon": "ic_hg_library",
    "BookOpen01Icon": "ic_hg_book",
    "LibraryIcon": "ic_hg_library_alt",
    "FavouriteIcon": "ic_hg_favourite",
    "StarIcon": "ic_hg_star",
    "ArrowRight01Icon": "ic_hg_arrow_right",
    "ArrowLeft01Icon": "ic_hg_arrow_left",
    "PlusSignIcon": "ic_hg_plus",
    "FilterIcon": "ic_hg_filter",
    "ShoppingBag01Icon": "ic_hg_bag",
    "Share08Icon": "ic_hg_share",
    "MoreHorizontalIcon": "ic_hg_more",
    "Tick02Icon": "ic_hg_tick",
    "Cancel01Icon": "ic_hg_close",
    "Settings02Icon": "ic_hg_settings",
    "Notification03Icon": "ic_hg_notification",
    "PackageIcon": "ic_hg_package",
    "GridViewIcon": "ic_hg_grid",
    "SparklesIcon": "ic_hg_sparkles",
    "Upload04Icon": "ic_hg_upload",
    "Analytics01Icon": "ic_hg_analytics",
}

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(ROOT, "app/src/main/res/drawable")


def fetch_bundle() -> str:
    with urllib.request.urlopen(CDN) as response:
        bundle = response.read().decode("utf-8")
    path = os.path.join(tempfile.gettempdir(), "hugeicons-free.js")
    with open(path, "w") as handle:
        handle.write(bundle)
    return path


def extract(bundle_path: str) -> dict:
    script = (
        f"const icons = require({json.dumps(bundle_path)});"
        f"const want = {json.dumps(list(NAME_MAP))};"
        "const out = {}; for (const n of want) if (icons[n]) out[n] = icons[n];"
        "process.stdout.write(JSON.stringify(out));"
    )
    result = subprocess.run(["node", "-e", script], capture_output=True, text=True, check=True)
    return json.loads(result.stdout)


def num(value) -> float:
    try:
        return float(value)
    except (TypeError, ValueError):
        return 0.0


def circle_path(cx: float, cy: float, r: float) -> str:
    return f"M{cx - r},{cy} a{r},{r} 0 1,0 {2 * r},0 a{r},{r} 0 1,0 {-2 * r},0"


def rect_path(x, y, w, h, rx) -> str:
    if rx <= 0:
        return f"M{x},{y} h{w} v{h} h{-w} z"
    return (
        f"M{x + rx},{y} h{w - 2 * rx} a{rx},{rx} 0 0,1 {rx},{rx} v{h - 2 * rx} "
        f"a{rx},{rx} 0 0 1 {-rx},{rx} h{-(w - 2 * rx)} a{rx},{rx} 0 0 1 {-rx},{-rx} "
        f"v{-(h - 2 * rx)} a{rx},{rx} 0 0 1 {rx},{-rx} z"
    )


def to_vector(name: str, nodes: list) -> str:
    paths = []
    for tag, attrs in nodes:
        stroke_width = num(attrs.get("strokeWidth", 1.5)) or 1.5
        has_stroke = attrs.get("stroke") is not None
        has_fill = attrs.get("fill") is not None and attrs.get("fill") != "none"
        fill = "#FF000000" if has_fill else "@android:color/transparent"
        stroke = "#FF000000" if has_stroke else "@android:color/transparent"

        if tag == "path":
            data = attrs["d"]
        elif tag == "circle":
            data = circle_path(num(attrs.get("cx")), num(attrs.get("cy")), num(attrs.get("r")))
        elif tag == "rect":
            data = rect_path(
                num(attrs.get("x")), num(attrs.get("y")),
                num(attrs.get("width")), num(attrs.get("height")), num(attrs.get("rx", 0)),
            )
        elif tag == "line":
            data = (f"M{num(attrs.get('x1'))},{num(attrs.get('y1'))} "
                    f"L{num(attrs.get('x2'))},{num(attrs.get('y2'))}")
        else:
            continue

        paths.append(
            "    <path\n"
            f'        android:pathData="{data}"\n'
            f'        android:fillColor="{fill}"\n'
            f'        android:strokeColor="{stroke}"\n'
            f'        android:strokeWidth="{stroke_width}"\n'
            '        android:strokeLineCap="round"\n'
            '        android:strokeLineJoin="round" />'
        )

    return (
        '<?xml version="1.0" encoding="utf-8"?>\n'
        f"<!-- Hugeicons Free {VERSION} — @hugeicons/core-free-icons (MIT) -->\n"
        '<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
        '    android:width="24dp"\n    android:height="24dp"\n'
        '    android:viewportWidth="24"\n    android:viewportHeight="24">\n'
        + "\n".join(paths) + "\n</vector>\n"
    )


def main() -> None:
    bundle = fetch_bundle()
    icons = extract(bundle)
    os.makedirs(OUT, exist_ok=True)
    written = 0
    for source_name, res_name in NAME_MAP.items():
        nodes = icons.get(source_name)
        if not nodes:
            print(f"missing in package: {source_name}")
            continue
        with open(os.path.join(OUT, res_name + ".xml"), "w") as handle:
            handle.write(to_vector(source_name, nodes))
        written += 1
    print(f"wrote {written} icons to {OUT}")


if __name__ == "__main__":
    main()
