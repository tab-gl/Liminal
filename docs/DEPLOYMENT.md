# Build and deployment

## Prerequisites

- Flutter 3.47.x, Xcode 16+ (iOS), Android SDK 36 / AGP 9.1 (Android)
- A Firebase project with the backend deployed (`docs/SETUP.md`)
- Signing keys for both stores

## Android

```bash
flutter build appbundle --release \
  --dart-define=SPROUTLY_ENV=prod \
  --dart-define=RAZORPAY_KEY_ID=rzp_live_xxx \
  --dart-define=STRIPE_PUBLISHABLE_KEY=pk_live_xxx
```

Checklist before upload:

1. `android/app/google-services.json` present (git-ignored).
2. Release keystore SHA-256 registered in Firebase (needed for Google sign-in and
   Play Integrity App Check).
3. `flutter build appbundle` output uploaded to the internal track first.
4. `minSdk` 24, `targetSdk` 36 as configured by the Flutter SDK.

## iOS

```bash
cd ios && pod install && cd ..
flutter build ipa --release \
  --dart-define=SPROUTLY_ENV=prod \
  --dart-define=RAZORPAY_KEY_ID=rzp_live_xxx
```

Checklist:

1. `GoogleService-Info.plist` in `ios/Runner/` (git-ignored).
2. Push capability + APNs key uploaded to Firebase Cloud Messaging.
3. `firebase_app_id_file.json` uploaded for Crashlytics symbolication.
4. Associated domains: `applinks:sproutly.app`.
5. Privacy manifest (`PrivacyInfo.xcprivacy`) lists the collected data types:
   identifiers, usage data, diagnostics.

## Backend

```bash
firebase deploy --only firestore:rules,firestore:indexes,storage:rules
firebase deploy --only functions
```

Deploy rules before functions when a function depends on a new field, so the
rules never lag behind the writer. Deploy functions before the app when the app
depends on a new callable.

## Environments

| Environment | Firebase project | Dart define |
| --- | --- | --- |
| dev | `sproutly-dev` | `SPROUTLY_ENV=dev` (Remote Config refreshes every request) |
| prod | `sproutly-prod` | `SPROUTLY_ENV=prod` (1-hour Remote Config TTL) |

`AppConfig` reads these at compile time; there is no runtime environment switch,
which prevents a debug build from ever pointing at production data.

## Release sequence

1. `flutter analyze` and `flutter test` green.
2. `cd functions && npm run build && npx tsc --noEmit` green.
3. Rules tests against the emulator green (`docs/TESTING.md`).
4. Deploy backend to dev, run the app against it.
5. Deploy backend to prod.
6. Ship the app to the internal track / TestFlight.
7. Promote after 24 hours of clean Crashlytics.

## Rollback

- **Functions**: `firebase deploy --only functions` from the previous tag;
  Cloud Functions keeps the previous revision warm during a rollout.
- **Rules**: the previous `firestore.rules` is in git; redeploy it. Rules changes
  take effect within a minute.
- **App**: Play Console staged rollback / App Store "remove from sale" plus a
  previous build. Because the client is thin, most fixes can ship as a function
  or rule change without a store release.

## Monitoring

- Crashlytics: crash-free users, non-fatals from `PlatformDispatcher.onError`.
- Firebase Analytics: `publish_started` → `publish_submitted` funnel,
  `app_viewed` → `app_clicked` engagement, `search_performed` with result counts.
- Cloud Functions logs: filter `severity>=ERROR`; the aggregation job logs its
  processed count every hour.
- Firestore: watch `applications` where `status == pending_review` count - a
  growing queue means moderators are the bottleneck.
