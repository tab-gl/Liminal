/**
 * Sproutly Cloud Functions.
 *
 * Every privileged operation lives here so the client can stay thin and the
 * security rules can stay strict. Region: asia-south1 (see config.ts).
 */
export { setUserRole, requestDeveloperAccess, deleteAccount } from "./auth";
export { submitApplication, moderateApplication } from "./applications";
export { serveAds, trackCampaignEvent, moderateCampaign } from "./campaigns";
export {
  createRazorpayOrder,
  razorpayWebhook,
  createStripeCheckout,
  stripeWebhook,
} from "./payments";
export { onReviewWritten, respondToReview } from "./reviews";
export { onNotificationCreated } from "./notifications";
export { aggregateApplicationEvents, onBookmarkWritten, onFollowWritten } from "./analytics";
export { resolveReport } from "./reports";
export { platformStats } from "./stats";
