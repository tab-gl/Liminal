# Sproutly

A native Android multi-vendor marketplace — Kotlin, Jetpack Compose, Material 3,
Firebase and Cloudinary. Customers browse and buy, sellers run a storefront,
admins moderate the platform.

> **Design identity:** Lime `#DBE6A1` · Olive `#3F4C06` · Espresso `#3C3130`,
> with the monstera mark as the app icon. See [docs/BRAND.md](docs/BRAND.md).

---

## What is in this repository

| Area | Contents |
| --- | --- |
| `app/` | The Android application (Kotlin, Compose, MVVM, Clean-ish layering) |
| `functions/` | Cloud Functions in TypeScript — all trusted operations |
| `firestore.rules` | Security rules for every collection |
| `firestore.indexes.json` | 23 composite indexes used by the queries in the app |
| `.github/workflows/` | Android CI (build + ARM32 verification) and Functions deploy |
| `tools/seed/` | One-time setup script for the category taxonomy |
| `docs/` | Setup, architecture, data model, brand, CI/CD |

## Feature coverage

**Customer** — email + Google sign-in, password reset, session persistence;
personalised home with promo banners, categories, featured / trending / new /
recommended rails and recently-viewed; search with category, price and sort
filters and pagination; product detail with gallery, stock awareness and
wishlist; cart with per-line quantity and stock guards; address book; checkout;
order history, live timeline and cancellation.

**Seller** — store onboarding, store profile with logo and banner uploads,
product CRUD with multi-image galleries, inventory and publish/unpublish,
incoming orders with a fulfilment state machine, and an analytics dashboard
(revenue, orders, units, average order value, 7-day chart, top products).

**Admin** — platform overview, store approval and suspension, user role
management, reported-product moderation, category management. Every admin
action is authorised **server-side**.

## Architecture in one paragraph

The app is a single Gradle module split into `core/`, `domain/`, `data/` and
`feature/` packages. ViewModels expose a `StateFlow<UiState>`; repositories are
interfaces in `domain/repository` bound to Firestore implementations in `data/`
via Hilt. The client never computes money, stock or permissions that matter:
orders, stock movement, role changes, moderation and Cloudinary signing all go
through Cloud Functions, which read the caller's role from Firestore and run in
`asia-south1`. Full detail in [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).

## Quick start

```bash
# 1. Firebase — one time
firebase login
firebase use tab-gl                      # the project in google-services.json
firebase deploy --only firestore:rules,firestore:indexes

# 2. Cloudinary secret (never hardcoded, never in the APK)
firebase functions:secrets:set CLOUDINARY_URL

# 3. Cloud Functions
cd functions && npm ci && npm run build && cd ..
firebase deploy --only functions

# 4. Category taxonomy (real config, not demo data)
cd tools/seed && npm ci && node seed.mjs && cd ../..

# 5. Build the app
gradle wrapper --gradle-version 8.9     # once, if ./gradlew is missing
./gradlew :app:assembleUniversalDebug
```

The debug APK lands in `app/build/outputs/apk/universal/debug/`.

## Build variants

| Flavour | ABIs | Purpose |
| --- | --- | --- |
| `universal` | `armeabi-v7a`, `arm64-v8a` | Normal distribution build |
| `arm32` | `armeabi-v7a` only | Guaranteed 32-bit build for old devices |

CI unzips the `arm32` APK and fails the build if it contains no
`lib/armeabi-v7a/*.so`, or if it leaks any `arm64-v8a` payload.

## Credentials

Nothing sensitive lives in this repository:

- `app/google-services.json` — Firebase client config (public by design).
- `CLOUDINARY_URL` — stored in **Firebase Secret Manager**, read only by Cloud
  Functions. The Android client receives a short-lived signature, never the
  secret. See [docs/SETUP.md](docs/SETUP.md#cloudinary).
- Release keystore — via `keystore.properties` locally or GitHub Actions
  secrets in CI.

## Verification status

- Cloud Functions: `tsc --noEmit` and `tsc` both pass cleanly (TypeScript 5.5,
  strict mode, `noUnusedLocals`).
- Android: the project is complete and self-consistent but has **not** been
  compiled in this environment — there is no Android SDK or JDK here. The first
  `./gradlew assembleUniversalDebug` run happens in CI or Codespaces.

## Further reading

- [docs/SETUP.md](docs/SETUP.md) — console-by-console setup
- [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) — layers, flows, trade-offs
- [docs/FIRESTORE.md](docs/FIRESTORE.md) — collections, fields, rules
- [docs/BRAND.md](docs/BRAND.md) — palette, type, components
- [docs/CI-CD.md](docs/CI-CD.md) — workflows and required secrets
