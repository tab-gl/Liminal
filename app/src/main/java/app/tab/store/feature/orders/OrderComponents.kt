package app.tab.store.feature.orders

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import app.tab.store.core.designsystem.HugeIcons
import app.tab.store.core.designsystem.HugeIcon
import app.tab.store.core.designsystem.SproutlyPalette
import app.tab.store.core.designsystem.SproutlyTag
import app.tab.store.core.utils.DateFormatters
import app.tab.store.domain.model.OrderEvent
import app.tab.store.domain.model.OrderStatus

@Composable
fun OrderStatusChip(status: OrderStatus, modifier: Modifier = Modifier) {
    val (container, content) = when (status) {
        OrderStatus.PENDING -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
        OrderStatus.CONFIRMED -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        OrderStatus.PACKED -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        OrderStatus.SHIPPED -> SproutlyPalette.LimeSoft to SproutlyPalette.Olive
        OrderStatus.DELIVERED -> SproutlyPalette.Lime to SproutlyPalette.OliveDeep
        OrderStatus.CANCELLED -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
    }
    SproutlyTag(
        text = status.name,
        container = container,
        content = content,
        modifier = modifier,
    )
}

fun statusLabel(status: OrderStatus): String = when (status) {
    OrderStatus.PENDING -> "Awaiting confirmation"
    OrderStatus.CONFIRMED -> "Confirmed by developer"
    OrderStatus.PACKED -> "Processing"
    OrderStatus.SHIPPED -> "Ready to install"
    OrderStatus.DELIVERED -> "Installed"
    OrderStatus.CANCELLED -> "Cancelled"
}

@Composable
fun OrderTimeline(events: List<OrderEvent>, currentStatus: OrderStatus, modifier: Modifier = Modifier) {
    val steps = listOf(
        OrderStatus.PENDING,
        OrderStatus.CONFIRMED,
        OrderStatus.PACKED,
        OrderStatus.SHIPPED,
        OrderStatus.DELIVERED,
    )
    val currentIndex = steps.indexOf(currentStatus).let { if (it < 0) 0 else it }

    Column(modifier = modifier.fillMaxWidth()) {
        steps.forEachIndexed { index, step ->
            val done = index <= currentIndex && currentStatus != OrderStatus.CANCELLED
            val event = events.lastOrNull { it.status == step }
            Row {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(
                                if (done) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceContainerHigh,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (done) {
                            Icon(
                                HugeIcons.Tick,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(13.dp),
                            )
                        }
                    }
                    if (index != steps.lastIndex) {
                        Box(
                            Modifier
                                .width(2.dp)
                                .height(34.dp)
                                .background(
                                    if (index < currentIndex) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceContainerHigh,
                                ),
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.padding(bottom = 18.dp)) {
                    Text(
                        statusLabel(step),
                        style = MaterialTheme.typography.titleSmall,
                        color = if (done) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        event?.let { DateFormatters.dateTime(it.at) } ?: "Pending",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        if (currentStatus == OrderStatus.CANCELLED) {
            Text(
                "This order was cancelled. Any amount collected is refunded to the original payment method.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}
