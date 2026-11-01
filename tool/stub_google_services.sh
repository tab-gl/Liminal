#!/usr/bin/env bash
#
# Writes a syntactically valid *placeholder* google-services.json.
#
# Why: the Firebase Android Gradle plugin reads this file for every build
# variant, so its absence fails the build even when nothing at runtime needs
# Firebase. Continuous integration (analyze, test, a debug APK) does not need a
# real project - it needs the file to exist and to name the right package.
#
# This must never be used for a release build. The workflow only calls it for
# pull-request validation, and a release build without a real config is refused
# by the job that requires the secret.
#
#   bash tool/stub_google_services.sh [package_id]
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
PACKAGE_ID="${1:-app.tab.store}"
TARGET="$ROOT/android/app/google-services.json"

if [ ! -d "$ROOT/android/app" ]; then
  echo "error: android/app not found. Run 'flutter create --platforms=android .' first." >&2
  exit 1
fi

cat > "$TARGET" <<JSON
{
  "project_info": {
    "project_number": "000000000000",
    "project_id": "sproutly-ci-stub",
    "storage_bucket": "sproutly-ci-stub.appspot.com"
  },
  "client": [
    {
      "client_info": {
        "mobilesdk_app_id": "1:000000000000:android:0000000000000000000000",
        "android_client_info": {
          "package_name": "${PACKAGE_ID}"
        }
      },
      "oauth_client": [],
      "api_key": [
        {
          "current_key": "AIzaSyAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
        }
      ],
      "services": {
        "appinvite_service": {
          "other_platform_oauth_client": []
        }
      }
    }
  ],
  "configuration_version": "1"
}
JSON

echo "stub google-services.json written for ${PACKAGE_ID} (placeholder, not a real Firebase project)"
