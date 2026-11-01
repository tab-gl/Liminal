import { onCall } from "firebase-functions/v2/https";
import { db, COLLECTIONS } from "./config";
import { requireAdmin } from "./util";

/** Counts for the admin header. Uses aggregation queries, never full scans. */
export const platformStats = onCall(async (request) => {
  await requireAdmin(request);

  const [users, apps, pending, campaigns, openReports] = await Promise.all([
    db.collection(COLLECTIONS.users).count().get(),
    db.collection(COLLECTIONS.applications).where("status", "==", "approved").count().get(),
    db.collection(COLLECTIONS.applications).where("status", "==", "pending_review").count().get(),
    db.collection(COLLECTIONS.campaigns).where("status", "==", "active").count().get(),
    db.collection(COLLECTIONS.reports).where("status", "==", "open").count().get(),
  ]);

  return {
    users: users.data().count,
    liveApplications: apps.data().count,
    pendingApplications: pending.data().count,
    activeCampaigns: campaigns.data().count,
    openReports: openReports.data().count,
  };
});
