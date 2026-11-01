package app.tab.store.feature.categories

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import app.tab.store.data.model.AppCategory
import app.tab.store.data.model.AppListing
import app.tab.store.data.model.SproutlyCategories
import app.tab.store.di.ServiceGraph
import app.tab.store.ui.components.AppCardSkeleton
import app.tab.store.ui.components.AppListingCard
import app.tab.store.ui.components.EmptyState
import app.tab.store.ui.components.SproutlyCard
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CategoriesViewModel : ViewModel() {
    private val _items = MutableStateFlow<List<AppListing>>(emptyList())
    val items: StateFlow<List<AppListing>> = _items.asStateFlow()
    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    fun load(categoryId: String) {
        _loading.value = true
        viewModelScope.launch {
            runCatching { ServiceGraph.applications.feed(limit = 20, categoryId = categoryId) }
                .onSuccess { _items.value = it.items }
                .onFailure { _items.value = emptyList() }
            _loading.value = false
        }
    }
}

/**
 * The taxonomy, then that category's listings in place - one screen, two states,
 * no extra route to keep in sync with the navigation graph.
 */
@Composable
fun CategoriesScreen(
    onOpenCategory: (String) -> Unit,
    bottomPadding: Dp = 0.dp,
    viewModel: CategoriesViewModel = viewModel(),
) {
    var selected by remember { mutableStateOf<AppCategory?>(null) }
    val items by viewModel.items.collectAsState()
    val loading by viewModel.loading.collectAsState()

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = bottomPadding + 24.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(2) }) {
            Column {
                Text(
                    text = if (selected == null) "Categories" else selected!!.label,
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    text = if (selected == null) "Find your people by what they build" else selected!!.blurb,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (selected != null) {
                    Spacer(Modifier.height(6.dp))
                    app.tab.store.ui.components.SproutlyTextButton(
                        label = "Back to categories",
                        onClick = { selected = null },
                    )
                }
                Spacer(Modifier.height(6.dp))
            }
        }

        if (selected == null) {
            items(SproutlyCategories) { category ->
                SproutlyCard(
                    onClick = {
                        selected = category
                        viewModel.load(category.id)
                    },
                    padding = PaddingValues(14.dp),
                ) {
                    Text(category.label, style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = category.blurb,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                    )
                }
            }
        } else if (loading) {
            items(3) { AppCardSkeleton(modifier = Modifier.fillMaxWidth()) }
        } else if (items.isEmpty()) {
            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(2) }) {
                EmptyState(
                    title = "No apps in ${selected!!.label} yet",
                    message = "Be the first to publish here - it takes about five minutes.",
                )
            }
        } else {
            items(items.size) { index ->
                val listing = items[index]
                AppListingCard(listing = listing, onClick = { onOpenCategory(listing.id) })
            }
        }
    }
}
