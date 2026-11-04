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
import app.tab.store.core.designsystem.SproutlyImage
import app.tab.store.core.designsystem.SproutlyRadii
import app.tab.store.core.designsystem.SproutlyTag
import app.tab.store.core.designsystem.ProductCard
import app.tab.store.core.designsystem.formatPrice

@Composable
fun ProductDetailScreen(
    productId: String,
    onBack: () -> Unit,
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

                    if (!product.apkUrl.isNullOrBlank()) {
                        Spacer(Modifier.height(10.dp))
                        Surface(
                            onClick = { viewModel.installBuild() },
                            shape = SproutlyRadii.card,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                TablerIcon(
                                    TablerIcons.Download,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    if (state.isInstalling) "Downloading…" else "Install the build",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                            }
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
                        text = if (product.isOutOfStock) "Unavailable" else "Get",
                        onClick = viewModel::install,
                        enabled = state.canInstall,
                        loading = state.isInstalling,
                    )
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
                    "Reports go to Sproutly moderators. You are not identified to the developer.",
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

