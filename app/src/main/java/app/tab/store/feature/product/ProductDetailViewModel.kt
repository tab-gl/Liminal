package app.tab.store.feature.product

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.tab.store.core.media.AppInstaller
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
    /** True when the device must allow Sproutly as an install source first. */
    val needsInstallPermission: Boolean = false,
    /** Which build of the app to install, when it ships more than one. */
    val chosenFlavour: Int = 0,
    /** What everyone else rated this app, and what you rated it. */
    val rating: app.tab.store.domain.model.RatingSummary =
        app.tab.store.domain.model.RatingSummary(),
    /** The developer behind this listing, and what else they have published. */
    val store: app.tab.store.domain.model.Store? = null,
    val developerApps: List<Product> = emptyList(),
) {
    val canInstall: Boolean get() = product?.status == app.tab.store.domain.model.ProductStatus.ACTIVE && !isInstalling
    val maxQuantity: Int get() = product?.stock ?: 1
}

@HiltViewModel
class ProductDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val catalogRepository: CatalogRepository,
    private val appInstaller: app.tab.store.core.media.AppInstaller,
    private val auth: com.google.firebase.auth.FirebaseAuth,
    private val orderRepository: OrderRepository,
    private val wishlistRepository: WishlistRepository,
    private val adminRepository: app.tab.store.domain.repository.AdminRepository,
) : ViewModel() {

    private val productId: String = savedStateHandle.get<String>("productId").orEmpty()

    private val _uiState = MutableStateFlow(ProductDetailUiState())
    val uiState: StateFlow<ProductDetailUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            catalogRepository.observeReviews(productId).collect { reviews ->
                val mine = auth.currentUser?.uid?.let { uid ->
                    reviews.firstOrNull { it.reviewerId == uid }
                }
                _uiState.update {
                    it.copy(
                        rating = app.tab.store.domain.model.RatingSummary(
                            average = if (reviews.isEmpty()) 0.0
                            else reviews.sumOf { review -> review.rating }.toDouble() / reviews.size,
                            count = reviews.size,
                            mine = mine,
                        ),
                    )
                }
            }
        }
        viewModelScope.launch {
            catalogRepository.observeProduct(productId).collect { product ->
                _uiState.update {
                    it.copy(
                        product = product,
                        isLoading = false,
                        error = if (product == null) "This product is no longer available." else null,
                    )
                }
                // Follow the listing to its developer, then to their shelf.
                val storeId = product?.storeId.orEmpty()
                if (storeId.isNotBlank()) {
                    launch {
                        catalogRepository.observeStore(storeId).collect { store ->
                            _uiState.update { it.copy(store = store) }
                        }
                    }
                    launch {
                        catalogRepository.observeStoreProducts(storeId).collect { apps ->
                            _uiState.update { current ->
                                current.copy(developerApps = apps.filter { it.id != productId })
                            }
                        }
                    }
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

    /**
     * Install downloads the build, hands it to the system installer, and records
     * the app in the library once the installer has opened. Nothing is recorded
     * for a build that could not be fetched.
     */
    fun installApp() {
        val product = _uiState.value.product ?: return
        if (_uiState.value.isInstalling) return
        val build = pickBuild(product)
        if (build == null) {
            _uiState.update { it.copy(message = "This app has no build attached yet.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isInstalling = true, message = null) }
            val outcome = appInstaller.downloadAndInstall(build.apkUrl, "${product.title}-${build.name}.apk")
            _uiState.update {
                it.copy(
                    isInstalling = false,
                    message = when (outcome) {
                        is AppInstaller.Outcome.Launched -> null
                        is AppInstaller.Outcome.NeedsPermission ->
                            "Allow Sproutly to install apps, then come back to finish."
                        is AppInstaller.Outcome.Failed -> outcome.message
                    },
                    needsInstallPermission = outcome is AppInstaller.Outcome.NeedsPermission,
                )
            }
            when (outcome) {
                is AppInstaller.Outcome.Launched -> {
                    // A failed record must not undo an install that already started.
                    (orderRepository.install(product.id) as? DataResult.Failure)?.let { failure ->
                        _uiState.update { it.copy(message = failure.message) }
                    }
                }
                is AppInstaller.Outcome.NeedsPermission -> appInstaller.requestPermission()
                is AppInstaller.Outcome.Failed -> Unit
            }
        }
    }

    private fun pickBuild(product: Product): app.tab.store.domain.model.Flavour? {
        val builds = product.builds
        return builds.getOrNull(_uiState.value.chosenFlavour) ?: builds.firstOrNull()
    }

    /** Called when the screen returns to the foreground, e.g. from the permission page. */
    fun onResumed() {
        if (_uiState.value.needsInstallPermission && appInstaller.canInstall()) {
            _uiState.update { it.copy(needsInstallPermission = false) }
            installApp()
        }
    }

    fun chooseFlavour(index: Int) = _uiState.update { it.copy(chosenFlavour = index) }

    fun openInstallPermission() = appInstaller.requestPermission()

    /** Admin: take a listing down, put it back, or clear a report. */
    fun adminSetStatus(status: app.tab.store.domain.model.ProductStatus) {
        viewModelScope.launch {
            when (val result = adminRepository.setProductStatus(productId, status)) {
                is DataResult.Failure -> _uiState.update { it.copy(message = result.message) }
                is DataResult.Success -> _uiState.update {
                    it.copy(message = "Listing is now ${status.name.lowercase()}.")
                }
            }
        }
    }

    fun adminResolve(keepListed: Boolean) {
        viewModelScope.launch {
            when (val result = adminRepository.resolveReport(productId, keepListed)) {
                is DataResult.Failure -> _uiState.update { it.copy(message = result.message) }
                is DataResult.Success -> _uiState.update {
                    it.copy(message = if (keepListed) "Report dismissed." else "Listing removed.")
                }
            }
        }
    }

    fun consumeMessage() = _uiState.update { it.copy(message = null) }

    /** Writes the caller's rating and refreshes the summary. */
    fun rate(stars: Int, text: String = "") {
        viewModelScope.launch {
            when (val result = catalogRepository.rate(productId, stars, text)) {
                is DataResult.Failure -> _uiState.update { it.copy(message = result.message) }
                is DataResult.Success -> _uiState.update {
                    it.copy(message = "Thanks for rating.")
                }
            }
        }
    }

    fun consumeNavigation() = _uiState.update { it.copy(navigateToLibrary = false) }

    /** Flags the listing; the admin queue reads the same field. */
    fun report(reason: String) {
        viewModelScope.launch {
            when (val result = catalogRepository.reportProduct(productId, reason)) {
                is DataResult.Failure -> _uiState.update { it.copy(message = result.message) }
                is DataResult.Success -> _uiState.update {
                    it.copy(message = "Thanks — this app is with our moderators.")
                }
            }
        }
    }
}
