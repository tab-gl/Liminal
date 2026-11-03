package app.tab.store.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.tab.store.domain.model.Category
import app.tab.store.domain.model.Product
import app.tab.store.domain.model.User
import app.tab.store.domain.repository.AuthRepository
import app.tab.store.domain.repository.CatalogRepository
import app.tab.store.domain.repository.NotificationRepository
import app.tab.store.domain.repository.DataResult
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val user: User? = null,
    val categories: List<Category> = emptyList(),
    val featured: List<Product> = emptyList(),
    val trending: List<Product> = emptyList(),
    val newArrivals: List<Product> = emptyList(),
    val recommended: List<Product> = emptyList(),
    val recentlyViewed: List<Product> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val message: String? = null,
) {
    val greeting: String
        get() {
            val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
            return when (hour) {
                in 5..11 -> "Good morning"
                in 12..16 -> "Good afternoon"
                in 17..20 -> "Good evening"
                else -> "Welcome back"
            }
        }

    val firstName: String get() = user?.displayName?.substringBefore(" ")?.ifBlank { "there" } ?: "there"
    val hasAnyContent: Boolean get() = featured.isNotEmpty() || trending.isNotEmpty()
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val catalogRepository: CatalogRepository,
    private val notificationRepository: NotificationRepository,
    authRepository: AuthRepository,
) : ViewModel() {

    /** Drives the badge on the bell. */
    val unreadCount: StateFlow<Int> = notificationRepository.observeUnreadCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        // Combined in two groups so each `combine` stays a typed overload.
        val rails = combine(
            catalogRepository.observeFeatured(8),
            catalogRepository.observeTrending(8),
            catalogRepository.observeNewArrivals(6),
        ) { featured, trending, newArrivals ->
            Triple(featured, trending, newArrivals)
        }
        val extras = combine(
            catalogRepository.observeRecommended(6),
            catalogRepository.observeRecentlyViewed(8),
        ) { recommended, recentlyViewed ->
            recommended to recentlyViewed
        }

        viewModelScope.launch {
            combine(
                authRepository.currentUser,
                catalogRepository.observeCategories(),
                rails,
                extras,
            ) { user, categories, railGroup, extraGroup ->
                HomeUiState(
                    user = user,
                    categories = categories,
                    featured = railGroup.first,
                    trending = railGroup.second,
                    newArrivals = railGroup.third,
                    recommended = extraGroup.first,
                    recentlyViewed = extraGroup.second,
                    isLoading = false,
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun consumeMessage() = _uiState.value.let { _uiState.value = it.copy(message = null) }

    fun retry() {
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        viewModelScope.launch { catalogRepository.refreshCategories() }
    }
}
