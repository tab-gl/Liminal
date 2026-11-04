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
import app.tab.store.domain.repository.DataResult
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
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
