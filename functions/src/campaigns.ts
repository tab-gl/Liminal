import { onCall, HttpsError } from "firebase-functions/v2/https";
import { db, COLLECTIONS, FieldValue, Timestamp } from "./config";
import { LIMITS } from "./config";
import { audit, optionalString, requireAdmin, requireUser, sendNotification } from "./util";

interface ServedAd {
  campaignId: string;
  applicationId: string;
  headline: string;
}

/**
 * Picks which sponsored placements to serve.
 *
 * Selection, pacing and billing all happen here. The client receives app ids and
 * nothing else, so it cannot inflate its own impressions, and a campaign that has
 * run out of budget simply stops appearing.
 */
export const serveAds = onCall(async (request) => {
  const ctx = await requireUser(request);
  const placement = optionalString(request.data?.placement) ?? "sponsoredCard";
  const limit = Math.min(Math.max(Number(request.data?.limit ?? 3), 1), 5);

  const now = Timestamp.now();
  const active = await db
    .collection(COLLECTIONS.campaigns)
    .where("status", "==", "active")
    .where("endDate", ">", now)
    .limit(40)
    .get();

  type CampaignDoc = Record<string, any> & { id: string };
  const candidates: CampaignDoc[] = active.docs
    .map((doc) => ({ id: doc.id, ...(doc.data() as Record<string, any>) }) as CampaignDoc)
    .filter((c) => {
      const placements: string[] = c.placements ?? [];
      if (!placements.includes(placement)) return false;
      const start = c.startDate?.toDate?.() ?? new Date(0);
      if (start > new Date()) return false;
      const remaining = Number(c.budget ?? 0) - Number(c.spend ?? 0);
      if (remaining <= 0) return false;
      const categories: string[] = c.targeting?.categoryIds ?? [];
      // Empty targeting means "everyone".
      return categories.length === 0 || categories.length > 0;
    })
    // Under-delivering campaigns go first so budgets are spent evenly.
    .sort((a, b) => {
      const aRatio = Number(a.spend ?? 0) / Math.max(Number(a.budget ?? 1), 1);
      const bRatio = Number(b.spend ?? 0) / Math.max(Number(b.budget ?? 1), 1);
      return aRatio - bRatio;
    })
    .slice(0, limit);

  if (candidates.length === 0) {
    return { applicationIds: [], campaigns: [] };
  }

  const served: ServedAd[] = [];
  const batch = db.batch();

  for (const campaign of candidates) {
    const campaignRef = db.collection(COLLECTIONS.campaigns).doc(campaign.id);

    // One impression per user per campaign per hour, recorded as a document so
    // a refresh loop cannot burn a budget.
    const hourBucket = new Date().toISOString().slice(0, 13);
    const dedupeRef = db
      .collection(COLLECTIONS.campaignEvents)
      .doc(`${campaign.id}_${ctx.uid}_${hourBucket}`);

    const existing = await dedupeRef.get();
    if (existing.exists) continue;

    const costPaise = Math.round(LIMITS.cpmPaise / 1000);
    const remaining = Number(campaign.budget ?? 0) - Number(campaign.spend ?? 0);
    if (remaining < costPaise) continue;

    batch.set(dedupeRef, {
      campaignId: campaign.id,
      ownerId: campaign.ownerId,
      applicationId: campaign.applicationId,
      userId: ctx.uid,
      kind: "impression",
      costPaise,
      createdAt: FieldValue.serverTimestamp(),
    });

    batch.update(campaignRef, {
      impressions: FieldValue.increment(1),
      spend: FieldValue.increment(costPaise),
      updatedAt: FieldValue.serverTimestamp(),
    });

    const exhausted = remaining - costPaise <= 0;
    if (exhausted) {
      batch.update(campaignRef, { status: "exhausted" });
    }

    served.push({
      campaignId: campaign.id,
      applicationId: campaign.applicationId,
      headline: campaign.creative?.headline ?? "",
    });
  }

  if (served.length === 0) return { applicationIds: [], campaigns: [] };

  await batch.commit();

  return {
    applicationIds: served.map((ad) => ad.applicationId),
    campaigns: served,
  };
});

/** Clicks. Impressions are counted at serve time; clicks are counted here. */
export const trackCampaignEvent = onCall(async (request) => {
  const ctx = await requireUser(request);
  const campaignId = request.data?.campaignId;
  const kind = request.data?.kind;
  if (typeof campaignId !== "string" || !["click", "impression"].includes(kind)) {
    throw new HttpsError("invalid-argument", "campaignId and a valid kind are required.");
  }

  const campaignRef = db.collection(COLLECTIONS.campaigns).doc(campaignId);
  const snap = await campaignRef.get();
  if (!snap.exists) throw new HttpsError("not-found", "Unknown campaign.");
  const campaign = snap.data()!;

  if (campaign.status !== "active") {
    // Silently accept clicks on campaigns that just stopped, without billing.
    return { ok: true, counted: false };
  }

  if (kind === "click") {
    await db.collection(COLLECTIONS.campaignEvents).add({
      campaignId,
      ownerId: campaign.ownerId,
      applicationId: campaign.applicationId,
      userId: ctx.uid,
      kind: "click",
      costPaise: 0,
      createdAt: FieldValue.serverTimestamp(),
    });
    await campaignRef.update({
      clicks: FieldValue.increment(1),
      updatedAt: FieldValue.serverTimestamp(),
    });
  }

  return { ok: true, counted: true };
});

/** Approval gate for new campaigns. */
export const moderateCampaign = onCall(async (request) => {
  const admin = await requireAdmin(request);
  const campaignId = request.data?.campaignId;
  const approve = request.data?.approve === true;
  const reason = optionalString(request.data?.reason, 600);

  if (typeof campaignId !== "string") {
    throw new HttpsError("invalid-argument", "campaignId is required.");
  }
  if (!approve && !reason) {
    throw new HttpsError("invalid-argument", "A reason is required when rejecting a campaign.");
  }

  const ref = db.collection(COLLECTIONS.campaigns).doc(campaignId);
  const snap = await ref.get();
  if (!snap.exists) throw new HttpsError("not-found", "Unknown campaign.");
  const campaign = snap.data()!;

  const start = campaign.startDate?.toDate?.() ?? new Date();
  const end = campaign.endDate?.toDate?.() ?? new Date();
  const now = new Date();
  const status = approve ? (now > end ? "completed" : now < start ? "paused" : "active") : "rejected";

  await ref.update({
    status,
    approvedAt: approve ? FieldValue.serverTimestamp() : null,
    approvedBy: approve ? admin.uid : null,
    rejectionReason: approve ? null : reason,
    updatedAt: FieldValue.serverTimestamp(),
  });

  await sendNotification({
    userId: campaign.ownerId,
    type: approve ? "campaignApproved" : "campaignRejected",
    title: approve ? "Campaign approved" : "Campaign needs changes",
    body: approve
      ? `${campaign.name} is ${status === "paused" ? "scheduled" : "live"}.`
      : reason ?? "A moderator asked for changes to this campaign.",
    route: `/campaign/${campaignId}`,
    data: { campaignId },
  });

  await audit(admin.uid, approve ? "approve_campaign" : "reject_campaign", campaignId, { reason });
  return { ok: true, status };
});
