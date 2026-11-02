package app.tab.store.core.designsystem

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Sproutly's silhouette: generous, organic radii. Cards lean soft and large,
 * controls stay pill-shaped so the UI feels friendly rather than clinical.
 */
val SproutlyShapes = Shapes(
    extraSmall = RoundedCornerShape(12.dp),
    small = RoundedCornerShape(16.dp),
    medium = RoundedCornerShape(22.dp),
    large = RoundedCornerShape(30.dp),
    extraLarge = RoundedCornerShape(38.dp),
)

object SproutlyRadii {
    val card = RoundedCornerShape(28.dp)
    val hero = RoundedCornerShape(36.dp)
    val pill = RoundedCornerShape(percent = 50)
    val sheet = RoundedCornerShape(topStart = 34.dp, topEnd = 34.dp)
    val thumb = RoundedCornerShape(18.dp)
    val nav = RoundedCornerShape(30.dp)
}
