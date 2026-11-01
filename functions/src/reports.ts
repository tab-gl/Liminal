import { onCall, HttpsError } from "firebase-functions/v2/https";
import { db, COLLECTIONS, FieldValue } from "./config";
import { audit, optionalString, requireAdmin, sendNotification } from "./util";

/** Closes a report. Upholding it can also suspend the reported listing. */
export const resolveReport = onCall(async (request) => {
  const admin = await requireAdmin(request);
  const reportId = request.data?.reportId;
  const uphold = request.data?.uphold === true;
  const note = optionalString(request.data?.note, 600);

  if (typeof reportId !== "string") throw new HttpsError("invalid-argument", "reportId is required.");

  const ref = db.collection(COLLECTIONS.reports).doc(reportId);
  const snap = await ref.get();
  if (!snap.exists) throw new HttpsError("not-found", "Unknown report.");
  const report = snap.data()!;

  await ref.update({
    status: uphold ? "resolved" : "dismissed",
    resolutionNote: note ?? null,
    resolvedBy: admin.uid,
    resolvedAt: FieldValue.serverTimestamp(),
  });

  if (uphold && report.target === "application" && typeof report.targetId === "string") {
    const appRef = db.collection(COLLECTIONS.applications).doc(report.targetId);
    const appSnap = await appRef.get();
    if (appSnap.exists) {
      await appRef.update({
        status: "suspended",
        moderation: {
          state: "suspended",
          reviewerId: admin.uid,
          reviewedAt: FieldValue.serverTimestamp(),
          note: note ?? "Removed after a moderation report.",
        },
        updatedAt: FieldValue.serverTimestamp(),
      });
      await sendNotification({
        userId: appSnap.data()!.ownerId,
        type: "applicationSuspended",
        title: "Listing suspended",
        body: note ?? "A moderator suspended this listing after a report.",
        route: `/dashboard/${report.targetId}`,
      });
    }
  }

  await sendNotification({
    userId: report.reporterId,
    type: "system",
    title: uphold ? "Thanks - we took action" : "We reviewed your report",
    body: uphold
      ? "The content you reported has been actioned."
      : "We looked at the content you reported and did not find a policy breach.",
    route: "/activity",
  });

  await audit(admin.uid, uphold ? "uphold_report" : "dismiss_report", reportId, { note });
  return { ok: true };
});
