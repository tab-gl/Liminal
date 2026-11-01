#!/usr/bin/env node
/**
 * Seeds the Sproutly category taxonomy and the platform stats document.
 *
 * These are real configuration records the app reads at runtime — not demo
 * products. Run once per environment:
 *
 *   GOOGLE_APPLICATION_CREDENTIALS=./service-account.json node seed.mjs
 */
import { initializeApp, applicationDefault } from "firebase-admin/app";
import { getFirestore, FieldValue } from "firebase-admin/firestore";

const CATEGORIES = [
  { name: "Plants", slug: "plants", iconKey: "leaf", order: 1 },
  { name: "Home & Garden", slug: "home-garden", iconKey: "home", order: 2 },
  { name: "Food & Pantry", slug: "food-pantry", iconKey: "basket", order: 3 },
  { name: "Wellness", slug: "wellness", iconKey: "heart", order: 4 },
  { name: "Handmade", slug: "handmade", iconKey: "sparkle", order: 5 },
  { name: "Fashion", slug: "fashion", iconKey: "shirt", order: 6 },
  { name: "Electronics", slug: "electronics", iconKey: "bolt", order: 7 },
  { name: "Stationery", slug: "stationery", iconKey: "pen", order: 8 },
];

async function main() {
  initializeApp({ credential: applicationDefault() });
  const db = getFirestore();

  for (const category of CATEGORIES) {
    const ref = db.collection("categories").doc(category.slug);
    const snap = await ref.get();
    if (snap.exists) {
      console.log(`skip  ${category.name} (exists)`);
      continue;
    }
    await ref.set({ ...category, productCount: 0 });
    console.log(`added ${category.name}`);
  }

  await db
    .collection("platform")
    .doc("stats")
    .set(
      {
        userCount: 0,
        sellerCount: 0,
        storeCount: 0,
        pendingStores: 0,
        productCount: 0,
        blockedProducts: 0,
        orderCount: 0,
        gmv: 0,
        updatedAt: FieldValue.serverTimestamp(),
      },
      { merge: true },
    );

  console.log("platform/stats initialised");
}

main().then(
  () => process.exit(0),
  (error) => {
    console.error(error);
    process.exit(1);
  },
);
