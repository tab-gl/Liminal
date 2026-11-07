package app.tab.store.feature.search

import app.tab.store.core.ui.Tokens
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import app.tab.store.core.ui.ExperimentalMaterial3ExpressiveApi
import app.tab.store.core.ui.LoadingIndicator
import app.tab.store.core.ui.ExperimentalMaterial3Api
import app.tab.store.core.ui.IconButton
import app.tab.store.core.ui.ModalBottomSheet
import app.tab.store.core.ui.Surface
import app.tab.store.core.ui.Text
import app.tab.store.core.ui.TextButton
import app.tab.store.core.ui.TextField
import app.tab.store.core.ui.TextFieldDefaults
import app.tab.store.core.ui.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.tab.store.core.designsystem.PhosphorIcons
import app.tab.store.core.designsystem.SproutlyIcon
import app.tab.store.core.designsystem.SproutlyImage
import app.tab.store.core.designsystem.OfficialBadge
import app.tab.store.core.designsystem.EmptyState
import app.tab.store.core.designsystem.ErrorState
import app.tab.store.core.designsystem.LoadingColumn
import app.tab.store.core.designsystem.ProductCard
import app.tab.store.core.designsystem.SproutlyChip
import app.tab.store.core.designsystem.SproutlyRadii
import app.tab.store.domain.repository.ProductSort

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SearchScreen(
    initialCategoryId: String?,
    initialQuery: String? = null,
    onProduct: (String) -> Unit,
    onStore: (String) -> Unit = {},
    viewModel: SearchViewModel = hiltViewModel(),
) = SearchPanel(
    initialCategoryId = initialCategoryId,
    initialQuery = initialQuery,
    onProduct = onProduct,
    onStore = onStore,
    title = { Text("Search", style = Tokens.typography.headlineLarge) },
    viewModel = viewModel,
)

/**
 * The search UI without a page around it, so it can slide up inside Home and
 * Games instead of taking over the screen. [onClose] shows a dismiss button.
 */
@Composable
fun SearchPanel(
    initialCategoryId: String? = null,
    initialQuery: String? = null,
    onProduct: (String) -> Unit,
    onStore: (String) -> Unit = {},
    onClose: (() -> Unit)? = null,
    title: @Composable (() -> Unit)? = null,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val keyboard = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }
    var showSortSheet by remember { mutableStateOf(false) }

    LaunchedEffect(initialQuery) {
        if (!initialQuery.isNullOrBlank()) viewModel.submitQuery(initialQuery)
    }

    LaunchedEffect(initialCategoryId, state.categories) {
        if (initialCategoryId != null) {
            val label = state.categories.firstOrNull { it.id == initialCategoryId }?.name
            viewModel.setCategory(initialCategoryId, label)
        }
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 8.dp, top = 14.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (title != null) title() else Text("Search", style = Tokens.typography.headlineLarge)
            Spacer(Modifier.weight(1f))
            IconButton(onClick = { showSortSheet = true }) {
                SproutlyIcon(PhosphorIcons.Filter, contentDescription = "Sort and filter")
            }
            if (onClose != null) {
                IconButton(onClick = onClose) {
                    SproutlyIcon(PhosphorIcons.Close, contentDescription = "Close search")
                }
            }
        }

        Surface(
            shape = SproutlyRadii.pill,
            color = Tokens.colorScheme.surface,
            border = androidx.compose.foundation.BorderStroke(
                1.dp, Tokens.colorScheme.outlineVariant,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .height(52.dp),
        ) {
            TextField(
                value = state.query,
                onValueChange = viewModel::onQueryChange,
                modifier = Modifier
                    .fillMaxSize()
                    .focusRequester(focusRequester),
                placeholder = { Text("Search apps", style = Tokens.typography.bodyMedium) },
                leadingIcon = {
                    SproutlyIcon(PhosphorIcons.Search, contentDescription = null, modifier = Modifier.size(20.dp))
                },
                trailingIcon = {
                    if (state.query.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onQueryChange("") }) {
                            SproutlyIcon(PhosphorIcons.Close, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        viewModel.submitQuery(state.query)
                        keyboard?.hide()
                    },
                ),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Tokens.colorScheme.surface,
                    unfocusedContainerColor = Tokens.colorScheme.surface,
                    focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                    unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                    cursorColor = Tokens.colorScheme.primary,
                ),
            )
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                SproutlyChip(
                    text = "All",
                    selected = state.categoryId == null,
                    onClick = { viewModel.setCategory(null, null) },
                )
            }
            items(state.categories, key = { it.id }) { category ->
                SproutlyChip(
                    text = category.name,
                    selected = state.categoryId == category.id,
                    onClick = { viewModel.setCategory(category.id, category.name) },
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = when {
                    state.isLoading -> "Searching…"
                    state.showDevelopers ->
                        if (state.stores.isEmpty()) "No developers found"
                        else "${state.stores.size} developer${if (state.stores.size == 1) "" else "s"}"

                    state.results.isEmpty() -> "No results yet"
                    else -> "${state.results.size} results"
                },
                style = Tokens.typography.labelMedium,
                color = Tokens.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.weight(1f))
            TextButton(onClick = { showSortSheet = true }) {
                Text(state.sortLabel, style = Tokens.typography.labelMedium)
            }
        }

        Box(Modifier.fillMaxSize()) {
            when {
                state.isLoading -> LoadingColumn(Modifier.padding(horizontal = 20.dp))
                state.error != null -> ErrorState(
                    message = state.error.orEmpty(),
                    onRetry = { viewModel.runSearch(reset = true) },
                )
                state.isEmpty -> EmptyState(
                    title = "Nothing matched",
                    message = "Try a different keyword, or widen the filters.",
                    icon = PhosphorIcons.SearchOff,
                    actionLabel = "Clear filters",
                    onAction = {
                        viewModel.setCategory(null, null)
                        viewModel.onQueryChange("")
                    },
                )
                else -> LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SproutlyChip(
                                text = "Apps",
                                selected = !state.showDevelopers,
                                onClick = { viewModel.showDevelopers(false) },
                            )
                            SproutlyChip(
                                text = "Developers",
                                selected = state.showDevelopers,
                                onClick = { viewModel.showDevelopers(true) },
                            )
                        }
                    }
                    if (state.showDevelopers) {
                        items(state.stores, key = { "store-${it.id}" }) { store ->
                            Surface(
                                onClick = { onStore(store.id) },
                                shape = SproutlyRadii.card,
                                color = Tokens.colorScheme.surface,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp),
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    SproutlyImage(
                                        url = store.logoUrl,
                                        contentDescription = store.name,
                                        modifier = Modifier.size(44.dp),
                                        corner = 12.dp,
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(store.name, style = Tokens.typography.titleSmall)
                                        Text(
                                            store.tagline.ifBlank { "Developer" },
                                            style = Tokens.typography.bodySmall,
                                            color = Tokens.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                        )
                                    }
                                    if (store.official) OfficialBadge()
                                }
                            }
                        }
                    } else {
                    items(state.results.distinctBy { it.id }.chunked(2), key = { row -> row.first().id }) { row ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            row.forEach { product ->
                                ProductCard(
                                    product = product,
                                    onClick = { onProduct(product.id) },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            if (row.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                    }
                    if (state.hasMore) {
                        item {
                            LaunchedEffect(state.nextPageToken) { viewModel.loadMore() }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                LoadingIndicator(
                                    modifier = Modifier.size(28.dp),
                                    color = Tokens.colorScheme.primary,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showSortSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSortSheet = false },
            sheetState = rememberModalBottomSheetState(),
            shape = SproutlyRadii.sheet,
            containerColor = Tokens.colorScheme.surface,
        ) {
            Column(Modifier.padding(horizontal = 24.dp, vertical = 8.dp)) {
                Text("Sort by", style = Tokens.typography.headlineSmall)
                Spacer(Modifier.height(10.dp))
                ProductSort.entries.forEach { sort ->
                    SortRow(
                        label = when (sort) {
                            ProductSort.RELEVANCE -> "Best match"
                            ProductSort.PRICE_LOW_HIGH -> "Price: low to high"
                            ProductSort.PRICE_HIGH_LOW -> "Price: high to low"
                            ProductSort.NEWEST -> "Newest first"
                            ProductSort.TOP_RATED -> "Top rated"
                        },
                        selected = state.sort == sort,
                        onClick = {
                            viewModel.setSort(sort)
                            showSortSheet = false
                        },
                    )
                }
                Spacer(Modifier.height(14.dp))
                Text("Price", style = Tokens.typography.headlineSmall)
                Spacer(Modifier.height(10.dp))
                priceBands.forEach { band ->
                    SortRow(
                        label = band.label,
                        selected = state.priceBand == band,
                        onClick = {
                            viewModel.setPriceBand(band)
                            showSortSheet = false
                        },
                    )
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun SortRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = SproutlyRadii.pill,
        color = if (selected) Tokens.colorScheme.primaryContainer
        else Tokens.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
    ) {
        Text(
            text = label,
            style = Tokens.typography.bodyLarge,
            color = if (selected) Tokens.colorScheme.onPrimaryContainer
            else Tokens.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        )
    }
}
