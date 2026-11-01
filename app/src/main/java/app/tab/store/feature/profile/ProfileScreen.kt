package app.tab.store.feature.profile

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.tab.store.core.designsystem.Avatar
import app.tab.store.core.designsystem.SproutlyPalette
import app.tab.store.core.designsystem.SproutlyRadii
import app.tab.store.core.designsystem.SproutlyTag

@Composable
fun ProfileScreen(
    onEditProfile: () -> Unit,
    onAddresses: () -> Unit,
    onWishlist: () -> Unit,
    onOrders: () -> Unit,
    onNotifications: () -> Unit,
    onSellerHub: () -> Unit,
    onAdmin: () -> Unit,
    onSettings: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val user = state.user

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        // Header panel in brand lime
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(SproutlyPalette.Lime)
                .padding(horizontal = 20.dp, vertical = 26.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Avatar(url = user?.photoUrl, name = user?.displayName.orEmpty(), size = 66.dp)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        user?.displayName?.ifBlank { "Sproutly shopper" } ?: "Sproutly shopper",
                        style = MaterialTheme.typography.headlineSmall,
                        color = SproutlyPalette.OliveDeep,
                    )
                    Text(
                        user?.email.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
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
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Filled.Edit,
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
            StatTile("Orders", state.orderCount.toString(), Modifier.weight(1f))
            StatTile("Wishlist", state.wishlistCount.toString(), Modifier.weight(1f))
        }

        Column(Modifier.padding(horizontal = 20.dp)) {
            Text("Account", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(8.dp))
            ProfileRow(Icons.Filled.Receipt, "My apps", "Installed and updates", onOrders)
            ProfileRow(Icons.Filled.FavoriteBorder, "Wishlist", "Saved for later", onWishlist)
            ProfileRow(Icons.Filled.LocationOn, "Addresses", "Delivery details", onAddresses)
            ProfileRow(Icons.Filled.Notifications, "Notifications", "Updates and offers", onNotifications)

            Spacer(Modifier.height(18.dp))
            Text("Publishing", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(8.dp))
            ProfileRow(
                Icons.Filled.Storefront,
                if (user?.isSeller == true) "Developer hub" else "Publish an app",
                if (user?.isSeller == true) "Manage your apps" else "Open a developer profile",
                onSellerHub,
            )
            if (user?.isAdmin == true) {
                ProfileRow(
                    Icons.Filled.AdminPanelSettings,
                    "Admin console",
                    "Moderate the marketplace",
                    onAdmin,
                )
            }

            Spacer(Modifier.height(18.dp))
            Text("Preferences", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(8.dp))
            ProfileRow(Icons.Filled.Settings, "Settings", "App preferences", onSettings)

            Spacer(Modifier.height(18.dp))
            Surface(
                onClick = viewModel::signOut,
                shape = SproutlyRadii.card,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ExitToApp,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        if (state.isSigningOut) "Signing out…" else "Sign out",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
            Spacer(Modifier.height(28.dp))
            Text(
                "Sproutly · v1.0.0",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun StatTile(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        shape = SproutlyRadii.card,
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier,
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(value, style = MaterialTheme.typography.headlineMedium)
            Text(
                label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ProfileRow(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = SproutlyRadii.card,
        color = MaterialTheme.colorScheme.surface,
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
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(19.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
