# Callable functions

All functions run in `asia-south1`. Every one requires authentication unless
noted, and every privileged one re-checks the caller's role from the auth token.

| Function | Auth | Arguments | Returns |
| --- | --- | --- | --- |
| `submitApplication` | owner | `applicationId` | `{ ok, status }` |
| `moderateApplication` | admin | `applicationId`, `approve`, `reason?` | `{ ok, status }` |
| `respondToReview` | app owner | `reviewId`, `body` | `{ ok }` |
| `serveAds` | signed in | `placement`, `limit?` | `{ applicationIds[], campaigns[] }` |
| `trackCampaignEvent` | signed in | `campaignId`, `kind` (`click`\|`impression`) | `{ ok, counted }` |
| `moderateCampaign` | admin | `campaignId`, `approve`, `reason?` | `{ ok, status }` |
| `createRazorpayOrder` | campaign owner | `campaignId`, `amountPaise` | `{ orderId, amountPaise, currency }` |
| `createStripeCheckout` | campaign owner | `campaignId`, `amountPaise` | `{ url, sessionId }` |
| `setUserRole` | admin | `uid`, `role` | `{ ok, role }` |
| `requestDeveloperAccess` | signed in | `motivation` | `{ ok }` |
| `deleteAccount` | signed in | - | `{ ok }` |
| `resolveReport` | admin | `reportId`, `uphold`, `note?` | `{ ok }` |
| `platformStats` | admin | - | counts |
| `razorpayWebhook` | signature | raw body | `200` |
| `stripeWebhook` | signature | raw body | `200` |

## Triggers

| Trigger | Event | Effect |
| --- | --- | --- |
| `onReviewWritten` | `reviews/{id}` write | Recomputes `rating` + `reviewCount`; notifies the developer on a new review |
| `onBookmarkWritten` | `bookmarks/{id}` write | Maintains `bookmarkCount` |
| `onFollowWritten` | `developer_follows/{id}` write | Maintains `followerCount` |
| `onNotificationCreated` | `notifications/{id}` create | Sends the FCM push; drops stale tokens |
| `aggregateApplicationEvents` | every 60 min | Folds raw events into counters and daily analytics, decays stale trending scores |

## Client usage

Everything goes through `CallableClient` (`lib/firebase/callable_client.dart`),
which normalises errors into `AppException`:

```dart
final Map<String, dynamic> result = await ref
    .read(callableProvider)
    .call('moderateApplication', <String, dynamic>{
      'applicationId': id,
      'approve': true,
    });
```

Fire-and-forget telemetry uses `beacon()`, which cannot throw into the UI.

## Error codes

| Code | Meaning in the app |
| --- | --- |
| `unauthenticated` | Session expired - route to sign-in |
| `permission-denied` | Wrong role or not the owner |
| `invalid-argument` | Show the message; it is written for humans |
| `failed-precondition` | Wrong state (already in review, campaign not active) |
| `already-exists` | Duplicate name, duplicate verification request |
| `resource-exhausted` | Rate limited - back off |
| `unavailable` / `deadline-exceeded` | Retryable network failure |
