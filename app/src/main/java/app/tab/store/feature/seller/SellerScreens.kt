package app.tab.store.feature.seller

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.togetherWith
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.tween
import app.tab.store.core.ui.Tokens
import androidx.annotation.DrawableRes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.style.TextOverflow
import app.tab.store.core.designsystem.SproutlyChip
import app.tab.store.domain.model.Product
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import app.tab.store.core.ui.TextButton
import app.tab.store.core.utils.InstallCount
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.ui.graphics.Color
import app.tab.store.core.ui.Surface
import app.tab.store.core.ui.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.tab.store.core.designsystem.PhosphorIcons
import app.tab.store.core.designsystem.SproutlyIcon
import app.tab.store.core.designsystem.EmptyState
import app.tab.store.core.designsystem.SproutlyButton
import app.tab.store.core.designsystem.SproutlyImage
import app.tab.store.core.designsystem.SproutlyPalette
import app.tab.store.core.designsystem.SproutlyRadii
import app.tab.store.core.designsystem.SproutlyTag
import app.tab.store.core.designsystem.SproutlyTextField
import app.tab.store.core.designsystem.SproutlyTopBar
import app.tab.store.core.designsystem.formatCompact
import app.tab.store.core.designsystem.formatPrice
import app.tab.store.core.utils.DateFormatters
import app.tab.store.feature.orders.OrderStatusChip

@Composable
fun SellerSetupScreen(
    onBack: () -> Unit,
    onCreated: () -> Unit,
    viewModel: SellerSetupViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.createdStoreId) {
        if (state.createdStoreId != null) onCreated()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding(),
    ) {
        SproutlyTopBar(title = "Become a developer", onBack = onBack)
        Column(Modifier.padding(horizontal = 24.dp)) {
            Surface(
                shape = SproutlyRadii.hero,
                color = SproutlyPalette.Lime,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(20.dp)) {
                    SproutlyIcon(PhosphorIcons.Store,
                        contentDescription = null,
                        tint = SproutlyPalette.OliveDeep,
                        modifier = Modifier.size(30.dp),
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "Build it. Publish it. Grow it.",
                        style = Tokens.typography.headlineMedium,
                        color = SproutlyPalette.OliveDeep,
                    )
                    Text(
                        "Set up a developer profile in under a minute, publish your first app and track installs.",
                        style = Tokens.typography.bodySmall,
                        color = SproutlyPalette.Olive,
                    )
                }
            }
            Spacer(Modifier.height(20.dp))

            SproutlyTextField(
                value = state.storeName,
                onValueChange = viewModel::onName,
                label = "Developer name",
                placeholder = "The Green Shelf",
                helperText = "This is what people installing your apps will see.",
            )
            SproutlyTextField(
                value = state.tagline,
                onValueChange = viewModel::onTagline,
                label = "Tagline",
                placeholder = "Plants and pots for small homes",
                helperText = "${state.tagline.length}/80",
            )

            if (state.error != null) {
                Surface(
                    color = Tokens.colorScheme.errorContainer,
                    shape = Tokens.shapes.small,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        state.error.orEmpty(),
                        style = Tokens.typography.bodySmall,
                        color = Tokens.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(12.dp),
                    )
                }
                Spacer(Modifier.height(12.dp))
            }

            SproutlyButton(
                text = "Create developer profile",
                onClick = viewModel::createStore,
                enabled = state.canSubmit,
                loading = state.isCreating,
                icon = PhosphorIcons.Plus,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "New stores start in review. You can add products right away; they go live once the store is approved.",
                style = Tokens.typography.bodySmall,
                color = Tokens.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
fun SellerDashboardScreen(
    onBack: () -> Unit,
    onProducts: () -> Unit,
    onOrders: () -> Unit,
    onAnalytics: () -> Unit,
    onStoreEdit: () -> Unit,
    onVerification: () -> Unit = {},
    onSetupStore: () -> Unit,
    viewModel: SellerDashboardViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var tab by remember { mutableStateOf(SellerTab.OVERVIEW) }
    var offerFor by remember { mutableStateOf<Product?>(null) }

    offerFor?.let { target ->
        SellerOfferDialog(
            product = target,
            onDismiss = { offerFor = null },
            onSave = { compareAt, days, kind, code ->
                offerFor = null
                viewModel.setOffer(target, compareAt, days, kind, code)
            },
        )
    }

    Box(Modifier.fillMaxSize()) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        SproutlyTopBar(
            title = state.store?.name ?: "Developer hub",
            subtitle = state.store?.status?.name?.lowercase()?.replaceFirstChar { it.uppercase() },
            onBack = onBack,
        )


        if (state.needsStore) {
            EmptyState(
                title = "No developer profile yet",
                message = "Open a developer profile to start publishing apps on Liminal.",
                icon = PhosphorIcons.Store,
                actionLabel = "Get started",
                onAction = onSetupStore,
            )
            return@Column
        }

        AnimatedContent(
            targetState = tab,
            transitionSpec = {
                (fadeIn(tween(220)) + slideInVertically { it / 14 })
                    .togetherWith(fadeOut(tween(120)) + slideOutVertically { -it / 20 })
            },
            label = "consoleTab",
        ) { current ->
        if (current == SellerTab.OFFERS) {
            Column(Modifier.padding(horizontal = 20.dp)) {
                Text("Offers", style = Tokens.typography.headlineSmall)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Put any of your apps on offer, or take one off. People see the " +
                        "discount on the app and on the Offers tab.",
                    style = Tokens.typography.bodySmall,
                    color = Tokens.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))

                if (state.products.isEmpty()) {
                    EmptyState(
                        title = "Nothing published yet",
                        message = "Publish an app and it will appear here, ready for an offer.",
                        icon = PhosphorIcons.Gift,
                    )
                }

                state.products.forEach { product ->
                    Surface(
                        shape = SproutlyRadii.card,
                        color = Tokens.colorScheme.surfaceContainerLow,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    product.title,
                                    style = Tokens.typography.titleSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    if (product.hasActiveOffer) {
                                        "${formatPrice(product.price)} · was " +
                                            formatPrice(product.compareAtPrice ?: product.price) +
                                            " · ${product.discountPercent}% off"
                                    } else {
                                        "${formatPrice(product.price)} · no offer"
                                    },
                                    style = Tokens.typography.bodySmall,
                                    color = Tokens.colorScheme.onSurfaceVariant,
                                )
                            }
                            Surface(
                                onClick = { offerFor = product },
                                shape = SproutlyRadii.pill,
                                color = if (product.hasActiveOffer) {
                                    Tokens.colorScheme.primary
                                } else {
                                    Tokens.colorScheme.primaryContainer
                                },
                            ) {
                                Text(
                                    if (product.hasActiveOffer) "Edit" else "Add offer",
                                    style = Tokens.typography.labelLarge,
                                    color = if (product.hasActiveOffer) {
                                        Tokens.colorScheme.onPrimary
                                    } else {
                                        Tokens.colorScheme.onPrimaryContainer
                                    },
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
            return@AnimatedContent
        }

        if (current == SellerTab.APPS) {
            Column(Modifier.padding(horizontal = 20.dp)) {
                if (state.products.isEmpty()) {
                    EmptyState(
                        title = "No apps yet",
                        message = "Publish one and it appears here.",
                        icon = PhosphorIcons.Grid,
                        actionLabel = "Publish an app",
                        onAction = onProducts,
                    )
                } else {
                    state.products.forEach { product ->
                        Surface(
                            onClick = onProducts,
                            shape = SproutlyRadii.card,
                            color = Tokens.colorScheme.surface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                SproutlyImage(
                                    url = product.images.firstOrNull()?.url,
                                    contentDescription = product.title,
                                    modifier = Modifier.size(44.dp),
                                    corner = 14.dp,
                                )
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(product.title, style = Tokens.typography.titleSmall)
                                    Text(
                                        product.status.name.lowercase()
                                            .replaceFirstChar { it.uppercase() },
                                        style = Tokens.typography.bodySmall,
                                        color = Tokens.colorScheme.onSurfaceVariant,
                                    )
                                }
                                SproutlyIcon(
                                    PhosphorIcons.ChevronRight,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = Tokens.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = onProducts) { Text("Manage all apps") }
                }
                Spacer(Modifier.height(110.dp))
            }
            return@AnimatedContent
        }

        if (current == SellerTab.INSTALLS) {
            Column(Modifier.padding(horizontal = 20.dp)) {
                if (state.recentOrders.isEmpty()) {
                    EmptyState(
                        title = "No installs yet",
                        message = "Share an app link to get your first one.",
                        icon = PhosphorIcons.Receipt,
                    )
                } else {
                    state.recentOrders.forEach { order ->
                        Surface(
                            shape = SproutlyRadii.card,
                            color = Tokens.colorScheme.surface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        order.items.firstOrNull()?.title ?: "An app",
                                        style = Tokens.typography.titleSmall,
                                    )
                                    Text(
                                        app.tab.store.core.utils.DateFormatters.relative(order.placedAt),
                                        style = Tokens.typography.bodySmall,
                                        color = Tokens.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = onOrders) { Text("See every install") }
                }
                Spacer(Modifier.height(110.dp))
            }
            return@AnimatedContent
        }

        Column(Modifier.padding(horizontal = 20.dp)) {
            Surface(
                shape = SproutlyRadii.hero,
                color = SproutlyPalette.Olive,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text(
                        "INSTALLS",
                        style = Tokens.typography.labelSmall,
                        color = SproutlyPalette.LimeSoft,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        InstallCount.label(state.stats.orderCount),
                        style = Tokens.typography.displaySmall,
                        color = SproutlyPalette.Lime,
                    )
                    Spacer(Modifier.height(14.dp))
                    Row {
                        DashStat("Apps", state.stats.activeProducts.toString(), Modifier.weight(1f))
                        DashStat("Drafts", state.drafts.size.toString(), Modifier.weight(1f))
                        DashStat("Live", state.stats.activeProducts.toString(), Modifier.weight(1f))
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Bento: one tall tile for the main action, then pairs. The grid is
            // built from rows rather than a LazyVerticalGrid so it scrolls with
            // the rest of the page instead of fighting it.
            Text("Publish", style = Tokens.typography.titleSmall)
            Spacer(Modifier.height(10.dp))

            Bento(
                icon = PhosphorIcons.Plus,
                label = "Publish a new app",
                detail = "Upload a build or paste a link, set a price, ship it.",
                emphasis = true,
                onClick = onProducts,
            )
            Spacer(Modifier.height(10.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Bento(
                    icon = PhosphorIcons.Dice,
                    label = "Your apps",
                    detail = if (state.stats.activeProducts == 1) "1 app"
                    else "${state.stats.activeProducts} apps",
                    modifier = Modifier.weight(1f),
                    onClick = onProducts,
                )
                Bento(
                    icon = PhosphorIcons.Receipt,
                    label = "Installs",
                    detail = app.tab.store.core.utils.InstallCount.label(state.stats.orderCount) + " installs",
                    modifier = Modifier.weight(1f),
                    onClick = onOrders,
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Bento(
                    icon = PhosphorIcons.Store,
                    label = "Store page",
                    detail = "What people see",
                    modifier = Modifier.weight(1f),
                    onClick = onStoreEdit,
                )
                Bento(
                    icon = PhosphorIcons.Analytics,
                    label = "Analytics",
                    detail = "Charts and trends",
                    modifier = Modifier.weight(1f),
                    onClick = onAnalytics,
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Bento(
                    icon = if (state.store?.verified == true) PhosphorIcons.Verified else PhosphorIcons.Tag,
                    label = "Verification",
                    detail = when {
                        state.store?.verified == true -> "Verified"
                        state.store?.verificationStatus == "PENDING" -> "In review"
                        else -> "Needed for paid apps"
                    },
                    modifier = Modifier.weight(1f),
                    // Its own screen: opening the store editor from here meant two
                    // tiles led to the same page.
                    onClick = onVerification,
                )
                Bento(
                    icon = PhosphorIcons.Receipt,
                    label = "All installs",
                    detail = "Every install, dated",
                    modifier = Modifier.weight(1f),
                    onClick = onOrders,
                )
            }
            Spacer(Modifier.height(18.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Developer profile", style = Tokens.typography.headlineSmall)
                Spacer(Modifier.weight(1f))
                Surface(
                    onClick = onStoreEdit,
                    shape = SproutlyRadii.pill,
                    color = Tokens.colorScheme.primaryContainer,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        SproutlyIcon(PhosphorIcons.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Edit developer profile", style = Tokens.typography.labelMedium)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Surface(
                shape = SproutlyRadii.card,
                color = Tokens.colorScheme.surface,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SproutlyImage(
                        url = state.store?.logoUrl,
                        contentDescription = state.store?.name,
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape),
                        corner = 27.dp,
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            state.store?.name.orEmpty(),
                            style = Tokens.typography.titleSmall,
                        )
                        Text(
                            if (state.stats.activeProducts == 1) "1 live app"
                            else "${state.stats.activeProducts} live apps",
                            style = Tokens.typography.bodySmall,
                            color = Tokens.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (state.stats.pendingOrders > 0) {
                        SproutlyTag(
                            text = "${state.stats.pendingOrders} new",
                            container = Tokens.colorScheme.primary,
                            content = Tokens.colorScheme.onPrimary,
                        )
                    }
                }
            }

            if (state.drafts.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                Surface(
                    shape = SproutlyRadii.card,
                    color = Tokens.colorScheme.tertiaryContainer,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Text(
                            "Not published yet",
                            style = Tokens.typography.titleSmall,
                            color = Tokens.colorScheme.onTertiaryContainer,
                        )
                        Text(
                            if (state.drafts.size == 1) "1 draft is not published"
                            else "${state.drafts.size} drafts are not published",
                            style = Tokens.typography.bodySmall,
                            color = Tokens.colorScheme.onTertiaryContainer,
                        )
                        state.drafts.take(3).forEach { product ->
                            Text(
                                product.title,
                                style = Tokens.typography.bodySmall,
                                color = Tokens.colorScheme.onTertiaryContainer,
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        // Says what to do, not just that something is wrong.
                        Surface(
                            onClick = { onProducts() },
                            shape = SproutlyRadii.pill,
                            color = Tokens.colorScheme.onTertiaryContainer,
                        ) {
                            Text(
                                "Open drafts",
                                style = Tokens.typography.labelMedium,
                                color = Tokens.colorScheme.tertiaryContainer,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(18.dp))
            Text("Recent installs", style = Tokens.typography.headlineSmall)
            Spacer(Modifier.height(8.dp))
            if (state.recentOrders.isEmpty()) {
                Text(
                    "No installs yet. Share an app link to get your first one.",
                    style = Tokens.typography.bodySmall,
                    color = Tokens.colorScheme.onSurfaceVariant,
                )
            } else {
                state.recentOrders.forEach { order ->
                    Surface(
                        shape = SproutlyRadii.card,
                        color = Tokens.colorScheme.surface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "#${order.id.take(8).uppercase()} · ${order.buyerName.ifBlank { "Customer" }}",
                                    style = Tokens.typography.titleSmall,
                                )
                                Text(
                                    DateFormatters.dateTime(order.placedAt),
                                    style = Tokens.typography.bodySmall,
                                    color = Tokens.colorScheme.onSurfaceVariant,
                                )
                            }
                            OrderStatusChip(order.status)
                        }
                    }
                }
            }
            Spacer(Modifier.height(110.dp))
        }
        }
    }
        SellerConsoleBar(
            selected = tab,
            onSelect = { tab = it },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 14.dp),
        )
    }
}

@Composable
private fun DashStat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(
            value,
            style = Tokens.typography.titleLarge,
            color = SproutlyPalette.Lime,
        )
        Text(
            label,
            style = Tokens.typography.bodySmall,
            color = SproutlyPalette.LimeSoft,
        )
    }
}

@Composable
private fun QuickAction(
    @DrawableRes icon: Int,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = SproutlyRadii.card,
        color = Tokens.colorScheme.surface,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Tokens.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                SproutlyIcon(icon,
                    contentDescription = null,
                    tint = Tokens.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(18.dp),
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(label, style = Tokens.typography.labelMedium)
        }
    }
}

@Composable
fun SellerAnalyticsScreen(
    onBack: () -> Unit,
    viewModel: SellerAnalyticsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val stats = state.stats
    val maxRevenue = stats.revenueByDay.maxOfOrNull { it.second }?.coerceAtLeast(1L) ?: 1L

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        SproutlyTopBar(title = "Analytics", subtitle = "Last 7 days and lifetime", onBack = onBack)
        Column(Modifier.padding(horizontal = 20.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricTile("Revenue", formatCompact(stats.totalRevenue), Modifier.weight(1f))
                MetricTile("Installs", stats.orderCount.toString(), Modifier.weight(1f))
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricTile("Units sold", stats.unitsSold.toString(), Modifier.weight(1f))
                MetricTile("Needs attention", stats.lowStockCount.toString(), Modifier.weight(1f))
            }

            Spacer(Modifier.height(20.dp))
            Text("Revenue, last 7 days", style = Tokens.typography.headlineSmall)
            Spacer(Modifier.height(12.dp))
            Surface(
                shape = SproutlyRadii.card,
                color = Tokens.colorScheme.surface,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(170.dp)
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom,
                ) {
                    stats.revenueByDay.forEach { (day, revenue) ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f),
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(18.dp)
                                    .height(
                                        (120 * (revenue.toFloat() / maxRevenue.toFloat()))
                                            .coerceAtLeast(3f).dp,
                                    )
                                    .clip(SproutlyRadii.pill)
                                    .background(
                                        if (revenue > 0) SproutlyPalette.Olive
                                        else Tokens.colorScheme.surfaceContainerHigh,
                                    ),
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(day, style = Tokens.typography.labelSmall)
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            Text("Top products", style = Tokens.typography.headlineSmall)
            Spacer(Modifier.height(8.dp))
            if (stats.topProducts.isEmpty()) {
                Text(
                    "No sales recorded yet.",
                    style = Tokens.typography.bodySmall,
                    color = Tokens.colorScheme.onSurfaceVariant,
                )
            } else {
                stats.topProducts.forEach { product ->
                    Surface(
                        shape = SproutlyRadii.card,
                        color = Tokens.colorScheme.surface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            SproutlyImage(
                                url = product.imageUrl,
                                contentDescription = product.title,
                                modifier = Modifier.size(44.dp),
                                corner = 12.dp,
                            )
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    product.title,
                                    style = Tokens.typography.titleSmall,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                )
                                Text(
                                    "${product.unitsSold} downloads",
                                    style = Tokens.typography.bodySmall,
                                    color = Tokens.colorScheme.onSurfaceVariant,
                                )
                            }
                            Text(formatPrice(product.revenue), style = Tokens.typography.titleSmall)
                        }
                    }
                }
            }
            Spacer(Modifier.height(28.dp))
        }
    }
}

@Composable
private fun MetricTile(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        shape = SproutlyRadii.card,
        color = Tokens.colorScheme.surface,
        modifier = modifier,
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(value, style = Tokens.typography.headlineMedium)
            Text(
                label,
                style = Tokens.typography.bodySmall,
                color = Tokens.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** One bento tile: a soft surface with an icon, a label and a line of detail. */
@Composable
private fun Bento(
    icon: Int,
    label: String,
    detail: String,
    modifier: Modifier = Modifier,
    emphasis: Boolean = false,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = SproutlyRadii.card,
        color = if (emphasis) Tokens.colorScheme.primaryContainer
        else Tokens.colorScheme.surfaceContainerLow,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(
                        if (emphasis) Tokens.colorScheme.primary
                        else Tokens.colorScheme.surfaceContainerHigh,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                SproutlyIcon(
                    icon = icon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = if (emphasis) Tokens.colorScheme.onPrimary
                    else Tokens.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                label,
                style = Tokens.typography.titleSmall,
                color = if (emphasis) Tokens.colorScheme.onPrimaryContainer
                else Tokens.colorScheme.onSurface,
            )
            Text(
                detail,
                style = Tokens.typography.bodySmall,
                color = if (emphasis) Tokens.colorScheme.onPrimaryContainer
                else Tokens.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** The developer console's tabs. */
/** The console's floating bar: icon and label, pill highlight, no chrome. */
@Composable
private fun SellerConsoleBar(
    selected: SellerTab,
    onSelect: (SellerTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = SproutlyRadii.nav,
        color = Tokens.colorScheme.surface,
        shadowElevation = 10.dp,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            Tokens.colorScheme.outlineVariant.copy(alpha = 0.5f),
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SellerTab.entries.forEach { entry ->
                val active = entry == selected
                Surface(
                    onClick = { onSelect(entry) },
                    shape = SproutlyRadii.pill,
                    color = if (active) Tokens.colorScheme.primary
                    else Color.Transparent,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        SproutlyIcon(
                            icon = entry.icon,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = if (active) Tokens.colorScheme.onPrimary
                            else Tokens.colorScheme.onSurfaceVariant,
                        )
                        if (active) {
                            Spacer(Modifier.width(6.dp))
                            Text(
                                entry.label,
                                style = Tokens.typography.labelMedium,
                                color = Tokens.colorScheme.onPrimary,
                            )
                        }
                    }
                }
            }
        }
    }
}

private enum class SellerTab(val label: String, val icon: Int) {
    OVERVIEW("Overview", PhosphorIcons.Home),
    APPS("Apps", PhosphorIcons.Grid),
    OFFERS("Offers", PhosphorIcons.Gift),
    INSTALLS("Installs", PhosphorIcons.Receipt),
}

/**
 * Verification, on its own page.
 *
 * It used to be a tile that opened the store editor, which made two tiles do the
 * same thing and left the one question people actually have — "am I verified, and
 * if not, what do I do?" — answered nowhere.
 */
@Composable
fun SellerVerificationScreen(
    onBack: () -> Unit,
    viewModel: SellerDashboardViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val store = state.store
    val verified = store?.verified == true
    val pending = store?.verificationStatus == "PENDING"

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        SproutlyTopBar(title = "Verification", onBack = onBack)
        Column(Modifier.padding(horizontal = 20.dp)) {
            Surface(
                shape = SproutlyRadii.hero,
                color = if (verified) Tokens.colorScheme.primaryContainer
                else Tokens.colorScheme.surfaceContainerLow,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SproutlyIcon(
                            if (verified) PhosphorIcons.Verified else PhosphorIcons.Tag,
                            contentDescription = null,
                            modifier = Modifier.size(22.dp),
                            tint = Tokens.colorScheme.primary,
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            when {
                                verified -> "Verified"
                                pending -> "In review"
                                else -> "Not verified"
                            },
                            style = Tokens.typography.titleMedium,
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        when {
                            verified ->
                                "Your studio is verified. You can publish paid apps, and your " +
                                    "listings carry the badge."
                            pending ->
                                "Your request is with the team. You can keep publishing free " +
                                    "apps while you wait."
                            else ->
                                "Free apps need no verification. Paid apps do: it is how a buyer " +
                                    "knows who they are paying."
                        },
                        style = Tokens.typography.bodyMedium,
                        color = Tokens.colorScheme.onSurfaceVariant,
                    )
                    if (!verified && !pending) {
                        Spacer(Modifier.height(16.dp))
                        SproutlyButton(
                            text = "Request verification",
                            onClick = { viewModel.requestVerification() },
                            loading = state.isLoading,
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(
                "What we check",
                style = Tokens.typography.titleSmall,
            )
            Spacer(Modifier.height(6.dp))
            listOf(
                "That you own the studio name you publish under.",
                "That a paid listing has a working build attached.",
                "That nothing you have published is under moderation.",
            ).forEach { line ->
                Row(Modifier.padding(vertical = 4.dp)) {
                    Text("·", style = Tokens.typography.bodyMedium, color = Tokens.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        line,
                        style = Tokens.typography.bodySmall,
                        color = Tokens.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(30.dp))
        }
    }
}
