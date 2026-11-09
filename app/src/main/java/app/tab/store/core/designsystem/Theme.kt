package app.tab.store.core.designsystem

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import android.os.Build
import androidx.compose.ui.platform.LocalContext
import app.tab.store.core.theme.AccentOption
import app.tab.store.core.theme.buildScheme
import app.tab.store.core.theme.monetSeed
import androidx.compose.runtime.remember
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
    val context = LocalContext.current
    // The accent drives the whole scheme (canvas, surfaces, ink, containers), and
    // every pairing is contrast-checked in buildScheme. Rebuilt only when the
    // accent, the mode or the wallpaper colour changes.
    val seed = remember(accent, darkTheme) {
        if (accent == AccentOption.MONET) {
            monetSeed(context, darkTheme) ?: Color(if (darkTheme) 0xFFB9C3FF else 0xFF4F5BD5)
        } else {
            Color(accent.hex or 0xFF000000L)
        }
    }
    val colors = remember(seed, darkTheme) { buildScheme(seed, darkTheme) }
    val extra = if (darkTheme) {
        SproutlyExtra(
            skeleton = colors.surfaceContainer,
            skeletonHighlight = colors.surface,
            // A rating star is not an interaction: it stays gold in both themes.
            ratingGold = SproutlyPalette.Gold,
            success = SproutlyPalette.SuccessDark,
        )
    } else {
        SproutlyExtra(
            skeleton = colors.surfaceContainer,
            skeletonHighlight = colors.surface,
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
