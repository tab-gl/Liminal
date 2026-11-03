package app.tab.store.domain

import app.tab.store.domain.model.Product
import app.tab.store.domain.model.ProductImage
import app.tab.store.domain.model.ProductStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProductTest {

    @Test
    fun `discount percent is derived from compare-at price`() {
        val product = Product(price = 79_900, compareAtPrice = 99_900, stock = 4)
        assertEquals(20, product.discountPercent)
    }

    @Test
    fun `no discount when compare-at is missing or lower`() {
        assertEquals(0, Product(price = 10_000, compareAtPrice = null).discountPercent)
        assertEquals(0, Product(price = 10_000, compareAtPrice = 9_000).discountPercent)
    }

    @Test
    fun `an app is installable regardless of stock`() {
        // A digital listing has no inventory: installs are not limited by stock,
        // only by whether the app is published.
        val product = Product(price = 1_000, stock = 0, status = ProductStatus.ACTIVE)
        assertTrue(product.isOutOfStock)
        assertTrue(product.isPurchasable)
    }

    @Test
    fun `a free app is free`() {
        assertTrue(Product(price = 0).isFree)
        assertFalse(Product(price = 1).isFree)
    }

    @Test
    fun `draft products are never purchasable even with stock`() {
        val product = Product(price = 1_000, stock = 8, status = ProductStatus.DRAFT)
        assertFalse(product.isPurchasable)
    }

    @Test
    fun `primary image falls back to null when gallery is empty`() {
        assertNull(Product().primaryImage)
        val withImage = Product(images = listOf(ProductImage("https://cdn/x.jpg", "sproutly/x")))
        assertEquals("https://cdn/x.jpg", withImage.primaryImage)
    }
}
