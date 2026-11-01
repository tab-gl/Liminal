package app.tab.store.core.designsystem

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Sproutly's silhouette: generous, organic radii. Cards lean soft and large,
 * controls stay pill-shaped so the UI feels friendly rather than clinical.
 */
val SproutlyShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(26.dp),
    extraLarge = RoundedCornerShape(34.dp),
)

object SproutlyRadii {
    val card = RoundedCornerShape(22.dp)
    val hero = RoundedCornerShape(30.dp)
    val pill = RoundedCornerShape(percent = 50)
    val sheet = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp)
    val thumb = RoundedCornerShape(14.dp)
}
