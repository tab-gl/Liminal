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
import kotlinx.coroutines.flow.map
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
    /**
     * A greeting that varies, but not on every recomposition.
     *
     * The day and the hour pick the phrase, so it is stable while you are on the
     * screen and different tomorrow — the way a person says hello, rather than one
     * string baked into the layout.
     */
    val greeting: String
        get() {
            val now = java.util.Calendar.getInstance()
            val hour = now.get(java.util.Calendar.HOUR_OF_DAY)
            val day = now.get(java.util.Calendar.DAY_OF_YEAR)
            val openers = when (hour) {
                in 5..11 -> listOf("Good morning", "Morning", "Hello, early bird", "Good morning")
                in 12..16 -> listOf("Good afternoon", "Afternoon", "Hello again", "Good afternoon")
                in 17..20 -> listOf("Good evening", "Evening", "Hello again", "Good evening")
                else -> listOf("Still up", "Late one", "Good night", "Welcome back")
            }
            return openers[day % openers.size]
        }

    /** The line under the greeting: what this screen is for, said differently each day. */
    val subtitle: String
        get() {
            val day = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_YEAR)
            val lines = listOf(
                "Beyond the ordinary. Into discovery.",
                "Something new was published today.",
                "Find the app nobody has told you about yet.",
                "Small studios, interesting things.",
                "Beyond the ordinary. Into discovery.",
            )
            return lines[day % lines.size]
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
        // One listener for the whole storefront: the four rails are views of the
        // same snapshot, so opening a query per rail downloaded the same documents
        // four times.
        val catalogue = catalogRepository.observeCatalogue(60)
        val rails = catalogue.map { items ->
            val featured = items.filter { it.featured }.sortedByDescending { it.unitsSold }
                .ifEmpty { items.sortedByDescending { it.unitsSold } }
                .take(8)
            Triple(
                featured.withoutGames(),
                items.sortedByDescending { it.unitsSold }.take(8).withoutGames(),
                items.sortedByDescending { it.createdAt }.take(6).withoutGames(),
            )
        }
        val extras = combine(
            catalogue.map { items -> items.sortedByDescending { it.rating }.take(6).withoutGames() },
            catalogRepository.observeRecentlyViewed(8),
        ) { recommended, recentlyViewed ->
            recommended to recentlyViewed.withoutGames()
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

/** Games live in their own destination, so the Apps rails skip them. */
private fun List<Product>.withoutGames(): List<Product> = filterNot {
    it.categoryId.equals("games", ignoreCase = true) ||
        it.categoryLabel.equals("Games", ignoreCase = true)
}
