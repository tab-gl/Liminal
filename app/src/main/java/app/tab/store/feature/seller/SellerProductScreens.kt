@file:OptIn(app.tab.store.core.ui.ExperimentalMaterial3ExpressiveApi::class)

package app.tab.store.feature.seller

import app.tab.store.core.ui.Tokens
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
import app.tab.store.core.ui.HorizontalDivider
import app.tab.store.core.ui.IconButton
import app.tab.store.core.ui.LoadingIndicator
import app.tab.store.core.ui.AlertDialog
import androidx.compose.ui.unit.sp
import app.tab.store.core.ui.SnackbarHost
import app.tab.store.core.ui.SnackbarHostState
import app.tab.store.core.ui.Surface
import app.tab.store.core.ui.Text
import app.tab.store.core.ui.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import app.tab.store.core.ui.DropdownMenu
import app.tab.store.core.ui.DropdownMenuItem
import app.tab.store.core.ui.LinearProgressIndicator
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
import app.tab.store.core.designsystem.PhosphorIcons
import app.tab.store.core.designsystem.SproutlyIcon
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
            onSave = { compareAt, days, kind, code ->
                offerFor = null
                viewModel.setOffer(target, compareAt, days, kind, code)
            },
        )
    }

    confirmDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            title = { Text("Delete ${target.title}?") },
            text = {
                Text(
                    "This deletes the listing and its screenshots. " +
                        "Existing installs stay.",
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
                        Text("Add", style = Tokens.typography.labelLarge)
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
                    icon = PhosphorIcons.Package,
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
                            color = Tokens.colorScheme.surface,
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
                                            style = Tokens.typography.titleSmall,
                                            maxLines = 2,
                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                        )
                                        Text(
                                            formatPrice(product.price),
                                            style = Tokens.typography.titleMedium,
                                        )
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            SproutlyTag(
                                                text = product.status.name,
                                                container = if (product.status == ProductStatus.ACTIVE) {
                                                    Tokens.colorScheme.primaryContainer
                                                } else {
                                                    Tokens.colorScheme.surfaceContainerHigh
                                                },
                                                content = if (product.status == ProductStatus.ACTIVE) {
                                                    Tokens.colorScheme.onPrimaryContainer
                                                } else {
                                                    Tokens.colorScheme.onSurfaceVariant
                                                },
                                            )
                                            Spacer(Modifier.width(6.dp))
                                            Text(
                                                app.tab.store.core.utils.InstallCount.phrase(product.unitsSold),
                                                style = Tokens.typography.bodySmall,
                                                color = Tokens.colorScheme.onSurfaceVariant,
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
                                        SproutlyIcon(
                                            if (product.status == ProductStatus.ACTIVE) {
                                                PhosphorIcons.Close
                                            } else {
                                                PhosphorIcons.Tick
                                            },
                                            contentDescription = if (product.status == ProductStatus.ACTIVE) {
                                                "Unpublish"
                                            } else {
                                                "Publish"
                                            },
                                            tint = Tokens.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(19.dp),
                                        )
                                    }
                                    IconButton(onClick = { offerFor = product }) {
                                        SproutlyIcon(
                                            PhosphorIcons.Tag,
                                            contentDescription = "Offer",
                                            tint = if (product.hasActiveOffer) {
                                                Tokens.colorScheme.primary
                                            } else {
                                                Tokens.colorScheme.onSurfaceVariant
                                            },
                                            modifier = Modifier.size(19.dp),
                                        )
                                    }
                                    IconButton(onClick = { confirmDelete = product }) {
                                        SproutlyIcon(PhosphorIcons.Trash,
                                            contentDescription = "Delete",
                                            tint = Tokens.colorScheme.error,
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
            // EDITORIAL masthead: the number, then what this page is.
            Spacer(Modifier.height(8.dp))
            Text(
                if (productId == null) "01" else "02",
                style = Tokens.typography.displayMedium,
                color = Tokens.colorScheme.onSurface,
            )
            Text(
                if (productId == null) "PUBLISH A NEW APP" else "EDIT THIS LISTING",
                style = Tokens.typography.labelMedium,
                color = Tokens.colorScheme.onSurfaceVariant,
                letterSpacing = 1.6.sp,
            )
            Spacer(Modifier.height(10.dp))
            HorizontalDivider(thickness = 2.dp, color = Tokens.colorScheme.onSurface)
            Spacer(Modifier.height(18.dp))

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
                            color = Tokens.colorScheme.surface,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(3.dp)
                                .size(22.dp),
                        ) {
                            SproutlyIcon(PhosphorIcons.Close,
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
                        color = Tokens.colorScheme.surfaceContainerHigh,
                        modifier = Modifier.size(96.dp),
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            SproutlyIcon(PhosphorIcons.Plus, contentDescription = "Add photo")
                            Text("Photo", style = Tokens.typography.labelSmall)
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
                            color = Tokens.colorScheme.surface,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(3.dp)
                                .size(22.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                SproutlyIcon(
                                    PhosphorIcons.Close,
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
                        color = Tokens.colorScheme.surfaceContainerLow,
                        modifier = Modifier.size(width = 108.dp, height = 190.dp),
                    ) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            if (state.isUploadingScreenshot) {
                                LoadingIndicator(modifier = Modifier.size(24.dp))
                            } else {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    SproutlyIcon(
                                        PhosphorIcons.Plus,
                                        contentDescription = "Add screenshot",
                                        modifier = Modifier.size(20.dp),
                                        tint = Tokens.colorScheme.onSurfaceVariant,
                                    )
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        "${state.screenshots.size}/8",
                                        style = Tokens.typography.labelSmall,
                                        color = Tokens.colorScheme.onSurfaceVariant,
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
                hint = "Paid apps need a verified account.",
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

            // Offers are managed from the Offers tab in the console, not here.

            Spacer(Modifier.height(12.dp))

            // ------------------------------------------------------ distribution
            BentoTile(
                title = "The build",
                hint = if (state.apkUploadEnabled) {
                    "Upload the APK (100 MB max) or paste a link."
                } else {
                    "Uploads are off in this build. Paste a link."
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

            BentoTile(
                title = "Builds",
                hint = "Optional: a lite build, a pro build, or separate ABIs.",
            ) {
                state.flavours.forEachIndexed { index, flavour ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.weight(1f)) {
                            SproutlyTextField(
                                value = flavour.name,
                                onValueChange = { viewModel.updateFlavour(index, name = it) },
                                label = "Name",
                                placeholder = "Lite",
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        TextButton(onClick = { viewModel.removeFlavour(index) }) { Text("Remove") }
                    }
                    FlavourBuildRow(
                        flavour = flavour,
                        uploading = state.uploadingFlavour == index,
                        progress = state.flavourProgress,
                        uploadEnabled = state.apkUploadEnabled,
                        onLink = { viewModel.updateFlavour(index, url = it) },
                        onPicked = { bytes, name -> viewModel.onFlavourApkPicked(index, bytes, name) },
                    )
                    Spacer(Modifier.height(8.dp))
                }
                state.flavourError?.let {
                    Text(it, style = Tokens.typography.bodySmall, color = Tokens.colorScheme.error)
                }
                if (state.flavours.size < 4) {
                    TextButton(onClick = viewModel::addFlavour) { Text("Add another build") }
                }
            }

            Spacer(Modifier.height(12.dp))

            // ---------------------------------------------------------- release
            Text("Release", style = Tokens.typography.titleSmall)
            Spacer(Modifier.height(4.dp))
            Text(
                if (productId == null) "The version you are publishing."
                else "Raise the version and replace the build.",
                style = Tokens.typography.bodySmall,
                color = Tokens.colorScheme.onSurfaceVariant,
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

            Spacer(Modifier.height(12.dp))

            Text(
                text = when {
                    state.isSaving && state.status == ProductStatus.DRAFT -> "Saving draft…"
                    state.productId != null && state.status == ProductStatus.DRAFT -> "Draft saved automatically"
                    state.status == ProductStatus.DRAFT -> "Draft saves automatically as you edit"
                    else -> "Changes to a live app are saved when you publish them"
                },
                style = Tokens.typography.bodySmall,
                color = Tokens.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp),
            )
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
            color = Tokens.colorScheme.surfaceContainerLow,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SproutlyIcon(
                    icon = if (isFree) PhosphorIcons.Sparkles else PhosphorIcons.Bag,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = Tokens.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        if (isFree) "Free" else "Paid",
                        style = Tokens.typography.titleSmall,
                    )
                    Text(
                        if (isFree) "Anyone can install it at no cost"
                        else "Installs are charged at the price you set",
                        style = Tokens.typography.bodySmall,
                        color = Tokens.colorScheme.onSurfaceVariant,
                    )
                }
                SproutlyIcon(
                    icon = PhosphorIcons.ChevronDown,
                    contentDescription = "Change pricing",
                    modifier = Modifier.size(20.dp),
                    tint = Tokens.colorScheme.onSurfaceVariant,
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

/** One extra build: upload an APK or paste a direct link. */
@Composable
private fun FlavourBuildRow(
    flavour: app.tab.store.domain.model.Flavour,
    uploading: Boolean,
    progress: Float,
    uploadEnabled: Boolean,
    onLink: (String) -> Unit,
    onPicked: (ByteArray, String) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val picker = rememberApkPicker { uri ->
        if (uri != null) {
            scope.launch {
                val bytes = withContext(Dispatchers.IO) { readPickedBuild(context, uri) }
                if (bytes != null) {
                    onPicked(bytes, uri.lastPathSegment?.substringAfterLast('/') ?: "app.apk")
                }
            }
        }
    }
    SproutlyTextField(
        value = flavour.apkUrl,
        onValueChange = onLink,
        label = "Build link",
        placeholder = "https://…",
        keyboardType = KeyboardType.Uri,
    )
    if (uploading) {
        Spacer(Modifier.height(6.dp))
        LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
    } else if (uploadEnabled) {
        TextButton(onClick = { picker.launch(APK_MIME_TYPES) }) {
            Text(if (flavour.sizeBytes > 0) "Replace APK · ${formatBytes(flavour.sizeBytes)}" else "Upload APK instead")
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
                val bytes = withContext(Dispatchers.IO) { readPickedBuild(context, uri) }
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
                    color = Tokens.colorScheme.surfaceContainerLow,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Uploading build…", style = Tokens.typography.titleSmall)
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
                    color = Tokens.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        SproutlyIcon(
                            icon = PhosphorIcons.Verified,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = Tokens.colorScheme.onPrimaryContainer,
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                fileName.ifBlank { "Build attached" },
                                style = Tokens.typography.titleSmall,
                                color = Tokens.colorScheme.onPrimaryContainer,
                                maxLines = 1,
                            )
                            if (sizeBytes != null) {
                                Text(
                                    formatBytes(sizeBytes),
                                    style = Tokens.typography.bodySmall,
                                    color = Tokens.colorScheme.onPrimaryContainer,
                                )
                            }
                        }
                        Surface(
                            onClick = onClear,
                            shape = CircleShape,
                            color = Tokens.colorScheme.surface,
                            modifier = Modifier.size(28.dp),
                        ) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                SproutlyIcon(
                                    icon = PhosphorIcons.Close,
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
                    icon = PhosphorIcons.Upload,
                    onClick = { picker.launch(APK_MIME_TYPES) },
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "Uploaded to Sproutly. 100 MB max.",
                    style = Tokens.typography.bodySmall,
                    color = Tokens.colorScheme.onSurfaceVariant,
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
            helperText = "Must stay online.",
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
        color = Tokens.colorScheme.errorContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SproutlyIcon(
                    icon = PhosphorIcons.Verified,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = Tokens.colorScheme.onErrorContainer,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Verification needed for paid apps",
                    style = Tokens.typography.titleSmall,
                    color = Tokens.colorScheme.onErrorContainer,
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                if (status == "PENDING") {
                    "Your request is with our team. You can publish this app for free in the meantime."
                } else {
                    "Paid apps can only be published by verified developers, so buyers know who they are paying. Free apps need no verification."
                },
                style = Tokens.typography.bodySmall,
                color = Tokens.colorScheme.onErrorContainer,
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
 * A tile of the publication grid, in the Swiss editorial manner: hairline rules
 * instead of soft cards, an uppercase label, everything on a strict left edge.
 * The form is a document you fill in, so it is set like one.
 */
@Composable
private fun BentoTile(
    title: String? = null,
    hint: String? = null,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
    ) {
        if (title != null) {
            Text(
                title.uppercase(),
                style = Tokens.typography.labelSmall,
                color = Tokens.colorScheme.onSurfaceVariant,
                letterSpacing = 1.4.sp,
            )
            Spacer(Modifier.height(6.dp))
            HorizontalDivider(
                thickness = 1.dp,
                color = Tokens.colorScheme.onSurface.copy(alpha = 0.24f),
            )
            Spacer(Modifier.height(10.dp))
            if (hint != null) {
                Text(
                    hint,
                    style = Tokens.typography.bodySmall,
                    color = Tokens.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(10.dp))
            }
        }
        content()
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
    onSave: (compareAtPaise: Long?, days: Int?, kind: String, code: String) -> Unit,
) {
    val current = product.compareAtPrice?.let { Money.toRupeeString(it) }.orEmpty()
    var was by remember { mutableStateOf(current) }
    var kind2 by remember {
        mutableStateOf(
            if (product.price <= 0 && product.offerKind == "PRICE_DROP") "PROMO_CODE"
            else product.offerKind,
        )
    }
    var code by remember { mutableStateOf(product.promoCode) }
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
                    if (product.price > 0) {
                        "People pay ${formatPrice(product.price)}. Enter the price it used to be " +
                            "to show a discount, and how long the offer runs."
                    } else {
                        "This app is free, so offer a code or something inside the app."
                    },
                    style = Tokens.typography.bodySmall,
                    color = Tokens.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                Text("Offer kind", style = Tokens.typography.labelSmall)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // A free app cannot have a price drop; offer a code or a perk.
                    listOfNotNull(
                        if (product.price > 0) "PRICE_DROP" to "Price drop" else null,
                        "PROMO_CODE" to "Code",
                        "IN_APP" to "In-app",
                    ).forEach { (kind, label) ->
                        SproutlyChip(
                            text = label,
                            selected = kind2 == kind,
                            onClick = { kind2 = kind },
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                if (kind2 == "PROMO_CODE") {
                    SproutlyTextField(
                        value = code,
                        onValueChange = { code = it.take(20) },
                        label = "Code",
                        placeholder = "SPROUT50",
                        helperText = "People type this inside the app.",
                    )
                }
                if (kind2 == "PRICE_DROP") {
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
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(
                    was.toLongOrNull()?.let { it * 100 },
                    days.toIntOrNull(),
                    kind2,
                    code.trim(),
                )
            }) { Text("Save offer") }
        },
        dismissButton = {
            Row {
                if (product.compareAtPrice != null) {
                    TextButton(onClick = { onSave(null, null, "PRICE_DROP", "") }) { Text("Remove") }
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
        "HIGH" -> Tokens.colorScheme.errorContainer
        "MEDIUM" -> Tokens.colorScheme.secondaryContainer
        else -> Tokens.colorScheme.primaryContainer
    }
    val onColour = when (risk) {
        "HIGH" -> Tokens.colorScheme.onErrorContainer
        "MEDIUM" -> Tokens.colorScheme.onSecondaryContainer
        else -> Tokens.colorScheme.onPrimaryContainer
    }

    Surface(
        shape = SproutlyRadii.card,
        color = colour,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SproutlyIcon(
                    PhosphorIcons.Verified,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = onColour,
                )
                Spacer(Modifier.width(6.dp))
                Text("Sprout Safe", style = Tokens.typography.titleSmall, color = onColour)
                Spacer(Modifier.weight(1f))
                Text(
                    when (risk) {
                        "HIGH" -> "High risk"
                        "MEDIUM" -> "Worth a look"
                        else -> "Looks fine"
                    },
                    style = Tokens.typography.labelMedium,
                    color = onColour,
                )
            }
            if (packageName.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    packageName,
                    style = Tokens.typography.bodySmall,
                    color = onColour,
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "${permissions.size} permission${if (permissions.size == 1) "" else "s"} requested",
                style = Tokens.typography.bodySmall,
                color = onColour,
            )
            warnings.forEach { warning ->
                Spacer(Modifier.height(3.dp))
                Text("· $warning", style = Tokens.typography.bodySmall, color = onColour)
            }
            if (warnings.isEmpty() && permissions.isNotEmpty()) {
                Spacer(Modifier.height(3.dp))
                Text(
                    "· None of them are unusual for an app.",
                    style = Tokens.typography.bodySmall,
                    color = onColour,
                )
            }
        }
    }
}

/** Reads a picked build, refusing anything over the ceiling before allocating it. */
private fun readPickedBuild(context: android.content.Context, uri: android.net.Uri): ByteArray? {
    val limit = 100L * 1024 * 1024
    val size = context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { it.length } ?: -1L
    if (size > limit) return null
    return context.contentResolver.openInputStream(uri)?.use { stream ->
        // A stream that lies about its length still cannot exceed the ceiling.
        val buffer = java.io.ByteArrayOutputStream()
        val chunk = ByteArray(64 * 1024)
        var total = 0L
        while (true) {
            val read = stream.read(chunk)
            if (read <= 0) break
            total += read
            if (total > limit) return null
            buffer.write(chunk, 0, read)
        }
        buffer.toByteArray()
    }
}
