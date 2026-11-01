package app.tab.store.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.tab.store.core.designsystem.Avatar
import app.tab.store.core.designsystem.BrandWordmark
import app.tab.store.core.designsystem.EmptyState
import app.tab.store.core.designsystem.LoadingColumn
import app.tab.store.core.designsystem.ProductCard
import app.tab.store.core.designsystem.ProductRowCard
import app.tab.store.core.designsystem.SectionHeader
import app.tab.store.core.designsystem.ShimmerBox
import app.tab.store.core.designsystem.SproutlyChip
import app.tab.store.core.designsystem.SproutlyPalette
import app.tab.store.core.designsystem.SproutlyRadii
import app.tab.store.core.designsystem.SproutlyTag
import app.tab.store.domain.model.Category
import app.tab.store.domain.model.Product

private data class Promo(
    val eyebrow: String,
    val title: String,
    val body: String,
    val cta: String,
)

private val promos = listOf(
    Promo("New this week", "New drops,\nevery week", "Fresh arrivals from verified sellers, shipped across India.", "Browse new"),
    Promo("Sproutly picks", "Up to 40% off\nseason edits", "Markdowns across every category, while stock lasts.", "Shop the edit"),
    Promo("Sell with us", "Your shelf,\nour reach", "Open a store in minutes and reach shoppers everywhere.", "Start selling"),
)

@Composable
fun HomeScreen(
    onProduct: (String) -> Unit,
    onStore: (String) -> Unit,
    onSearch: () -> Unit,
    onCategory: (String) -> Unit,
    onSeeAll: () -> Unit,
    onCart: () -> Unit,
    onProfile: () -> Unit,
    onNotifications: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
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
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 28.dp),
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 12.dp, top = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    BrandWordmark()
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = onNotifications) {
                        BadgedBox(badge = {}) {
                            Icon(Icons.Filled.Notifications, contentDescription = "Notifications")
                        }
                    }
                    IconButton(onClick = onCart) {
                        Icon(Icons.Outlined.ShoppingBag, contentDescription = "Cart")
                    }
                    Avatar(
                        url = state.user?.photoUrl,
                        name = state.user?.displayName.orEmpty(),
                        size = 36.dp,
                        modifier = Modifier
                            .padding(start = 4.dp)
                            .clickable(onClick = onProfile),
                    )
                }
            }

            item {
                Column(Modifier.padding(horizontal = 20.dp, vertical = 14.dp)) {
                    Text(
                        text = "${state.greeting}, ${state.firstName}",
                        style = MaterialTheme.typography.headlineLarge,
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = "What are you shopping for today?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(16.dp))
                    Surface(
                        onClick = onSearch,
                        shape = SproutlyRadii.pill,
                        color = MaterialTheme.colorScheme.surface,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp, MaterialTheme.colorScheme.outlineVariant,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 18.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Filled.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                "Search products, stores, categories",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            // Promotional banners
            item {
                val pager = rememberPagerState(pageCount = { promos.size })
                HorizontalPager(
                    state = pager,
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    pageSpacing = 12.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(168.dp),
                ) { page ->
                    val promo = promos[page]
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(SproutlyRadii.hero)
                            .background(
                                if (page % 2 == 0) {
                                    Brush.linearGradient(
                                        listOf(SproutlyPalette.Lime, SproutlyPalette.LimeSoft),
                                    )
                                } else {
                                    Brush.linearGradient(
                                        listOf(SproutlyPalette.Olive, SproutlyPalette.OliveDeep),
                                    )
                                },
                            )
                            .padding(20.dp),
                    ) {
                        Column(Modifier.align(Alignment.CenterStart)) {
                            Text(
                                promo.eyebrow,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (page % 2 == 0) SproutlyPalette.Olive
                                else SproutlyPalette.Lime,
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                promo.title,
                                style = MaterialTheme.typography.headlineMedium,
                                color = if (page % 2 == 0) SproutlyPalette.OliveDeep
                                else SproutlyPalette.Lime,
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                promo.body,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (page % 2 == 0) SproutlyPalette.Olive
                                else SproutlyPalette.LimeSoft,
                                modifier = Modifier.fillMaxWidth(0.72f),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Spacer(Modifier.height(12.dp))
                            Surface(
                                onClick = onSeeAll,
                                shape = SproutlyRadii.pill,
                                color = if (page % 2 == 0) SproutlyPalette.Olive
                                else SproutlyPalette.Lime,
                            ) {
                                Text(
                                    promo.cta,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (page % 2 == 0) SproutlyPalette.Lime
                                    else SproutlyPalette.OliveDeep,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                )
                            }
                        }
                    }
                }
            }

            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    item {
                        SproutlyChip(
                            text = "All",
                            selected = true,
                            onClick = onSeeAll,
                        )
                    }
                    items(state.categories, key = { it.id }) { category ->
                        CategoryChip(category = category, onClick = { onCategory(category.id) })
                    }
                }
            }

            if (state.isLoading) {
                item { LoadingColumn(Modifier.padding(horizontal = 20.dp)) }
            } else if (!state.hasAnyContent) {
                item {
                    EmptyState(
                        title = "The shelves are being stocked",
                        message = "No products are live yet. Once sellers publish their first items they will appear right here.",
                        actionLabel = "Browse categories",
                        onAction = onSeeAll,
                    )
                }
            } else {
                if (state.featured.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title = "Featured today",
                            modifier = Modifier.padding(horizontal = 20.dp),
                            actionLabel = "See all",
                            onAction = onSeeAll,
                        )
                    }
                    item {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            items(state.featured, key = { it.id }) { product ->
                                ProductCard(
                                    product = product,
                                    onClick = { onProduct(product.id) },
                                    onAddToCart = { viewModel.addToCart(product) },
                                    modifier = Modifier.width(164.dp),
                                    compact = true,
                                )
                            }
                        }
                    }
                }

                if (state.trending.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title = "Trending now",
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                            actionLabel = "See all",
                            onAction = onSeeAll,
                        )
                    }
                    items(state.trending.chunked(2)) { row ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            row.forEach { product ->
                                ProductCard(
                                    product = product,
                                    onClick = { onProduct(product.id) },
                                    onAddToCart = { viewModel.addToCart(product) },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            if (row.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }

                if (state.newArrivals.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title = "Just landed",
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                        )
                    }
                    item {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            items(state.newArrivals, key = { it.id }) { product ->
                                Column(
                                    modifier = Modifier
                                        .width(200.dp)
                                        .clip(SproutlyRadii.card)
                                        .background(MaterialTheme.colorScheme.surface)
                                        .clickable { onProduct(product.id) }
                                        .padding(10.dp),
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        SproutlyTag("NEW")
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            product.storeName,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        product.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        app.tab.store.core.designsystem.formatPrice(product.price),
                                        style = MaterialTheme.typography.titleMedium,
                                    )
                                }
                            }
                        }
                    }
                }

                if (state.recommended.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title = "Picked for you",
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                        )
                    }
                    items(state.recommended, key = { "rec-${it.id}" }) { product ->
                        ProductRowCard(
                            product = product,
                            onClick = { onProduct(product.id) },
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                        )
                    }
                }

                if (state.recentlyViewed.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title = "Recently viewed",
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                        )
                    }
                    item {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            items(state.recentlyViewed, key = { "recent-${it.id}" }) { product ->
                                Column(
                                    modifier = Modifier
                                        .width(120.dp)
                                        .clickable { onProduct(product.id) },
                                ) {
                                    app.tab.store.core.designsystem.SproutlyImage(
                                        url = product.primaryImage,
                                        contentDescription = product.title,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(120.dp),
                                        corner = 16.dp,
                                    )
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        product.title,
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        app.tab.store.core.designsystem.formatPrice(product.price),
                                        style = MaterialTheme.typography.labelLarge,
                                    )
                                }
                            }
                        }
                    }
                }

                item { Spacer(Modifier.height(20.dp)) }
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
private fun CategoryChip(category: Category, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = SproutlyRadii.pill,
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            1.dp, MaterialTheme.colorScheme.outlineVariant,
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(SproutlyPalette.OliveMid),
            )
            Spacer(Modifier.width(7.dp))
            Text(category.name, style = MaterialTheme.typography.labelLarge)
        }
    }
}
