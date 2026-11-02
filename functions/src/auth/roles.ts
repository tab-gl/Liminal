import { onCall, HttpsError } from "firebase-functions/v2/https";
import { getFirestore, FieldValue } from "firebase-admin/firestore";
import { REGION } from "../config";
import { badRequest, requireAdmin, requireCaller } from "../guards";

const db = () => getFirestore();

/**
 * Creates a store for the calling user and promotes them to SELLER.
 * Idempotent: calling it twice returns the existing store id.
 */
export const registerSeller = onCall(
  { region: REGION, enforceAppCheck: true },
  async (request) => {
    const caller = await requireCaller(request);
    const storeName = String(request.data?.storeName ?? "").trim();
    const tagline = String(request.data?.tagline ?? "").trim().slice(0, 80);
    const city = String(request.data?.city ?? "").trim().slice(0, 60);

    if (storeName.length < 3) {
      throw badRequest("Store name needs at least 3 characters.");
    }

    if (caller.storeId) {
      return { ok: true, storeId: caller.storeId, existing: true };
    }

    const storeRef = db().collection("stores").doc();
    const slug = storeName
      .toLowerCase()
      .replace(/[^a-z0-9\s-]/g, "")
      .replace(/\s+/g, "-")
      .slice(0, 60);

    const batch = db().batch();
    batch.set(storeRef, {
      ownerId: caller.uid,
      name: storeName,
      slug,
      tagline,
      description: "",
      logoUrl: null,
      logoPublicId: null,
      bannerUrl: null,
      bannerPublicId: null,
      city,
      status: "PENDING",
      rating: 0,
      ratingCount: 0,
      productCount: 0,
      createdAt: FieldValue.serverTimestamp(),
      updatedAt: FieldValue.serverTimestamp(),
    });
    batch.set(
      db().collection("users").doc(caller.uid),
      { role: "SELLER", storeId: storeRef.id, updatedAt: FieldValue.serverTimestamp() },
      { merge: true },
    );
    await batch.commit();

    return { ok: true, storeId: storeRef.id };
  },
);

/** Admin-only role changes. Guards against an admin locking themselves out. */
export const setUserRole = onCall(
  { region: REGION, enforceAppCheck: true },
  async (request) => {
    const caller = await requireAdmin(request);
    const uid = String(request.data?.uid ?? "");
    const role = String(request.data?.role ?? "").toUpperCase();

    if (!uid) throw badRequest("A user id is required.");
    if (!["CUSTOMER", "SELLER", "ADMIN"].includes(role)) {
      throw badRequest("Unknown role.");
    }
    if (uid === caller.uid && role !== "ADMIN") {
      throw new HttpsError("failed-precondition", "You cannot remove your own admin access.");
    }

    const targetRef = db().collection("users").doc(uid);
    const target = await targetRef.get();
    if (!target.exists) throw new HttpsError("not-found", "That user profile does not exist.");

    const updates: Record<string, unknown> = {
      role,
      updatedAt: FieldValue.serverTimestamp(),
    };
    if (role === "CUSTOMER") {
      updates.storeId = null;
    } else if (role === "SELLER") {
      const storeId = target.get("storeId");
      if (typeof storeId !== "string" || !storeId) {
        throw new HttpsError("failed-precondition", "The user must create a store before receiving the seller role.");
      }
      const store = await db().collection("stores").doc(storeId).get();
      if (!store.exists || store.get("ownerId") !== uid) {
        throw new HttpsError("failed-precondition", "The user must own the linked store before receiving the seller role.");
      }
    }

    await targetRef.set(updates, { merge: true });
    return { ok: true };
  },
);
