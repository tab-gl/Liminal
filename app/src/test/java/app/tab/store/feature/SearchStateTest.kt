package app.tab.store.feature

import app.tab.store.feature.search.PriceBand
import app.tab.store.feature.search.SearchUiState
import app.tab.store.feature.search.priceBands
import app.tab.store.domain.repository.ProductSort
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchStateTest {

    @Test
    fun `empty result is only shown after a search completed`() {
        assertFalse(SearchUiState().isEmpty)
        assertTrue(SearchUiState(hasSearched = true).isEmpty)
        assertFalse(SearchUiState(hasSearched = true, isLoading = true).isEmpty)
    }

    @Test
    fun `pagination is available only when a page token exists`() {
        assertFalse(SearchUiState().hasMore)
        assertTrue(SearchUiState(nextPageToken = "abc").hasMore)
    }

    @Test
    fun `price bands are contiguous and cover the extremes`() {
        assertEquals(4, priceBands.size)
        assertEquals(null, priceBands.first().min)
        assertEquals(null, priceBands.last().max)
        priceBands.zipWithNext().forEach { (lower, upper) ->
            assertEquals(lower.max, upper.min)
        }
    }

    @Test
    fun `sort label reflects the selected sort`() {
        assertEquals("Best match", SearchUiState().sortLabel)
        assertEquals(
            "Price: low to high",
            SearchUiState(sort = ProductSort.PRICE_LOW_HIGH).sortLabel,
        )
    }

    @Test
    fun `price band equality works for selection toggling`() {
        val band = PriceBand("Under ₹500", null, 50_000)
        assertEquals(band, PriceBand("Under ₹500", null, 50_000))
    }
}
