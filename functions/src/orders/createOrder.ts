import { onCall } from "firebase-functions/v2/https";
import { getFirestore, FieldValue, Timestamp } from "firebase-admin/firestore";
import { FLAT_SHIPPING_PAISE, FREE_SHIPPING_THRESHOLD_PAISE, REGION } from "../config";
import { badRequest, failedPrecondition, requireCaller } from "../guards";

const db = () => getFirestore();

interface CartLine {
  productId: string;
  quantity: number;
}

interface OrderItem {
  productId: string;
  title: string;
  imageUrl: string | null;
  unitPrice: number;
  quantity: number;
  storeId: string;
  storeName: string;
}

/**
 * Creates an order from the caller's server-side cart.
 *
 * Everything that matters is re-derived here: prices, stock, shipping and the
 * totals. The client cannot pass a price, a discount or a total — it only says
 * "place the order at this address". Stock is decremented in the same
 * transaction, so two shoppers cannot buy the last unit.
 */
export const createOrder = onCall(
  { region: REGION, enforceAppCheck: true },
  async (request) => {
    const caller = await requireCaller(request);
    const addressId = String(request.data?.addressId ?? "");
    const note = String(request.data?.note ?? "").slice(0, 240);

    if (!addressId) throw badRequest("A delivery address is required.");

    const firestore = db();
    const orderRef = firestore.collection("orders").doc();

    const orderId = await firestore.runTransaction(async (tx) => {
      const cartSnap = await tx.get(
        firestore.collection("carts").doc(caller.uid).collection("lines"),
      );
      if (cartSnap.empty) {
        throw failedPrecondition("Your cart is empty.");
      }

      const lines: CartLine[] = cartSnap.docs.map((doc) => ({
        productId: doc.id,
        quantity: Number(doc.get("quantity") ?? 0),
      }));

      const addressSnap = await tx.get(
        firestore
          .collection("addresses")
          .doc(caller.uid)
          .collection("items")
          .doc(addressId),
      );
      if (!addressSnap.exists) {
        throw failedPrecondition("That delivery address no longer exists.");
      }
      const address = addressSnap.data() ?? {};

      const productRefs = lines.map((line) => firestore.collection("products").doc(line.productId));
      const productSnaps = await tx.getAll(...productRefs);

      const items: OrderItem[] = [];
      const storeIds = new Set<string>();

      productSnaps.forEach((snap, index) => {
        const line = lines[index];
        if (!snap.exists) {
          throw failedPrecondition("An item in your cart is no longer available.");
        }
        const data = snap.data() ?? {};
        const stock = Number(data.stock ?? 0);
        const status = String(data.status ?? "DRAFT");
        const price = Number(data.price ?? 0);
        const images = Array.isArray(data.images) ? data.images : [];

        if (status !== "ACTIVE") {
          throw failedPrecondition(`${data.title ?? "An item"} is no longer for sale.`);
        }
        if (stock < line.quantity) {
          throw failedPrecondition(
            `Only ${stock} left of ${data.title ?? "an item"}. Please update your cart.`,
          );
        }

        items.push({
          productId: snap.id,
          title: String(data.title ?? ""),
          imageUrl: images.length > 0 ? String(images[0]?.url ?? "") : null,
          unitPrice: price,
          quantity: line.quantity,
          storeId: String(data.storeId ?? ""),
          storeName: String(data.storeName ?? ""),
        });
        storeIds.add(String(data.storeId ?? ""));

        tx.update(snap.ref, {
          stock: stock - line.quantity,
          unitsSold: Number(data.unitsSold ?? 0) + line.quantity,
          updatedAt: FieldValue.serverTimestamp(),
        });
      });

      const subtotal = items.reduce((sum, item) => sum + item.unitPrice * item.quantity, 0);
      const shipping = subtotal >= FREE_SHIPPING_THRESHOLD_PAISE ? 0 : FLAT_SHIPPING_PAISE;
      const total = subtotal + shipping;
      const now = Timestamp.now();

      tx.set(orderRef, {
        buyerId: caller.uid,
        buyerName: String(request.auth?.token?.name ?? "") || caller.email.split("@")[0],
        items,
        subtotal,
        shipping,
        total,
        currency: "INR",
        status: "PENDING",
        note,
        address: {
          id: addressId,
          label: address.label ?? "Home",
          fullName: address.fullName ?? "",
          phone: address.phone ?? "",
          line1: address.line1 ?? "",
          line2: address.line2 ?? "",
          city: address.city ?? "",
          state: address.state ?? "",
          pincode: address.pincode ?? "",
          isDefault: address.isDefault ?? false,
        },
        storeIds: Array.from(storeIds),
        placedAt: FieldValue.serverTimestamp(),
        updatedAt: FieldValue.serverTimestamp(),
        timeline: [{ status: "PENDING", at: now.toMillis(), note: "Order received" }],
        payment: { method: "COD", status: "PENDING" },
      });

      // Empty the cart inside the same transaction.
      cartSnap.docs.forEach((doc) => tx.delete(doc.ref));

      return orderRef.id;
    });

    await db()
      .collection("platform")
      .doc("stats")
      .set(
        {
          orderCount: FieldValue.increment(1),
          updatedAt: FieldValue.serverTimestamp(),
        },
        { merge: true },
      );

    return { ok: true, orderId };
  },
);
