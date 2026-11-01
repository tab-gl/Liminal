package app.tab.store.feature.publish

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import app.tab.store.data.model.SproutlyCategories
import app.tab.store.ui.components.ButtonSize
import app.tab.store.ui.components.ButtonVariant
import app.tab.store.ui.components.SproutlyButton
import app.tab.store.ui.components.SproutlyCard
import app.tab.store.ui.components.SproutlyPill
import app.tab.store.ui.components.SproutlyTextButton
import app.tab.store.ui.theme.LocalSproutlyTokens

/**
 * Publishing, as one scrollable form.
 *
 * The Flutter build split this across five steps; on a phone a single form with
 * everything visible is fewer taps and fewer states to get wrong. Only name and
 * category are required - the same contract `submitApplication` enforces.
 */
@Composable
fun PublishScreen(
    onSubmitted: () -> Unit,
    bottomPadding: Dp = 0.dp,
    viewModel: PublishViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val pickIcon = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri -> if (uri != null) viewModel.update { it.copy(iconUri = uri) } }
    val pickScreenshots = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(8),
    ) { uris -> if (uris.isNotEmpty()) viewModel.update { it.copy(screenshotUris = uris) } }

    LaunchedEffect(state.submitted) {
        if (state.submitted) onSubmitted()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = bottomPadding + 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column {
                Text("Publish", style = MaterialTheme.typography.headlineSmall)
                Text(
                    text = "Free, forever. No listing fee, no revenue share.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item {
            SproutlyCard(color = LocalSproutlyTokens.current.heroSurface) {
                Text(
                    text = "Put your app in front of people who care",
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "A moderator reviews every listing, usually within a day. " +
                        "Only the name and a category are required.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (state.error != null) {
            item {
                Text(
                    text = state.error!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }

        item {
            OutlinedTextField(
                value = state.name,
                onValueChange = { value -> viewModel.update { it.copy(name = value) } },
                label = { Text("App name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            OutlinedTextField(
                value = state.tagline,
                onValueChange = { value -> viewModel.update { it.copy(tagline = value) } },
                label = { Text("Tagline (optional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            OutlinedTextField(
                value = state.description,
                onValueChange = { value -> viewModel.update { it.copy(description = value) } },
                label = { Text("Description (optional)") },
                minLines = 3,
                maxLines = 6,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SproutlyButton(
                    label = if (state.iconUri == null) "Choose icon" else "Icon selected",
                    variant = ButtonVariant.Outline,
                    size = ButtonSize.Small,
                    onClick = {
                        pickIcon.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                        )
                    },
                )
                SproutlyButton(
                    label = if (state.screenshotUris.isEmpty()) {
                        "Add screenshots"
                    } else {
                        "${state.screenshotUris.size} screenshots"
                    },
                    variant = ButtonVariant.Outline,
                    size = ButtonSize.Small,
                    onClick = {
                        pickScreenshots.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                        )
                    },
                )
            }
        }

        item {
            Column {
                Text("Category", style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    val chosen = SproutlyCategories.first { it.id == state.categoryId }
                    SproutlyPill(label = chosen.label)
                    SproutlyPill(label = "Tap to change")
                }
                Spacer(Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(SproutlyCategories, key = { it.id }) { category ->
                        SproutlyTextButton(
                            label = category.label,
                            onClick = { viewModel.update { it.copy(categoryId = category.id) } },
                        )
                    }
                }
            }
        }

        item {
            OutlinedTextField(
                value = state.websiteUrl,
                onValueChange = { value -> viewModel.update { it.copy(websiteUrl = value) } },
                label = { Text("Website (optional)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            OutlinedTextField(
                value = state.downloadUrl,
                onValueChange = { value -> viewModel.update { it.copy(downloadUrl = value) } },
                label = { Text("Download link (optional)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                modifier = Modifier.fillMaxWidth(),
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Open source", style = MaterialTheme.typography.bodyMedium)
                Switch(
                    checked = state.isOpenSource,
                    onCheckedChange = { value -> viewModel.update { it.copy(isOpenSource = value) } },
                )
            }
        }

        if (state.uploadNote != null) {
            item {
                Text(
                    text = state.uploadNote!!,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item {
            SproutlyButton(
                label = "Submit for review",
                onClick = { viewModel.submit(context) },
                loading = state.busy,
                fillWidth = true,
                size = ButtonSize.Large,
            )
        }
        item {
            SproutlyButton(
                label = "Save as draft",
                onClick = { viewModel.submit(context) },
                variant = ButtonVariant.Ghost,
                fillWidth = true,
            )
        }
    }
}
