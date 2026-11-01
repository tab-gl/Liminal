import { onCall, HttpsError } from "firebase-functions/v2/https";
import { getStorage } from "firebase-admin/storage";
import { db, auth, COLLECTIONS, FieldValue } from "./config";
import { audit, optionalString, requireAdmin, requireUser } from "./util";

/**
 * Grants or revokes a role. Roles live in custom claims so security rules can
 * read them without a document lookup, and in the profile document so the UI can
 * render them without a token refresh.
 */
export const setUserRole = onCall(async (request) => {
  const admin = await requireAdmin(request);
  const uid = request.data?.uid;
  const role = request.data?.role;
  if (typeof uid !== "string" || !["user", "developer", "admin"].includes(role)) {
    throw new HttpsError("invalid-argument", "uid and a valid role are required.");
  }
  if (uid === admin.uid && role !== "admin") {
    throw new HttpsError("failed-precondition", "You cannot remove your own admin role.");
  }

  await auth.setCustomUserClaims(uid, { role });
  await db.collection(COLLECTIONS.users).doc(uid).set(
    {
      role,
      isDeveloper: role === "developer" || role === "admin",
      updatedAt: FieldValue.serverTimestamp(),
    },
    { merge: true },
  );
  await audit(admin.uid, "set_user_role", uid, { role });

  return { ok: true, role };
});

/** A verified-developer request. The badge itself is an admin decision. */
export const requestDeveloperAccess = onCall(async (request) => {
  const ctx = await requireUser(request);
  const motivation = optionalString(request.data?.motivation, 500) ?? "";

  const existing = await db
    .collection(COLLECTIONS.users)
    .doc(ctx.uid)
    .collection("private")
    .doc("verification")
    .get();
  if (existing.exists && existing.data()?.status === "pending") {
    throw new HttpsError("already-exists", "You already have a request in review.");
  }

  await db
    .collection(COLLECTIONS.users)
    .doc(ctx.uid)
    .collection("private")
    .doc("verification")
    .set({
      status: "pending",
      motivation,
      requestedAt: FieldValue.serverTimestamp(),
    });

  const admins = await db
    .collection(COLLECTIONS.users)
    .where("role", "==", "admin")
    .limit(20)
    .get();
  await Promise.all(
    admins.docs.map((doc) =>
      db.collection(COLLECTIONS.notifications).add({
        userId: doc.id,
        type: "system",
        title: "Verification request",
        body: `${ctx.uid} asked for a verified developer badge.`,
        route: "/admin",
        data: {},
        readAt: null,
        createdAt: FieldValue.serverTimestamp(),
      }),
    ),
  );

  return { ok: true };
});

/** Deletes every document a query returns, in pages that fit a write batch. */
async function purge(query: FirebaseFirestore.Query, apply?: "delete" | ((doc: FirebaseFirestore.QueryDocumentSnapshot) => Record<string, unknown>)): Promise<number> {
  let total = 0;
  for (;;) {
    const page = await query.limit(400).get();
    if (page.empty) return total;
    const batch = db.batch();
    for (const doc of page.docs) {
      if (typeof apply === "function") batch.update(doc.ref, apply(doc));
      else batch.delete(doc.ref);
    }
    await batch.commit();
    total += page.size;
    // An update-style purge must not re-read the same rows forever.
    if (typeof apply === "function" || page.size < 400) return total;
  }
}

/** How recently the caller must have signed in to delete their account. */
const RECENT_LOGIN_SECONDS = 5 * 60;

/**
 * Server-managed account deletion.
 *
 * Requires a recent sign-in, then removes the person's data in pages (no fixed
 * caps), and finally the profile and the Auth record itself. The profile goes
 * last, so a failure part-way leaves the account intact and the call can simply
 * be retried; every step is idempotent. Applications are kept but anonymised and
 * suspended so reviews written about them do not dangle. Payment records and the
 * audit log are retained for financial and security accountability.
 */
export const deleteAccount = onCall(async (request) => {
  const ctx = await requireUser(request);
  const authTime = Number((request.auth?.token as Record<string, unknown> | undefined)?.auth_time ?? 0);
  if (Date.now() / 1000 - authTime > RECENT_LOGIN_SECONDS) {
    throw new HttpsError("unauthenticated", "recent-login-required: sign in again to delete your account.");
  }
  const uid = ctx.uid;

  // Listings: anonymise and suspend. Suspended rows drop out of the query, so
  // looping until it is empty pages through every listing.
  let apps = 0;
  for (;;) {
    const batchOfApps = await db
      .collection(COLLECTIONS.applications)
      .where("ownerId", "==", uid)
      .where("status", "in", ["draft", "pending_review", "approved", "rejected"])
      .limit(400)
      .get();
    if (batchOfApps.empty) break;
    const batch = db.batch();
    for (const doc of batchOfApps.docs) {
      batch.update(doc.ref, {
        status: "suspended",
        ownerName: "Former member",
        ownerPhotoUrl: null,
        updatedAt: FieldValue.serverTimestamp(),
      });
    }
    await batch.commit();
    apps += batchOfApps.size;
  }

  // Campaigns stop serving immediately.
  const campaigns = await purge(
    db.collection(COLLECTIONS.campaigns).where("ownerId", "==", uid).where("status", "in", ["active", "pending_approval", "paused", "exhausted"]),
    () => ({ status: "completed", updatedAt: FieldValue.serverTimestamp() }),
  );

  await purge(db.collection(COLLECTIONS.reviews).where("userId", "==", uid));
  await purge(db.collection(COLLECTIONS.bookmarks).where("userId", "==", uid));
  await purge(db.collection(COLLECTIONS.follows).where("followerId", "==", uid));
  await purge(db.collection(COLLECTIONS.follows).where("developerId", "==", uid));
  await purge(db.collection(COLLECTIONS.notifications).where("userId", "==", uid));
  await purge(db.collection("reports").where("reporterId", "==", uid));

  // Uploaded media.
  const bucket = getStorage().bucket();
  for (const folder of ["avatars", "app_icons", "screenshots", "banners"]) {
    await bucket.deleteFiles({ prefix: `${folder}/${uid}/`, force: true });
  }

  // Profile and private subcollection last, then the Auth record.
  await db.recursiveDelete(db.collection(COLLECTIONS.users).doc(uid));
  try {
    await auth.deleteUser(uid);
  } catch (error) {
    if ((error as { code?: string }).code !== "auth/user-not-found") throw error;
  }

  await audit(uid, "delete_account", uid, { apps, campaigns });
  return { ok: true };
});
