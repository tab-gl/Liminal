/** Payment-capture validation. Pure, so it can be unit tested without Firebase. */

export type CaptureVerdict = "ok" | "duplicate" | "provider_mismatch" | "amount_mismatch" | "currency_mismatch";

export interface StoredPayment {
  provider?: unknown;
  status?: unknown;
  amountPaise?: unknown;
  currency?: unknown;
}

export interface ProviderCapture {
  provider: "razorpay" | "stripe";
  amountPaise: number;
  currency: string;
}

/**
 * Compares what the provider says was captured with the order the server
 * created. Nothing the client or the webhook payload claims about the campaign
 * or the amount is trusted over the stored record.
 */
export function validateCapture(stored: StoredPayment, capture: ProviderCapture): CaptureVerdict {
  if (stored.status !== "created") return "duplicate";
  if (stored.provider !== capture.provider) return "provider_mismatch";
  if (!Number.isInteger(capture.amountPaise) || stored.amountPaise !== capture.amountPaise) {
    return "amount_mismatch";
  }
  if (String(stored.currency ?? "").toUpperCase() !== capture.currency.toUpperCase()) {
    return "currency_mismatch";
  }
  return "ok";
}

/** Funded balance still spendable on a campaign, never negative. */
export function remainingPaise(campaign: Record<string, any>): number {
  return Math.max(Number(campaign.budget ?? 0) - Number(campaign.spend ?? 0), 0);
}
