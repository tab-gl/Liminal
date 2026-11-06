package app.tab.store.feature.product

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import app.tab.store.core.designsystem.TablerIcons
import app.tab.store.core.designsystem.TablerIcon
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
    viewModel: ProductDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var showDeveloperSheet by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    fun shareProduct() {
        val product = state.product ?: return
        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(
                android.content.Intent.EXTRA_TEXT,
                "${product.title} on Sproutly — ${Routes.shareLink(product.id)}",
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
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                            modifier = Modifier.size(40.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                TablerIcon(TablerIcons.ChevronLeft, contentDescription = "Back")
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
                                    TablerIcon(
                                        if (state.isWishlisted) TablerIcons.Favourite
                                        else TablerIcons.Favourite,
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
                                    TablerIcon(TablerIcons.Share, contentDescription = "Share")
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
                        if (product.status == app.tab.store.domain.model.ProductStatus.BLOCKED) {
                            Spacer(Modifier.width(6.dp))
                            SproutlyTag(
                                text = "REMOVED",
                                container = MaterialTheme.colorScheme.errorContainer,
                                content = MaterialTheme.colorScheme.onErrorContainer,
                            )
                        } else if (false) {
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
                        onClick = { showDeveloperSheet = true },
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
                                TablerIcon(TablerIcons.Store,
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
                                    "About the developer",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            TablerIcon(
                                TablerIcons.ArrowRight,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    if (isAdmin) {
                        AdminActions(
                            status = product.status,
                            reported = product.reported,
                            onStatus = viewModel::adminSetStatus,
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

                    Spacer(Modifier.height(14.dp))
                    Surface(
                        onClick = { shareProduct() },
                        shape = SproutlyRadii.card,
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            TablerIcon(
                                TablerIcons.Share,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                "Share this app",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    Spacer(Modifier.height(10.dp))
                    Surface(
                        onClick = { showReportDialog = true },
                        shape = SproutlyRadii.card,
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            TablerIcon(
                                TablerIcons.Tag,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                "Report this app",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    if (product.versionName.isNotBlank()) {
                        Spacer(Modifier.height(14.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TablerIcon(
                                TablerIcons.Tag,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "Version ${product.versionName}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    if (product.builds.size > 1) {
                        Spacer(Modifier.height(14.dp))
                        Text("Builds", style = MaterialTheme.typography.titleSmall)
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            product.builds.forEachIndexed { index, build ->
                                SproutlyChip(
                                    text = build.name.ifBlank { "Build ${index + 1}" },
                                    selected = state.chosenFlavour == index,
                                    onClick = { viewModel.chooseFlavour(index) },
                                )
                            }
                        }
                    }

                    if (product.scanRisk.isNotBlank()) {
                        Spacer(Modifier.height(14.dp))
                        RiskMeter(
                            score = product.scanScore,
                            reasons = product.scanWarnings,
                        )
                    }

                    if (product.releaseNotes.isNotBlank()) {
                        Spacer(Modifier.height(16.dp))
                        Text("What's new", style = MaterialTheme.typography.titleSmall)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            product.releaseNotes,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
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
                        text = when {
                            !state.isInstalling -> "Install"
                            state.downloadProgress > 0f ->
                                "Downloading ${(state.downloadProgress * 100).toInt()}%"
                            else -> "Preparing…"
                        },
                        onClick = viewModel::installApp,
                        enabled = state.canInstall,
                        loading = state.isInstalling,
                    )
                    if (state.isInstalling) {
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { state.downloadProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(SproutlyRadii.pill),
                        )
                    }
                    if (state.installedPackage != null && !state.isInstalling) {
                        Spacer(Modifier.height(10.dp))
                        Surface(
                            shape = SproutlyRadii.card,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(Modifier.padding(14.dp)) {
                                Text(
                                    "Android is installing it",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                                Text(
                                    "Tap Install on the Android prompt, then open it here.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
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
                            "Android needs permission to install apps from Sproutly.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        TextButton(onClick = viewModel::openInstallPermission) {
                            Text("Open settings")
                        }
                    }
            if (product.screenshots.isNotEmpty()) {
                Spacer(Modifier.height(22.dp))
                Text("Screenshots", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(10.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(product.screenshots) { shot ->
                        SproutlyImage(
                            url = shot.url,
                            contentDescription = null,
                            modifier = Modifier.size(width = 150.dp, height = 266.dp),
                            corner = 18.dp,
                        )
                    }
                }
            }

                    Spacer(Modifier.height(28.dp))
                }
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

        if (showDeveloperSheet) {
            DeveloperSheet(
                store = state.store,
                storeName = state.product?.storeName.orEmpty(),
                apps = state.developerApps,
                onDismiss = { showDeveloperSheet = false },
                onApp = { id ->
                    showDeveloperSheet = false
                    onProduct(id)
                },
            )
        }

        SnackbarHost(
            hostState = snackbar,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp),
        )
    }
}

/**
 * Developer sheet: who made this, and what else they have published. A sheet
 * rather than a screen — it is context, not a destination.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun DeveloperSheet(
    store: app.tab.store.domain.model.Store?,
    storeName: String,
    apps: List<app.tab.store.domain.model.Product>,
    onDismiss: () -> Unit,
    onApp: (String) -> Unit,
) {
    androidx.compose.material3.ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = SproutlyRadii.sheet,
    ) {
        Column(Modifier.padding(horizontal = 20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SproutlyImage(
                    url = store?.logoUrl,
                    contentDescription = null,
                    modifier = Modifier.size(56.dp),
                    corner = 18.dp,
                )
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            store?.name?.ifBlank { storeName } ?: storeName,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                        )
                        if (store?.verified == true) {
                            Spacer(Modifier.width(6.dp))
                            TablerIcon(
                                TablerIcons.Verified,
                                contentDescription = "Verified developer",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                    Text(
                        store?.tagline?.ifBlank { "Independent developer" }
                            ?: "Independent developer",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                    )
                }
            }

            if (!store?.description.isNullOrBlank()) {
                Spacer(Modifier.height(14.dp))
                Text(
                    store?.description.orEmpty(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (apps.isNotEmpty()) {
                Spacer(Modifier.height(20.dp))
                Text("More from this developer", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(10.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(apps) { app ->
                        ProductCard(
                            product = app,
                            onClick = { onApp(app.id) },
                            modifier = Modifier.width(140.dp),
                            compact = true,
                        )
                    }
                }
            }

            Spacer(Modifier.height(28.dp))
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

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Report this app") },
        text = {
            Column {
                Text(
                    "Goes to moderators. The developer does not see who reported.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                reasons.forEach { reason ->
                    Surface(
                        onClick = { chosen = reason },
                        shape = SproutlyRadii.pill,
                        color = if (chosen == reason) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceContainerLow,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp),
                    ) {
                        Text(
                            reason,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        )
                    }
                }
            }
        },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = { onReport(chosen) }) {
                Text("Send report")
            }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) { Text("Cancel") }
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
    val shown = mine.takeIf { it > 0 } ?: summary.average.roundToInt()
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(summary.display, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.width(6.dp))
        repeat(5) { index ->
            val star = index + 1
            TablerIcon(
                icon = TablerIcons.Star,
                contentDescription = if (mine > 0) "Your rating" else null,
                modifier = Modifier
                    .size(18.dp)
                    .clickable { onRate(star) },
                tint = if (star <= shown) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.outlineVariant
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
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
) {
    Surface(
        shape = SproutlyRadii.card,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(
                "ADMIN",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                    AdminChip("Remove") { onResolve(false) }
                }
            }
        }
    }
}

@Composable
private fun AdminChip(label: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = SproutlyRadii.pill,
        color = MaterialTheme.colorScheme.surface,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
        )
    }
}
