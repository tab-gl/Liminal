package app.tab.store.core.navigation

import androidx.annotation.DrawableRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
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

/** Browsing destinations, in the left pill. */
val primaryTabs = listOf(
    TabDestination(Routes.HOME, "Apps", TablerIcons.Grid),
    TabDestination(Routes.GAMES, "Games", TablerIcons.Gamepad),
    TabDestination(Routes.OFFERS, "Offers", TablerIcons.Bag),
)

/** You, in the right pill. Search is a FAB, not a tab. */
val utilityTabs = listOf(
    TabDestination(Routes.PROFILE, "Profile", TablerIcons.User),
)

val mainTabs = primaryTabs + utilityTabs

/**
 * Two floating pills, not one bar: browsing on the left, you on the right.
 *
 * Keeping them separate means the bar never grows into a strip of five equal
 * icons, and each group can animate on its own.
 */
@Composable
fun SproutlyBottomBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PillGroup(
            tabs = primaryTabs,
            currentRoute = currentRoute,
            onNavigate = onNavigate,
            modifier = Modifier.weight(1f, fill = false),
        )
        Spacer(Modifier.width(10.dp))
        PillGroup(
            tabs = utilityTabs,
            currentRoute = currentRoute,
            onNavigate = onNavigate,
        )
    }
}

@Composable
private fun PillGroup(
    tabs: List<TabDestination>,
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(30.dp),
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shadowElevation = 12.dp,
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
        ),
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            tabs.forEach { tab ->
                val selected = currentRoute == tab.route
                FloatingTab(
                    tab = tab,
                    selected = selected,
                    onClick = { if (!selected) onNavigate(tab.route) },
                    modifier = if (tabs.size > 1) {
                        Modifier.weight(if (selected) 1.6f else 1f)
                    } else {
                        Modifier
                    },
                )
            }
        }
    }
}

@Composable
private fun FloatingTab(
    tab: TabDestination,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val container by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
        label = "tabContainer",
    )
    val content by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
        else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "tabContent",
    )
    // Fast and light: the pill should feel like it snaps, not settles.
    val iconSize by animateDpAsState(
        targetValue = if (selected) 21.dp else 23.dp,
        animationSpec = spring(dampingRatio = 0.9f, stiffness = 1600f),
        label = "iconSize",
    )

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        color = container,
        contentColor = content,
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = if (selected) 12.dp else 10.dp,
                vertical = 12.dp,
            ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TablerIcon(tab.icon, tab.label, Modifier.size(iconSize), content)
            if (selected) {
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
