package app.tab.store.feature.store

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.tab.store.core.designsystem.HugeIcons
import app.tab.store.core.designsystem.HugeIcon
import app.tab.store.core.designsystem.EmptyState
import app.tab.store.core.designsystem.ProductCard
import app.tab.store.core.designsystem.RatingBadge
import app.tab.store.core.designsystem.ShimmerBox
import app.tab.store.core.designsystem.SproutlyImage
import app.tab.store.core.designsystem.SproutlyRadii
import app.tab.store.core.designsystem.SproutlyTag
import app.tab.store.domain.model.Product
import app.tab.store.domain.model.Store
import app.tab.store.domain.repository.CatalogRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class StoreUiState(
    val store: Store? = null,
    val products: List<Product> = emptyList(),
    val isLoading: Boolean = true,
)

@HiltViewModel
class StoreViewModel @Inject constructor(
    savedStateHandle: androidx.lifecycle.SavedStateHandle,
    catalogRepository: CatalogRepository,
) : ViewModel() {

    private val storeId: String = savedStateHandle.get<String>("storeId").orEmpty()

    private val _uiState = MutableStateFlow(StoreUiState())
    val uiState: StateFlow<StoreUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            catalogRepository.observeStore(storeId).collect { store ->
                _uiState.value = _uiState.value.copy(store = store, isLoading = store == null)
            }
        }
        viewModelScope.launch {
            catalogRepository.observeStoreProducts(storeId).collect { products ->
                _uiState.value = _uiState.value.copy(products = products, isLoading = false)
            }
        }
    }
}

@Composable
fun StoreScreen(
    storeId: String,
    onBack: () -> Unit,
    onProduct: (String) -> Unit,
    viewModel: StoreViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val store = state.store

    LazyColumn(Modifier.fillMaxSize()) {
        item {
            Box {
                SproutlyImage(
                    url = store?.bannerUrl,
                    contentDescription = store?.name,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(190.dp),
                    corner = 0.dp,
                )
                Surface(
                    onClick = onBack,
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                    modifier = Modifier
                        .padding(14.dp)
                        .size(40.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        HugeIcon(HugeIcons.ArrowLeft, contentDescription = "Back")
                    }
                }
            }
        }

        item {
            Column(Modifier.padding(horizontal = 20.dp)) {
                Row(
                    modifier = Modifier.padding(top = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SproutlyImage(
                        url = store?.logoUrl,
                        contentDescription = store?.name,
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape),
                        corner = 32.dp,
                    )
                    Spacer(Modifier.size(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            store?.name ?: "Loading developer…",
                            style = MaterialTheme.typography.headlineMedium,
                        )
                        if (store != null) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RatingBadge(store.rating, store.ratingCount)
                                Spacer(Modifier.size(8.dp))
                                Text(
                                    store.city.ifBlank { "India" },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                    if (store != null) {
                        SproutlyTag(
                            text = "${state.products.size} apps",
                            container = MaterialTheme.colorScheme.primaryContainer,
                            content = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
                if (!store?.tagline.isNullOrBlank()) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        store?.tagline.orEmpty(),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (!store?.description.isNullOrBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        store?.description.orEmpty(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(18.dp))
                Text("More from this developer", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(6.dp))
            }
        }

        if (state.isLoading) {
            item {
                ShimmerBox(
                    Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .padding(20.dp),
                    corner = 22.dp,
                )
            }
        } else if (state.products.isEmpty()) {
            item {
                EmptyState(
                    title = "No live apps",
                    message = "This store has not published anything yet. Check back soon.",
                )
            }
        } else {
            items(state.products.chunked(2)) { row ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
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
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}
