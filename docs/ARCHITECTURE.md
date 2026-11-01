# Architecture

## Layering

Feature-first clean architecture. Each feature owns its slice end to end:

```
features/<feature>/
├── domain/        Pure Dart: entities, repository interfaces, invariants
├── data/          Firebase implementations of those interfaces
└── presentation/  Riverpod controllers, screens, feature widgets
```

Rules that keep the layers honest:

- `domain/` imports nothing from `data/`, Firebase or Flutter widgets.
- `presentation/` never touches `FirebaseFirestore`, `FirebaseAuth` or
  `FirebaseFunctions` directly. It goes through a repository provider.
- Only `data/` knows about `Timestamp`, `FieldValue` and collection paths.
- Every error crossing a repository boundary is normalised to `AppException`
  (`core/errors`), so no screen has to pattern-match `FirebaseException`.

Shared things live in `core/` (theme, services, generic widgets, providers) and
`shared/` (domain models used by more than one feature, shared widgets).

## State management

Riverpod 3, no code generation.

| Provider kind | Used for |
| --- | --- |
| `Provider` | Service locator: repositories, services, `CallableClient` |
| `StreamProvider` | Live documents: profile, listing detail, reviews, inbox, campaign |
| `FutureProvider` | One-shot reads: rails, dashboards, admin queues |
| `AsyncNotifier` | Screens with commands: auth, publishing draft, settings |
| `FeedController<T>` | Infinite lists: pagination contract implemented once |

Two deliberate choices:

1. **Services that need async init are injected, not lazily built.**
   `bootstrap.dart` creates analytics, remote config, push and deep links, then
   overrides their providers in `ProviderScope`. No provider performs hidden I/O
   the first time it is read.
2. **Screen-scoped pagination where the argument varies.** `CategoryScreen` and
   the search screen own a `PaginatedList` instance rather than a global family
   provider, so two categories can never share state.

## Navigation

`go_router` 18 with a `StatefulShellRoute.indexedStack` for the five tabs. Each
branch keeps its own stack and scroll position.

```
/discover /categories /publish /activity /profile     ← shell branches
/app/:id  /developer/:uid  /category/:id  /search     ← public, full screen
/dashboard  /dashboard/:id  /advertising  /campaign/:id
/publish/new  /publish/edit/:id
/settings  /settings/edit-profile  /bookmarks  /admin
```

`redirect` enforces the session rules in one place:

| State | Behaviour |
| --- | --- |
| Auth resolving | `/splash` |
| No user, private route | `/sign-in?from=<attempted>` |
| No user, public route | allowed (`/discover`, `/app/:id`, ...) |
| Profile document missing | `/onboarding` |
| Signed in but not onboarded | `/onboarding` |
| Signed in, on an auth route | `/discover` |

A `ValueNotifier` refresh listener re-runs the redirect when the auth state or
the profile document changes, without rebuilding the router - rebuilding would
reset the navigation stack.

## Error handling

```
FirebaseException / FirebaseAuthException / FirebaseFunctionsException
        ↓ mapError()  (core/errors/error_mapper.dart)
AppException(code, message, retryable)
        ↓
ErrorStateView (retry button only when retryable)
```

Codes are semantic, not provider-specific: `network`, `unauthenticated`,
`permissionDenied`, `notFound`, `invalidInput`, `quotaExceeded`, `moderation`.
The mapper produces copy a person can read ("You appear to be offline"), never a
stack trace or a Firebase error code.

Loading, empty and error states are mandatory. `EmptyView`, `ErrorStateView` and
`CardListSkeleton` exist so no screen can skip them, and every empty state
carries a way forward.

## Startup sequence

`lib/app/bootstrap.dart`, deliberately split in two.

**Phase 1 — awaited (~200 ms), the only work allowed to delay the first frame:**

1. `WidgetsFlutterBinding.ensureInitialized()`
2. `Firebase.initializeApp` — a hard prerequisite for the first Auth and
   Firestore read
3. Optional emulator wiring
4. Crash handlers
5. Firestore offline-cache settings

**Phase 2 — started after `runApp`, never awaited:** App Check (awaited in prod
only), Remote Config, analytics, push, deep links, AdMob, orientation lock.

The design follows from one fact: `FirebaseAuth.currentUser` is populated the
moment `initializeApp` returns, so routing can decide where to send someone
synchronously. That is why **there is no splash screen and no loading route** -
the gap between the native launch surface and the first Flutter frame is a single
frame, and the native launch theme is a flat brand colour with no logo.

Every phase-2 consumer tolerates being cold: Remote Config serves its compiled
defaults until the fetch lands, `SproutlyBannerAd` retries until the AdMob SDK is
ready, and a deep link that arrives a beat late still navigates.

## Performance

- Cursor pagination everywhere; no offset queries and no client-side counts.
- `CachedNetworkImage` for every remote image, with a shimmer placeholder and a
  branded fallback.
- Denormalised developer name/photo on the listing document, so a feed row is
  one read.
- Indexed composite queries only - `firestore.indexes.json` is the complete list.
- `const` widgets throughout, `AnimatedContainer`/`AnimatedScale` instead of
  controllers where a controller is not needed.
- Text scaling clamped to 1.6x so expressive layouts hold together.
