#!/usr/bin/env bash
#
# Registers the four signing secrets that GitHub Actions needs to produce
# signed release APKs.
#
# Run this yourself, once, from a machine where you are logged in as a user
# with admin rights on the repository:
#
#     gh auth login                 # if you have not already
#     ./scripts/setup-ci-secrets.sh /path/to/sproutly-release.jks
#
# The keystore password is prompted for and never written to disk or to the
# repository. CI builds are debug-signed until these secrets exist.

set -euo pipefail

KEYSTORE="${1:-}"
REPO="${2:-tab-gl/Sproutly}"
ALIAS="${SPROUTLY_KEY_ALIAS:-sproutly}"

if [[ -z "$KEYSTORE" ]]; then
  echo "usage: $0 /path/to/sproutly-release.jks [owner/repo]" >&2
  exit 2
fi

if [[ ! -f "$KEYSTORE" ]]; then
  echo "error: keystore not found: $KEYSTORE" >&2
  exit 2
fi

if ! command -v gh >/dev/null 2>&1; then
  echo "error: GitHub CLI (gh) is not installed — https://cli.github.com" >&2
  exit 2
fi

if ! gh auth status >/dev/null 2>&1; then
  echo "error: not logged in. Run 'gh auth login' first." >&2
  exit 2
fi

read -r -s -p "Keystore password: " KEYSTORE_PASSWORD
echo
if [[ -z "$KEYSTORE_PASSWORD" ]]; then
  echo "error: password cannot be empty" >&2
  exit 2
fi

read -r -s -p "Key password (press Enter if the same): " KEY_PASSWORD
echo
KEY_PASSWORD="${KEY_PASSWORD:-$KEYSTORE_PASSWORD}"

echo "→ base64-encoding $KEYSTORE"
BASE64_KEYSTORE="$(base64 -w0 "$KEYSTORE" 2>/dev/null || base64 "$KEYSTORE" | tr -d '\n')"

echo "→ writing secrets to $REPO"
printf '%s' "$BASE64_KEYSTORE"    | gh secret set SPROUTLY_KEYSTORE_BASE64   --repo "$REPO"
printf '%s' "$KEYSTORE_PASSWORD"  | gh secret set SPROUTLY_KEYSTORE_PASSWORD --repo "$REPO"
printf '%s' "$ALIAS"              | gh secret set SPROUTLY_KEY_ALIAS         --repo "$REPO"
printf '%s' "$KEY_PASSWORD"       | gh secret set SPROUTLY_KEY_PASSWORD      --repo "$REPO"

echo "→ secrets now registered:"
gh secret list --repo "$REPO"

cat <<'EOF'

Done. The next push to main (or a manual "Android CI" run) will build
release APKs signed with this keystore instead of the debug key.

Check the signing worked:
  gh run list --repo REPO --workflow "Android CI" --limit 1
  gh run download <run-id> --repo REPO -n sproutly-apks
  apksigner verify --print-certs app-universal-release.apk
EOF
