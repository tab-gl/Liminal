import { HttpsError, onCall } from "firebase-functions/v2/https";
import { getFirestore, FieldValue, Timestamp } from "firebase-admin/firestore";
import { REGION } from "../config";
import { badRequest, failedPrecondition, requireCaller } from "../guards";

const db = () => getFirestore();

type OrderStatus = "PENDING" | "CONFIRMED" | "PACKED" | "SHIPPED" | "DELIVERED" | "CANCELLED";

const SELLER_TRANSITIONS: Record<string, OrderStatus[]> = {
  PENDING: ["CONFIRMED", "CANCELLED"],
  CONFIRMED: ["PACKED", "CANCELLED"],
  PACKED: ["SHIPPED"],
  SHIPPED: ["DELIVERED"],
  DELIVERED: [],
  CANCELLED: [],
};

const ADMIN_TRANSITIONS: Record<string, OrderStatus[]> = {
  PENDING: ["CONFIRMED", "CANCELLED"],
  CONFIRMED: ["PACKED", "CANCELLED"],
  PACKED: ["SHIPPED", "CANCELLED"],
  SHIPPED: ["DELIVERED", "CANCELLED"],
  DELIVERED: [],
  CANCELLED: [],
};

/** Seller/admin fulfilment updates with an explicit state machine. */
export const updateOrderStatus = onCall(
  { region: REGION, enforceAppCheck: true },
  async (request) => {
    const caller = await requireCaller(request);
    const orderId = String(request.data?.orderId ?? "");
    const next = String(request.data?.status ?? "").toUpperCase() as OrderStatus;

    if (!orderId) throw badRequest("An order id is required.");
    const validTargets: OrderStatus[] = ["CONFIRMED", "PACKED", "SHIPPED", "DELIVERED", "CANCELLED"];
    if (!validTargets.includes(next)) {
      throw badRequest("Unknown order status.");
    }

    const ref = db().collection("orders").doc(orderId);

    await db().runTransaction(async (tx) => {
      const snap = await tx.get(ref);
      if (!snap.exists) throw failedPrecondition("That order no longer exists.");

      const data = snap.data() ?? {};
      const current = String(data.status ?? "PENDING") as OrderStatus;
      const storeIds = (data.storeIds as string[]) ?? [];
      const isOwner = caller.storeId !== null && storeIds.includes(caller.storeId);
      const isAdmin = caller.role === "ADMIN";

      if (!isOwner && !isAdmin) {
        throw new HttpsError("permission-denied", "You do not have permission to update this order.");
      }

      const allowed = (isAdmin ? ADMIN_TRANSITIONS : SELLER_TRANSITIONS)[current] ?? [];
      if (!allowed.includes(next)) {
        throw failedPrecondition(`An order that is ${current.toLowerCase()} cannot become ${next.toLowerCase()}.`);
      }

      const timeline = Array.isArray(data.timeline) ? data.timeline : [];
      tx.update(ref, {
        status: next,
        updatedAt: FieldValue.serverTimestamp(),
        timeline: [
          ...timeline,
          { status: next, at: Timestamp.now().toMillis(), note: null },
        ],
      });
    });

    return { ok: true };
  },
);

/**
 * Cancellation. A buyer may cancel while the order is still PENDING or
 * CONFIRMED; sellers and admins use the same path. Stock is returned to the
 * shelf and the timeline records who cancelled.
 */
export const cancelOrder = onCall(
  { region: REGION, enforceAppCheck: true },
  async (request) => {
    const caller = await requireCaller(request);
    const orderId = String(request.data?.orderId ?? "");
    if (!orderId) throw badRequest("An order id is required.");

    const firestore = db();
    const ref = firestore.collection("orders").doc(orderId);

    await firestore.runTransaction(async (tx) => {
      const snap = await tx.get(ref);
      if (!snap.exists) throw failedPrecondition("That order no longer exists.");
      const data = snap.data() ?? {};

      const isBuyer = data.buyerId === caller.uid;
      const storeIds = (data.storeIds as string[]) ?? [];
      const isOwner = caller.storeId !== null && storeIds.includes(caller.storeId);
      const isAdmin = caller.role === "ADMIN";
      if (!isBuyer && !isOwner && !isAdmin) {
        throw new HttpsError("permission-denied", "You do not have permission to update this order.");
      }

      const status = String(data.status ?? "PENDING");
      if (!["PENDING", "CONFIRMED"].includes(status)) {
        throw failedPrecondition("This order can no longer be cancelled.");
      }

      const items = Array.isArray(data.items) ? data.items : [];
      for (const item of items) {
        const productRef = firestore.collection("products").doc(String(item.productId));
        const productSnap = await tx.get(productRef);
        if (productSnap.exists) {
          const stock = Number(productSnap.get("stock") ?? 0);
          const unitsSold = Number(productSnap.get("unitsSold") ?? 0);
          tx.update(productRef, {
            stock: stock + Number(item.quantity ?? 0),
            unitsSold: Math.max(0, unitsSold - Number(item.quantity ?? 0)),
            updatedAt: FieldValue.serverTimestamp(),
          });
        }
      }

      const timeline = Array.isArray(data.timeline) ? data.timeline : [];
      tx.update(ref, {
        status: "CANCELLED",
        cancelledBy: isBuyer ? "BUYER" : isOwner ? "SELLER" : "ADMIN",
        updatedAt: FieldValue.serverTimestamp(),
        timeline: [
          ...timeline,
          {
            status: "CANCELLED",
            at: Timestamp.now().toMillis(),
            note: isBuyer ? "Cancelled by shopper" : "Cancelled by seller",
          },
        ],
      });
    });

    return { ok: true };
  },
);
