# Sproutly

A native Android multi-vendor marketplace — Kotlin, Jetpack Compose, Material 3,
Firebase and Cloudinary. Customers browse and buy, sellers run a storefront,
admins moderate the platform.

> **Design identity:** Material 3 **Expressive** — Lime `#DBE6A1` · Olive
> `#3F4C06` · Espresso `#3C3130`, the monstera mark as the app icon, springy
> shape-morph motion, and the M3e shape-morph `LoadingIndicator` for every wait
> state. See [docs/BRAND.md](docs/BRAND.md).

---

## What is in this repository

| Area | Contents |
| --- | --- |
| `app/` | The Android application (Kotlin, Compose, MVVM, Clean-ish layering) |
| `functions/` | Cloud Functions in TypeScript — the trusted operations for a Blaze deployment (the app also runs without them, see [SPARK-MODE.md](docs/SPARK-MODE.md)) |
| `firestore.rules` | Security rules for every collection |
| `firestore.indexes.json` | 23 composite indexes used by the queries in the app |
| `.github/workflows/` | Android CI (build + ARM32 verification) and Functions deploy |
| `tools/seed/` | One-time setup script for the category taxonomy |
| `docs/` | Setup, architecture, data model, brand, CI/CD |

**Toolchain:** AGP 8.13.2 · Gradle 8.14.3 · Kotlin 2.2.20 · Compose 1.8.1 ·
material3 **1.5.0-alpha08** · compileSdk/targetSdk 36 · minSdk 24.

`material3 1.5.0-alpha08` is the one pre-release pin, and it is deliberate: it
is the first line that ships `MaterialExpressiveTheme`,
`MotionScheme.expressive()` and the `LoadingIndicator` shape-morph loader while
still supporting AGP 8.x. The later betas require AGP 9.1 and compileSdk 37.

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

## Design system

`core/designsystem` holds the tokens (colour, type, shapes, spacing) plus the
composables every screen is built from. The theme is `MaterialExpressiveTheme`
with `MotionScheme.expressive()`, so buttons, chips, sheets and indicators use
Material's springy expressive motion and shape morphing rather than the older
linear specs. `SproutlyLoader` wraps the M3e `LoadingIndicator` — one shape
morphing through the Material shape set — and replaces spinners everywhere the
app is waiting on data.

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

Both CI workflows are green on `sproutly-native`:

| Check | Result |
| --- | --- |
| Cloud Functions `tsc` (strict, `noUnusedLocals`) | pass |
| Kotlin compile + 24 unit tests | pass |
| Android lint | pass |
| `assembleUniversalDebug` / `assembleArm32Debug` | pass |
| `assembleUniversalRelease` / `assembleArm32Release` (R8) | pass |
| ARM32 APK contains `lib/armeabi-v7a/*.so` and no arm64 payload | verified in CI |
| Universal APK contains both `armeabi-v7a` and `arm64-v8a` | verified in CI |

Release APKs are R8-minified and resource-shrunk (~3.5 MB). CI signs them with
the release keystore when the four `SPROUTLY_*` secrets are present, and falls
back to debug signing when they are not — see [docs/CI-CD.md](docs/CI-CD.md).

## Further reading

- [docs/SETUP.md](docs/SETUP.md) — console-by-console setup
- [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) — layers, flows, trade-offs
- [docs/FIRESTORE.md](docs/FIRESTORE.md) — collections, fields, rules
- [docs/BRAND.md](docs/BRAND.md) — palette, type, components
- [docs/CI-CD.md](docs/CI-CD.md) — workflows and required secrets
