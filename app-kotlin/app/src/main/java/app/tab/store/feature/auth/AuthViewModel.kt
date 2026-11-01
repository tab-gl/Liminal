package app.tab.store.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.tab.store.di.ServiceGraph
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthUiState(
    val busy: Boolean = false,
    val error: String? = null,
    val signedIn: Boolean = false,
)

class AuthViewModel : ViewModel() {

    private val auth = ServiceGraph.auth
    private val _state = MutableStateFlow(AuthUiState(signedIn = auth.uid != null))
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    fun signIn(email: String, password: String) = run {
        _state.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            runCatching { auth.signIn(email, password) }
                .onSuccess { _state.update { s -> s.copy(busy = false, signedIn = true) } }
                .onFailure { e -> _state.update { s -> s.copy(busy = false, error = e.friendly()) } }
        }
    }

    fun signUp(name: String, email: String, password: String) = run {
        _state.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            runCatching { auth.signUp(email, password, name) }
                .onSuccess { _state.update { s -> s.copy(busy = false, signedIn = true) } }
                .onFailure { e -> _state.update { s -> s.copy(busy = false, error = e.friendly()) } }
        }
    }

    private fun Throwable.friendly(): String {
        val raw = message ?: return "Sign-in failed. Try again."
        return when {
            raw.contains("password is invalid", true) || raw.contains("INVALID_LOGIN_CREDENTIALS") ->
                "Email or password is incorrect."
            raw.contains("no user record", true) -> "Email or password is incorrect."
            raw.contains("already in use", true) -> "An account already uses this email."
            raw.contains("network", true) -> "Sign-in failed because the network dropped."
            raw.contains("at least 6 characters", true) -> "Use a password of at least 8 characters."
            else -> raw
        }
    }
}
