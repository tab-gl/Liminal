import { onCall } from "firebase-functions/v2/https";
import { getFirestore, FieldValue } from "firebase-admin/firestore";
import { REGION } from "../config";
import { badRequest, failedPrecondition, requireAdmin } from "../guards";

/** Approve / suspend a store. Server-side admin check, always. */
export const adminSetStoreStatus = onCall(
  { region: REGION, enforceAppCheck: true },
  async (request) => {
    await requireAdmin(request);
    const storeId = String(request.data?.storeId ?? "");
    const status = String(request.data?.status ?? "").toUpperCase();

    if (!storeId) throw badRequest("A store id is required.");
    if (!["PENDING", "ACTIVE", "SUSPENDED"].includes(status)) {
      throw badRequest("Unknown store status.");
    }

    const firestore = getFirestore();
    const ref = firestore.collection("stores").doc(storeId);
    const snap = await ref.get();
    if (!snap.exists) throw failedPrecondition("That store no longer exists.");

    await ref.update({
      status,
      moderatedAt: FieldValue.serverTimestamp(),
      updatedAt: FieldValue.serverTimestamp(),
    });

    if (status === "SUSPENDED") {
      // Pull every listing from the marketplace while the store is suspended.
      const products = await firestore
        .collection("products")
        .where("storeId", "==", storeId)
        .where("status", "==", "ACTIVE")
        .get();
      const batch = firestore.batch();
      products.docs.forEach((doc) => batch.update(doc.ref, { status: "BLOCKED" }));
      await batch.commit();
    }

    return { ok: true };
  },
);

/** Block or restore an individual listing (moderation queue). */
export const adminSetProductStatus = onCall(
  { region: REGION, enforceAppCheck: true },
  async (request) => {
    await requireAdmin(request);
    const productId = String(request.data?.productId ?? "");
    const status = String(request.data?.status ?? "").toUpperCase();

    if (!productId) throw badRequest("A product id is required.");
    if (!["DRAFT", "ACTIVE", "ARCHIVED", "BLOCKED"].includes(status)) {
      throw badRequest("Unknown product status.");
    }

    const ref = getFirestore().collection("products").doc(productId);
    const snap = await ref.get();
    if (!snap.exists) throw failedPrecondition("That product no longer exists.");

    await ref.update({
      status,
      moderatedAt: FieldValue.serverTimestamp(),
      updatedAt: FieldValue.serverTimestamp(),
    });

    return { ok: true };
  },
);
