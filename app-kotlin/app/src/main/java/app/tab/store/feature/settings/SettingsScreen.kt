package app.tab.store.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.tab.store.di.ServiceGraph
import app.tab.store.install.InstallManager
import app.tab.store.ui.components.ButtonVariant
import app.tab.store.ui.components.SproutlyButton
import app.tab.store.ui.components.SproutlyCard
import app.tab.store.ui.components.SproutlyTextButton
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onSignOut: () -> Unit,
    bottomPadding: Dp = 0.dp,
) {
    val context = LocalContext.current
    var personalisedAds by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    LazyColumn(
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = bottomPadding + 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column {
                SproutlyTextButton(label = "Back", onClick = onBack)
                Text("Settings", style = MaterialTheme.typography.headlineSmall)
            }
        }

        item {
            SproutlyCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.fillMaxWidth(0.72f)) {
                        Text("Personalised ads", style = MaterialTheme.typography.titleSmall)
                        Text(
                            text = "Off means non-personalised requests. Ads still fund free publishing.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(checked = personalisedAds, onCheckedChange = { personalisedAds = it })
                }
            }
        }

        item {
            SproutlyCard {
                Column {
                    Text("Install permissions", style = MaterialTheme.typography.titleSmall)
                    Text(
                        text = "Android needs permission to install APKs from Sproutly. " +
                            "Apps from Google Play do not need it.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 4.dp),
                    )
                    SproutlyButton(
                        label = "Open install settings",
                        variant = ButtonVariant.Outline,
                        size = app.tab.store.ui.components.ButtonSize.Small,
                        onClick = { InstallManager.openInstallSettings(context) },
                    )
                }
            }
        }

        item {
            SproutlyCard {
                Column {
                    Text("Account", style = MaterialTheme.typography.titleSmall)
                    Text(
                        text = ServiceGraph.auth.currentUser?.email ?: "Not signed in",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 4.dp),
                    )
                }
            }
        }

        item {
            SproutlyButton(
                label = "Sign out",
                variant = ButtonVariant.Outline,
                fillWidth = true,
                onClick = {
                    scope.launch {
                        runCatching { ServiceGraph.auth.signOut() }
                        onSignOut()
                    }
                },
            )
        }
    }
}
