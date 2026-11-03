package app.tab.store.feature.profile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.tab.store.core.designsystem.BrandMark
import app.tab.store.core.designsystem.SproutlyPalette
import app.tab.store.core.designsystem.SproutlyRadii
import app.tab.store.core.designsystem.SproutlyTopBar

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        SproutlyTopBar(title = "Settings", onBack = onBack)
        SettingsContent(state)
    }
}

/**
 * Settings as a sheet: it is reference information, not a destination, so it
 * rises over the profile instead of replacing it.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    onDismiss: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    androidx.compose.material3.ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = SproutlyRadii.sheet,
    ) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(bottom = 12.dp),
        ) {
            Text(
                "Settings",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
            )
            SettingsContent(state)
        }
    }
}

@Composable
private fun SettingsContent(state: ProfileUiState) {
        Column(Modifier.padding(horizontal = 20.dp)) {
            Surface(
                shape = SproutlyRadii.card,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(16.dp)) {
                    SettingRow("Theme", "Follows your device dark mode")
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = MaterialTheme.colorScheme.outlineVariant,
                    )
                    SettingRow("Account email", state.user?.email.orEmpty())
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = MaterialTheme.colorScheme.outlineVariant,
                    )
                    SettingRow("Role", state.user?.role?.name?.lowercase()?.replaceFirstChar { it.uppercase() }.orEmpty())
                }
            }

            Spacer(Modifier.height(18.dp))
            Surface(
                shape = SproutlyRadii.card,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row {
                        BrandMark(size = 30.dp)
                        Spacer(Modifier.height(10.dp))
                    }
                    Spacer(Modifier.height(10.dp))
                    Text("Sproutly ${app.tab.store.BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "The app store for independent developers. Built with Kotlin, Jetpack Compose and Firebase; app imagery is delivered from Cloudinary.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "Palette — Lime ${"#DBE6A1"} · Olive ${"#3F4C06"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = SproutlyPalette.OliveMid,
                    )
                }
            }
            Spacer(Modifier.height(28.dp))
        }
}

@Composable
private fun SettingRow(title: String, value: String) {
    Column {
        Text(title, style = MaterialTheme.typography.titleSmall)
        Text(
            value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
