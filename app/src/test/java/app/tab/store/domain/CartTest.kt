package app.tab.store.domain

import app.tab.store.domain.model.Cart
import app.tab.store.domain.model.CartLine
import app.tab.store.domain.model.ProductStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CartTest {

    private fun line(
        id: String,
        price: Long,
        qty: Int,
        stock: Int = 10,
        status: ProductStatus = ProductStatus.ACTIVE,
        storeId: String = "store-1",
    ) = CartLine(
        productId = id,
        storeId = storeId,
        storeName = "The Green Shelf",
        title = "Product $id",
        imageUrl = null,
        unitPrice = price,
        quantity = qty,
        stock = stock,
        status = status,
    )

    @Test
    fun `empty cart reports zero items and zero subtotal`() {
        val cart = Cart()
        assertEquals(0, cart.itemCount)
        assertEquals(0L, cart.subtotal)
        assertTrue(cart.isEmpty)
        assertFalse(cart.hasUnavailable)
    }

    @Test
    fun `subtotal sums only available lines`() {
        val cart = Cart(
            lines = listOf(
                line("a", 49_900, 2),
                line("b", 12_500, 1),
                line("c", 99_900, 3, status = ProductStatus.BLOCKED),
            ),
        )
        assertEquals(6, cart.itemCount)
        assertEquals(49_900L * 2 + 12_500L, cart.subtotal)
    }

    @Test
    fun `unavailable line flags the cart`() {
        val cart = Cart(lines = listOf(line("a", 1_000, 1, stock = 0)))
        assertTrue(cart.hasUnavailable)
        assertFalse(cart.lines.first().isAvailable)
    }

    @Test
    fun `quantity above stock is flagged but still counted as available`() {
        val cart = Cart(lines = listOf(line("a", 1_000, 5, stock = 3)))
        assertTrue(cart.lines.first().exceedsStock)
        assertTrue(cart.hasUnavailable)
    }

    @Test
    fun `lines group by store for split fulfilment`() {
        val cart = Cart(
            lines = listOf(
                line("a", 1_000, 1, storeId = "s1"),
                line("b", 2_000, 1, storeId = "s2"),
                line("c", 3_000, 1, storeId = "s1"),
            ),
        )
        val grouped = cart.groupedByStore
        assertEquals(2, grouped.size)
        assertEquals(2, grouped["s1"]?.size)
        assertEquals(1, grouped["s2"]?.size)
    }
}
