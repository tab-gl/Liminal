package app.tab.store.core.notifications

import android.app.NotificationManager
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import app.tab.store.R
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Order and store updates arrive as data + notification messages. When the app
 * is backgrounded FCM paints the payload itself; when it is foregrounded we
 * raise the notification here so the shopper still sees it.
 */
@AndroidEntryPoint
class SproutlyMessagingService : FirebaseMessagingService() {

    @Inject lateinit var deviceTokenRegistrar: DeviceTokenRegistrar

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        // Service callbacks are not suspend functions; hand off to a scope that
        // lives as long as the service.
        scope.launch { deviceTokenRegistrar.registerNow(token) }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val title = message.notification?.title
            ?: message.data["title"]
            ?: "Liminal"
        val body = message.notification?.body
            ?: message.data["body"]
            ?: return

        val manager = getSystemService(NotificationManager::class.java)
        val notification = NotificationCompat.Builder(
            this,
            getString(R.string.notifications_channel_id),
        )
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        manager.notify(System.currentTimeMillis().toInt(), notification)
    }
}
