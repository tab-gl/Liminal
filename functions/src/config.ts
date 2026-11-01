import { initializeApp, getApps } from "firebase-admin/app";
import { getFirestore, FieldValue, Timestamp } from "firebase-admin/firestore";
import { getAuth } from "firebase-admin/auth";
import { setGlobalOptions } from "firebase-functions/v2";

if (getApps().length === 0) {
  initializeApp();
}

/** All functions run close to the primary audience. */
export const REGION = "asia-south1";
setGlobalOptions({ region: REGION, maxInstances: 40 });

export const db = getFirestore();
export const auth = getAuth();
export { FieldValue, Timestamp };

export const COLLECTIONS = {
  users: "users",
  applications: "applications",
  reviews: "reviews",
  bookmarks: "bookmarks",
  follows: "developer_follows",
  events: "application_events",
  eventMarkers: "event_markers",
  campaigns: "advertising_campaigns",
  campaignEvents: "campaign_events",
  notifications: "notifications",
  reports: "reports",
  audit: "admin_audit_logs",
  payments: "payments",
} as const;

/** Business limits. Kept in sync with lib/core/constants/app_constants.dart. */
export const LIMITS = {
  minCampaignBudgetPaise: 50_000,
  maxCampaignBudgetPaise: 50_000_000,
  maxNameLength: 80,
  maxTaglineLength: 140,
  minDescriptionLength: 60,
  maxScreenshots: 8,
  maxReviewLength: 2000,
  /** Blended cost per thousand impressions, in paise. */
  cpmPaise: 12_000,
  /** Hard daily delivery cap as a fraction of the remaining budget. */
  dailyCapRatio: 0.2,
} as const;

export class HttpsErrorLike extends Error {
  constructor(public code: string, message: string) {
    super(message);
  }
}
