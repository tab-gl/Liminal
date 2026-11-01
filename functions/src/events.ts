import { onCall, HttpsError } from "firebase-functions/v2/https";
import { db, COLLECTIONS, FieldValue } from "./config";
import { dayKey, requireUser } from "./util";

const TYPES = ["view", "click", "download", "share"] as const;

/**
 * Ingests a product event (view / click / download / share).
 *
 * Replaces direct client writes to `application_events`, which let any signed-in
 * user forge events for any app. Here the application must exist and be
 * approved, an owner's own activity is ignored, and each (user, app, type) is
 * counted once per bucket - a day for views, an hour for everything else - via a
 * marker document written atomically with the event.
 */
export const recordApplicationEvent = onCall(async (request) => {
  const ctx = await requireUser(request);
  const applicationId = request.data?.applicationId;
  const type = request.data?.type;
  if (typeof applicationId !== "string" || applicationId.length === 0 || applicationId.length > 128) {
    throw new HttpsError("invalid-argument", "applicationId is required.");
  }
  if (!TYPES.includes(type)) {
    throw new HttpsError("invalid-argument", "Unknown event type.");
  }

  const appSnap = await db.collection(COLLECTIONS.applications).doc(applicationId).get();
  const app = appSnap.data();
  if (!appSnap.exists || !app || app.status !== "approved" || app.ownerId === ctx.uid) {
    return { ok: true, counted: false };
  }

  const now = new Date();
  const bucket = type === "view" ? dayKey(now) : now.toISOString().slice(0, 13);
  const markerRef = db
    .collection(COLLECTIONS.eventMarkers)
    .doc(`${ctx.uid}_${applicationId}_${type}_${bucket}`);
  const eventRef = db.collection(COLLECTIONS.events).doc();

  try {
    const batch = db.batch();
    batch.create(markerRef, { createdAt: FieldValue.serverTimestamp() });
    batch.create(eventRef, {
      applicationId,
      userId: ctx.uid,
      type,
      platform: typeof request.data?.platform === "string" ? request.data.platform.slice(0, 16) : "unknown",
      createdAt: FieldValue.serverTimestamp(),
    });
    await batch.commit();
    return { ok: true, counted: true };
  } catch (error) {
    // gRPC ALREADY_EXISTS: this user already produced this event in this bucket.
    if ((error as { code?: number }).code === 6) return { ok: true, counted: false };
    throw error;
  }
});
