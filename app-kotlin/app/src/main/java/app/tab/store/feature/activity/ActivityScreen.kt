package app.tab.store.feature.activity

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
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import app.tab.store.ui.components.EmptyState
import app.tab.store.ui.components.SproutlyCard
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ActivityScreen(
    bottomPadding: Dp = 0.dp,
    viewModel: ActivityViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = bottomPadding + 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Column {
                Text("Activity", style = MaterialTheme.typography.headlineSmall)
                Text(
                    text = "Approvals, reviews and campaign alerts",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
            }
        }

        if (state.items.isEmpty() && !state.loading) {
            item {
                EmptyState(
                    title = "Nothing yet",
                    message = "When your app is approved, reviewed, or a campaign needs " +
                        "attention, it shows up here.",
                )
            }
        }

        items(state.items, key = { it.id }) { item ->
            SproutlyCard(
                onClick = { if (item.isUnread) viewModel.markRead(item.id) },
                color = if (item.isUnread) {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                } else {
                    MaterialTheme.colorScheme.surfaceContainerLowest
                },
            ) {
                Row {
                    Column(Modifier.weight(1f)) {
                        Text(item.title, style = MaterialTheme.typography.titleSmall)
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = item.body,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(5.dp))
                        Text(
                            text = item.createdAt?.let { formatDate(it) } ?: "",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (item.isUnread) {
                        Spacer(Modifier.width(8.dp))
                        app.tab.store.ui.components.SproutlyPill(label = "New")
                    }
                }
            }
        }
    }
}

private fun formatDate(date: Date): String =
    SimpleDateFormat("d MMM, HH:mm", Locale.getDefault()).format(date)
