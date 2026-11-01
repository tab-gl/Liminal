import { onCall, HttpsError } from "firebase-functions/v2/https";
import { db, COLLECTIONS, FieldValue, Timestamp } from "./config";
import { LIMITS } from "./config";
import { remainingPaise } from "./billing";
import { buildAdContext, matchesTargeting } from "./targeting";
import { optionalString, requireAdmin, requireUser, sendNotification } from "./util";

interface ServedAd {
  campaignId: string;
  applicationId: string;
  headline: string;
}

function hourBucket(): string {
  return new Date().toISOString().slice(0, 13);
}

/**
 * Picks which sponsored placements to serve.
 *
 * Selection, pacing and billing all happen here. The client receives app ids and
 * nothing else, so it cannot inflate its own impressions. Every charge is made
 * inside a transaction that re-reads the campaign, so concurrent requests cannot
 * both spend the same remaining balance or double-count one impression.
 */
export const serveAds = onCall(async (request) => {
  const ctx = await requireUser(request);
  const placement = optionalString(request.data?.placement) ?? "sponsoredCard";
  const limit = Math.min(Math.max(Number(request.data?.limit ?? 3), 1), 5);
  const adContext = buildAdContext(request.data, request.rawRequest?.headers as Record<string, unknown>);

  const now = Timestamp.now();
  const active = await db
    .collection(COLLECTIONS.campaigns)
    .where("status", "==", "active")
    .where("endDate", ">", now)
    .limit(40)
    .get();

  type CampaignDoc = Record<string, any> & { id: string };
  const costPaise = Math.round(LIMITS.cpmPaise / 1000);

  const candidates: CampaignDoc[] = active.docs
    .map((doc) => ({ id: doc.id, ...(doc.data() as Record<string, any>) }) as CampaignDoc)
    .filter((c) => {
      // Never bill an advertiser for their own browsing.
      if (c.ownerId === ctx.uid) return false;
      const placements: string[] = c.placements ?? [];
      if (!placements.includes(placement)) return false;
      const start = c.startDate?.toDate?.() ?? new Date(0);
      if (start > new Date()) return false;
      if (remainingPaise(c) < costPaise) return false;
      return matchesTargeting(c.targeting, adContext);
    })
    // Under-delivering campaigns go first so budgets are spent evenly.
    .sort((a, b) => {
      const aRatio = Number(a.spend ?? 0) / Math.max(Number(a.budget ?? 1), 1);
      const bRatio = Number(b.spend ?? 0) / Math.max(Number(b.budget ?? 1), 1);
      return aRatio - bRatio;
    })
    .slice(0, limit);

  const served: ServedAd[] = [];
  const bucket = hourBucket();

  for (const candidate of candidates) {
    const campaignRef = db.collection(COLLECTIONS.campaigns).doc(candidate.id);
    // One impression per user per campaign per hour.
    const dedupeRef = db
      .collection(COLLECTIONS.campaignEvents)
      .doc(`${candidate.id}_${ctx.uid}_${bucket}`);

    const result = await db.runTransaction(async (tx) => {
      const [campaignSnap, dedupeSnap] = await Promise.all([tx.get(campaignRef), tx.get(dedupeRef)]);
      if (!campaignSnap.exists || dedupeSnap.exists) return null;
      const campaign = campaignSnap.data()!;

      if (campaign.status !== "active") return null;
      const end = campaign.endDate?.toDate?.();
      if (end && end <= new Date()) return null;
      const remaining = remainingPaise(campaign);
      if (remaining < costPaise) return null;

      tx.create(dedupeRef, {
        campaignId: candidate.id,
        ownerId: campaign.ownerId,
        applicationId: campaign.applicationId,
        userId: ctx.uid,
        kind: "impression",
        costPaise,
        createdAt: FieldValue.serverTimestamp(),
      });

      const update: Record<string, unknown> = {
        impressions: FieldValue.increment(1),
        spend: FieldValue.increment(costPaise),
        updatedAt: FieldValue.serverTimestamp(),
      };
      // Cannot afford another impression: stop delivering.
      if (remaining - costPaise < costPaise) update.status = "exhausted";
      tx.update(campaignRef, update);

      return {
        campaignId: candidate.id,
        applicationId: campaign.applicationId as string,
        headline: (campaign.creative?.headline as string | undefined) ?? "",
      };
    });

    if (result) served.push(result);
  }

  return {
    applicationIds: served.map((ad) => ad.applicationId),
    campaigns: served,
  };
});

/**
 * Clicks. Impressions are counted at serve time; clicks are counted here.
 *
 * A click is only counted when this user was actually served the campaign
 * within the last day, and at most once per user per campaign per hour. The
 * advertiser's own clicks are ignored.
 */
export const trackCampaignEvent = onCall(async (request) => {
  const ctx = await requireUser(request);
  const campaignId = request.data?.campaignId;
  const kind = request.data?.kind;
  if (typeof campaignId !== "string" || !["click", "impression"].includes(kind)) {
    throw new HttpsError("invalid-argument", "campaignId and a valid kind are required.");
  }

  // Impressions are only ever recorded by serveAds.
  if (kind !== "click") return { ok: true, counted: false };

  const campaignRef = db.collection(COLLECTIONS.campaigns).doc(campaignId);
  const snap = await campaignRef.get();
  if (!snap.exists) throw new HttpsError("not-found", "Unknown campaign.");
  const campaign = snap.data()!;

  if (campaign.status !== "active" || campaign.ownerId === ctx.uid) {
    // Silently accept clicks on campaigns that just stopped, without counting.
    return { ok: true, counted: false };
  }

  const served = await db
    .collection(COLLECTIONS.campaignEvents)
    .where("campaignId", "==", campaignId)
    .where("userId", "==", ctx.uid)
    .where("kind", "==", "impression")
    .where("createdAt", ">", Timestamp.fromMillis(Date.now() - 24 * 3600 * 1000))
    .limit(1)
    .get();
  if (served.empty) return { ok: true, counted: false };

  const dedupeRef = db
    .collection(COLLECTIONS.campaignEvents)
    .doc(`${campaignId}_${ctx.uid}_click_${hourBucket()}`);

  const counted = await db.runTransaction(async (tx) => {
    const dedupeSnap = await tx.get(dedupeRef);
    if (dedupeSnap.exists) return false;
    tx.create(dedupeRef, {
      campaignId,
      ownerId: campaign.ownerId,
      applicationId: campaign.applicationId,
      userId: ctx.uid,
      kind: "click",
      costPaise: 0,
      createdAt: FieldValue.serverTimestamp(),
    });
    tx.update(campaignRef, {
      clicks: FieldValue.increment(1),
      updatedAt: FieldValue.serverTimestamp(),
    });
    return true;
  });

  return { ok: true, counted };
});

/** Approval gate for new campaigns. A campaign cannot go live without funds. */
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
  const { campaign, status } = await db.runTransaction(async (tx) => {
    const snap = await tx.get(ref);
    if (!snap.exists) throw new HttpsError("not-found", "Unknown campaign.");
    const data = snap.data()!;
    if (data.status !== "pending_approval") {
      throw new HttpsError("failed-precondition", "Only campaigns awaiting approval can be moderated.");
    }
    if (approve && remainingPaise(data) <= 0) {
      throw new HttpsError("failed-precondition", "This campaign has no confirmed payment yet.");
    }

    const start = data.startDate?.toDate?.() ?? new Date();
    const end = data.endDate?.toDate?.() ?? new Date();
    const now = new Date();
    const next = approve ? (now > end ? "completed" : now < start ? "paused" : "active") : "rejected";

    tx.update(ref, {
      status: next,
      approvedAt: approve ? FieldValue.serverTimestamp() : null,
      approvedBy: approve ? admin.uid : null,
      rejectionReason: approve ? null : reason,
      updatedAt: FieldValue.serverTimestamp(),
    });
    tx.create(db.collection(COLLECTIONS.audit).doc(), {
      actorUid: admin.uid,
      action: approve ? "approve_campaign" : "reject_campaign",
      target: campaignId,
      detail: { reason: reason ?? null },
      createdAt: FieldValue.serverTimestamp(),
    });
    return { campaign: data, status: next };
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

  return { ok: true, status };
});
