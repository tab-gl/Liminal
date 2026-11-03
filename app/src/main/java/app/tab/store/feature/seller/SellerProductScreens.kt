package app.tab.store.feature.seller

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import app.tab.store.domain.model.ProductStatus

@Composable
fun SellerProductsScreen(
    onBack: () -> Unit,
    onEdit: (String?) -> Unit,
    viewModel: SellerProductsViewModel = hiltViewModel(),
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
                            ProductFilter.OUT_OF_STOCK -> "Out of stock"
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
                                    IconButton(onClick = { viewModel.delete(product) }) {
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
    val picker = rememberImagePicker { uri -> if (uri != null) viewModel.onImagePicked(uri) }

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
            // Images
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
            Spacer(Modifier.height(16.dp))

            SproutlyTextField(
                value = state.title,
                onValueChange = viewModel::onTitle,
                label = "App name",
                placeholder = "Monstera deliciosa, 6 inch pot",
            )
            SproutlyTextField(
                value = state.description,
                onValueChange = viewModel::onDescription,
                label = "Description",
                singleLine = false,
                minLines = 4,
                helperText = "What it does, what is included, any requirements.",
            )

            Text("Category", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.categories) { category ->
                    SproutlyChip(
                        text = category.name,
                        selected = state.categoryId == category.id,
                        onClick = { viewModel.onCategory(category) },
                    )
                }
            }
            Spacer(Modifier.height(18.dp))

            // ---------------------------------------------------------- pricing
            Text("Pricing", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))
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
                            label = "MRP (optional)",
                            prefix = "₹ ",
                            keyboardType = KeyboardType.Decimal,
                        )
                    }
                }
            }
            Spacer(Modifier.height(18.dp))

            // ------------------------------------------------------ distribution
            Text("The build", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))
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
                    onClick = { viewModel.setStatus(ProductStatus.ACTIVE) },
                )
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
                text = if (productId == null) "Publish app" else "Save app",
                onClick = viewModel::save,
                enabled = state.canSave,
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
                    "The build is uploaded to your Cloudinary account and linked to this listing.",
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
            helperText = "A direct link you control. Keep it live — installs use it.",
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
