package app.tab.store.feature.offers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.tab.store.domain.model.Product
import app.tab.store.domain.repository.CatalogRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class OffersUiState(
    val offers: List<Product> = emptyList(),
    val isLoading: Boolean = true,
) {
    val hasOffers: Boolean get() = offers.isNotEmpty()
}

/**
 * Offers are apps whose compare-at price sits above what they actually cost.
 * Nothing is stored separately: a discount is a property of the listing, so the
 * rail is derived from the same catalogue everyone else reads.
 */
@HiltViewModel
class OffersViewModel @Inject constructor(
    catalogRepository: CatalogRepository,
) : ViewModel() {

    private val loading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = loading.asStateFlow()

    val uiState: StateFlow<OffersUiState> = combine(
        catalogRepository.observeNewArrivals(limit = 40),
        catalogRepository.observeTrending(limit = 40),
    ) { fresh, popular ->
        (fresh + popular)
            .distinctBy { it.id }
            .filter { it.discountPercent > 0 }
            .sortedByDescending { it.discountPercent }
            .let { ranked ->
                loading.value = false
                OffersUiState(offers = ranked, isLoading = false)
            }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        OffersUiState(),
    )
}
