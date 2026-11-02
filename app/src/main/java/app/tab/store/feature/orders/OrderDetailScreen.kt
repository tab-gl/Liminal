package app.tab.store.feature.orders

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.tab.store.core.designsystem.ErrorState
import app.tab.store.core.designsystem.SproutlyButton
import app.tab.store.core.designsystem.SproutlyImage
import app.tab.store.core.designsystem.SproutlyRadii
import app.tab.store.core.designsystem.SproutlyTopBar
import app.tab.store.core.designsystem.formatPrice
import app.tab.store.core.utils.DateFormatters

@Composable
fun OrderDetailScreen(
    orderId: String,
    onBack: () -> Unit,
    viewModel: OrderDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbar.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            SproutlyTopBar(
                title = "Order #${orderId.take(8).uppercase()}",
                subtitle = state.order?.let { DateFormatters.dateTime(it.placedAt) },
                onBack = onBack,
            )
            val order = state.order
            when {
                state.isLoading || order == null -> ErrorState(
                    message = "We could not load this order.",
                    onRetry = onBack,
                )
                else -> Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            statusLabel(order.status),
                            style = MaterialTheme.typography.headlineSmall,
                        )
                        Spacer(Modifier.weight(1f))
                        OrderStatusChip(order.status)
                    }
                    Spacer(Modifier.height(18.dp))

                    Surface(
                        shape = SproutlyRadii.card,
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            OrderTimeline(order.timeline, order.status)
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    Text("Items", style = MaterialTheme.typography.headlineSmall)
                    Spacer(Modifier.height(8.dp))
                    order.items.forEach { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            SproutlyImage(
                                url = item.imageUrl,
                                contentDescription = item.title,
                                modifier = Modifier.size(56.dp),
                                corner = 12.dp,
                            )
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    item.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 2,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                )
                                Text(
                                    "${item.storeName}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Text(
                                formatPrice(item.lineTotal),
                                style = MaterialTheme.typography.titleSmall,
                            )
                        }
                    }

                    Spacer(Modifier.height(14.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(Modifier.height(12.dp))
                    PriceRow("Subtotal", formatPrice(order.subtotal))
                    PriceRow("Service fee", if (order.shipping == 0L) "Free" else formatPrice(order.shipping))
                    Spacer(Modifier.height(6.dp))
                    Row {
                        Text("Total paid", style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.weight(1f))
                        Text(formatPrice(order.total), style = MaterialTheme.typography.headlineSmall)
                    }

                    if (order.address != null) {
                        Spacer(Modifier.height(20.dp))
                        Text("Billing address", style = MaterialTheme.typography.headlineSmall)
                        Spacer(Modifier.height(6.dp))
                        Surface(
                            shape = SproutlyRadii.card,
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(Modifier.padding(14.dp)) {
                                Text(order.address.fullName, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    order.address.oneLine,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    order.address.phone,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }

                    if (order.canCancel) {
                        Spacer(Modifier.height(22.dp))
                        SproutlyButton(
                            text = if (state.isCancelling) "Cancelling…" else "Cancel",
                            onClick = viewModel::cancel,
                            loading = state.isCancelling,
                        )
                    }
                    Spacer(Modifier.height(28.dp))
                }
            }
        }
        SnackbarHost(
            hostState = snackbar,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp),
        )
    }
}

@Composable
private fun PriceRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}
