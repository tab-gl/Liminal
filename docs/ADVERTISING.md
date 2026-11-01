# Advertising

## Lifecycle

```
owner creates  →  pending_approval  →  active  →  exhausted
                         │              │  ↑
                         │              ↓  │
                      rejected       paused (owner or schedule)
```

1. **Create** (`pending_approval`). The owner sets name, application, objective,
   budget, dates, placements, category targeting and a headline. Delivery
   counters start at zero and the rules assert that.
2. **Approve** (`moderateCampaign`). An admin approves; the function sets the
   real status from the schedule - `active` if today is inside the window,
   `paused` if it starts later, `completed` if it already ended. Approval is
   required because a marketplace that serves unvetted creative loses its
   audience.
3. **Deliver** (`serveAds`). Called when a placement renders.
4. **Exhaust**. When `spend` reaches `budget`, the same transaction flips the
   campaign to `exhausted` and it stops being served.

## Placements

| Placement | Surface |
| --- | --- |
| `sponsoredCard` | Sponsored application cards in the feed |
| `featuredHome` | Featured slot on Discover |
| `discoveryRail` | The dedicated "Sponsored" rail |
| `categoryPromotion` | Inside a category listing |
| `featuredLaunch` | Launch-week prominence |

Every rendered placement carries `SponsoredBadge` - the word, not just a colour
or an icon. There is no way to serve a paid listing unlabelled: the client sets
`sponsored` from the campaign, and the card renders the badge whenever that flag
is true.

## Pricing

Billing is **per impression** at a blended CPM, defined once:

```ts
// functions/src/config.ts
cpmPaise: 12_000   // ₹120 per 1000 impressions => ₹0.12 per impression
```

So `spend` is fully derivable: `impressions × (cpmPaise / 1000)`. Clicks are
tracked and reported (CTR is what advertisers actually judge) but cost nothing,
which removes the incentive to fake them. Because cost per impression is
deterministic, a developer can reconcile their dashboard against their invoices
by hand - which is the point.

Budget floor is ₹500 and the ceiling is ₹5,00,000 per campaign
(`LIMITS` in `functions/src/config.ts`, mirrored in
`lib/core/constants/app_constants.dart`).

## Fraud and pacing controls

| Control | Where |
| --- | --- |
| Amount validated against platform limits | `createRazorpayOrder`, `createStripeCheckout` |
| Budget credited only by a signed webhook | `razorpayWebhook`, `stripeWebhook` |
| Idempotent webhook handling | both webhooks check the payment row before crediting |
| One billed impression per user per campaign per hour | deterministic `campaign_events` id in `serveAds` |
| Clicks refused on non-active campaigns | `trackCampaignEvent` |
| Budget exhaustion in the same transaction | `serveAds` batch |
| Under-delivering campaigns prioritised | `serveAds` sorts by spend ratio |
| Daily cap | `LIMITS.dailyCapRatio` |
| Every approval and rejection audited | `admin_audit_logs` |

## Campaign analytics

Available on the campaign screen, all server-authored: impressions, clicks, CTR,
spend, remaining budget, spend ratio, cost per click, status, schedule,
placements and targeting.

## What the client cannot do

- Write `impressions`, `clicks`, `spend`, `status` (other than pause/resume), or
  `budget` after approval. The rules reject all of them.
- Write `campaign_events` at all - the collection has no client write path.
- Change targeting or dates once a campaign is approved; a new campaign is
  required, which keeps the audit trail clean.
- Pay an amount it chose: the order is created server-side from a validated
  amount, and the credit comes from the provider's webhook.

## Operating notes

- Refunds are manual: refund in the provider dashboard, then set the campaign to
  `paused` and adjust `budget` with an admin write. There is deliberately no
  self-service refund path.
- A campaign whose application is suspended should be paused by the moderator in
  the same action; `resolveReport` suspends the listing, and the next `serveAds`
  run will not serve a campaign for a listing that is no longer `approved` if you
  add that check - see the `TODO` in `functions/src/campaigns.ts` if you want to
  enforce it strictly.
