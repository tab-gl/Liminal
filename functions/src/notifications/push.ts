import { getMessaging } from "firebase-admin/messaging";
import { getFirestore, FieldValue } from "firebase-admin/firestore";

export type NotificationKind = "ORDER" | "STORE" | "SYSTEM" | "PROMO";

export interface PushPayload {
  uid: string;
  title: string;
  body: string;
  kind: NotificationKind;
  deepLink?: string;
}

/**
 * Writes an in-app notification document and delivers a data-only push to every
 * device the user has registered. Stale tokens are pruned as we go.
 */
export async function notifyUser(payload: PushPayload): Promise<void> {
  const firestore = getFirestore();
  const ref = firestore
    .collection("notifications")
    .doc(payload.uid)
    .collection("items")
    .doc();

  await ref.set({
    title: payload.title,
    body: payload.body,
    type: payload.kind,
    read: false,
    deepLink: payload.deepLink ?? null,
    createdAt: FieldValue.serverTimestamp(),
  });

  const tokensSnap = await firestore
    .collection("users")
    .doc(payload.uid)
    .collection("tokens")
    .get();
  const tokens = tokensSnap.docs.map((doc) => doc.id).filter(Boolean);
  if (tokens.length === 0) return;

  const response = await getMessaging().sendEachForMulticast({
    tokens,
    notification: { title: payload.title, body: payload.body },
    data: {
      title: payload.title,
      body: payload.body,
      deepLink: payload.deepLink ?? "",
      type: payload.kind,
    },
    android: { priority: "high", notification: { channelId: "sproutly_default" } },
  });

  const stale: string[] = [];
  response.responses.forEach((result, index) => {
    if (!result.success) {
      const code = result.error?.code ?? "";
      if (
        code.includes("registration-token-not-registered") ||
        code.includes("invalid-argument")
      ) {
        stale.push(tokens[index]);
      }
    }
  });

  await Promise.all(
    stale.map((token) =>
      firestore.collection("users").doc(payload.uid).collection("tokens").doc(token).delete(),
    ),
  );
}
