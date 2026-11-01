import { onDocumentCreated, onDocumentUpdated } from "firebase-functions/v2/firestore";
import { getFirestore, FieldValue } from "firebase-admin/firestore";
import { REGION } from "../config";
import { notifyUser } from "./push";

/**
 * Fan-out on order creation: tell the buyer their order landed and tell each
 * seller they have something to fulfil.
 */
export const onOrderCreated = onDocumentCreated(
  { document: "orders/{orderId}", region: REGION },
  async (event) => {
    const snap = event.data;
    if (!snap) return;
    const order = snap.data();
    const orderId = event.params.orderId;
    const buyerId = String(order.buyerId ?? "");
    const items = (order.items as Array<Record<string, unknown>>) ?? [];
    const total = Number(order.total ?? 0);
    const shortId = orderId.slice(0, 8).toUpperCase();

    if (buyerId) {
      await notifyUser({
        uid: buyerId,
        title: "Order placed",
        body: `Order #${shortId} is with the seller. We will let you know when it is confirmed.`,
        kind: "ORDER",
        deepLink: `order/${orderId}`,
      });
    }

    const storeIds = Array.from(new Set(items.map((item) => String(item.storeId ?? ""))));
    const firestore = getFirestore();

    for (const storeId of storeIds) {
      if (!storeId) continue;
      const storeSnap = await firestore.collection("stores").doc(storeId).get();
      const ownerId = String(storeSnap.get("ownerId") ?? "");
      if (!ownerId) continue;
      await notifyUser({
        uid: ownerId,
        title: "New order",
        body: `Order #${shortId} for ₹${Math.round(total / 100)} needs your confirmation.`,
        kind: "ORDER",
        deepLink: `order/${orderId}`,
      });
      await firestore.collection("stores").doc(storeId).set(
        { pendingOrderCount: FieldValue.increment(1) },
        { merge: true },
      );
    }
  },
);

/** Notify the buyer whenever a seller moves the order forward. */
export const onOrderStatusChanged = onDocumentUpdated(
  { document: "orders/{orderId}", region: REGION },
  async (event) => {
    const before = event.data?.before.data();
    const after = event.data?.after.data();
    if (!before || !after) return;
    if (before.status === after.status) return;

    const buyerId = String(after.buyerId ?? "");
    if (!buyerId) return;

    const orderId = event.params.orderId;
    const status = String(after.status ?? "");
    const messages: Record<string, string> = {
      CONFIRMED: "Your order was confirmed by the seller.",
      PACKED: "Your order is packed and ready to ship.",
      SHIPPED: "Your order is on the way.",
      DELIVERED: "Your order was delivered. Enjoy!",
      CANCELLED: "Your order was cancelled.",
    };
    const body = messages[status];
    if (!body) return;

    await notifyUser({
      uid: buyerId,
      title: `Order #${orderId.slice(0, 8).toUpperCase()} · ${status.toLowerCase()}`,
      body,
      kind: "ORDER",
      deepLink: `order/${orderId}`,
    });
  },
);
