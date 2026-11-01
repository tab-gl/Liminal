package app.tab.store.feature.seller

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.tab.store.core.designsystem.EmptyState
import app.tab.store.core.designsystem.SproutlyImage
import app.tab.store.core.designsystem.SproutlyRadii
import app.tab.store.core.designsystem.SproutlyTopBar
import app.tab.store.core.designsystem.formatPrice
import app.tab.store.core.utils.DateFormatters
import app.tab.store.domain.model.Order
import app.tab.store.domain.model.OrderStatus
import app.tab.store.feature.orders.OrderStatusChip

private fun nextStatus(order: Order): Pair<OrderStatus, String>? = when (order.status) {
    OrderStatus.PENDING -> OrderStatus.CONFIRMED to "Confirm order"
    OrderStatus.CONFIRMED -> OrderStatus.PACKED to "Mark packed"
    OrderStatus.PACKED -> OrderStatus.SHIPPED to "Mark shipped"
    OrderStatus.SHIPPED -> OrderStatus.DELIVERED to "Mark delivered"
    OrderStatus.DELIVERED, OrderStatus.CANCELLED -> null
}

@Composable
fun SellerOrdersScreen(
    onBack: () -> Unit,
    viewModel: SellerOrdersViewModel = hiltViewModel(),
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
                title = "Orders",
                subtitle = "${state.orders.size} total",
                onBack = onBack,
            )
            if (!state.isLoading && state.orders.isEmpty()) {
                EmptyState(
                    title = "No orders yet",
                    message = "Orders placed at your store will appear here for fulfilment.",
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(state.orders, key = { it.id }) { order ->
                        Surface(
                            shape = SproutlyRadii.card,
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(Modifier.padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            "#${order.id.take(8).uppercase()}",
                                            style = MaterialTheme.typography.titleSmall,
                                        )
                                        Text(
                                            DateFormatters.dateTime(order.placedAt),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    OrderStatusChip(order.status)
                                }
                                Spacer(Modifier.height(8.dp))
                                order.items.forEach { item ->
                                    Row(
                                        modifier = Modifier.padding(vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        SproutlyImage(
                                            url = item.imageUrl,
                                            contentDescription = item.title,
                                            modifier = Modifier.size(38.dp),
                                            corner = 10.dp,
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            "${item.title} × ${item.quantity}",
                                            style = MaterialTheme.typography.bodySmall,
                                            modifier = Modifier.weight(1f),
                                            maxLines = 1,
                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                        )
                                        Text(
                                            formatPrice(item.lineTotal),
                                            style = MaterialTheme.typography.bodySmall,
                                        )
                                    }
                                }
                                Spacer(Modifier.height(6.dp))
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                Spacer(Modifier.height(6.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            order.buyerName.ifBlank { "Customer" },
                                            style = MaterialTheme.typography.titleSmall,
                                        )
                                        Text(
                                            order.address?.oneLine.orEmpty(),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 2,
                                        )
                                    }
                                    nextStatus(order)?.let { (next, label) ->
                                        TextButton(
                                            onClick = { viewModel.advance(order, next) },
                                            enabled = state.updatingId != order.id,
                                        ) {
                                            Text(
                                                if (state.updatingId == order.id) "Updating…" else label,
                                                style = MaterialTheme.typography.labelMedium,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    item { Spacer(Modifier.height(20.dp)) }
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
