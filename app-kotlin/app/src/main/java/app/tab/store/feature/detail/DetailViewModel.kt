package app.tab.store.feature.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.tab.store.data.model.AppListing
import app.tab.store.data.model.Review
import app.tab.store.di.ServiceGraph
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DetailUiState(
    val listing: AppListing? = null,
    val reviews: List<Review> = emptyList(),
    val bookmarked: Boolean = false,
    val loading: Boolean = true,
    val error: String? = null,
)

class DetailViewModel(private val appId: String) : ViewModel() {

    private val applications = ServiceGraph.applications
    private val bookmarks = ServiceGraph.bookmarks
    private val _state = MutableStateFlow(DetailUiState())
    val state: StateFlow<DetailUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            applications.watch(appId).collect { listing ->
                _state.update {
                    it.copy(
                        listing = listing,
                        loading = listing == null,
                        error = if (listing == null) "This listing is not available." else null,
                    )
                }
            }
        }
        viewModelScope.launch {
            applications.reviews(appId).collect { reviews ->
                _state.update { it.copy(reviews = reviews) }
            }
        }
        viewModelScope.launch {
            bookmarks.watch(ServiceGraph.auth.uid, appId).collect { saved ->
                _state.update { it.copy(bookmarked = saved) }
            }
        }
        // A view is a product event; the hourly function folds it into the count.
        viewModelScope.launch {
            applications.recordEvent(appId, "view", ServiceGraph.auth.uid)
        }
    }

    fun toggleBookmark() {
        val uid = ServiceGraph.auth.uid ?: return
        val listing = _state.value.listing ?: return
        viewModelScope.launch {
            runCatching { bookmarks.toggle(uid, listing.id, listing.name) }
        }
    }

    fun recordClick(url: String) {
        viewModelScope.launch {
            applications.recordEvent(appId, "click", ServiceGraph.auth.uid)
        }
    }
}
