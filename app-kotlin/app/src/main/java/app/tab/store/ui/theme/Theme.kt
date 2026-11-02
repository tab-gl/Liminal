package app.tab.store.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import app.tab.store.ui.components.SproutlyShapes

private val LightScheme = lightColorScheme(
    primary = SproutlyColors.Olive,
    onPrimary = Color(0xFFF7FBE4),
    primaryContainer = SproutlyColors.Lime,
    onPrimaryContainer = SproutlyColors.OliveDeep,
    secondary = Color(0xFF5A6430),
    onSecondary = Color(0xFFFBFCEF),
    secondaryContainer = SproutlyColors.LimeTint,
    onSecondaryContainer = SproutlyColors.OliveDeep,
    tertiary = Color(0xFF6B6A3E),
    onTertiary = Color(0xFFFCFBEF),
    error = SproutlyColors.Error,
    onError = Color.White,
    errorContainer = SproutlyColors.ErrorContainer,
    onErrorContainer = Color(0xFF410E0B),
    background = SproutlyColors.OffWhite,
    onBackground = SproutlyColors.Charcoal,
    surface = SproutlyColors.OffWhite,
    onSurface = SproutlyColors.Charcoal,
    surfaceVariant = SproutlyColors.SoftNeutral,
    onSurfaceVariant = SproutlyColors.MutedText,
    surfaceContainer = SproutlyColors.SoftNeutral,
    surfaceContainerLow = Color(0xFFF5F4EB),
    surfaceContainerLowest = Color.White,
    surfaceContainerHigh = Color(0xFFE9E9DE),
    surfaceContainerHighest = Color(0xFFE1E2D3),
    outline = Color(0xFFC9CBB8),
    outlineVariant = SproutlyColors.Border,
    inverseSurface = SproutlyColors.Charcoal,
    inverseOnSurface = SproutlyColors.SoftNeutral,
)

private val DarkScheme = darkColorScheme(
    primary = SproutlyColors.Lime,
    onPrimary = SproutlyColors.OliveDeep,
    primaryContainer = Color(0xFF39430A),
    onPrimaryContainer = Color(0xFFE9F2C0),
    secondary = Color(0xFFC3CB9A),
    onSecondary = SproutlyColors.OliveDeep,
    secondaryContainer = Color(0xFF3A4220),
    onSecondaryContainer = Color(0xFFE7EDC9),
    tertiary = Color(0xFFD4D2A6),
    onTertiary = Color(0xFF2B2A12),
    error = SproutlyColors.ErrorDark,
    onError = Color(0xFF601410),
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFF9DEDC),
    background = SproutlyColors.Ink,
    onBackground = Color(0xFFF2F1E6),
    surface = SproutlyColors.Ink,
    onSurface = Color(0xFFF2F1E6),
    surfaceVariant = SproutlyColors.InkHigh,
    onSurfaceVariant = SproutlyColors.InkMutedText,
    surfaceContainer = SproutlyColors.InkSurface,
    surfaceContainerLow = Color(0xFF1B1C13),
    surfaceContainerLowest = Color(0xFF101108),
    surfaceContainerHigh = SproutlyColors.InkHigh,
    surfaceContainerHighest = Color(0xFF34352A),
    outline = Color(0xFF5A5C4C),
    outlineVariant = Color(0xFF35362A),
    inverseSurface = Color(0xFFF2F1E6),
    inverseOnSurface = SproutlyColors.Ink,
)

/** Brand extras the Material colour scheme has no slot for. */
data class SproutlyTokens(
    val lime: Color = SproutlyColors.Lime,
    val olive: Color = SproutlyColors.Olive,
    val success: Color = SproutlyColors.Success,
    val successContainer: Color = SproutlyColors.SuccessContainer,
    val warning: Color = SproutlyColors.Warning,
    val warningContainer: Color = SproutlyColors.WarningContainer,
    /** Flat fill for hero and call-to-action surfaces. Never a gradient. */
    val heroSurface: Color = SproutlyColors.LimeTint,
)

val LocalSproutlyTokens = staticCompositionLocalOf { SproutlyTokens() }

@Composable
fun SproutlyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val tokens = if (darkTheme) {
        SproutlyTokens(
            success = Color(0xFF9BD69B),
            successContainer = Color(0xFF1F3A22),
            warning = Color(0xFFE8C07A),
            warningContainer = Color(0xFF3D2E12),
            heroSurface = Color(0xFF252C0C),
        )
    } else {
        SproutlyTokens()
    }

    CompositionLocalProvider(LocalSproutlyTokens provides tokens) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkScheme else LightScheme,
            typography = SproutlyTypography,
            // Without this every stock component (text fields, dialogs, menus,
            // snackbars, chips, sheets) falls back to Material's near-square
            // 4-12dp corners. Slots map onto the Sproutly radius scale.
            shapes = Shapes(
                extraSmall = SproutlyShapes.Md,
                small = SproutlyShapes.Lg,
                medium = SproutlyShapes.Lg,
                large = SproutlyShapes.Xl,
                extraLarge = SproutlyShapes.Xxl,
            ),
            content = content,
        )
    }
}
