/**
 * Security-rule tests.
 *
 * Every case here exists because that exact path broke in production once:
 * a batched promotion the rules could not see, a role typed in lower case, a
 * signed-in account with no profile document, an admin listing reported apps,
 * and a free app that could not be installed.
 *
 *   firebase emulators:exec --only firestore "cd firestore-tests && npm test"
 */
const { readFileSync } = require("fs");
const { join } = require("path");
const assert = require("assert");
const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const {
  doc, getDoc, setDoc, updateDoc, collection, getDocs, query, where, writeBatch,
} = require("firebase/firestore");

const ALICE = "alice";
const BOB = "bob";
const ADMIN = "admin1";
const STORE = "store1";

let env;

/** A payload that satisfies validProduct(). */
function product(overrides = {}) {
  return {
    storeId: STORE,
    storeName: "Ace Studio",
    title: "Notes",
    description: "A notes app",
    categoryId: "productivity",
    categoryLabel: "Productivity",
    price: 0,
    stock: 0,
    images: [],
    screenshots: [],
    apkUrl: "https://example.com/app.apk",
    status: "ACTIVE",
    unitsSold: 0,
    rating: 0,
    ratingCount: 0,
    tags: [],
    createdAt: 1,
    updatedAt: 1,
    ...overrides,
  };
}

before(async () => {
  env = await initializeTestEnvironment({
    projectId: "demo-sproutly",
    firestore: {
      rules: readFileSync(join(__dirname, "..", "firestore.rules"), "utf8"),
    },
  });
});

after(async () => {
  await env.cleanup();
});

beforeEach(async () => {
  await env.clearFirestore();
  await env.withSecurityRulesDisabled(async (context) => {
    const db = context.firestore();
    await setDoc(doc(db, "users", ALICE), {
      uid: ALICE, email: "alice@example.com", displayName: "Alice", role: "CUSTOMER",
    });
    await setDoc(doc(db, "users", ADMIN), {
      uid: ADMIN, email: "admin@example.com", displayName: "Admin", role: "ADMIN",
    });
    await setDoc(doc(db, "stores", STORE), {
      ownerId: ALICE, name: "Ace Studio", slug: "ace-studio", status: "ACTIVE",
      verified: false, productCount: 0, rating: 0, ratingCount: 0, createdAt: 1,
    });
    await setDoc(doc(db, "products", "app1"), product());
    await setDoc(doc(db, "products", "reported1"), product({ status: "DRAFT" }));
  });
});

const alice = () => env.authenticatedContext(ALICE).firestore();
const admin = () => env.authenticatedContext(ADMIN).firestore();
const guest = () => env.unauthenticatedContext().firestore();

describe("storefront", () => {
  it("lets anyone read a published app", async () => {
    await assertSucceeds(getDoc(doc(guest(), "products", "app1")));
  });

  it("hides drafts from the public", async () => {
    await assertFails(getDoc(doc(guest(), "products", "reported1")));
  });

  it("does not deny a signed-in account that has no profile yet", async () => {
    // The regression: role() read .role on a missing document, which is an
    // evaluation error, and an error denies the whole rule.
    const noProfile = env.authenticatedContext("newcomer").firestore();
    await assertSucceeds(
      getDocs(query(collection(noProfile, "products"), where("status", "==", "ACTIVE"))),
    );
  });
});

describe("roles", () => {
  it("treats a lower-case role as that role", async () => {
    await env.withSecurityRulesDisabled(async (context) => {
      await updateDoc(doc(context.firestore(), "users", ADMIN), { role: "admin" });
    });
    await assertSucceeds(getDocs(collection(admin(), "products")));
  });

  it("lets an admin list reported apps", async () => {
    // A list query with no status filter cannot satisfy isPublic() alone.
    await assertSucceeds(getDocs(collection(admin(), "products")));
  });

  it("does not let a shopper list every product", async () => {
    await assertFails(getDocs(collection(alice(), "products")));
  });
});

describe("developer conversion", () => {
  it("promotes a shopper and opens a store in one batch", async () => {
    const db = alice();
    const batch = writeBatch(db);
    batch.set(doc(db, "stores", "store2"), {
      ownerId: ALICE, name: "Second Studio", slug: "second-studio", status: "PENDING",
      verified: false, productCount: 0, rating: 0, ratingCount: 0, createdAt: 1,
    });
    batch.set(
      doc(db, "users", ALICE),
      { role: "SELLER", storeId: "store2", updatedAt: 2 },
      { merge: true },
    );
    await assertSucceeds(batch.commit());
  });

  it("lets an admin open a store without being demoted", async () => {
    const db = admin();
    const batch = writeBatch(db);
    batch.set(doc(db, "stores", "store3"), {
      ownerId: ADMIN, name: "Admin Studio", slug: "admin-studio", status: "PENDING",
      verified: false, productCount: 0, rating: 0, ratingCount: 0, createdAt: 1,
    });
    batch.set(doc(db, "users", ADMIN), { storeId: "store3", updatedAt: 2 }, { merge: true });
    await assertSucceeds(batch.commit());
  });

  it("refuses a promotion towards a store the caller does not own", async () => {
    await env.withSecurityRulesDisabled(async (context) => {
      await setDoc(doc(context.firestore(), "stores", "someone-else"), {
        ownerId: BOB, name: "Bob Studio", slug: "bob-studio", status: "PENDING",
        verified: false, productCount: 0, rating: 0, ratingCount: 0, createdAt: 1,
      });
    });
    await assertFails(
      updateDoc(doc(alice(), "users", ALICE), { role: "SELLER", storeId: "someone-else" }),
    );
  });
});

describe("paid apps", () => {
  it("refuses a paid listing from an unverified developer", async () => {
    await assertFails(
      setDoc(doc(alice(), "products", "paid1"), product({ price: 9900, status: "DRAFT" })),
    );
  });

  it("allows it once the store is verified", async () => {
    await env.withSecurityRulesDisabled(async (context) => {
      await updateDoc(doc(context.firestore(), "stores", STORE), { verified: true });
    });
    await assertSucceeds(
      setDoc(doc(alice(), "products", "paid1"), product({ price: 9900, status: "DRAFT" })),
    );
  });

  it("always allows a free listing", async () => {
    await assertSucceeds(
      setDoc(doc(alice(), "products", "free1"), product({ price: 0, status: "DRAFT" })),
    );
  });
});

describe("installs", () => {
  const order = (overrides = {}) => ({
    buyerId: ALICE,
    buyerName: "Alice",
    items: [{
      productId: "app1",
      title: "Notes",
      imageUrl: null,
      unitPrice: 0,
      quantity: 1,
      storeId: STORE,
      storeName: "Ace Studio",
    }],
    subtotal: 0,
    shipping: 0,
    total: 0,
    currency: "INR",
    status: "DELIVERED",
    note: "",
    storeIds: [STORE],
    placedAt: 1,
    updatedAt: 1,
    timeline: [],
    payment: { method: "NONE", status: "PAID" },
    ...overrides,
  });

  it("installs a free app with no address and no shipping", async () => {
    await assertSucceeds(setDoc(doc(alice(), "orders", "o1"), order()));
  });

  it("keeps an install private to its buyer", async () => {
    await assertSucceeds(setDoc(doc(alice(), "orders", "o2"), order()));
    await assertFails(getDoc(doc(env.authenticatedContext(BOB).firestore(), "orders", "o2")));
  });

  it("rejects a tampered price", async () => {
    await assertFails(
      setDoc(doc(alice(), "orders", "o3"), order({ subtotal: 100, total: 100 })),
    );
  });
});

describe("wishlist", () => {
  it("is scoped to its owner", async () => {
    await assertSucceeds(
      setDoc(doc(alice(), "wishlists", ALICE, "items", "app1"), { productId: "app1" }),
    );
    await assertFails(
      getDocs(collection(env.authenticatedContext(BOB).firestore(), "wishlists", ALICE, "items")),
    );
  });
});
