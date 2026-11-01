package app.tab.store.feature.discover

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.tab.store.data.model.AppListing
import app.tab.store.data.repo.FeedSort
import app.tab.store.di.ServiceGraph
import com.google.firebase.firestore.DocumentSnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DiscoverUiState(
    val feed: List<AppListing> = emptyList(),
    val featured: List<AppListing> = emptyList(),
    val trending: List<AppListing> = emptyList(),
    val recent: List<AppListing> = emptyList(),
    val sort: FeedSort = FeedSort.NEWEST,
    val loading: Boolean = true,
    val loadingMore: Boolean = false,
    val error: String? = null,
    val hasMore: Boolean = true,
)

/**
 * Discover.
 *
 * One paginated feed plus three rails, loaded in parallel. Every failure path
 * ends in a message the UI can show with a retry, never a blank screen.
 */
class DiscoverViewModel : ViewModel() {

    private val repository = ServiceGraph.applications
    private val _state = MutableStateFlow(DiscoverUiState())
    val state: StateFlow<DiscoverUiState> = _state.asStateFlow()

    private var cursor: DocumentSnapshot? = null

    init {
        refresh()
    }

    fun refresh() {
        cursor = null
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            runCatching {
                val page = repository.feed(limit = 12, sort = _state.value.sort)
                val featured = repository.rail("trendingScore", limit = 6, onlyFeatured = true)
                val trending = repository.rail("trendingScore")
                val recent = repository.rail("createdAt")
                page to Triple(featured, trending, recent)
            }.onSuccess { (page, rails) ->
                cursor = page.cursor
                _state.update {
                    it.copy(
                        feed = page.items,
                        featured = rails.first,
                        trending = rails.second,
                        recent = rails.third,
                        hasMore = page.hasMore,
                        loading = false,
                    )
                }
            }.onFailure { error ->
                _state.update { it.copy(loading = false, error = error.readableMessage()) }
            }
        }
    }

    fun loadMore() {
        val current = _state.value
        if (current.loadingMore || !current.hasMore) return
        _state.update { it.copy(loadingMore = true) }
        viewModelScope.launch {
            runCatching { repository.feed(limit = 12, cursor = cursor, sort = current.sort) }
                .onSuccess { page ->
                    cursor = page.cursor
                    _state.update {
                        it.copy(
                            feed = it.feed + page.items,
                            hasMore = page.hasMore,
                            loadingMore = false,
                        )
                    }
                }
                .onFailure { _state.update { s -> s.copy(loadingMore = false) } }
        }
    }

    fun setSort(sort: FeedSort) {
        _state.update { it.copy(sort = sort) }
        refresh()
    }
}

/** Firebase errors are readable enough to show, once the noise is stripped. */
internal fun Throwable.readableMessage(): String = when {
    message == null -> "Something went wrong. Please try again."
    message!!.contains("PERMISSION_DENIED") -> "You do not have access to this."
    message!!.contains("UNAVAILABLE") -> "You appear to be offline. Check your connection."
    else -> message!!
}
