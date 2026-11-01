import { onCall, onRequest, HttpsError } from "firebase-functions/v2/https";
import { defineSecret } from "firebase-functions/params";
import Razorpay from "razorpay";
import Stripe from "stripe";
import { db, COLLECTIONS, FieldValue, Timestamp } from "./config";
import { LIMITS } from "./config";
import { audit, requireUser } from "./util";

const razorpayKeyId = defineSecret("RAZORPAY_KEY_ID");
const razorpayKeySecret = defineSecret("RAZORPAY_KEY_SECRET");
const razorpayWebhookSecret = defineSecret("RAZORPAY_WEBHOOK_SECRET");
const stripeSecretKey = defineSecret("STRIPE_SECRET_KEY");
const stripeWebhookSecret = defineSecret("STRIPE_WEBHOOK_SECRET");

const secrets = [razorpayKeyId, razorpayKeySecret, razorpayWebhookSecret, stripeSecretKey, stripeWebhookSecret];

function razorpay(): Razorpay {
  return new Razorpay({ key_id: razorpayKeyId.value(), key_secret: razorpayKeySecret.value() });
}

function stripe(): Stripe {
  return new Stripe(stripeSecretKey.value());
}

async function assertCampaignOwnership(campaignId: string, uid: string): Promise<void> {
  const snap = await db.collection(COLLECTIONS.campaigns).doc(campaignId).get();
  if (!snap.exists) throw new HttpsError("not-found", "Unknown campaign.");
  if (snap.data()!.ownerId !== uid) {
    throw new HttpsError("permission-denied", "You can only fund your own campaign.");
  }
}

/**
 * Creates a Razorpay order. The amount is validated here against the platform
 * floor and ceiling, so a tampered client cannot pay one rupee for ten thousand
 * impressions.
 */
export const createRazorpayOrder = onCall({ secrets }, async (request) => {
  const ctx = await requireUser(request);
  const campaignId = request.data?.campaignId;
  const amountPaise = Number(request.data?.amountPaise);

  if (typeof campaignId !== "string") throw new HttpsError("invalid-argument", "campaignId is required.");
  if (!Number.isInteger(amountPaise) || amountPaise < LIMITS.minCampaignBudgetPaise || amountPaise > LIMITS.maxCampaignBudgetPaise) {
    throw new HttpsError("invalid-argument", "That amount is outside the allowed range.");
  }

  await assertCampaignOwnership(campaignId, ctx.uid);

  const order = await razorpay().orders.create({
    amount: amountPaise,
    currency: "INR",
    receipt: `sproutly_${campaignId}_${Date.now()}`,
    notes: { campaignId, uid: ctx.uid },
  });

  await db.collection(COLLECTIONS.payments).doc(order.id).set({
    provider: "razorpay",
    status: "created",
    userId: ctx.uid,
    campaignId,
    amountPaise,
    currency: "INR",
    createdAt: FieldValue.serverTimestamp(),
  });

  return { orderId: order.id, amountPaise, currency: "INR" };
});

/**
 * Razorpay webhook. This - not the client - is what credits a campaign budget.
 * Signature verification is mandatory; an unsigned request is dropped.
 */
export const razorpayWebhook = onRequest({ secrets }, async (req, res) => {
  const signature = req.headers["x-razorpay-signature"];
  const raw = req.rawBody.toString();

  const crypto = await import("node:crypto");
  const expected = crypto
    .createHmac("sha256", razorpayWebhookSecret.value())
    .update(raw)
    .digest("hex");

  if (typeof signature !== "string" || signature !== expected) {
    res.status(400).send("invalid signature");
    return;
  }

  const event = req.body?.event;
  const payment = req.body?.payload?.payment?.entity;

  if (event === "payment.captured" && payment) {
    const paymentRef = db.collection(COLLECTIONS.payments).doc(payment.order_id);
    const existing = await paymentRef.get();
    if (existing.exists && existing.data()?.status === "captured") {
      res.status(200).send("already processed");
      return;
    }

    const campaignId = payment.notes?.campaignId ?? existing.data()?.campaignId;
    const amountPaise = Number(payment.amount ?? 0);

    await paymentRef.set(
      {
        status: "captured",
        capturedAt: FieldValue.serverTimestamp(),
        razorpayPaymentId: payment.id,
      },
      { merge: true },
    );

    if (campaignId) {
      await db.collection(COLLECTIONS.campaigns).doc(campaignId).update({
        budget: FieldValue.increment(amountPaise),
        updatedAt: FieldValue.serverTimestamp(),
        status: "pending_approval",
      });
    }
  }

  res.status(200).send("ok");
});

/** Stripe Checkout for international cards. Same rules: server decides amounts. */
export const createStripeCheckout = onCall({ secrets }, async (request) => {
  const ctx = await requireUser(request);
  const campaignId = request.data?.campaignId;
  const amountPaise = Number(request.data?.amountPaise);

  if (typeof campaignId !== "string") throw new HttpsError("invalid-argument", "campaignId is required.");
  if (!Number.isInteger(amountPaise) || amountPaise < LIMITS.minCampaignBudgetPaise || amountPaise > LIMITS.maxCampaignBudgetPaise) {
    throw new HttpsError("invalid-argument", "That amount is outside the allowed range.");
  }
  await assertCampaignOwnership(campaignId, ctx.uid);

  const session = await stripe().checkout.sessions.create({
    mode: "payment",
    line_items: [
      {
        price_data: {
          currency: "inr",
          product_data: { name: "Sproutly campaign budget" },
          unit_amount: amountPaise,
        },
        quantity: 1,
      },
    ],
    metadata: { campaignId, uid: ctx.uid },
    success_url: "https://sproutly.app/campaign/" + campaignId,
    cancel_url: "https://sproutly.app/campaign/" + campaignId,
  });

  await db.collection(COLLECTIONS.payments).doc(session.id).set({
    provider: "stripe",
    status: "created",
    userId: ctx.uid,
    campaignId,
    amountPaise,
    currency: "INR",
    createdAt: FieldValue.serverTimestamp(),
  });

  return { url: session.url, sessionId: session.id };
});

export const stripeWebhook = onRequest({ secrets }, async (req, res) => {
  const signature = req.headers["stripe-signature"];
  if (typeof signature !== "string") {
    res.status(400).send("missing signature");
    return;
  }

  let event: Stripe.Event;
  try {
    event = stripe().webhooks.constructEvent(req.rawBody, signature, stripeWebhookSecret.value());
  } catch {
    res.status(400).send("invalid signature");
    return;
  }

  if (event.type === "checkout.session.completed") {
    const session = event.data.object as Stripe.Checkout.Session;
    const campaignId = session.metadata?.campaignId;
    const paymentRef = db.collection(COLLECTIONS.payments).doc(session.id);
    const existing = await paymentRef.get();
    if (existing.data()?.status === "captured") {
      res.status(200).send("already processed");
      return;
    }

    await paymentRef.set(
      {
        status: "captured",
        capturedAt: FieldValue.serverTimestamp(),
        stripePaymentIntent: String(session.payment_intent ?? ""),
      },
      { merge: true },
    );

    if (campaignId) {
      await db.collection(COLLECTIONS.campaigns).doc(campaignId).update({
        budget: FieldValue.increment(Number(session.amount_total ?? 0)),
        status: "pending_approval",
        updatedAt: Timestamp.now(),
      });
    }
    await audit("system", "stripe_capture", session.id, { campaignId });
  }

  res.status(200).send("ok");
});
