# Testing

## What exists

| Suite | Location | Needs |
| --- | --- | --- |
| Unit: validators, currency, pagination | `test/core/` | nothing |
| Unit: models and wire formats | `test/models/` | nothing |
| Widget: application card | `test/widgets/` | nothing |
| Security rules | `test/rules/` | Firestore + Storage emulators |
| Functions | `functions/` | `tsc --noEmit` in CI; add emulator tests as needed |

```bash
flutter test                       # unit + widget
flutter analyze                    # lints (strict-casts, strict-inference)

cd functions && npx tsc --noEmit   # type check

cd test/rules && npm install
npx firebase emulators:exec --only firestore "node --test"
```

## Why the rules suite is the important one

Client tests prove the app does what it says. Rule tests prove the app *cannot*
do what it should not. `test/rules/firestore.rules.test.js` asserts the negative
cases that matter:

- a stranger cannot create or edit someone else's listing;
- an owner cannot seed `rating`, `viewCount` or any other counter;
- an owner cannot move a listing to `approved`;
- a bookmark cannot be written under another user's id;
- a user cannot promote themselves to admin;
- `campaign_events` and `admin_audit_logs` have no client write path;
- notifications are readable only by their recipient;
- anonymous users can read categories and nothing else.

A rule that allows everything passes a happy-path test and fails these.

## Coverage strategy

Not "100% coverage" - coverage of the decisions that can lose money or trust:

1. **Money**: amount validation, budget credit only via webhook, exhaustion.
2. **Moderation**: only an admin can approve; only the allowed status edges.
3. **Aggregates**: rating recomputed from published reviews, never incremented.
4. **Ownership**: composite ids for bookmarks and follows; owner checks in
   `respondToReview` and `deleteAccount`.
5. **Error surfaces**: every screen has loading, empty and error states, and
   retry appears only for retryable errors.

## Manual test pass before release

1. Sign up → onboarding → Discover; confirm the verification email arrives.
2. Publish a draft with an icon and two screenshots; save, close, reopen from
   Publish tab; submit. Confirm the moderator notification.
3. As an admin, approve it; confirm the developer notification and that the
   listing appears in Discover within a minute.
4. Bookmark it from a second account; confirm `bookmarkCount` increments once.
5. Post a review; confirm `rating` and `reviewCount` update and the developer is
   notified. Try reviewing your own app - it must be refused.
6. Create a campaign, approve it as admin, reload Discover; confirm the sponsored
   rail shows it labelled Sponsored. Refresh repeatedly - impressions must not
   climb more than once per hour.
7. Pay for budget in test mode; confirm budget rises only after the webhook
   fires, not on the client callback.
8. Toggle dark mode, reduced motion and 1.5x text; confirm nothing clips.
9. Kill the network mid-scroll; confirm cached content renders and the error
   state offers retry.
