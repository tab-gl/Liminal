package app.tab.store.feature.dashboard

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import app.tab.store.data.model.DailyStat
import app.tab.store.ui.components.AppListingCard
import app.tab.store.ui.components.EmptyState
import app.tab.store.ui.components.ScreenLoader
import app.tab.store.ui.components.SproutlyCard
import app.tab.store.ui.components.SproutlyTextButton
import app.tab.store.ui.theme.LocalSproutlyTokens
import java.text.NumberFormat
import java.util.Locale

@Composable
fun DashboardScreen(
    onBack: () -> Unit,
    bottomPadding: Dp = 0.dp,
    viewModel: DashboardViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()

    if (state.loading) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) { ScreenLoader(label = "Loading analytics") }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = bottomPadding + 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column {
                SproutlyTextButton(label = "Back", onClick = onBack)
                Text("Dashboard", style = MaterialTheme.typography.headlineSmall)
                Text(
                    text = "Everything below is aggregated server-side from real events",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (state.apps.isEmpty()) {
            item {
                EmptyState(
                    title = "No listings yet",
                    message = "Publish an app and its numbers appear here within the hour.",
                )
            }
            return@LazyColumn
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricTile("Views", compact(state.totalViews))
                MetricTile("Clicks", compact(state.totalClicks))
                MetricTile("CTR", "${(state.clickThroughRate * 100).roundToInt()}%")
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricTile("Saved", compact(state.totalBookmarks))
                MetricTile("Reviews", compact(state.totalReviews))
                MetricTile("Rating", if (state.averageRating == 0.0) "-" else String.format(Locale.US, "%.1f", state.averageRating))
            }
        }

        item {
            SproutlyCard {
                Text("Views, last 30 days", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(10.dp))
                ViewsChart(series = state.series)
            }
        }

        item {
            Text("Your listings", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 8.dp))
        }

        items(state.apps, key = { it.id }) { listing ->
            AppListingCard(
                listing = listing,
                onClick = { viewModel.selectApp(listing.id) },
                showDeveloper = false,
            )
        }
    }
}

@Composable
private fun MetricTile(label: String, value: String) {
    SproutlyCard(modifier = Modifier.fillMaxWidth(0.32f), padding = PaddingValues(12.dp)) {
        Text(value, style = MaterialTheme.typography.headlineSmall)
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * A plain bar chart drawn on a Canvas.
 *
 * No charting dependency: thirty thin bars with a 2dp corner and no gridlines
 * is the whole requirement, and a library would add weight for less control.
 */
@Composable
private fun ViewsChart(series: List<DailyStat>) {
    val barColor = LocalSproutlyTokens.current.olive
    val emptyColor = MaterialTheme.colorScheme.surfaceContainerHigh

    if (series.isEmpty()) {
        Text(
            text = "No activity recorded yet.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }

    val max = series.maxOf { it.views }.coerceAtLeast(1)
    Canvas(modifier = Modifier.fillMaxWidth().height(120.dp)) {
        val count = series.size
        val gap = 3.dp.toPx()
        val barWidth = ((size.width - gap * (count - 1)) / count).coerceAtLeast(1f)
        series.forEachIndexed { index, stat ->
            val fraction = stat.views.toFloat() / max
            val barHeight = (size.height * fraction).coerceAtLeast(if (stat.views > 0) 3f else 2f)
            drawRect(
                color = if (stat.views > 0) barColor else emptyColor,
                topLeft = Offset(index * (barWidth + gap), size.height - barHeight),
                size = Size(barWidth, barHeight),
            )
        }
    }
}

private fun compact(value: Int): String = when {
    value >= 1_000_000 -> "${value / 1_000_000}M"
    value >= 1_000 -> "${value / 1_000}k"
    else -> NumberFormat.getInstance(Locale.US).format(value)
}

private fun Double.roundToInt(): Int = kotlin.math.round(this).toInt()
