package app.tab.store.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

enum class AppStatus(val wire: String, val label: String) {
    DRAFT("draft", "Draft"),
    PENDING_REVIEW("pending_review", "In review"),
    APPROVED("approved", "Live"),
    REJECTED("rejected", "Needs changes"),
    SUSPENDED("suspended", "Suspended");

    companion object {
        fun from(raw: String?): AppStatus = entries.firstOrNull { it.wire == raw } ?: DRAFT
    }
}

enum class PricingModel(val wire: String, val label: String) {
    FREE("free", "Free"),
    FREEMIUM("freemium", "Freemium"),
    PAID("paid", "Paid"),
    SUBSCRIPTION("subscription", "Subscription");

    companion object {
        fun from(raw: String?): PricingModel = entries.firstOrNull { it.wire == raw } ?: FREE
    }
}

enum class AppPlatform(val wire: String, val label: String) {
    ANDROID("android", "Android"),
    IOS("ios", "iOS"),
    WEB("web", "Web"),
    DESKTOP("desktop", "Desktop"),
    OTHER("other", "Other");

    companion object {
        fun from(raw: String?): AppPlatform = entries.firstOrNull { it.wire == raw } ?: OTHER
    }
}

enum class NotificationType(val wire: String) {
    REVIEW_RECEIVED("reviewReceived"),
    APPLICATION_APPROVED("applicationApproved"),
    APPLICATION_REJECTED("applicationRejected"),
    APPLICATION_SUSPENDED("applicationSuspended"),
    NEW_FOLLOWER("newFollower"),
    CAMPAIGN_APPROVED("campaignApproved"),
    CAMPAIGN_REJECTED("campaignRejected"),
    CAMPAIGN_BUDGET_LOW("campaignBudgetLow"),
    CAMPAIGN_EXHAUSTED("campaignExhausted"),
    SYSTEM("system");

    companion object {
        fun from(raw: String?): NotificationType = entries.firstOrNull { it.wire == raw } ?: SYSTEM
    }
}

/**
 * A listing. Field names match the Firestore documents the Cloud Functions and
 * security rules already expect, so the backend needs no changes at all.
 *
 * Server-owned counters are read-only here: the rules reject client writes to
 * them, and Cloud Functions maintain them from raw events.
 */
data class AppListing(
    @DocumentId val id: String = "",
    val ownerId: String = "",
    val ownerName: String = "",
    val ownerPhotoUrl: String? = null,
    val name: String = "",
    val tagline: String = "",
    val description: String = "",
    val categoryId: String = "other",
    val tags: List<String> = emptyList(),
    val iconUrl: String? = null,
    val bannerUrl: String? = null,
    val screenshots: List<String> = emptyList(),
    val websiteUrl: String? = null,
    val launchUrl: String? = null,
    val downloadUrl: String? = null,
    val platforms: List<String> = emptyList(),
    val pricingModel: String = "free",
    val priceLabel: String? = null,
    val isOpenSource: Boolean = false,
    val features: List<String> = emptyList(),
    val documentationUrl: String? = null,
    val supportEmail: String? = null,
    val privacyPolicyUrl: String? = null,
    val termsUrl: String? = null,
    val status: String = "draft",
    val rejectionReason: String? = null,
    val rating: Double = 0.0,
    val reviewCount: Int = 0,
    val viewCount: Int = 0,
    val uniqueViewerCount: Int = 0,
    val clickCount: Int = 0,
    val downloadCount: Int = 0,
    val bookmarkCount: Int = 0,
    val trendingScore: Int = 0,
    val sponsored: Boolean = false,
    val featured: Boolean = false,
    @ServerTimestamp val createdAt: Date? = null,
    @ServerTimestamp val updatedAt: Date? = null,
    val publishedAt: Date? = null,
) {
    val appStatus: AppStatus get() = AppStatus.from(status)
    val pricing: PricingModel get() = PricingModel.from(pricingModel)
    val platformList: List<AppPlatform> get() = platforms.map { AppPlatform.from(it) }

    /** The URL the install button should act on, in priority order. */
    val primaryActionUrl: String? get() = downloadUrl ?: launchUrl ?: websiteUrl
    val isFree: Boolean get() = pricing == PricingModel.FREE
    val clickThroughRate: Double get() = if (viewCount == 0) 0.0 else clickCount.toDouble() / viewCount
}

data class UserProfile(
    @DocumentId val uid: String = "",
    val displayName: String = "",
    val photoUrl: String? = null,
    val bio: String? = null,
    val websiteUrl: String? = null,
    val role: String = "user",
    val isDeveloper: Boolean = false,
    val isPublic: Boolean = true,
    val followerCount: Int = 0,
    val publishedAppCount: Int = 0,
    val reviewCount: Int = 0,
    val onboarded: Boolean = false,
    @ServerTimestamp val createdAt: Date? = null,
) {
    val isAdmin: Boolean get() = role == "admin"
    val canPublish: Boolean get() = isDeveloper || role == "admin"
}

data class Review(
    @DocumentId val id: String = "",
    val applicationId: String = "",
    val userId: String = "",
    val userName: String = "",
    val userPhotoUrl: String? = null,
    val rating: Int = 5,
    val body: String = "",
    val helpfulCount: Int = 0,
    val status: String = "published",
    @ServerTimestamp val createdAt: Date? = null,
)

data class AppNotification(
    @DocumentId val id: String = "",
    val userId: String = "",
    val type: String = "system",
    val title: String = "",
    val body: String = "",
    val route: String? = null,
    val readAt: Date? = null,
    @ServerTimestamp val createdAt: Date? = null,
) {
    val notificationType: NotificationType get() = NotificationType.from(type)
    val isUnread: Boolean get() = readAt == null
}

/** One day of activity, aggregated by the hourly Cloud Function. */
data class DailyStat(
    val date: String = "",
    val views: Int = 0,
    val clicks: Int = 0,
    val downloads: Int = 0,
    val bookmarks: Int = 0,
    val reviews: Int = 0,
)
