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
 * Tabler Icons (v3.31.0, MIT) as Compose.
 *
 * Tabler publishes SVG only, so the outline set is compiled into vector
 * drawables under `res/drawable/ic_tb_*.xml` by `tools/tabler.py`. Every icon
 * is a 24dp stroked outline at stroke width 2 — Tabler's own default — so they
 * stay optically consistent with one another.
 *
 * Regenerate with: `python3 tools/tabler.py`
 */
object TablerIcons {
    @DrawableRes val Store = R.drawable.ic_tb_store
    @DrawableRes val Home = R.drawable.ic_tb_home
    @DrawableRes val Search = R.drawable.ic_tb_search
    @DrawableRes val User = R.drawable.ic_tb_user
    @DrawableRes val Download = R.drawable.ic_tb_download
    @DrawableRes val DownloadAlt = R.drawable.ic_tb_downloadalt
    @DrawableRes val Library = R.drawable.ic_tb_library
    @DrawableRes val LibraryAlt = R.drawable.ic_tb_libraryalt
    @DrawableRes val Book = R.drawable.ic_tb_book
    @DrawableRes val Favourite = R.drawable.ic_tb_favourite
    @DrawableRes val Star = R.drawable.ic_tb_star
    @DrawableRes val ArrowRight = R.drawable.ic_tb_arrowright
    @DrawableRes val ArrowLeft = R.drawable.ic_tb_arrowleft
    @DrawableRes val Plus = R.drawable.ic_tb_plus
    @DrawableRes val Filter = R.drawable.ic_tb_filter
    @DrawableRes val Bag = R.drawable.ic_tb_bag
    @DrawableRes val Share = R.drawable.ic_tb_share
    @DrawableRes val More = R.drawable.ic_tb_more
    @DrawableRes val Tick = R.drawable.ic_tb_tick
    @DrawableRes val Close = R.drawable.ic_tb_close
    @DrawableRes val Settings = R.drawable.ic_tb_settings
    @DrawableRes val Notification = R.drawable.ic_tb_notification
    @DrawableRes val Package = R.drawable.ic_tb_package
    @DrawableRes val Grid = R.drawable.ic_tb_grid
    @DrawableRes val Sparkles = R.drawable.ic_tb_sparkles
    @DrawableRes val Upload = R.drawable.ic_tb_upload
    @DrawableRes val Analytics = R.drawable.ic_tb_analytics
    @DrawableRes val ChevronDown = R.drawable.ic_tb_chevrondown
    @DrawableRes val ChevronLeft = R.drawable.ic_tb_chevronleft
    @DrawableRes val ChevronRight = R.drawable.ic_tb_chevronright
    @DrawableRes val Apps = R.drawable.ic_tb_apps
    @DrawableRes val Dice = R.drawable.ic_tb_dice
    @DrawableRes val Discount = R.drawable.ic_tb_discount
    @DrawableRes val AppsFilled = R.drawable.ic_tb_apps_filled
    @DrawableRes val GamesFilled = R.drawable.ic_tb_games_filled
    @DrawableRes val OffersFilled = R.drawable.ic_tb_offers_filled
    @DrawableRes val UserFilled = R.drawable.ic_tb_user_filled
    @DrawableRes val FavouriteFilled = R.drawable.ic_tb_favourite_filled

    @DrawableRes val Link = R.drawable.ic_tb_link
    @DrawableRes val Verified = R.drawable.ic_tb_verified
    @DrawableRes val Receipt = R.drawable.ic_tb_receipt
    @DrawableRes val Edit = R.drawable.ic_tb_edit
    @DrawableRes val Trash = R.drawable.ic_tb_trash
    @DrawableRes val Image = R.drawable.ic_tb_image
    @DrawableRes val SearchOff = R.drawable.ic_tb_searchoff
    @DrawableRes val Gamepad = R.drawable.ic_tb_gamepad
    @DrawableRes val Eye = R.drawable.ic_tb_eye
    @DrawableRes val EyeOff = R.drawable.ic_tb_eyeoff
    @DrawableRes val Camera = R.drawable.ic_tb_camera
    @DrawableRes val Wallet = R.drawable.ic_tb_wallet
    @DrawableRes val Users = R.drawable.ic_tb_users
    @DrawableRes val Category = R.drawable.ic_tb_category
    @DrawableRes val Tag = R.drawable.ic_tb_tag
    @DrawableRes val Clock = R.drawable.ic_tb_clock
}

/** Renders a Tabler icon, tinted like any Material icon. */
@Composable
fun TablerIcon(
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
    "check", "productivity" -> TablerIcons.Tick
    "gamepad", "games" -> TablerIcons.Gamepad
    "camera", "photography" -> TablerIcons.Camera
    "wallet", "finance" -> TablerIcons.Wallet
    "book", "education" -> TablerIcons.Book
    "heart", "health", "health-fitness" -> TablerIcons.Favourite
    "users", "social" -> TablerIcons.Users
    "settings", "tools" -> TablerIcons.Settings
    else -> TablerIcons.Category
}
