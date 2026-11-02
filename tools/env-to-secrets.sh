#!/usr/bin/env bash
#
# Pushes a .env file into the repository's GitHub Actions secrets.
#
#   ./tools/env-to-secrets.sh .env            # uses tab-gl/Sproutly
#   ./tools/env-to-secrets.sh .env owner/repo
#
# Only the keys GitHub actually needs are pushed; local-only keys (project ids,
# paths) are skipped. Empty values are skipped too, so a half-filled .env is
# safe to run.
set -euo pipefail

ENV_FILE="${1:-.env}"
REPO="${2:-tab-gl/Sproutly}"

[ -f "$ENV_FILE" ] || { echo "No such file: $ENV_FILE"; exit 1; }
command -v gh >/dev/null || { echo "gh CLI not found — install it first"; exit 1; }
gh auth status >/dev/null 2>&1 || { echo "Run: gh auth login"; exit 1; }

# Keys that belong in GitHub Actions.
WANTED=(
  GOOGLE_SERVICES_JSON_BASE64
  SPROUTLY_KEYSTORE_BASE64
  ANDROID_KEYSTORE_BASE64
  SPROUTLY_KEYSTORE_PASSWORD
  ANDROID_KEYSTORE_PASSWORD
  SPROUTLY_KEY_ALIAS
  ANDROID_KEY_ALIAS
  SPROUTLY_KEY_PASSWORD
  ANDROID_KEY_PASSWORD
  RAZORPAY_KEY_ID
  POSTHOG_API_KEY
)

set_count=0
skip_count=0
for key in "${WANTED[@]}"; do
  value="$(grep -E "^${key}=" "$ENV_FILE" | tail -1 | cut -d= -f2- || true)"
  if [ -z "${value}" ]; then
    skip_count=$((skip_count + 1))
    continue
  fi
  printf '%s' "$value" | gh secret set "$key" --repo "$REPO" >/dev/null
  echo "  set ${key}"
  set_count=$((set_count + 1))
done

echo
echo "${set_count} secret(s) set on ${REPO}, ${skip_count} skipped (empty)."
echo "Verify with: gh secret list --repo ${REPO}"
