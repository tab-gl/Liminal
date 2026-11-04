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
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.tween
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.launch
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.foundation.lazy.rememberLazyListState

import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.runtime.setValue
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
import app.tab.store.core.designsystem.SproutlyImage
import app.tab.store.core.designsystem.formatPrice
import app.tab.store.core.designsystem.SproutlyPalette
import app.tab.store.core.designsystem.SproutlyRadii
import app.tab.store.core.designsystem.TablerIcon
import app.tab.store.core.designsystem.categoryIcon
import app.tab.store.core.designsystem.TablerIcons
import app.tab.store.core.designsystem.SproutlyTag
import app.tab.store.domain.model.Category
import app.tab.store.domain.model.Product

/**
 * A hero card. These used to be three hardcoded slogans; they are now built from
 * whatever is actually in the catalogue, and fall back to the developer invite
 * only when there is nothing to show.
 */
private data class HeroSlide(
    val eyebrow: String,
    val title: String,
    val body: String,
    val cta: String,
    val onClick: () -> Unit,
)

private fun heroSlides(
    state: HomeUiState,
    onProduct: (String) -> Unit,
    onSeeAll: () -> Unit,
    onOffers: () -> Unit,
    onBecomeDeveloper: () -> Unit,
): List<HeroSlide> {
    val slides = mutableListOf<HeroSlide>()

    state.newArrivals.firstOrNull()?.let { app ->
        slides += HeroSlide(
            eyebrow = "New this week",
            title = app.title,
            body = "Fresh from ${app.storeName.ifBlank { "an independent developer" }}.",
            cta = "Open",
            onClick = { onProduct(app.id) },
        )
    }

    (state.featured + state.trending).firstOrNull { it.hasActiveOffer }?.let { app ->
        slides += HeroSlide(
            eyebrow = "${app.discountPercent}% off",
            title = app.title,
            body = "On offer now — ${formatPrice(app.price)}.",
            cta = "See all offers",
            onClick = onOffers,
        )
    }

    state.trending.firstOrNull()?.let { app ->
        slides += HeroSlide(
            eyebrow = "Top of the charts",
            title = app.title,
            body = "${app.unitsSold} installs and counting.",
            cta = "Open",
            onClick = { onProduct(app.id) },
        )
    }

    // Always last, and the only one that is not about a specific app.
    slides += HeroSlide(
        eyebrow = "Publish with us",
        title = "Build it.\nPublish it.\nGrow it.",
        body = "Open a developer profile and ship your first app today.",
        cta = "Start building",
        onClick = onBecomeDeveloper,
    )
    return slides
}

@Composable
fun HomeScreen(
    onProduct: (String) -> Unit,
    onStore: (String) -> Unit,
    onSearch: () -> Unit,
    onCategory: (String) -> Unit,
    onSeeAll: () -> Unit,
    onDownloads: () -> Unit,
    onProfile: () -> Unit,
    onNotifications: () -> Unit,
    onBecomeDeveloper: () -> Unit,
    onOpenSwitcher: () -> Unit,
    onRankings: () -> Unit,
    onOffers: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val unread by viewModel.unreadCount.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbar.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }

    val listState = rememberLazyListState()
    // The search FAB starts as a labelled pill and shrinks to a bare circle once
    // the list is scrolled, so it stops covering content but stays reachable.
    val showControls by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 140
        }
    }
    val scope = rememberCoroutineScope()


    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 148.dp),
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
                        BadgedBox(
                            badge = {
                                if (unread > 0) {
                                    Badge(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary,
                                    ) { Text(if (unread > 99) "99+" else unread.toString()) }
                                }
                            },
                        ) {
                            TablerIcon(TablerIcons.Notification, contentDescription = "Notifications")
                        }
                    }
                    // Downloads (the library of installed apps) lives in the top
                    // bar, not the bottom navigation: installing is a rare action
                    // compared with browsing, but checking what you own is common.
                    IconButton(onClick = onDownloads) {
                        TablerIcon(
                            icon = TablerIcons.Download,
                            contentDescription = "Downloads",
                            modifier = Modifier.size(22.dp),
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    // One tap on the avatar opens the account switcher; the
                    // profile itself is a bottom-bar destination.
                    Avatar(
                        url = state.user?.photoUrl,
                        name = state.user?.displayName.orEmpty(),
                        size = 36.dp,
                        modifier = Modifier
                            .padding(start = 4.dp)
                            .clickable(onClick = onOpenSwitcher),
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
                        text = "Find your next favourite app.",
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
                            TablerIcon(TablerIcons.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                "Search apps, developers, categories",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            // A developer's own offer banner, right under the hero — the thing
            // Play Store puts in front of you and that opens the app.
            val promoApp = (state.featured + state.trending)
                .firstOrNull { it.hasActiveOffer && (it.promoHeadline.isNotBlank() || it.promoImageUrl != null) }
            if (promoApp != null) {
                item {
                    PromoBanner(product = promoApp, onClick = { onProduct(promoApp.id) })
                }
            }

            // Promotional banners
            item {
                val slides = heroSlides(state, onProduct, onSeeAll, onOffers, onBecomeDeveloper)
                val pager = rememberPagerState(pageCount = { slides.size })
                HorizontalPager(
                    state = pager,
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    pageSpacing = 12.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(168.dp),
                ) { page ->
                    val promo = slides[page]
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
                                onClick = promo.onClick,
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
                    // Always reachable, even with an empty catalogue — otherwise
                    // the charts are only linked from a rail that may not exist.
                    item {
                        Surface(
                            onClick = onRankings,
                            shape = SproutlyRadii.pill,
                            color = MaterialTheme.colorScheme.primaryContainer,
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                TablerIcon(
                                    TablerIcons.Analytics,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    "Top charts",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                            }
                        }
                    }
                    // Games have their own tab, so they are not a chip here.
                    items(
                        state.categories.filterNot {
                            it.slug.equals("games", ignoreCase = true) ||
                                it.name.equals("Games", ignoreCase = true)
                        },
                        key = { it.id },
                    ) { category ->
                        CategoryChip(category = category, onClick = { onCategory(category.id) })
                    }
                }
            }

            if (state.isLoading) {
                item { LoadingColumn(Modifier.padding(horizontal = 20.dp)) }
            } else if (!state.hasAnyContent) {
                item {
                    EmptyState(
                        modifier = Modifier.padding(bottom = 96.dp),
                        title = "No apps yet",
                        message = "Once developers publish their first apps they will appear right here.",
                        actionLabel = "Browse the store",
                        onAction = onSeeAll,
                        icon = TablerIcons.Store,
                    )
                }
            } else {
                if (state.featured.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title = "New",
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
                            title = "Top charts",
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                            actionLabel = "See all",
                            onAction = onRankings,
                        )
                    }
                    itemsIndexed(state.trending.chunked(2)) { rowIndex, row ->
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
                                    modifier = Modifier
                                        .weight(1f)
                                        .animateItem(),
                                    rank = rowIndex * 2 + row.indexOf(product) + 1,
                                )
                            }
                            if (row.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }

                if (state.newArrivals.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title = "Fresh releases",
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
                            title = "For you",
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
                            title = "Recently opened",
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

        // Controls appear only once the list has actually moved: a circular
        // search FAB sitting just above the profile pill, with scroll-to-top
        // stacked above it. At rest they would cover the first rail for nothing.
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(tween(140)) + scaleIn(tween(160), initialScale = 0.85f),
            exit = fadeOut(tween(120)) + scaleOut(tween(120), targetScale = 0.85f),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 84.dp),
        ) {
            Column(horizontalAlignment = Alignment.End) {
                SmallFab(
                    icon = TablerIcons.ArrowRight,
                    contentDescription = "Back to top",
                    container = MaterialTheme.colorScheme.surface,
                    content = MaterialTheme.colorScheme.onSurface,
                    rotate = 270f,
                    onClick = {
                        scope.launch { listState.animateScrollToItem(0) }
                    },
                )
                Spacer(Modifier.height(10.dp))
                SmallFab(
                    icon = TablerIcons.Search,
                    contentDescription = "Search apps",
                    container = MaterialTheme.colorScheme.primary,
                    content = MaterialTheme.colorScheme.onPrimary,
                    onClick = onSearch,
                )
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
            TablerIcon(
                icon = categoryIcon(category.iconKey),
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.width(7.dp))
            Text(category.name, style = MaterialTheme.typography.labelLarge)
        }
    }
}

/** A 52dp circular action used for the floating controls. */
@Composable
private fun SmallFab(
    icon: Int,
    contentDescription: String,
    container: androidx.compose.ui.graphics.Color,
    content: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
    rotate: Float = 0f,
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = container,
        contentColor = content,
        shadowElevation = 8.dp,
        modifier = Modifier.size(52.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            TablerIcon(
                icon = icon,
                contentDescription = contentDescription,
                modifier = Modifier
                    .size(22.dp)
                    .graphicsLayer { rotationZ = rotate },
                tint = content,
            )
        }
    }
}

/** The developer's offer artwork, tappable through to the listing. */
@Composable
private fun PromoBanner(product: Product, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = SproutlyRadii.hero,
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (!product.promoImageUrl.isNullOrBlank()) {
                SproutlyImage(
                    url = product.promoImageUrl,
                    contentDescription = null,
                    modifier = Modifier.size(84.dp),
                    corner = 0.dp,
                )
            }
            Column(Modifier.padding(14.dp)) {
                Text(
                    product.promoHeadline.ifBlank { "${product.discountPercent}% off ${product.title}" },
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                if (product.promoBody.isNotBlank()) {
                    Spacer(Modifier.height(3.dp))
                    Text(
                        product.promoBody,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    "Open",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}
