package app.tab.store.feature.seller

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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
                    icon = Icons.Outlined.Inventory2,
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
                                                "${product.stock} available",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                    }
                                    IconButton(onClick = { viewModel.delete(product) }) {
                                        Icon(
                                            Icons.Filled.Delete,
                                            contentDescription = "Delete",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(19.dp),
                                        )
                                    }
                                }
                                Spacer(Modifier.height(8.dp))
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                Spacer(Modifier.height(8.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    QuantityStepper(
                                        quantity = product.stock.coerceAtLeast(0),
                                        onDecrease = {
                                            viewModel.updateStock(product, (product.stock - 1).coerceAtLeast(0))
                                        },
                                        onIncrease = { viewModel.updateStock(product, product.stock + 1) },
                                        max = 9999,
                                    )
                                    Spacer(Modifier.weight(1f))
                                    TextButton(onClick = { onEdit(product.id) }) { Text("Edit") }
                                    TextButton(onClick = { viewModel.togglePublished(product) }) {
                                        Text(
                                            if (product.status == ProductStatus.ACTIVE) "Unpublish" else "Publish",
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
                            Icon(
                                Icons.Filled.Close,
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
                            Icon(Icons.Filled.Add, contentDescription = "Add photo")
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
                helperText = "What is it, what size, what is included.",
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
            SproutlyTextField(
                value = state.stock,
                onValueChange = viewModel::onStock,
                label = "Stock on hand",
                keyboardType = KeyboardType.Number,
            )
            SproutlyTextField(
                value = state.tags,
                onValueChange = viewModel::onTags,
                label = "Tags (comma separated)",
                placeholder = "indoor, air purifying",
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
