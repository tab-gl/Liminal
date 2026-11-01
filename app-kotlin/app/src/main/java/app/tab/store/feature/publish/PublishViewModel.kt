package app.tab.store.feature.publish

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.tab.store.data.model.AppListing
import android.content.Context
import app.tab.store.di.ServiceGraph
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PublishUiState(
    val name: String = "",
    val tagline: String = "",
    val description: String = "",
    val categoryId: String = "productivity",
    val tags: List<String> = emptyList(),
    val websiteUrl: String = "",
    val downloadUrl: String = "",
    val pricingModel: String = "free",
    val isOpenSource: Boolean = false,
    val iconUri: Uri? = null,
    val screenshotUris: List<Uri> = emptyList(),
    val busy: Boolean = false,
    val uploadNote: String? = null,
    val error: String? = null,
    val submitted: Boolean = false,
) {
    /**
     * Only name and category are required. Tagline, description and links are
     * optional - the Cloud Function accepts a listing without them, and the
     * client should not be stricter than the server.
     */
    fun validate(): String? = when {
        name.trim().length < 2 -> "App name must be at least 2 characters."
        name.trim().length > 80 -> "App name must be 80 characters or fewer."
        tagline.length > 140 -> "Tagline must be 140 characters or fewer."
        websiteUrl.isNotEmpty() && !websiteUrl.startsWith("https://") ->
            "Links must start with https://."
        downloadUrl.isNotEmpty() && !downloadUrl.startsWith("https://") ->
            "Links must start with https://."
        else -> null
    }
}

class PublishViewModel : ViewModel() {

    private val repository = ServiceGraph.publishing
    private val _state = MutableStateFlow(PublishUiState())
    val state: StateFlow<PublishUiState> = _state.asStateFlow()

    fun update(transform: (PublishUiState) -> PublishUiState) = _state.update(transform)

    /**
     * Uploads first, then saves. Media is staged under the owner's uid until the
     * draft exists, which is exactly what `ownsStaging()` in storage.rules allows.
     */
    fun submit(context: Context) {
        val current = _state.value
        val problem = current.validate()
        if (problem != null) {
            _state.update { it.copy(error = problem) }
            return
        }
        val uid = ServiceGraph.auth.uid
        val profile = ServiceGraph.auth.currentUser
        if (uid == null) {
            _state.update { it.copy(error = "Sign in to publish.") }
            return
        }

        _state.update { it.copy(busy = true, error = null, uploadNote = "Uploading images…") }
        viewModelScope.launch {
            runCatching {
                var iconUrl: String? = null
                if (current.iconUri != null) {
                    iconUrl = ServiceGraph.media.upload(context, current.iconUri!!, uid, "icon")
                }
                val shotUrls = current.screenshotUris.map { uri ->
                    ServiceGraph.media.upload(context, uri, uid, "screenshot")
                }
                _state.update { it.copy(uploadNote = "Saving listing…") }

                val draft = AppListing(
                    name = current.name,
                    tagline = current.tagline,
                    description = current.description,
                    categoryId = current.categoryId,
                    tags = current.tags,
                    websiteUrl = current.websiteUrl.ifBlank { null },
                    downloadUrl = current.downloadUrl.ifBlank { null },
                    pricingModel = current.pricingModel,
                    isOpenSource = current.isOpenSource,
                    iconUrl = iconUrl,
                    screenshots = shotUrls,
                )
                val id = repository.saveDraft(
                    draft,
                    uid,
                    profile?.displayName ?: "Independent developer",
                )
                repository.submit(id)
            }.onSuccess {
                _state.update { it.copy(busy = false, submitted = true, uploadNote = null) }
            }.onFailure { error ->
                _state.update {
                    it.copy(busy = false, uploadNote = null, error = error.message ?: "Could not submit.")
                }
            }
        }
    }
}
