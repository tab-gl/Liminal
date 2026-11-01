package app.tab.store.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.tab.store.data.model.AppListing
import app.tab.store.data.model.categoryById
import app.tab.store.ui.theme.LocalSproutlyTokens

/**
 * The listing card.
 *
 * Carries the whole decision set at a glance: icon, name, pitch, category,
 * developer, rating and review count, pricing, and an unmissable Sponsored label
 * when the placement is paid.
 */
@Composable
fun AppListingCard(
    listing: AppListing,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showDeveloper: Boolean = true,
) {
    val category = categoryById(listing.categoryId)
    SproutlyCard(modifier = modifier.fillMaxWidth(), onClick = onClick) {
        Row {
            AppImage(url = listing.iconUrl, size = 56.dp, contentDescription = listing.name)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = listing.name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    if (listing.sponsored) SponsoredBadge()
                }
                if (listing.tagline.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = listing.tagline,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    if (listing.reviewCount > 0) {
                        RatingStars(rating = listing.rating, count = listing.reviewCount)
                    } else {
                        Text(
                            text = "New",
                            style = MaterialTheme.typography.labelSmall,
                            color = LocalSproutlyTokens.current.success,
                        )
                    }
                    SproutlyPill(label = category.label)
                    SproutlyPill(label = if (listing.isFree) "Free" else listing.pricing.label)
                    if (listing.isOpenSource) SproutlyPill(label = "Open source")
                    if (showDeveloper && listing.ownerName.isNotBlank()) {
                        SproutlyPill(label = listing.ownerName)
                    }
                }
            }
        }
    }
}
