package app.tab.store.feature.games

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

data class GamesUiState(
    val games: List<Product> = emptyList(),
    val top: List<Product> = emptyList(),
    val fresh: List<Product> = emptyList(),
    val isLoading: Boolean = true,
)

@HiltViewModel
class GamesViewModel @Inject constructor(
    catalogRepository: CatalogRepository,
) : ViewModel() {

    val uiState: StateFlow<GamesUiState> = catalogRepository.observeByCategory("games", limit = 40)
        .map { products ->
            GamesUiState(
                games = products,
                top = products.sortedByDescending { it.unitsSold }.take(8),
                fresh = products.sortedByDescending { it.createdAt }.take(8),
                isLoading = false,
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GamesUiState())
}
