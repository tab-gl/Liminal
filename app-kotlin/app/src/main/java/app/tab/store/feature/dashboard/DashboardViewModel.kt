package app.tab.store.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.tab.store.data.model.AppListing
import app.tab.store.data.model.DailyStat
import app.tab.store.di.ServiceGraph
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DashboardUiState(
    val apps: List<AppListing> = emptyList(),
    val series: List<DailyStat> = emptyList(),
    val selectedAppId: String? = null,
    val loading: Boolean = true,
    val error: String? = null,
) {
    val totalViews: Int get() = apps.sumOf { it.viewCount }
    val totalClicks: Int get() = apps.sumOf { it.clickCount }
    val totalBookmarks: Int get() = apps.sumOf { it.bookmarkCount }
    val totalReviews: Int get() = apps.sumOf { it.reviewCount }
    val averageRating: Double
        get() = apps.filter { it.reviewCount > 0 }.map { it.rating }.average()
            .takeIf { !it.isNaN() } ?: 0.0
    val clickThroughRate: Double
        get() = if (totalViews == 0) 0.0 else totalClicks.toDouble() / totalViews
}

class DashboardViewModel : ViewModel() {

    private val _state = MutableStateFlow(DashboardUiState())
    val state: StateFlow<DashboardUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        val uid = ServiceGraph.auth.uid ?: return
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            runCatching { ServiceGraph.publishing.myApps(uid) }
                .onSuccess { apps ->
                    _state.update { it.copy(apps = apps, loading = false) }
                    val first = apps.firstOrNull()?.id
                    if (first != null) selectApp(first)
                }
                .onFailure { error ->
                    _state.update { it.copy(loading = false, error = error.message ?: "Could not load") }
                }
        }
    }

    fun selectApp(appId: String) {
        _state.update { it.copy(selectedAppId = appId) }
        viewModelScope.launch {
            runCatching { ServiceGraph.applications.series(appId) }
                .onSuccess { stats -> _state.update { it.copy(series = stats) } }
        }
    }
}
