package app.tab.store.data.repository

import app.tab.store.domain.model.Product
import app.tab.store.domain.model.ProductImage
import com.google.firebase.firestore.FieldValue

/**
 * The Firestore document for a listing.
 *
 * This lives on its own, and is covered by a test, because the payload once
 * omitted `apkUrl`, `screenshots`, the offer fields and the version fields. The
 * rules require several of those, so every publish was denied while the model
 * carried the values happily — the field simply never reached Firestore.
 *
 * Every field of [Product] that belongs in the document must appear here; the
 * test fails if a new one is added to the model and forgotten here.
 */
internal fun productPayload(product: Product): MutableMap<String, Any?> {
    fun images(list: List<ProductImage>) =
        list.map { mapOf("url" to it.url, "publicId" to it.publicId) }

    return mutableMapOf(
        "storeId" to product.storeId,
        "storeName" to product.storeName,
        "title" to product.title.trim(),
        "titleLower" to product.title.trim().lowercase(),
        "description" to product.description.trim(),
        "categoryId" to product.categoryId,
        "categoryLabel" to product.categoryLabel,
        "images" to images(product.images),
        "screenshots" to images(product.screenshots),
        "price" to product.price,
        "compareAtPrice" to product.compareAtPrice,
        "offerEndsAt" to product.offerEndsAt,
        "promoHeadline" to product.promoHeadline,
        "promoBody" to product.promoBody,
        "promoImageUrl" to product.promoImageUrl,
        "currency" to "INR",
        "stock" to product.stock,
        "status" to product.status.name,
        "apkUrl" to product.apkUrl,
        "apkSizeBytes" to product.apkSizeBytes,
        "versionName" to product.versionName,
        "versionCode" to product.versionCode,
        "releaseNotes" to product.releaseNotes,
        "scanRisk" to product.scanRisk,
        "scanPermissions" to product.scanPermissions,
        "scanWarnings" to product.scanWarnings,
        "tags" to product.tags,
        "updatedAt" to FieldValue.serverTimestamp(),
    )
}

/** Fields that are derived, client-only, or set by someone other than the seller. */
internal val productPayloadExempt = setOf(
    "id",          // the document id
    "rating",      // buyers and moderation own these
    "ratingCount",
    "unitsSold",
    "createdAt",   // written once, on create
    "updatedAt",   // written as a server timestamp
)
