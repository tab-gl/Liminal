package app.tab.store.feature.admin

import androidx.annotation.DrawableRes

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.tab.store.core.designsystem.TablerIcons
import app.tab.store.core.designsystem.TablerIcon
import app.tab.store.core.designsystem.Avatar
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
import app.tab.store.domain.model.ProductStatus
import app.tab.store.domain.model.StoreStatus
import app.tab.store.domain.model.UserRole

@Composable
fun AdminDashboardScreen(
    onBack: () -> Unit,
    onStores: () -> Unit,
    onUsers: () -> Unit,
    onProducts: () -> Unit,
    onCategories: () -> Unit,
    viewModel: AdminDashboardViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        SproutlyTopBar(title = "Admin console", subtitle = "Platform overview", onBack = onBack)
        Column(Modifier.padding(horizontal = 20.dp)) {
            Surface(
                shape = SproutlyRadii.hero,
                color = SproutlyPalette.Espresso,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text(
                        "TOTAL APP SALES",
                        style = MaterialTheme.typography.labelSmall,
                        color = SproutlyPalette.LimeSoft,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        formatCompact(state.overview.grossMerchandiseValue / 100),
                        style = MaterialTheme.typography.displaySmall,
                        color = SproutlyPalette.Lime,
                    )
                    Spacer(Modifier.height(14.dp))
                    Row {
                        AdminStat("Installs", state.overview.orderCount.toString(), Modifier.weight(1f))
                        AdminStat("Apps", state.overview.productCount.toString(), Modifier.weight(1f))
                        AdminStat("Developers", state.overview.storeCount.toString(), Modifier.weight(1f))
                    }
                }
            }

            // What actually needs a decision, surfaced first.
            val pendingCount = state.pendingStores.size
            if (pendingCount > 0 || state.overview.blockedProducts > 0) {
                Surface(
                    shape = SproutlyRadii.card,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            "Needs attention",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        Spacer(Modifier.height(6.dp))
                        if (pendingCount > 0) {
                            Text(
                                "$pendingCount store${if (pendingCount == 1) "" else "s"} waiting for approval",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }
                        if (state.overview.blockedProducts > 0) {
                            Text(
                                "${state.overview.blockedProducts} app${if (state.overview.blockedProducts == 1) "" else "s"} currently blocked",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricBox("Users", state.overview.userCount.toString(), Modifier.weight(1f))
                MetricBox("Developers", state.overview.sellerCount.toString(), Modifier.weight(1f))
                MetricBox(
                    "Pending",
                    state.overview.pendingStores.toString(),
                    Modifier.weight(1f),
                    highlight = state.overview.pendingStores > 0,
                )
            }

            Spacer(Modifier.height(20.dp))
            Text("Moderation", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(10.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AdminTile(
                    icon = TablerIcons.Store,
                    label = "Developers",
                    count = state.overview.sellerCount.toString(),
                    detail = if (state.overview.pendingStores > 0) {
                        "${state.overview.pendingStores} awaiting approval"
                    } else {
                        "All reviewed"
                    },
                    emphasise = state.overview.pendingStores > 0,
                    modifier = Modifier.weight(1f),
                    onClick = onStores,
                )
                AdminTile(
                    icon = TablerIcons.User,
                    label = "Users",
                    count = state.overview.userCount.toString(),
                    detail = "Roles and access",
                    modifier = Modifier.weight(1f),
                    onClick = onUsers,
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AdminTile(
                    icon = TablerIcons.Package,
                    label = "Reported apps",
                    count = state.overview.blockedProducts.toString(),
                    detail = if (state.overview.blockedProducts > 0) "Needs a decision" else "Nothing flagged",
                    emphasise = state.overview.blockedProducts > 0,
                    modifier = Modifier.weight(1f),
                    onClick = onProducts,
                )
                AdminTile(
                    icon = TablerIcons.Grid,
                    label = "Categories",
                    count = "",
                    detail = "App categories",
                    modifier = Modifier.weight(1f),
                    onClick = onCategories,
                )
            }

            if (state.pendingStores.isNotEmpty()) {
                Spacer(Modifier.height(20.dp))
                Text("Awaiting approval", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(8.dp))
                state.pendingStores.take(3).forEach { store ->
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
                                url = store.logoUrl,
                                contentDescription = store.name,
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape),
                                corner = 20.dp,
                            )
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(store.name, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    store.city.ifBlank { "India" },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            TextButton(onClick = { viewModel.approve(store) }) { Text("Approve") }
                        }
                    }
                }
            }
            Spacer(Modifier.height(28.dp))
        }
    }
}

@Composable
private fun AdminStat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(value, style = MaterialTheme.typography.titleLarge, color = SproutlyPalette.Lime)
        Text(label, style = MaterialTheme.typography.bodySmall, color = SproutlyPalette.LimeSoft)
    }
}

@Composable
private fun MetricBox(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    highlight: Boolean = false,
) {
    Surface(
        shape = SproutlyRadii.card,
        color = if (highlight) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surface,
        modifier = modifier,
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(value, style = MaterialTheme.typography.headlineSmall)
            Text(
                label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun AdminRow(@DrawableRes icon: Int, title: String, subtitle: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = SproutlyRadii.card,
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                TablerIcon(icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(19.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TablerIcon(
                TablerIcons.ArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun AdminStoresScreen(
    onBack: () -> Unit,
    viewModel: AdminStoresViewModel = hiltViewModel(),
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
        Column(Modifier.fillMaxSize()) {
            SproutlyTopBar(
                title = "Developers",
                subtitle = "${state.pending.size} pending · ${state.stores.size} developers",
                onBack = onBack,
            )
            if (!state.isLoading && state.stores.isEmpty()) {
                EmptyState(title = "No developers", message = "No developer has published yet.")
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(state.stores, key = { it.id }) { store ->
                        Surface(
                            shape = SproutlyRadii.card,
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    SproutlyImage(
                                        url = store.logoUrl,
                                        contentDescription = store.name,
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CircleShape),
                                        corner = 22.dp,
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(store.name, style = MaterialTheme.typography.titleSmall)
                                        Text(
                                            "${store.name.ifBlank { "India" }} · ${store.productCount} apps",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    SproutlyTag(
                                        text = store.status.name,
                                        container = when (store.status) {
                                            StoreStatus.ACTIVE -> MaterialTheme.colorScheme.primaryContainer
                                            StoreStatus.PENDING -> MaterialTheme.colorScheme.tertiaryContainer
                                            StoreStatus.SUSPENDED -> MaterialTheme.colorScheme.errorContainer
                                        },
                                        content = when (store.status) {
                                            StoreStatus.ACTIVE -> MaterialTheme.colorScheme.onPrimaryContainer
                                            StoreStatus.PENDING -> MaterialTheme.colorScheme.onTertiaryContainer
                                            StoreStatus.SUSPENDED -> MaterialTheme.colorScheme.onErrorContainer
                                        },
                                    )
                                }
                                Spacer(Modifier.height(6.dp))
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                Row {
                                    if (store.status != StoreStatus.ACTIVE) {
                                        TextButton(
                                            onClick = { viewModel.setStatus(store, StoreStatus.ACTIVE) },
                                        ) {
                                            TablerIcon(TablerIcons.Tick, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text("Approve")
                                        }
                                    }
                                    // The store's own studio: everything it publishes
                                    // carries the Official badge.
                                    TextButton(onClick = { viewModel.setOfficial(store, !store.official) }) {
                                        TablerIcon(
                                            TablerIcons.Verified,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text(if (store.official) "Unmark official" else "Mark official")
                                    }
                                    if (store.status != StoreStatus.SUSPENDED) {
                                        TextButton(
                                            onClick = { viewModel.setStatus(store, StoreStatus.SUSPENDED) },
                                        ) {
                                            Text("Suspend", color = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    item { Spacer(Modifier.height(20.dp)) }
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

@Composable
fun AdminUsersScreen(
    onBack: () -> Unit,
    viewModel: AdminUsersViewModel = hiltViewModel(),
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
        Column(Modifier.fillMaxSize()) {
            SproutlyTopBar(
                title = "Users",
                subtitle = "${state.users.size} accounts",
                onBack = onBack,
            )
            var query by remember { mutableStateOf("") }
            SproutlyTextField(
                value = query,
                onValueChange = { query = it },
                label = "Search people",
                placeholder = "Name or email",
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
            )
            val visible = state.users.filter {
                query.isBlank() ||
                    it.displayName.contains(query, ignoreCase = true) ||
                    it.email.contains(query, ignoreCase = true)
            }

            LazyColumn(
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(visible, key = { it.uid }) { user ->
                    Surface(
                        shape = SproutlyRadii.card,
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Avatar(url = user.photoUrl, name = user.displayName, size = 40.dp)
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        user.displayName.ifBlank { "Unnamed" },
                                        style = MaterialTheme.typography.titleSmall,
                                    )
                                    Text(
                                        user.email,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                SproutlyTag(text = user.role.name)
                            }
                            Spacer(Modifier.height(4.dp))
                            Row {
                                UserRole.entries.forEach { role ->
                                    val canAssign = role != UserRole.SELLER || !user.storeId.isNullOrBlank()
                                    if (role != user.role && canAssign) {
                                        TextButton(
                                            onClick = { viewModel.setRole(user, role) },
                                            enabled = state.busyUid != user.uid,
                                        ) {
                                            Text(
                                                "Make ${role.label.lowercase()}",
                                                style = MaterialTheme.typography.labelMedium,
                                            )
                                        }
                                    }
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
fun AdminProductsScreen(
    onBack: () -> Unit,
    viewModel: AdminProductsViewModel = hiltViewModel(),
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
        Column(Modifier.fillMaxSize()) {
            SproutlyTopBar(
                title = "Reported apps",
                subtitle = "${state.reported.size} flagged",
                onBack = onBack,
            )
            if (!state.isLoading && state.reported.isEmpty()) {
                EmptyState(
                    title = "Nothing reported",
                    message = "Flagged apps will appear here for review.",
                    icon = TablerIcons.Verified,
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(state.reported, key = { it.id }) { product ->
                        Surface(
                            shape = SproutlyRadii.card,
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                SproutlyImage(
                                    url = product.primaryImage,
                                    contentDescription = product.title,
                                    modifier = Modifier.size(56.dp),
                                    corner = 12.dp,
                                )
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        product.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        maxLines = 2,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        "${product.storeName} · ${formatPrice(product.price)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                // Resolving clears the flag and tells the developer
                                // either way, so the queue actually empties.
                                TextButton(onClick = { viewModel.resolve(product, keepListed = true) }) {
                                    Text("Keep")
                                }
                                TextButton(
                                    onClick = { viewModel.resolve(product, keepListed = false) },
                                ) {
                                    Text("Remove", color = MaterialTheme.colorScheme.error)
                                }
                                TextButton(
                                    onClick = { viewModel.setStatus(product, ProductStatus.ACTIVE) },
                                ) {
                                    Text("Restore")
                                }
                            }
                        }
                    }
                    item { Spacer(Modifier.height(20.dp)) }
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

@Composable
fun AdminCategoriesScreen(
    onBack: () -> Unit,
    viewModel: AdminCategoriesViewModel = hiltViewModel(),
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            SproutlyTopBar(title = "Categories", onBack = onBack)
            Text(
                "Used for home rails and search filters.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))

            // The standard taxonomy in one tap. This exists because many Google
            // Cloud organisations block service-account key creation, which
            // makes tools/seed impossible to run against such a project.
            Surface(
                shape = SproutlyRadii.card,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        "Start from the standard set",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Adds the eight app-store categories: Productivity, Games, " +
                            "Photography, Finance, Education, Health & Fitness, Social, Tools.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Spacer(Modifier.height(12.dp))
                    SproutlyButton(
                        text = "Add 8 categories",
                        onClick = viewModel::seedDefaults,
                        loading = state.isSaving,
                    )
                }
            }
            Spacer(Modifier.height(20.dp))
            Text(
                "Or add one yourself",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            SproutlyTextField(
                value = state.name,
                onValueChange = viewModel::onName,
                label = "Category name",
                placeholder = "Productivity",
            )
            SproutlyTextField(
                value = state.slug,
                onValueChange = viewModel::onSlug,
                label = "Slug",
                helperText = "Used in links and search.",
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
                text = "Create category",
                onClick = viewModel::create,
                enabled = state.canSave,
                loading = state.isSaving,
            )
            Spacer(Modifier.height(28.dp))
        }
        SnackbarHost(
            hostState = snackbar,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp),
        )
    }
}

/** One bento tile of the admin console: a count, a name, and what it needs. */
@Composable
private fun AdminTile(
    @DrawableRes icon: Int,
    label: String,
    count: String,
    detail: String,
    modifier: Modifier = Modifier,
    emphasise: Boolean = false,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = SproutlyRadii.card,
        color = if (emphasise) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surface,
        modifier = modifier,
    ) {
        Column(Modifier.padding(14.dp)) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(
                        if (emphasise) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.primaryContainer,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                TablerIcon(
                    icon = icon,
                    contentDescription = null,
                    modifier = Modifier.size(17.dp),
                    tint = if (emphasise) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            if (count.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                Text(count, style = MaterialTheme.typography.headlineSmall)
            } else {
                Spacer(Modifier.height(10.dp))
            }
            Text(label, style = MaterialTheme.typography.titleSmall)
            Text(
                detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
            )
        }
    }
}
