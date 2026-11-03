package app.tab.store.feature.rankings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.tab.store.domain.model.Product
import app.tab.store.domain.repository.CatalogRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class RankingsUiState(
    val ranked: List<Product> = emptyList(),
    val isLoading: Boolean = true,
)

@HiltViewModel
class RankingsViewModel @Inject constructor(
    catalogRepository: CatalogRepository,
) : ViewModel() {

    /** observeTrending is already ordered by installs — the chart is the query. */
    val uiState: StateFlow<RankingsUiState> = catalogRepository.observeTrending(limit = 50)
        .map { products ->
            RankingsUiState(
                ranked = products.filterNot {
                    it.categoryId.equals("games", ignoreCase = true)
                },
                isLoading = false,
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RankingsUiState())
}
