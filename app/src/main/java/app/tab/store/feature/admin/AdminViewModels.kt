package app.tab.store.feature.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.tab.store.domain.model.AdminOverview
import app.tab.store.domain.model.Product
import app.tab.store.domain.model.ProductStatus
import app.tab.store.domain.model.Store
import app.tab.store.domain.model.StoreStatus
import app.tab.store.domain.model.User
import app.tab.store.domain.model.UserRole
import app.tab.store.domain.repository.AdminRepository
import app.tab.store.domain.repository.CatalogRepository
import app.tab.store.domain.repository.DataResult
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class AdminDashboardUiState(
    val overview: AdminOverview = AdminOverview(),
    val pendingStores: List<Store> = emptyList(),
    val isLoading: Boolean = true,
)

@HiltViewModel
class AdminDashboardViewModel @Inject constructor(
    private val adminRepository: AdminRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminDashboardUiState())
    val uiState: StateFlow<AdminDashboardUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            adminRepository.observeOverview().collect { overview ->
                _uiState.update { it.copy(overview = overview, isLoading = false) }
            }
        }
        viewModelScope.launch {
            adminRepository.observePendingStores().collect { stores ->
                _uiState.update { it.copy(pendingStores = stores) }
            }
        }
    }

    fun approve(store: Store) {
        viewModelScope.launch { adminRepository.setStoreStatus(store.id, StoreStatus.ACTIVE) }
    }

    fun suspend(store: Store) {
        viewModelScope.launch { adminRepository.setStoreStatus(store.id, StoreStatus.SUSPENDED) }
    }
}

data class AdminStoresUiState(
    val stores: List<Store> = emptyList(),
    val pending: List<Store> = emptyList(),
    val isLoading: Boolean = true,
    val message: String? = null,
)

@HiltViewModel
class AdminStoresViewModel @Inject constructor(
    private val adminRepository: AdminRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminStoresUiState())
    val uiState: StateFlow<AdminStoresUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            adminRepository.observeAllStores().collect { stores ->
                _uiState.update { it.copy(stores = stores, isLoading = false) }
            }
        }
        viewModelScope.launch {
            adminRepository.observePendingStores().collect { pending ->
                _uiState.update { it.copy(pending = pending) }
            }
        }
    }

    /** Marks a studio as the store's own. Only an admin may do this. */
    fun setOfficial(store: Store, official: Boolean) {
        viewModelScope.launch {
            when (val result = adminRepository.setStoreOfficial(store.id, official)) {
                is DataResult.Failure -> _uiState.update { it.copy(message = result.message) }
                is DataResult.Success -> _uiState.update {
                    it.copy(
                        message = if (official) {
                            "${store.name} is now marked official."
                        } else {
                            "${store.name} is no longer marked official."
                        },
                    )
                }
            }
        }
    }

    fun setStatus(store: Store, status: StoreStatus) {
        viewModelScope.launch {
            val res = adminRepository.setStoreStatus(store.id, status)
            _uiState.update {
                it.copy(
                    message = when (res) {
                        is DataResult.Failure -> res.message
                        is DataResult.Success -> "${store.name} → ${status.name.lowercase()}"
                    },
                )
            }
        }
    }

    fun consumeMessage() = _uiState.update { it.copy(message = null) }
}

data class AdminUsersUiState(
    val users: List<User> = emptyList(),
    val isLoading: Boolean = true,
    val message: String? = null,
    val busyUid: String? = null,
)

@HiltViewModel
class AdminUsersViewModel @Inject constructor(
    private val adminRepository: AdminRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminUsersUiState())
    val uiState: StateFlow<AdminUsersUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            adminRepository.observeUsers().collect { users ->
                _uiState.update { it.copy(users = users, isLoading = false) }
            }
        }
    }

    fun setRole(user: User, role: UserRole) {
        viewModelScope.launch {
            _uiState.update { it.copy(busyUid = user.uid) }
            val res = adminRepository.setUserRole(user.uid, role)
            _uiState.update {
                it.copy(
                    busyUid = null,
                    message = when (res) {
                        is DataResult.Failure -> res.message
                        is DataResult.Success -> "Role updated"
                    },
                )
            }
        }
    }

    fun consumeMessage() = _uiState.update { it.copy(message = null) }
}

data class AdminProductsUiState(
    val reported: List<Product> = emptyList(),
    /** Listings a developer has submitted and nobody has approved yet. */
    val pending: List<Product> = emptyList(),
    val isLoading: Boolean = true,
    val message: String? = null,
)

@HiltViewModel
class AdminProductsViewModel @Inject constructor(
    private val adminRepository: AdminRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminProductsUiState())
    val uiState: StateFlow<AdminProductsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            adminRepository.observeReportedProducts().collect { products ->
                _uiState.update { it.copy(reported = products, isLoading = false) }
            }
        }
        viewModelScope.launch {
            adminRepository.observePendingProducts().collect { products ->
                _uiState.update { it.copy(pending = products) }
            }
        }
    }

    /** Approves a submitted listing, which is the only way one goes live now. */
    fun approve(product: Product) {
        viewModelScope.launch {
            when (val result = adminRepository.setProductStatus(product.id, ProductStatus.ACTIVE)) {
                is DataResult.Failure -> _uiState.update { it.copy(message = result.message) }
                is DataResult.Success -> _uiState.update {
                    it.copy(message = "${product.title} is live.")
                }
            }
        }
    }

    /** Clears a report: keep the app, or take it down. */
    fun resolve(product: Product, keepListed: Boolean) {
        viewModelScope.launch {
            when (val result = adminRepository.resolveReport(product.id, keepListed)) {
                is DataResult.Failure -> _uiState.update { it.copy(message = result.message) }
                is DataResult.Success -> _uiState.update {
                    it.copy(
                        message = if (keepListed) {
                            "${product.title} stays up; the report is dismissed."
                        } else {
                            "${product.title} was removed and the developer told."
                        },
                    )
                }
            }
        }
    }

    fun setStatus(product: Product, status: ProductStatus) {
        viewModelScope.launch {
            val res = adminRepository.setProductStatus(product.id, status)
            _uiState.update {
                it.copy(
                    message = when (res) {
                        is DataResult.Failure -> res.message
                        is DataResult.Success -> "App ${status.name.lowercase()}"
                    },
                )
            }
        }
    }

    fun consumeMessage() = _uiState.update { it.copy(message = null) }
}

data class AdminCategoriesUiState(
    val name: String = "",
    val slug: String = "",
    val isSaving: Boolean = false,
    val message: String? = null,
    val error: String? = null,
) {
    val canSave: Boolean get() = name.trim().length >= 2 && slug.isNotBlank() && !isSaving
}

@HiltViewModel
class AdminCategoriesViewModel @Inject constructor(
    private val adminRepository: AdminRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminCategoriesUiState())
    val uiState: StateFlow<AdminCategoriesUiState> = _uiState.asStateFlow()

    fun onName(value: String) = _uiState.update {
        it.copy(
            name = value,
            slug = app.tab.store.core.utils.Validators.slugify(value),
            error = null,
        )
    }

    fun onSlug(value: String) = _uiState.update { it.copy(slug = value) }

    /** One tap instead of a service-account key (orgs often block those). */
    fun seedDefaults() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            when (val res = adminRepository.seedDefaultCategories()) {
                is DataResult.Failure -> _uiState.update {
                    it.copy(isSaving = false, error = res.message)
                }
                is DataResult.Success -> _uiState.update {
                    it.copy(
                        isSaving = false,
                        name = "",
                        slug = "",
                        message = "${res.value} categories added",
                    )
                }
            }
        }
    }

    fun create() {
        val state = _uiState.value
        if (!state.canSave) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            when (val res = adminRepository.createCategory(state.name, state.slug, "leaf")) {
                is DataResult.Failure -> _uiState.update {
                    it.copy(isSaving = false, error = res.message)
                }
                is DataResult.Success -> _uiState.update {
                    it.copy(isSaving = false, name = "", slug = "", message = "Category created")
                }
            }
        }
    }

    fun consumeMessage() = _uiState.update { it.copy(message = null) }
}

data class AdminTagsUiState(
    val tags: List<Pair<String, Int>> = emptyList(),
    /** Category name to its document id, so rename and delete can address it. */
    val ids: Map<String, String> = emptyMap(),
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val editing: String? = null,
    val draft: String = "",
    val message: String? = null,
    val error: String? = null,
)

/**
 * Tag curation.
 *
 * Tags are written by whoever publishes an app, so the same idea arrives as
 * "rpg", "RPG" and "role playing". An admin can fold one into another or drop it
 * entirely, which is a batch edit across every listing that carries it.
 */
@HiltViewModel
class AdminTagsViewModel @Inject constructor(
    private val adminRepository: AdminRepository,
    // Categories are read through the catalogue; the admin repository only edits
    // them.
    private val catalogRepository: CatalogRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminTagsUiState())
    val uiState: StateFlow<AdminTagsUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            // Categories, not free-text tags: the shelf a listing sits on is what a
            // shopper filters by, and it is what needs curating.
            val categories = catalogRepository.observeCategories().first()
            _uiState.update {
                it.copy(
                    isLoading = false,
                    tags = categories.map { category -> category.name to category.productCount },
                    ids = categories.associate { category -> category.name to category.id },
                )
            }
        }
    }

    fun startEdit(tag: String) = _uiState.update { it.copy(editing = tag, draft = tag) }
    fun onDraft(value: String) = _uiState.update { it.copy(draft = value) }
    fun cancelEdit() = _uiState.update { it.copy(editing = null, draft = "") }

    fun saveEdit() {
        val state = _uiState.value
        val from = state.editing ?: return
        val to = state.draft.trim()
        if (to.isBlank() || to.equals(from, ignoreCase = true)) {
            cancelEdit()
            return
        }
        val categoryId = state.ids[from] ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            when (val res = adminRepository.renameCategory(categoryId, to)) {
                is DataResult.Failure -> _uiState.update {
                    it.copy(isSaving = false, error = res.message)
                }
                is DataResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            editing = null,
                            draft = "",
                            message = "\"$from\" is now \"$to\" on ${res.value} listing(s).",
                        )
                    }
                    refresh()
                }
            }
        }
    }

    fun delete(tag: String) {
        val categoryId = _uiState.value.ids[tag] ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            when (val res = adminRepository.deleteCategory(categoryId)) {
                is DataResult.Failure -> _uiState.update {
                    it.copy(isSaving = false, error = res.message)
                }
                is DataResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            message = "\"$tag\" removed from ${res.value} listing(s).",
                        )
                    }
                    refresh()
                }
            }
        }
    }

    fun consumeMessage() = _uiState.update { it.copy(message = null, error = null) }
}
