package app.tab.store.core.navigation

import app.tab.store.core.ui.Tokens
import androidx.annotation.DrawableRes
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.drawBehind
import androidx.compose.animation.core.animateFloatAsState
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
    /** Some glyphs fill their whole box and look larger than their neighbours. */
    val iconScale: Float = 1f,
)

/** Browsing destinations, in the left pill. */
val primaryTabs = listOf(
    TabDestination(Routes.HOME, "Apps", PhosphorIcons.Apps, PhosphorIcons.AppsFilled),
    // Outline and filled must be the same glyph: the controller, not a die.
    TabDestination(Routes.GAMES, "Games", PhosphorIcons.Gamepad, PhosphorIcons.GamesFilled, iconScale = 0.88f),
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
    // Measured positions, so one indicator can slide between tabs instead of
    // each tab fading its own background in and out.
    val bounds = remember { mutableStateMapOf<String, Pair<Int, Int>>() }
    val selected = tabs.firstOrNull { it.route == currentRoute }
    val target = selected?.let { bounds[it.route] }

    // Settles with a small overshoot. Animated as plain floats and read only in
    // the draw phase below, so a slide redraws the pill without recomposing it.
    val slide = spring<Float>(dampingRatio = 0.72f, stiffness = 420f)
    val indicatorX = animateFloatAsState(target?.first?.toFloat() ?: 0f, slide, label = "indicatorX")
    val indicatorWidth = animateFloatAsState(target?.second?.toFloat() ?: 0f, slide, label = "indicatorWidth")
    val indicatorColor = Tokens.colorScheme.primaryContainer
    val indicatorVisible = target != null

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
            Row(
                modifier = Modifier.drawBehind {
                    val width = indicatorWidth.value
                    if (indicatorVisible && width > 1f) {
                        drawRoundRect(
                            color = indicatorColor,
                            topLeft = Offset(indicatorX.value, 0f),
                            size = Size(width, size.height),
                            cornerRadius = CornerRadius(30.dp.toPx()),
                        )
                    }
                },
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
                        onMeasured = { x, width ->
                            val next = x to width
                            if (bounds[tab.route] != next) bounds[tab.route] = next
                        },
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
    val iconPop = animateFloatAsState(
        targetValue = if (selected) 0.92f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 900f),
        label = "iconPop",
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
        // Centred in the tab, so the icon sits in the middle of the sliding pill
        // whatever width the tab takes.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.Center,
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
                    modifier = Modifier
                        .size(24.dp)
                        .graphicsLayer {
                            val scale = iconPop.value * tab.iconScale
                            scaleX = scale
                            scaleY = scale
                        },
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
