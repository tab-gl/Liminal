package app.tab.store.feature.product

import app.tab.store.core.ui.Tokens
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import app.tab.store.core.ui.IconButton
import app.tab.store.core.ui.LinearProgressIndicator
import app.tab.store.core.ui.TextButton
import app.tab.store.core.ui.SnackbarHost
import app.tab.store.core.ui.SnackbarHostState
import app.tab.store.core.ui.AlertDialog
import app.tab.store.core.ui.Surface
import app.tab.store.core.ui.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.platform.LocalContext
import app.tab.store.core.navigation.Routes
import app.tab.store.domain.model.Product
import androidx.compose.foundation.clickable
import kotlin.math.roundToInt
import app.tab.store.core.designsystem.RiskMeter
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.tab.store.core.designsystem.PhosphorIcons
import app.tab.store.core.designsystem.SproutlyIcon
import app.tab.store.core.designsystem.ErrorState
import app.tab.store.core.designsystem.PriceText
import app.tab.store.core.designsystem.RatingBadge
import app.tab.store.core.designsystem.ShimmerBox
import app.tab.store.core.designsystem.SproutlyButton
import app.tab.store.core.designsystem.OfficialBadge
import app.tab.store.core.designsystem.SproutlyChip
import app.tab.store.core.designsystem.SproutlyImage
import app.tab.store.core.designsystem.SproutlyRadii
import app.tab.store.core.designsystem.SproutlyTag
import app.tab.store.core.designsystem.ProductCard
import app.tab.store.core.designsystem.formatPrice

@Composable
fun ProductDetailScreen(
    productId: String,
    onBack: () -> Unit,
    isAdmin: Boolean = false,
    onStore: (String) -> Unit,
    onDownloads: () -> Unit,
    onProduct: (String) -> Unit = {},
    onSearchTag: (String) -> Unit = {},
    viewModel: ProductDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var showReportDialog by remember { mutableStateOf(false) }
    var viewerStart by remember { mutableStateOf<Int?>(null) }
    val context = LocalContext.current

    var showBuilds by remember { mutableStateOf(false) }

    fun shareProduct() {
        val product = state.product ?: return
        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(
                android.content.Intent.EXTRA_TEXT,
                "${product.title} on Liminal — ${Routes.shareLink(product.id)}",
            )
        }
        context.startActivity(
            android.content.Intent.createChooser(intent, "Share this app"),
        )
    }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbar.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }
    androidx.lifecycle.compose.LifecycleEventEffect(androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
        viewModel.onResumed()
    }

    LaunchedEffect(state.navigateToLibrary) {
        if (state.navigateToLibrary) {
            viewModel.consumeNavigation()
            onDownloads()
        }
    }

    val product = state.product
    var showBuilds by remember { mutableStateOf(false) }

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
                    .verticalScroll(rememberScrollState())
                    // Room for the pinned install bar, which is 54dp plus its own
                    // padding and the gesture area.
                    .padding(bottom = 140.dp),
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
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 12.dp),
                            corner = 32.dp,
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
                            color = Tokens.colorScheme.surface.copy(alpha = 0.92f),
                            modifier = Modifier.size(40.dp),
                        ) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                SproutlyIcon(PhosphorIcons.ChevronLeft, contentDescription = "Back")
                            }
                        }
                        Row {
                            Surface(
                                onClick = viewModel::toggleWishlist,
                                shape = CircleShape,
                                color = Tokens.colorScheme.surface.copy(alpha = 0.92f),
                                modifier = Modifier.size(40.dp),
                            ) {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    SproutlyIcon(
                                        if (state.isWishlisted) PhosphorIcons.Favourite
                                        else PhosphorIcons.Favourite,
                                        contentDescription =
                                            if (state.isWishlisted) "Remove from wishlist" else "Add to wishlist",
                                        tint = Tokens.colorScheme.primary,
                                    )
                                }
                            }
                            Spacer(Modifier.width(8.dp))
                            Surface(
                                onClick = {},
                                shape = CircleShape,
                                color = Tokens.colorScheme.surface.copy(alpha = 0.92f),
                                modifier = Modifier.size(40.dp),
                            ) {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    SproutlyIcon(PhosphorIcons.Share, contentDescription = "Share")
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
                                                Tokens.colorScheme.primary
                                            } else {
                                                Tokens.colorScheme.surface.copy(alpha = 0.7f)
                                            },
                                        ),
                                )
                            }
                        }
                    }
                }

                Column(Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SproutlyTag(product.categoryLabel.ifBlank { "Liminal" })
                        if (product.status == app.tab.store.domain.model.ProductStatus.BLOCKED) {
                            Spacer(Modifier.width(6.dp))
                            SproutlyTag(
                                text = "REMOVED",
                                container = Tokens.colorScheme.errorContainer,
                                content = Tokens.colorScheme.onErrorContainer,
                            )
                        } else if (false) {
                            Spacer(Modifier.width(6.dp))
                            SproutlyTag(
                                text = "LIMITED AVAILABILITY",
                                container = Tokens.colorScheme.tertiaryContainer,
                                content = Tokens.colorScheme.onTertiaryContainer,
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(product.title, style = Tokens.typography.headlineLarge)
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RatingBadge(product.rating, product.ratingCount)
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "${product.unitsSold} downloads",
                            style = Tokens.typography.bodySmall,
                            color = Tokens.colorScheme.onSurfaceVariant,
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
                        color = Tokens.colorScheme.surfaceContainerHigh,
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
                                    .background(Tokens.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center,
                            ) {
                                SproutlyIcon(PhosphorIcons.Store,
                                    contentDescription = null,
                                    tint = Tokens.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    product.storeName.ifBlank { "Liminal developer" },
                                    style = Tokens.typography.titleSmall,
                                )
                                Text(
                                    "About the developer",
                                    style = Tokens.typography.bodySmall,
                                    color = Tokens.colorScheme.onSurfaceVariant,
                                )
                            }
                            SproutlyIcon(
                                PhosphorIcons.ArrowRight,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = Tokens.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    if (isAdmin) {
                        AdminActions(
                            status = product.status,
                            reported = product.reported,
                            onStatus = viewModel::adminSetStatus,
                            onRemove = { viewModel.adminRemove(onDone = onBack) },
                            onResolve = viewModel::adminResolve,
                        )
                        Spacer(Modifier.height(12.dp))
                    }
                    if (product.official) {
                        OfficialBadge()
                        Spacer(Modifier.height(8.dp))
                    }
                    RatingRow(
                        summary = state.rating,
                        onRate = { stars -> viewModel.rate(stars) },
                    )

                    if (state.installedPackage != null && !state.isInstalling) {
                        Spacer(Modifier.height(10.dp))
                        Surface(
                            shape = SproutlyRadii.card,
                            color = Tokens.colorScheme.primaryContainer,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(Modifier.padding(14.dp)) {
                                Text(
                                    "Android is installing it",
                                    style = Tokens.typography.titleSmall,
                                    color = Tokens.colorScheme.onPrimaryContainer,
                                )
                                Text(
                                    "Tap Install on the Android prompt, then open it here.",
                                    style = Tokens.typography.bodySmall,
                                    color = Tokens.colorScheme.onPrimaryContainer,
                                )
                                Spacer(Modifier.height(6.dp))
                                TextButton(onClick = viewModel::openInstalledApp) {
                                    Text("Open the app")
                                }
                            }
                        }
                    }
                    if (state.needsInstallPermission) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Android needs permission to install apps from Liminal.",
                            style = Tokens.typography.bodySmall,
                            color = Tokens.colorScheme.onSurfaceVariant,
                        )
                        TextButton(onClick = viewModel::openInstallPermission) {
                            Text("Open settings")
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    Surface(
                        onClick = { shareProduct() },
                        shape = SproutlyRadii.card,
                        color = Tokens.colorScheme.surfaceContainerLow,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            SproutlyIcon(
                                PhosphorIcons.Share,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = Tokens.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                "Share this app",
                                style = Tokens.typography.bodyMedium,
                                color = Tokens.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    Spacer(Modifier.height(10.dp))
                    Surface(
                        onClick = { showReportDialog = true },
                        shape = SproutlyRadii.card,
                        color = Tokens.colorScheme.surfaceContainerLow,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            SproutlyIcon(
                                PhosphorIcons.Tag,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = Tokens.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                "Report this app",
                                style = Tokens.typography.bodyMedium,
                                color = Tokens.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    if (product.versionName.isNotBlank()) {
                        Spacer(Modifier.height(14.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            SproutlyIcon(
                                PhosphorIcons.Tag,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp),
                                tint = Tokens.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "Version ${product.versionName}",
                                style = Tokens.typography.labelMedium,
                                color = Tokens.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    if (product.builds.size > 1) {
                        Spacer(Modifier.height(14.dp))
                        val recommended = viewModel.recommendedFlavourIndex(product)
                        val shown = product.builds.getOrNull(state.chosenFlavour)
                            ?: product.builds.getOrNull(recommended)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(SproutlyRadii.pill)
                                .clickable { showBuilds = !showBuilds }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                "Build: ${shown?.name?.ifBlank { "Standard" } ?: "Standard"}",
                                style = Tokens.typography.labelMedium,
                            )
                            if (state.chosenFlavour == 0 && recommended > 0) {
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    "for this device",
                                    style = Tokens.typography.labelSmall,
                                    color = Tokens.colorScheme.onSurfaceVariant,
                                )
                            }
                            Spacer(Modifier.weight(1f))
                            SproutlyIcon(
                                icon = if (showBuilds) PhosphorIcons.ChevronDown else PhosphorIcons.ChevronRight,
                                contentDescription = if (showBuilds) "Hide builds" else "Choose a build",
                                modifier = Modifier
                                    .size(18.dp)
                                    .graphicsLayer { rotationZ = if (showBuilds) 180f else 0f },
                                tint = Tokens.colorScheme.onSurfaceVariant,
                            )
                        }
                        AnimatedVisibility(visible = showBuilds) {
                            Row(
                                modifier = Modifier.padding(top = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                product.builds.forEachIndexed { index, build ->
                                    SproutlyChip(
                                        text = build.name.ifBlank { "Build ${index + 1}" },
                                        selected = state.chosenFlavour == index,
                                        onClick = { viewModel.chooseFlavour(index) },
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(14.dp))
                    if (product.scanRisk.isNotBlank() || product.scanScore > 0) {
                        RiskMeter(
                            score = product.scanScore,
                            reasons = product.scanWarnings,
                        )
                    } else {
                        Surface(
                            shape = SproutlyRadii.card,
                            color = Tokens.colorScheme.surfaceContainerLow,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(Modifier.padding(14.dp)) {
                                Text("Sprout Safe", style = Tokens.typography.titleSmall)
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "This build was not scanned, so Liminal cannot say what " +
                                        "it can reach. Install it at your own discretion.",
                                    style = Tokens.typography.bodySmall,
                                    color = Tokens.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }

                    if (product.releaseNotes.isNotBlank()) {
                        Spacer(Modifier.height(16.dp))
                        Text("What's new", style = Tokens.typography.titleSmall)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            product.releaseNotes,
                            style = Tokens.typography.bodyMedium,
                            color = Tokens.colorScheme.onSurfaceVariant,
                        )
                    }

                    Spacer(Modifier.height(20.dp))
                    Text("About this app", style = Tokens.typography.headlineSmall)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        product.description.ifBlank { "No description provided by the developer yet." },
                        style = Tokens.typography.bodyLarge,
                        color = Tokens.colorScheme.onSurfaceVariant,
                    )

                    if (product.tags.isNotEmpty()) {
                        Spacer(Modifier.height(14.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            product.tags.take(6).forEach { tag ->
                                // A tag is a way in, not decoration: tapping it searches.
                                SproutlyChip(
                                    text = tag,
                                    selected = false,
                                    onClick = { onSearchTag(tag) },
                                )
                            }
                        }
                    }

            if (product.screenshots.isNotEmpty()) {
                Spacer(Modifier.height(22.dp))
                Text("Screenshots", style = Tokens.typography.titleSmall)
                Spacer(Modifier.height(10.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    itemsIndexed(product.screenshots) { index, shot ->
                        SproutlyImage(
                            url = shot.url,
                            contentDescription = "Screenshot ${index + 1} of ${product.screenshots.size}",
                            modifier = Modifier
                                .size(width = 150.dp, height = 266.dp)
                                .clickable { viewerStart = index },
                            corner = 18.dp,
                        )
                    }
                }
            }

                    Spacer(Modifier.height(28.dp))
                }
            }
        }

        viewerStart?.let { start ->
            state.product?.screenshots?.takeIf { it.isNotEmpty() }?.let { shots ->
                ScreenshotViewer(
                    urls = shots.map { it.url },
                    startIndex = start.coerceIn(0, shots.lastIndex),
                    onDismiss = { viewerStart = null },
                )
            }
        }

        if (showReportDialog) {
            ReportDialog(
                onDismiss = { showReportDialog = false },
                onReport = { reason ->
                    showReportDialog = false
                    viewModel.report(reason)
                },
            )
        }


            if (product != null) {
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(Tokens.colorScheme.background)
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                ) {
                    if (state.isInstalling) {
                        LinearProgressIndicator(
                            progress = { state.downloadProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(SproutlyRadii.pill),
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                    SproutlyButton(
                        text = when {
                            !state.isInstalling -> if (state.installedPackage != null) "Open" else "Install"
                            state.downloadProgress > 0f ->
                                "Downloading ${(state.downloadProgress * 100).toInt()}%"
                            else -> "Preparing…"
                        },
                        onClick = {
                            if (state.installedPackage != null) viewModel.openInstalledApp()
                            else viewModel.installApp()
                        },
                        enabled = state.canInstall || state.installedPackage != null,
                        loading = state.isInstalling,
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


/** Full-screen screenshots: swipe sideways between them, tap outside or the close button to leave. */
@Composable
private fun ScreenshotViewer(urls: List<String>, startIndex: Int, onDismiss: () -> Unit) {
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        val pager = rememberPagerState(initialPage = startIndex) { urls.size }
        Box(
            Modifier
                .fillMaxSize()
                .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.94f))
                .clickable(onClick = onDismiss),
        ) {
            HorizontalPager(
                state = pager,
                modifier = Modifier.fillMaxSize(),
                pageSpacing = 12.dp,
            ) { page ->
                SproutlyImage(
                    url = urls[page],
                    contentDescription = "Screenshot ${page + 1} of ${urls.size}",
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 64.dp),
                    corner = 16.dp,
                    contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                )
            }
            Text(
                "${pager.currentPage + 1} / ${urls.size}",
                style = Tokens.typography.labelLarge,
                color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.9f),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 36.dp),
            )
            Surface(
                onClick = onDismiss,
                shape = CircleShape,
                color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.16f),
                modifier = Modifier.align(Alignment.TopEnd).padding(top = 20.dp, end = 16.dp).size(40.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    SproutlyIcon(
                        PhosphorIcons.Close,
                        contentDescription = "Close",
                        tint = androidx.compose.ui.graphics.Color.White,
                    )
                }
            }
        }
    }
}

/** A short, structured report — the reason is what a moderator needs. */
@Composable
private fun ReportDialog(onDismiss: () -> Unit, onReport: (String) -> Unit) {
    val reasons = listOf(
        "It does not work",
        "Misleading description",
        "Harmful or inappropriate",
        "It is not my content / copyright",
        "Something else",
    )
    var chosen by remember { mutableStateOf(reasons.first()) }

    app.tab.store.core.ui.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Report this app") },
        text = {
            Column {
                Text(
                    "Goes to moderators. The developer does not see who reported.",
                    style = Tokens.typography.bodySmall,
                    color = Tokens.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                reasons.forEach { reason ->
                    Surface(
                        onClick = { chosen = reason },
                        shape = SproutlyRadii.pill,
                        color = if (chosen == reason) Tokens.colorScheme.primaryContainer
                        else Tokens.colorScheme.surfaceContainerLow,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp),
                    ) {
                        Text(
                            reason,
                            style = Tokens.typography.bodyMedium,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        )
                    }
                }
            }
        },
        confirmButton = {
            app.tab.store.core.ui.TextButton(onClick = { onReport(chosen) }) {
                Text("Send report")
            }
        },
        dismissButton = {
            app.tab.store.core.ui.TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

/** The app's rating, and a way to set your own — one tap per star. */
@Composable
private fun RatingRow(
    summary: app.tab.store.domain.model.RatingSummary,
    onRate: (Int) -> Unit,
) {
    val mine = summary.mine?.rating ?: 0
    // Fill from the person's own rating when they have one, otherwise from the
    // average. Clamped to five: rounding 4.6 up to 5 is fine, 5.4 is not a thing.
    val shown = (if (mine > 0) mine else summary.average.roundToInt()).coerceIn(0, 5)
    Row(verticalAlignment = Alignment.CenterVertically) {
        // With no ratings there is no average to show: "—" rather than "0.0".
        Text(
            if (summary.hasRatings) summary.display else "—",
            style = Tokens.typography.titleMedium,
        )
        Spacer(Modifier.width(6.dp))
        repeat(5) { index ->
            val star = index + 1
            SproutlyIcon(
                // A filled glyph for a star you have, an outline for one you do
                // not. Tinting an outline star does not read as filled.
                icon = if (star <= shown) PhosphorIcons.StarFilled else PhosphorIcons.Star,
                contentDescription = if (mine > 0) "Your rating" else null,
                modifier = Modifier
                    .size(18.dp)
                    .clickable { onRate(star) },
                tint = if (star <= shown) {
                    Tokens.colorScheme.primary
                } else {
                    Tokens.colorScheme.outlineVariant
                },
            )
        }
        Spacer(Modifier.width(6.dp))
        Text(
            when {
                mine > 0 -> "You rated $mine"
                summary.hasRatings -> "${summary.count} rating${if (summary.count == 1) "" else "s"}"
                else -> "No ratings yet"
            },
            style = Tokens.typography.bodySmall,
            color = Tokens.colorScheme.onSurfaceVariant,
        )
    }
}

/** Every moderation action, on the listing it applies to. */
@Composable
private fun AdminActions(
    status: app.tab.store.domain.model.ProductStatus,
    reported: Boolean,
    onStatus: (app.tab.store.domain.model.ProductStatus) -> Unit,
    onResolve: (Boolean) -> Unit,
    onRemove: () -> Unit,
) {
    var confirmRemove by remember { mutableStateOf(false) }

    if (confirmRemove) {
        AlertDialog(
            onDismissRequest = { confirmRemove = false },
            title = { Text("Remove this app?") },
            text = {
                Text(
                    "The listing and its reviews are deleted for good. Blocking keeps it " +
                        "and only hides it from the store — use that if you may restore it.",
                )
            },
            confirmButton = {
                TextButton(onClick = { confirmRemove = false; onRemove() }) {
                    Text("Remove", color = Tokens.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmRemove = false }) { Text("Cancel") }
            },
        )
    }
    Surface(
        shape = SproutlyRadii.card,
        color = Tokens.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(
                "ADMIN",
                style = Tokens.typography.labelSmall,
                color = Tokens.colorScheme.onSurfaceVariant,
                letterSpacing = 1.4.sp,
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (status != app.tab.store.domain.model.ProductStatus.BLOCKED) {
                    AdminChip("Block") { onStatus(app.tab.store.domain.model.ProductStatus.BLOCKED) }
                } else {
                    AdminChip("Restore") { onStatus(app.tab.store.domain.model.ProductStatus.ACTIVE) }
                }
                if (status != app.tab.store.domain.model.ProductStatus.DRAFT) {
                    AdminChip("Unpublish") { onStatus(app.tab.store.domain.model.ProductStatus.DRAFT) }
                }
                if (reported) {
                    AdminChip("Keep") { onResolve(true) }
                }
                AdminChip("Delete") { confirmRemove = true }
            }
        }
    }
}

@Composable
private fun AdminChip(label: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = SproutlyRadii.pill,
        color = Tokens.colorScheme.surface,
    ) {
        Text(
            label,
            style = Tokens.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
        )
    }
}
