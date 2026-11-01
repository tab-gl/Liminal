# Go live

Everything in this file is a step that needs credentials I do not hold (Firebase
console, Cloudinary, GitHub Actions secrets). Each one is a single command or a
single console action. Work top to bottom.

## Already done and verified

| | Status |
| --- | --- |
| Native app built (Kotlin + Compose + M3 Expressive) | ✅ 1.0.1 |
| Cloud Functions typecheck (strict TS) | ✅ green in CI |
| Kotlin compile, 24 unit tests, lint | ✅ green in CI |
| 4 APK variants, ARM32 + universal ABI assertions | ✅ green in CI |
| Release APKs signed (RSA 4096, v2 + v3) | ✅ delivered |
| Branch pushed, PR #2 open and mergeable | ✅ |

## 0. Enable the sign-in providers

Firebase Console → **Authentication → Sign-in method** → enable
**Email/Password** and **Google**. A new project has every provider switched
off, and `createUserWithEmailAndPassword` fails with
`auth/operation-not-allowed` until this is done.

If a screen reports a Firebase error, the app now names the cause and appends
the raw Firebase text — for example "Email sign-in is switched off for this
Firebase project · An internal error has occurred. [ OPERATION_NOT_ALLOWED ]".

## 1. Sign the CI builds

CI currently debug-signs the release APKs because the signing secrets are not
set. Run this once, as yourself:

```bash
gh auth login
./scripts/setup-ci-secrets.sh /path/to/sproutly-release.jks
```

It prompts for the keystore password (never stored), registers
`SPROUTLY_KEYSTORE_BASE64`, `SPROUTLY_KEYSTORE_PASSWORD`, `SPROUTLY_KEY_ALIAS`
and `SPROUTLY_KEY_PASSWORD`, and prints the result.

## 2. Firebase: rules and indexes

```bash
firebase login
firebase use tab-gl
firebase deploy --only firestore:rules,firestore:indexes
```

Index builds take a few minutes. Until they finish, list queries surface as
"This list needs a moment to load. Please retry."

## 3. Cloudinary secret

```bash
firebase functions:secrets:set CLOUDINARY_URL
# paste: cloudinary://<api_key>:<api_secret>@<cloud_name>
```

Rotate the key first if it has ever been pasted into chat or a ticket — the one
from the original brief was shared in plain text.

## 4. Deploy the functions

```bash
cd functions && npm ci && npm run build && cd ..
firebase deploy --only functions
```

All functions deploy to `asia-south1`.

To deploy from CI instead, add two secrets and one variable (the workflow
already references them):

```bash
gh secret set GCP_WORKLOAD_IDENTITY_PROVIDER --repo tab-gl/Sproutly
gh secret set GCP_SERVICE_ACCOUNT             --repo tab-gl/Sproutly
gh variable set FIREBASE_PROJECT_ID --repo tab-gl/Sproutly --body tab-gl
```

## 5. Category taxonomy

```bash
cd tools/seed && npm ci
GOOGLE_APPLICATION_CREDENTIALS=./service-account.json node seed.mjs
```

Creates the eight categories the home rails and search filters read. Delete the
service-account key afterwards.

## 6. First administrator

1. Sign up in the app.
2. Firestore → `users/{your-uid}` → set `role` to `ADMIN`.
3. Reopen the app; Profile now shows **Admin console**.

Every later role change goes through Admin → Users, and the client can never
promote itself.

## 7. Google Sign-In fingerprints

Firebase Console → Project settings → Your apps → add both fingerprints from
`sproutly-signing-bundle.zip` (SHA-1 and SHA-256). Release builds cannot sign in
with Google until this is done.

## 8. Merge the branch

PR #2 replaces the Flutter tree on `main` with the native app. Review the diff,
then merge — after that, pushes to `main` run CI and publish signed APKs.

---

## Deliberately not built yet

These are gaps I chose not to fake, all documented in
[ARCHITECTURE.md](ARCHITECTURE.md#known-trade-offs):

- **Reviews and ratings.** `rating` / `ratingCount` exist on products and
  stores, but nothing writes them. A `reviews` subcollection plus an aggregate
  Cloud Function is the next feature; a rating UI backed by numbers that never
  change would be worse than none.
- **Online payments.** Orders are cash-on-delivery. The order document already
  carries a `payment` block, so a gateway (Razorpay / Stripe) slots in without
  reshaping orders.
- **Seller analytics at scale.** Computed client-side from the last 300 orders
  per store. Fine now; a scheduled aggregation into `stores/{id}/stats/daily`
  is the production move.
- **Search relevance.** Prefix scan on `titleLower` — no typo tolerance.
  `CatalogRepository.search` is the seam for Algolia or Typesense.
- **iOS / web clients.** Out of scope; the Firestore rules and functions are
  client-agnostic.
