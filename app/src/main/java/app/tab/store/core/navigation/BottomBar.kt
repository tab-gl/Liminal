package app.tab.store.core.navigation

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.tab.store.core.designsystem.HugeIcon
import app.tab.store.core.designsystem.HugeIcons

data class TabDestination(
    val route: String,
    val label: String,
    @DrawableRes val icon: Int,
)

/** Store · Search · Library · My apps · Profile */
val mainTabs = listOf(
    TabDestination(Routes.HOME, "Store", HugeIcons.Store),
    TabDestination(Routes.SEARCH, "Search", HugeIcons.Search),
    TabDestination(Routes.WISHLIST, "Wishlist", HugeIcons.Favourite),
    TabDestination(Routes.PROFILE, "Profile", HugeIcons.User),
)

/**
 * Floating navigation bar: a rounded, elevated pill that hovers over the
 * content. The selected destination expands to show its icon *and* label; the
 * others stay icon-only, so the bar stays narrow but never hides where you are.
 */
@Composable
fun SproutlyBottomBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 10.dp),
    ) {
        Surface(
            shape = RoundedCornerShape(30.dp),
            color = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            shadowElevation = 14.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                mainTabs.forEach { tab ->
                    FloatingTab(
                        tab = tab,
                        selected = currentRoute == tab.route,
                        badgeCount = 0,
                        onClick = { if (currentRoute != tab.route) onNavigate(tab.route) },
                        modifier = Modifier.weight(1f, fill = false),
                    )
                }
            }
        }
    }
}

@Composable
private fun FloatingTab(
    tab: TabDestination,
    selected: Boolean,
    badgeCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(24.dp)
    val container by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
        label = "tabContainer",
    )
    val content by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
        else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "tabContent",
    )
    val iconSize by animateDpAsState(
        targetValue = if (selected) 21.dp else 23.dp,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 900f),
        label = "iconSize",
    )

    Surface(
        onClick = onClick,
        shape = shape,
        color = container,
        contentColor = content,
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = if (selected) 14.dp else 11.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (badgeCount > 0) {
                BadgedBox(
                    badge = {
                        Badge(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ) { Text(badgeCount.coerceAtMost(99).toString()) }
                    },
                ) {
                    HugeIcon(tab.icon, tab.label, Modifier.size(iconSize), content)
                }
            } else {
                HugeIcon(tab.icon, tab.label, Modifier.size(iconSize), content)
            }

            AnimatedVisibility(
                visible = selected,
                enter = fadeIn() + expandHorizontally(),
                exit = fadeOut() + shrinkHorizontally(),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Spacer(Modifier.width(7.dp))
                    Text(tab.label, style = MaterialTheme.typography.labelLarge, maxLines = 1)
                }
            }
        }
    }
}
