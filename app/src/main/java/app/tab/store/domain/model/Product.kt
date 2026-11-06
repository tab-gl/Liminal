package app.tab.store.domain.model

enum class ProductStatus { DRAFT, ACTIVE, ARCHIVED, BLOCKED;
    companion object {
        fun from(value: String?): ProductStatus =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: DRAFT
    }
}

data class ProductImage(val url: String, val publicId: String?)

/**
 * One build of the same app.
 *
 * Plenty of apps ship more than one: a lite edition, a pro edition, or separate
 * 32-bit and 64-bit builds. Each flavour is a name plus where its build lives.
 */
data class Flavour(
    val name: String = "",
    val apkUrl: String = "",
    val versionName: String = "",
    val versionCode: Int = 0,
    val sizeBytes: Long = 0L,
)

data class Product(
    val id: String = "",
    val storeId: String = "",
    val storeName: String = "",
    val title: String = "",
    val description: String = "",
    val categoryId: String = "",
    val categoryLabel: String = "",
    val images: List<ProductImage> = emptyList(),
    /** Store screenshots, shown in a carousel on the detail page. */
    val screenshots: List<ProductImage> = emptyList(),
    val price: Long = 0L,
    val compareAtPrice: Long? = null,
    /** When a declared offer ends; null means the price is simply the price. */
    val offerEndsAt: Long? = null,
    /**
     * What kind of offer this is: a price drop, a code people type in the app, or
     * something inside the app. Not every offer is a cheaper price.
     */
    val offerKind: String = "PRICE_DROP",
    val promoCode: String = "",
    /** The offer banner: what the store shows while the offer runs. */
    val promoHeadline: String = "",
    val promoBody: String = "",
    val promoImageUrl: String? = null,
    val currency: String = "INR",
    val stock: Int = 0,
    val status: ProductStatus = ProductStatus.DRAFT,
    val rating: Double = 0.0,
    val ratingCount: Int = 0,
    val unitsSold: Int = 0,
    /** Where the build lives: an uploaded APK or a link the developer owns. */
    val apkUrl: String? = null,
    /** Extra builds of the same app, if the developer publishes any. */
    val flavours: List<Flavour> = emptyList(),
    val apkSizeBytes: Long? = null,
    /** Release identity, so an update is a version rather than a new listing. */
    val versionName: String = "",
    val versionCode: Int = 0,
    val releaseNotes: String = "",
    /** Published by a verified studio the store itself runs. */
    val official: Boolean = false,
    /** Picked by the store for the front page. */
    val featured: Boolean = false,
    /** Flagged for moderation. */
    val reported: Boolean = false,
    /** Sprout Safe: what the build can reach, from its own manifest. */
    val scanRisk: String = "",
    val scanScore: Int = 0,
    val scanPermissions: List<String> = emptyList(),
    val scanWarnings: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
) {
    /**
     * Every installable build: the main one first, then the extra flavours.
     * Install and the build picker both read this, so the main build is always
     * reachable even when flavours exist.
     */
    val builds: List<Flavour>
        get() {
            val main = apkUrl?.takeIf { it.isNotBlank() }?.let {
                Flavour(
                    name = "Standard",
                    apkUrl = it,
                    versionName = versionName,
                    versionCode = versionCode,
                    sizeBytes = apkSizeBytes ?: 0L,
                )
            }
            return listOfNotNull(main) + flavours.filter { it.apkUrl.isNotBlank() }
        }

    val primaryImage: String? get() = images.firstOrNull()?.url
    val isFree: Boolean get() = price <= 0L

    /** A real, running discount — not just a crossed-out number. */
    val hasActiveOffer: Boolean
        get() {
            val was = compareAtPrice ?: return false
            if (was <= price) return false
            val ends = offerEndsAt ?: return true
            return ends > System.currentTimeMillis()
        }
    /** A code or an in-app perk is an offer too, with no price change. */
    val hasAnyOffer: Boolean
        get() = (hasActiveOffer || (offerKind != "PRICE_DROP" && promoCode.isNotBlank()))

    /** One line describing the offer, whatever kind it is. */
    val offerLine: String
        get() = when {
            hasActiveOffer -> "${discountPercent}% off"
            offerKind == "PROMO_CODE" && promoCode.isNotBlank() -> "Code $promoCode"
            offerKind == "IN_APP" -> "In-app perk"
            else -> ""
        }

    val isPurchasable: Boolean get() = status == ProductStatus.ACTIVE
    val discountPercent: Int
        get() {
            val compare = compareAtPrice ?: return 0
            if (compare <= price) return 0
            return (((compare - price) * 100) / compare).toInt()
        }
}
