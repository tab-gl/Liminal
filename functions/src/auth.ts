import { onCall, HttpsError } from "firebase-functions/v2/https";
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

/**
 * Deletes the caller's data, then the auth record is removed client side.
 * Applications are kept but anonymised rather than deleted, so reviews written
 * about them do not dangle.
 */
export const deleteAccount = onCall(async (request) => {
  const ctx = await requireUser(request);

  const apps = await db
    .collection(COLLECTIONS.applications)
    .where("ownerId", "==", ctx.uid)
    .limit(200)
    .get();

  const batch = db.batch();
  for (const app of apps.docs) {
    batch.update(app.ref, {
      status: "suspended",
      ownerName: "Former member",
      updatedAt: FieldValue.serverTimestamp(),
    });
  }
  batch.delete(db.collection(COLLECTIONS.users).doc(ctx.uid));
  await batch.commit();

  const campaigns = await db
    .collection(COLLECTIONS.campaigns)
    .where("ownerId", "==", ctx.uid)
    .limit(200)
    .get();
  await Promise.all(
    campaigns.docs.map((doc) =>
      doc.ref.update({ status: "paused", updatedAt: FieldValue.serverTimestamp() }),
    ),
  );

  await audit(ctx.uid, "delete_account", ctx.uid, { apps: apps.size });
  return { ok: true };
});
