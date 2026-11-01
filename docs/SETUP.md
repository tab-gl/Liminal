# Setup

Everything here is a one-time step per environment. The app will not work
against a fresh Firebase project until steps 1–5 are done.

## 0. Prerequisites

- JDK 17, Android SDK 35 (`compileSdk`), Gradle 8.9
- Node 20 (Cloud Functions)
- `firebase-tools` (`npm i -g firebase-tools`)
- A Firebase project — this repo is wired to **`tab-gl`**
  (`app/google-services.json`)

## 1. Firebase project

`app/google-services.json` already targets:

- project id `tab-gl`
- package `app.tab.store`
- a web OAuth client used by Google Sign-In

If you use a different project:

1. Create the Android app with package `app.tab.store`.
2. Download the new `google-services.json` into `app/`.
3. Add your SHA-1 and SHA-256 fingerprints (required for Google Sign-In):

```bash
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey \
  -storepass android -keypass android
```

4. Update `.firebaserc` to the new project id.

## 2. Enable services

In the Firebase console, for `tab-gl`:

| Service | Setting |
| --- | --- |
| Authentication | Enable **Email/Password** and **Google** |
| Cloud Firestore | Create in **asia-south1** (Mumbai), production mode |
| Cloud Functions | Requires the Blaze plan |
| Cloud Messaging | Enabled by default |
| App Check | Register the Android app with **Play Integrity**; add a debug token for local builds |

## 3. Firestore rules and indexes

```bash
firebase deploy --only firestore:rules,firestore:indexes
```

Index creation takes a few minutes. Queries will throw
`FAILED_PRECONDITION` until the composite indexes finish building — the app
surfaces that as "This list needs a moment to load. Please retry."

## 4. Cloudinary

<a id="cloudinary"></a>

Create (or reuse) a Cloudinary account and note the API environment variable,
which has the shape:

```
cloudinary://<api_key>:<api_secret>@<cloud_name>
```

Store it as a secret — **never** in the repository, Gradle file or APK:

```bash
firebase functions:secrets:set CLOUDINARY_URL
# paste the cloudinary:// value when prompted
firebase functions:secrets:access CLOUDINARY_URL   # verify
```

What the client gets instead, per upload: `cloudName`, the public `apiKey`, a
`timestamp`, a SHA-1 `signature` over `folder` + `timestamp`, and the upload
URL. `getCloudinaryUploadSignature` returns only that, after checking the
caller is signed in and that the target folder is one of
`sproutly/products`, `sproutly/avatars`, `sproutly/store-logos`,
`sproutly/store-banners`.

> **If a Cloudinary secret has ever been pasted into chat, a ticket or a
> screenshot, rotate it in the Cloudinary console** and update the secret:
> `firebase functions:secrets:set CLOUDINARY_URL`.

## 5. Deploy functions

```bash
cd functions
npm ci
npm run build
cd ..
firebase deploy --only functions
```

All functions deploy to `asia-south1`. The first deploy creates the secret
bindings.

## 6. Seed the category taxonomy

Categories are real configuration data that the home rails and search filters
read from Firestore. Seed them once:

```bash
cd tools/seed
npm ci
GOOGLE_APPLICATION_CREDENTIALS=./service-account.json node seed.mjs
cd ../..
```

Create the service account key in **Project settings → Service accounts**.
Delete the key file afterwards.

## 7. Make yourself an admin

New accounts are always `CUSTOMER`. To bootstrap the first administrator:

1. Sign up in the app.
2. In Firestore, open `users/{your-uid}` and set `role` to `ADMIN`.
3. Reopen the app — the Profile tab now shows **Admin console**.

From then on, use **Admin → Users** to change anyone's role. The client can
never promote itself: `firestore.rules` blocks writes to `role`, and
`setUserRole` checks the caller's stored role server-side.

## 8. Local development

```bash
# Emulators (auth, firestore, functions, UI)
firebase emulators:start --only auth,firestore,functions

# Point the app at them (optional): see docs/ARCHITECTURE.md#emulator
./gradlew :app:assembleUniversalDebug
```

For App Check in debug builds the app installs the **debug** provider; copy the
debug token from logcat into **App Check → Apps → Manage debug tokens**.

## 9. Release signing

Local:

```bash
cp keystore.properties.example keystore.properties
# fill in storeFile / storePassword / keyAlias / keyPassword
./gradlew :app:assembleUniversalRelease
```

CI: add the secrets listed in [CI-CD.md](CI-CD.md). Without them the release
build falls back to debug signing so the pipeline still produces installable
artifacts.

## Troubleshooting

| Symptom | Cause |
| --- | --- |
| `PERMISSION_DENIED` on first sign-in | `firestore.rules` deployed? User doc missing? |
| Google Sign-In returns `10` | SHA-1 not registered, or `GOOGLE_WEB_CLIENT_ID` is wrong |
| Uploads fail with 401 from Cloudinary | Secret missing/stale, or device clock skew |
| Empty home rails | No `ACTIVE` products yet, or indexes still building |
| `FAILED_PRECONDITION` on search | Composite index still building |
| Functions say `unauthenticated` | App Check token missing (debug token not registered) |
