# CI/CD

## Repo checks (runs first, ~10s)

`python3 tools/checks/check_repo.py` gates the build. Each check exists because
this repository hit that failure once:

| Check | Catches |
| --- | --- |
| `secrets` | a live Cloudinary URL, private key or keystore committed. `app/google-services.json` is allow-listed — it is public by design |
| `rules` | unbalanced or malformed `firestore.rules`, unconditional writes, a catch-all that does not deny, and any collection in `FirestorePaths.kt` with no `match` block |
| `icons` | a `HugeIcons` constant with no drawable, or an `ic_hg_*.xml` that nothing exposes |
| `kotlin` | an import into a package that no longer exists, or a symbol that package does not declare; unbalanced braces |
| `build` | an `applicationId` that `google-services.json` has no client for, `abiFilters` leaking into `defaultConfig`, missing version fields, release not minified |

Run one in isolation: `python3 tools/checks/check_repo.py rules`.

## `android.yml`

Runs on push/PR to `main`.

1. JDK 17 + Gradle 8.9 (`gradle/actions/setup-gradle` with caching)
2. `gradle wrapper` — materialises the wrapper in the runner (no wrapper jar is
   committed, which keeps the repo free of binaries)
3. `:app:testUniversalDebugUnitTest`
4. `:app:lintUniversalDebug` (non-blocking; report uploaded)
5. `assembleUniversalDebug` + `assembleArm32Debug`
6. `assembleUniversalRelease` + `assembleArm32Release`
7. **ARM32 verification** — unzips the arm32 APK and fails if there is no
   `lib/armeabi-v7a/*.so` or if any `lib/arm64-v8a/` entry appears
8. **Universal verification** — asserts both ABIs are present
9. Uploads APKs and test/lint reports

### Secrets

| Secret | Required | Purpose |
| --- | --- | --- |
| `SPROUTLY_KEYSTORE_BASE64` | optional | base64 of the release `.jks` |
| `SPROUTLY_KEYSTORE_PASSWORD` | optional | keystore password |
| `SPROUTLY_KEY_ALIAS` | optional | key alias |
| `SPROUTLY_KEY_PASSWORD` | optional | key password |

Without the keystore the release APKs are debug-signed, so the pipeline still
produces installable artifacts on a fork.

```bash
base64 -w0 sproutly-release.jks   # → SPROUTLY_KEYSTORE_BASE64
```

## `functions.yml`

- **verify** (every PR): `npm ci` + `npm run build` in `functions/`
- **deploy** (main only): Google Cloud Workload Identity Federation →
  `firebase deploy --only functions,firestore:rules,firestore:indexes`

### Secrets / vars for deploy

| Name | Kind | Value |
| --- | --- | --- |
| `GCP_WORKLOAD_IDENTITY_PROVIDER` | secret | `projects/NNN/locations/global/workloadIdentityPools/gh/providers/github` |
| `GCP_SERVICE_ACCOUNT` | secret | deployer service account email |
| `FIREBASE_PROJECT_ID` | var | `tab-gl` |

The service account needs `Firebase Admin`, `Cloud Functions Developer`,
`Service Account User` and `Secret Manager Secret Accessor`.

## Manual deploy

```bash
firebase deploy --only firestore:rules,firestore:indexes,functions
```
