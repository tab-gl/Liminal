#!/usr/bin/env bash
#
# Native configuration that cannot come from Dart.
#
# Two jobs:
#   1. AdMob app id - read by the native SDK before Dart starts, so it must be in
#      the Android manifest and the iOS Info.plist.
#   2. REQUEST_INSTALL_PACKAGES - required on Android 8+ before the app may hand
#      a downloaded APK to the system package installer (see
#      lib/core/services/install_service.dart).
#
# Run once, after `flutter create .` has generated android/ and ios/:
#
#   ./tool/configure_platform.sh
#
# It is idempotent: running it twice changes nothing.
set -euo pipefail

ADMOB_APP_ID="${ADMOB_APP_ID:-ca-app-pub-9808362123414569~3261485863}"
PACKAGE_ID="${PACKAGE_ID:-app.tab.store}"
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
MANIFEST="$ROOT/android/app/src/main/AndroidManifest.xml"
PLIST="$ROOT/ios/Runner/Info.plist"

python3 - "$MANIFEST" "$PLIST" "$ADMOB_APP_ID" "$PACKAGE_ID" "$ROOT" <<'PY'
import sys, os, re, glob, shutil

manifest, plist, app_id, package_id, root = sys.argv[1:6]
changed = []

# ---------------------------------------------------------------- Android
if os.path.exists(manifest):
    src = open(manifest).read()
    if "com.google.android.gms.ads.APPLICATION_ID" in src:
        print("android: AdMob app id already present")
    else:
        meta = (
            '        <meta-data\n'
            '            android:name="com.google.android.gms.ads.APPLICATION_ID"\n'
            f'            android:value="{app_id}"/>\n'
        )
        # Insert just before the closing </application> tag.
        if "</application>" in src:
            src = src.replace("</application>", meta + "    </application>", 1)
        else:
            src = src.replace("</manifest>", meta + "</manifest>", 1)
        open(manifest, "w").write(src)
        changed.append("android manifest")

    # Permissions. INTERNET is needed for ads and the API on some OEM builds;
    # REQUEST_INSTALL_PACKAGES is what lets Sproutly open the system installer
    # for a downloaded APK. Android still shows its own confirmation dialog - the
    # permission does not allow silent installs.
    src = open(manifest).read()
    perms = []
    if 'android.permission.INTERNET' not in src:
        perms.append('    <uses-permission android:name="android.permission.INTERNET"/>')
    if 'android.permission.REQUEST_INSTALL_PACKAGES' not in src:
        perms.append('    <uses-permission android:name="android.permission.REQUEST_INSTALL_PACKAGES"/>')
    if perms:
        src = src.replace("<application", "\n".join(perms) + "\n    <application", 1)
        open(manifest, "w").write(src)
        changed.append("android permissions (" + str(len(perms)) + ")")
else:
    print(f"skip: {manifest} not found - run `flutter create .` first")

# -------------------------------------------------------------------- iOS
if os.path.exists(plist):
    src = open(plist).read()
    if "GADApplicationIdentifier" in src:
        print("ios: AdMob app id already present")
    else:
        entry = (
            "\t<key>GADApplicationIdentifier</key>\n"
            f"\t<string>{app_id}</string>\n"
        )
        new = src.replace("</dict>\n</plist>", entry + "</dict>\n</plist>", 1)
        if new != src:
            open(plist, "w").write(new)
            changed.append("ios Info.plist")

    src = open(plist).read()
    if "NSUserTrackingUsageDescription" not in src:
        entry = (
            "\t<key>NSUserTrackingUsageDescription</key>\n"
            "\t<string>Sproutly uses this to show relevant ads that keep the "
            "platform free. You can turn it off in Settings.</string>\n"
        )
        new = src.replace("</dict>\n</plist>", entry + "</dict>\n</plist>", 1)
        if new != src:
            open(plist, "w").write(new)
            changed.append("ios ATT description")

    if "SKAdNetworkItems" not in src:
        # The full list is long; these are the networks Google requires as a
        # minimum for AdMob mediation-free bidding on iOS 14+.
        networks = [
            "cstr6suwn9.skadnetwork",
            "4fzdc2evr5.skadnetwork",
            "2fnua5tdw4.skadnetwork",
            "ydx93a7ass.skadnetwork",
            "p78axxw29g.skadnetwork",
            "v72qych5uu.skadnetwork",
            "ludvb6z3bs.skadnetwork",
            "cp8zw746q7.skadnetwork",
            "3sh42y64q3.skadnetwork",
            "c6k4g5qg8m.skadnetwork",
        ]
        items = "\t<key>SKAdNetworkItems</key>\n\t<array>\n"
        for network in networks:
            items += f"\t\t<dict>\n\t\t\t<key>SKAdNetworkIdentifier</key>\n\t\t\t<string>{network}</string>\n\t\t</dict>\n"
        items += "\t</array>\n"
        new = src.replace("</dict>\n</plist>", items + "</dict>\n</plist>", 1)
        if new != src:
            open(plist, "w").write(new)
            changed.append("ios SKAdNetworkItems")
else:
    print(f"skip: {plist} not found - run `flutter create .` first")

# --------------------------------------------------- Android package id
gradle_files = [p for p in (
    os.path.join(root, "android/app/build.gradle.kts"),
    os.path.join(root, "android/app/build.gradle"),
) if os.path.exists(p)]
for gradle in gradle_files:
    src = open(gradle).read()
    new = re.sub(r'(applicationId|namespace)(\s*=\s*|\s+)["\'][^"\']+["\']',
                 lambda m: m.group(1) + m.group(2) + '"' + package_id + '"', src)
    if new != src:
        open(gradle, "w").write(new)
        changed.append("android package id")

# MainActivity must live in the directory matching its package.
for activity in glob.glob(os.path.join(root, "android/app/src/main/kotlin/**/MainActivity.kt"), recursive=True):
    src = open(activity).read()
    if f"package {package_id}" in src:
        continue
    src = re.sub(r'package\s+[\w.]+', f'package {package_id}', src, count=1)
    target_dir = os.path.join(root, "android/app/src/main/kotlin", *package_id.split("."))
    os.makedirs(target_dir, exist_ok=True)
    target = os.path.join(target_dir, "MainActivity.kt")
    open(target, "w").write(src)
    if os.path.abspath(activity) != os.path.abspath(target):
        os.remove(activity)
    changed.append("MainActivity package")

# ------------------------------------------------------- Android compileSdk
# permission_handler_android 13 compiles against API 37, so the app must too.
# AGP 9.1 caps its *recommended* compile SDK at 36, hence the suppression flag
# below - without it the build fails on AAR metadata before compiling anything.
for gradle in gradle_files:
    src = open(gradle).read()
    new = src
    if 'compileSdk = flutter.compileSdkVersion' in new:
        new = new.replace('compileSdk = flutter.compileSdkVersion', 'compileSdk = 37')
    elif 'compileSdkVersion flutter.compileSdkVersion' in new:
        new = new.replace('compileSdkVersion flutter.compileSdkVersion', 'compileSdkVersion 37')
    if new != src:
        open(gradle, 'w').write(new)
        changed.append("compileSdk 37")

gradle_props = os.path.join(root, "android/gradle.properties")
if os.path.exists(gradle_props):
    src = open(gradle_props).read()
    if "suppressUnsupportedCompileSdk" not in src:
        if not src.endswith("\n"):
            src += "\n"
        src += ("# compileSdk 37 is required by permission_handler_android 13; AGP 9.1\n"
                "# only recommends up to 36, so the warning is suppressed deliberately.\n"
                "android.suppressUnsupportedCompileSdk=37\n")
        open(gradle_props, "w").write(src)
        changed.append("compileSdk suppression flag")

# --------------------------------------------------------- iOS bundle id
pbxproj = os.path.join(root, "ios/Runner.xcodeproj/project.pbxproj")
if os.path.exists(pbxproj):
    src = open(pbxproj).read()
    def bundle(match):
        value = match.group(1)
        suffix = ".RunnerTests" if value.endswith("RunnerTests") else ""
        return f"PRODUCT_BUNDLE_IDENTIFIER = {package_id}{suffix};"
    new = re.sub(r'PRODUCT_BUNDLE_IDENTIFIER = ([^;]+);', bundle, src)
    if new != src:
        open(pbxproj, "w").write(new)
        changed.append("ios bundle id")

# ------------------------------------------- launch theme: solid, no logo
# A logo splash is the thing that makes an app feel slow. A flat surface in the
# app's own background colour reads as "already loaded".
res = os.path.join(root, "android/app/src/main/res")
if os.path.isdir(res):
    colors = os.path.join(res, "values/colors.xml")
    os.makedirs(os.path.dirname(colors), exist_ok=True)
    if not os.path.exists(colors):
        open(colors, "w").write(
            '<?xml version="1.0" encoding="utf-8"?>\n<resources>\n'
            '    <color name="sproutly_launch">#FAF9F2</color>\n'
            '    <color name="sproutly_launch_dark">#16170F</color>\n'
            '</resources>\n'
        )
        changed.append("launch colours")

    styles = os.path.join(res, "values/styles.xml")
    if os.path.exists(styles):
        src = open(styles).read()
        if "sproutly_launch" not in src:
            src = re.sub(
                r'(<style name="LaunchTheme"[^>]*>[\s\S]*?)<item name="android:windowBackground">[^<]*</item>',
                r'\1<item name="android:windowBackground">@color/sproutly_launch</item>',
                src, count=1,
            )
            if "sproutly_launch" not in src:
                src = src.replace(
                    '<style name="LaunchTheme"',
                    '<style name="LaunchTheme" parent="@android:style/Theme.Light.NoTitleBar">\n        <item name="android:windowBackground">@color/sproutly_launch</item>\n        <!--',
                    1,
                )
            open(styles, "w").write(src)
            changed.append("launch theme")

    # Android 12+ draws its own splash; give it the same flat colour and no icon.
    v31 = os.path.join(res, "values-v31/styles.xml")
    os.makedirs(os.path.dirname(v31), exist_ok=True)
    if not os.path.exists(v31):
        open(v31, "w").write(
            '<?xml version="1.0" encoding="utf-8"?>\n<resources>\n'
            '    <style name="LaunchTheme" parent="@android:style/Theme.Light.NoTitleBar">\n'
            '        <item name="android:windowSplashScreenBackground">@color/sproutly_launch</item>\n'
            '        <item name="android:windowBackground">@color/sproutly_launch</item>\n'
            '    </style>\n</resources>\n'
        )
        changed.append("android 12 splash theme")

print("updated: " + (", ".join(changed) if changed else "nothing"))
PY

echo
echo "AdMob app id: $ADMOB_APP_ID"
echo "Package id:    $PACKAGE_ID"
echo "Android permissions ensured: INTERNET, REQUEST_INSTALL_PACKAGES"
echo
echo "Next: confirm the same id appears in the AdMob console under App settings,"
echo "then run the app once - the debug log prints a test-device hash you can"
echo "add to AdsService.testDeviceIds."
