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
const { doc, getDoc, setDoc, updateDoc, deleteDoc, collection, addDoc } = require("firebase/firestore");

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
    clickCount: 0,
    bookmarkCount: 0,
    sponsored: false,
    featured: false,
    createdAt: new Date(),
    updatedAt: new Date(),
    ...overrides,
  };
}

test("a stranger cannot create a listing owned by someone else", async () => {
  await assertFails(
    setDoc(doc(stranger(), "applications/forged"), applicationDoc()),
  );
});

test("an owner can create a draft with zeroed counters", async () => {
  await assertSucceeds(
    setDoc(doc(owner(), "applications/mine"), applicationDoc()),
  );
});

test("an owner cannot seed their own rating or view count", async () => {
  await assertFails(
    setDoc(doc(owner(), "applications/cheat"), applicationDoc({ rating: 5, viewCount: 9999 })),
  );
});

test("an owner cannot move their own listing straight to approved", async () => {
  await assertFails(
    updateDoc(doc(owner(), "applications/mine"), { status: "approved" }),
  );
});

test("an owner may submit a draft for review", async () => {
  await assertSucceeds(
    updateDoc(doc(owner(), "applications/mine"), { status: "pending_review", updatedAt: new Date() }),
  );
});

test("an owner cannot rewrite their own counters", async () => {
  await assertFails(
    updateDoc(doc(owner(), "applications/mine"), { viewCount: 5000 }),
  );
});

test("a stranger cannot edit someone else's listing", async () => {
  await assertFails(
    updateDoc(doc(stranger(), "applications/mine"), { name: "Hijacked" }),
  );
});

test("an admin can moderate", async () => {
  await assertSucceeds(
    updateDoc(doc(admin(), "applications/mine"), { status: "approved" }),
  );
});

test("drafts are not readable by strangers", async () => {
  await assertFails(getDoc(doc(stranger(), "applications/mine")));
});

test("bookmarks cannot be written for another user", async () => {
  await assertFails(
    setDoc(doc(stranger(), "bookmarks/owner-1_app-1"), {
      userId: "owner-1",
      applicationId: "app-1",
      createdAt: new Date(),
    }),
  );
});

test("a user can bookmark with the composite id", async () => {
  await assertSucceeds(
    setDoc(doc(owner(), "bookmarks/owner-1_app-1"), {
      userId: "owner-1",
      applicationId: "app-1",
      createdAt: new Date(),
    }),
  );
});

test("users cannot promote themselves", async () => {
  await assertSucceeds(
    setDoc(doc(owner(), "users/owner-1"), {
      uid: "owner-1",
      displayName: "Owner",
      role: "user",
      isDeveloper: false,
      isPublic: true,
      createdAt: new Date(),
      updatedAt: new Date(),
    }),
  );
  await assertFails(
    updateDoc(doc(owner(), "users/owner-1"), { role: "admin" }),
  );
});

test("campaign events are not writable by clients", async () => {
  await assertFails(
    addDoc(collection(owner(), "campaign_events"), { campaignId: "c1", kind: "impression" }),
  );
});

test("notifications are readable only by their recipient", async () => {
  await env.withSecurityRulesDisabled(async (context) => {
    await setDoc(doc(context.firestore(), "notifications/n1"), {
      userId: "owner-1",
      type: "system",
      title: "Hi",
      body: "There",
      createdAt: new Date(),
    });
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
