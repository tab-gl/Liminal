# Setup

## 1. Toolchain

| Tool | Version used | Notes |
| --- | --- | --- |
| Flutter | 3.47.x stable | `material_ui` will not resolve on older SDKs |
| Dart | 3.13.x | Ships with Flutter 3.47 |
| Node | 20 | Cloud Functions runtime |
| Firebase CLI | 14+ | `npm i -g firebase-tools` |
| FlutterFire CLI | latest | `dart pub global activate flutterfire_cli` |

`flutter --version` must report 3.47 or newer. The Material library was
decoupled from the SDK in that release, and Sproutly imports
`package:material_ui/material_ui.dart` rather than `package:flutter/material.dart`.

## 2. Firebase project

```bash
firebase login
firebase projects:create sproutly-prod      # or use an existing project
firebase use sproutly-prod

# Registers the Android and iOS apps and writes the config file
flutterfire configure --project=sproutly-prod
```

`flutterfire configure` overwrites `lib/core/config/firebase_options.dart`,
which is git-ignored. Until you run it, that file throws a readable
`UnsupportedError` at startup instead of failing obscurely.

Enable in the console:

1. **Authentication** → Email/Password and Google.
2. **Firestore** → create the database in `asia-south1` (production mode).
3. **Storage** → default bucket, `asia-south1`.
4. **App Check** → register Android (Play Integrity), iOS (DeviceCheck), and add
   the debug token printed by the app on first run.
5. **Cloud Messaging** → upload an APNs key for iOS push.
6. **Crashlytics** → enable, then upload the iOS `dSYM` on release.

## 3. Deploy the backend

```bash
firebase deploy --only firestore:rules,firestore:indexes,storage:rules,functions
```

Indexes take a few minutes to build. Until they finish, composite queries fail
with `failed-precondition` - the app surfaces that as a retryable error.

## 4. Secrets

Never put secrets in the client or in `--dart-define`. Cloud Functions read them
from Secret Manager:

```bash
firebase functions:secrets:set RAZORPAY_KEY_ID
firebase functions:secrets:set RAZORPAY_KEY_SECRET
firebase functions:secrets:set RAZORPAY_WEBHOOK_SECRET
firebase functions:secrets:set STRIPE_SECRET_KEY
firebase functions:secrets:set STRIPE_WEBHOOK_SECRET
```

The client only receives **publishable** values at build time:

```bash
flutter build apk \
  --dart-define=SPROUTLY_ENV=prod \
  --dart-define=RAZORPAY_KEY_ID=rzp_live_xxxxxxxx \
  --dart-define=STRIPE_PUBLISHABLE_KEY=pk_live_xxxxxxxx
```

## 5. Payment webhooks

| Provider | Endpoint |
| --- | --- |
| Razorpay | `https://asia-south1-<project>.cloudfunctions.net/razorpayWebhook` |
| Stripe | `https://asia-south1-<project>.cloudfunctions.net/stripeWebhook` |

Register both in the respective dashboards. **Budget is credited only by these
webhooks** - the client's success callback means "submitted", not "paid".

## 6. First administrator

Roles live in custom claims, so the first admin must be bootstrapped once:

```bash
node -e "
const admin = require('firebase-admin');
admin.initializeApp({ projectId: 'sproutly-prod' });
admin.auth().getUserByEmail('you@example.com')
  .then(u => admin.auth().setCustomUserClaims(u.uid, { role: 'admin' }))
  .then(() => console.log('done'));
"
```

Then mirror it in Firestore so the UI shows it immediately:

```
users/{uid} → { "role": "admin", "isDeveloper": true }
```

Every subsequent role change goes through the `setUserRole` callable, which
writes both the claim and the document and appends an audit log entry.

## 6b. AdMob

The AdMob SDK needs its app id in the native manifest, which cannot come from a
dart-define. After `flutter create .` generates `android/` and `ios/`:

```bash
bash tool/configure_platform.sh
```

Details, including why interstitials are disabled until a second ad unit exists:
**docs/ADS.md**.

## 7. Deep links

Both `sproutly.app/app/<id>` and `sproutly.app/developer/<uid>` must be
associated with the app:

- **Android** - host `/.well-known/assetlinks.json` with the release signing
  SHA-256. `AndroidManifest.xml` already declares the intent filter.
- **iOS** - host `/.well-known/apple-app-site-association` with your Team ID.

## 8. Emulator run

```bash
firebase emulators:start --only auth,firestore,functions,storage
flutter run --dart-define=USE_FIREBASE_EMULATORS=true \
            --dart-define=FIREBASE_EMULATOR_HOST=10.0.2.2   # 10.0.2.2 on Android emulator, localhost on iOS
```

## Known gaps in this repository

Stated plainly, because they matter more than the parts that work:

1. **Not compiled here.** This environment has no Flutter SDK, so the Dart code
   has not been analysed or run. The TypeScript functions *have* been
   type-checked (`npx tsc --noEmit` passes). Expect a handful of small
   signature fixes on first `flutter analyze` - most likely in the three M3E
   components (see docs/DESIGN_SYSTEM.md) and in package APIs that moved
   between minor versions.
2. **`firebase_options.dart` is a stub** until `flutterfire configure` runs.
3. **No native Google Sign-In SDK.** Google sign-in goes through
   `signInWithProvider(GoogleAuthProvider())`, which needs the SHA-1 registered
   on Android. If you want the native account picker, add `google_sign_in` and
   swap the method in `AuthRepositoryImpl`.
4. **Stripe uses hosted Checkout** in the system browser rather than a native
   payment sheet, so no card data touches the app. A native sheet would need
   `flutter_stripe` plus PaymentSheet setup.
5. **Images are uploaded at the picker's compression**, not re-encoded on the
   client. For stricter limits, add a resize step or a Storage extension.
6. **Search is prefix + keyword matching**, not full-text. Firestore has no
   full-text index; for fuzzy search add Algolia or Typesense behind a callable.
