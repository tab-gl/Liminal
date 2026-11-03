package app.tab.store.feature.product

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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.tab.store.core.designsystem.HugeIcons
import app.tab.store.core.designsystem.HugeIcon
import app.tab.store.core.designsystem.ErrorState
import app.tab.store.core.designsystem.PriceText
import app.tab.store.core.designsystem.RatingBadge
import app.tab.store.core.designsystem.ShimmerBox
import app.tab.store.core.designsystem.SproutlyButton
import app.tab.store.core.designsystem.SproutlyImage
import app.tab.store.core.designsystem.SproutlyRadii
import app.tab.store.core.designsystem.SproutlyTag
import app.tab.store.core.designsystem.formatPrice

@Composable
fun ProductDetailScreen(
    productId: String,
    onBack: () -> Unit,
    onStore: (String) -> Unit,
    onDownloads: () -> Unit,
    viewModel: ProductDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbar.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }
    LaunchedEffect(state.navigateToLibrary) {
        if (state.navigateToLibrary) {
            viewModel.consumeNavigation()
            onDownloads()
        }
    }

    val product = state.product

    Box(Modifier.fillMaxSize()) {
        when {
            state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                ShimmerBox(
                    Modifier
                        .fillMaxWidth()
                        .height(420.dp),
                    corner = 0.dp,
                )
            }
            product == null -> ErrorState(
                message = state.error ?: "Product unavailable.",
                onRetry = onBack,
            )
            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
            ) {
                Box {
                    val pagerState = rememberPagerState(
                        pageCount = { product.images.size.coerceAtLeast(1) },
                    )
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(400.dp),
                    ) { page ->
                        SproutlyImage(
                            url = product.images.getOrNull(page)?.url,
                            contentDescription = product.title,
                            modifier = Modifier.fillMaxSize(),
                            corner = 0.dp,
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Surface(
                            onClick = onBack,
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                            modifier = Modifier.size(40.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                HugeIcon(HugeIcons.ArrowLeft, contentDescription = "Back")
                            }
                        }
                        Row {
                            Surface(
                                onClick = viewModel::toggleWishlist,
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                                modifier = Modifier.size(40.dp),
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    HugeIcon(
                                        if (state.isWishlisted) HugeIcons.Favourite
                                        else HugeIcons.Favourite,
                                        contentDescription = "Wishlist",
                                        tint = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            }
                            Spacer(Modifier.width(8.dp))
                            Surface(
                                onClick = {},
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                                modifier = Modifier.size(40.dp),
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    HugeIcon(HugeIcons.Share, contentDescription = "Share")
                                }
                            }
                        }
                    }
                    if (product.images.size > 1) {
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 14.dp),
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                        ) {
                            repeat(product.images.size) { index ->
                                Box(
                                    Modifier
                                        .size(if (pagerState.currentPage == index) 8.dp else 6.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (pagerState.currentPage == index) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                                            },
                                        ),
                                )
                            }
                        }
                    }
                }

                Column(Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SproutlyTag(product.categoryLabel.ifBlank { "Sproutly" })
                        if (product.isOutOfStock) {
                            Spacer(Modifier.width(6.dp))
                            SproutlyTag(
                                text = "UNAVAILABLE",
                                container = MaterialTheme.colorScheme.errorContainer,
                                content = MaterialTheme.colorScheme.onErrorContainer,
                            )
                        } else if (product.stock <= 5) {
                            Spacer(Modifier.width(6.dp))
                            SproutlyTag(
                                text = "LIMITED AVAILABILITY",
                                container = MaterialTheme.colorScheme.tertiaryContainer,
                                content = MaterialTheme.colorScheme.onTertiaryContainer,
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(product.title, style = MaterialTheme.typography.headlineLarge)
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RatingBadge(product.rating, product.ratingCount)
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "${product.unitsSold} downloads",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.height(14.dp))
                    PriceText(
                        paise = product.price,
                        compareAt = product.compareAtPrice,
                        large = true,
                    )
                    Spacer(Modifier.height(18.dp))

                    Surface(
                        onClick = { onStore(product.storeId) },
                        shape = SproutlyRadii.card,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center,
                            ) {
                                HugeIcon(HugeIcons.Store,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    product.storeName.ifBlank { "Sproutly developer" },
                                    style = MaterialTheme.typography.titleSmall,
                                )
                                Text(
                                    "View developer",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(20.dp))
                    Text("About this app", style = MaterialTheme.typography.headlineSmall)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        product.description.ifBlank { "No description provided by the developer yet." },
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    if (product.tags.isNotEmpty()) {
                        Spacer(Modifier.height(14.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            product.tags.take(4).forEach { tag ->
                                SproutlyTag(
                                    text = tag.uppercase(),
                                    container = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    content = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(24.dp))
                    Spacer(Modifier.height(20.dp))
                    SproutlyButton(
                        text = if (product.isOutOfStock) "Unavailable" else "Get",
                        onClick = viewModel::install,
                        enabled = state.canInstall,
                        loading = state.isInstalling,
                    )
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
