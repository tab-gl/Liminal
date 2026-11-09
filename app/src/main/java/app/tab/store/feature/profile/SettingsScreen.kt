package app.tab.store.feature.profile

import app.tab.store.core.ui.Tokens
import androidx.compose.foundation.layout.Arrangement
import app.tab.store.BuildConfig
import app.tab.store.core.designsystem.SproutlyChip
import androidx.compose.foundation.clickable
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.size
import app.tab.store.core.designsystem.SproutlyIcon
import app.tab.store.core.designsystem.PhosphorIcons
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import app.tab.store.core.ui.HorizontalDivider
import app.tab.store.core.ui.Surface
import app.tab.store.core.ui.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.tab.store.core.theme.AccentOption
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.background
import app.tab.store.core.theme.ThemeMode
import app.tab.store.core.designsystem.BrandMark
import app.tab.store.core.designsystem.SproutlyRadii
import app.tab.store.core.designsystem.SproutlyTopBar

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onLegal: (Boolean) -> Unit = {},
    onClearCache: () -> Unit = {},
    onFeedback: () -> Unit = {},
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val lastCrash = remember { app.tab.store.core.utils.CrashLogHolder.last }
    val themeViewModel: ThemeViewModel = hiltViewModel()
    val themeMode by themeViewModel.mode.collectAsStateWithLifecycle()
    val accent by themeViewModel.accent.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        SproutlyTopBar(title = "Settings", onBack = onBack)
        SettingsContent(
            state = state,
            themeMode = themeMode,
            onTheme = themeViewModel::set,
            accent = accent,
            onAccent = themeViewModel::setAccent,
            onLegal = onLegal,
            onClearCache = onClearCache,
            onFeedback = onFeedback,
        )
    }
}

/**
 * Settings as a sheet: it is reference information, not a destination, so it
 * rises over the profile instead of replacing it.
 */
@OptIn(app.tab.store.core.ui.ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    onDismiss: () -> Unit,
    onLegal: (Boolean) -> Unit = {},
    onClearCache: () -> Unit = {},
    onFeedback: () -> Unit = {},
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val lastCrash = remember { app.tab.store.core.utils.CrashLogHolder.last }
    val themeViewModel: ThemeViewModel = hiltViewModel()
    val themeMode by themeViewModel.mode.collectAsStateWithLifecycle()
    val accent by themeViewModel.accent.collectAsStateWithLifecycle()
    app.tab.store.core.ui.ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Tokens.colorScheme.surface,
        shape = SproutlyRadii.sheet,
    ) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(bottom = 12.dp),
        ) {
            Text(
                "Settings",
                style = Tokens.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
            )
            SettingsContent(
            state = state,
            themeMode = themeMode,
            onTheme = themeViewModel::set,
            accent = accent,
            onAccent = themeViewModel::setAccent,
            onLegal = onLegal,
            onClearCache = onClearCache,
            onFeedback = onFeedback,
        )
        }
    }
}

@Composable
private fun SettingsContent(
    state: ProfileUiState,
    themeMode: ThemeMode,
    onTheme: (ThemeMode) -> Unit,
    accent: AccentOption,
    onAccent: (AccentOption) -> Unit,
    onLegal: (Boolean) -> Unit,
    onClearCache: () -> Unit,
    onFeedback: () -> Unit,
) {
    val lastCrash = remember { app.tab.store.core.utils.CrashLogHolder.last }
        Column(Modifier.padding(horizontal = 20.dp)) {
            Surface(
                shape = SproutlyRadii.card,
                color = Tokens.colorScheme.surface,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Theme", style = Tokens.typography.titleSmall)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ThemeMode.entries.forEach { option ->
                            SproutlyChip(
                                text = option.label,
                                selected = themeMode == option,
                                onClick = { onTheme(option) },
                            )
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    Text("Accent", style = Tokens.typography.titleSmall)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "The app is monochrome; this is the one colour it uses.",
                        style = Tokens.typography.bodySmall,
                        color = Tokens.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                    // Swatches rather than words: an accent is a colour, and a chip
                    // that says "Indigo" tells you less than a dot of it.
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(AccentOption.entries.toList()) { option ->
                            val swatch = when (option) {
                                AccentOption.MONET -> Tokens.colorScheme.primary
                                else -> androidx.compose.ui.graphics.Color(option.hex or 0xFF000000L)
                            }
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(androidx.compose.foundation.shape.CircleShape)
                                    .background(swatch)
                                    .then(
                                        if (accent == option) {
                                            Modifier.border(
                                                3.dp,
                                                Tokens.colorScheme.onSurface,
                                                androidx.compose.foundation.shape.CircleShape,
                                            )
                                        } else {
                                            Modifier
                                        },
                                    )
                                    .clickable { onAccent(option) },
                                contentAlignment = Alignment.Center,
                            ) {
                                if (option == AccentOption.MONET) {
                                    SproutlyIcon(
                                        PhosphorIcons.Sparkles,
                                        contentDescription = "Match wallpaper",
                                        modifier = Modifier.size(16.dp),
                                        tint = Tokens.colorScheme.onPrimary,
                                    )
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        accent.label,
                        style = Tokens.typography.labelMedium,
                        color = Tokens.colorScheme.onSurfaceVariant,
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = Tokens.colorScheme.outlineVariant,
                    )
                    SettingRow("Account email", state.user?.email.orEmpty())
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = Tokens.colorScheme.outlineVariant,
                    )
                    SettingRow("Role", state.user?.role?.name?.lowercase()?.replaceFirstChar { it.uppercase() }.orEmpty())
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = Tokens.colorScheme.outlineVariant,
                    )
                    SettingRow("App version", BuildConfig.VERSION_NAME)
                    // The audit found the upload mode was never visible anywhere,
                    // so nobody could tell which one a build used.
                    SettingRow("Image uploads", app.tab.store.BuildConfig.CLOUDINARY_MODE)
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = Tokens.colorScheme.outlineVariant,
                    )
                    SettingLink("Clear image cache") { onClearCache() }
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = Tokens.colorScheme.outlineVariant,
                    )
                    SettingLink("Send feedback") { onFeedback() }
                    if (!lastCrash.isNullOrBlank()) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = Tokens.colorScheme.outlineVariant,
                        )
                        Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            Text("Last crash", style = Tokens.typography.titleSmall)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                lastCrash,
                                style = Tokens.typography.bodySmall,
                                color = Tokens.colorScheme.onSurfaceVariant,
                                maxLines = 8,
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(18.dp))
            Surface(
                shape = SproutlyRadii.card,
                color = Tokens.colorScheme.surface,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(16.dp)) {
                    SettingLink("Privacy policy") { onLegal(false) }
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = Tokens.colorScheme.outlineVariant,
                    )
                    SettingLink("Terms of service") { onLegal(true) }
                }
            }

            Spacer(Modifier.height(18.dp))
            Surface(
                shape = SproutlyRadii.card,
                color = Tokens.colorScheme.surface,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row {
                        BrandMark(size = 30.dp)
                        Spacer(Modifier.height(10.dp))
                    }
                    Spacer(Modifier.height(10.dp))
                    Text("Liminal ${app.tab.store.BuildConfig.VERSION_NAME}", style = Tokens.typography.titleSmall)
                    Text(
                        "An app store for independent developers.",
                        style = Tokens.typography.bodySmall,
                        color = Tokens.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(28.dp))
        }
}

@Composable
private fun SettingRow(title: String, value: String) {
    Column {
        Text(title, style = Tokens.typography.titleSmall)
        Text(
            value,
            style = Tokens.typography.bodySmall,
            color = Tokens.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SettingLink(title: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = Tokens.typography.titleSmall, modifier = Modifier.weight(1f))
        SproutlyIcon(
            PhosphorIcons.ChevronRight,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = Tokens.colorScheme.onSurfaceVariant,
        )
    }
}
