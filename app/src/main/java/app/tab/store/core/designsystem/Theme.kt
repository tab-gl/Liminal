package app.tab.store.core.designsystem

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import android.os.Build
import androidx.compose.ui.platform.LocalContext
import app.tab.store.core.theme.AccentOption
import app.tab.store.core.theme.ThemeMode
import app.tab.store.core.ui.ProvideTokens
import app.tab.store.core.ui.Tokens
import app.tab.store.core.ui.lightScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

private val LightColors = lightScheme(
    // Red is the interaction colour: install, offer, destructive. Ink carries
    // everything else, which is the whole point of a monochrome base.
    primary = SproutlyPalette.Accent,
    onPrimary = SproutlyPalette.Surface,
    primaryContainer = SproutlyPalette.AccentSoft,
    onPrimaryContainer = SproutlyPalette.AccentPressed,
    secondary = SproutlyPalette.Ink,
    onSecondary = SproutlyPalette.Surface,
    secondaryContainer = SproutlyPalette.SurfaceSubtle,
    onSecondaryContainer = SproutlyPalette.Ink,
    tertiary = SproutlyPalette.InkSecondary,
    onTertiary = SproutlyPalette.Surface,
    tertiaryContainer = SproutlyPalette.SurfaceSubtle,
    onTertiaryContainer = SproutlyPalette.Ink,
    background = SproutlyPalette.Canvas,
    onBackground = SproutlyPalette.Ink,
    surface = SproutlyPalette.Surface,
    onSurface = SproutlyPalette.Ink,
    surfaceVariant = SproutlyPalette.SurfaceSubtle,
    onSurfaceVariant = SproutlyPalette.InkMuted,
    surfaceContainer = SproutlyPalette.SurfaceSubtle,
    surfaceContainerHigh = SproutlyPalette.SurfaceSubtle,
    outline = SproutlyPalette.Border,
    outlineVariant = SproutlyPalette.Border,
    error = SproutlyPalette.Error,
    onError = SproutlyPalette.Surface,
    errorContainer = SproutlyPalette.AccentSoft,
    onErrorContainer = SproutlyPalette.Error,
    inverseSurface = SproutlyPalette.Ink,
    inverseOnSurface = SproutlyPalette.Canvas,
)

private val DarkColors = lightScheme(
    primary = SproutlyPalette.AccentDark,
    onPrimary = Color(0xFF1A0A0D),
    primaryContainer = SproutlyPalette.AccentSoftDark,
    onPrimaryContainer = SproutlyPalette.AccentDark,
    secondary = SproutlyPalette.InkDark,
    onSecondary = SproutlyPalette.CanvasDark,
    secondaryContainer = SproutlyPalette.SurfaceSubtleDark,
    onSecondaryContainer = SproutlyPalette.InkDark,
    tertiary = SproutlyPalette.InkSecondaryDark,
    onTertiary = SproutlyPalette.CanvasDark,
    tertiaryContainer = SproutlyPalette.SurfaceSubtleDark,
    onTertiaryContainer = SproutlyPalette.InkDark,
    background = SproutlyPalette.CanvasDark,
    onBackground = SproutlyPalette.InkDark,
    surface = SproutlyPalette.SurfaceDark,
    onSurface = SproutlyPalette.InkDark,
    surfaceVariant = SproutlyPalette.SurfaceSubtleDark,
    onSurfaceVariant = SproutlyPalette.InkMutedDark,
    surfaceContainer = SproutlyPalette.SurfaceSubtleDark,
    surfaceContainerHigh = SproutlyPalette.SurfaceSubtleDark,
    outline = SproutlyPalette.BorderDark,
    outlineVariant = SproutlyPalette.BorderDark,
    error = SproutlyPalette.ErrorDark,
    onError = Color(0xFF1A0A0D),
    errorContainer = SproutlyPalette.AccentSoftDark,
    onErrorContainer = SproutlyPalette.ErrorDark,
    inverseSurface = SproutlyPalette.InkDark,
    inverseOnSurface = SproutlyPalette.CanvasDark,
)

/** Spacing scale — every screen composes from these, never from magic numbers. */
data class Spacing(
    val xs: Dp = 4.dp,
    val sm: Dp = 8.dp,
    val md: Dp = 12.dp,
    val lg: Dp = 16.dp,
    val xl: Dp = 24.dp,
    val xxl: Dp = 32.dp,
    val huge: Dp = 48.dp,
)

val LocalSpacing = staticCompositionLocalOf { Spacing() }

/** Brand-tinted elevation surface colours for product cards and sheets. */
data class SproutlyExtra(
    val skeleton: androidx.compose.ui.graphics.Color,
    val skeletonHighlight: androidx.compose.ui.graphics.Color,
    val ratingGold: androidx.compose.ui.graphics.Color,
    val success: androidx.compose.ui.graphics.Color,
)

val LocalSproutlyExtra = staticCompositionLocalOf {
    SproutlyExtra(
        skeleton = SproutlyPalette.LimeSoft,
        skeletonHighlight = SproutlyPalette.Paper,
        ratingGold = SproutlyPalette.Gold,
        success = SproutlyPalette.OliveMid,
    )
}

@Composable
fun SproutlyTheme(
    /** Null means "follow the device"; set by the Settings sheet. */
    mode: ThemeMode? = null,
    /** The one colour in a monochrome app. Monet follows the wallpaper. */
    accent: AccentOption = AccentOption.MONET,
    content: @Composable () -> Unit,
) {
    val systemDark = isSystemInDarkTheme()
    val darkTheme = when (mode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        else -> systemDark
    }
    val base = if (darkTheme) DarkColors else LightColors
    val colors = applyAccent(base, accent, darkTheme)
    val extra = if (darkTheme) {
        SproutlyExtra(
            skeleton = SproutlyPalette.SurfaceSubtleDark,
            skeletonHighlight = SproutlyPalette.SurfaceDark,
            // A rating star is not an interaction: it stays gold in both themes.
            ratingGold = SproutlyPalette.Gold,
            success = SproutlyPalette.SuccessDark,
        )
    } else {
        SproutlyExtra(
            skeleton = SproutlyPalette.SurfaceSubtle,
            skeletonHighlight = SproutlyPalette.Surface,
            ratingGold = SproutlyPalette.Gold,
            success = SproutlyPalette.Success,
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    // The Sproutly palette is the brand, so it is used on every device. Wallpaper
    // derived colour was tried and removed: lime and olive are the identity.
    // Material 3 Expressive: springy, shape-morphing motion for every M3
    // component (buttons, chips, sheets, and the loading indicator).
    // Our own tokens, our own components. Nothing here comes from Material.
    ProvideTokens(
        colors = colors,
        typography = SproutlyTypography,
        shapes = SproutlyShapes,
        contentColor = colors.onBackground,
    ) {
        CompositionLocalProvider(
            LocalSpacing provides Spacing(),
            LocalSproutlyExtra provides extra,
            content = content,
        )
    }
}

/** Relative luminance, per WCAG, so contrast is computed rather than guessed. */
private fun luminance(color: Color): Double {
    fun channel(value: Float): Double {
        val c = value.toDouble()
        return if (c <= 0.03928) c / 12.92 else Math.pow((c + 0.055) / 1.055, 2.4)
    }
    return 0.2126 * channel(color.red) + 0.7152 * channel(color.green) + 0.0722 * channel(color.blue)
}

/** Black or white, whichever reads on [background]. */
internal fun onColorFor(background: Color): Color =
    if (luminance(background) > 0.45) Color(0xFF111111) else Color(0xFFFFFFFF)

/**
 * Swaps the accent into the scheme and derives everything that depends on it.
 *
 * The foreground of an accent is computed from its luminance rather than assumed
 * white: an amber accent with white text is unreadable, and a person picking their
 * own accent would hit exactly that.
 */
private fun applyAccent(base: SkColorScheme, accent: AccentOption, dark: Boolean): SkColorScheme {
    val seed = when {
        // Monet: the wallpaper colour on Android 12+, the brand's own indigo before.
        accent == AccentOption.MONET -> if (dark) Color(0xFFB9C3FF) else Color(0xFF4F5BD5)
        else -> Color(accent.hex or 0xFF000000L)
    }
    val resolved = if (dark && accent != AccentOption.MONET) seed.copy(alpha = 1f) else seed
    val onAccent = onColorFor(resolved)
    return base.copy(
        primary = resolved,
        onPrimary = onAccent,
        primaryContainer = resolved.copy(alpha = 0.16f),
        onPrimaryContainer = if (dark) resolved else resolved,
    )
}
