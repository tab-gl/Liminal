import { onCall, HttpsError } from "firebase-functions/v2/https";
import { onDocumentWritten } from "firebase-functions/v2/firestore";
import { AggregateField } from "firebase-admin/firestore";
import { db, COLLECTIONS, FieldValue } from "./config";
import { requireUser, sendNotification } from "./util";

/**
 * Keeps the aggregate rating and review count on the application honest.
 *
 * Recomputed from the published reviews rather than incremented, so a deleted
 * or edited review cannot leave a stale average behind.
 */
export const onReviewWritten = onDocumentWritten("reviews/{reviewId}", async (event) => {
  const after = event.data?.after?.data();
  const before = event.data?.before?.data();
  const applicationId = after?.applicationId ?? before?.applicationId;
  if (!applicationId) return;

  // Server-side aggregation over *all* published reviews (no page cap).
  const aggregate = await db
    .collection(COLLECTIONS.reviews)
    .where("applicationId", "==", applicationId)
    .where("status", "==", "published")
    .aggregate({ count: AggregateField.count(), average: AggregateField.average("rating") })
    .get();
  const { count, average } = aggregate.data();
  const rating = count === 0 || average === null ? 0 : Math.round(average * 10) / 10;

  const appRef = db.collection(COLLECTIONS.applications).doc(applicationId);
  if ((await appRef.get()).exists) {
    await appRef.update({ rating, reviewCount: count });
  }

  // A new review notifies the developer once (not on edit or delete).
  if (!before && after) {
    const appSnap = await db.collection(COLLECTIONS.applications).doc(applicationId).get();
    const ownerId = appSnap.data()?.ownerId;
    if (ownerId) {
      await sendNotification({
        userId: ownerId,
        type: "reviewReceived",
        title: `${after.rating} star review`,
        body: `${after.userName ?? "Someone"} reviewed ${appSnap.data()?.name ?? "your app"}.`,
        route: `/app/${applicationId}`,
        data: { applicationId, reviewId: event.params.reviewId },
      });
    }
    await db.collection(COLLECTIONS.users).doc(after.userId).set(
      { reviewCount: FieldValue.increment(1) },
      { merge: true },
    );
  }
});

/** Developer reply. Ownership is verified here, not just in the UI. */
export const respondToReview = onCall(async (request) => {
  const ctx = await requireUser(request);
  const reviewId = request.data?.reviewId;
  const body = request.data?.body;
  if (typeof reviewId !== "string" || typeof body !== "string" || body.trim().length < 2) {
    throw new HttpsError("invalid-argument", "reviewId and a reply are required.");
  }
  if (body.length > 600) throw new HttpsError("invalid-argument", "Keep replies under 600 characters.");

  const reviewRef = db.collection(COLLECTIONS.reviews).doc(reviewId);
  const reviewSnap = await reviewRef.get();
  if (!reviewSnap.exists) throw new HttpsError("not-found", "That review does not exist.");

  const appSnap = await db
    .collection(COLLECTIONS.applications)
    .doc(reviewSnap.data()!.applicationId)
    .get();
  if (appSnap.data()?.ownerId !== ctx.uid) {
    throw new HttpsError("permission-denied", "Only the app owner can reply.");
  }

  await reviewRef.update({
    developerResponse: {
      body: body.trim(),
      respondedAt: FieldValue.serverTimestamp(),
    },
    updatedAt: FieldValue.serverTimestamp(),
  });

  await sendNotification({
    userId: reviewSnap.data()!.userId,
    type: "system",
    title: "The developer replied",
    body: `${appSnap.data()?.name ?? "A developer"} responded to your review.`,
    route: `/app/${reviewSnap.data()!.applicationId}`,
  });

  return { ok: true };
});
