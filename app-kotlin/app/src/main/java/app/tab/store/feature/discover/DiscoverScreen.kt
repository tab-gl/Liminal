package app.tab.store.feature.discover

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import app.tab.store.ads.AdMobBanner
import app.tab.store.data.repo.FeedSort
import app.tab.store.ui.components.AppCardSkeleton
import app.tab.store.ui.components.AppListingCard
import app.tab.store.ui.components.EmptyState
import app.tab.store.ui.components.ShapeMorphLoader
import app.tab.store.ui.components.SproutlyButton
import app.tab.store.ui.components.SproutlyPill
import app.tab.store.ui.components.SproutlyTextButton

@Composable
fun DiscoverScreen(
    onOpenApp: (String) -> Unit,
    bottomPadding: Dp = 0.dp,
    viewModel: DiscoverViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(bottom = bottomPadding + 24.dp),
    ) {
        item {
            Column(Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                Text(
                    text = "Build it. Publish it. Grow it.",
                    style = MaterialTheme.typography.headlineSmall,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "Fresh apps from independent developers",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (state.loading) {
            item {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    ShapeMorphLoader(label = "Loading the feed")
                    Spacer(Modifier.height(8.dp))
                    repeat(3) { AppCardSkeleton() }
                }
            }
            return@LazyColumn
        }

        if (state.error != null) {
            item {
                EmptyState(
                    title = "We could not load the feed",
                    message = state.error!!,
                    actionLabel = "Try again",
                    onAction = { viewModel.refresh() },
                )
            }
            return@LazyColumn
        }

        if (state.featured.isNotEmpty()) {
            item { RailHeader("Featured this week", "Hand-picked by the editors") }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(state.featured, key = { it.id }) { listing ->
                        CompactCard(listing = listing, onClick = { onOpenApp(listing.id) })
                    }
                }
            }
        }

        if (state.trending.isNotEmpty()) {
            item { RailHeader("Trending now", "What people are opening today") }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(state.trending, key = { it.id }) { listing ->
                        CompactCard(listing = listing, onClick = { onOpenApp(listing.id) })
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, top = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("All apps", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.weight(1f))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    FeedSort.entries.take(2).forEach { sort ->
                        SproutlyTextButton(
                            label = sort.label,
                            onClick = { viewModel.setSort(sort) },
                        )
                    }
                }
            }
        }

        items(state.feed, key = { it.id }) { listing ->
            AppListingCard(
                listing = listing,
                onClick = { onOpenApp(listing.id) },
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
            )
        }

        if (state.feed.isEmpty()) {
            item {
                EmptyState(
                    title = "Nothing here yet",
                    message = "No apps match this view. Try a different sort, or publish the first one.",
                    actionLabel = "Refresh",
                    onAction = { viewModel.refresh() },
                )
            }
        }

        if (state.hasMore && state.feed.isNotEmpty()) {
            item {
                LaunchedEffect(state.feed.size) { viewModel.loadMore() }
                Column(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    if (state.loadingMore) ShapeMorphLoader(size = 28.dp)
                }
            }
        }

        // One banner at the end of the feed: after real content, never above it.
        item {
            AdMobBanner(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp))
        }
    }
}

@Composable
private fun RailHeader(title: String, subtitle: String) {
    Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 10.dp)) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        Text(
            subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun CompactCard(listing: app.tab.store.data.model.AppListing, onClick: () -> Unit) {
    app.tab.store.ui.components.SproutlyCard(
        modifier = Modifier.width(156.dp),
        onClick = onClick,
        padding = PaddingValues(12.dp),
    ) {
        app.tab.store.ui.components.AppImage(url = listing.iconUrl, size = 42.dp)
        Spacer(Modifier.height(10.dp))
        Text(
            text = listing.name,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 1,
        )
        Text(
            text = listing.tagline,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
        )
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (listing.reviewCount > 0) {
                app.tab.store.ui.components.RatingStars(rating = listing.rating)
            } else {
                SproutlyPill(label = "New")
            }
        }
    }
}
