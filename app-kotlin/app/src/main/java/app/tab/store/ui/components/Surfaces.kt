package app.tab.store.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/** Corner radii. Variation is the visual signature: cards use a large radius
 *  with one tightened corner, chips are pills, sheets round only at the top. */
object SproutlyShapes {
    val Xs = RoundedCornerShape(10.dp)
    val Sm = RoundedCornerShape(16.dp)
    val Md = RoundedCornerShape(20.dp)
    val Lg = RoundedCornerShape(28.dp)
    val Xl = RoundedCornerShape(34.dp)
    val Xxl = RoundedCornerShape(44.dp)
    val Pill = RoundedCornerShape(999.dp)

    /** Card: large radius, top-right and bottom-left tightened. */
    val Card: Shape = RoundedCornerShape(
        topStart = 22.dp,
        topEnd = 12.dp,
        bottomEnd = 12.dp,
        bottomStart = 22.dp,
    )

    val SheetTop: Shape = RoundedCornerShape(topStart = 34.dp, topEnd = 34.dp)
}

/**
 * The base surface of the interface: one hairline border, no heavy shadow, and a
 * 0.985 press scale when it is tappable.
 */
@Composable
fun SproutlyCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    padding: PaddingValues = PaddingValues(16.dp),
    color: Color = MaterialTheme.colorScheme.surfaceContainerLowest,
    content: @Composable ColumnScope.() -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.985f else 1f,
        animationSpec = tween(90),
        label = "cardScale",
    )

    Surface(
        modifier = modifier.scale(scale),
        onClick = onClick ?: {},
        enabled = onClick != null,
        shape = SproutlyShapes.Card,
        color = color,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        interactionSource = interaction,
    ) {
        Column(modifier = Modifier.padding(padding), content = content)
    }
}
