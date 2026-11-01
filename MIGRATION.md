# Flutter → Kotlin migration

The native Android app lives in `app-kotlin/`. The Flutter app stays where it is
until the Kotlin build reaches parity, so nothing is lost and both can be built
in parallel.

## What carries over untouched

This is the part that makes the switch affordable: **the backend needs no
changes at all.**

| Asset | Status |
| --- | --- |
| `firestore.rules` (field-level, role-aware) | Unchanged |
| `storage.rules` (ownership, size and type caps) | Unchanged |
| `firestore.indexes.json` (25 composite indexes) | Unchanged |
| `functions/src/*` (12 Cloud Functions) | Unchanged |
| Data model (collection names, field names, composite ids) | Unchanged |
| AdMob app id and banner unit | Unchanged |
| Design tokens (lime/olive, DM Sans, flat surfaces) | Ported by hand |

The Kotlin models in `data/model/Models.kt` deliberately use the exact Firestore
field names (`ownerName`, `trendingScore`, `reviewCount`…), so the documents
deserialize without a mapping layer and the security rules keep matching.

## Ported so far

| Flutter | Kotlin | Notes |
| --- | --- | --- |
| `sproutly_theme.dart`, `sproutly_colors.dart` | `ui/theme/Theme.kt`, `Color.kt` | Same palette, same light/dark pairs, flat fills only |
| `sproutly_typography.dart` | `ui/theme/Type.kt` | DM Sans from `res/font/dmsans.ttf` (variable); its OFL licence lives in `assets/licenses/` because `res/font` only accepts font files |
| `sproutly_nav_bar.dart` | `ui/components/FloatingNavBar.kt` | Floating island, capsule indicator, raised Publish |
| `sproutly_button.dart`, `sproutly_card.dart` | `ui/components/SproutlyButton.kt`, `Surfaces.kt` | Press scale, pill/expressive shapes |
| `badges.dart`, `rating_stars.dart` | `ui/components/Pills.kt` | Sponsored label included |
| `skeleton.dart`, `shape_morph_loader.dart` | `ui/components/Loaders.kt` | Pulse instead of shimmer; superellipse morph |
| `application_card.dart` | `ui/components/AppListingCard.kt` | Full decision set per card |
| `app_image.dart` | `ui/components/AppImage.kt` | Coil 3 |
| Discover | `feature/discover/*` | Feed + 3 rails, cursor pagination, sort |
| Listing detail | `feature/detail/*` | Live document, reviews, bookmark, install |
| Categories | `feature/categories/*` | Grid, then that category's listings |
| Publish | `feature/publish/*` | Single form, saves a draft then calls `submitApplication` |
| Activity | `feature/activity/*` | In-app inbox, mark read |
| Profile | `feature/profile/*` | Profile, listings, sign out |
| Auth | `feature/auth/*` | Email/password, profile bootstrap |
| `install_service.dart` | `install/InstallManager.kt` | APK intent, Play Store, browser, permission path |
| AdMob banner | `ads/AdMobBanner.kt` | Test unit in debug builds |

## Completed in the second pass

| Flutter | Kotlin | Notes |
| --- | --- | --- |
| Media upload in publishing | `data/repo/Extras.kt` (`MediaRepository`), publish screen pickers | Icon and up to 8 screenshots, uploaded to Storage before the draft is saved |
| Developer dashboard | `feature/dashboard/*` | Totals, CTR, 30-day bar chart drawn on a `Canvas` (no charting dependency) |
| Advertising | `feature/ads/*`, `CampaignRepository` | Create, pause/resume, spend progress; funding goes through Razorpay callables |
| Admin console | `feature/admin/*`, `AdminRepository` | Queue, campaigns, reports, people; every action re-checks the admin claim server-side |
| Review composer | `DetailScreen` dialog, `ReviewRepository` | One review per person per app, enforced by the document id |
| Bookmarks screen | `feature/bookmarks/*` | Saved apps, newest first |
| Settings | `feature/settings/*` | Ad personalisation, install-permission shortcut, sign out |
| Onboarding | `feature/onboarding/*` | Gated on `onboarded` in the profile, not a local flag |
| Push | `push/PushManager.kt` | Token stored on `users/{uid}/private/devices`; channel created at launch |
| Deep links | `Routes.fromDeepLink`, `MainActivity` | `https://sproutly.app/app/<id>` and friends resolve to graph routes |

## Deliberately out of scope

1. **iOS.** This is an Android client. A SwiftUI app would be a second port of the
   same backend — the rules, functions and model still apply unchanged.
2. **Gradle wrapper.** The wrapper jar is binary and is not committed; CI
   provisions Gradle via `gradle/actions/setup-gradle`. Run `gradle wrapper` once
   locally if you want `./gradlew`.
3. **Hugeicons.** No official Compose library exists, so the nav bar uses
   Material outline/filled pairs. Export the icons as vector drawables and swap
   the two `ImageVector`s per destination in `FloatingNavBar.kt`.
4. **Stripe funding.** Razorpay is wired end to end; Stripe's hosted checkout is
   a Cloud Function away but has no client screen yet.

## Deliberate differences

- **No Hilt.** Six repositories, no scopes worth annotating, and skipping the
  annotation processor keeps the build fast. `di/ServiceGraph.kt` is the whole
  graph; adding Hilt later means annotating constructors and deleting that file.
- **No Room.** Firestore's own persistent cache is the offline store.
- **Publish is one form, not five steps.** Fewer states to get wrong on a phone.
- **Skeletons pulse instead of shimmering.** A shimmer sweep is a moving
  gradient, and this design system has no gradients.
- **`Page` is called `Paged`** — `Page` collides with Compose's pager types the
  same way it collided with Flutter's navigator.

## Build

```bash
cd app-kotlin
gradle :app:assembleDebug          # or ./gradlew after `gradle wrapper`
gradle :app:assembleRelease        # needs keystore.properties or env vars
```

CI: `.github/workflows/android-native.yml` — `assembleDebug` on every PR touching
`app-kotlin/`, and the two signed release APKs on `v*-native` tags or manual
dispatch. It also asserts that `google-services.json` targets `app.tab.store`.
