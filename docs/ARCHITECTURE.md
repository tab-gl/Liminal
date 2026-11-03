# Architecture

## Layers

```
app/
  core/            cross-cutting: design system, navigation, Firebase wiring,
                   Cloudinary uploader, Room, DataStore, WorkManager, utils
  domain/          models, repository interfaces, DataResult
  data/            Firestore implementations, DTO mappers, DI bindings
  feature/         one package per surface: auth, home, search, product, store,
                   library, wishlist, checkout, orders, profile, seller, admin,
                   notifications
functions/src/     trusted backend: auth, orders, media, products, admin,
                   notifications, platform
```

Dependency direction is inward: `feature → domain ← data`. ViewModels depend on
repository **interfaces** only, so a Firestore query can be swapped for a fake
in tests without touching UI code.

## State

Every screen owns a ViewModel exposing `StateFlow<UiState>`; Compose collects
with `collectAsStateWithLifecycle()`. UiState classes are immutable data classes
with derived properties (`canSubmit`, `hasMore`, `isEmpty`) so the UI stays
declarative and the branching logic is unit-testable without Android.

One-shot events (snackbars, "navigate to library") are modelled as nullable fields
consumed by a `consume*()` call, which keeps them out of recomposition.

## Money

All prices are `Long` **paise**. No floats anywhere in pricing. The client
formats with `NumberFormat` for `en-IN`; the server computes totals in integers.

## The order flow (the important one)

```
Checkout screen
  └── OrderRepository.placeOrder(addressId, note)
        └── FunctionsClient.call("createOrder")
              └── [server] transaction:
                    read librarys/{uid}/lines
                    read every product document
                    reject if not ACTIVE or stock < quantity
                    recompute subtotal from product prices
                    apply shipping policy (free ≥ ₹999, else ₹49)
                    write orders/{id} with address snapshot + timeline
                    decrement stock, increment unitsSold
                    delete the library lines
                    increment platform/stats.orderCount
                    return orderId
```

Why a function and not a client write:

- the client cannot choose a price, a discount or a total;
- two shoppers cannot buy the last unit — stock is decremented inside the
  transaction that reads it;
- a forged `status: "DELIVERED"` never exists, because `orders` is
  write-denied to clients in `firestore.rules`.

A Firestore trigger (`onOrderCreated`) then notifies the buyer and each
affected seller, and `onOrderStatusChanged` pushes fulfilment updates.

## Media flow (Cloudinary)

```
pick image → downscale to ≤1600px JPEG (CloudinaryUploader.prepare)
  → getCloudinaryUploadSignature(folder)        [server signs]
  → POST multipart straight to api.cloudinary.com with progress
  → store {url, publicId} in Firestore
```

The device posts bytes directly to Cloudinary's ingest endpoint — Firebase
never proxies the binary, so uploads do not count against function egress and
progress is real. Deletion (`deleteCloudinaryAsset`, `deleteProduct`) checks
ownership before calling Cloudinary's destroy endpoint.

## Roles and authorisation

`users/{uid}.role` is the single source of truth. It is:

- written by `setUserRole` (admin only) and `registerSeller` (server-side),
- blocked from client writes in `firestore.rules`,
- re-read from Firestore on **every** privileged callable — never trusted from
  a client payload or a stale custom claim.

## Search

Search is a Firestore range scan on `titleLower`:

```
whereGreaterThanOrEqualTo("titleLower", q)
whereLessThan("titleLower", q + "\uF8FF")
```

This gives real prefix matching with zero extra infrastructure and correct
pagination via `startAfter`. Its limits are honest ones: no typo tolerance, no
fuzzy ranking, no multi-field relevance. `CatalogRepository.search` is the seam
where Algolia/Typesense slots in — the signature already takes filters, sort and
a page token, so only the implementation changes.

## Offline behaviour

- Firestore persistent cache is on (unlimited size) — reads work offline.
- Room caches the catalogue and the recently-viewed rail so the home screen
  paints on a cold, offline start.
- DataStore mirrors the session so the app opens straight into the right place
  before Firestore answers. It is a cache for UI, never for authorisation.
- `CatalogSyncWorker` (WorkManager, 6h, network-constrained) warms the cache.

## Design system

`core/designsystem` holds colour tokens, a type scale, shapes, spacing
(`LocalSpacing`) and ~25 composable primitives (cards, chips, steppers,
skeletons, empty/error states). Screens compose from these, so light/dark and
future rebrands are a token change rather than a screen rewrite.

## Emulator

<a id="emulator"></a>

To run against the local emulators, add to the debug source set:

```kotlin
FirebaseFirestore.getInstance().useEmulator("10.0.2.2", 8080)
FirebaseAuth.getInstance().useEmulator("10.0.2.2", 9099)
FirebaseFunctions.getInstance("asia-south1").useEmulator("10.0.2.2", 5001)
```

and set `CLOUDINARY_URL` in `functions/.env` (see `.env.example`). App Check is
skipped automatically in the emulator.

## Known trade-offs

1. **No reviews yet.** `rating`/`ratingCount` exist on products and stores but
   nothing writes them. Adding a `reviews` subcollection plus an aggregate
   function is the natural next step — I did not want to ship a rating UI backed
   by numbers that never change.
2. **Seller analytics are computed client-side** from up to 300 recent orders
   for that store. Fine at this scale, honest at any scale — but a scheduled
   aggregation into `stores/{id}/stats/daily` is the production move.
3. **Payment is cash-on-delivery only.** The order document carries a
   `payment` block so a gateway (Razorpay/Stripe) can be added without
   reshaping orders.
4. **Single Android module.** Deliberately: one module with clean packages
   builds faster and reads more easily than seven Gradle modules at this size.
