package app.tab.store.feature.profile

import app.tab.store.core.ui.Tokens
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoCamera
import app.tab.store.core.ui.Surface
import app.tab.store.core.ui.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.tab.store.core.designsystem.Avatar
import app.tab.store.core.designsystem.SproutlyButton
import app.tab.store.core.designsystem.SproutlyTextField
import app.tab.store.core.designsystem.SproutlyTopBar
import app.tab.store.core.designsystem.UploadProgressBar
import app.tab.store.core.utils.pickImageRequest
import app.tab.store.core.designsystem.SproutlyIcon
import app.tab.store.core.designsystem.PhosphorIcons
import app.tab.store.core.utils.rememberImagePicker

@Composable
fun ProfileEditScreen(
    onBack: () -> Unit,
    viewModel: ProfileEditViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val picker = rememberImagePicker { uri ->
        if (uri != null) viewModel.onPhotoPicked(uri)
    }

    LaunchedEffect(state.saved) {
        if (state.saved) onBack()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding(),
    ) {
        SproutlyTopBar(title = "Edit profile", onBack = onBack)
        Column(Modifier.padding(horizontal = 24.dp)) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Box {
                    Avatar(
                        url = state.photoUrl,
                        name = state.displayName,
                        size = 104.dp,
                    )
                    Surface(
                        onClick = { picker.launch(pickImageRequest()) },
                        shape = CircleShape,
                        color = Tokens.colorScheme.primary,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(2.dp),
                    ) {
                        Box(Modifier.padding(8.dp)) {
                            SproutlyIcon(PhosphorIcons.Image,
                                contentDescription = "Change photo",
                                tint = Tokens.colorScheme.onPrimary,
                                modifier = Modifier.height(18.dp),
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            if (state.isUploading) {
                UploadProgressBar(
                    fraction = state.uploadProgress,
                    modifier = Modifier.padding(horizontal = 40.dp),
                )
                Spacer(Modifier.height(10.dp))
            }
            Spacer(Modifier.height(14.dp))

            SproutlyTextField(
                value = state.displayName,
                onValueChange = viewModel::onNameChange,
                label = "Full name",
            )

            if (state.error != null) {
                Surface(
                    color = Tokens.colorScheme.errorContainer,
                    shape = Tokens.shapes.small,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        state.error.orEmpty(),
                        style = Tokens.typography.bodySmall,
                        color = Tokens.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(12.dp),
                    )
                }
                Spacer(Modifier.height(12.dp))
            }

            SproutlyButton(
                text = "Save changes",
                onClick = viewModel::save,
                enabled = state.canSave,
                loading = state.isSaving,
            )
            Spacer(Modifier.height(32.dp))
        }
    }
}
