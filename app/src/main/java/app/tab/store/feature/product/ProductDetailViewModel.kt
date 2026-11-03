package app.tab.store.feature.product

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.tab.store.domain.model.Product
import app.tab.store.domain.repository.OrderRepository
import app.tab.store.domain.repository.CatalogRepository
import app.tab.store.domain.repository.DataResult
import app.tab.store.domain.repository.WishlistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProductDetailUiState(
    val product: Product? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
    val quantity: Int = 1,
    val isWishlisted: Boolean = false,
    val isInstalling: Boolean = false,
    val message: String? = null,
    val navigateToLibrary: Boolean = false,
) {
    val canInstall: Boolean get() = product?.status == app.tab.store.domain.model.ProductStatus.ACTIVE && !isInstalling
    val maxQuantity: Int get() = product?.stock ?: 1
}

@HiltViewModel
class ProductDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val catalogRepository: CatalogRepository,
    private val orderRepository: OrderRepository,
    private val wishlistRepository: WishlistRepository,
) : ViewModel() {

    private val productId: String = savedStateHandle.get<String>("productId").orEmpty()

    private val _uiState = MutableStateFlow(ProductDetailUiState())
    val uiState: StateFlow<ProductDetailUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            catalogRepository.observeProduct(productId).collect { product ->
                _uiState.update {
                    it.copy(
                        product = product,
                        isLoading = false,
                        error = if (product == null) "This product is no longer available." else null,
                    )
                }
            }
        }
        viewModelScope.launch {
            wishlistRepository.observeWishlistIds().collect { ids ->
                _uiState.update { it.copy(isWishlisted = ids.contains(productId)) }
            }
        }
        viewModelScope.launch { catalogRepository.recordProductView(productId) }
    }

    fun setQuantity(value: Int) = _uiState.update {
        it.copy(quantity = value.coerceIn(1, it.maxQuantity.coerceAtLeast(1)))
    }

    fun toggleWishlist() {
        val product = _uiState.value.product ?: return
        viewModelScope.launch {
            when (val res = wishlistRepository.toggle(product)) {
                is DataResult.Failure -> _uiState.update { it.copy(message = res.message) }
                is DataResult.Success -> _uiState.update {
                    it.copy(
                        message = if (res.value) "Saved to your wishlist" else "Removed from wishlist",
                    )
                }
            }
        }
    }

    /** Get / Install: writes the install record and opens the library. */
    fun install() {
        val product = _uiState.value.product ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isInstalling = true) }
            when (val res = orderRepository.install(product.id)) {
                is DataResult.Failure -> _uiState.update {
                    it.copy(isInstalling = false, message = res.message)
                }
                is DataResult.Success -> _uiState.update {
                    it.copy(
                        isInstalling = false,
                        message = "${product.title} added to your library",
                        navigateToLibrary = true,
                    )
                }
            }
        }
    }

    fun consumeMessage() = _uiState.update { it.copy(message = null) }

    fun consumeNavigation() = _uiState.update { it.copy(navigateToLibrary = false) }
}
