package app.tab.store.core.designsystem

import androidx.compose.ui.graphics.Color

/**
 * Liminal, in the Porcelain palette.
 *
 * Monochrome first, with one red reserved for interaction: an install button, an
 * offer, a destructive action. Nothing decorative is coloured, so the app art on
 * a listing is the only thing competing for attention.
 *
 * The names are roles rather than hues — the previous palette was called Lime and
 * Olive, which stops making sense the moment the accent is not lime. The old
 * names remain as aliases so call sites did not have to change in the same commit.
 */
object SproutlyPalette {
    // Light
    val Canvas = Color(0xFFFAFAF8)
    val Surface = Color(0xFFFFFFFF)
    val SurfaceSubtle = Color(0xFFF0F0EC)
    val Ink = Color(0xFF111111)
    val InkSecondary = Color(0xFF555555)
    val InkMuted = Color(0xFF777777)
    val Border = Color(0xFFE5E5E0)
    val Accent = Color(0xFFD7263D)
    val AccentPressed = Color(0xFFB91C32)
    val AccentSoft = Color(0xFFFCE9EC)
    val Success = Color(0xFF18794E)
    val Warning = Color(0xFF946200)
    val Error = Color(0xFFC62828)
    val Info = Color(0xFF2457A7)

    // Dark
    val CanvasDark = Color(0xFF101010)
    val SurfaceDark = Color(0xFF181818)
    val SurfaceSubtleDark = Color(0xFF222222)
    val InkDark = Color(0xFFF5F5F2)
    val InkSecondaryDark = Color(0xFFC4C4BF)
    val InkMutedDark = Color(0xFF92928C)
    val BorderDark = Color(0xFF30302D)
    val AccentDark = Color(0xFFFF5268)
    val AccentPressedDark = Color(0xFFE43B53)
    val AccentSoftDark = Color(0xFF351B20)
    val SuccessDark = Color(0xFF55C58B)
    val WarningDark = Color(0xFFE6B84C)
    val ErrorDark = Color(0xFFFF6B6B)
    val InfoDark = Color(0xFF79A9F5)

    // Aliases kept from the old palette so existing call sites keep working while
    // the roles settle. Lime is now the accent, Olive the ink, Espresso the canvas.
    val Lime = Accent
    val LimeSoft = AccentSoft
    val LimeTint = SurfaceSubtle
    val Olive = Ink
    val OliveDeep = Color(0xFF000000)
    val OliveMid = InkSecondary
    val Moss = InkMuted
    val Espresso = Ink
    val EspressoDeep = Color(0xFF0A0A0A)
    val EspressoMid = Color(0xFF161616)
    val EspressoSoft = Color(0xFF242422)
    val Cream = Canvas
    val Paper = Surface
    val Outline = Border
    val OutlineDark = BorderDark
    val Clay = Warning
    val ClaySoft = Color(0xFFF6EFE2)
    val Berry = Error
    val Gold = Color(0xFFB8860B)
}
