package app.tab.store.core.navigation

import app.tab.store.core.ui.Tokens
import androidx.annotation.DrawableRes
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import app.tab.store.core.ui.Surface
import app.tab.store.core.ui.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.tab.store.core.designsystem.SproutlyIcon
import app.tab.store.core.designsystem.PhosphorIcons

data class TabDestination(
    val route: String,
    val label: String,
    @DrawableRes val icon: Int,
    /** The filled variant of the same icon, shown while this tab is current. */
    @DrawableRes val iconFilled: Int = icon,
)

/** Browsing destinations, in the left pill. */
val primaryTabs = listOf(
    TabDestination(Routes.HOME, "Apps", PhosphorIcons.Apps, PhosphorIcons.AppsFilled),
    TabDestination(Routes.GAMES, "Games", PhosphorIcons.Dice, PhosphorIcons.GamesFilled),
    TabDestination(Routes.OFFERS, "Offers", PhosphorIcons.Gift, PhosphorIcons.OffersFilled),
)

/** You, in the right pill. Search is a FAB, not a tab. */
val utilityTabs = listOf(
    TabDestination(Routes.PROFILE, "Profile", PhosphorIcons.User, PhosphorIcons.UserFilled),
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
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
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
    val density = LocalDensity.current
    // Measured positions, so one indicator can slide between tabs instead of
    // each tab fading its own background in and out.
    val bounds = remember { mutableStateMapOf<String, Pair<Int, Int>>() }
    val selected = tabs.firstOrNull { it.route == currentRoute }
    val target = selected?.let { bounds[it.route] }

    // Low damping on purpose: the pill overshoots slightly and settles, which
    // is what makes the movement read as fluid rather than mechanical.
    val gooey = spring<Dp>(dampingRatio = 0.52f, stiffness = 520f)

    val indicatorX by animateDpAsState(
        targetValue = with(density) { (target?.first ?: 0).toDp() },
        animationSpec = gooey,
        label = "indicatorX",
    )
    val indicatorWidth by animateDpAsState(
        targetValue = with(density) { (target?.second ?: 0).toDp() },
        animationSpec = gooey,
        label = "indicatorWidth",
    )

    Surface(
        shape = RoundedCornerShape(36.dp),
        color = Tokens.colorScheme.surface,
        contentColor = Tokens.colorScheme.onSurface,
        shadowElevation = 12.dp,
        border = BorderStroke(
            1.dp,
            Tokens.colorScheme.outlineVariant.copy(alpha = 0.6f),
        ),
        modifier = modifier,
    ) {
        Box(Modifier.padding(horizontal = 8.dp, vertical = 7.dp)) {
            if (target != null && indicatorWidth > 0.dp) {
                Box(
                    Modifier
                        .offset(x = indicatorX)
                        .size(width = indicatorWidth, height = 46.dp)
                        .background(
                            Tokens.colorScheme.primaryContainer,
                            RoundedCornerShape(30.dp),
                        ),
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                tabs.forEach { tab ->
                    FloatingTab(
                        tab = tab,
                        selected = tab.route == currentRoute,
                        onClick = { if (tab.route != currentRoute) onNavigate(tab.route) },
                        modifier = if (tabs.size > 1) {
                            Modifier.weight(if (tab.route == currentRoute) 1.6f else 1f)
                        } else {
                            Modifier
                        },
                        onMeasured = { x, width -> bounds[tab.route] = x to width },
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
    onClick: () -> Unit,
    onMeasured: (Int, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val content by animateColorAsState(
        targetValue = if (selected) Tokens.colorScheme.onPrimaryContainer
        else Tokens.colorScheme.onSurfaceVariant,
        animationSpec = spring(dampingRatio = 0.9f, stiffness = 1400f),
        label = "tabContent",
    )
    val iconSize by animateDpAsState(
        targetValue = if (selected) 21.dp else 23.dp,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 1500f),
        label = "iconSize",
    )

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(30.dp),
        color = Color.Transparent,
        contentColor = content,
        modifier = modifier.onGloballyPositioned { coords ->
            onMeasured(coords.positionInParent().x.toInt(), coords.size.width)
        },
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = if (selected) 12.dp else 10.dp,
                vertical = 12.dp,
            ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Outline at rest, filled when current — cross-faded so the change
            // reads as the icon settling, not as a swap.
            Crossfade(
                targetState = selected,
                animationSpec = tween(180),
                label = "tabIconFill",
            ) { filled ->
                SproutlyIcon(
                    icon = if (filled) tab.iconFilled else tab.icon,
                    contentDescription = tab.label,
                    modifier = Modifier.size(iconSize),
                    tint = content,
                )
            }
            if (selected) {
                Spacer(Modifier.width(6.dp))
                Text(
                    text = tab.label,
                    style = Tokens.typography.labelMedium,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Clip,
                    modifier = Modifier.padding(end = 2.dp),
                )
            }
        }
    }
}
