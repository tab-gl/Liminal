@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package app.tab.store.feature.seller

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.tab.store.core.designsystem.TablerIcons
import app.tab.store.core.designsystem.TablerIcon
import app.tab.store.core.designsystem.EmptyState
import app.tab.store.core.designsystem.QuantityStepper
import app.tab.store.core.designsystem.RiskMeter
import app.tab.store.core.designsystem.SproutlyButton
import app.tab.store.core.designsystem.SproutlyChip
import app.tab.store.core.designsystem.SproutlyImage
import app.tab.store.core.designsystem.SproutlyRadii
import app.tab.store.core.designsystem.SproutlyTag
import app.tab.store.core.designsystem.SproutlyTextField
import app.tab.store.core.designsystem.SproutlyTopBar
import app.tab.store.core.designsystem.UploadProgressBar
import app.tab.store.core.designsystem.formatPrice
import app.tab.store.core.utils.pickImageRequest
import app.tab.store.core.utils.APK_MIME_TYPES
import app.tab.store.core.utils.rememberApkPicker
import app.tab.store.core.utils.rememberImagePicker
import app.tab.store.core.utils.Money
import app.tab.store.domain.model.Product
import app.tab.store.domain.model.ProductStatus

@Composable
fun SellerProductsScreen(
    onBack: () -> Unit,
    onEdit: (String?) -> Unit,
    viewModel: SellerProductsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var confirmDelete by remember { mutableStateOf<Product?>(null) }
    var offerFor by remember { mutableStateOf<Product?>(null) }

    offerFor?.let { target ->
        SellerOfferDialog(
            product = target,
            onDismiss = { offerFor = null },
            onSave = { compareAt, days ->
                offerFor = null
                viewModel.setOffer(target, compareAt, days)
            },
        )
    }

    confirmDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            title = { Text("Delete ${target.title}?") },
            text = {
                Text(
                    "This removes the listing and its screenshots. Installs already " +
                        "made stay in people's libraries, and the app stops appearing in the store.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = null
                    viewModel.delete(target)
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = null }) { Text("Keep it") }
            },
        )
    }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbar.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            SproutlyTopBar(
                title = "Apps",
                subtitle = "${state.products.size} apps",
                onBack = onBack,
                actions = {
                    TextButton(onClick = { onEdit(null) }) {
                        Text("Add", style = MaterialTheme.typography.labelLarge)
                    }
                },
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ProductFilter.entries.forEach { filter ->
                    SproutlyChip(
                        text = when (filter) {
                            ProductFilter.ALL -> "All"
                            ProductFilter.LIVE -> "Live"
                            ProductFilter.DRAFT -> "Drafts"
                            ProductFilter.HIDDEN -> "Not live"
                        },
                        selected = state.filter == filter,
                        onClick = { viewModel.setFilter(filter) },
                    )
                }
            }

            if (!state.isLoading && state.visible.isEmpty()) {
                EmptyState(
                    title = "No apps here",
                    message = "Publish your first app and it will appear in the store.",
                    icon = TablerIcons.Package,
                    actionLabel = "Publish app",
                    onAction = { onEdit(null) },
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(state.visible, key = { it.id }) { product ->
                        Surface(
                            onClick = { onEdit(product.id) },
                            shape = SproutlyRadii.card,
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    SproutlyImage(
                                        url = product.primaryImage,
                                        contentDescription = product.title,
                                        modifier = Modifier.size(64.dp),
                                        corner = 14.dp,
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
                                            formatPrice(product.price),
                                            style = MaterialTheme.typography.titleMedium,
                                        )
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            SproutlyTag(
                                                text = product.status.name,
                                                container = if (product.status == ProductStatus.ACTIVE) {
                                                    MaterialTheme.colorScheme.primaryContainer
                                                } else {
                                                    MaterialTheme.colorScheme.surfaceContainerHigh
                                                },
                                                content = if (product.status == ProductStatus.ACTIVE) {
                                                    MaterialTheme.colorScheme.onPrimaryContainer
                                                } else {
                                                    MaterialTheme.colorScheme.onSurfaceVariant
                                                },
                                            )
                                            Spacer(Modifier.width(6.dp))
                                            Text(
                                                "${product.unitsSold} installs",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                    }
                                    IconButton(
                                        onClick = {
                                            // Unpublishing keeps the listing and its
                                            // reviews; it just stops being shown.
                                            if (product.status == ProductStatus.ACTIVE) {
                                                viewModel.unpublish(product)
                                            } else {
                                                viewModel.republish(product)
                                            }
                                        },
                                    ) {
                                        TablerIcon(
                                            if (product.status == ProductStatus.ACTIVE) {
                                                TablerIcons.Close
                                            } else {
                                                TablerIcons.Tick
                                            },
                                            contentDescription = if (product.status == ProductStatus.ACTIVE) {
                                                "Unpublish"
                                            } else {
                                                "Publish"
                                            },
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(19.dp),
                                        )
                                    }
                                    IconButton(onClick = { offerFor = product }) {
                                        TablerIcon(
                                            TablerIcons.Tag,
                                            contentDescription = "Offer",
                                            tint = if (product.hasActiveOffer) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                            },
                                            modifier = Modifier.size(19.dp),
                                        )
                                    }
                                    IconButton(onClick = { confirmDelete = product }) {
                                        TablerIcon(TablerIcons.Trash,
                                            contentDescription = "Delete",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(19.dp),
                                        )
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
fun SellerProductEditScreen(
    productId: String?,
    onBack: () -> Unit,
    viewModel: SellerProductEditViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val picker = rememberImagePicker { uri -> if (uri != null) viewModel.onImagePicked(uri) }
    val screenshotPicker = rememberImagePicker { uri ->
        if (uri != null) viewModel.onScreenshotPicked(uri)
    }

    LaunchedEffect(state.saved) {
        if (state.saved) onBack()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding(),
    ) {
        SproutlyTopBar(
            title = if (productId == null) "New app" else "Edit app",
            onBack = onBack,
        )
        Column(Modifier.padding(horizontal = 20.dp)) {
            // Images — the icon tile sits first, as the anchor of the grid.
            BentoTile(title = "Icon", hint = "What people see in the store.") {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.images) { image ->
                    Box {
                        SproutlyImage(
                            url = image.url,
                            contentDescription = null,
                            modifier = Modifier.size(96.dp),
                            corner = 16.dp,
                        )
                        Surface(
                            onClick = { viewModel.removeImage(image.publicId) },
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(3.dp)
                                .size(22.dp),
                        ) {
                            TablerIcon(TablerIcons.Close,
                                contentDescription = "Remove image",
                                modifier = Modifier.padding(4.dp),
                            )
                        }
                    }
                }
                item {
                    Surface(
                        onClick = { picker.launch(pickImageRequest()) },
                        shape = SproutlyRadii.card,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier.size(96.dp),
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            TablerIcon(TablerIcons.Plus, contentDescription = "Add photo")
                            Text("Photo", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
            if (state.isUploading) {
                Spacer(Modifier.height(10.dp))
                UploadProgressBar(state.uploadProgress)
            }
            }

            Spacer(Modifier.height(12.dp))
            BentoTile(title = "The listing", hint = "Name and description as people will read them.") {
            SproutlyTextField(
                value = state.title,
                onValueChange = viewModel::onTitle,
                label = "App name",
                placeholder = "Notes, Focus, Weather…",
            )
            SproutlyTextField(
                value = state.description,
                onValueChange = viewModel::onDescription,
                label = "Description",
                singleLine = false,
                minLines = 4,
                helperText = "What it does, what is included, any requirements.",
            )

            }
            Spacer(Modifier.height(12.dp))
            BentoTile(title = "Category", hint = "Where people will find it.") {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.categories) { category ->
                    SproutlyChip(
                        text = category.name,
                        selected = state.categoryId == category.id,
                        onClick = { viewModel.onCategory(category) },
                    )
                }
            }
            }

            Spacer(Modifier.height(12.dp))

            // -------------------------------------------------------- screenshots
            BentoTile(
                title = "Screenshots",
                hint = "Up to 8. These are what people see before installing.",
            ) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.screenshots) { shot ->
                    Box {
                        SproutlyImage(
                            url = shot.url,
                            contentDescription = null,
                            modifier = Modifier.size(width = 108.dp, height = 190.dp),
                            corner = 14.dp,
                        )
                        Surface(
                            onClick = { viewModel.removeScreenshot(shot.publicId) },
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(3.dp)
                                .size(22.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                TablerIcon(
                                    TablerIcons.Close,
                                    contentDescription = "Remove screenshot",
                                    modifier = Modifier.size(13.dp),
                                )
                            }
                        }
                    }
                }
                item {
                    Surface(
                        onClick = { screenshotPicker.launch(pickImageRequest()) },
                        shape = SproutlyRadii.card,
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        modifier = Modifier.size(width = 108.dp, height = 190.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (state.isUploadingScreenshot) {
                                LoadingIndicator(modifier = Modifier.size(24.dp))
                            } else {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    TablerIcon(
                                        TablerIcons.Plus,
                                        contentDescription = "Add screenshot",
                                        modifier = Modifier.size(20.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        "${state.screenshots.size}/8",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
            }
            }


            // ---------------------------------------------------------- pricing
            BentoTile(
                title = "Pricing",
                hint = "Free apps install straight away; paid apps need a verified account.",
            ) {
            PricingDropdown(
                isFree = state.isFree,
                onChange = viewModel::onFreeChanged,
            )
            if (!state.isFree) {
                Spacer(Modifier.height(10.dp))
                Row {
                    Box(Modifier.weight(1f)) {
                        SproutlyTextField(
                            value = state.priceRupees,
                            onValueChange = viewModel::onPrice,
                            label = "Price",
                            prefix = "₹ ",
                            keyboardType = KeyboardType.Decimal,
                            isError = state.priceError != null,
                            errorText = state.priceError,
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Box(Modifier.weight(1f)) {
                        SproutlyTextField(
                            value = state.compareAtRupees,
                            onValueChange = viewModel::onCompareAt,
                            label = "Original price",
                            prefix = "₹ ",
                            keyboardType = KeyboardType.Decimal,
                        )
                    }
                }
                // Declaring an offer is just: an original price above the price,
                // and optionally a date it ends. It shows on the Offers tab and
                // expires by itself.
                if (state.compareAtRupees.isNotBlank()) {
                    Spacer(Modifier.height(10.dp))
                    SproutlyTextField(
                        value = state.offerDays,
                        onValueChange = viewModel::onOfferDays,
                        label = "Offer ends in (days, optional)",
                        keyboardType = KeyboardType.Number,
                        helperText = "Leave blank to keep the offer running.",
                    )
                }
            }
            }

            Spacer(Modifier.height(12.dp))

            // ------------------------------------------------------ offer banner
            BentoTile(
                title = "Offer banner",
                hint = "Shown in the store while your offer runs. Tap it to open the app.",
            ) {
                val promoScope = rememberCoroutineScope()
                val promoPicker = rememberLauncherForActivityResult(
                    ActivityResultContracts.PickVisualMedia(),
                ) { uri ->
                    if (uri != null) {
                        promoScope.launch {
                            val bytes = withContext(Dispatchers.IO) {
                                context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                            }
                            if (bytes != null) viewModel.uploadPromoImage(bytes, "promo.jpg")
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        onClick = {
                            promoPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                            )
                        },
                        shape = SproutlyRadii.card,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier.size(88.dp),
                    ) {
                        if (state.promoImageUrl != null) {
                            SproutlyImage(
                                url = state.promoImageUrl,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                corner = 16.dp,
                            )
                        } else {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                            ) {
                                TablerIcon(TablerIcons.Image, contentDescription = "Add banner")
                                Text("Banner", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            if (state.promoImageUrl == null) "No banner yet"
                            else "Banner ready",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (state.promoImageUrl != null) {
                            TextButton(onClick = viewModel::clearPromoImage) {
                                Text("Remove")
                            }
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                SproutlyTextField(
                    value = state.promoHeadline,
                    onValueChange = viewModel::onPromoHeadline,
                    label = "Headline",
                    placeholder = "50% off this week",
                )
                SproutlyTextField(
                    value = state.promoBody,
                    onValueChange = viewModel::onPromoBody,
                    label = "Line under it",
                    placeholder = "One line that says why it is worth a look.",
                    singleLine = false,
                    minLines = 2,
                )
            }

            Spacer(Modifier.height(12.dp))

            // ------------------------------------------------------ distribution
            BentoTile(
                title = "The build",
                hint = if (state.apkUploadEnabled) {
                    "Upload the APK (up to 100 MB) or point at a link you control."
                } else {
                    "Uploads are off in this build — paste a direct link instead."
                },
            ) {
            ApkSourcePicker(
                apkUrl = state.apkUrl,
                fileName = state.apkFileName,
                sizeBytes = state.apkSizeBytes,
                isUploading = state.isUploadingApk,
                progress = state.uploadProgress,
                link = state.apkLink,
                onLinkChange = viewModel::onApkLink,
                onPicked = viewModel::onApkPicked,
                onClear = viewModel::clearApk,
            )
            }

            if (state.scanRisk.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                RiskMeter(
                    score = state.scanScore,
                    reasons = state.scanWarnings,
                )
            }

            Spacer(Modifier.height(12.dp))

            // ---------------------------------------------------------- release
            Text("Release", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(4.dp))
            Text(
                if (productId == null) "The version you are publishing."
                else "Bump the version and replace the build to ship an update.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(10.dp))
            Row {
                Box(Modifier.weight(1f)) {
                    SproutlyTextField(
                        value = state.versionName,
                        onValueChange = viewModel::onVersionName,
                        label = "Version name",
                        placeholder = "1.0.0",
                    )
                }
                Spacer(Modifier.width(10.dp))
                Box(Modifier.weight(1f)) {
                    SproutlyTextField(
                        value = state.versionCode,
                        onValueChange = viewModel::onVersionCode,
                        label = "Version code",
                        placeholder = "1",
                        keyboardType = KeyboardType.Number,
                    )
                }
            }
            SproutlyTextField(
                value = state.releaseNotes,
                onValueChange = viewModel::onReleaseNotes,
                label = "What's new",
                singleLine = false,
                minLines = 3,
                helperText = "Shown to people deciding whether to update.",
            )
            Spacer(Modifier.height(18.dp))

            // ------------------------------------------------------ verification
            if (state.needsVerification) {
                VerificationGate(
                    status = state.verificationStatus,
                    isRequesting = state.isRequestingVerification,
                    onRequest = viewModel::requestVerification,
                )
                Spacer(Modifier.height(14.dp))
            }

            SproutlyTextField(
                value = state.tags,
                onValueChange = viewModel::onTags,
                label = "Tags (comma separated)",
                placeholder = "productivity, notes, offline",
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                SproutlyChip(
                    text = "Save as draft",
                    selected = state.status == ProductStatus.DRAFT,
                    onClick = { viewModel.setStatus(ProductStatus.DRAFT) },
                )
                Spacer(Modifier.width(8.dp))
                SproutlyChip(
                    text = "Publish now",
                    selected = state.status == ProductStatus.ACTIVE,
                    // Going live needs an approved store; the rules enforce it, so
                    // the control says so rather than failing at the end.
                    onClick = { viewModel.setStatus(ProductStatus.ACTIVE) },
                )
            }

            if (state.status == ProductStatus.ACTIVE && state.paidNeedsVerification) {
                Spacer(Modifier.height(10.dp))
                Surface(
                    shape = SproutlyRadii.card,
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Text(
                            "Paid apps need a verified profile",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Ask for verification from your developer profile, or set the " +
                                "price to 0 to publish it free.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                        )
                    }
                }
            }

            if (state.status == ProductStatus.ACTIVE && !state.canPublish) {
                Spacer(Modifier.height(10.dp))
                Surface(
                    shape = SproutlyRadii.card,
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Text(
                            "Your developer profile is still in review",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Listings can go live once an admin approves your profile. " +
                                "Save it as a draft meanwhile — nothing is lost.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                        )
                    }
                }
            }
            Spacer(Modifier.height(18.dp))

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
                text = when {
                    state.status == ProductStatus.ACTIVE && !state.canPublish -> "Waiting for approval"
                    state.status == ProductStatus.ACTIVE && state.paidNeedsVerification -> "Verification needed"
                    productId == null -> "Publish app"
                    else -> "Save app"
                },
                onClick = viewModel::save,
                enabled = state.canSave &&
                    (state.status != ProductStatus.ACTIVE || state.canGoLive),
                loading = state.isSaving,
            )
            Spacer(Modifier.height(32.dp))
        }
    }
}

// ---------------------------------------------------------------------------
// Listing helpers
// ---------------------------------------------------------------------------

/** Free or paid, as a dropdown rather than two radio buttons. */
@Composable
private fun PricingDropdown(isFree: Boolean, onChange: (Boolean) -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        Surface(
            onClick = { expanded = true },
            shape = SproutlyRadii.card,
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TablerIcon(
                    icon = if (isFree) TablerIcons.Sparkles else TablerIcons.Bag,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        if (isFree) "Free" else "Paid",
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        if (isFree) "Anyone can install it at no cost"
                        else "Installs are charged at the price you set",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                TablerIcon(
                    icon = TablerIcons.ChevronDown,
                    contentDescription = "Change pricing",
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("Free") },
                onClick = { onChange(true); expanded = false },
            )
            DropdownMenuItem(
                text = { Text("Paid") },
                onClick = { onChange(false); expanded = false },
            )
        }
    }
}

/** Upload an APK, or point at a link that already hosts the build. */
@Composable
private fun ApkSourcePicker(
    apkUrl: String?,
    fileName: String,
    sizeBytes: Long?,
    isUploading: Boolean,
    progress: Float,
    link: String,
    onLinkChange: (String) -> Unit,
    onPicked: (ByteArray, String) -> Unit,
    onClear: () -> Unit,
) {
    var mode by remember { mutableStateOf(if (apkUrl != null && link.isBlank()) 0 else if (link.isNotBlank()) 1 else 0) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val picker = rememberApkPicker { uri ->
        if (uri != null) {
            scope.launch {
                val bytes = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                }
                if (bytes != null) {
                    val name = uri.lastPathSegment?.substringAfterLast('/') ?: "app.apk"
                    onPicked(bytes, name)
                }
            }
        }
    }

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        SproutlyChip(
            text = "Upload APK",
            selected = mode == 0,
            onClick = { mode = 0 },
        )
        SproutlyChip(
            text = "Paste link",
            selected = mode == 1,
            onClick = { mode = 1 },
        )
    }
    Spacer(Modifier.height(12.dp))

    if (mode == 0) {
        when {
            isUploading -> {
                Surface(
                    shape = SproutlyRadii.card,
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Uploading build…", style = MaterialTheme.typography.titleSmall)
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
            apkUrl != null -> {
                Surface(
                    shape = SproutlyRadii.card,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TablerIcon(
                            icon = TablerIcons.Verified,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                fileName.ifBlank { "Build attached" },
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                maxLines = 1,
                            )
                            if (sizeBytes != null) {
                                Text(
                                    formatBytes(sizeBytes),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                            }
                        }
                        Surface(
                            onClick = onClear,
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.size(28.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                TablerIcon(
                                    icon = TablerIcons.Close,
                                    contentDescription = "Remove build",
                                    modifier = Modifier.size(14.dp),
                                )
                            }
                        }
                    }
                }
            }
            else -> {
                SproutlyButton(
                    text = "Choose APK",
                    icon = TablerIcons.Upload,
                    onClick = { picker.launch(APK_MIME_TYPES) },
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "Uploaded to Sproutly storage (Cloudflare R2) through our uploader, up to 100 MB.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    } else {
        SproutlyTextField(
            value = link,
            onValueChange = onLinkChange,
            label = "Build link",
            placeholder = "https://example.com/app-release.apk",
            keyboardType = KeyboardType.Uri,
            helperText = "A direct https link you control. Keep it live — installs use it.",
        )
    }
}

/** Paid listings need a verified developer account. */
@Composable
private fun VerificationGate(
    status: String,
    isRequesting: Boolean,
    onRequest: () -> Unit,
) {
    Surface(
        shape = SproutlyRadii.card,
        color = MaterialTheme.colorScheme.errorContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TablerIcon(
                    icon = TablerIcons.Verified,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Verification needed for paid apps",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                if (status == "PENDING") {
                    "Your request is with our team. You can publish this app for free in the meantime."
                } else {
                    "Paid apps can only be published by verified developers, so buyers know who they are paying. Free apps need no verification."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            if (status != "PENDING") {
                Spacer(Modifier.height(12.dp))
                SproutlyButton(
                    text = "Request verification",
                    onClick = onRequest,
                    loading = isRequesting,
                )
            }
        }
    }
}

private fun formatBytes(bytes: Long): String = when {
    bytes >= 1024L * 1024L -> "%.1f MB".format(bytes / (1024.0 * 1024.0))
    bytes >= 1024L -> "%.0f KB".format(bytes / 1024.0)
    else -> "$bytes B"
}

/**
 * One tile of the publication bento.
 *
 * The form is a grid of cards rather than a column of fields: identity and build
 * are wide, pricing and release sit side by side. A tile is just a surface with
 * a label, so the same block can be dropped anywhere in the grid.
 */
@Composable
private fun BentoTile(
    title: String? = null,
    hint: String? = null,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        shape = SproutlyRadii.card,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(14.dp)) {
            if (title != null) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                if (hint != null) {
                    Text(
                        hint,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(10.dp))
            }
            content()
        }
    }
}

/**
 * Puts an app on offer, or takes it off.
 *
 * The price stays what it is; the "was" price is what makes the discount real, so
 * it has to be higher than what people actually pay.
 */
@Composable
internal fun SellerOfferDialog(
    product: Product,
    onDismiss: () -> Unit,
    onSave: (compareAtPaise: Long?, days: Int?) -> Unit,
) {
    val current = product.compareAtPrice?.let { Money.toRupeeString(it) }.orEmpty()
    var was by remember { mutableStateOf(current) }
    var days by remember {
        mutableStateOf(
            product.offerEndsAt
                ?.let { ((it - System.currentTimeMillis()) / 86_400_000L).coerceAtLeast(0) }
                ?.toString()
                .orEmpty(),
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Offer for ${product.title}") },
        text = {
            Column {
                Text(
                    "People pay ${formatPrice(product.price)}. Enter the price it used to be " +
                        "to show a discount, and how long the offer runs.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                SproutlyTextField(
                    value = was,
                    onValueChange = { was = it.filter { c -> c.isDigit() }.take(7) },
                    label = "Was (₹)",
                    keyboardType = KeyboardType.Number,
                )
                SproutlyTextField(
                    value = days,
                    onValueChange = { days = it.filter { c -> c.isDigit() }.take(3) },
                    label = "Runs for (days)",
                    helperText = "Leave blank to keep it running.",
                    keyboardType = KeyboardType.Number,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(
                    was.toLongOrNull()?.let { it * 100 },
                    days.toIntOrNull(),
                )
            }) { Text("Save offer") }
        },
        dismissButton = {
            Row {
                if (product.compareAtPrice != null) {
                    TextButton(onClick = { onSave(null, null) }) { Text("Remove") }
                }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        },
    )
}

/**
 * Sprout Safe, for the developer: what their own build asks for, read from the
 * APK's manifest before it is published.
 */
@Composable
private fun SproutSafeCard(
    risk: String,
    permissions: List<String>,
    warnings: List<String>,
    packageName: String,
) {
    val colour = when (risk) {
        "HIGH" -> MaterialTheme.colorScheme.errorContainer
        "MEDIUM" -> MaterialTheme.colorScheme.secondaryContainer
        else -> MaterialTheme.colorScheme.primaryContainer
    }
    val onColour = when (risk) {
        "HIGH" -> MaterialTheme.colorScheme.onErrorContainer
        "MEDIUM" -> MaterialTheme.colorScheme.onSecondaryContainer
        else -> MaterialTheme.colorScheme.onPrimaryContainer
    }

    Surface(
        shape = SproutlyRadii.card,
        color = colour,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TablerIcon(
                    TablerIcons.Verified,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = onColour,
                )
                Spacer(Modifier.width(6.dp))
                Text("Sprout Safe", style = MaterialTheme.typography.titleSmall, color = onColour)
                Spacer(Modifier.weight(1f))
                Text(
                    when (risk) {
                        "HIGH" -> "High risk"
                        "MEDIUM" -> "Worth a look"
                        else -> "Looks fine"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = onColour,
                )
            }
            if (packageName.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = onColour,
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "${permissions.size} permission${if (permissions.size == 1) "" else "s"} requested",
                style = MaterialTheme.typography.bodySmall,
                color = onColour,
            )
            warnings.forEach { warning ->
                Spacer(Modifier.height(3.dp))
                Text("· $warning", style = MaterialTheme.typography.bodySmall, color = onColour)
            }
            if (warnings.isEmpty() && permissions.isNotEmpty()) {
                Spacer(Modifier.height(3.dp))
                Text(
                    "· None of them are unusual for an app.",
                    style = MaterialTheme.typography.bodySmall,
                    color = onColour,
                )
            }
        }
    }
}
