# Data model

All timestamps are Firestore `Timestamp` (UTC). All money is an integer number
of **paise**. Document ids that encode two entities are built by helpers in
`lib/firebase/firestore_paths.dart` so the client and the rules cannot disagree.

## `users/{uid}`

| Field | Type | Written by |
| --- | --- | --- |
| `uid`, `displayName`, `photoUrl`, `bio`, `websiteUrl`, `links` | string / map | owner |
| `isPublic`, `onboarded` | bool | owner |
| `role` (`user` \| `developer` \| `admin`) | string | `setUserRole` callable only |
| `isDeveloper` | bool | `setUserRole` callable only |
| `followerCount`, `publishedAppCount`, `reviewCount` | int | Cloud Functions only |
| `createdAt`, `updatedAt` | timestamp | owner |

Subcollection `private/devices` holds the FCM token; `private/verification`
holds a pending developer-badge request. Both are owner-and-admin readable only.

## `applications/{appId}`

Content (owner-writable): `name`, `tagline`, `description`, `categoryId`,
`tags[]`, `iconUrl`, `bannerUrl`, `screenshots[]`, `previewImages[]`,
`websiteUrl`, `launchUrl`, `downloadUrl`, `platforms[]`, `pricingModel`,
`priceLabel`, `isOpenSource`, `features[]`, `documentationUrl`, `supportEmail`,
`privacyPolicyUrl`, `termsUrl`, `ownerId`, `ownerName`, `ownerPhotoUrl`,
`search { title, keywords[] }`.

Server-owned (client writes are rejected): `rating`, `reviewCount`, `viewCount`,
`uniqueViewerCount`, `clickCount`, `downloadCount`, `bookmarkCount`,
`trendingScore`, `sponsored`, `featured`, `moderation { state, reviewerId,
reviewedAt, note }`, `publishedAt`, `rejectionReason`.

`status` moves `draft → pending_review → approved | rejected`, plus `suspended`.
The owner may only perform `draft ⇄ pending_review` and `rejected → draft`;
approval is a moderator action.

Subcollection `analytics/{yyyy-mm-dd}`: `views`, `clicks`, `downloads`,
`uniqueViewers`, `reviews`, `bookmarks` - written by the hourly aggregation job,
read by the dashboard.

## `reviews/{appId}_{uid}`

One review per person per app, enforced by the id and by the rules.
`rating` 1-5, `body` ≤ 2000 chars, `developerResponse { body, respondedAt }`
(written by the `respondToReview` callable), `helpfulCount`, `status`.
A review is rejected by the rules if the reviewer owns the app.

## `bookmarks/{uid}_{appId}`

`userId`, `applicationId`, `applicationName`, `createdAt`. `bookmarkCount` on the
listing is maintained by an `onDocumentWritten` trigger.

## `developer_follows/{followerUid}_{developerUid}`

`followerId`, `developerId`, `createdAt`. Maintains `followerCount`.

## `application_events/{autoId}`

Raw product events: `applicationId`, `userId`, `type`
(`view` \| `unique_view` \| `click` \| `download` \| `share`), `createdAt`.
Append-only by the client, readable by admins only. The hourly job folds them
into counters and daily analytics, then deletes them.

## `advertising_campaigns/{campaignId}`

| Field | Written by |
| --- | --- |
| `name`, `creative`, `status` (pause/resume only) | owner |
| `ownerId`, `applicationId`, `budget`, `objective`, `targeting`, `placements`, `startDate`, `endDate` | owner at creation, locked after approval |
| `impressions`, `clicks`, `spend` | Cloud Functions only |
| `status` transitions to `active`/`rejected`/`exhausted`, `approvedAt`, `approvedBy`, `rejectionReason` | `moderateCampaign` / `serveAds` |

`spend` is charged per impression at a blended CPM (`LIMITS.cpmPaise` in
`functions/src/config.ts`), so budget burn is deterministic and auditable.

## `campaign_events/{campaignId}_{uid}_{yyyy-mm-ddTHH}`

Impression and click ledger. The deterministic id is the impression de-duplicator:
one impression per user per campaign per hour, which is what stops a refresh loop
from draining a budget. Not writable by clients at all.

## `notifications/{autoId}`

`userId`, `type`, `title`, `body`, `route`, `data`, `readAt`, `createdAt`.
Created by Cloud Functions, readable by the recipient, and the only field the
recipient may change is `readAt`.

## `reports/{autoId}`

`reporterId`, `target` (`application` \| `review` \| `user` \| `campaign`),
`targetId`, `reason`, `details`, `status`, `resolutionNote`, `resolvedBy`.

## `admin_audit_logs/{autoId}`

`actorUid`, `action`, `target`, `detail`, `createdAt`. Append-only from
functions, readable by admins. Every privileged mutation writes one.

## `payments/{providerOrderId}`

`provider`, `status` (`created` \| `captured`), `userId`, `campaignId`,
`amountPaise`, `currency`, `capturedAt`, provider payment id. Written only by the
payment functions and webhooks.

## `config/{doc}`

`aggregation` (last aggregation run) and app-level configuration documents.
Public read, admin write.

## Indexes

`firestore.indexes.json` contains every composite index used by the client and
the functions. The pairs that matter most:

- `applications`: `status + createdAt`, `status + trendingScore`,
  `status + rating`, `status + viewCount`, `status + categoryId + {createdAt,
  rating, trendingScore}`, `status + platforms[]`, `status + tags[]`,
  `ownerId + updatedAt`.
- `reviews`: `applicationId + status + {createdAt, rating}`, `userId + createdAt`.
- `advertising_campaigns`: `ownerId + createdAt`, `status + endDate`,
  `status + createdAt`.
- `notifications`: `userId + createdAt`, `userId + readAt + createdAt`.
- `application_events`: `applicationId + type + createdAt`.
