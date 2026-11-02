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
  { name: "Productivity", slug: "productivity", iconKey: "check", order: 1 },
  { name: "Games", slug: "games", iconKey: "gamepad", order: 2 },
  { name: "Photography", slug: "photography", iconKey: "camera", order: 3 },
  { name: "Finance", slug: "finance", iconKey: "wallet", order: 4 },
  { name: "Education", slug: "education", iconKey: "book", order: 5 },
  { name: "Health & Fitness", slug: "health-fitness", iconKey: "heart", order: 6 },
  { name: "Social", slug: "social", iconKey: "users", order: 7 },
  { name: "Tools", slug: "tools", iconKey: "settings", order: 8 },
];

async function main() {
  initializeApp({ credential: applicationDefault() });
  const db = getFirestore();

  for (const category of CATEGORIES) {
    const ref = db.collection("categories").doc(category.slug);
    // merge, not skip: re-running updates the taxonomy of an existing project
    await ref.set(
      { ...category, productCount: 0, updatedAt: FieldValue.serverTimestamp() },
      { merge: true },
    );
    console.log(`  ${category.name}`);
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
