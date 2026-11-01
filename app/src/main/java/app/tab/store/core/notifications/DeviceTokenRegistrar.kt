package app.tab.store.core.notifications

import app.tab.store.domain.repository.NotificationRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.tasks.await

/**
 * Keeps the device's FCM token attached to the signed-in user so Cloud
 * Functions can target order updates at the right phone.
 */
@Singleton
class DeviceTokenRegistrar @Inject constructor(
    private val notificationRepository: NotificationRepository,
    private val auth: FirebaseAuth,
    private val messaging: FirebaseMessaging,
) {
    suspend fun syncToken() {
        if (auth.currentUser == null) return
        runCatching {
            val token = messaging.token.await()
            notificationRepository.registerDeviceToken(token)
        }
    }

    suspend fun registerNow(token: String) {
        if (auth.currentUser == null) return
        notificationRepository.registerDeviceToken(token)
    }
}
