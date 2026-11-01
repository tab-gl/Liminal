package app.tab.store.feature.seller

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.tab.store.core.media.CloudinaryUploader
import app.tab.store.core.utils.Money
import app.tab.store.core.utils.Validators
import app.tab.store.domain.model.Category
import app.tab.store.domain.model.Order
import app.tab.store.domain.model.OrderStatus
import app.tab.store.domain.model.Product
import app.tab.store.domain.model.ProductImage
import app.tab.store.domain.model.ProductStatus
import app.tab.store.domain.model.SellerStats
import app.tab.store.domain.model.Store
import app.tab.store.domain.repository.CatalogRepository
import app.tab.store.domain.repository.DataResult
import app.tab.store.domain.repository.MediaRepository
import app.tab.store.domain.repository.OrderRepository
import app.tab.store.domain.repository.ProfileRepository
import app.tab.store.domain.repository.SellerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// ------------------------------------------------------------------- setup
data class SellerSetupUiState(
    val storeName: String = "",
    val tagline: String = "",
    val city: String = "",
    val isCreating: Boolean = false,
    val error: String? = null,
    val createdStoreId: String? = null,
) {
    val canSubmit: Boolean get() = storeName.trim().length >= 3 && !isCreating
}

@HiltViewModel
class SellerSetupViewModel @Inject constructor(
    private val sellerRepository: SellerRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SellerSetupUiState())
    val uiState: StateFlow<SellerSetupUiState> = _uiState.asStateFlow()

    fun onName(value: String) = _uiState.update { it.copy(storeName = value, error = null) }
    fun onTagline(value: String) = _uiState.update { it.copy(tagline = value.take(80)) }
    fun onCity(value: String) = _uiState.update { it.copy(city = value) }

    fun createStore() {
        val state = _uiState.value
        if (!state.canSubmit) return
        viewModelScope.launch {
            _uiState.update { it.copy(isCreating = true, error = null) }
            when (
                val res = sellerRepository.createStore(
                    name = state.storeName,
                    tagline = state.tagline,
                    city = state.city,
                )
            ) {
                is DataResult.Failure -> _uiState.update {
                    it.copy(isCreating = false, error = res.message)
                }
                is DataResult.Success -> _uiState.update {
                    it.copy(isCreating = false, createdStoreId = res.value)
                }
            }
        }
    }
}

// ---------------------------------------------------------------- dashboard
data class SellerDashboardUiState(
    val store: Store? = null,
    val stats: SellerStats = SellerStats(),
    val recentOrders: List<Order> = emptyList(),
    val lowStock: List<Product> = emptyList(),
    val isLoading: Boolean = true,
    val needsStore: Boolean = false,
)

@HiltViewModel
class SellerDashboardViewModel @Inject constructor(
    sellerRepository: SellerRepository,
    orderRepository: OrderRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SellerDashboardUiState())
    val uiState: StateFlow<SellerDashboardUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            sellerRepository.observeMyStore().collect { store ->
                _uiState.update {
                    it.copy(store = store, isLoading = false, needsStore = store == null)
                }
                if (store != null) {
                    launch {
                        sellerRepository.observeStats(store.id).collect { stats ->
                            _uiState.update { s -> s.copy(stats = stats) }
                        }
                    }
                    launch {
                        orderRepository.observeSellerOrders(store.id).collect { orders ->
                            _uiState.update {
                                it.copy(
                                    recentOrders = orders.take(4),
                                    lowStock = emptyList(),
                                )
                            }
                        }
                    }
                    launch {
                        sellerRepository.observeStoreProducts(store.id).collect { products ->
                            _uiState.update {
                                it.copy(lowStock = products.filter { p -> p.stock in 0..5 }.take(5))
                            }
                        }
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------------------- products
data class SellerProductsUiState(
    val store: Store? = null,
    val products: List<Product> = emptyList(),
    val isLoading: Boolean = true,
    val message: String? = null,
    val filter: ProductFilter = ProductFilter.ALL,
) {
    val visible: List<Product>
        get() = when (filter) {
            ProductFilter.ALL -> products
            ProductFilter.LIVE -> products.filter { it.status == ProductStatus.ACTIVE }
            ProductFilter.DRAFT -> products.filter { it.status == ProductStatus.DRAFT }
            ProductFilter.OUT_OF_STOCK -> products.filter { it.stock <= 0 }
        }
}

enum class ProductFilter { ALL, LIVE, DRAFT, OUT_OF_STOCK }

@HiltViewModel
class SellerProductsViewModel @Inject constructor(
    private val sellerRepository: SellerRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SellerProductsUiState())
    val uiState: StateFlow<SellerProductsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            sellerRepository.observeMyStore().collect { store ->
                _uiState.update { it.copy(store = store) }
                if (store != null) {
                    launch {
                        sellerRepository.observeStoreProducts(store.id).collect { products ->
                            _uiState.update { it.copy(products = products, isLoading = false) }
                        }
                    }
                }
            }
        }
    }

    fun setFilter(filter: ProductFilter) = _uiState.update { it.copy(filter = filter) }

    fun togglePublished(product: Product) {
        viewModelScope.launch {
            val next = if (product.status == ProductStatus.ACTIVE) ProductStatus.DRAFT
            else ProductStatus.ACTIVE
            val res = sellerRepository.setProductStatus(product.id, next)
            _uiState.update {
                it.copy(
                    message = when (res) {
                        is DataResult.Failure -> res.message
                        is DataResult.Success ->
                            if (next == ProductStatus.ACTIVE) "Product is live" else "Product moved to drafts"
                    },
                )
            }
        }
    }

    fun updateStock(product: Product, stock: Int) {
        viewModelScope.launch { sellerRepository.setStock(product.id, stock) }
    }

    fun delete(product: Product) {
        viewModelScope.launch {
            val res = sellerRepository.deleteProduct(product.id)
            _uiState.update {
                it.copy(
                    message = when (res) {
                        is DataResult.Failure -> res.message
                        is DataResult.Success -> "Product deleted"
                    },
                )
            }
        }
    }

    fun consumeMessage() = _uiState.update { it.copy(message = null) }
}

// ------------------------------------------------------------ product editor
data class ProductEditorUiState(
    val productId: String? = null,
    val title: String = "",
    val description: String = "",
    val categoryId: String = "",
    val categoryLabel: String = "",
    val priceRupees: String = "",
    val compareAtRupees: String = "",
    val stock: String = "0",
    val images: List<ProductImage> = emptyList(),
    val tags: String = "",
    val status: ProductStatus = ProductStatus.DRAFT,
    val categories: List<Category> = emptyList(),
    val isUploading: Boolean = false,
    val uploadProgress: Float = 0f,
    val isSaving: Boolean = false,
    val error: String? = null,
    val saved: Boolean = false,
    val loaded: Boolean = false,
) {
    val canSave: Boolean
        get() = title.trim().length >= 3 &&
            categoryId.isNotBlank() &&
            (Money.toPaise(priceRupees) ?: 0L) > 0 &&
            !isSaving && !isUploading

    val priceError: String?
        get() = if (priceRupees.isNotBlank() && (Money.toPaise(priceRupees) ?: 0L) <= 0) {
            "Enter a price greater than zero."
        } else null
}

@HiltViewModel
class SellerProductEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val sellerRepository: SellerRepository,
    private val catalogRepository: CatalogRepository,
    private val mediaRepository: MediaRepository,
    private val uploader: CloudinaryUploader,
) : ViewModel() {

    private val productId: String? =
        savedStateHandle.get<String>("productId")?.takeIf { it.isNotBlank() }

    private val _uiState = MutableStateFlow(ProductEditorUiState(productId = productId))
    val uiState: StateFlow<ProductEditorUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            catalogRepository.observeCategories().collect { categories ->
                _uiState.update { it.copy(categories = categories) }
            }
        }
        if (productId != null) {
            viewModelScope.launch {
                sellerRepository.observeMyStore().collect { store ->
                    if (store == null) return@collect
                    sellerRepository.observeStoreProducts(store.id).collect { products ->
                        val product = products.firstOrNull { it.id == productId }
                        if (product != null && !_uiState.value.loaded) {
                            _uiState.update {
                                it.copy(
                                    title = product.title,
                                    description = product.description,
                                    categoryId = product.categoryId,
                                    categoryLabel = product.categoryLabel,
                                    priceRupees = Money.toRupeeString(product.price),
                                    compareAtRupees = product.compareAtPrice
                                        ?.let { c -> Money.toRupeeString(c) }.orEmpty(),
                                    stock = product.stock.toString(),
                                    images = product.images,
                                    tags = product.tags.joinToString(", "),
                                    status = product.status,
                                    loaded = true,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    fun onTitle(v: String) = _uiState.update { it.copy(title = v, error = null) }
    fun onDescription(v: String) = _uiState.update { it.copy(description = v) }
    fun onPrice(v: String) = _uiState.update { it.copy(priceRupees = sanitiseMoney(v), error = null) }
    fun onCompareAt(v: String) = _uiState.update { it.copy(compareAtRupees = sanitiseMoney(v)) }
    fun onStock(v: String) = _uiState.update {
        it.copy(stock = v.filter { c -> c.isDigit() }.take(6))
    }
    fun onTags(v: String) = _uiState.update { it.copy(tags = v) }

    fun onCategory(category: Category) = _uiState.update {
        it.copy(categoryId = category.id, categoryLabel = category.name)
    }

    fun setStatus(status: ProductStatus) = _uiState.update { it.copy(status = status) }

    fun onImagePicked(uri: Uri) {
        val state = _uiState.value
        if (state.images.size >= 6) {
            _uiState.update { it.copy(error = "You can add up to 6 images.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isUploading = true, uploadProgress = 0f, error = null) }
            val bytes = uploader.prepare(uri)
            if (bytes == null) {
                _uiState.update { it.copy(isUploading = false, error = "Could not read that image.") }
                return@launch
            }
            val storeId = _uiState.value.productId ?: "new"
            when (
                val res = mediaRepository.upload(
                    bytes = bytes,
                    fileName = "product-${System.currentTimeMillis()}.jpg",
                    folder = "sproutly/products",
                    onProgress = { p -> _uiState.update { it.copy(uploadProgress = p) } },
                )
            ) {
                is DataResult.Failure -> _uiState.update {
                    it.copy(isUploading = false, error = res.message)
                }
                is DataResult.Success -> _uiState.update {
                    it.copy(
                        isUploading = false,
                        uploadProgress = 1f,
                        images = it.images + ProductImage(res.value.url, res.value.publicId),
                    )
                }
            }
        }
    }

    fun removeImage(publicId: String?) {
        _uiState.update { state ->
            state.copy(images = state.images.filterNot { it.publicId == publicId })
        }
        if (publicId != null) {
            viewModelScope.launch { mediaRepository.delete(publicId) }
        }
    }

    fun save() {
        val state = _uiState.value
        if (!state.canSave) return
        viewModelScope.launch {
            // The store already exists by the time a product can be created.
            val store = sellerRepository.observeMyStore().first() ?: return@launch

            _uiState.update { it.copy(isSaving = true, error = null) }
            val product = Product(
                id = state.productId.orEmpty(),
                storeId = store.id,
                storeName = store.name,
                title = state.title,
                description = state.description,
                categoryId = state.categoryId,
                categoryLabel = state.categoryLabel,
                images = state.images,
                price = Money.toPaise(state.priceRupees) ?: 0L,
                compareAtPrice = Money.toPaise(state.compareAtRupees),
                stock = state.stock.toIntOrNull() ?: 0,
                status = state.status,
                tags = state.tags.split(",").map { it.trim() }.filter { it.isNotBlank() },
            )
            when (val res = sellerRepository.upsertProduct(product)) {
                is DataResult.Failure -> _uiState.update {
                    it.copy(isSaving = false, error = res.message)
                }
                is DataResult.Success -> _uiState.update {
                    it.copy(isSaving = false, saved = true, productId = res.value)
                }
            }
        }
    }

    private fun sanitiseMoney(input: String): String {
        val cleaned = input.filter { it.isDigit() || it == '.' }
        val parts = cleaned.split(".")
        return if (parts.size > 1) "${parts[0]}.${parts[1].take(2)}" else cleaned
    }
}

// ------------------------------------------------------------------- orders
data class SellerOrdersUiState(
    val store: Store? = null,
    val orders: List<Order> = emptyList(),
    val isLoading: Boolean = true,
    val message: String? = null,
    val updatingId: String? = null,
)

@HiltViewModel
class SellerOrdersViewModel @Inject constructor(
    private val sellerRepository: SellerRepository,
    private val orderRepository: OrderRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SellerOrdersUiState())
    val uiState: StateFlow<SellerOrdersUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            sellerRepository.observeMyStore().collect { store ->
                _uiState.update { it.copy(store = store) }
                if (store != null) {
                    launch {
                        orderRepository.observeSellerOrders(store.id).collect { orders ->
                            _uiState.update { it.copy(orders = orders, isLoading = false) }
                        }
                    }
                }
            }
        }
    }

    fun advance(order: Order, next: OrderStatus) {
        viewModelScope.launch {
            _uiState.update { it.copy(updatingId = order.id) }
            val res = orderRepository.updateOrderStatus(order.id, next)
            _uiState.update {
                it.copy(
                    updatingId = null,
                    message = when (res) {
                        is DataResult.Failure -> res.message
                        is DataResult.Success -> "Order marked ${next.name.lowercase()}"
                    },
                )
            }
        }
    }

    fun consumeMessage() = _uiState.update { it.copy(message = null) }
}

// ---------------------------------------------------------------- analytics
data class SellerAnalyticsUiState(
    val stats: SellerStats = SellerStats(),
    val store: Store? = null,
    val isLoading: Boolean = true,
)

@HiltViewModel
class SellerAnalyticsViewModel @Inject constructor(
    sellerRepository: SellerRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SellerAnalyticsUiState())
    val uiState: StateFlow<SellerAnalyticsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            sellerRepository.observeMyStore().collect { store ->
                _uiState.update { it.copy(store = store) }
                if (store != null) {
                    launch {
                        sellerRepository.observeStats(store.id).collect { stats ->
                            _uiState.update { it.copy(stats = stats, isLoading = false) }
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------- store editor
data class StoreEditorUiState(
    val store: Store = Store(),
    val isUploadingLogo: Boolean = false,
    val isUploadingBanner: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
    val saved: Boolean = false,
    val loaded: Boolean = false,
)

@HiltViewModel
class SellerStoreEditViewModel @Inject constructor(
    private val sellerRepository: SellerRepository,
    private val mediaRepository: MediaRepository,
    private val uploader: CloudinaryUploader,
) : ViewModel() {

    private val _uiState = MutableStateFlow(StoreEditorUiState())
    val uiState: StateFlow<StoreEditorUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            sellerRepository.observeMyStore().collect { store ->
                if (store != null && !_uiState.value.loaded) {
                    _uiState.update { it.copy(store = store, loaded = true) }
                }
            }
        }
    }

    fun update(transform: (Store) -> Store) = _uiState.update {
        it.copy(store = transform(it.store), error = null)
    }

    fun uploadLogo(uri: Uri) = upload(uri, isLogo = true)
    fun uploadBanner(uri: Uri) = upload(uri, isLogo = false)

    private fun upload(uri: Uri, isLogo: Boolean) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isUploadingLogo = isLogo,
                    isUploadingBanner = !isLogo,
                    error = null,
                )
            }
            val bytes = uploader.prepare(uri, maxEdge = if (isLogo) 800 else 1800)
            if (bytes == null) {
                _uiState.update {
                    it.copy(isUploadingLogo = false, isUploadingBanner = false, error = "Could not read that image.")
                }
                return@launch
            }
            val res = mediaRepository.upload(
                bytes = bytes,
                fileName = if (isLogo) "logo.jpg" else "banner.jpg",
                folder = if (isLogo) "sproutly/store-logos" else "sproutly/store-banners",
            )
            when (res) {
                is DataResult.Failure -> _uiState.update {
                    it.copy(
                        isUploadingLogo = false,
                        isUploadingBanner = false,
                        error = res.message,
                    )
                }
                is DataResult.Success -> _uiState.update { state ->
                    state.copy(
                        isUploadingLogo = false,
                        isUploadingBanner = false,
                        store = if (isLogo) {
                            state.store.copy(logoUrl = res.value.url, logoPublicId = res.value.publicId)
                        } else {
                            state.store.copy(bannerUrl = res.value.url, bannerPublicId = res.value.publicId)
                        },
                    )
                }
            }
        }
    }

    fun save() {
        val state = _uiState.value
        if (state.store.name.trim().length < 3) {
            _uiState.update { it.copy(error = "Store name needs at least 3 characters.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            when (val res = sellerRepository.updateStore(state.store)) {
                is DataResult.Failure -> _uiState.update {
                    it.copy(isSaving = false, error = res.message)
                }
                is DataResult.Success -> _uiState.update { it.copy(isSaving = false, saved = true) }
            }
        }
    }
}
