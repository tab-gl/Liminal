package app.tab.store.feature.orders

import app.tab.store.core.ui.Tokens
import androidx.compose.foundation.background
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
import app.tab.store.core.ui.Surface
import app.tab.store.core.ui.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.tab.store.core.designsystem.SproutlyButton
import app.tab.store.core.designsystem.SproutlyImage
import app.tab.store.core.designsystem.SproutlyRadii
import app.tab.store.core.designsystem.SproutlySecondaryButton
import app.tab.store.core.designsystem.SuccessCheck
import app.tab.store.core.designsystem.formatPrice

@Composable
fun OrderPlacedScreen(
    orderId: String,
    onTrackOrder: (String) -> Unit,
    onKeepShopping: () -> Unit,
    viewModel: OrderPlacedViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val order = state.order

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(36.dp))
        SuccessCheck(size = 84.dp)
        Spacer(Modifier.height(20.dp))
        Text(
            "Order placed",
            style = Tokens.typography.displaySmall,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Installed. We will tell you when there is an update.",
            style = Tokens.typography.bodyMedium,
            color = Tokens.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))

        Surface(
            shape = SproutlyRadii.card,
            color = Tokens.colorScheme.surface,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(16.dp)) {
                Row {
                    Text(
                        "Install",
                        style = Tokens.typography.bodyMedium,
                        color = Tokens.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        "#${orderId.take(8).uppercase()}",
                        style = Tokens.typography.titleSmall,
                    )
                }
                if (order != null) {
                    Spacer(Modifier.height(10.dp))
                    order.items.take(3).forEach { item ->
                        Row(
                            modifier = Modifier.padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            SproutlyImage(
                                url = item.imageUrl,
                                contentDescription = item.title,
                                modifier = Modifier.size(42.dp),
                                corner = 10.dp,
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "${item.title} × ${item.quantity}",
                                style = Tokens.typography.bodySmall,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            )
                            Text(formatPrice(item.lineTotal), style = Tokens.typography.bodySmall)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(Tokens.colorScheme.outlineVariant),
                    )
                    Spacer(Modifier.height(8.dp))
                    Row {
                        Text("Total", style = Tokens.typography.titleSmall)
                        Spacer(Modifier.weight(1f))
                        Text(formatPrice(order.total), style = Tokens.typography.titleSmall)
                    }
                }
            }
        }

        Spacer(Modifier.weight(1f))
        SproutlyButton(
            text = "Track this order",
            onClick = { onTrackOrder(orderId) },
        )
        Spacer(Modifier.height(10.dp))
        SproutlySecondaryButton(
            text = "Keep shopping",
            onClick = onKeepShopping,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
    }
}
