package app.tab.store.feature.wishlist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.tab.store.core.designsystem.HugeIcons
import app.tab.store.core.designsystem.HugeIcon
import app.tab.store.core.designsystem.EmptyState
import app.tab.store.core.designsystem.ProductRowCard
import app.tab.store.core.designsystem.SproutlyTopBar
import app.tab.store.domain.model.Product
import app.tab.store.domain.repository.DataResult
import app.tab.store.domain.repository.WishlistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class WishlistUiState(
    val items: List<Product> = emptyList(),
    val isLoading: Boolean = true,
)

@HiltViewModel
class WishlistViewModel @Inject constructor(
    private val wishlistRepository: WishlistRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(WishlistUiState())
    val uiState: StateFlow<WishlistUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            wishlistRepository.observeWishlist().collect { items ->
                _uiState.value = WishlistUiState(items = items, isLoading = false)
            }
        }
    }

    fun remove(productId: String) {
        viewModelScope.launch { wishlistRepository.remove(productId) }
    }
}

@Composable
fun WishlistScreen(
    onBack: () -> Unit,
    onProduct: (String) -> Unit,
    viewModel: WishlistViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    androidx.compose.foundation.layout.Column(Modifier.fillMaxSize()) {
        SproutlyTopBar(
            title = "Wishlist",
            subtitle = "${state.items.size} saved",
            onBack = onBack,
        )
        if (!state.isLoading && state.items.isEmpty()) {
            EmptyState(
                title = "Nothing saved yet",
                message = "Tap the heart on any product to keep it here for later.",
                icon = HugeIcons.Favourite,
            )
        } else {
            LazyColumn(
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    horizontal = 20.dp,
                    vertical = 8.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(state.items, key = { it.id }) { product ->
                    ProductRowCard(
                        product = product,
                        onClick = { onProduct(product.id) },
                        trailing = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { viewModel.remove(product.id) }) {
                                    Icon(
                                        HugeIcons.Close,
                                        contentDescription = "Remove",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        },
                    )
                }
            }
        }
    }
}
