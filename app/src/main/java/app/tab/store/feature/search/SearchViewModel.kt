package app.tab.store.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.tab.store.domain.model.Category
import app.tab.store.domain.model.Product
import app.tab.store.domain.repository.CatalogRepository
import app.tab.store.domain.repository.DataResult
import app.tab.store.domain.repository.ProductSort
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch

data class PriceBand(val label: String, val min: Long?, val max: Long?)

val priceBands = listOf(
    PriceBand("Under ₹500", null, 50_000),
    PriceBand("₹500 – ₹2,000", 50_000, 200_000),
    PriceBand("₹2,000 – ₹5,000", 200_000, 500_000),
    PriceBand("Above ₹5,000", 500_000, null),
)

data class SearchUiState(
    val query: String = "",
    val categoryId: String? = null,
    val categoryLabel: String? = null,
    val sort: ProductSort = ProductSort.RELEVANCE,
    val priceBand: PriceBand? = null,
    val categories: List<Category> = emptyList(),
    val results: List<Product> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasSearched: Boolean = false,
    val error: String? = null,
    val nextPageToken: String? = null,
    /** Studios matching the same query. */
    val stores: List<app.tab.store.domain.model.Store> = emptyList(),
    val showDevelopers: Boolean = false,
) {
    val hasMore: Boolean get() = nextPageToken != null
    val isEmpty: Boolean get() = hasSearched && results.isEmpty() && !isLoading
    val sortLabel: String
        get() = when (sort) {
            ProductSort.RELEVANCE -> "Best match"
            ProductSort.PRICE_LOW_HIGH -> "Price: low to high"
            ProductSort.PRICE_HIGH_LOW -> "Price: high to low"
            ProductSort.NEWEST -> "Newest first"
            ProductSort.TOP_RATED -> "Top rated"
        }
}

@OptIn(FlowPreview::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val catalogRepository: CatalogRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private val queryStream = MutableStateFlow("")
    private var searchJob: Job? = null

    init {
        viewModelScope.launch {
            catalogRepository.observeCategories().collect { categories ->
                _uiState.value = _uiState.value.copy(categories = categories)
            }
        }
        viewModelScope.launch {
            queryStream.debounce(320).collect { runSearch(reset = true) }
        }
        runSearch(reset = true)
    }

    fun setCategory(categoryId: String?, label: String?) {
        _uiState.value = _uiState.value.copy(categoryId = categoryId, categoryLabel = label)
        runSearch(reset = true)
    }

    fun onQueryChange(value: String) {
        _uiState.value = _uiState.value.copy(query = value)
        queryStream.value = value
    }

    fun submitQuery(value: String) {
        _uiState.value = _uiState.value.copy(query = value)
        queryStream.value = value
        runSearch(reset = true)
    }

    fun setSort(sort: ProductSort) {
        _uiState.value = _uiState.value.copy(sort = sort)
        runSearch(reset = true)
    }

    fun setPriceBand(band: PriceBand?) {
        _uiState.value = _uiState.value.copy(
            priceBand = if (_uiState.value.priceBand == band) null else band,
        )
        runSearch(reset = true)
    }

    fun loadMore() {
        val state = _uiState.value
        if (state.isLoadingMore || state.nextPageToken == null) return
        viewModelScope.launch { fetch(state, reset = false) }
    }

    fun runSearch(reset: Boolean) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            fetch(_uiState.value, reset)
            if (_uiState.value.showDevelopers) runStoreSearch()
        }
    }

    /** Switches the results between apps and developers. */
    fun showDevelopers(show: Boolean) {
        _uiState.value = _uiState.value.copy(showDevelopers = show)
        if (show) runStoreSearch()
    }

    private fun runStoreSearch() {
        viewModelScope.launch {
            val stores = catalogRepository.searchStores(_uiState.value.query)
            _uiState.value = _uiState.value.copy(stores = stores)
        }
    }

    private suspend fun fetch(state: SearchUiState, reset: Boolean) {
        _uiState.value = state.copy(
            isLoading = reset,
            isLoadingMore = !reset,
            error = null,
        )
        val page = when (
            val res = catalogRepository.search(
                query = state.query,
                categoryId = state.categoryId,
                minPricePaise = state.priceBand?.min,
                maxPricePaise = state.priceBand?.max,
                sort = state.sort,
                pageToken = if (reset) null else state.nextPageToken,
            )
        ) {
            is DataResult.Failure -> {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isLoadingMore = false,
                    hasSearched = true,
                    error = res.message,
                )
                return
            }
            is DataResult.Success -> res.value
        }
        val merged = if (reset) page.products else _uiState.value.results + page.products
        _uiState.value = _uiState.value.copy(
            results = merged,
            isLoading = false,
            isLoadingMore = false,
            hasSearched = true,
            nextPageToken = page.nextPageToken,
            error = null,
        )
    }
}
