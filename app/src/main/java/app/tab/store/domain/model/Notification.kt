package app.tab.store.domain.model

enum class NotificationType {
    ORDER,
    STORE,
    SYSTEM,
    PROMO,
    /** The once-a-day nudge back into the store. */
    CHECK_IN,
    /** Something you installed has a newer build. */
    UPDATE,
}

data class AppNotification(
    val id: String = "",
    val title: String = "",
    val body: String = "",
    val type: NotificationType = NotificationType.SYSTEM,
    val read: Boolean = false,
    val deepLink: String? = null,
    /**
     * A picture to lead with — a screenshot or the app's own art. A notification
     * that says "The Visitor was updated" should show The Visitor, not a bell.
     */
    val imageUrl: String? = null,
    val createdAt: Long = 0L,
)
