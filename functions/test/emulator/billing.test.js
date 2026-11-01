/**
 * Transactional billing tests. Run against the Firestore emulator:
 *   npm run test:emulator
 */
const { test, before, beforeEach } = require("node:test");
const assert = require("node:assert");

process.env.GCLOUD_PROJECT = process.env.GCLOUD_PROJECT || "sproutly-functions-test";

const { db } = require("../../lib/config");
const { creditCapturedPayment } = require("../../lib/payments");
const { serveAds } = require("../../lib/campaigns");

before(() => {
  assert.ok(process.env.FIRESTORE_EMULATOR_HOST, "run through `firebase emulators:exec --only firestore`");
});

async function reset() {
  for (const name of ["payments", "advertising_campaigns", "campaign_events", "admin_audit_logs"]) {
    const snap = await db.collection(name).get();
    await Promise.all(snap.docs.map((d) => d.ref.delete()));
  }
}
beforeEach(reset);

async function seedCampaign(extra = {}) {
  await db.collection("advertising_campaigns").doc("c1").set({
    ownerId: "owner", applicationId: "app-1", name: "Launch", status: "pending_approval",
    budget: 0, requestedBudget: 100000, spend: 0, impressions: 0, clicks: 0,
    placements: ["sponsoredCard"], targeting: {}, creative: { headline: "Hi" },
    endDate: new Date(Date.now() + 7 * 864e5), startDate: new Date(Date.now() - 864e5),
    ...extra,
  });
}
async function seedPayment(id = "order_1", extra = {}) {
  await db.collection("payments").doc(id).set({
    provider: "razorpay", status: "created", userId: "owner", campaignId: "c1",
    amountPaise: 100000, currency: "INR", ...extra,
  });
}
const credit = (extra = {}) =>
  creditCapturedPayment({
    paymentId: "order_1", provider: "razorpay", amountPaise: 100000, currency: "INR",
    providerFields: { razorpayPaymentId: "pay_1" }, ...extra,
  });

test("concurrent deliveries of one webhook credit exactly once", async () => {
  await seedCampaign();
  await seedPayment();
  const results = await Promise.all([credit(), credit(), credit(), credit(), credit()]);
  assert.strictEqual(results.filter((r) => r === "credited").length, 1);
  assert.strictEqual(results.filter((r) => r === "duplicate").length, 4);
  const campaign = (await db.collection("advertising_campaigns").doc("c1").get()).data();
  assert.strictEqual(campaign.budget, 100000);
  assert.strictEqual(campaign.status, "pending_approval", "funding must not approve a campaign");
});

test("a capture with the wrong amount is flagged, not credited", async () => {
  await seedCampaign();
  await seedPayment();
  assert.strictEqual(await credit({ amountPaise: 1 }), "amount_mismatch");
  assert.strictEqual((await db.collection("advertising_campaigns").doc("c1").get()).data().budget, 0);
  assert.strictEqual((await db.collection("payments").doc("order_1").get()).data().status, "flagged");
});

test("an unknown order credits nothing", async () => {
  await seedCampaign();
  assert.strictEqual(await credit({ paymentId: "order_missing" }), "unknown_payment");
  assert.strictEqual((await db.collection("advertising_campaigns").doc("c1").get()).data().budget, 0);
});

test("topping up an approved, exhausted campaign reactivates it", async () => {
  await seedCampaign({ status: "exhausted", approvedAt: new Date(), budget: 100, spend: 100 });
  await seedPayment();
  assert.strictEqual(await credit(), "credited");
  assert.strictEqual((await db.collection("advertising_campaigns").doc("c1").get()).data().status, "active");
});

const serve = (uid) =>
  serveAds.run({ auth: { uid, token: {} }, data: { placement: "sponsoredCard", limit: 3 }, rawRequest: { headers: {} } });

test("concurrent ad requests can never spend more than the funded balance", async () => {
  // 12 paise per impression; fund exactly 5 impressions.
  await seedCampaign({ status: "active", budget: 60, approvedAt: new Date() });
  await Promise.all(Array.from({ length: 30 }, (_, i) => serve(`user-${i}`)));
  const campaign = (await db.collection("advertising_campaigns").doc("c1").get()).data();
  assert.ok(campaign.spend <= campaign.budget, `spend ${campaign.spend} exceeds budget ${campaign.budget}`);
  assert.strictEqual(campaign.impressions * 12, campaign.spend);
  assert.strictEqual(campaign.impressions, 5);
  assert.strictEqual(campaign.status, "exhausted");
});

test("one user is billed at most once per campaign per hour", async () => {
  await seedCampaign({ status: "active", budget: 100000, approvedAt: new Date() });
  await Promise.all(Array.from({ length: 10 }, () => serve("same-user")));
  assert.strictEqual((await db.collection("advertising_campaigns").doc("c1").get()).data().impressions, 1);
});

test("an advertiser is never billed for their own browsing", async () => {
  await seedCampaign({ status: "active", budget: 100000, approvedAt: new Date() });
  await serve("owner");
  assert.strictEqual((await db.collection("advertising_campaigns").doc("c1").get()).data().impressions, 0);
});

test("targeted campaigns only serve to matching users", async () => {
  await seedCampaign({ status: "active", budget: 100000, approvedAt: new Date(), targeting: { platforms: ["ios"] } });
  await serveAds.run({ auth: { uid: "u1", token: {} }, data: { placement: "sponsoredCard", platform: "android" }, rawRequest: { headers: {} } });
  assert.strictEqual((await db.collection("advertising_campaigns").doc("c1").get()).data().impressions, 0);
  await serveAds.run({ auth: { uid: "u2", token: {} }, data: { placement: "sponsoredCard", platform: "ios" }, rawRequest: { headers: {} } });
  assert.strictEqual((await db.collection("advertising_campaigns").doc("c1").get()).data().impressions, 1);
});
