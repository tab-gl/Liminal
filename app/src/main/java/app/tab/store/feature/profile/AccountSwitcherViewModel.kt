package app.tab.store.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.tab.store.core.account.AccountRegistry
import app.tab.store.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AccountSwitcherState(
    val accounts: List<AccountRegistry.KnownAccount> = emptyList(),
    val currentUid: String? = null,
)

@HiltViewModel
class AccountSwitcherViewModel @Inject constructor(
    private val registry: AccountRegistry,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AccountSwitcherState())
    val uiState: StateFlow<AccountSwitcherState> = _uiState.asStateFlow()

    init {
        refresh()
        viewModelScope.launch {
            authRepository.currentUser.collect { user ->
                _uiState.update { it.copy(currentUid = user?.uid, accounts = registry.all()) }
            }
        }
    }

    private fun refresh() {
        _uiState.update { it.copy(accounts = registry.all()) }
    }

    /**
     * Ends this session so the provider can sign the chosen account in. The
     * address stays in the list, so there is nothing to type or remember.
     */
    fun selectAccount(uid: String) {
        if (uid == _uiState.value.currentUid) return
        viewModelScope.launch { authRepository.signOut() }
    }

    fun forgetAccount(uid: String) {
        registry.forget(uid)
        refresh()
    }

    fun addAccount() {
        viewModelScope.launch { authRepository.signOut() }
    }
}
