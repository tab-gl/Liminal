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
 * Hugeicons (Free 4.4.0) as Compose.
 *
 * Hugeicons ships as SVG/stroke data with no Android artifact, so the free set
 * is compiled into vector drawables under `res/drawable/ic_hg_*.xml` by
 * `tools/hugeicons.py`, and addressed here by name. Every icon is a 24dp
 * stroke icon at 1.5 stroke width, so they stay optically consistent with each
 * other — which the Material icon set does not, once you mix filled and
 * outlined families.
 *
 * Regenerate with: `python3 tools/hugeicons.py`
 */
object HugeIcons {
    @DrawableRes val Store = R.drawable.ic_hg_store
    @DrawableRes val Home = R.drawable.ic_hg_home
    @DrawableRes val Search = R.drawable.ic_hg_search
    @DrawableRes val User = R.drawable.ic_hg_user
    @DrawableRes val Download = R.drawable.ic_hg_download
    @DrawableRes val DownloadAlt = R.drawable.ic_hg_download_alt
    @DrawableRes val Library = R.drawable.ic_hg_library
    @DrawableRes val LibraryAlt = R.drawable.ic_hg_library_alt
    @DrawableRes val Book = R.drawable.ic_hg_book
    @DrawableRes val Favourite = R.drawable.ic_hg_favourite
    @DrawableRes val Star = R.drawable.ic_hg_star
    @DrawableRes val ArrowRight = R.drawable.ic_hg_arrow_right
    @DrawableRes val ArrowLeft = R.drawable.ic_hg_arrow_left
    @DrawableRes val Plus = R.drawable.ic_hg_plus
    @DrawableRes val Filter = R.drawable.ic_hg_filter
    @DrawableRes val Bag = R.drawable.ic_hg_bag
    @DrawableRes val Share = R.drawable.ic_hg_share
    @DrawableRes val More = R.drawable.ic_hg_more
    @DrawableRes val Tick = R.drawable.ic_hg_tick
    @DrawableRes val Close = R.drawable.ic_hg_close
    @DrawableRes val Settings = R.drawable.ic_hg_settings
    @DrawableRes val Notification = R.drawable.ic_hg_notification
    @DrawableRes val Package = R.drawable.ic_hg_package
    @DrawableRes val Grid = R.drawable.ic_hg_grid
    @DrawableRes val Sparkles = R.drawable.ic_hg_sparkles
    @DrawableRes val Upload = R.drawable.ic_hg_upload
    @DrawableRes val Analytics = R.drawable.ic_hg_analytics
}

/** Renders a Hugeicon, tinted like any Material icon. */
@Composable
fun HugeIcon(
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
