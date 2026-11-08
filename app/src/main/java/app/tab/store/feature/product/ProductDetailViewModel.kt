package app.tab.store.feature.product

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.tab.store.core.media.AppInstaller
import app.tab.store.domain.model.Product
import app.tab.store.domain.repository.AppLibraryRepository
import app.tab.store.domain.repository.CatalogRepository
import app.tab.store.domain.repository.DataResult
import app.tab.store.domain.repository.WishlistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

data class ProductDetailUiState(
    val product: Product? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
    val quantity: Int = 1,
    val isWishlisted: Boolean = false,
    val isInstalling: Boolean = false,
    /** 0..1 while the build is downloading; -1 when the size is unknown. */
    val downloadProgress: Float = 0f,
    val message: String? = null,
    val navigateToLibrary: Boolean = false,
    /** True when the device must allow Sproutly as an install source first. */
    val needsInstallPermission: Boolean = false,
    /** Which build of the app to install, when it ships more than one. */
    val chosenFlavour: Int = 0,
    /** Set once the system installer has been handed the build. */
    val installedPackage: String? = null,
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
    private val appLibraryRepository: AppLibraryRepository,
    private val wishlistRepository: WishlistRepository,
    private val adminRepository: app.tab.store.domain.repository.AdminRepository,
) : ViewModel() {

    private val productId: String = savedStateHandle.get<String>("productId").orEmpty()

    /** The store whose listeners are running, so they are not restarted per snapshot. */
    private var followingStoreId: String? = null
    private var storeJobs: List<Job> = emptyList()

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
                // Follow the listing to its developer. Collected once per store id:
                // launching two more collectors on every product snapshot piled up
                // duplicate listeners whenever the listing was edited.
                val storeId = product?.storeId.orEmpty()
                if (storeId.isNotBlank() && storeId != followingStoreId) {
                    followingStoreId = storeId
                    storeJobs.forEach { it.cancel() }
                    storeJobs = listOf(
                        launch {
                            catalogRepository.observeStore(storeId)
                                .distinctUntilChanged()
                                .collect { store -> _uiState.update { it.copy(store = store) } }
                        },
                        launch {
                            catalogRepository.observeStoreProducts(storeId)
                                .distinctUntilChanged()
                                .collect { apps ->
                                    _uiState.update { current ->
                                        current.copy(developerApps = apps.filter { it.id != productId })
                                    }
                                }
                        },
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
                    downloadProgress = 0f,
                    installedPackage = when (outcome) {
                        is AppInstaller.Outcome.Installed -> outcome.packageName
                        is AppInstaller.Outcome.Launched -> outcome.packageName
                        else -> it.installedPackage
                    } ?: it.installedPackage,
                    message = when (outcome) {
                        is AppInstaller.Outcome.Installed -> "Installed. Open it below."
                        is AppInstaller.Outcome.Launched -> "Android is installing it now."
                        is AppInstaller.Outcome.NeedsPermission ->
                            "Allow Liminal to install apps, then come back to finish."
                        is AppInstaller.Outcome.Failed -> outcome.message
                    },
                    needsInstallPermission = outcome is AppInstaller.Outcome.NeedsPermission,
                )
            }
            when (outcome) {
                is AppInstaller.Outcome.Launched -> {
                    // Opened is not installed: wait for Android to finish, and only
                    // then record it. Cancelling the prompt no longer counts.
                    val pkg = outcome.packageName
                    if (pkg == null) {
                        (appLibraryRepository.recordInstall(product.id) as? DataResult.Failure)?.let { failure ->
                            _uiState.update { it.copy(message = failure.message) }
                        }
                    } else {
                        viewModelScope.launch {
                            val installed = appInstaller.awaitInstalled(pkg)
                            if (installed) {
                                (appLibraryRepository.recordInstall(product.id) as? DataResult.Failure)?.let { failure ->
                                    _uiState.update { it.copy(message = failure.message) }
                                }
                            } else {
                                _uiState.update {
                                    it.copy(message = "Install was not completed, so it was not recorded.")
                                }
                            }
                        }
                    }
                }
                is AppInstaller.Outcome.Installed -> {
                    // Silent install through Shizuku: record it straight away.
                    (appLibraryRepository.recordInstall(product.id) as? DataResult.Failure)?.let { failure ->
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

    /** Opens the app once Android has finished installing it. */
    fun openInstalledApp() {
        val pkg = _uiState.value.installedPackage ?: return
        if (!appInstaller.openInstalled(pkg)) {
            _uiState.update { it.copy(message = "Not installed yet — finish the Android prompt.") }
        }
    }

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

    /** Admin: delete the listing and its reviews. */
    fun adminRemove(onDone: () -> Unit = {}) {
        viewModelScope.launch {
            when (val result = adminRepository.removeProduct(productId)) {
                is DataResult.Failure -> _uiState.update { it.copy(message = result.message) }
                is DataResult.Success -> {
                    _uiState.update { it.copy(message = "App removed.") }
                    onDone()
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
