#!/usr/bin/env python3
"""Fails the build if google-services.json is missing or targets the wrong package.

A mismatched package_name is the classic cause of a build that compiles and then
crashes on first Firebase call, so it is worth failing loudly instead.
"""
import json
import pathlib
import sys

EXPECTED = "app.tab.store"
path = pathlib.Path(__file__).resolve().parent.parent / "app" / "google-services.json"

if not path.exists():
    print(f"::error::{path} is missing. Download it from the Firebase console.")
    sys.exit(1)

data = json.loads(path.read_text())
clients = data.get("client", [])
if not clients:
    print("::error::google-services.json has no client entries.")
    sys.exit(1)

package = clients[0].get("client_info", {}).get("android_client_info", {}).get("package_name", "")
print(f"firebase project: {data.get('project_info', {}).get('project_id')}")
print(f"firebase package: {package}")
if package != EXPECTED:
    print(f"::error::package_name must be {EXPECTED}, found {package!r}.")
    sys.exit(1)
