package app.tab.store.feature.bookmarks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.tab.store.data.model.AppListing
import app.tab.store.di.ServiceGraph
import app.tab.store.ui.components.AppCardSkeleton
import app.tab.store.ui.components.AppListingCard
import app.tab.store.ui.components.EmptyState
import app.tab.store.ui.components.SproutlyTextButton

@Composable
fun BookmarksScreen(
    onOpenApp: (String) -> Unit,
    onBack: () -> Unit,
    bottomPadding: Dp = 0.dp,
) {
    var items by remember { mutableStateOf<List<AppListing>?>(null) }
    val uid = ServiceGraph.auth.uid

    LaunchedEffect(uid) {
        items = if (uid == null) {
            emptyList()
        } else {
            runCatching { ServiceGraph.bookmarks.saved(uid) }.getOrDefault(emptyList())
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = bottomPadding + 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Column {
                SproutlyTextButton(label = "Back", onClick = onBack)
                Text("Saved apps", style = MaterialTheme.typography.headlineSmall)
                Text(
                    text = "Everything you bookmarked, newest first",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
        }

        when {
            items == null -> items(3) { AppCardSkeleton() }
            items!!.isEmpty() -> item {
                EmptyState(
                    title = "Nothing saved yet",
                    message = "Tap Save on any listing and it lands here for later.",
                )
            }
            else -> items(items!!, key = { it.id }) { listing ->
                AppListingCard(listing = listing, onClick = { onOpenApp(listing.id) })
            }
        }
    }
}
