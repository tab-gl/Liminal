package app.tab.store.core.kit

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Sproutly's own design tokens.
 *
 * These replace MaterialTheme: a colour set, a type scale and a radius scale,
 * held in CompositionLocals so a screen reads [Sk.colors] and [Sk.type] and
 * nothing else. No Material dependency, which means the components below can be
 * shaped however they need to be — every one of them is built here, rounded, and
 * not inherited from a library's opinion of what a button is.
 */
@Immutable
data class SkColors(
    val lime: Color = Color(0xFFDBE6A1),
    val limeSoft: Color = Color(0xFFEDF3C8),
    val olive: Color = Color(0xFF3F4C06),
    val oliveDeep: Color = Color(0xFF2C3604),
    val espresso: Color = Color(0xFF3C3130),
    val cream: Color = Color(0xFFF9F7EF),
    val surface: Color = Color(0xFFFFFFFF),
    val surfaceHigh: Color = Color(0xFFF1EFE4),
    val surfaceLow: Color = Color(0xFFF5F3EA),
    val outline: Color = Color(0xFFD9D5C4),
    val onSurface: Color = Color(0xFF3C3130),
    val onSurfaceMuted: Color = Color(0xFF7A7267),
    val onAccent: Color = Color(0xFFFFFFFF),
    val danger: Color = Color(0xFFB3261E),
    val dangerSoft: Color = Color(0xFFF9DEDC),
    val warn: Color = Color(0xFF7A5B00),
    val warnSoft: Color = Color(0xFFFFEFC2),
) {
    val primary: Color get() = olive
    val onPrimary: Color get() = lime
    val primaryContainer: Color get() = lime
    val onPrimaryContainer: Color get() = olive
    val background: Color get() = cream
}

@Immutable
data class SkType(
    val display: TextStyle = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 34.sp,
        lineHeight = 40.sp,
    ),
    val title: TextStyle = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
    ),
    val headline: TextStyle = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
    ),
    val body: TextStyle = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 15.sp,
        lineHeight = 21.sp,
    ),
    val small: TextStyle = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 13.sp,
        lineHeight = 18.sp,
    ),
    val label: TextStyle = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 16.sp,
    ),
    val eyebrow: TextStyle = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 1.1.sp,
    ),
)

@Immutable
data class SkRadii(
    val small: Int = 16,
    val card: Int = 28,
    val hero: Int = 36,
    val sheet: Int = 40,
    val pill: Int = 999,
)

private val LocalColors = staticCompositionLocalOf { SkColors() }
private val LocalType = staticCompositionLocalOf { SkType() }
private val LocalRadii = staticCompositionLocalOf { SkRadii() }

/** Read the tokens: `Sk.colors.olive`, `Sk.type.title`, `Sk.radii.card`. */
object Sk {
    val colors: SkColors
        @Composable @ReadOnlyComposable get() = LocalColors.current
    val type: SkType
        @Composable @ReadOnlyComposable get() = LocalType.current
    val radii: SkRadii
        @Composable @ReadOnlyComposable get() = LocalRadii.current

    /** Corner radius in dp, rounded by construction. */
    val card = androidx.compose.ui.unit.Dp(28f)
}

@Composable
fun SkTheme(
    colors: SkColors = SkColors(),
    type: SkType = SkType(),
    radii: SkRadii = SkRadii(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalColors provides colors,
        LocalType provides type,
        LocalRadii provides radii,
        content = content,
    )
}

/** Shorthand used across the kit. */
val SkCardRadius = 28.dp
val SkHeroRadius = 36.dp
val SkSheetRadius = 40.dp
