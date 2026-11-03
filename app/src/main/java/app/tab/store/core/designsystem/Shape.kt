package app.tab.store.core.designsystem

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Sproutly's silhouette: generous, organic radii. Cards lean soft and large,
 * controls stay pill-shaped so the UI feels friendly rather than clinical.
 */
/**
 * Material 3 Expressive tokens: the shape scale leans further than the baseline
 * set, so cards and sheets read as soft objects rather than boxes.
 */
val SproutlyShapes = Shapes(
    extraSmall = RoundedCornerShape(14.dp),
    small = RoundedCornerShape(18.dp),
    medium = RoundedCornerShape(24.dp),
    large = RoundedCornerShape(32.dp),
    extraLarge = RoundedCornerShape(40.dp),
)

object SproutlyRadii {
    val card = RoundedCornerShape(30.dp)
    val hero = RoundedCornerShape(40.dp)
    val pill = RoundedCornerShape(percent = 50)
    val sheet = RoundedCornerShape(topStart = 34.dp, topEnd = 34.dp)
    val thumb = RoundedCornerShape(22.dp)
    val nav = RoundedCornerShape(30.dp)
}
