package app.tab.store.feature.seller

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import app.tab.store.core.ui.Surface
import app.tab.store.core.ui.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.tab.store.core.designsystem.SproutlyButton
import app.tab.store.core.designsystem.SproutlyImage
import app.tab.store.core.designsystem.SproutlyRadii
import app.tab.store.core.designsystem.SproutlySecondaryButton
import app.tab.store.core.designsystem.SproutlyTextField
import app.tab.store.core.designsystem.SproutlyTopBar
import app.tab.store.core.utils.pickImageRequest
import app.tab.store.core.utils.rememberImagePicker

@Composable
fun SellerStoreEditScreen(
    onBack: () -> Unit,
    viewModel: SellerStoreEditViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val logoPicker = rememberImagePicker { uri -> if (uri != null) viewModel.uploadLogo(uri) }
    val bannerPicker = rememberImagePicker { uri -> if (uri != null) viewModel.uploadBanner(uri) }

    LaunchedEffect(state.saved) {
        if (state.saved) onBack()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding(),
    ) {
        SproutlyTopBar(title = "Store profile", onBack = onBack)
        Column(Modifier.padding(horizontal = 20.dp)) {
            Box {
                SproutlyImage(
                    url = state.store.bannerUrl,
                    contentDescription = "Store banner",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    corner = 22.dp,
                )
                if (state.isUploadingBanner) {
                    Surface(
                        shape = SproutlyRadii.pill,
                        color = Tokens.colorScheme.surface,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(8.dp),
                    ) {
                        Text(
                            "Uploading…",
                            style = Tokens.typography.labelMedium,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        )
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                SproutlyImage(
                    url = state.store.logoUrl,
                    contentDescription = "Store logo",
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape),
                    corner = 32.dp,
                )
                Spacer(Modifier.size(10.dp))
                SproutlySecondaryButton(
                    text = "Change logo",
                    onClick = { logoPicker.launch(pickImageRequest()) },
                    enabled = !state.isUploadingLogo,
                )
            }
            Spacer(Modifier.height(8.dp))
            SproutlySecondaryButton(
                text = "Change banner",
                onClick = { bannerPicker.launch(pickImageRequest()) },
                enabled = !state.isUploadingBanner,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(18.dp))

            SproutlyTextField(
                value = state.store.name,
                onValueChange = { v -> viewModel.update { it.copy(name = v) } },
                label = "Store name",
            )
            SproutlyTextField(
                value = state.store.tagline,
                onValueChange = { v -> viewModel.update { it.copy(tagline = v.take(80)) } },
                label = "Tagline",
            )
            SproutlyTextField(
                value = state.store.description,
                onValueChange = { v -> viewModel.update { it.copy(description = v) } },
                label = "About the developer",
                singleLine = false,
                minLines = 3,
            )
            SproutlyTextField(
                value = state.store.city,
                onValueChange = { v -> viewModel.update { it.copy(city = v) } },
                label = "City",
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
                text = "Save store",
                onClick = viewModel::save,
                loading = state.isSaving,
            )
            Spacer(Modifier.height(32.dp))
        }
    }
}
