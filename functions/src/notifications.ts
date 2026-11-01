import { onDocumentCreated } from "firebase-functions/v2/firestore";
import { getMessaging } from "firebase-admin/messaging";
import { db, COLLECTIONS } from "./config";

/**
 * Turns an in-app notification row into a push.
 *
 * The row is the source of truth: if the push fails (no token, no permission)
 * the notification is still waiting in the Activity tab.
 */
export const onNotificationCreated = onDocumentCreated("notifications/{notificationId}", async (event) => {
  const data = event.data?.data();
  if (!data) return;

  const userId = String(data.userId ?? "");
  if (!userId) return;

  const device = await db
    .collection(COLLECTIONS.users)
    .doc(userId)
    .collection("private")
    .doc("devices")
    .get();
  const token = device.data()?.fcmToken;
  if (typeof token !== "string" || token.length === 0) return;

  try {
    await getMessaging().send({
      token,
      notification: {
        title: String(data.title ?? "Sproutly"),
        body: String(data.body ?? ""),
      },
      data: {
        route: String(data.route ?? "/activity"),
        type: String(data.type ?? "system"),
        ...Object.fromEntries(
          Object.entries(data.data ?? {}).map(([key, value]) => [key, String(value)]),
        ),
      },
      android: { priority: "normal" },
      apns: { payload: { aps: { sound: "default" } } },
    });
  } catch (error) {
    // A stale token is dropped so the next launch re-registers a fresh one.
    const code = (error as { code?: string }).code ?? "";
    if (code.includes("registration-token-not-registered") || code.includes("invalid-argument")) {
      await device.ref.delete().catch(() => undefined);
    }
  }
});
