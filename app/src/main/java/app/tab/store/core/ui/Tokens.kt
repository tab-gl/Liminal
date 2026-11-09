package app.tab.store.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.tab.store.core.designsystem.Outfit

/**
 * The app's own design tokens, and the only theme object the UI reads.
 *
 * This replaces MaterialTheme. The names are deliberately close to the ones the
 * screens already used — `Tokens.colorScheme.primary`, `Tokens.typography.bodyMedium`
 * — so the migration is a rename rather than a rewrite of every call site, but the
 * values are ours and nothing is inherited from Material.
 */
@Immutable
data class SkColorScheme(
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val secondary: Color,
    val onSecondary: Color,
    val secondaryContainer: Color,
    val onSecondaryContainer: Color,
    val tertiary: Color,
    val onTertiary: Color,
    val tertiaryContainer: Color,
    val onTertiaryContainer: Color,
    val background: Color,
    val onBackground: Color,
    val surface: Color,
    val onSurface: Color,
    val surfaceVariant: Color,
    val onSurfaceVariant: Color,
    val surfaceContainer: Color,
    val surfaceContainerLow: Color,
    val surfaceContainerHigh: Color,
    val surfaceContainerHighest: Color,
    val outline: Color,
    val outlineVariant: Color,
    val error: Color,
    val onError: Color,
    val errorContainer: Color,
    val onErrorContainer: Color,
    val inverseSurface: Color,
    val inverseOnSurface: Color,
    val scrim: Color,
    /** The accent as it reads on [inverseSurface]; for dark feature cards. */
    val inversePrimary: Color = Color.Unspecified,
)

@Immutable
data class SkTypography(
    val displayLarge: TextStyle,
    val displayMedium: TextStyle,
    val displaySmall: TextStyle,
    val headlineLarge: TextStyle,
    val headlineMedium: TextStyle,
    val headlineSmall: TextStyle,
    val titleLarge: TextStyle,
    val titleMedium: TextStyle,
    val titleSmall: TextStyle,
    val bodyLarge: TextStyle,
    val bodyMedium: TextStyle,
    val bodySmall: TextStyle,
    val labelLarge: TextStyle,
    val labelMedium: TextStyle,
    val labelSmall: TextStyle,
)

/** Corner shapes, used by every rounded thing in the app. */
@Immutable
data class SkShapes(
    val extraSmall: androidx.compose.ui.graphics.Shape =
        androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
    val small: androidx.compose.ui.graphics.Shape =
        androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
    val medium: androidx.compose.ui.graphics.Shape =
        androidx.compose.foundation.shape.RoundedCornerShape(30.dp),
    val large: androidx.compose.ui.graphics.Shape =
        androidx.compose.foundation.shape.RoundedCornerShape(38.dp),
    val extraLarge: androidx.compose.ui.graphics.Shape =
        androidx.compose.foundation.shape.RoundedCornerShape(48.dp),
)

private val LocalColors = staticCompositionLocalOf<SkColorScheme> {
    error("Tokens.colors read outside SkTheme")
}
private val LocalType = staticCompositionLocalOf<SkTypography> {
    error("Tokens.typography read outside SkTheme")
}
private val LocalShapes = staticCompositionLocalOf { SkShapes() }
private val LocalContent = staticCompositionLocalOf<Color> { Color.Unspecified }

/** The one theme object screens read. */
object Tokens {
    val colorScheme: SkColorScheme
        @Composable @ReadOnlyComposable get() = LocalColors.current
    val typography: SkTypography
        @Composable @ReadOnlyComposable get() = LocalType.current
    val shapes: SkShapes
        @Composable @ReadOnlyComposable get() = LocalShapes.current
}

/** Material calls this LocalContentColor; the name is kept for the call sites. */
val LocalContentColor = LocalContent

@Composable
fun ProvideTokens(
    colors: SkColorScheme,
    typography: SkTypography,
    shapes: SkShapes = SkShapes(),
    contentColor: Color = colors.onBackground,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalColors provides colors,
        LocalType provides typography,
        LocalShapes provides shapes,
        LocalContent provides contentColor,
        content = content,
    )
}

/** The default light scheme: lime, olive, espresso. */
fun lightScheme(
    primary: Color,
    onPrimary: Color,
    primaryContainer: Color,
    onPrimaryContainer: Color,
    secondary: Color,
    onSecondary: Color,
    secondaryContainer: Color,
    onSecondaryContainer: Color,
    tertiary: Color,
    onTertiary: Color,
    tertiaryContainer: Color,
    onTertiaryContainer: Color,
    background: Color,
    onBackground: Color,
    surface: Color,
    onSurface: Color,
    surfaceVariant: Color,
    onSurfaceVariant: Color,
    surfaceContainer: Color,
    surfaceContainerHigh: Color,
    outline: Color,
    outlineVariant: Color,
    error: Color,
    onError: Color,
    errorContainer: Color,
    onErrorContainer: Color,
    inverseSurface: Color,
    inverseOnSurface: Color,
    inversePrimary: Color = Color.Unspecified,
) = SkColorScheme(
    primary = primary,
    onPrimary = onPrimary,
    primaryContainer = primaryContainer,
    onPrimaryContainer = onPrimaryContainer,
    secondary = secondary,
    onSecondary = onSecondary,
    secondaryContainer = secondaryContainer,
    onSecondaryContainer = onSecondaryContainer,
    tertiary = tertiary,
    onTertiary = onTertiary,
    tertiaryContainer = tertiaryContainer,
    onTertiaryContainer = onTertiaryContainer,
    background = background,
    onBackground = onBackground,
    surface = surface,
    onSurface = onSurface,
    surfaceVariant = surfaceVariant,
    onSurfaceVariant = onSurfaceVariant,
    surfaceContainer = surfaceContainer,
    surfaceContainerLow = surfaceContainer,
    surfaceContainerHigh = surfaceContainerHigh,
    surfaceContainerHighest = surfaceContainerHigh,
    outline = outline,
    outlineVariant = outlineVariant,
    error = error,
    onError = onError,
    errorContainer = errorContainer,
    onErrorContainer = onErrorContainer,
    inverseSurface = inverseSurface,
    inverseOnSurface = inverseOnSurface,
    scrim = Color(0x52000000),
    inversePrimary = if (inversePrimary == Color.Unspecified) primary else inversePrimary,
)

/** Outfit, weighted the way the brand uses it. */
fun sproutlyTypography(
    displayLarge: TextStyle,
    displayMedium: TextStyle,
    displaySmall: TextStyle,
    headlineLarge: TextStyle,
    headlineMedium: TextStyle,
    headlineSmall: TextStyle,
    titleLarge: TextStyle,
    titleMedium: TextStyle,
    titleSmall: TextStyle,
    bodyLarge: TextStyle,
    bodyMedium: TextStyle,
    bodySmall: TextStyle,
    labelLarge: TextStyle,
    labelMedium: TextStyle,
    labelSmall: TextStyle,
) = SkTypography(
    displayLarge = displayLarge.copy(fontFamily = Outfit),
    displayMedium = displayMedium.copy(fontFamily = Outfit),
    displaySmall = displaySmall.copy(fontFamily = Outfit),
    headlineLarge = headlineLarge.copy(fontFamily = Outfit),
    headlineMedium = headlineMedium.copy(fontFamily = Outfit),
    headlineSmall = headlineSmall.copy(fontFamily = Outfit),
    titleLarge = titleLarge.copy(fontFamily = Outfit),
    titleMedium = titleMedium.copy(fontFamily = Outfit),
    titleSmall = titleSmall.copy(fontFamily = Outfit),
    bodyLarge = bodyLarge.copy(fontFamily = Outfit),
    bodyMedium = bodyMedium.copy(fontFamily = Outfit),
    bodySmall = bodySmall.copy(fontFamily = Outfit),
    labelLarge = labelLarge.copy(fontFamily = Outfit),
    labelMedium = labelMedium.copy(fontFamily = Outfit),
    labelSmall = labelSmall.copy(fontFamily = Outfit),
)

/** Default text style when a screen does not name one. */
val DefaultTextStyle = TextStyle(
    fontFamily = Outfit,
    fontWeight = FontWeight.Normal,
    fontSize = 15.sp,
    lineHeight = 21.sp,
)

/** Annotations Material marked its experimental APIs with; kept so call sites compile. */
@RequiresOptIn(level = RequiresOptIn.Level.WARNING)
annotation class ExperimentalMaterial3Api

@RequiresOptIn(level = RequiresOptIn.Level.WARNING)
annotation class ExperimentalMaterial3ExpressiveApi
