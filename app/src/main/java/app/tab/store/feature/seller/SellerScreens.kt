package app.tab.store.feature.seller

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import app.tab.store.core.designsystem.TablerIcons
import app.tab.store.core.designsystem.TablerIcon
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
                    TablerIcon(TablerIcons.Store,
                        contentDescription = null,
                        tint = SproutlyPalette.OliveDeep,
                        modifier = Modifier.size(30.dp),
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "Build it. Publish it. Grow it.",
                        style = MaterialTheme.typography.headlineMedium,
                        color = SproutlyPalette.OliveDeep,
                    )
                    Text(
                        "Set up a developer profile in under a minute, publish your first app and track installs.",
                        style = MaterialTheme.typography.bodySmall,
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
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        state.error.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
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
                icon = TablerIcons.Plus,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "New stores start in review. You can add products right away; they go live once the store is approved.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
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

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SellerTab.entries.forEach { entry ->
                SproutlyChip(
                    text = entry.label,
                    selected = tab == entry,
                    onClick = { tab = entry },
                )
            }
        }

        if (state.needsStore) {
            EmptyState(
                title = "No developer profile yet",
                message = "Open a developer profile to start publishing apps on Sproutly.",
                icon = TablerIcons.Store,
                actionLabel = "Get started",
                onAction = onSetupStore,
            )
            return@Column
        }

        if (tab == SellerTab.OFFERS) {
            Column(Modifier.padding(horizontal = 20.dp)) {
                Text("Offers", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Put any of your apps on offer, or take one off. People see the " +
                        "discount on the app and on the Offers tab.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))

                if (state.products.isEmpty()) {
                    EmptyState(
                        title = "Nothing published yet",
                        message = "Publish an app and it will appear here, ready for an offer.",
                        icon = TablerIcons.Gift,
                    )
                }

                state.products.forEach { product ->
                    Surface(
                        shape = SproutlyRadii.card,
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
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
                                    style = MaterialTheme.typography.titleSmall,
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
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Surface(
                                onClick = { offerFor = product },
                                shape = SproutlyRadii.pill,
                                color = if (product.hasActiveOffer) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.primaryContainer
                                },
                            ) {
                                Text(
                                    if (product.hasActiveOffer) "Edit" else "Add offer",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (product.hasActiveOffer) {
                                        MaterialTheme.colorScheme.onPrimary
                                    } else {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    },
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
            return@Column
        }

        Column(Modifier.padding(horizontal = 20.dp)) {
            Surface(
                shape = SproutlyRadii.hero,
                color = SproutlyPalette.Olive,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text(
                        "TOTAL REVENUE",
                        style = MaterialTheme.typography.labelSmall,
                        color = SproutlyPalette.LimeSoft,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        formatPrice(state.stats.totalRevenue),
                        style = MaterialTheme.typography.displaySmall,
                        color = SproutlyPalette.Lime,
                    )
                    Spacer(Modifier.height(14.dp))
                    Row {
                        DashStat("Installs", state.stats.orderCount.toString(), Modifier.weight(1f))
                        DashStat("Apps", state.stats.activeProducts.toString(), Modifier.weight(1f))
                        DashStat("Avg order", formatPrice(state.stats.averageOrderValue), Modifier.weight(1f))
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Bento: one tall tile for the main action, then pairs. The grid is
            // built from rows rather than a LazyVerticalGrid so it scrolls with
            // the rest of the page instead of fighting it.
            Text("Publish", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(10.dp))

            Bento(
                icon = TablerIcons.Plus,
                label = "Publish a new app",
                detail = "Upload a build or paste a link, set a price, ship it.",
                emphasis = true,
                onClick = onProducts,
            )
            Spacer(Modifier.height(10.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Bento(
                    icon = TablerIcons.Dice,
                    label = "Your apps",
                    detail = if (state.stats.activeProducts == 1) "1 app"
                    else "${state.stats.activeProducts} apps",
                    modifier = Modifier.weight(1f),
                    onClick = onProducts,
                )
                Bento(
                    icon = TablerIcons.Receipt,
                    label = "Installs",
                    detail = app.tab.store.core.utils.InstallCount.label(state.stats.orderCount) + " installs",
                    modifier = Modifier.weight(1f),
                    onClick = onOrders,
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Bento(
                    icon = TablerIcons.Receipt,
                    label = "Installs",
                    detail = "Who and when",
                    modifier = Modifier.weight(1f),
                    onClick = onOrders,
                )
                Bento(
                    icon = TablerIcons.Store,
                    label = "Your store page",
                    detail = "What people see",
                    modifier = Modifier.weight(1f),
                    onClick = onStoreEdit,
                )
                Bento(
                    icon = TablerIcons.Analytics,
                    label = "Analytics",
                    detail = "Charts and trends",
                    modifier = Modifier.weight(1f),
                    onClick = onAnalytics,
                )
                Bento(
                    icon = if (state.store?.verified == true) TablerIcons.Verified else TablerIcons.Tag,
                    label = "Verification",
                    detail = when {
                        state.store?.verified == true -> "Verified"
                        state.store?.verificationStatus == "PENDING" -> "In review"
                        else -> "Needed for paid apps"
                    },
                    modifier = Modifier.weight(1f),
                    onClick = onStoreEdit,
                )
            }

            Spacer(Modifier.height(18.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Developer profile", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.weight(1f))
                Surface(
                    onClick = onStoreEdit,
                    shape = SproutlyRadii.pill,
                    color = MaterialTheme.colorScheme.primaryContainer,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TablerIcon(TablerIcons.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Edit developer profile", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Surface(
                shape = SproutlyRadii.card,
                color = MaterialTheme.colorScheme.surface,
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
                            style = MaterialTheme.typography.titleSmall,
                        )
                        Text(
                            if (state.stats.activeProducts == 1) "1 live app"
                            else "${state.stats.activeProducts} live apps",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (state.stats.pendingOrders > 0) {
                        SproutlyTag(
                            text = "${state.stats.pendingOrders} new",
                            container = MaterialTheme.colorScheme.primary,
                            content = MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                }
            }

            if (state.drafts.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                Surface(
                    shape = SproutlyRadii.card,
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Text(
                            "Not published yet",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                        )
                        Text(
                            if (state.drafts.size == 1) "1 draft is not published"
                            else "${state.drafts.size} drafts are not published",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                        )
                        state.drafts.take(3).forEach { product ->
                            Text(
                                product.title,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        // Says what to do, not just that something is wrong.
                        Surface(
                            onClick = { onProducts() },
                            shape = SproutlyRadii.pill,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                        ) {
                            Text(
                                "Open drafts",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.tertiaryContainer,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(18.dp))
            Text("Recent installs", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(8.dp))
            if (state.recentOrders.isEmpty()) {
                Text(
                    "No installs yet. Share an app link to get your first one.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                state.recentOrders.forEach { order ->
                    Surface(
                        shape = SproutlyRadii.card,
                        color = MaterialTheme.colorScheme.surface,
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
                                    style = MaterialTheme.typography.titleSmall,
                                )
                                Text(
                                    DateFormatters.dateTime(order.placedAt),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            OrderStatusChip(order.status)
                        }
                    }
                }
            }
            Spacer(Modifier.height(28.dp))
        }
    }
}

@Composable
private fun DashStat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(
            value,
            style = MaterialTheme.typography.titleLarge,
            color = SproutlyPalette.Lime,
        )
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
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
        color = MaterialTheme.colorScheme.surface,
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
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                TablerIcon(icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(18.dp),
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(label, style = MaterialTheme.typography.labelMedium)
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
            Text("Revenue, last 7 days", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(12.dp))
            Surface(
                shape = SproutlyRadii.card,
                color = MaterialTheme.colorScheme.surface,
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
                                        else MaterialTheme.colorScheme.surfaceContainerHigh,
                                    ),
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(day, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            Text("Top products", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(8.dp))
            if (stats.topProducts.isEmpty()) {
                Text(
                    "No sales recorded yet.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                stats.topProducts.forEach { product ->
                    Surface(
                        shape = SproutlyRadii.card,
                        color = MaterialTheme.colorScheme.surface,
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
                                    style = MaterialTheme.typography.titleSmall,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                )
                                Text(
                                    "${product.unitsSold} downloads",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Text(formatPrice(product.revenue), style = MaterialTheme.typography.titleSmall)
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
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier,
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(value, style = MaterialTheme.typography.headlineMedium)
            Text(
                label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
        color = if (emphasis) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(
                        if (emphasis) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceContainerHigh,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                TablerIcon(
                    icon = icon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = if (emphasis) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                label,
                style = MaterialTheme.typography.titleSmall,
                color = if (emphasis) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSurface,
            )
            Text(
                detail,
                style = MaterialTheme.typography.bodySmall,
                color = if (emphasis) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** The developer console's tabs. */
private enum class SellerTab(val label: String) {
    OVERVIEW("Overview"),
    OFFERS("Offers"),
}
