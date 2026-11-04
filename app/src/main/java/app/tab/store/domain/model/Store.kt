package app.tab.store.domain.model

enum class StoreStatus { PENDING, ACTIVE, SUSPENDED;
    companion object {
        fun from(value: String?): StoreStatus =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: PENDING
    }
}

data class Store(
    val id: String = "",
    val ownerId: String = "",
    val name: String = "",
    val slug: String = "",
    val tagline: String = "",
    val description: String = "",
    val logoUrl: String? = null,
    val logoPublicId: String? = null,
    val bannerUrl: String? = null,
    val bannerPublicId: String? = null,
    val city: String = "",
    val status: StoreStatus = StoreStatus.PENDING,
    val rating: Double = 0.0,
    val ratingCount: Int = 0,
    /** Paid apps require a verified developer account. */
    val verified: Boolean = false,
    /** The store's own studio; its listings carry the Official badge. */
    val official: Boolean = false,
    val verificationStatus: String = "NONE",
    val productCount: Int = 0,
    val createdAt: Long = 0L,
) {
    val isActive: Boolean get() = status == StoreStatus.ACTIVE
}
