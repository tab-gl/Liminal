import { CallableRequest, HttpsError } from "firebase-functions/v2/https";
import { db, auth, COLLECTIONS, FieldValue } from "./config";

export interface AuthedContext {
  uid: string;
  role: "user" | "developer" | "admin";
}

/** Resolves the caller and their role from the *token*, never from the payload. */
export async function requireUser(request: CallableRequest): Promise<AuthedContext> {
  const uid = request.auth?.uid;
  if (!uid) {
    throw new HttpsError("unauthenticated", "Sign in to continue.");
  }
  const token = (request.auth?.token ?? {}) as Record<string, unknown>;
  const claimRole = token.role;
  const role: AuthedContext["role"] =
    claimRole === "admin" ? "admin" : claimRole === "developer" ? "developer" : "user";
  return { uid, role };
}

export async function requireAdmin(request: CallableRequest): Promise<AuthedContext> {
  const ctx = await requireUser(request);
  if (ctx.role !== "admin") {
    throw new HttpsError("permission-denied", "Administrators only.");
  }
  return ctx;
}

/** Writes an audit row for every privileged mutation. */
export async function audit(
  actorUid: string,
  action: string,
  target: string,
  detail: Record<string, unknown> = {},
): Promise<void> {
  await db.collection(COLLECTIONS.audit).add({
    actorUid,
    action,
    target,
    detail,
    createdAt: FieldValue.serverTimestamp(),
  });
}

export async function sendNotification(input: {
  userId: string;
  type: string;
  title: string;
  body: string;
  route?: string;
  data?: Record<string, string>;
}): Promise<void> {
  await db.collection(COLLECTIONS.notifications).add({
    userId: input.userId,
    type: input.type,
    title: input.title,
    body: input.body,
    route: input.route ?? null,
    data: input.data ?? {},
    readAt: null,
    createdAt: FieldValue.serverTimestamp(),
  });
}

export function assertString(value: unknown, field: string, max = 2000): string {
  if (typeof value !== "string" || value.trim().length === 0) {
    throw new HttpsError("invalid-argument", `${field} is required.`);
  }
  if (value.length > max) {
    throw new HttpsError("invalid-argument", `${field} is too long.`);
  }
  return value.trim();
}

export function optionalString(value: unknown, max = 2000): string | undefined {
  if (typeof value !== "string") return undefined;
  const trimmed = value.trim();
  if (!trimmed) return undefined;
  return trimmed.slice(0, max);
}

/** yyyy-mm-dd in UTC, the id format of the daily analytics documents. */
export function dayKey(date: Date): string {
  return date.toISOString().slice(0, 10);
}

export async function userDisplayName(uid: string): Promise<string> {
  try {
    const record = await auth.getUser(uid);
    return record.displayName ?? "Sproutly member";
  } catch {
    return "Sproutly member";
  }
}
