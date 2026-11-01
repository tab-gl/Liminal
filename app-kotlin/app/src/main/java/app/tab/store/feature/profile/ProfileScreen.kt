package app.tab.store.feature.profile

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.tab.store.data.model.AppListing
import app.tab.store.data.model.UserProfile
import app.tab.store.di.ServiceGraph
import app.tab.store.ui.components.AppImage
import app.tab.store.ui.components.AppListingCard
import app.tab.store.ui.components.ButtonVariant
import app.tab.store.ui.components.SproutlyButton
import app.tab.store.ui.components.SproutlyPill
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(
    onSignOut: () -> Unit,
    onOpenDashboard: () -> Unit,
    onOpenAdvertising: () -> Unit,
    onOpenBookmarks: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAdmin: () -> Unit,
    bottomPadding: Dp = 0.dp,
) {
    val uid = ServiceGraph.auth.uid
    var profile by remember { mutableStateOf<UserProfile?>(null) }
    var apps by remember { mutableStateOf<List<AppListing>>(emptyList()) }

    LaunchedEffect(uid) {
        if (uid == null) return@LaunchedEffect
        runCatching { ServiceGraph.auth.ensureProfile() }.onSuccess { profile = it }
        runCatching { ServiceGraph.publishing.myApps(uid) }.onSuccess { apps = it }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = bottomPadding + 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppImage(
                    url = profile?.photoUrl,
                    size = 68.dp,
                    shape = androidx.compose.foundation.shape.CircleShape,
                    contentDescription = profile?.displayName,
                )
                Spacer(Modifier.width(14.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = profile?.displayName ?: "Sproutly member",
                            style = MaterialTheme.typography.titleLarge,
                        )
                        if (profile?.isDeveloper == true) {
                            Spacer(Modifier.width(6.dp))
                            SproutlyPill(label = "Verified")
                        }
                    }
                    Text(
                        text = profile?.bio?.takeIf { it.isNotBlank() }
                            ?: "Add a short bio to tell people what you build.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Metric("Published apps", "${profile?.publishedAppCount ?: apps.size}")
                Metric("Followers", "${profile?.followerCount ?: 0}")
                Metric("Reviews written", "${profile?.reviewCount ?: 0}")
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SproutlyButton(
                    label = "Dashboard",
                    variant = ButtonVariant.Tonal,
                    fillWidth = true,
                    onClick = onOpenDashboard,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SproutlyButton(
                        label = "Advertising",
                        variant = ButtonVariant.Outline,
                        fillWidth = true,
                        modifier = Modifier.fillMaxWidth(0.5f),
                        onClick = onOpenAdvertising,
                    )
                    SproutlyButton(
                        label = "Saved",
                        variant = ButtonVariant.Outline,
                        fillWidth = true,
                        modifier = Modifier.fillMaxWidth(0.5f),
                        onClick = onOpenBookmarks,
                    )
                }
                SproutlyButton(
                    label = "Settings",
                    variant = ButtonVariant.Ghost,
                    fillWidth = true,
                    onClick = onOpenSettings,
                )
                if (profile?.isAdmin == true) {
                    SproutlyButton(
                        label = "Moderation",
                        variant = ButtonVariant.Destructive,
                        fillWidth = true,
                        onClick = onOpenAdmin,
                    )
                }
            }
        }

        item {
            Text(
                text = "Your listings",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(top = 8.dp),
            )
        }

        if (apps.isEmpty()) {
            item {
                Text(
                    text = "Nothing published yet. The Publish tab takes about five minutes.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        items(apps, key = { it.id }) { listing ->
            AppListingCard(listing = listing, onClick = {}, showDeveloper = false)
        }

        item {
            Spacer(Modifier.height(16.dp))
            SproutlyButton(
                label = "Sign out",
                variant = ButtonVariant.Outline,
                fillWidth = true,
                onClick = {
                    // Fire-and-forget is fine: the auth listener drives navigation.
                    kotlinx.coroutines.MainScope().launch {
                        runCatching { ServiceGraph.auth.signOut() }
                        onSignOut()
                    }
                },
            )
        }
    }
}

@Composable
private fun Metric(label: String, value: String) {
    Column(
        modifier = Modifier.fillMaxWidth(0.32f),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.headlineSmall)
    }
}
