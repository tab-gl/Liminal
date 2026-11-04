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

    fun createStore() {
        val state = _uiState.value
        if (!state.canSubmit) return
        viewModelScope.launch {
            _uiState.update { it.copy(isCreating = true, error = null) }
            when (
                val res = sellerRepository.createStore(
                    name = state.storeName,
                    tagline = state.tagline,
                    city = "",
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
            ProductFilter.HIDDEN -> products.filter { it.status == ProductStatus.DRAFT }
        }
}

enum class ProductFilter { ALL, LIVE, DRAFT, HIDDEN }

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
    /** Free or paid decides whether the price fields are even shown. */
    val isFree: Boolean = false,
    val priceRupees: String = "",
    val compareAtRupees: String = "",
    /** Days until a declared offer ends; blank means no end date. */
    val offerDays: String = "",
    /** Release identity: what makes this an update rather than a new app. */
    val versionName: String = "",
    val versionCode: String = "",
    val releaseNotes: String = "",
    /** Where the build comes from: uploaded APK or an existing link. */
    val apkUrl: String? = null,
    /** Storage path, so a replaced build can be cleaned up later. */
    val apkPath: String? = null,
    val storeId: String = "",
    val apkLink: String = "",
    val apkFileName: String = "",
    val apkSizeBytes: Long? = null,
    val isUploadingApk: Boolean = false,
    /** Paid apps need a verified developer account. */
    val storeVerified: Boolean = false,
    /** Approval state of the store: publishing needs it to be ACTIVE. */
    val storeStatus: String = "PENDING",
    /** False when this build has no uploader URL, so paste-a-link is the way. */
    val apkUploadEnabled: Boolean = true,
    val verificationStatus: String = "NONE",
    val isRequestingVerification: Boolean = false,
    val stock: String = "0",
    val images: List<ProductImage> = emptyList(),
    /** Store screenshots — separate from the icon/gallery images. */
    val screenshots: List<ProductImage> = emptyList(),
    val isUploadingScreenshot: Boolean = false,
    val tags: String = "",
    val status: ProductStatus = ProductStatus.DRAFT,
    val categories: List<Category> = emptyList(),
    val isUploading: Boolean = false,
    val uploadProgress: Float = 0f,
    val isSaving: Boolean = false,
    val error: String? = null,
    val message: String? = null,
    val saved: Boolean = false,
    val loaded: Boolean = false,
) {
    val effectivePrice: Long
        get() = if (isFree) 0L else Money.toPaise(priceRupees) ?: 0L

    val hasBuild: Boolean
        get() = !apkUrl.isNullOrBlank() || apkLink.trim().length > 8

    val needsVerification: Boolean
        get() = !isFree && !storeVerified

    /** True when the store is approved, i.e. a listing may go live. */
    val canPublish: Boolean get() = storeStatus == "ACTIVE"

    val canSave: Boolean
        get() = title.trim().length >= 3 &&
            categoryId.isNotBlank() &&
            hasBuild &&
            !needsVerification &&
            (isFree || effectivePrice > 0) &&
            !isSaving && !isUploading && !isUploadingApk

    val priceError: String?
        get() = if (!isFree && priceRupees.isNotBlank() && (Money.toPaise(priceRupees) ?: 0L) <= 0) {
            "Enter a price greater than zero, or make the app free."
        } else null
}

@HiltViewModel
class SellerProductEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val sellerRepository: SellerRepository,
    private val catalogRepository: CatalogRepository,
    private val mediaRepository: MediaRepository,
    private val uploader: CloudinaryUploader,
    private val apkStorage: app.tab.store.core.media.ApkStorage,
) : ViewModel() {

    private val productId: String? =
        savedStateHandle.get<String>("productId")?.takeIf { it.isNotBlank() }

    private val _uiState = MutableStateFlow(
        ProductEditorUiState(productId = productId, apkUploadEnabled = apkStorage.isConfigured),
    )
    val uiState: StateFlow<ProductEditorUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            catalogRepository.observeCategories().collect { categories ->
                _uiState.update { it.copy(categories = categories) }
            }
        }
        viewModelScope.launch {
            sellerRepository.observeMyStore().collect { store ->
                _uiState.update {
                    it.copy(
                        storeVerified = store?.verified == true,
                        verificationStatus = store?.verificationStatus ?: "NONE",
                        storeStatus = store?.status?.name ?: "PENDING",
                        storeId = store?.id.orEmpty(),
                    )
                }
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
                                    isFree = product.price <= 0L,
                                    priceRupees = Money.toRupeeString(product.price),
                                    apkUrl = product.apkUrl,
                                    apkLink = product.apkUrl.orEmpty(),
                                    apkSizeBytes = product.apkSizeBytes,
                                    compareAtRupees = product.compareAtPrice
                                        ?.let { c -> Money.toRupeeString(c) }.orEmpty(),
                                    versionName = product.versionName,
                                    versionCode = product.versionCode.takeIf { it > 0 }?.toString().orEmpty(),
                                    releaseNotes = product.releaseNotes,
                                    offerDays = product.offerEndsAt
                                        ?.let { ends ->
                                            val days = ((ends - System.currentTimeMillis()) / 86_400_000L)
                                            if (days > 0) days.toString() else ""
                                        }.orEmpty(),
                                    stock = product.stock.toString(),
                                    images = product.images,
                                    screenshots = product.screenshots,
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

    fun onOfferDays(v: String) = _uiState.update {
        it.copy(offerDays = v.filter { c -> c.isDigit() }.take(3))
    }

    fun onVersionName(v: String) = _uiState.update { it.copy(versionName = v.take(20)) }

    fun onVersionCode(v: String) = _uiState.update {
        it.copy(versionCode = v.filter { c -> c.isDigit() }.take(9))
    }

    fun onReleaseNotes(v: String) = _uiState.update { it.copy(releaseNotes = v.take(500)) }
    fun onStock(v: String) = _uiState.update {
        it.copy(stock = v.filter { c -> c.isDigit() }.take(6))
    }
    fun onTags(v: String) = _uiState.update { it.copy(tags = v) }

    fun onFreeChanged(free: Boolean) = _uiState.update {
        it.copy(isFree = free, priceRupees = if (free) "" else it.priceRupees, error = null)
    }

    fun onApkLink(v: String) = _uiState.update { it.copy(apkLink = v, error = null) }

    /**
     * Builds go to Cloudflare R2 through the uploader Worker: Cloudinary's
     * unsigned preset rejects ZIP content and caps at 10 MB, which no real APK
     * fits inside, and a Firestore document is capped at 1 MiB.
     */
    fun onApkPicked(bytes: ByteArray, fileName: String) {
        if (bytes.isEmpty()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isUploadingApk = true, error = null, uploadProgress = 0f) }
            apkStorage.upload(bytes, fileName) { progress ->
                _uiState.update { it.copy(uploadProgress = progress) }
            }.collect { result ->
                result.fold(
                    onSuccess = { uploaded ->
                        _uiState.update {
                            it.copy(
                                isUploadingApk = false,
                                uploadProgress = 0f,
                                apkUrl = uploaded.url,
                                apkPath = uploaded.path,
                                apkFileName = fileName,
                                apkSizeBytes = uploaded.sizeBytes,
                            )
                        }
                    },
                    onFailure = { error ->
                        _uiState.update {
                            it.copy(
                                isUploadingApk = false,
                                uploadProgress = 0f,
                                error = error.message ?: "The upload failed.",
                            )
                        }
                    },
                )
            }
        }
    }

    fun clearApk() = _uiState.update {
        it.copy(apkUrl = null, apkFileName = "", apkSizeBytes = null)
    }

    /** Asks an admin to verify the developer account, which unlocks paid apps. */
    fun requestVerification() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRequestingVerification = true, error = null) }
            when (val res = sellerRepository.requestVerification()) {
                is DataResult.Failure -> _uiState.update {
                    it.copy(isRequestingVerification = false, error = res.message)
                }
                is DataResult.Success -> _uiState.update {
                    it.copy(
                        isRequestingVerification = false,
                        verificationStatus = "PENDING",
                        message = "Verification requested. An admin will review it.",
                    )
                }
            }
        }
    }

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

    fun onScreenshotPicked(uri: Uri) {
        val state = _uiState.value
        if (state.screenshots.size >= 8) {
            _uiState.update { it.copy(error = "You can add up to 8 screenshots.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isUploadingScreenshot = true, error = null) }
            val bytes = uploader.prepare(uri)
            if (bytes == null) {
                _uiState.update {
                    it.copy(isUploadingScreenshot = false, error = "Could not read that image.")
                }
                return@launch
            }
            when (
                val res = mediaRepository.upload(
                    bytes = bytes,
                    fileName = "shot-${System.currentTimeMillis()}.jpg",
                    folder = "sproutly/screenshots",
                )
            ) {
                is DataResult.Failure -> _uiState.update {
                    it.copy(isUploadingScreenshot = false, error = res.message)
                }
                is DataResult.Success -> _uiState.update {
                    it.copy(
                        isUploadingScreenshot = false,
                        screenshots = it.screenshots + ProductImage(res.value.url, res.value.publicId),
                    )
                }
            }
        }
    }

    fun removeScreenshot(publicId: String?) {
        _uiState.update { state ->
            state.copy(screenshots = state.screenshots.filterNot { it.publicId == publicId })
        }
        if (publicId != null) {
            viewModelScope.launch { mediaRepository.delete(publicId) }
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
                screenshots = state.screenshots,
                price = state.effectivePrice,
                compareAtPrice = if (state.isFree) null else Money.toPaise(state.compareAtRupees),
                offerEndsAt = state.offerDays.toIntOrNull()?.let { days ->
                    System.currentTimeMillis() + days * 86_400_000L
                },
                stock = state.stock.toIntOrNull() ?: 0,
                apkUrl = state.apkUrl?.takeIf { it.isNotBlank() } ?: state.apkLink.trim(),
                apkSizeBytes = state.apkSizeBytes,
                versionName = state.versionName.trim().ifBlank { "1.0" },
                versionCode = state.versionCode.toIntOrNull() ?: 1,
                releaseNotes = state.releaseNotes.trim(),
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
    private val apkStorage: app.tab.store.core.media.ApkStorage,
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
