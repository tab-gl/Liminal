package app.tab.store.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person

data class NavDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector,
    val emphasized: Boolean = false,
)

val SproutlyDestinations = listOf(
    NavDestination("discover", "Discover", Icons.Outlined.Explore, Icons.Filled.Explore),
    NavDestination("categories", "Categories", Icons.Outlined.Category, Icons.Filled.Category),
    NavDestination("publish", "Publish", Icons.Filled.Add, Icons.Filled.Add, emphasized = true),
    NavDestination("activity", "Activity", Icons.Outlined.Notifications, Icons.Filled.Notifications),
    NavDestination("profile", "Profile", Icons.Outlined.Person, Icons.Filled.Person),
)

/**
 * The floating navigation island.
 *
 * Detached from every edge, translucent so the feed reads through it, with a lime
 * capsule that widens behind the active slot and a raised olive capsule for
 * Publish. Icons are Material's outline/filled pairs; swapping in Hugeicons means
 * adding its vectors as drawables and changing the two ImageVectors per entry.
 */
@Composable
fun FloatingNavBar(
    currentRoute: String,
    unreadCount: Int,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 18.dp, vertical = 12.dp),
        shape = SproutlyShapes.Xxl,
        color = scheme.surfaceContainerLowest.copy(alpha = 0.94f),
        border = BorderStroke(1.dp, scheme.outlineVariant),
        shadowElevation = 12.dp,
    ) {
        Row(
            modifier = Modifier.height(74.dp).padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            SproutlyDestinations.forEach { destination ->
                val selected = currentRoute == destination.route
                if (destination.emphasized) {
                    PublishSlot(selected = selected, onClick = { onSelect(destination.route) })
                } else {
                    NavSlot(
                        destination = destination,
                        selected = selected,
                        badge = if (destination.route == "activity") unreadCount else 0,
                        onClick = { onSelect(destination.route) },
                    )
                }
            }
        }
    }
}

@Composable
private fun NavSlot(
    destination: NavDestination,
    selected: Boolean,
    badge: Int,
    onClick: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val capsuleWidth by animateDpAsState(
        targetValue = if (selected) 54.dp else 36.dp,
        animationSpec = tween(260),
        label = "capsule",
    )

    Surface(
        onClick = onClick,
        shape = SproutlyShapes.Lg,
        color = Color.Transparent,
        modifier = Modifier.width(64.dp),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(contentAlignment = Alignment.Center) {
                Surface(
                    modifier = Modifier.width(capsuleWidth).height(32.dp),
                    shape = SproutlyShapes.Pill,
                    color = if (selected) scheme.primaryContainer else Color.Transparent,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (selected) destination.selectedIcon else destination.icon,
                            contentDescription = destination.label,
                            tint = if (selected) scheme.onPrimaryContainer else scheme.onSurfaceVariant,
                            modifier = Modifier.size(if (selected) 21.dp else 20.dp),
                        )
                    }
                }
                if (badge > 0) {
                    Surface(
                        modifier = Modifier.align(Alignment.TopEnd).padding(top = 0.dp),
                        shape = CircleShape,
                        color = scheme.error,
                    ) {
                        Text(
                            text = if (badge > 9) "9+" else badge.toString(),
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = scheme.onError,
                        )
                    }
                }
            }
            Spacer(Modifier.height(3.dp))
            Text(
                text = destination.label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                ),
                color = if (selected) scheme.onSurface else scheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PublishSlot(selected: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        shape = SproutlyShapes.Pill,
        color = scheme.primary,
        contentColor = scheme.onPrimary,
        modifier = Modifier.width(60.dp).height(50.dp),
        shadowElevation = if (selected) 8.dp else 2.dp,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.Add, contentDescription = "Publish", modifier = Modifier.size(26.dp))
        }
    }
}
