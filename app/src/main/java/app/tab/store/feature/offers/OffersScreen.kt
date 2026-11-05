package app.tab.store.feature.offers

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.tab.store.core.ads.BannerAd
import app.tab.store.core.designsystem.EmptyState
import app.tab.store.core.designsystem.SproutlyImage
import app.tab.store.core.designsystem.SproutlyRadii
import app.tab.store.core.designsystem.SproutlyTag
import app.tab.store.core.designsystem.TablerIcons
import app.tab.store.core.designsystem.formatPrice
import app.tab.store.domain.model.Product

/** Discounted apps, ranked by how much they save you. */
@Composable
fun OffersScreen(
    onProduct: (String) -> Unit,
    viewModel: OffersViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 140.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        state.banner?.let { banner ->
            item {
                OfferBanner(product = banner, onClick = { onProduct(banner.id) })
            }
        }

        item {
            BannerAd()
        }

        item {
            Column {
                Text("Offers", style = MaterialTheme.typography.headlineMedium)
                Text(
                    if (state.hasOffers) "${state.offers.size} apps on offer right now"
                    else "Price drops from developers, in one place",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
            }
        }

        if (!state.hasOffers && !state.isLoading) {
            item {
                EmptyState(
                    title = "No offers right now",
                    message = "When a developer discounts an app it shows up here.",
                    icon = TablerIcons.Bag,
                )
            }
        }

        items(state.offers, key = { it.id }) { product ->
            OfferRow(product = product, onClick = { onProduct(product.id) }, modifier = Modifier.animateItem())
        }
    }
}

@Composable
private fun OfferRow(
    product: Product,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        shape = SproutlyRadii.card,
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SproutlyImage(
                url = product.primaryImage,
                contentDescription = product.title,
                modifier = Modifier.size(64.dp),
                corner = 16.dp,
            )
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    product.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                )
                Text(
                    product.storeName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        formatPrice(product.price),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    product.compareAtPrice?.let { was ->
                        Spacer(Modifier.width(8.dp))
                        Text(
                            formatPrice(was),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough,
                        )
                    }
                }
            }
            Box(Modifier.padding(start = 8.dp)) {
                SproutlyTag(
                    text = "-${product.discountPercent}%",
                    container = MaterialTheme.colorScheme.primary,
                    content = MaterialTheme.colorScheme.onPrimary,
                )
            }
        }
    }
}

/**
 * The developer's own offer artwork and words, shown as a banner that opens the
 * app. This is the one place the developer controls the picture, so it is given
 * the width of the screen rather than a card in a list.
 */
@Composable
private fun OfferBanner(product: Product, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = SproutlyRadii.hero,
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column {
            if (!product.promoImageUrl.isNullOrBlank()) {
                SproutlyImage(
                    url = product.promoImageUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    corner = 0.dp,
                )
            }
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (product.offerLine.isNotBlank()) {
                        SproutlyTag(product.offerLine)
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(
                        product.title,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
                if (product.promoHeadline.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        product.promoHeadline,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
                if (product.promoBody.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        product.promoBody,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    when {
                        product.hasActiveOffer ->
                            "${formatPrice(product.price)} · was ${formatPrice(product.compareAtPrice ?: product.price)}"
                        product.offerKind == "PROMO_CODE" && product.promoCode.isNotBlank() ->
                            "Use code ${product.promoCode} in the app"
                        product.offerKind == "IN_APP" -> "In-app perk — open the app to see it"
                        else -> formatPrice(product.price)
                    },
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
    }
}
