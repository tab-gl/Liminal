import { CallableRequest, HttpsError } from "firebase-functions/v2/https";
import { getFirestore, FieldValue } from "firebase-admin/firestore";

export type Role = "CUSTOMER" | "SELLER" | "ADMIN";

export interface CallerProfile {
  uid: string;
  role: Role;
  storeId: string | null;
  email: string;
}

/**
 * Reads the caller's profile from Firestore.
 *
 * Roles are NEVER taken from the request payload or from a client-supplied
 * claim. The Firestore document is the single source of truth, and it can only
 * be written by an administrator through a trusted function.
 */
export async function requireCaller(request: CallableRequest): Promise<CallerProfile> {
  const uid = request.auth?.uid;
  if (!uid) {
    throw new HttpsError("unauthenticated", "Please sign in to continue.");
  }
  const snap = await getFirestore().collection("users").doc(uid).get();
  if (!snap.exists) {
    throw new HttpsError("permission-denied", "Your account profile is missing.");
  }
  const data = snap.data() ?? {};
  const role = (data.role as Role) ?? "CUSTOMER";
  return {
    uid,
    role,
    storeId: (data.storeId as string) ?? null,
    email: (data.email as string) ?? "",
  };
}

export async function requireAdmin(request: CallableRequest): Promise<CallerProfile> {
  const caller = await requireCaller(request);
  if (caller.role !== "ADMIN") {
    throw new HttpsError("permission-denied", "This action is restricted to Sproutly administrators.");
  }
  return caller;
}

export async function requireSeller(request: CallableRequest): Promise<CallerProfile> {
  const caller = await requireCaller(request);
  if (caller.role !== "SELLER" && caller.role !== "ADMIN") {
    throw new HttpsError("permission-denied", "Open a store to use seller tools.");
  }
  return caller;
}

export function badRequest(message: string): HttpsError {
  return new HttpsError("invalid-argument", message);
}

export function failedPrecondition(message: string): HttpsError {
  return new HttpsError("failed-precondition", message);
}

export function serverTimestamp() {
  return FieldValue.serverTimestamp();
}

export function serverTimestampMillis() {
  return Date.now();
}
