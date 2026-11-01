/**
 * Security rule tests.
 *
 * These run against the Firestore emulator and assert the *negative* cases -
 * the ones that matter. A rule that allows everything passes a happy-path test;
 * it fails these.
 *
 *   cd test/rules && npm install
 *   npx firebase emulators:exec --only firestore "node --test"
 */
const { test, before, after } = require("node:test");
const assert = require("node:assert");
const fs = require("node:fs");
const path = require("node:path");
const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const {
  doc, getDoc, setDoc, updateDoc, deleteDoc, collection, addDoc, serverTimestamp,
} = require("firebase/firestore");

let env;

before(async () => {
  env = await initializeTestEnvironment({
    projectId: "sproutly-rules-test",
    firestore: {
      rules: fs.readFileSync(path.resolve(__dirname, "../../firestore.rules"), "utf8"),
      host: "127.0.0.1",
      port: 8080,
    },
  });
});

after(async () => {
  await env.cleanup();
});

const owner = () => env.authenticatedContext("owner-1", { role: "user" }).firestore();
const stranger = () => env.authenticatedContext("stranger-1", { role: "user" }).firestore();
const admin = () => env.authenticatedContext("admin-1", { role: "admin" }).firestore();
const anon = () => env.unauthenticatedContext().firestore();

function applicationDoc(overrides = {}) {
  return {
    ownerId: "owner-1",
    ownerName: "Owner One",
    name: "Test app",
    tagline: "A tagline",
    description: "x".repeat(80),
    categoryId: "productivity",
    status: "draft",
    rating: 0,
    reviewCount: 0,
    viewCount: 0,
    uniqueViewerCount: 0,
    clickCount: 0,
    downloadCount: 0,
    bookmarkCount: 0,
    trendingScore: 0,
    sponsored: false,
    featured: false,
    search: { title: "test app", keywords: ["test"] },
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
    ...overrides,
  };
}

/** Seeds documents bypassing rules, for tests that start from an existing state. */
async function seed(path, data) {
  await env.withSecurityRulesDisabled(async (ctx) => {
    await setDoc(doc(ctx.firestore(), path), data);
  });
}

// ------------------------------------------------------------- applications
test("a stranger cannot create a listing owned by someone else", async () => {
  await assertFails(setDoc(doc(stranger(), "applications/forged"), applicationDoc()));
});

test("an owner can create a draft with zeroed counters", async () => {
  await assertSucceeds(setDoc(doc(owner(), "applications/mine"), applicationDoc()));
});

test("an owner cannot seed ranking or analytics fields at creation", async () => {
  for (const field of [
    { rating: 5 }, { viewCount: 9999 }, { uniqueViewerCount: 50 },
    { downloadCount: 100 }, { trendingScore: 9999 }, { clickCount: 10 },
  ]) {
    await assertFails(setDoc(doc(owner(), "applications/cheat"), applicationDoc(field)));
  }
});

test("creation rejects unknown server-owned fields", async () => {
  await assertFails(
    setDoc(doc(owner(), "applications/cheat"), applicationDoc({ lastActivityAt: new Date() })),
  );
  await assertFails(
    setDoc(doc(owner(), "applications/cheat"), applicationDoc({ moderation: { state: "approved" } })),
  );
});

test("an owner cannot move their own listing straight to approved", async () => {
  await assertFails(updateDoc(doc(owner(), "applications/mine"), { status: "approved" }));
});

test("an owner may submit a draft for review", async () => {
  await assertSucceeds(
    updateDoc(doc(owner(), "applications/mine"), {
      status: "pending_review", updatedAt: serverTimestamp(),
    }),
  );
});

test("an owner cannot rewrite counters, ranking or moderation fields", async () => {
  await seed("applications/live", applicationDoc({ status: "approved", createdAt: new Date() }));
  for (const field of [
    { viewCount: 5000 }, { trendingScore: 99999 }, { lastActivityAt: new Date() },
    { downloadCount: 10 }, { uniqueViewerCount: 10 }, { featured: true }, { sponsored: true },
    { moderation: { state: "approved" } },
  ]) {
    await assertFails(
      updateDoc(doc(owner(), "applications/live"), { ...field, updatedAt: serverTimestamp() }),
    );
  }
});

test("an owner can edit listing content including the search index", async () => {
  await assertSucceeds(
    updateDoc(doc(owner(), "applications/live"), {
      name: "Renamed",
      search: { title: "renamed", keywords: ["renamed"] },
      updatedAt: serverTimestamp(),
    }),
  );
});

test("a stranger cannot edit someone else's listing", async () => {
  await assertFails(updateDoc(doc(stranger(), "applications/mine"), { name: "Hijacked" }));
});

test("an admin can moderate", async () => {
  await assertSucceeds(updateDoc(doc(admin(), "applications/mine"), { status: "approved" }));
});

test("drafts are not readable by strangers", async () => {
  await seed("applications/secret-draft", applicationDoc({ createdAt: new Date() }));
  await assertFails(getDoc(doc(stranger(), "applications/secret-draft")));
});

// ------------------------------------------------------------------ reviews
test("review updates enforce rating range and body length", async () => {
  await seed("reviews/live_owner-1", {
    userId: "owner-1", applicationId: "live", status: "published",
    rating: 4, body: "ok", createdAt: new Date(),
  });
  await assertFails(updateDoc(doc(owner(), "reviews/live_owner-1"), { rating: 99, updatedAt: serverTimestamp() }));
  await assertFails(updateDoc(doc(owner(), "reviews/live_owner-1"), { rating: 0, updatedAt: serverTimestamp() }));
  await assertFails(updateDoc(doc(owner(), "reviews/live_owner-1"), { rating: 4.5, updatedAt: serverTimestamp() }));
  await assertFails(updateDoc(doc(owner(), "reviews/live_owner-1"), { body: "x".repeat(2001), updatedAt: serverTimestamp() }));
  await assertFails(updateDoc(doc(owner(), "reviews/live_owner-1"), { body: 123, updatedAt: serverTimestamp() }));
  await assertSucceeds(updateDoc(doc(owner(), "reviews/live_owner-1"), { rating: 5, body: "better", updatedAt: serverTimestamp() }));
});

// -------------------------------------------------------- application events
test("clients cannot write application events directly", async () => {
  await assertFails(
    addDoc(collection(owner(), "application_events"), {
      applicationId: "live", userId: "owner-1", type: "view", createdAt: serverTimestamp(),
    }),
  );
  await assertFails(
    addDoc(collection(stranger(), "application_events"), {
      applicationId: "anything", userId: "stranger-1", type: "click", createdAt: serverTimestamp(),
    }),
  );
});

// ---------------------------------------------------------------- campaigns
function campaignDoc(overrides = {}) {
  return {
    ownerId: "owner-1",
    applicationId: "live",
    name: "Launch",
    objective: "installs",
    budget: 0,
    requestedBudget: 100000,
    spend: 0,
    impressions: 0,
    clicks: 0,
    status: "pending_approval",
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
    ...overrides,
  };
}

test("a new campaign must start with zero funded balance", async () => {
  await assertSucceeds(setDoc(doc(owner(), "advertising_campaigns/c-ok"), campaignDoc()));
  await assertFails(setDoc(doc(owner(), "advertising_campaigns/c-free"), campaignDoc({ budget: 100000 })));
  await assertFails(setDoc(doc(owner(), "advertising_campaigns/c-free"), campaignDoc({ budget: 1 })));
});

test("a requested budget outside the platform limits is rejected", async () => {
  await assertFails(setDoc(doc(owner(), "advertising_campaigns/c-low"), campaignDoc({ requestedBudget: 500 })));
  await assertFails(setDoc(doc(owner(), "advertising_campaigns/c-high"), campaignDoc({ requestedBudget: 50000001 })));
});

test("a campaign cannot be created for an app the caller does not own", async () => {
  await assertFails(setDoc(doc(stranger(), "advertising_campaigns/c-theirs"), campaignDoc({ ownerId: "stranger-1" })));
});

test("owners can pause and resume but not change money or approve", async () => {
  await seed("advertising_campaigns/live-c", campaignDoc({
    status: "active", budget: 100000, createdAt: new Date(), updatedAt: new Date(),
  }));
  await assertSucceeds(updateDoc(doc(owner(), "advertising_campaigns/live-c"), { status: "paused", updatedAt: serverTimestamp() }));
  await assertSucceeds(updateDoc(doc(owner(), "advertising_campaigns/live-c"), { status: "active", updatedAt: serverTimestamp() }));
  await assertFails(updateDoc(doc(owner(), "advertising_campaigns/live-c"), { budget: 99999999, updatedAt: serverTimestamp() }));
  await assertFails(updateDoc(doc(owner(), "advertising_campaigns/live-c"), { spend: 0, updatedAt: serverTimestamp() }));
  await assertFails(updateDoc(doc(owner(), "advertising_campaigns/live-c"), { status: "completed", updatedAt: serverTimestamp() }));
});

test("an owner cannot approve their own pending campaign", async () => {
  await seed("advertising_campaigns/pending-c", campaignDoc({ createdAt: new Date(), updatedAt: new Date() }));
  await assertFails(updateDoc(doc(owner(), "advertising_campaigns/pending-c"), { status: "active", updatedAt: serverTimestamp() }));
});

// ------------------------------------------------------------------ the rest
test("bookmarks cannot be written for another user", async () => {
  await assertFails(
    setDoc(doc(stranger(), "bookmarks/owner-1_app-1"), {
      userId: "owner-1", applicationId: "app-1", createdAt: serverTimestamp(),
    }),
  );
});

test("a user can bookmark with the composite id", async () => {
  await assertSucceeds(
    setDoc(doc(owner(), "bookmarks/owner-1_app-1"), {
      userId: "owner-1", applicationId: "app-1", createdAt: serverTimestamp(),
    }),
  );
});

test("users cannot promote themselves", async () => {
  await assertSucceeds(
    setDoc(doc(owner(), "users/owner-1"), {
      uid: "owner-1", displayName: "Owner", role: "user", isDeveloper: false,
      isPublic: true, createdAt: serverTimestamp(), updatedAt: serverTimestamp(),
    }),
  );
  await assertFails(updateDoc(doc(owner(), "users/owner-1"), { role: "admin" }));
});

test("campaign events and payments are not writable by clients", async () => {
  await assertFails(addDoc(collection(owner(), "campaign_events"), { campaignId: "c1", kind: "impression" }));
  await assertFails(setDoc(doc(owner(), "payments/order_1"), { status: "captured", userId: "owner-1" }));
  await assertFails(setDoc(doc(owner(), "event_markers/anything"), { createdAt: serverTimestamp() }));
});

test("notifications are readable only by their recipient", async () => {
  await seed("notifications/n1", {
    userId: "owner-1", type: "system", title: "Hi", body: "There", createdAt: new Date(),
  });
  await assertSucceeds(getDoc(doc(owner(), "notifications/n1")));
  await assertFails(getDoc(doc(stranger(), "notifications/n1")));
});

test("anonymous users cannot read anything but public categories", async () => {
  await assertFails(getDoc(doc(anon(), "users/owner-1")));
  await assertSucceeds(getDoc(doc(anon(), "categories/productivity")));
});

test("audit logs are admin-only and never client-writable", async () => {
  await assertFails(setDoc(doc(admin(), "admin_audit_logs/log-1"), { action: "fake" }));
  await assertSucceeds(getDoc(doc(admin(), "admin_audit_logs/log-1")));
});
