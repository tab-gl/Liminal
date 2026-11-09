package app.tab.store.feature.home

import app.tab.store.core.ui.Tokens
import androidx.compose.foundation.background
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.graphics.Color
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
import androidx.compose.foundation.layout.heightIn
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
import kotlinx.coroutines.delay
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import app.tab.store.core.ui.Badge
import app.tab.store.core.ui.BadgedBox
import app.tab.store.core.ui.IconButton
import app.tab.store.core.ui.SnackbarHost
import app.tab.store.core.ui.SnackbarHostState
import app.tab.store.core.ui.Surface
import app.tab.store.core.ui.Text
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
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
import app.tab.store.core.ads.BannerAd
import app.tab.store.core.designsystem.EmptyState
import app.tab.store.core.utils.InstallCount
import app.tab.store.core.designsystem.LoadingColumn
import app.tab.store.core.designsystem.ProductCard
import app.tab.store.core.designsystem.ProductRowCard
import app.tab.store.core.designsystem.SectionHeader
import app.tab.store.core.designsystem.ShimmerBox
import app.tab.store.core.designsystem.SproutlyChip
import app.tab.store.core.designsystem.SproutlyImage
import app.tab.store.core.designsystem.formatPrice
import app.tab.store.core.designsystem.SproutlyRadii
import app.tab.store.core.designsystem.SproutlyIcon
import app.tab.store.core.designsystem.categoryIcon
import app.tab.store.core.designsystem.PhosphorIcons
import app.tab.store.core.designsystem.SproutlyTag
import app.tab.store.domain.model.Category
import app.tab.store.domain.model.Product

/**
 * A hero card. These used to be three hardcoded slogans; they are now built from
 * whatever is actually in the catalogue, and fall back to the developer invite
 * only when there is nothing to show.
 */
/** A screenshot of the app when the developer set one. Never the icon. */
private fun Product.screenshotUrl(): String? =
    screenshots.firstOrNull { it.url.isNotBlank() }?.url ?: promoImageUrl?.takeIf { it.isNotBlank() }

private data class HeroSlide(
    val eyebrow: String,
    val title: String,
    val body: String,
    val cta: String,
    /** The app's own art, shown on the card when there is one. */
    val imageUrl: String? = null,
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
            imageUrl = app.screenshotUrl(),
            onClick = { onProduct(app.id) },
        )
    }

    (state.featured + state.trending).firstOrNull { it.hasActiveOffer }?.let { app ->
        slides += HeroSlide(
            eyebrow = "${app.discountPercent}% off",
            title = app.title,
            body = "On offer now — ${formatPrice(app.price)}.",
            cta = "See all offers",
            imageUrl = app.screenshotUrl(),
            onClick = onOffers,
        )
    }

    state.trending.firstOrNull()?.let { app ->
        slides += HeroSlide(
            eyebrow = "Top of the charts",
            title = app.title,
            body = InstallCount.phrase(app.unitsSold),
            cta = "Open",
            imageUrl = app.screenshotUrl(),
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
                    BrandWordmark(animateLeaf = true, leafKey = "home")
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = onNotifications) {
                        BadgedBox(
                            badge = {
                                if (unread > 0) {
                                    Badge(
                                        containerColor = Tokens.colorScheme.primary,
                                        contentColor = Tokens.colorScheme.onPrimary,
                                    ) { Text(if (unread > 99) "99+" else unread.toString()) }
                                }
                            },
                        ) {
                            SproutlyIcon(PhosphorIcons.Notification, contentDescription = "Notifications")
                        }
                    }
                    // Downloads (the library of installed apps) lives in the top
                    // bar, not the bottom navigation: installing is a rare action
                    // compared with browsing, but checking what you own is common.
                    IconButton(onClick = onDownloads) {
                        SproutlyIcon(
                            icon = PhosphorIcons.Download,
                            contentDescription = "Downloads",
                            modifier = Modifier.size(22.dp),
                            tint = Tokens.colorScheme.onSurface,
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
                        style = Tokens.typography.headlineLarge,
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = state.subtitle,
                        style = Tokens.typography.bodyMedium,
                        color = Tokens.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(16.dp))
                    Surface(
                        onClick = onSearch,
                        shape = SproutlyRadii.pill,
                        color = Tokens.colorScheme.surface,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp, Tokens.colorScheme.outlineVariant,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 18.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            SproutlyIcon(PhosphorIcons.Search,
                                contentDescription = null,
                                tint = Tokens.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                "Search apps, developers, categories",
                                style = Tokens.typography.bodyMedium,
                                color = Tokens.colorScheme.onSurfaceVariant,
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

                // The cards advance on their own, slowly enough to read. A drag
                // pauses the timer, and it does not fight the user's own swipe.
                val autoAdvance = slides.size > 1 && !pager.isScrollInProgress
                LaunchedEffect(autoAdvance, pager.currentPage) {
                    if (!autoAdvance) return@LaunchedEffect
                    delay(6_000)
                    if (!pager.isScrollInProgress) {
                        val next = (pager.currentPage + 1) % slides.size
                        pager.animateScrollToPage(next)
                    }
                }

                HorizontalPager(
                    state = pager,
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    pageSpacing = 12.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 200.dp),
                ) { page ->
                    val promo = slides[page]
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(SproutlyRadii.hero)
                            .clickable(onClick = promo.onClick)
                            .background(
                                // Monochrome: the card is ink on paper, and the only
                                // colour in it is the app's own artwork.
                                if (page % 2 == 0) Tokens.colorScheme.surfaceContainerHigh
                                else Tokens.colorScheme.inverseSurface,
                            )
                            .padding(20.dp),
                    ) {
                        if (!promo.imageUrl.isNullOrBlank()) {
                            SproutlyImage(
                                url = promo.imageUrl,
                                contentDescription = promo.title,
                                // Phone-shaped, so a portrait screenshot is not cropped to a square.
                                modifier = Modifier
                                    .align(Alignment.CenterEnd)
                                    .fillMaxHeight()
                                    .aspectRatio(9f / 16f),
                                corner = 18.dp,
                            )
                        }
                        Column(
                            Modifier
                                .align(Alignment.CenterStart)
                                .fillMaxWidth(if (promo.imageUrl.isNullOrBlank()) 1f else 0.62f),
                        ) {
                            Text(
                                promo.eyebrow,
                                style = Tokens.typography.labelSmall,
                                color = if (page % 2 == 0) Tokens.colorScheme.onSurfaceVariant
                                else Tokens.colorScheme.inverseOnSurface.copy(alpha = 0.75f),
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                promo.title,
                                style = Tokens.typography.headlineMedium,
                                color = if (page % 2 == 0) Tokens.colorScheme.onSurface
                                else Tokens.colorScheme.inverseOnSurface,
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                promo.body,
                                style = Tokens.typography.bodySmall,
                                color = if (page % 2 == 0) Tokens.colorScheme.onSurfaceVariant
                                else Tokens.colorScheme.inverseOnSurface.copy(alpha = 0.8f),
                                modifier = Modifier.fillMaxWidth(0.72f),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Spacer(Modifier.height(12.dp))
                            Surface(
                                onClick = promo.onClick,
                                shape = SproutlyRadii.pill,
                                // The accent is the one colour on the card; its label colour
                                // comes from the theme, which checks the contrast.
                                color = if (page % 2 == 0) Tokens.colorScheme.primary
                                else Tokens.colorScheme.inversePrimary,
                            ) {
                                Text(
                                    promo.cta,
                                    style = Tokens.typography.labelMedium,
                                    color = if (page % 2 == 0) Tokens.colorScheme.onPrimary
                                    else Tokens.colorScheme.inverseSurface,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                )
                            }
                        }
                    }
                }
            }

            item {
                // An ad, below the content that matters, never over a control.
                BannerAd(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
                )
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
                            color = Tokens.colorScheme.primaryContainer,
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                SproutlyIcon(
                                    PhosphorIcons.Analytics,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = Tokens.colorScheme.onPrimaryContainer,
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    "Top charts",
                                    style = Tokens.typography.labelLarge,
                                    color = Tokens.colorScheme.onPrimaryContainer,
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
                        title = "Nothing published yet",
                        message = "New apps show up here.",
                        actionLabel = "Browse apps",
                        onAction = onSeeAll,
                        icon = PhosphorIcons.Store,
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
                                        .background(Tokens.colorScheme.surface)
                                        .clickable { onProduct(product.id) }
                                        .padding(10.dp),
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        SproutlyTag("NEW")
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            product.storeName,
                                            style = Tokens.typography.labelSmall,
                                            color = Tokens.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        product.title,
                                        style = Tokens.typography.titleSmall,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        app.tab.store.core.designsystem.priceLabel(product.price),
                                        style = Tokens.typography.titleMedium,
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
                                        style = Tokens.typography.bodySmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        app.tab.store.core.designsystem.priceLabel(product.price),
                                        style = Tokens.typography.labelLarge,
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
                    icon = PhosphorIcons.ArrowRight,
                    contentDescription = "Back to top",
                    container = Tokens.colorScheme.surface,
                    content = Tokens.colorScheme.onSurface,
                    rotate = 270f,
                    onClick = {
                        scope.launch { listState.animateScrollToItem(0) }
                    },
                )
                Spacer(Modifier.height(10.dp))
                SmallFab(
                    icon = PhosphorIcons.Search,
                    contentDescription = "Search apps",
                    container = Tokens.colorScheme.primary,
                    content = Tokens.colorScheme.onPrimary,
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
        color = Tokens.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            1.dp, Tokens.colorScheme.outlineVariant,
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SproutlyIcon(
                icon = categoryIcon(category.iconKey),
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = Tokens.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.width(7.dp))
            Text(category.name, style = Tokens.typography.labelLarge)
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
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            SproutlyIcon(
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
        color = Tokens.colorScheme.primaryContainer,
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
                    style = Tokens.typography.titleSmall,
                    color = Tokens.colorScheme.onPrimaryContainer,
                )
                if (product.promoBody.isNotBlank()) {
                    Spacer(Modifier.height(3.dp))
                    Text(
                        product.promoBody,
                        style = Tokens.typography.bodySmall,
                        color = Tokens.colorScheme.onPrimaryContainer,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    "Open",
                    style = Tokens.typography.labelLarge,
                    color = Tokens.colorScheme.primary,
                )
            }
        }
    }
}
