package app.tab.store.feature.games

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import app.tab.store.core.designsystem.SproutlyIcon
import app.tab.store.core.ui.IconButton
import app.tab.store.core.ui.Surface
import app.tab.store.core.utils.InstallCount
import app.tab.store.core.ui.Tokens
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
import app.tab.store.core.ui.Text
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
    var searchOpen by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 148.dp),
    ) {
        item {
            Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Games", style = Tokens.typography.headlineMedium)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        InstallCount.label(state.games.size),
                        style = Tokens.typography.labelLarge,
                        color = Tokens.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = { searchOpen = true }) {
                        SproutlyIcon(PhosphorIcons.Search, contentDescription = "Search games")
                    }
                }
                Text(
                    "Play something new",
                    style = Tokens.typography.bodyMedium,
                    color = Tokens.colorScheme.onSurfaceVariant,
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
        AnimatedVisibility(
            visible = searchOpen,
            enter = fadeIn(tween(160)),
            exit = fadeOut(tween(140)),
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.28f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { searchOpen = false },
            )
        }
        AnimatedVisibility(
            visible = searchOpen,
            enter = slideInVertically(spring(dampingRatio = 0.86f, stiffness = 420f)) { it },
            exit = slideOutVertically(tween(200)) { it },
            modifier = Modifier.fillMaxSize(),
        ) {
            Surface(
                shape = androidx.compose.foundation.shape.RoundedCornerShape(
                    topStart = 34.dp, topEnd = 34.dp,
                ),
                color = Tokens.colorScheme.background,
                modifier = Modifier.fillMaxSize(),
            ) {
                app.tab.store.feature.search.SearchPanel(
                    onProduct = { id ->
                        searchOpen = false
                        onProduct(id)
                    },
                    onClose = { searchOpen = false },
                    title = { Text("Search games", style = Tokens.typography.headlineLarge) },
                )
            }
        }
    }
}
