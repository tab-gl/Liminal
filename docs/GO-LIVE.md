# Go live

Everything here needs credentials I do not hold (Firebase console, Cloudinary,
GitHub Actions secrets). Each step is a command or a console action, with a way
to verify it worked.

## Release signing fingerprints

Package `app.tab.store`, key alias `sproutly`, valid until 2056-10-24.

```
SHA-1    03:B5:BB:53:E1:6B:1A:77:E5:19:65:05:FA:92:9D:58:0C:95:A5:9E
SHA-256  9F:A9:32:31:8A:17:F6:A8:5A:19:B7:BF:02:B8:7A:F6:B4:32:6E:E5:CD:5A:58:51:48:1F:05:AB:71:16:A2:12
```

Debug builds use `~/.android/debug.keystore`, whose SHA-1 is unique to your
machine — add it too if you want Google Sign-In to work on debug installs:

```bash
keytool -list -v -keystore ~/.android/debug.keystore \
  -alias androiddebugkey -storepass android -keypass android | grep SHA1
```

## Deploying, and proving you deployed the latest

Deploying is half the job; the other half is proving the project is running the
rules and indexes that are actually in this repo. `tools/verify-live.mjs` asks
the live project directly — no credentials, just the public API key from
`google-services.json` — and exits non-zero when something is stale.

```bash
git pull                                  # 1. get the latest branch
git log -1 --oneline                      #    confirm the commit you expect

firebase use tab-gl                       # 2. point at the project
firebase deploy --only firestore:rules,firestore:indexes

cd tools/seed && npm ci                   # 3. categories (upsert, safe to re-run)
GOOGLE_APPLICATION_CREDENTIALS=./service-account.json node seed.mjs && cd ../..

node tools/verify-live.mjs                # 4. prove it
```

Read the verifier's output as a checklist:

| Line | Meaning if ✗ |
| --- | --- |
| `Rules: Sproutly ruleset live` | a different ruleset is deployed — run the deploy step |
| `Rules: private data denied` | profiles are readable by guests; deploy the rules (the `isPublic` default) |
| `Data: categories seeded` | run the seed script |
| `Indexes: composite query` | indexes not deployed, or still building — re-run in a few minutes |
| `Auth: …` | enable the provider in Authentication → Sign-in method |

Indexes build in the background (5–20 minutes for the full set), so the index
line can be ✗ on the first run and ✓ on the second. Everything else should be
green immediately after deploying.

Run it again after any rules change, and treat a red line as "the device is
testing old configuration", not "the app is broken".

## 0. Get the code

The app lives on the `sproutly-native` branch until PR #2 is merged, so either
merge first or check the branch out:

```bash
git clone https://github.com/tab-gl/Sproutly.git
cd Sproutly
git checkout sproutly-native      # skip once PR #2 is merged
```

Everything below runs from that directory. If you would rather not clone, the
only step that truly needs files is #1 (rules + indexes) — you can download
`firestore.rules` and `firestore.indexes.json` from the branch and deploy them
from an empty folder containing a `firebase.json` that points at those two
files. Cloning is simpler and you need it again for #3 and #4.

## 1. Deploy Firestore rules and indexes

Nothing in the app can read or write until this runs — the project is still on
the default deny-all rules.

```bash
npm install -g firebase-tools
firebase login
firebase use tab-gl
firebase deploy --only firestore:rules,firestore:indexes
```

Verify: Firebase Console → Firestore → **Rules** shows your rules with a recent
timestamp, and **Indexes** lists 23 entries building to "Enabled" (5–20 minutes).

While indexes build, list queries fail with "This list needs a moment to load."

## 2. Sign in

The account `aceyash-dev@proton.me` already exists — the first sign-up attempt
created it before the profile write was denied. Use **Sign in**, not Create
account. On 1.0.2 any new email registers cleanly.

## 3. Cloudinary upload preset (no functions needed)

This app runs on the Spark plan, so uploads use an unsigned preset instead of a
server signature. Cloudinary Console → **Settings → Upload → Upload presets →
Add upload preset**:

| Field | Value |
| --- | --- |
| Preset name | `sproutly_unsigned` |
| Signing mode | **Unsigned** |
| Folder | `sproutly` |
| Max file size | `5 MB` |
| Allowed formats | `jpg,png,webp` |

The cloud name (`wjoegjfk`) and preset name are already baked into the APK, so
nothing needs rebuilding. Full detail and the trade-offs:
[docs/SPARK-MODE.md](SPARK-MODE.md).

Verify: add a product image in the seller hub and check it appears in the
Cloudinary media library under `sproutly/`.

Cloud Functions are optional now. If you move to Blaze later:
`firebase functions:secrets:set CLOUDINARY_URL`, `firebase deploy --only functions`,
and clear `SPROUTLY_CLOUDINARY_UPLOAD_PRESET` so the client signs instead.

## 4. Seed the category taxonomy

```bash
cd tools/seed && npm ci
# Firebase Console → Project settings → Service accounts → Generate new private key
# save the JSON as tools/seed/service-account.json
GOOGLE_APPLICATION_CREDENTIALS=./service-account.json node seed.mjs
rm service-account.json
```

Verify: Firestore → `categories` has 8 documents. Delete the key file.

## 5. Make yourself an administrator

Sign in, then Firestore → `users/{your-uid}` → set `role` to `ADMIN`. The app
reads the role from Firestore live, so Profile → **Admin console** appears
without a restart. Every later role change goes through Admin → Users.

## 6. Register the fingerprints

Firebase Console → Project settings → Your apps → `app.tab.store` → **Add
fingerprint** → paste the SHA-1, then repeat for SHA-256. Google Sign-In fails
in release builds until this is done.

## 7. Sign the CI builds

CI debug-signs release APKs until these four secrets exist:

```bash
./scripts/setup-ci-secrets.sh /path/to/sproutly-release.jks
```

or Settings → Secrets and variables → Actions → New repository secret, four
times: `SPROUTLY_KEYSTORE_BASE64` (base64 of the .jks), `SPROUTLY_KEYSTORE_PASSWORD`,
`SPROUTLY_KEY_ALIAS` (`sproutly`), `SPROUTLY_KEY_PASSWORD`.

Verify: the next "Android CI" run's release step no longer prints
"No release keystore configured".

## 8. Merge PR #2

Replaces the Flutter tree on `main` with this app. After merging, pushes to
`main` build and publish signed APKs.

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
