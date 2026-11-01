package app.tab.store.feature.cart

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ShoppingBag
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
import app.tab.store.core.designsystem.QuantityStepper
import app.tab.store.core.designsystem.SproutlyButton
import app.tab.store.core.designsystem.SproutlyImage
import app.tab.store.core.designsystem.SproutlyRadii
import app.tab.store.core.designsystem.SproutlyTag
import app.tab.store.core.designsystem.SproutlyTopBar
import app.tab.store.core.designsystem.formatPrice
import app.tab.store.domain.model.CartLine

@Composable
fun CartScreen(
    onCheckout: () -> Unit,
    onProduct: (String) -> Unit,
    onContinueShopping: () -> Unit,
    viewModel: CartViewModel = hiltViewModel(),
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
                title = "Your cart",
                subtitle = if (state.cart.isEmpty) null
                else "${state.cart.itemCount} item${if (state.cart.itemCount == 1) "" else "s"}",
                actions = {
                    if (!state.cart.isEmpty) {
                        TextButton(onClick = viewModel::clear) {
                            Text("Clear", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                },
            )

            if (state.cart.isEmpty) {
                EmptyState(
                    title = "Your cart is empty",
                    message = "Browse the marketplace and add something you like.",
                    icon = Icons.Outlined.ShoppingBag,
                    actionLabel = "Start shopping",
                    onAction = onContinueShopping,
                )
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        horizontal = 20.dp,
                        vertical = 8.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (state.cart.hasUnavailable) {
                        item {
                            Surface(
                                color = MaterialTheme.colorScheme.errorContainer,
                                shape = SproutlyRadii.card,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(
                                    "Some items are no longer available and will not be charged.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.padding(12.dp),
                                )
                            }
                        }
                    }
                    items(state.cart.lines, key = { it.productId }) { line ->
                        CartLineRow(
                            line = line,
                            onClick = { onProduct(line.productId) },
                            onIncrease = { viewModel.increment(line.productId) },
                            onDecrease = { viewModel.decrement(line.productId) },
                            onRemove = { viewModel.remove(line.productId) },
                        )
                    }
                    item { Spacer(Modifier.height(8.dp)) }
                }

                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = SproutlyRadii.sheet,
                    tonalElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(20.dp)) {
                        if (state.remainingForFreeShipping > 0) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = SproutlyRadii.pill,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(
                                    "Add ${formatPrice(state.remainingForFreeShipping)} more for free delivery",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                )
                            }
                            Spacer(Modifier.height(12.dp))
                        }
                        SummaryRow("Subtotal", formatPrice(state.cart.subtotal))
                        SummaryRow(
                            "Delivery",
                            if (state.shipping == 0L) "Free" else formatPrice(state.shipping),
                        )
                        Spacer(Modifier.height(8.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Spacer(Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Total", style = MaterialTheme.typography.titleLarge)
                            Spacer(Modifier.weight(1f))
                            Text(
                                formatPrice(state.total),
                                style = MaterialTheme.typography.headlineSmall,
                            )
                        }
                        Spacer(Modifier.height(14.dp))
                        SproutlyButton(
                            text = "Checkout",
                            onClick = onCheckout,
                            enabled = !state.cart.isEmpty && !state.cart.hasUnavailable,
                        )
                    }
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
private fun SummaryRow(label: String, value: String) {
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

@Composable
private fun CartLineRow(
    line: CartLine,
    onClick: () -> Unit,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    onRemove: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = SproutlyRadii.card,
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(10.dp)) {
            SproutlyImage(
                url = line.imageUrl,
                contentDescription = line.title,
                modifier = Modifier.size(82.dp),
                corner = 14.dp,
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    line.storeName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    line.title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
                if (!line.isAvailable) {
                    SproutlyTag(
                        text = "UNAVAILABLE",
                        container = MaterialTheme.colorScheme.errorContainer,
                        content = MaterialTheme.colorScheme.onErrorContainer,
                    )
                } else {
                    Text(
                        formatPrice(line.unitPrice),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    QuantityStepper(
                        quantity = line.quantity,
                        onDecrease = onDecrease,
                        onIncrease = onIncrease,
                        max = line.stock,
                        enabled = line.isAvailable,
                    )
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = onRemove) {
                        Text(
                            "Remove",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
