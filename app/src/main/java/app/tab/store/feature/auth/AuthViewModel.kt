package app.tab.store.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.tab.store.core.utils.Validators
import app.tab.store.domain.model.User
import app.tab.store.core.account.PendingAccountSwitch
import app.tab.store.domain.repository.AuthRepository
import app.tab.store.domain.repository.DataResult
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class AuthUiState(
    val user: User? = null,
    val isRestoringSession: Boolean = true,
    val isLoading: Boolean = false,
    val error: String? = null,
    val info: String? = null,
    val email: String = "",
    val password: String = "",
    val name: String = "",
    val passwordVisible: Boolean = false,
) {
    val canSubmitLogin: Boolean
        get() = Validators.isValidEmail(email) && password.isNotEmpty() && !isLoading

    val canSubmitRegister: Boolean
        get() = name.trim().length >= 2 && Validators.isValidEmail(email) &&
            Validators.isValidPassword(password) && !isLoading
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    pendingSwitch: PendingAccountSwitch,
) : ViewModel() {

    // A cached session means "hold the UI until the profile arrives"; no cached
    // session means "show sign-in now". Guessing wrong is what flashed the login
    // screen at people who were already signed in.
    private val _uiState = MutableStateFlow(
        AuthUiState(
            isRestoringSession = authRepository.hasCachedSession(),
            // Arriving here from the account switcher: the address is already
            // known, so only the password is left to type.
            email = pendingSwitch.take().orEmpty(),
        ),
    )
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    val session: StateFlow<User?> = authRepository.currentUser
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    init {
        viewModelScope.launch {
            authRepository.currentUser.collect { user ->
                _uiState.update { it.copy(user = user, isRestoringSession = false) }
            }
        }
    }

    suspend fun awaitSession() {
        // Give the profile a moment to arrive, but never hold the UI forever.
        withTimeoutOrNull(2_000) {
            authRepository.currentUser.first { it != null }
        }
        _uiState.update { it.copy(isRestoringSession = false) }
    }

    fun onEmailChange(value: String) = _uiState.update { it.copy(email = value, error = null) }
    fun onPasswordChange(value: String) = _uiState.update { it.copy(password = value, error = null) }
    fun onNameChange(value: String) = _uiState.update { it.copy(name = value, error = null) }
    fun togglePasswordVisibility() = _uiState.update { it.copy(passwordVisible = !it.passwordVisible) }
    fun clearMessages() = _uiState.update { it.copy(error = null, info = null) }

    /** Surfaces an error raised outside the form (e.g. Google sign-in). */
    fun showError(message: String) = _uiState.update { it.copy(error = message) }

    fun signIn() {
        val state = _uiState.value
        when {
            state.email.isBlank() ->
                return _uiState.update { it.copy(error = "Enter your email address.") }
            !Validators.isValidEmail(state.email) ->
                return _uiState.update { it.copy(error = "That email address does not look right.") }
            state.password.isBlank() ->
                return _uiState.update { it.copy(error = "Enter your password.") }
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val res = authRepository.signInWithEmail(state.email, state.password)) {
                is DataResult.Failure -> _uiState.update {
                    it.copy(isLoading = false, error = res.message)
                }
                is DataResult.Success -> _uiState.update {
                    it.copy(isLoading = false, user = res.value, password = "")
                }
            }
        }
    }

    fun register() {
        val state = _uiState.value
        when {
            state.name.trim().length < 2 ->
                return _uiState.update { it.copy(error = "Enter your name.") }
            !Validators.isValidEmail(state.email) ->
                return _uiState.update { it.copy(error = "That email address does not look right.") }
            !Validators.isValidPassword(state.password) ->
                return _uiState.update { it.copy(error = "Use a password of at least 8 characters.") }
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val res = authRepository.registerWithEmail(state.name, state.email, state.password)) {
                is DataResult.Failure -> _uiState.update {
                    it.copy(isLoading = false, error = res.message)
                }
                is DataResult.Success -> _uiState.update {
                    it.copy(isLoading = false, user = res.value, password = "")
                }
            }
        }
    }

    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val res = authRepository.signInWithGoogle(idToken)) {
                is DataResult.Failure -> _uiState.update {
                    it.copy(isLoading = false, error = res.message)
                }
                is DataResult.Success -> _uiState.update {
                    it.copy(isLoading = false, user = res.value)
                }
            }
        }
    }

    fun sendPasswordReset() {
        val email = _uiState.value.email
        if (!Validators.isValidEmail(email)) {
            _uiState.update { it.copy(error = "Enter a valid email address.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, info = null) }
            when (val res = authRepository.sendPasswordReset(email)) {
                is DataResult.Failure -> _uiState.update {
                    it.copy(isLoading = false, error = res.message)
                }
                is DataResult.Success -> _uiState.update {
                    it.copy(
                        isLoading = false,
                        info = "Reset link sent. Check ${email.trim()} for the next step.",
                    )
                }
            }
        }
    }

    fun signOut() {
        viewModelScope.launch { authRepository.signOut() }
    }
}
