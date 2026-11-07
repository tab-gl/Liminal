package app.tab.store.core.designsystem

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import app.tab.store.R

/**
 * Phosphor icons (MIT) as Compose.
 *
 * Phosphor publishes SVG only, so the set is compiled into vector drawables under
 * `res/drawable/ic_ph_*.xml` by `tools/phosphor.py`. Solid paths at a 256
 * viewport rather than stroked outlines, so they hold their shape at 16dp and
 * tint cleanly.
 *
 * Regenerate with: `python3 tools/phosphor.py`
 */

/** Renders a Phosphor icon, tinted like any other. */
@Composable
fun SproutlyIcon(
    @DrawableRes icon: Int,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
) {
    Image(
        painter = painterResource(id = icon),
        contentDescription = contentDescription,
        modifier = modifier,
        colorFilter = ColorFilter.tint(tint),
    )
}

/** Category chips use the iconKey stored on the category document. */
@DrawableRes
fun categoryIcon(iconKey: String?): Int = when (iconKey?.lowercase()) {
    "check", "productivity" -> PhosphorIcons.Tick
    "gamepad", "games" -> PhosphorIcons.Gamepad
    "camera", "photography" -> PhosphorIcons.Camera
    "wallet", "finance" -> PhosphorIcons.Wallet
    "book", "education" -> PhosphorIcons.Book
    "heart", "health", "health-fitness" -> PhosphorIcons.Favourite
    "users", "social" -> PhosphorIcons.Users
    "settings", "tools" -> PhosphorIcons.Settings
    else -> PhosphorIcons.Category
}
