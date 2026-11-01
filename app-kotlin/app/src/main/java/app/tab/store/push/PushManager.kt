package app.tab.store.push

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import app.tab.store.data.FirestorePaths
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.tasks.await

/**
 * Push registration.
 *
 * The token is stored on a private sub-document of the user, which the rules
 * allow the owner to write and nobody else to read. Handlers deliberately do not
 * build their own notifications: the Cloud Function sends a display message, so
 * the system tray rendering stays in one place.
 */
object PushManager {

    const val CHANNEL_ID = "sproutly_default"

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Sproutly",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "Approvals, reviews and campaign alerts"
        }
        manager.createNotificationChannel(channel)
    }

    /** Registers this device for the signed-in user. Safe to call repeatedly. */
    suspend fun syncToken(uid: String) {
        val token = runCatching { FirebaseMessaging.getInstance().token.await() }.getOrNull()
            ?: return
        runCatching {
            FirestorePaths.user(uid).collection("private").document("devices")
                .set(
                    mapOf(
                        "fcmToken" to token,
                        "platform" to "android",
                        "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                    ),
                )
                .await()
        }
    }
}

/** Foreground/background message handling. Registered in the manifest. */
class SproutlyMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        // A token can rotate while the app is not running; the next sign-in syncs it.
    }

    override fun onMessageReceived(message: RemoteMessage) {
        // The Cloud Function sends `notification` payloads, which the system
        // renders itself. Nothing to do here for those, and we deliberately do
        // not double-post a notification.
    }
}
