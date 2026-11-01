package app.tab.store.feature.activity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.tab.store.data.model.AppNotification
import app.tab.store.di.ServiceGraph
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ActivityUiState(
    val items: List<AppNotification> = emptyList(),
    val loading: Boolean = true,
)

class ActivityViewModel : ViewModel() {

    private val repository = ServiceGraph.notifications
    private val _state = MutableStateFlow(ActivityUiState())
    val state: StateFlow<ActivityUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            repository.inbox(ServiceGraph.auth.uid).collect { items ->
                _state.update { it.copy(items = items, loading = false) }
            }
        }
    }

    fun markRead(id: String) {
        viewModelScope.launch { runCatching { repository.markRead(id) } }
    }
}
