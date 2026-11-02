package app.tab.store.domain.model

enum class NotificationType { ORDER, STORE, SYSTEM, PROMO }

data class AppNotification(
    val id: String = "",
    val title: String = "",
    val body: String = "",
    val type: NotificationType = NotificationType.SYSTEM,
    val read: Boolean = false,
    val deepLink: String? = null,
    val createdAt: Long = 0L,
)
