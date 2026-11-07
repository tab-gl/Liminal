package app.tab.store.feature.library

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import app.tab.store.core.ui.Tokens
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.tab.store.core.designsystem.EmptyState
import app.tab.store.core.designsystem.LoadingColumn
import app.tab.store.core.designsystem.SproutlyImage
import app.tab.store.core.designsystem.SproutlyRadii
import app.tab.store.core.designsystem.SproutlyTopBar
import app.tab.store.domain.model.InstalledApp
import app.tab.store.domain.repository.AppLibraryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class MyAppsUiState(val apps: List<InstalledApp> = emptyList(), val loading: Boolean = true)

@HiltViewModel
class MyAppsViewModel @Inject constructor(repository: AppLibraryRepository) : ViewModel() {
    val state: StateFlow<MyAppsUiState> = repository.observeMyApps()
        .map { MyAppsUiState(it, false) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MyAppsUiState())
}

@Composable
fun MyAppsScreen(onProduct: (String) -> Unit, onBrowse: () -> Unit, viewModel: MyAppsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize()) {
        SproutlyTopBar(title = "My apps", subtitle = state.apps.size.takeIf { it > 0 }?.let { "$it installed" })
        when {
            state.loading -> LoadingColumn(Modifier.padding(horizontal = 20.dp))
            state.apps.isEmpty() -> EmptyState(
                title = "Your library is empty",
                message = "Apps you install will be saved here.",
                actionLabel = "Browse apps",
                onAction = onBrowse,
            )
            else -> LazyColumn(
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(state.apps, key = { it.productId }) { app ->
                    Surface(
                        onClick = { onProduct(app.productId) },
                        shape = SproutlyRadii.card,
                        color = Tokens.colorScheme.surface,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            SproutlyImage(app.imageUrl, app.title, Modifier.size(54.dp), corner = 12.dp)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(app.title, style = Tokens.typography.titleSmall, maxLines = 1)
                                Text(
                                    listOfNotNull(app.storeName.takeIf(String::isNotBlank), app.versionName.takeIf(String::isNotBlank)?.let { "v$it" }).joinToString(" · "),
                                    style = Tokens.typography.bodySmall,
                                    color = Tokens.colorScheme.onSurfaceVariant,
                                )
                            }
                            Text("Open", style = Tokens.typography.labelMedium, color = Tokens.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }
}
