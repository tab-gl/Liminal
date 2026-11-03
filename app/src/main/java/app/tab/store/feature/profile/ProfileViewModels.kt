package app.tab.store.feature.profile

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.tab.store.core.utils.Validators
import app.tab.store.domain.model.User
import app.tab.store.domain.repository.AuthRepository
import app.tab.store.domain.repository.DataResult
import app.tab.store.domain.repository.MediaRepository
import app.tab.store.domain.repository.OrderRepository
import app.tab.store.domain.repository.ProfileRepository
import app.tab.store.domain.repository.WishlistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileUiState(
    val user: User? = null,
    val orderCount: Int = 0,
    val wishlistCount: Int = 0,
    val isSigningOut: Boolean = false,
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    orderRepository: OrderRepository,
    wishlistRepository: WishlistRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            authRepository.currentUser.collect { user ->
                _uiState.update { it.copy(user = user) }
            }
        }
        viewModelScope.launch {
            orderRepository.observeBuyerOrders().collect { orders ->
                _uiState.update { it.copy(orderCount = orders.size) }
            }
        }
        viewModelScope.launch {
            wishlistRepository.observeWishlist().collect { items ->
                _uiState.update { it.copy(wishlistCount = items.size) }
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSigningOut = true) }
            authRepository.signOut()
            _uiState.update { it.copy(isSigningOut = false) }
        }
    }
}

data class ProfileEditUiState(
    val displayName: String = "",
    val phone: String = "",
    val photoUrl: String? = null,
    val photoPublicId: String? = null,
    val isUploading: Boolean = false,
    val uploadProgress: Float = 0f,
    val isSaving: Boolean = false,
    val error: String? = null,
    val saved: Boolean = false,
    val loaded: Boolean = false,
) {
    val canSave: Boolean
        get() = displayName.trim().length >= 2 && !isSaving && !isUploading
}

@HiltViewModel
class ProfileEditViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val authRepository: AuthRepository,
    private val mediaRepository: MediaRepository,
    private val uploader: app.tab.store.core.media.CloudinaryUploader,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileEditUiState())
    val uiState: StateFlow<ProfileEditUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            authRepository.currentUser.collect { user ->
                if (user != null && !_uiState.value.loaded) {
                    _uiState.update {
                        it.copy(
                            displayName = user.displayName,
                            phone = user.phone.orEmpty(),
                            photoUrl = user.photoUrl,
                            loaded = true,
                        )
                    }
                }
            }
        }
    }

    fun onNameChange(value: String) = _uiState.update { it.copy(displayName = value, error = null) }
    fun onPhoneChange(value: String) = _uiState.update {
        it.copy(phone = value.filter { c -> c.isDigit() }.take(13), error = null)
    }

    fun onPhotoPicked(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isUploading = true, uploadProgress = 0f, error = null) }
            val bytes = uploader.prepare(uri)
            if (bytes == null) {
                _uiState.update { it.copy(isUploading = false, error = "Could not read that image.") }
                return@launch
            }
            when (
                val res = mediaRepository.upload(
                    bytes = bytes,
                    fileName = "avatar.jpg",
                    folder = "sproutly/avatars",
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
                        photoUrl = res.value.url,
                        photoPublicId = res.value.publicId,
                    )
                }
            }
        }
    }

    fun save() {
        val state = _uiState.value
        if (!state.canSave) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            when (
                val res = profileRepository.updateProfile(
                    displayName = state.displayName,
                    phone = state.phone.takeIf { Validators.isValidPhone(it) },
                    photoUrl = state.photoUrl,
                    photoPublicId = state.photoPublicId,
                )
            ) {
                is DataResult.Failure -> _uiState.update {
                    it.copy(isSaving = false, error = res.message)
                }
                is DataResult.Success -> _uiState.update { it.copy(isSaving = false, saved = true) }
            }
        }
    }
}


