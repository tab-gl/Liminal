package app.tab.store.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Offline cache of products the shopper has looked at, so the rail works offline. */
@Entity(tableName = "recently_viewed")
data class RecentlyViewedEntity(
    @PrimaryKey val productId: String,
    val title: String,
    val storeName: String,
    val imageUrl: String?,
    val price: Long,
    val compareAtPrice: Long?,
    val categoryId: String,
    val categoryLabel: String,
    val viewedAt: Long,
)

/** Cached catalogue rows used to paint lists before Firestore responds. */
@Entity(tableName = "cached_products")
data class CachedProductEntity(
    @PrimaryKey val productId: String,
    val storeId: String,
    val storeName: String,
    val title: String,
    val imageUrl: String?,
    val price: Long,
    val compareAtPrice: Long?,
    val stock: Int,
    val status: String,
    val rating: Double,
    val ratingCount: Int,
    val unitsSold: Int,
    val categoryId: String,
    val categoryLabel: String,
    val description: String,
    val tags: String,
    val cachedAt: Long,
)
