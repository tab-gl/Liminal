package app.tab.store.feature.profile

import app.tab.store.core.ui.Tokens
import androidx.annotation.DrawableRes

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import app.tab.store.core.ui.Surface
import app.tab.store.core.ui.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.tab.store.core.designsystem.PhosphorIcons
import app.tab.store.core.designsystem.SproutlyIcon
import app.tab.store.core.designsystem.Avatar
import app.tab.store.core.designsystem.SproutlyPalette
import app.tab.store.core.designsystem.SproutlyRadii
import app.tab.store.core.designsystem.SproutlyTag
import app.tab.store.feature.seller.DeveloperSwitchDialog

@Composable
fun ProfileScreen(
    onEditProfile: () -> Unit,
    onWishlist: () -> Unit,
    onOrders: () -> Unit,
    onNotifications: () -> Unit,
    onSellerHub: () -> Unit,
    onBecomeDeveloper: () -> Unit,
    onOpenSwitcher: () -> Unit,
    onLegal: (Boolean) -> Unit = {},
    onAdmin: () -> Unit,
    onSettings: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val user = state.user
    var showDeveloperPrompt by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            // The floating bar sits over the last row otherwise.
            .padding(bottom = 120.dp),
    ) {
        // Header panel in brand lime
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(SproutlyPalette.Lime)
                .padding(horizontal = 20.dp, vertical = 26.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpenSwitcher),
            ) {
                Avatar(url = user?.photoUrl, name = user?.displayName.orEmpty(), size = 66.dp)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        user?.displayName?.ifBlank { "Sproutly shopper" } ?: "Sproutly shopper",
                        style = Tokens.typography.headlineSmall,
                        color = SproutlyPalette.OliveDeep,
                    )
                    Text(
                        user?.email.orEmpty(),
                        style = Tokens.typography.bodySmall,
                        color = SproutlyPalette.Olive,
                    )
                    if (user?.role != null && user.role.name != "CUSTOMER") {
                        Spacer(Modifier.height(6.dp))
                        SproutlyTag(
                            text = user.role.name,
                            container = SproutlyPalette.Olive,
                            content = SproutlyPalette.Lime,
                        )
                    }
                }
                Surface(
                    onClick = onEditProfile,
                    shape = CircleShape,
                    color = SproutlyPalette.Olive,
                    modifier = Modifier.size(38.dp),
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        SproutlyIcon(PhosphorIcons.Edit,
                            contentDescription = "Edit profile",
                            tint = SproutlyPalette.Lime,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            StatTile("Installs", state.orderCount.toString(), Modifier.weight(1f))
            StatTile("Wishlist", state.wishlistCount.toString(), Modifier.weight(1f))
        }

        Column(Modifier.padding(horizontal = 20.dp)) {
            Text("Account", style = Tokens.typography.headlineSmall)
            Spacer(Modifier.height(8.dp))
            ProfileRow(PhosphorIcons.Receipt, "My apps", "Installed and updates", onOrders)
            ProfileRow(PhosphorIcons.Favourite, "Wishlist", "Saved for later", onWishlist)
            ProfileRow(PhosphorIcons.Notification, "Notifications", "Updates and offers", onNotifications)

            Spacer(Modifier.height(18.dp))
            Text("Publishing", style = Tokens.typography.headlineSmall)
            Spacer(Modifier.height(8.dp))
            ProfileRow(
                PhosphorIcons.Store,
                if (user?.isSeller == true) "Developer hub" else "Publish an app",
                if (user?.isSeller == true) "Manage your apps" else "Open a developer profile",
                onClick = {
                    // Publishing needs a developer account: ask first.
                    if (user?.isSeller == true) onSellerHub() else showDeveloperPrompt = true
                },
            )
            if (user?.isAdmin == true) {
                ProfileRow(
                    PhosphorIcons.Settings,
                    "Admin console",
                    "Moderate the store",
                    onAdmin,
                )
            }

            Spacer(Modifier.height(18.dp))
            Text("Preferences", style = Tokens.typography.headlineSmall)
            Spacer(Modifier.height(8.dp))
            ProfileRow(PhosphorIcons.Settings, "Settings", "App preferences") { showSettings = true }

            Spacer(Modifier.height(18.dp))
            Surface(
                onClick = viewModel::signOut,
                shape = SproutlyRadii.card,
                color = Tokens.colorScheme.surface,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SproutlyIcon(PhosphorIcons.ArrowRight,
                        contentDescription = null,
                        tint = Tokens.colorScheme.error,
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        if (state.isSigningOut) "Signing out…" else "Sign out",
                        style = Tokens.typography.titleSmall,
                        color = Tokens.colorScheme.error,
                    )
                }
            }
            Spacer(Modifier.height(28.dp))
            Text(
                "Sproutly · v${app.tab.store.BuildConfig.VERSION_NAME} " +
                    "(${app.tab.store.BuildConfig.VERSION_CODE})",
                style = Tokens.typography.bodySmall,
                color = Tokens.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(24.dp))
        }
    }

    if (showSettings) {
        SettingsSheet(
            onDismiss = { showSettings = false },
            onLegal = { terms ->
                showSettings = false
                onLegal(terms)
            },
            onClearCache = {
                // Coil keeps decoded images on disk; clearing is the only way to
                // reclaim it without uninstalling.
                coil.Coil.imageLoader(context).apply {
                    memoryCache?.clear()
                    diskCache?.clear()
                }
                android.widget.Toast
                    .makeText(context, "Image cache cleared.", android.widget.Toast.LENGTH_SHORT)
                    .show()
            },
            onFeedback = {
                val intent = android.content.Intent(android.content.Intent.ACTION_SENDTO).apply {
                    data = android.net.Uri.parse("mailto:support@sproutly.app")
                    putExtra(android.content.Intent.EXTRA_SUBJECT, "Sproutly feedback")
                }
                runCatching { context.startActivity(intent) }
            },
        )
    }

    if (showDeveloperPrompt) {
        DeveloperSwitchDialog(
            onDismiss = { showDeveloperPrompt = false },
            onConfirm = {
                showDeveloperPrompt = false
                onBecomeDeveloper()
            },
        )
    }
}

@Composable
private fun StatTile(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        shape = SproutlyRadii.card,
        color = Tokens.colorScheme.surface,
        modifier = modifier,
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(value, style = Tokens.typography.headlineMedium)
            Text(
                label,
                style = Tokens.typography.bodySmall,
                color = Tokens.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ProfileRow(@DrawableRes icon: Int, title: String, subtitle: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = SproutlyRadii.card,
        color = Tokens.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Tokens.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                SproutlyIcon(icon,
                    contentDescription = null,
                    tint = Tokens.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(19.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = Tokens.typography.titleSmall)
                Text(
                    subtitle,
                    style = Tokens.typography.bodySmall,
                    color = Tokens.colorScheme.onSurfaceVariant,
                )
            }
            SproutlyIcon(PhosphorIcons.ArrowRight,
                contentDescription = null,
                tint = Tokens.colorScheme.onSurfaceVariant,
            )
        }
    }
}
