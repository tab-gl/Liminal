package app.tab.store.feature.profile

import app.tab.store.core.ui.Tokens
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import app.tab.store.core.ui.ExperimentalMaterial3Api
import app.tab.store.core.ui.HorizontalDivider
import app.tab.store.core.ui.DropdownMenu
import app.tab.store.core.ui.DropdownMenuItem
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import app.tab.store.core.ui.AlertDialog
import app.tab.store.core.ui.TextButton
import app.tab.store.core.ui.ModalBottomSheet
import app.tab.store.core.ui.Surface
import app.tab.store.core.ui.Text
import app.tab.store.core.ui.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.tab.store.core.account.AccountRegistry
import app.tab.store.core.designsystem.Avatar
import app.tab.store.core.designsystem.SproutlyRadii
import app.tab.store.core.designsystem.SproutlyIcon
import app.tab.store.core.designsystem.PhosphorIcons

/** The three places a signed-in person can be. */
enum class Workspace(val label: String, val blurb: String) {
    SHOPPER("Shopper", "Browse and install apps"),
    DEVELOPER("Developer Studio", "Publish apps, releases and earnings"),
    ADMIN("Admin console", "Moderation, users and approvals"),
}

/**
 * Account switcher.
 *
 * Every account that has signed in on this device is listed with its name and
 * photo, so switching is a tap rather than a fresh sign-in form. Firebase keeps
 * one live session, so choosing another account ends this one and hands over to
 * the provider's picker — but you never have to remember which address it was.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountSwitcherSheet(
    accounts: List<AccountRegistry.KnownAccount>,
    currentUid: String?,
    currentWorkspace: Workspace,
    isSeller: Boolean,
    isAdmin: Boolean,
    onDismiss: () -> Unit,
    onSelectAccount: (String) -> Unit,
    onForgetAccount: (String) -> Unit,
    onSwitchWorkspace: (Workspace) -> Unit,
    onAddAccount: () -> Unit,
    onSignOut: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var menuFor by remember { mutableStateOf<String?>(null) }
    var confirm by remember { mutableStateOf<AccountRegistry.KnownAccount?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Tokens.colorScheme.surface,
        shape = SproutlyRadii.sheet,
    ) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            Text(
                "Accounts",
                style = Tokens.typography.titleMedium,
            )
            Spacer(Modifier.height(10.dp))

            if (accounts.isEmpty()) {
                Text(
                    "No other accounts on this device yet.",
                    style = Tokens.typography.bodySmall,
                    color = Tokens.colorScheme.onSurfaceVariant,
                )
            }

            accounts.forEach { account ->
                val isCurrent = account.uid == currentUid
                Surface(
                    onClick = {
                        if (isCurrent) return@Surface
                        // Google accounts swap through the provider's picker with
                        // no password; anything else has to sign out first, so ask
                        // before doing that to someone.
                        val isGoogle = account.provider.contains("google", ignoreCase = true)
                        if (isGoogle) onSelectAccount(account.uid) else confirm = account
                    },
                    shape = SproutlyRadii.card,
                    color = if (isCurrent) Tokens.colorScheme.primaryContainer
                    else Tokens.colorScheme.surfaceContainerLow,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Avatar(
                            url = account.photoUrl,
                            name = account.displayName.ifBlank { account.email },
                            size = 38.dp,
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                account.displayName.ifBlank { account.email },
                                style = Tokens.typography.titleSmall,
                                maxLines = 1,
                                color = if (isCurrent) Tokens.colorScheme.onPrimaryContainer
                                else Tokens.colorScheme.onSurface,
                            )
                            Text(
                                account.email,
                                style = Tokens.typography.bodySmall,
                                maxLines = 1,
                                color = if (isCurrent) Tokens.colorScheme.onPrimaryContainer
                                else Tokens.colorScheme.onSurfaceVariant,
                            )
                            if (!isCurrent) {
                                Text(
                                    "Tap to switch",
                                    style = Tokens.typography.labelSmall,
                                    color = Tokens.colorScheme.primary,
                                )
                            }
                        }
                        if (isCurrent) {
                            SproutlyIcon(
                                icon = PhosphorIcons.Tick,
                                contentDescription = "Signed in",
                                modifier = Modifier.size(18.dp),
                                tint = Tokens.colorScheme.onPrimaryContainer,
                            )
                        } else {
                            Box {
                                Surface(
                                    onClick = { menuFor = account.uid },
                                    shape = CircleShape,
                                    color = Tokens.colorScheme.surface,
                                    modifier = Modifier.size(28.dp),
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        SproutlyIcon(
                                            icon = PhosphorIcons.More,
                                            contentDescription = "Options for ${account.email}",
                                            modifier = Modifier.size(16.dp),
                                            tint = Tokens.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                                DropdownMenu(
                                    expanded = menuFor == account.uid,
                                    onDismissRequest = { menuFor = null },
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Remove from this device") },
                                        onClick = {
                                            menuFor = null
                                            onForgetAccount(account.uid)
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            }

            SheetAction(PhosphorIcons.Plus, "Add another account", onAddAccount)

            Spacer(Modifier.height(8.dp))
            HorizontalDivider(color = Tokens.colorScheme.outlineVariant)
            Spacer(Modifier.height(12.dp))

            Text("Workspace", style = Tokens.typography.titleMedium)
            Spacer(Modifier.height(8.dp))

            Workspace.entries.forEach { workspace ->
                val available = when (workspace) {
                    Workspace.SHOPPER -> true
                    Workspace.DEVELOPER -> isSeller
                    Workspace.ADMIN -> isAdmin
                }
                if (!available) return@forEach

                val selected = workspace == currentWorkspace
                Surface(
                    onClick = {
                        onDismiss()
                        if (!selected) onSwitchWorkspace(workspace)
                    },
                    shape = SproutlyRadii.card,
                    color = if (selected) Tokens.colorScheme.primaryContainer
                    else Tokens.colorScheme.surfaceContainerLow,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        SproutlyIcon(
                            icon = when (workspace) {
                                Workspace.SHOPPER -> PhosphorIcons.Store
                                Workspace.DEVELOPER -> PhosphorIcons.Sparkles
                                Workspace.ADMIN -> PhosphorIcons.Settings
                            },
                            contentDescription = null,
                            modifier = Modifier.size(22.dp),
                            tint = if (selected) Tokens.colorScheme.onPrimaryContainer
                            else Tokens.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                workspace.label,
                                style = Tokens.typography.titleSmall,
                                color = if (selected) Tokens.colorScheme.onPrimaryContainer
                                else Tokens.colorScheme.onSurface,
                            )
                            Text(
                                workspace.blurb,
                                style = Tokens.typography.bodySmall,
                                color = if (selected) Tokens.colorScheme.onPrimaryContainer
                                else Tokens.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (selected) {
                            SproutlyIcon(
                                icon = PhosphorIcons.Tick,
                                contentDescription = "Current workspace",
                                modifier = Modifier.size(20.dp),
                                tint = Tokens.colorScheme.onPrimaryContainer,
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            HorizontalDivider(color = Tokens.colorScheme.outlineVariant)
            Spacer(Modifier.height(6.dp))
            SheetAction(PhosphorIcons.ArrowRight, "Sign out", onSignOut)

            Spacer(Modifier.height(24.dp))
        }
    }

    confirm?.let { account ->
        AlertDialog(
            onDismissRequest = { confirm = null },
            title = { Text("Switch to ${account.email}?") },
            text = {
                Text(
                    "This account signs in with a password, so switching means " +
                        "signing out first. We will keep the address filled in for you.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val target = account.uid
                    confirm = null
                    onSelectAccount(target)
                }) { Text("Sign out and continue") }
            },
            dismissButton = {
                TextButton(onClick = { confirm = null }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun SheetAction(icon: Int, label: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = SproutlyRadii.card,
        color = Tokens.colorScheme.surface,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .background(Tokens.colorScheme.surfaceContainerHigh, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                SproutlyIcon(
                    icon = icon,
                    contentDescription = null,
                    modifier = Modifier.size(17.dp),
                    tint = Tokens.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(12.dp))
            Text(label, style = Tokens.typography.titleSmall)
        }
    }
}
