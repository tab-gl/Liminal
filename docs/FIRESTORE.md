# Data model

All timestamps are Firestore server timestamps unless noted. All money fields
are integers in **paise**.

## Collections

### `users/{uid}`
| Field | Type | Notes |
| --- | --- | --- |
| `uid` | string | same as document id |
| `email` | string | |
| `displayName` | string | 1–80 chars |
| `photoUrl` | string? | Cloudinary secure URL |
| `photoPublicId` | string? | for deletion |
| `phone` | string? | |
| `role` | string | `CUSTOMER` \| `SELLER` \| `ADMIN` — function-written only |
| `storeId` | string? | set by `registerSeller` |
| `createdAt` | timestamp | |

Subcollection `tokens/{fcmToken}` — device push tokens, owner-only.

### `stores/{storeId}`
`ownerId`, `name`, `slug`, `tagline`, `description`, `logoUrl`, `logoPublicId`,
`bannerUrl`, `bannerPublicId`, `city`, `status` (`PENDING` \| `ACTIVE` \|
`SUSPENDED`), `rating`, `ratingCount`, `productCount`, `createdAt`.

### `products/{productId}`
`storeId`, `storeName`, `title`, `titleLower`, `description`, `categoryId`,
`categoryLabel`, `images[] { url, publicId }`, `price`, `compareAtPrice`,
`currency`, `stock`, `status` (`DRAFT` \| `ACTIVE` \| `ARCHIVED` \| `BLOCKED`),
`rating`, `ratingCount`, `unitsSold`, `tags[]`, `featured`, `reported`,
`createdAt`, `updatedAt`.

`titleLower` exists purely so search can do a prefix range scan.

### `categories/{categoryId}`
`name`, `slug`, `iconKey`, `order`, `productCount`, `imageUrl`.

### `carts/{uid}/lines/{productId}`
Denormalised snapshot for a fast cart render: `productId`, `storeId`,
`storeName`, `title`, `imageUrl`, `unitPrice`, `quantity`, `stock`, `status`,
`addedAt`. The server re-reads the real product before charging anyone.

### `wishlists/{uid}/items/{productId}`
Same snapshot shape as a cart line, minus quantity.

### `addresses/{uid}/items/{addressId}`
`label`, `fullName`, `phone`, `line1`, `line2`, `city`, `state`, `pincode`,
`isDefault`.

### `orders/{orderId}`
`buyerId`, `buyerName`, `items[] { productId, title, imageUrl, unitPrice,
quantity, storeId, storeName }`, `subtotal`, `shipping`, `total`, `currency`,
`status`, `note`, `address { …snapshot… }`, `storeIds[]`, `placedAt`,
`updatedAt`, `timeline[] { status, at, note }`, `payment { method, status }`,
`cancelledBy`.

`storeIds` is an array so a seller can query their own orders with
`whereArrayContains`.

### `notifications/{uid}/items/{itemId}`
`title`, `body`, `type` (`ORDER` \| `STORE` \| `SYSTEM` \| `PROMO`), `read`,
`deepLink`, `createdAt`. Clients may only flip `read`.

### `platform/stats`
Counters maintained by `refreshPlatformStats` (every 15 min) and incremented by
`createOrder`: `userCount`, `sellerCount`, `storeCount`, `pendingStores`,
`productCount`, `blockedProducts`, `orderCount`, `gmv`.

## Access summary

| Collection | Read | Write |
| --- | --- | --- |
| `users` | self, admin | self (profile fields only), admin |
| `stores` | everyone | owner (profile fields), functions, admin |
| `products` | `ACTIVE` public; owner/admin all | owner store (validated), admin |
| `categories` | everyone | admin |
| `carts`, `wishlists`, `addresses` | self | self |
| `orders` | buyer, seller of record, admin | **functions only** |
| `notifications` | self | self (`read` only) |
| `platform` | signed in | functions only |

## Indexes

23 composite indexes ship in `firestore.indexes.json`. The non-obvious ones:

- `products(status, titleLower, price)` ×2 (asc/desc) — search with sort
- `products(status, categoryId, unitsSold)` — category rails
- `products(storeId, updatedAt desc)` — seller product list
- `orders(storeIds CONTAINS, placedAt desc)` — seller order feed
- `stores(status, createdAt)` — admin approval queue

Deploy with `firebase deploy --only firestore:indexes`.
