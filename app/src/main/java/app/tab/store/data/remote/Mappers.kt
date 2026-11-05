package app.tab.store.data.remote

import app.tab.store.domain.model.AppNotification
import app.tab.store.domain.model.Category
import app.tab.store.domain.model.NotificationType
import app.tab.store.domain.model.Order
import app.tab.store.domain.model.OrderEvent
import app.tab.store.domain.model.OrderItem
import app.tab.store.domain.model.OrderStatus
import app.tab.store.domain.model.Product
import app.tab.store.domain.model.ProductImage
import app.tab.store.domain.model.ProductStatus
import app.tab.store.domain.model.Store
import app.tab.store.domain.model.StoreStatus
import app.tab.store.domain.model.User
import app.tab.store.domain.model.UserRole
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.QueryDocumentSnapshot

private fun DocumentSnapshot.long(key: String): Long =
    (get(key) as? Number)?.toLong() ?: 0L

private fun DocumentSnapshot.int(key: String): Int =
    (get(key) as? Number)?.toInt() ?: 0

private fun DocumentSnapshot.double(key: String): Double =
    (get(key) as? Number)?.toDouble() ?: 0.0

private fun DocumentSnapshot.str(key: String): String = getString(key).orEmpty()

private fun DocumentSnapshot.strOrNull(key: String): String? =
    getString(key)?.takeIf { it.isNotBlank() }

fun DocumentSnapshot.toUser(): User = User(
    uid = id,
    email = str("email"),
    displayName = str("displayName"),
    photoUrl = strOrNull("photoUrl"),
    phone = strOrNull("phone"),
    role = UserRole.from(str("role")),
    storeId = strOrNull("storeId"),
    createdAt = long("createdAt"),
)

fun DocumentSnapshot.toStore(): Store = Store(
    id = id,
    ownerId = str("ownerId"),
    name = str("name"),
    slug = str("slug"),
    tagline = str("tagline"),
    description = str("description"),
    logoUrl = strOrNull("logoUrl"),
    logoPublicId = strOrNull("logoPublicId"),
    bannerUrl = strOrNull("bannerUrl"),
    bannerPublicId = strOrNull("bannerPublicId"),
    city = str("city"),
    status = StoreStatus.from(str("status")),
    rating = double("rating"),
    ratingCount = int("ratingCount"),
        verified = getBoolean("verified") ?: false,
        official = getBoolean("official") ?: false,
        verificationStatus = getString("verificationStatus") ?: "NONE",
    productCount = int("productCount"),
    createdAt = long("createdAt"),
)

@Suppress("UNCHECKED_CAST")
fun DocumentSnapshot.toProduct(): Product {
    val rawImages = get("images") as? List<Map<String, Any?>> ?: emptyList()
    val images = rawImages.mapNotNull { map ->
        val url = map["url"] as? String
        if (url.isNullOrBlank()) null else ProductImage(url, map["publicId"] as? String)
    }
    val screenshots = ((get("screenshots") as? List<*>).orEmpty()).mapNotNull { raw ->
        val map = raw as? Map<*, *> ?: return@mapNotNull null
        val url = map["url"] as? String ?: return@mapNotNull null
        ProductImage(url = url, publicId = map["publicId"] as? String)
    }
    val tags = (get("tags") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
    return Product(
        id = id,
        storeId = str("storeId"),
        storeName = str("storeName"),
        title = str("title"),
        description = str("description"),
        categoryId = str("categoryId"),
        categoryLabel = str("categoryLabel"),
        images = images,
        screenshots = screenshots,
        price = long("price"),
        compareAtPrice = (get("compareAtPrice") as? Number)?.toLong(),
        offerEndsAt = (get("offerEndsAt") as? Number)?.toLong(),
        offerKind = getString("offerKind") ?: "PRICE_DROP",
        promoCode = getString("promoCode") ?: "",
        promoHeadline = getString("promoHeadline") ?: "",
        promoBody = getString("promoBody") ?: "",
        promoImageUrl = getString("promoImageUrl"),
        currency = getString("currency") ?: "INR",
        stock = int("stock"),
        status = ProductStatus.from(str("status")),
        rating = double("rating"),
        ratingCount = int("ratingCount"),
        apkUrl = strOrNull("apkUrl"),
        apkSizeBytes = (get("apkSizeBytes") as? Number)?.toLong(),
        versionName = getString("versionName") ?: "",
        versionCode = int("versionCode"),
        releaseNotes = getString("releaseNotes") ?: "",
        official = getBoolean("official") ?: false,
        scanRisk = getString("scanRisk") ?: "",
        scanScore = int("scanScore"),
        scanPermissions = (get("scanPermissions") as? List<*>)?.filterIsInstance<String>().orEmpty(),
        scanWarnings = (get("scanWarnings") as? List<*>)?.filterIsInstance<String>().orEmpty(),
        unitsSold = int("unitsSold"),
        tags = tags,
        createdAt = long("createdAt"),
        updatedAt = long("updatedAt"),
    )
}

fun DocumentSnapshot.toCategory(): Category = Category(
    id = id,
    name = str("name"),
    slug = str("slug"),
    iconKey = getString("iconKey") ?: "leaf",
    order = int("order"),
    productCount = int("productCount"),
    imageUrl = strOrNull("imageUrl"),
)

@Suppress("UNCHECKED_CAST")
fun DocumentSnapshot.toOrder(): Order {
    val rawItems = get("items") as? List<Map<String, Any?>> ?: emptyList()
    val items = rawItems.map { map ->
        OrderItem(
            productId = map["productId"] as? String ?: "",
            title = map["title"] as? String ?: "",
            imageUrl = map["imageUrl"] as? String,
            unitPrice = (map["unitPrice"] as? Number)?.toLong() ?: 0L,
            quantity = (map["quantity"] as? Number)?.toInt() ?: 0,
            storeId = map["storeId"] as? String ?: "",
            storeName = map["storeName"] as? String ?: "",
        )
    }
    val rawTimeline = get("timeline") as? List<Map<String, Any?>> ?: emptyList()
    val timeline = rawTimeline.map { map ->
        OrderEvent(
            status = OrderStatus.from(map["status"] as? String),
            at = (map["at"] as? Number)?.toLong() ?: 0L,
            note = map["note"] as? String,
        )
    }
    return Order(
        id = id,
        buyerId = str("buyerId"),
        buyerName = str("buyerName"),
        items = items,
        subtotal = long("subtotal"),
        shipping = long("shipping"),
        total = long("total"),
        status = OrderStatus.from(str("status")),
        storeIds = (get("storeIds") as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
        placedAt = long("placedAt"),
        updatedAt = long("updatedAt"),
        timeline = timeline,
    )
}

fun DocumentSnapshot.toNotification(): AppNotification = AppNotification(
    id = id,
    title = str("title"),
    body = str("body"),
    type = runCatching { NotificationType.valueOf(str("type")) }.getOrDefault(NotificationType.SYSTEM),
    read = getBoolean("read") ?: false,
    deepLink = strOrNull("deepLink"),
    createdAt = long("createdAt"),
)

fun QueryDocumentSnapshot.toProductCached() = toProduct()
