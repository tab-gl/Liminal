const { test } = require("node:test");
const assert = require("node:assert");
const { validateCapture, remainingPaise } = require("../lib/billing");

const stored = { provider: "razorpay", status: "created", amountPaise: 100000, currency: "INR" };
const capture = { provider: "razorpay", amountPaise: 100000, currency: "INR" };

test("a matching capture is accepted", () => {
  assert.strictEqual(validateCapture(stored, capture), "ok");
});

test("an already captured or flagged payment is a duplicate", () => {
  assert.strictEqual(validateCapture({ ...stored, status: "captured" }, capture), "duplicate");
  assert.strictEqual(validateCapture({ ...stored, status: "flagged" }, capture), "duplicate");
});

test("amount, currency and provider mismatches are never credited", () => {
  assert.strictEqual(validateCapture(stored, { ...capture, amountPaise: 1 }), "amount_mismatch");
  assert.strictEqual(validateCapture(stored, { ...capture, amountPaise: 100000.5 }), "amount_mismatch");
  assert.strictEqual(validateCapture(stored, { ...capture, currency: "USD" }), "currency_mismatch");
  assert.strictEqual(validateCapture(stored, { ...capture, provider: "stripe" }), "provider_mismatch");
});

test("currency comparison ignores case", () => {
  assert.strictEqual(validateCapture(stored, { ...capture, currency: "inr" }), "ok");
});

test("remaining balance is never negative", () => {
  assert.strictEqual(remainingPaise({ budget: 1000, spend: 400 }), 600);
  assert.strictEqual(remainingPaise({ budget: 1000, spend: 4000 }), 0);
  assert.strictEqual(remainingPaise({}), 0);
});
