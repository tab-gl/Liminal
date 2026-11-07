package app.tab.store.core.designsystem

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import android.os.Build
import androidx.compose.ui.platform.LocalContext
import app.tab.store.core.theme.ThemeMode
import app.tab.store.core.ui.ProvideTokens
import app.tab.store.core.ui.Tokens
import app.tab.store.core.ui.lightScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

private val LightColors = lightScheme(
    primary = SproutlyPalette.Olive,
    onPrimary = SproutlyPalette.LimeTint,
    primaryContainer = SproutlyPalette.Lime,
    onPrimaryContainer = SproutlyPalette.OliveDeep,
    secondary = SproutlyPalette.Moss,
    onSecondary = SproutlyPalette.Paper,
    secondaryContainer = SproutlyPalette.LimeSoft,
    onSecondaryContainer = SproutlyPalette.OliveDeep,
    tertiary = SproutlyPalette.Espresso,
    onTertiary = SproutlyPalette.Cream,
    tertiaryContainer = SproutlyPalette.ClaySoft,
    onTertiaryContainer = SproutlyPalette.Clay,
    background = SproutlyPalette.Cream,
    onBackground = SproutlyPalette.Ink,
    surface = SproutlyPalette.Paper,
    onSurface = SproutlyPalette.Ink,
    surfaceVariant = SproutlyPalette.LimeTint,
    onSurfaceVariant = SproutlyPalette.InkMuted,
    surfaceContainer = SproutlyPalette.LimeTint,
    surfaceContainerHigh = SproutlyPalette.LimeSoft,
    outline = SproutlyPalette.Outline,
    outlineVariant = SproutlyPalette.LimeSoft,
    error = SproutlyPalette.Berry,
    onError = SproutlyPalette.Paper,
    errorContainer = SproutlyPalette.ClaySoft,
    onErrorContainer = SproutlyPalette.Clay,
    inverseSurface = SproutlyPalette.Espresso,
    inverseOnSurface = SproutlyPalette.Cream,
)

private val ColorTokens_DarkMuted = androidx.compose.ui.graphics.Color(0xFFB9B7A6)
private val ColorTokens_DarkError = androidx.compose.ui.graphics.Color(0xFFE8A99A)

private val DarkColors = lightScheme(
    primary = SproutlyPalette.Lime,
    onPrimary = SproutlyPalette.OliveDeep,
    primaryContainer = SproutlyPalette.Olive,
    onPrimaryContainer = SproutlyPalette.Lime,
    secondary = SproutlyPalette.Moss,
    onSecondary = SproutlyPalette.EspressoDeep,
    secondaryContainer = SproutlyPalette.OliveDeep,
    onSecondaryContainer = SproutlyPalette.Lime,
    tertiary = SproutlyPalette.ClaySoft,
    onTertiary = SproutlyPalette.Clay,
    tertiaryContainer = SproutlyPalette.EspressoSoft,
    onTertiaryContainer = SproutlyPalette.ClaySoft,
    background = SproutlyPalette.EspressoDeep,
    onBackground = SproutlyPalette.Cream,
    surface = SproutlyPalette.EspressoMid,
    onSurface = SproutlyPalette.Cream,
    surfaceVariant = SproutlyPalette.Espresso,
    onSurfaceVariant = ColorTokens_DarkMuted,
    surfaceContainer = SproutlyPalette.Espresso,
    surfaceContainerHigh = SproutlyPalette.EspressoSoft,
    outline = SproutlyPalette.OutlineDark,
    outlineVariant = SproutlyPalette.EspressoSoft,
    error = ColorTokens_DarkError,
    onError = SproutlyPalette.EspressoDeep,
    errorContainer = SproutlyPalette.EspressoSoft,
    onErrorContainer = SproutlyPalette.ClaySoft,
    inverseSurface = SproutlyPalette.Cream,
    inverseOnSurface = SproutlyPalette.Ink,
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
    content: @Composable () -> Unit,
) {
    val systemDark = isSystemInDarkTheme()
    val darkTheme = when (mode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        else -> systemDark
    }
    val colors = if (darkTheme) DarkColors else LightColors
    val extra = if (darkTheme) {
        SproutlyExtra(
            skeleton = SproutlyPalette.EspressoSoft,
            skeletonHighlight = SproutlyPalette.Espresso,
            ratingGold = SproutlyPalette.Gold,
            success = SproutlyPalette.Lime,
        )
    } else {
        SproutlyExtra(
            skeleton = SproutlyPalette.LimeSoft,
            skeletonHighlight = SproutlyPalette.Paper,
            ratingGold = SproutlyPalette.Gold,
            success = SproutlyPalette.OliveMid,
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
