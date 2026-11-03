package app.tab.store.feature.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.tab.store.core.designsystem.Avatar
import app.tab.store.core.designsystem.TablerIcon
import app.tab.store.core.designsystem.TablerIcons
import app.tab.store.core.designsystem.SproutlyRadii
import app.tab.store.domain.model.User

/** The three places a signed-in person can be. */
enum class Workspace(val label: String, val blurb: String) {
    SHOPPER("Shopper", "Browse and install apps"),
    DEVELOPER("Developer Studio", "Publish apps, releases and earnings"),
    ADMIN("Admin console", "Moderation, users and approvals"),
}

/**
 * Account switcher.
 *
 * One account can hold several workspaces — the same person shops, publishes
 * and (if they are an admin) moderates. Switching moves between them without
 * signing out; "Add another account" is for a genuinely different account, and
 * hands over to the sign-in screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountSwitcherSheet(
    user: User?,
    current: Workspace,
    onDismiss: () -> Unit,
    onSwitch: (Workspace) -> Unit,
    onAddAccount: () -> Unit,
    onSignOut: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = SproutlyRadii.sheet,
    ) {
        Column(Modifier.padding(horizontal = 20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Avatar(
                    name = user?.displayName ?: "Sproutly",
                    url = user?.photoUrl,
                    size = 46.dp,
                )
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(
                        user?.displayName?.ifBlank { "Sproutly account" } ?: "Sproutly account",
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                    )
                    Text(
                        user?.email.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
            }

            Spacer(Modifier.height(18.dp))
            Text(
                "Switch workspace",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))

            Workspace.entries.forEach { workspace ->
                val available = when (workspace) {
                    Workspace.SHOPPER -> true
                    Workspace.DEVELOPER -> user?.isSeller == true
                    Workspace.ADMIN -> user?.isAdmin == true
                }
                if (!available) return@forEach

                val selected = workspace == current
                Surface(
                    onClick = {
                        onDismiss()
                        if (!selected) onSwitch(workspace)
                    },
                    shape = SproutlyRadii.card,
                    color = if (selected) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TablerIcon(
                            icon = when (workspace) {
                                Workspace.SHOPPER -> TablerIcons.Store
                                Workspace.DEVELOPER -> TablerIcons.Sparkles
                                Workspace.ADMIN -> TablerIcons.Settings
                            },
                            contentDescription = null,
                            modifier = Modifier.size(22.dp),
                            tint = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                workspace.label,
                                style = MaterialTheme.typography.titleSmall,
                                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                                else MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                workspace.blurb,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (selected) {
                            TablerIcon(
                                icon = TablerIcons.Tick,
                                contentDescription = "Current workspace",
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(6.dp))

            SheetAction(TablerIcons.Plus, "Add another account", onAddAccount)
            SheetAction(TablerIcons.ArrowRight, "Sign out", onSignOut)

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SheetAction(icon: Int, label: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = SproutlyRadii.card,
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                TablerIcon(
                    icon = icon,
                    contentDescription = null,
                    modifier = Modifier.size(17.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(12.dp))
            Text(label, style = MaterialTheme.typography.titleSmall)
        }
    }
}
