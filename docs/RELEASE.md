# Release: signing, secrets, CI

Android package: **`app.tab.store`** (set by `tool/configure_platform.sh`, together
with the iOS bundle id and the Kotlin `MainActivity` package path).

## One-time setup

```bash
flutter create --platforms=android,ios --org app.tab --project-name sproutly .
bash tool/configure_platform.sh   # package id, permissions, AdMob id, flat launch theme
bash tool/configure_signing.sh    # signingConfig + key.properties
```

Both scripts are idempotent and both fail loudly if `android/` has not been
generated yet. Neither invents a keystore - you create it once and keep it:

```bash
keytool -genkey -v -keystore android/app/upload-keystore.jks \
  -keyalg RSA -keysize 2048 -validity 10000 -alias upload
```

Then fill `android/key.properties` (or export the variables and re-run the
script):

```properties
storePassword=…
keyPassword=…
keyAlias=upload
storeFile=upload-keystore.jks
```

Both files are git-ignored. **Losing this keystore means losing the ability to
update the app** - back it up somewhere that is not this repository, with the
passwords stored separately.

## What the signing script actually changes

- Injects a `signingConfigs.release` block, in Kotlin DSL or Groovy, whichever
  `flutter create` produced.
- Loads `key.properties` from `rootProject` at configuration time.
- Points the `release` build type at it.
- **Deletes the template's `signingConfig = signingConfigs.debug` line.** Leaving
  it would sign a release build with the debug key - Play rejects that, and it
  breaks upgrades for anyone with an earlier install.

Verify locally:

```bash
cd android && ./gradlew signingReport   # the release variant must show your alias
```

## Secrets

| Secret | Used by | Notes |
| --- | --- | --- |
| `ANDROID_KEYSTORE_BASE64` | APK workflow | `base64 -w0 upload-keystore.jks` |
| `ANDROID_KEYSTORE_PASSWORD` | APK workflow | store password |
| `ANDROID_KEY_PASSWORD` | APK workflow | key password |
| `ANDROID_KEY_ALIAS` | APK workflow | optional, defaults to `upload` |
| `GOOGLE_SERVICES_JSON_BASE64` | APK workflow | `base64 -w0 google-services.json` |
| `RAZORPAY_KEY_ID` | APK workflow | publishable, injected as a dart-define |
| `POSTHOG_API_KEY` | APK workflow | optional |

Server-side secrets (`RAZORPAY_KEY_SECRET`, `STRIPE_SECRET_KEY`, webhook secrets)
never appear in CI for the app build - they live in Secret Manager and are bound
to Cloud Functions only. See docs/SECURITY.md.

## CI workflows

`.github/workflows/android-apk.yml` has **two jobs with different requirements**,
because a check that can only fail is worse than no check.

### `validate` — pull requests, no secrets needed

Runs on every PR touching `lib/`, `android/`, `pubspec.yaml` or `tool/`:

1. Generates `android/` if the checkout has none.
2. `bash tool/configure_platform.sh` (package id, permissions, AdMob, launch theme).
3. `bash tool/stub_google_services.sh` — writes a **placeholder**
   `google-services.json`. The Firebase Gradle plugin reads that file for every
   variant, so its absence breaks compilation even though CI needs no real
   project. The stub is never used for a release.
4. `flutter pub get`, `flutter analyze` (reported, not gated), `flutter test`.
5. `flutter build apk --debug --target-platform android-arm64`.

A contributor without the signing key still gets a meaningful green tick.

### `build` — tags and manual dispatch, secrets required

Two parallel jobs, one per ABI:

| Job | Flutter target | Artifact |
| --- | --- | --- |
| `armeabi-v7a` | `android-arm` | `app-armeabi-v7a-release.apk` |
| `arm64-v8a` | `android-arm64` | `app-arm64-v8a-release.apk` |

Both:

1. Generate `android/` when absent, then apply `configure_platform.sh`.
2. **Check every required secret up front** and print one `::error::` per missing
   value, naming the variable and pointing here — so a failure says
   "ANDROID_KEYSTORE_BASE64 is missing" instead of dying three steps later.
3. Decode `google-services.json` and run `configure_signing.sh`.
4. `flutter build apk --release --target-platform <abi> --split-per-abi`.
5. `apksigner verify --print-certs` before upload — a debug-signed artifact
   cannot slip through.
6. Upload the artifact; on a `v*` tag, attach it to the GitHub release.

## Generating the secrets

```bash
# Firebase Android config (from the Firebase console: Project settings -> Your apps)
base64 -w0 android/app/google-services.json   # -> GOOGLE_SERVICES_JSON_BASE64

# Upload keystore (create it once, back it up somewhere that is not this repo)
keytool -genkey -v -keystore upload-keystore.jks \
  -keyalg RSA -keysize 2048 -validity 10000 -alias upload
base64 -w0 upload-keystore.jks                # -> ANDROID_KEYSTORE_BASE64
```

Add them under **Settings → Secrets and variables → Actions**. On macOS use
`base64 -i file | tr -d '\n'` instead of `-w0`.

The release jobs stay skipped until you either push a `v*` tag or run the
workflow manually from the Actions tab.

## Why split by ABI

A universal APK carries both architectures' native libraries - roughly double the
download for every user, and Play penalises the size. Two split APKs cost one
extra CI job and give each device only its own libraries. `--split-per-abi`
derives the file names, which is why the workflow asserts on
`app-<abi>-release.apk` rather than guessing.

## iOS

Not wired here. `flutter build ipa` needs a macOS runner, an Apple Developer
team, a distribution certificate and a provisioning profile; the bundle id is
already set to `app.tab.store` by `configure_platform.sh`. If you want it, the
same pattern applies: `configure_platform.sh`, then a `macos-latest` job with the
certificate and profile imported from base64 secrets.
