# Sproutly

**Build it. Publish it. Grow it.**

A cross-platform Flutter marketplace where independent developers publish apps
for free, find early audiences, and promote them through a self-service
advertising system.

---

## Two clients, one backend

| Path | Stack | Status |
| --- | --- | --- |
| `lib/`, `android/`, `ios/` | Flutter + Firebase | Complete, CI-verified (analyze, tests, debug APK) |
| `app-kotlin/` | Kotlin + Jetpack Compose + Firebase | In progress — core flows ported, see [MIGRATION.md](MIGRATION.md) |

The Cloud Functions, security rules, indexes and data model are shared: a
platform switch does not touch the backend.

## What is in this repository

```
sproutly/
├── lib/                        Flutter application (feature-first clean architecture)
│   ├── app/                    bootstrap, router, shell, root widget
│   ├── core/                   theme, services, providers, widgets, errors, utils
│   ├── features/               auth, onboarding, discovery, categories, applications,
│   │                           publishing, developer_dashboard, advertising, bookmarks,
│   │                           reviews, profiles, notifications, settings, admin
│   ├── firebase/               collection paths + typed callable client
│   └── shared/                 domain models and shared widgets
├── functions/                  Cloud Functions (TypeScript, Node 20, asia-south1)
├── firestore.rules             Field-level, role-aware security rules
├── storage.rules               Storage rules with size/type caps and ownership checks
├── firestore.indexes.json      Every composite index the queries need
├── firebase.json               Emulator + deploy configuration
├── app-kotlin/                 Native Android client (Kotlin + Compose)
├── .github/workflows/          Flutter validation + native APK builds
├── tool/                       Platform, signing and AdMob configuration scripts
├── test/                       Unit, widget and security-rule tests
└── docs/                       Architecture, data model, security, design system, ops
```

## Quick start

```bash
flutter --version          # needs 3.47.x (Dart 3.13) or newer
flutter pub get
flutterfire configure --project=<your-firebase-project-id>   # writes lib/core/config/firebase_options.dart

# Generate the native projects, then apply the config that cannot come from Dart
flutter create --platforms=android,ios --org app.tab --project-name sproutly .
bash tool/configure_platform.sh     # package id app.tab.store, permissions, AdMob, flat launch theme
bash tool/configure_signing.sh      # release signingConfig + key.properties

cd functions && npm install && npm run build && cd ..

# Emulators (recommended for first run)
firebase emulators:start --only auth,firestore,functions,storage
flutter run --dart-define=SPROUTLY_ENV=dev \
            --dart-define=USE_FIREBASE_EMULATORS=true \
            --dart-define=FIREBASE_EMULATOR_HOST=10.0.2.2
```

Full instructions, including Razorpay/Stripe and App Check setup: **docs/SETUP.md**.

## The five destinations

| Tab | What it does |
| --- | --- |
| **Discover** | Featured, trending, recently launched, editor's picks, rising startups, sponsored rail, popular developers, then the paginated all-apps feed with search and sorting |
| **Categories** | Thirteen categories, each with its own paginated, sortable listing |
| **Publish** | Five-step publishing flow with media upload, draft saving, validation and a real preview |
| **Activity** | In-app notification inbox (approvals, reviews, campaign alerts) |
| **Profile** | Your profile, saved apps, developer dashboard, advertising, settings, admin (if you have the role) |

Android package: **`app.tab.store`**. Startup has no splash screen - Firebase
initialises, the app renders, and Remote Config, push, deep links and AdMob load
behind the first frame.

Ads: Google AdMob banners fund free publishing (one at the end of the feed, one
below the reviews - never above the fold), separate from the Sponsored
placements developers buy. See **docs/ADS.md**.

## What is real, and what is not

Everything that looks like a backend call **is** a backend call. There are no
mocked repositories, no seeded fake listings, and no placeholder handlers:

- Listings, reviews, bookmarks, follows and campaigns are Firestore documents
  read through indexed queries with cursor pagination.
- Ratings, view counts, click counts, bookmark counts, follower counts,
  campaign impressions, clicks and spend are computed by Cloud Functions from
  raw events. The client cannot write them - `firestore.rules` rejects it.
- Moderation, campaign approval, payment capture and ad delivery are server-side
  operations with admin-claim checks.
- Images are uploaded to Cloud Storage through the native picker with progress,
  and re-validated by `storage.rules`.
- AdMob banners use Google's test units in every debug build, and the app id is
  injected into the native manifests by `tool/configure_platform.sh`.
- "Install" installs: an `.apk` listing requests `REQUEST_INSTALL_PACKAGES`,
  streams the package with progress and hands it to the Android installer; store
  links open the native store client. See docs/INSTALL.md.

The honest gaps are listed at the end of **docs/SETUP.md** - chiefly that this
repository has not been compiled in this environment (no Flutter SDK available
here), and that `lib/core/config/firebase_options.dart` ships as a stub until
you run `flutterfire configure`.

## Design

Lime `#DBE6A1` and Olive `#3F4C06` on a warm off-white `#FAF9F2`, set in **DM
Sans** (one variable font file, real weights via the `wght` axis), with flat
fills instead of gradients, expressive shapes, spring-driven press feedback and a
floating five-slot navigation bar built on Hugeicons.
Light and dark themes both keep the brand identity. Details, including why
Material 3 Expressive arrives through a package rather than the SDK:
**docs/DESIGN_SYSTEM.md**.

## Documentation

| Document | Contents |
| --- | --- |
| [docs/SETUP.md](docs/SETUP.md) | Firebase project, CLI, emulators, payments, App Check, known gaps |
| [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) | Layering, state management, routing, error handling |
| [docs/DATA_MODEL.md](docs/DATA_MODEL.md) | Every collection, field, id convention and index |
| [docs/SECURITY.md](docs/SECURITY.md) | Threat model, rules walkthrough, roles, secrets |
| [docs/DESIGN_SYSTEM.md](docs/DESIGN_SYSTEM.md) | Tokens, components, motion, M3E integration |
| [docs/ADVERTISING.md](docs/ADVERTISING.md) | Campaign lifecycle, pricing, fraud controls, labelling |
| [docs/CALLABLES.md](docs/CALLABLES.md) | Callable function reference (arguments and returns) |
| [docs/ADS.md](docs/ADS.md) | AdMob: credentials, platform setup, placements, consent |
| [docs/INSTALL.md](docs/INSTALL.md) | Install flow: APK hand-off, permissions, store links |
| [docs/RELEASE.md](docs/RELEASE.md) | Signing, secrets, CI workflows, ABI splits |
| [MIGRATION.md](MIGRATION.md) | Kotlin rewrite: what carried over, what is left |
| [docs/DEPLOYMENT.md](docs/DEPLOYMENT.md) | Build, sign, release, rollback |
| [docs/TESTING.md](docs/TESTING.md) | Test strategy and how to run each suite |

## Credits

- Icons: [Hugeicons](https://hugeicons.com) - 6,000+ free stroke-rounded icons,
  MIT licensed. Used throughout the interface, including the floating
  navigation bar.
- Design language: Material 3 Expressive, via `material_ui` and
  `material_3_expressive` (see docs/DESIGN_SYSTEM.md).

## License

Proprietary. © Sproutly.
