package app.tab.store.core.designsystem

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import android.os.Build
import androidx.compose.ui.platform.LocalContext
import app.tab.store.core.theme.ThemeMode
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

private val LightColors = lightColorScheme(
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

private val DarkColors = darkColorScheme(
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

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
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

    // Monet: on Android 12+ the palette is derived from the wallpaper with the
    // TonalSpot scheme (M3's default tonal mapping), so the app belongs to the
    // device instead of fighting it. Below 12 the Sproutly palette stands.
    val context = LocalContext.current
    val dynamic = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val active = when {
        dynamic && darkTheme -> dynamicDarkColorScheme(context)
        dynamic -> dynamicLightColorScheme(context)
        else -> colors
    }

    // Material 3 Expressive: springy, shape-morphing motion for every M3
    // component (buttons, chips, sheets, and the loading indicator).
    MaterialExpressiveTheme(
        colorScheme = active,
        motionScheme = MotionScheme.expressive(),
        typography = SproutlyTypography,
        shapes = SproutlyShapes,
    ) {
        CompositionLocalProvider(
            LocalSpacing provides Spacing(),
            LocalSproutlyExtra provides extra,
            content = content,
        )
    }
}
