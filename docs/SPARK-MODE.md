# Running on the Spark plan (no Cloud Functions)

The original design put every trusted operation in Cloud Functions. Cloud
Functions require the **Blaze** plan, so this app now runs on **Spark** by
moving that trust into two places instead:

1. **Cloudinary unsigned upload presets** — uploads without a server signature.
2. **Firestore transactions validated by security rules** — orders, stock,
   roles, store onboarding and moderation.

Nothing else in the app changed shape: the same repositories, the same screens,
the same Firestore schema.

## What moved where

| Operation | Before (Blaze) | Now (Spark) |
| --- | --- | --- |
| Place order | `createOrder` function | Firestore transaction in `OrderRepositoryImpl`, validated by `match /orders` |
| Cancel / advance order | `cancelOrder`, `updateOrderStatus` | Transaction + status machine in rules |
| Stock movement | inside the order function | same transaction; rules allow only `stock`/`unitsSold` to move, and never below zero |
| Seller onboarding | `registerSeller` | batch write (store + user), rules verify ownership and the CUSTOMER → SELLER move |
| Role changes | `setUserRole` | admin-only rule on `users/{uid}` |
| Moderation | `adminSetStoreStatus`, `adminSetProductStatus` | admin-only rules |
| Image upload | `getCloudinaryUploadSignature` | Cloudinary **unsigned preset** |
| Image deletion | `deleteCloudinaryAsset` | not possible without the api_secret — see below |
| Seller "new order" notice | FCM trigger | in-app notification document written by the buyer |
| Platform counters | scheduled function | `count()` aggregations from the admin dashboard |

The Cloud Functions source stays in `functions/`. Deploy it and clear the
preset when you move to Blaze; the client switches back to signed uploads
automatically.

## 1. Create the unsigned upload preset

Cloudinary Console → **Settings → Upload → Upload presets → Add upload preset**:

| Field | Value |
| --- | --- |
| Preset name | `sproutly_unsigned` (must match `CLOUDINARY_UPLOAD_PRESET`) |
| Signing mode | **Unsigned** |
| Folder | `sproutly` |
| Max file size | `5 MB` |
| Allowed formats | `jpg,png,webp` |

The app ships with cloud name `wjoegjfk` and preset `sproutly_unsigned` baked
in, so it works with no rebuild. To change either, set
`SPROUTLY_CLOUDINARY_CLOUD_NAME` / `SPROUTLY_CLOUDINARY_UPLOAD_PRESET` (or
`cloudinaryCloudName` / `cloudinaryUploadPreset` in `keystore.properties`)
before building.

An unsigned preset means anyone who knows its name can upload into that folder.
Locking it to a folder and a size cap is what keeps that harmless.

## 2. Deploy the rules

The rules are the security boundary now, so they must be deployed:

```bash
firebase deploy --only firestore:rules,firestore:indexes
```

## Trade-offs you should know about

- **Stock is not bound to a specific order.** A signed-in shopper may change
  `stock`/`unitsSold` on an ACTIVE product (that is how the order transaction
  decrements them). Overselling is impossible and prices cannot be altered, but
  a malicious client could zero out a product's stock. Closing this fully needs
  a server — which is exactly what Cloud Functions were for.
- **Deleting a product leaves its Cloudinary images.** Destroying an asset needs
  the api_secret. Remove orphans in the Cloudinary console, or deploy
  `deleteProduct` once you are on Blaze.
- **Order notices are in-app only.** No FCM push without a server. The rule
  scopes client-written notices to `type == 'ORDER'` with a bounded body, but a
  determined user could still post notices to another user's inbox.
- **Admin GMV is a sample.** It sums the most recent 300 orders rather than the
  whole history, because summing everything would burn the free read quota.

## Quotas worth watching (Spark)

| Resource | Free per day |
| --- | --- |
| Firestore reads | 50,000 |
| Firestore writes | 20,000 |
| Firestore deletes | 20,000 |
| Cloudinary transformations | 25 credits/month |

The home screen opens several live listeners; each document change counts as a
read. Fine for development and small betas, not for a launch.
