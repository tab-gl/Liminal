package app.tab.store.feature.games

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.tab.store.core.designsystem.EmptyState
import app.tab.store.core.designsystem.ProductCard
import app.tab.store.core.designsystem.SectionHeader
import app.tab.store.core.designsystem.PhosphorIcons

/**
 * Games, as a section of their own rather than a filter chip on the app list.
 *
 * Same catalogue, categoryId == "games"; what differs is the framing — its own
 * rails, its own empty state, its own place in the bar.
 */
@Composable
fun GamesScreen(
    onProduct: (String) -> Unit,
    viewModel: GamesViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 148.dp),
    ) {
        item {
            Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Games", style = MaterialTheme.typography.headlineMedium)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "${state.games.size}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    "Play something new",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (!state.isLoading && state.games.isEmpty()) {
            item {
                EmptyState(
                    title = "No games yet",
                    message = "When developers publish games they will appear here.",
                    icon = PhosphorIcons.Gamepad,
                )
            }
        }

        if (state.top.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "Top games",
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                )
            }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(state.top, key = { "top-${it.id}" }) { game ->
                        ProductCard(
                            product = game,
                            onClick = { onProduct(game.id) },
                            modifier = Modifier.width(164.dp),
                            compact = true,
                        )
                    }
                }
            }
        }

        if (state.fresh.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "New releases",
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                )
            }
            items(state.fresh.chunked(2), key = { row -> "fresh-${row.first().id}" }) { row ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    row.forEach { game ->
                        ProductCard(
                            product = game,
                            onClick = { onProduct(game.id) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}
