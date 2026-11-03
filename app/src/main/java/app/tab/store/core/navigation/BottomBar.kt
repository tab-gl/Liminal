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
import androidx.compose.foundation.background
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.tab.store.core.designsystem.TablerIcon
import app.tab.store.core.designsystem.TablerIcons

data class TabDestination(
    val route: String,
    val label: String,
    @DrawableRes val icon: Int,
)

/**
 * Two groups, as sketched: browsing on the left, you on the right.
 *
 *   [ Apps  Games  Studio ]  |  [ Search  Profile ]
 */
val mainTabs = listOf(
    TabDestination(Routes.HOME, "Apps", TablerIcons.Grid),
    TabDestination(Routes.GAMES, "Games", TablerIcons.Gamepad),
    TabDestination(Routes.SELLER_DASHBOARD, "Studio", TablerIcons.Edit),
    TabDestination(Routes.SEARCH, "Search", TablerIcons.Search),
    TabDestination(Routes.PROFILE, "Profile", TablerIcons.User),
)

/** Tabs after this index sit in the second group, behind a divider. */
const val SECOND_GROUP_FROM = 3

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
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                mainTabs.forEachIndexed { index, tab ->
                    if (index == SECOND_GROUP_FROM) {
                        Box(
                            Modifier
                                .padding(horizontal = 2.dp)
                                .size(width = 1.dp, height = 24.dp)
                                .background(MaterialTheme.colorScheme.outlineVariant),
                        )
                    }
                    FloatingTab(
                        tab = tab,
                        selected = currentRoute == tab.route,
                        badgeCount = 0,
                        onClick = { if (currentRoute != tab.route) onNavigate(tab.route) },
                        modifier = Modifier.weight(if (currentRoute == tab.route) 1.7f else 1f),
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
            modifier = Modifier.padding(horizontal = if (selected) 12.dp else 10.dp, vertical = 12.dp),
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
                    TablerIcon(tab.icon, tab.label, Modifier.size(iconSize), content)
                }
            } else {
                TablerIcon(tab.icon, tab.label, Modifier.size(iconSize), content)
            }

            AnimatedVisibility(
                visible = selected,
                enter = fadeIn() + expandHorizontally(),
                exit = fadeOut() + shrinkHorizontally(),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = tab.label,
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Clip,
                        modifier = Modifier.padding(end = 2.dp),
                    )
                }
            }
        }
    }
}
