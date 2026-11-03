package app.tab.store.domain.model

enum class ProductStatus { DRAFT, ACTIVE, ARCHIVED, BLOCKED;
    companion object {
        fun from(value: String?): ProductStatus =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: DRAFT
    }
}

data class ProductImage(val url: String, val publicId: String?)

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
    val currency: String = "INR",
    val stock: Int = 0,
    val status: ProductStatus = ProductStatus.DRAFT,
    val rating: Double = 0.0,
    val ratingCount: Int = 0,
    val unitsSold: Int = 0,
    /** Where the build lives: an uploaded APK or a link the developer owns. */
    val apkUrl: String? = null,
    val apkSizeBytes: Long? = null,
    val tags: List<String> = emptyList(),
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
) {
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
    val isOutOfStock: Boolean get() = stock <= 0
    val isPurchasable: Boolean get() = status == ProductStatus.ACTIVE
    val discountPercent: Int
        get() {
            val compare = compareAtPrice ?: return 0
            if (compare <= price) return 0
            return (((compare - price) * 100) / compare).toInt()
        }
}
