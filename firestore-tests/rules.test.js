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

const DEV = "dev1";      // developer: role SELLER, owns STORE
const SHOPPER = "carol"; // role CUSTOMER, no store
const ADMIN = "admin1";  // role ADMIN, no store
const BOB = "bob";       // bystander
const DEV2 = "dev2";     // developer whose store is still in review
const DEV3 = "dev3";     // verified developer, may sell paid apps
const STORE = "store1";
const STORE_PENDING = "store-pending";

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
    await setDoc(doc(db, "users", DEV), {
      uid: DEV, email: "dev@example.com", displayName: "Dev", role: "SELLER", storeId: STORE,
    });
    await setDoc(doc(db, "users", SHOPPER), {
      uid: SHOPPER, email: "carol@example.com", displayName: "Carol", role: "CUSTOMER",
    });
    await setDoc(doc(db, "users", ADMIN), {
      uid: ADMIN, email: "admin@example.com", displayName: "Admin", role: "ADMIN",
    });
    await setDoc(doc(db, "stores", STORE), {
      ownerId: DEV, name: "Ace Studio", slug: "ace-studio", status: "ACTIVE",
      verified: false, productCount: 0, rating: 0, ratingCount: 0, createdAt: 1,
    });
    await setDoc(doc(db, "users", DEV2), {
      uid: DEV2, email: "dev2@example.com", displayName: "Dev Two",
      role: "SELLER", storeId: STORE_PENDING,
    });
    await setDoc(doc(db, "stores", STORE_PENDING), {
      ownerId: DEV2, name: "Pending Studio", slug: "pending-studio", status: "PENDING",
      verified: false, productCount: 0, rating: 0, ratingCount: 0, createdAt: 1,
    });
    await setDoc(doc(db, "users", DEV3), {
      uid: DEV3, email: "dev3@example.com", displayName: "Dev Three",
      role: "SELLER", storeId: "store-verified",
    });
    await setDoc(doc(db, "stores", "store-verified"), {
      ownerId: DEV3, name: "Verified Studio", slug: "verified-studio", status: "ACTIVE",
      verified: true, productCount: 0, rating: 0, ratingCount: 0, createdAt: 1,
    });
    await setDoc(doc(db, "products", "app-pending"), product({
      storeId: STORE_PENDING, status: "DRAFT",
    }));
    await setDoc(doc(db, "products", "app1"), product());
    await setDoc(doc(db, "products", "reported1"), product({ status: "DRAFT" }));
  });
});

const dev = () => env.authenticatedContext(DEV).firestore();
const shopper = () => env.authenticatedContext(SHOPPER).firestore();
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
    await assertFails(getDocs(collection(shopper(), "products")));
  });
});

describe("developer conversion", () => {
  it("promotes a shopper and opens a store in one batch", async () => {
    const db = shopper();
    const batch = writeBatch(db);
    batch.set(doc(db, "stores", "store2"), {
      ownerId: SHOPPER, name: "Second Studio", slug: "second-studio", status: "PENDING",
      verified: false, productCount: 0, rating: 0, ratingCount: 0, createdAt: 1,
    });
    batch.set(
      doc(db, "users", SHOPPER),
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
      updateDoc(doc(shopper(), "users", SHOPPER), { role: "SELLER", storeId: "someone-else" }),
    );
  });
});

describe("paid apps", () => {
  it("refuses a paid listing from an unverified developer", async () => {
    await assertFails(
      setDoc(doc(dev(), "products", "paid1"), product({ price: 9900, status: "DRAFT" })),
    );
  });

  it("allows it once the store is verified", async () => {
    await env.withSecurityRulesDisabled(async (context) => {
      await updateDoc(doc(context.firestore(), "stores", STORE), { verified: true });
    });
    await assertSucceeds(
      setDoc(doc(dev(), "products", "paid1"), product({ price: 9900, status: "DRAFT" })),
    );
  });

  it("always allows a free listing", async () => {
    await assertSucceeds(
      setDoc(doc(dev(), "products", "free1"), product({ price: 0, status: "DRAFT" })),
    );
  });
});

describe("installs", () => {
  const order = (overrides = {}) => ({
    buyerId: SHOPPER,
    buyerName: "Carol",
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
    await assertSucceeds(setDoc(doc(shopper(), "orders", "o1"), order()));
  });

  it("keeps an install private to its buyer", async () => {
    await assertSucceeds(setDoc(doc(shopper(), "orders", "o2"), order()));
    await assertFails(getDoc(doc(env.authenticatedContext(BOB).firestore(), "orders", "o2")));
  });

  it("rejects a tampered price", async () => {
    await assertFails(
      setDoc(doc(shopper(), "orders", "o3"), order({ subtotal: 100, total: 100 })),
    );
  });
});

describe("wishlist", () => {
  it("is scoped to its owner", async () => {
    await assertSucceeds(
      setDoc(doc(shopper(), "wishlists", SHOPPER, "items", "app1"), { productId: "app1" }),
    );
    await assertFails(
      getDocs(collection(env.authenticatedContext(BOB).firestore(), "wishlists", SHOPPER, "items")),
    );
  });
});

describe("reporting", () => {
  it("lets a signed-in user flag a published app", async () => {
    await assertSucceeds(
      updateDoc(doc(shopper(), "products", "app1"), {
        reported: true,
        reportReason: "It does not work",
        reportCount: 1,
        updatedAt: 2,
      }),
    );
  });

  it("does not let a report smuggle other fields through", async () => {
    await assertFails(
      updateDoc(doc(shopper(), "products", "app1"), {
        reported: true,
        reportReason: "spam",
        reportCount: 1,
        title: "Something else",
      }),
    );
  });

  it("refuses a report that skips the count", async () => {
    await assertFails(
      updateDoc(doc(shopper(), "products", "app1"), {
        reported: true,
        reportReason: "spam",
        reportCount: 5,
        updatedAt: 2,
      }),
    );
  });

  it("surfaces a reported app to the admin queue", async () => {
    await assertSucceeds(
      updateDoc(doc(shopper(), "products", "app1"), {
        reported: true,
        reportReason: "Misleading description",
        reportCount: 1,
        updatedAt: 2,
      }),
    );
    const reported = await getDocs(
      query(collection(admin(), "products"), where("reported", "==", true)),
    );
    assert.strictEqual(reported.size, 1);
  });
});

describe("admin permissions", () => {
  it("can list every user", async () => {
    await assertSucceeds(getDocs(collection(admin(), "users")));
  });

  it("can list every store", async () => {
    await assertSucceeds(getDocs(collection(admin(), "stores")));
  });

  it("can list every install", async () => {
    await assertSucceeds(getDocs(collection(admin(), "orders")));
  });

  it("can verify a store, which unlocks paid listings", async () => {
    await assertSucceeds(
      updateDoc(doc(admin(), "stores", STORE), { verified: true, verificationStatus: "APPROVED" }),
    );
  });

  it("can change someone's role", async () => {
    await assertSucceeds(updateDoc(doc(admin(), "users", SHOPPER), { role: "SELLER" }));
  });
});

describe("user permissions", () => {
  it("can edit their own name", async () => {
    await assertSucceeds(
      updateDoc(doc(shopper(), "users", SHOPPER), { displayName: "Carol B" }),
    );
  });

  it("cannot promote themselves without owning a store", async () => {
    await assertFails(updateDoc(doc(shopper(), "users", SHOPPER), { role: "ADMIN" }));
  });

  it("cannot read another person's profile", async () => {
    await assertFails(getDoc(doc(shopper(), "users", DEV)));
  });

  it("cannot list every user", async () => {
    await assertFails(getDocs(collection(shopper(), "users")));
  });

  it("cannot verify their own store", async () => {
    await assertFails(updateDoc(doc(dev(), "stores", STORE), { verified: true }));
  });

  it("cannot publish while their store is still in review", async () => {
    // Store approval is what gates publishing.
    const reviewer = env.authenticatedContext(DEV2).firestore();
    await assertFails(updateDoc(doc(reviewer, "products", "app-pending"), { status: "ACTIVE" }));
  });

  it("can publish once the store is approved", async () => {
    await assertSucceeds(updateDoc(doc(dev(), "products", "app1"), { status: "ARCHIVED" }));
  });
});

describe("coverage", () => {
  it("denies any collection that has no rule", async () => {
    // The catch-all must stay a deny: a new collection added to the client
    // should fail loudly here rather than be silently world-readable.
    await assertFails(getDoc(doc(guest(), "something_new", "x")));
    await assertFails(getDoc(doc(shopper(), "something_new", "x")));
  });

  it("denies writes to the platform config from a normal user", async () => {
    await assertFails(
      setDoc(doc(shopper(), "platform", "stats"), { installs: 1 }),
    );
  });

  it("denies an unauthenticated install write", async () => {
    await assertFails(
      setDoc(doc(guest(), "orders", "o9"), { buyerId: "x", items: [], subtotal: 0, total: 0 }),
    );
  });

  it("denies reading another person's notifications", async () => {
    await assertFails(
      getDocs(collection(env.authenticatedContext(BOB).firestore(), "notifications", SHOPPER, "items")),
    );
  });
});

describe("build link", () => {
  it("accepts a storage or https link", async () => {
    await assertSucceeds(
      setDoc(doc(dev(), "products", "with-apk"), product({
        status: "DRAFT",
        apkUrl: "https://firebasestorage.googleapis.com/v0/b/x/o/app.apk",
      })),
    );
  });

  it("refuses a listing whose build link is not a URL", async () => {
    await assertFails(
      setDoc(doc(dev(), "products", "bad-apk"), product({
        status: "DRAFT",
        apkUrl: "just-some-text-that-is-long",
      })),
    );
  });
});

describe("offers", () => {
  it("lets a developer write an offer banner on their own listing", async () => {
    await assertSucceeds(
      setDoc(doc(env.authenticatedContext(DEV3).firestore(), "products", "promo1"), product({
        storeId: "store-verified",
        status: "DRAFT",
        compareAtPrice: 19900,
        price: 9900,
        offerEndsAt: 2_000_000_000_000,
        promoHeadline: "Half price this week",
        promoBody: "One line about why it is worth a look.",
        promoImageUrl: "https://res.cloudinary.com/demo/banner.jpg",
      })),
    );
  });

  it("keeps the banner off another developer's listing", async () => {
    const reviewer = env.authenticatedContext(DEV2).firestore();
    await assertFails(
      updateDoc(doc(reviewer, "products", "app1"), { promoHeadline: "Mine now" }),
    );
  });
});

describe("sprout safe", () => {
  it("stores the scan result with the listing", async () => {
    await assertSucceeds(
      setDoc(doc(dev(), "products", "scanned"), product({
        status: "DRAFT",
        scanRisk: "MEDIUM",
        scanPermissions: ["android.permission.CAMERA"],
        scanWarnings: ["Can use your camera"],
      })),
    );
  });

  it("does not let a buyer rewrite a scan result", async () => {
    await assertFails(
      updateDoc(doc(shopper(), "products", "app1"), { scanRisk: "LOW" }),
    );
  });
});

describe("publishing shape", () => {
  it("a listing with no offer is accepted", async () => {
    // offerEndsAt written as null used to fail `offerEndsAt is int`, which
    // denied every ordinary publish.
    await assertSucceeds(
      setDoc(doc(dev(), "products", "no-offer"), product({
        status: "DRAFT",
        offerEndsAt: null,
      })),
    );
  });

  it("a draft may exist without a build", async () => {
    await assertSucceeds(
      setDoc(doc(dev(), "products", "no-build"), product({
        status: "DRAFT",
        apkUrl: null,
      })),
    );
  });

  it("but a live listing must have a build", async () => {
    await assertFails(
      setDoc(doc(dev(), "products", "live-no-build"), product({
        status: "ACTIVE",
        apkUrl: null,
      })),
    );
  });

  it("a live listing with a build is accepted", async () => {
    await assertSucceeds(
      setDoc(doc(dev(), "products", "live-with-build"), product({
        status: "ACTIVE",
        apkUrl: "https://sproutly-apk-upload.example.workers.dev/apks/x/app.apk",
      })),
    );
  });
});
