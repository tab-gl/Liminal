package app.tab.store.data.repository

import app.tab.store.domain.model.Product
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.reflect.full.primaryConstructor

/**
 * Guards the bug that blocked publishing: the model carried `apkUrl`,
 * `screenshots` and the offer fields, the payload silently left them out, and the
 * rules refused every write because of it.
 *
 * If a field is added to [Product] and not to the payload, this fails — and that
 * is the point.
 */
class ProductPayloadTest {

    @Test
    fun `every product field reaches the firestore payload`() {
        // Fully populated: the payload drops nulls on purpose, so an empty
        // Product would look like it was missing fields.
        val full = Product(
            id = "p1",
            storeId = "s1",
            storeName = "Studio",
            title = "Notes",
            description = "A notes app",
            categoryId = "productivity",
            categoryLabel = "Productivity",
            images = listOf(app.tab.store.domain.model.ProductImage("https://x/i.jpg", "i1")),
            screenshots = listOf(app.tab.store.domain.model.ProductImage("https://x/s.jpg", "s1")),
            price = 9900,
            compareAtPrice = 19900,
            offerEndsAt = 2_000_000_000_000,
            promoHeadline = "Half price",
            promoBody = "This week only",
            promoImageUrl = "https://x/b.jpg",
            stock = 0,
            apkUrl = "https://x/app.apk",
            apkSizeBytes = 1024,
            versionName = "1.0",
            versionCode = 1,
            releaseNotes = "Fixes",
            scanRisk = "LOW",
            scanPermissions = listOf("android.permission.CAMERA"),
            scanWarnings = listOf("Can use your camera"),
            tags = listOf("notes"),
        )
        val payload = productPayload(full)
        val modelFields = Product::class.primaryConstructor!!
            .parameters
            .mapNotNull { it.name }
            .filterNot { it in productPayloadExempt }

        val missing = modelFields.filterNot { it in payload.keys }

        assertTrue(
            "Product fields missing from the Firestore payload: $missing. " +
                "Add them to productPayload() — the rules validate several of them.",
            missing.isEmpty(),
        )
    }

    @Test
    fun `the build link is written`() {
        val payload = productPayload(Product(apkUrl = "https://example.com/app.apk"))
        assertTrue(payload["apkUrl"] == "https://example.com/app.apk")
    }

    @Test
    fun `screenshots are written as objects`() {
        val payload = productPayload(
            Product(screenshots = listOf(app.tab.store.domain.model.ProductImage("https://x/1.jpg", "p1"))),
        )
        val shots = payload["screenshots"] as List<*>
        val first = shots.first() as Map<*, *>
        assertTrue(first["url"] == "https://x/1.jpg" && first["publicId"] == "p1")
    }
}
