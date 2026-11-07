package app.tab.store.core.designsystem

import androidx.compose.foundation.shape.RoundedCornerShape
import app.tab.store.core.ui.SkShapes
import androidx.compose.ui.unit.dp

/**
 * Sproutly's silhouette: generous, organic radii. Cards lean soft and large,
 * controls stay pill-shaped so the UI feels friendly rather than clinical.
 */
/**
 * Material 3 Expressive tokens: the shape scale leans further than the baseline
 * set, so cards and sheets read as soft objects rather than boxes.
 */
val SproutlyShapes = SkShapes(
    extraSmall = RoundedCornerShape(20.dp),
    small = RoundedCornerShape(24.dp),
    medium = RoundedCornerShape(30.dp),
    large = RoundedCornerShape(38.dp),
    extraLarge = RoundedCornerShape(48.dp),
)

object SproutlyRadii {
    val card = RoundedCornerShape(36.dp)
    val hero = RoundedCornerShape(48.dp)
    val pill = RoundedCornerShape(percent = 50)
    val sheet = RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp)
    val thumb = RoundedCornerShape(26.dp)
    val nav = RoundedCornerShape(36.dp)
}
