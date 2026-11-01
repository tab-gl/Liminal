import { onSchedule } from "firebase-functions/v2/scheduler";
import { getFirestore } from "firebase-admin/firestore";
import { REGION } from "../config";

/**
 * Recomputes the platform counters the admin console reads.
 *
 * Counting is done server-side on a schedule rather than trusting the client,
 * so the numbers on the dashboard always come from real documents.
 */
export const refreshPlatformStats = onSchedule(
  { schedule: "every 15 minutes", region: REGION, timeZone: "Asia/Kolkata" },
  async () => {
    const firestore = getFirestore();

    const [users, stores, pendingStores, products, blockedProducts, orders] = await Promise.all([
      firestore.collection("users").count().get(),
      firestore.collection("stores").count().get(),
      firestore.collection("stores").where("status", "==", "PENDING").count().get(),
      firestore.collection("products").count().get(),
      firestore.collection("products").where("status", "==", "BLOCKED").count().get(),
      firestore.collection("orders").count().get(),
    ]);

    const sellers = await firestore.collection("users").where("role", "==", "SELLER").count().get();

    // GMV: sum of totals on orders that were not cancelled.
    let gmv = 0;
    let cursor: string | undefined;
    for (;;) {
      let query = firestore
        .collection("orders")
        .where("status", "in", ["PENDING", "CONFIRMED", "PACKED", "SHIPPED", "DELIVERED"])
        .orderBy("__name__")
        .limit(500);
      if (cursor) query = query.startAfter(cursor);
      const page = await query.get();
      if (page.empty) break;
      page.docs.forEach((doc) => {
        gmv += Number(doc.get("total") ?? 0);
      });
      cursor = page.docs[page.docs.length - 1].id;
      if (page.size < 500) break;
    }

    await firestore.collection("platform").doc("stats").set(
      {
        userCount: users.data().count,
        sellerCount: sellers.data().count,
        storeCount: stores.data().count,
        pendingStores: pendingStores.data().count,
        productCount: products.data().count,
        blockedProducts: blockedProducts.data().count,
        orderCount: orders.data().count,
        gmv,
        updatedAt: new Date(),
      },
      { merge: true },
    );
  },
);
