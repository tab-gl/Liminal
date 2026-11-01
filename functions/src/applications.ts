import { onCall, HttpsError } from "firebase-functions/v2/https";
import { db, COLLECTIONS, FieldValue } from "./config";
import { LIMITS } from "./config";
import { audit, optionalString, requireAdmin, requireUser, sendNotification } from "./util";

const REQUIRED_HTTPS = ["websiteUrl", "launchUrl", "downloadUrl", "documentationUrl", "privacyPolicyUrl", "termsUrl"];

/**
 * Moves a draft into the moderation queue.
 *
 * Runs the automated pre-checks the client cannot be trusted to run: ownership,
 * required fields, link shape, screenshot count, and a duplicate-name check.
 */
export const submitApplication = onCall(async (request) => {
  const ctx = await requireUser(request);
  const applicationId = request.data?.applicationId;
  if (typeof applicationId !== "string") {
    throw new HttpsError("invalid-argument", "applicationId is required.");
  }

  const ref = db.collection(COLLECTIONS.applications).doc(applicationId);
  const snap = await ref.get();
  if (!snap.exists) throw new HttpsError("not-found", "That listing does not exist.");
  const app = snap.data()!;

  if (app.ownerId !== ctx.uid) {
    throw new HttpsError("permission-denied", "You can only submit your own listing.");
  }
  if (!["draft", "rejected"].includes(app.status)) {
    throw new HttpsError("failed-precondition", "This listing is already in review.");
  }

  // Required: name, category, one link. Optional: tagline, description,
  // screenshots, features, docs and legal links. Optional fields are still
  // validated when present, so a listing cannot ship a 10-character description
  // or a twelve-screenshot gallery.
  const problems: string[] = [];
  if (!app.name || String(app.name).trim().length < 2 || app.name.length > LIMITS.maxNameLength) {
    problems.push("App name must be 2-80 characters.");
  }
  if (typeof app.tagline === "string" && app.tagline.length > LIMITS.maxTaglineLength) {
    problems.push("Tagline is too long.");
  }
  if (typeof app.description === "string" && app.description.trim().length > 0 &&
      app.description.trim().length < LIMITS.minDescriptionLength) {
    problems.push(`Description must be at least ${LIMITS.minDescriptionLength} characters if you add one.`);
  }
  if (Array.isArray(app.screenshots) && app.screenshots.length > LIMITS.maxScreenshots) {
    problems.push("Too many screenshots.");
  }
  if (!app.categoryId) problems.push("A category is required.");
  if (!app.iconUrl) problems.push("An app icon is required.");
  if (!Array.isArray(app.platforms) || app.platforms.length === 0) {
    problems.push("Select at least one platform.");
  }
  if (!app.downloadUrl && !app.launchUrl && !app.websiteUrl) problems.push("Add at least one link.");
  for (const field of REQUIRED_HTTPS) {
    const value = app[field];
    if (typeof value === "string" && value.length > 0 && !value.startsWith("https://")) {
      problems.push(`${field} must be an https:// link.`);
    }
  }

  if (problems.length > 0) {
    throw new HttpsError("invalid-argument", problems.join(" "));
  }

  const duplicate = await db
    .collection(COLLECTIONS.applications)
    .where("search.title", "==", String(app.name).trim().toLowerCase())
    .where("status", "in", ["approved", "pending_review"])
    .limit(3)
    .get();
  const clash = duplicate.docs.find((doc) => doc.id !== applicationId);
  if (clash) {
    throw new HttpsError("already-exists", "A listing with that name is already published or in review.");
  }

  await ref.update({
    // The client computes `search`; never let a forged title steer the duplicate check.
    "search.title": String(app.name).trim().toLowerCase(),
    status: "pending_review",
    moderation: {
      state: "queued",
      submittedAt: FieldValue.serverTimestamp(),
      reviewerId: null,
      note: null,
    },
    updatedAt: FieldValue.serverTimestamp(),
  });

  await audit(ctx.uid, "submit_application", applicationId);

  const admins = await db.collection(COLLECTIONS.users).where("role", "==", "admin").limit(20).get();
  await Promise.all(
    admins.docs.map((admin) =>
      db.collection(COLLECTIONS.notifications).add({
        userId: admin.id,
        type: "system",
        title: "New listing to review",
        body: `${app.name} by ${app.ownerName ?? "a developer"}`,
        route: "/admin",
        data: { applicationId },
        readAt: null,
        createdAt: FieldValue.serverTimestamp(),
      }),
    ),
  );

  return { ok: true, status: "pending_review" };
});

/** Approve or send back. Approval is the only path to a public listing. */
export const moderateApplication = onCall(async (request) => {
  const admin = await requireAdmin(request);
  const applicationId = request.data?.applicationId;
  const approve = request.data?.approve === true;
  const reason = optionalString(request.data?.reason, 600);

  if (typeof applicationId !== "string") {
    throw new HttpsError("invalid-argument", "applicationId is required.");
  }
  if (!approve && !reason) {
    throw new HttpsError("invalid-argument", "A reason is required when sending a listing back.");
  }

  const ref = db.collection(COLLECTIONS.applications).doc(applicationId);
  const snap = await ref.get();
  if (!snap.exists) throw new HttpsError("not-found", "That listing does not exist.");
  const app = snap.data()!;

  const isFirstApproval = app.status !== "approved";

  await ref.update({
    status: approve ? "approved" : "rejected",
    rejectionReason: approve ? null : reason,
    moderation: {
      state: approve ? "approved" : "rejected",
      reviewerId: admin.uid,
      reviewedAt: FieldValue.serverTimestamp(),
      note: approve ? null : reason,
    },
    publishedAt: isFirstApproval && approve ? FieldValue.serverTimestamp() : app.publishedAt ?? null,
    updatedAt: FieldValue.serverTimestamp(),
  });

  if (isFirstApproval && approve) {
    await db.collection(COLLECTIONS.users).doc(app.ownerId).set(
      { publishedAppCount: FieldValue.increment(1) },
      { merge: true },
    );
  }

  await sendNotification({
    userId: app.ownerId,
    type: approve ? "applicationApproved" : "applicationRejected",
    title: approve ? "Your app is live" : "Changes requested",
    body: approve
      ? `${app.name} is now listed on Sproutly. Share the link and watch the first views arrive.`
      : reason ?? "A moderator asked for changes before this listing can go live.",
    route: `/dashboard/${applicationId}`,
    data: { applicationId },
  });

  await audit(admin.uid, approve ? "approve_application" : "reject_application", applicationId, { reason });
  return { ok: true, status: approve ? "approved" : "rejected" };
});
