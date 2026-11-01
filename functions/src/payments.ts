import { onCall, onRequest, HttpsError } from "firebase-functions/v2/https";
import { defineSecret } from "firebase-functions/params";
import { createHmac, timingSafeEqual } from "node:crypto";
import Razorpay from "razorpay";
import Stripe from "stripe";
import { db, COLLECTIONS, FieldValue } from "./config";
import { LIMITS } from "./config";
import { validateCapture, type CaptureVerdict } from "./billing";
import { requireUser } from "./util";

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

/** Campaigns that can still receive funds. */
const FUNDABLE = ["pending_approval", "active", "paused", "exhausted"];

async function assertFundable(campaignId: string, uid: string): Promise<void> {
  const snap = await db.collection(COLLECTIONS.campaigns).doc(campaignId).get();
  if (!snap.exists) throw new HttpsError("not-found", "Unknown campaign.");
  const campaign = snap.data()!;
  if (campaign.ownerId !== uid) {
    throw new HttpsError("permission-denied", "You can only fund your own campaign.");
  }
  if (!FUNDABLE.includes(campaign.status)) {
    throw new HttpsError("failed-precondition", "This campaign can no longer be funded.");
  }
}

function assertAmount(amountPaise: number): void {
  if (
    !Number.isInteger(amountPaise) ||
    amountPaise < LIMITS.minCampaignBudgetPaise ||
    amountPaise > LIMITS.maxCampaignBudgetPaise
  ) {
    throw new HttpsError("invalid-argument", "That amount is outside the allowed range.");
  }
}

export type CreditResult = CaptureVerdict | "unknown_payment" | "unknown_campaign" | "credited";

/**
 * The only place a campaign balance is ever increased.
 *
 * Runs in one transaction: the payment is re-read, validated against the order
 * the server created (provider, amount, currency, status), and then the payment
 * is marked captured and the stored campaign credited together. A second,
 * concurrent delivery of the same webhook sees `captured` after the first
 * commits (Firestore retries the loser) and credits nothing. The campaign id
 * comes from the stored order, never from the webhook payload.
 */
export async function creditCapturedPayment(input: {
  paymentId: string;
  provider: "razorpay" | "stripe";
  amountPaise: number;
  currency: string;
  providerFields: Record<string, unknown>;
}): Promise<CreditResult> {
  const paymentRef = db.collection(COLLECTIONS.payments).doc(input.paymentId);

  return db.runTransaction(async (tx): Promise<CreditResult> => {
    const paymentSnap = await tx.get(paymentRef);
    if (!paymentSnap.exists) return "unknown_payment";
    const payment = paymentSnap.data()!;

    const verdict = validateCapture(payment, {
      provider: input.provider,
      amountPaise: input.amountPaise,
      currency: input.currency,
    });
    if (verdict === "duplicate") return "duplicate";
    if (verdict !== "ok") {
      tx.update(paymentRef, {
        status: "flagged",
        flagReason: verdict,
        flaggedAt: FieldValue.serverTimestamp(),
      });
      return verdict;
    }

    const campaignRef = db.collection(COLLECTIONS.campaigns).doc(String(payment.campaignId));
    const campaignSnap = await tx.get(campaignRef);
    if (!campaignSnap.exists) {
      tx.update(paymentRef, {
        status: "flagged",
        flagReason: "unknown_campaign",
        flaggedAt: FieldValue.serverTimestamp(),
      });
      return "unknown_campaign";
    }
    const campaign = campaignSnap.data()!;

    tx.update(paymentRef, {
      ...input.providerFields,
      status: "captured",
      capturedAt: FieldValue.serverTimestamp(),
    });

    const update: Record<string, unknown> = {
      budget: FieldValue.increment(input.amountPaise),
      updatedAt: FieldValue.serverTimestamp(),
    };
    // Topping up a campaign that ran dry brings it back, but only if a moderator
    // already approved it and it has not finished. Everything else keeps its
    // status: funding never approves a campaign and never pauses a live one.
    const end = campaign.endDate?.toDate?.();
    if (campaign.status === "exhausted" && campaign.approvedAt && (!end || end > new Date())) {
      update.status = "active";
    }
    tx.update(campaignRef, update);

    tx.create(db.collection(COLLECTIONS.audit).doc(), {
      actorUid: "system",
      action: `${input.provider}_capture`,
      target: input.paymentId,
      detail: { campaignId: payment.campaignId, amountPaise: input.amountPaise },
      createdAt: FieldValue.serverTimestamp(),
    });
    return "credited";
  });
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
  assertAmount(amountPaise);
  await assertFundable(campaignId, ctx.uid);

  const order = await razorpay().orders.create({
    amount: amountPaise,
    currency: "INR",
    receipt: `sproutly_${campaignId}_${Date.now()}`.slice(0, 40),
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

function validSignature(raw: string, signature: unknown, secret: string): boolean {
  if (typeof signature !== "string") return false;
  const expected = createHmac("sha256", secret).update(raw).digest("hex");
  const a = Buffer.from(signature, "utf8");
  const b = Buffer.from(expected, "utf8");
  return a.length === b.length && timingSafeEqual(a, b);
}

/**
 * Razorpay webhook. This - not the client - is what credits a campaign budget.
 * Signature verification is mandatory; an unsigned request is dropped.
 *
 * Permanent outcomes (credited, duplicate, mismatched, unknown) answer 200 so the
 * provider stops retrying. A thrown error answers 500 so it retries.
 */
export const razorpayWebhook = onRequest({ secrets }, async (req, res) => {
  const raw = req.rawBody.toString();
  if (!validSignature(raw, req.headers["x-razorpay-signature"], razorpayWebhookSecret.value())) {
    res.status(400).send("invalid signature");
    return;
  }

  try {
    const event = req.body?.event;
    const payment = req.body?.payload?.payment?.entity;

    if (event === "payment.captured" && payment?.order_id && payment.status === "captured") {
      const result = await creditCapturedPayment({
        paymentId: String(payment.order_id),
        provider: "razorpay",
        amountPaise: Number(payment.amount),
        currency: String(payment.currency ?? ""),
        providerFields: { razorpayPaymentId: String(payment.id ?? "") },
      });
      res.status(200).send(result);
      return;
    }
    res.status(200).send("ignored");
  } catch (error) {
    console.error("razorpayWebhook failed", error);
    res.status(500).send("retry");
  }
});

/** Stripe Checkout for international cards. Same rules: server decides amounts. */
export const createStripeCheckout = onCall({ secrets }, async (request) => {
  const ctx = await requireUser(request);
  const campaignId = request.data?.campaignId;
  const amountPaise = Number(request.data?.amountPaise);

  if (typeof campaignId !== "string") throw new HttpsError("invalid-argument", "campaignId is required.");
  assertAmount(amountPaise);
  await assertFundable(campaignId, ctx.uid);

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

  try {
    if (event.type === "checkout.session.completed" || event.type === "checkout.session.async_payment_succeeded") {
      const session = event.data.object as Stripe.Checkout.Session;
      // `completed` fires before delayed payment methods settle; only credit paid sessions.
      if (session.payment_status === "paid") {
        const result = await creditCapturedPayment({
          paymentId: session.id,
          provider: "stripe",
          amountPaise: Number(session.amount_total ?? 0),
          currency: String(session.currency ?? ""),
          providerFields: { stripePaymentIntent: String(session.payment_intent ?? "") },
        });
        res.status(200).send(result);
        return;
      }
    }
    res.status(200).send("ignored");
  } catch (error) {
    console.error("stripeWebhook failed", error);
    res.status(500).send("retry");
  }
});
