package app.tab.store.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.StarHalf
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import app.tab.store.data.model.AppStatus
import app.tab.store.ui.theme.LocalSproutlyTokens

@Composable
fun SproutlyPill(
    label: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    background: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    foreground: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    Row(
        modifier = modifier
            .background(background, SproutlyShapes.Pill)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (icon != null) Icon(icon, contentDescription = null, tint = foreground, modifier = Modifier.size(12.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = foreground)
    }
}

/** Paid placement. The word, not just a colour - required by the ad policy. */
@Composable
fun SponsoredBadge(modifier: Modifier = Modifier) {
    SproutlyPill(
        label = "Sponsored",
        modifier = modifier,
        icon = Icons.Outlined.Campaign,
        background = LocalSproutlyTokens.current.heroSurface,
        foreground = LocalSproutlyTokens.current.olive,
    )
}

@Composable
fun StatusChip(status: AppStatus, modifier: Modifier = Modifier) {
    val tokens = LocalSproutlyTokens.current
    val (bg, fg) = when (status) {
        AppStatus.APPROVED -> tokens.successContainer to tokens.success
        AppStatus.PENDING_REVIEW -> tokens.warningContainer to tokens.warning
        AppStatus.REJECTED, AppStatus.SUSPENDED -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
        AppStatus.DRAFT -> MaterialTheme.colorScheme.surfaceContainerHigh to MaterialTheme.colorScheme.onSurfaceVariant
    }
    SproutlyPill(label = status.label, modifier = modifier, background = bg, foreground = fg)
}

@Composable
fun RatingStars(
    rating: Double,
    modifier: Modifier = Modifier,
    count: Int? = null,
) {
    val filled = LocalSproutlyTokens.current.olive
    val empty = MaterialTheme.colorScheme.outline
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        for (i in 1..5) {
            val icon = when {
                rating >= i -> Icons.Outlined.Star
                rating >= i - 0.5 -> Icons.Outlined.StarHalf
                else -> Icons.Outlined.Star
            }
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (rating >= i - 0.5) filled else empty.copy(alpha = 0.5f),
                modifier = Modifier.size(13.dp),
            )
        }
        if (count != null) {
            Text(
                text = " ($count)",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
