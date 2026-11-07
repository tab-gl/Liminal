package app.tab.store.core.kit

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.tab.store.core.designsystem.Outfit
import kotlinx.coroutines.launch

/**
 * Sproutly's own design tokens. There is no Material anywhere: a colour set, a
 * type scale, a shape scale and a press effect, held in CompositionLocals.
 *
 * The property names deliberately follow the vocabulary the screens already
 * speak (`primary`, `surface`, `onSurfaceVariant`, `bodySmall`…), so a screen
 * reads `Sk.colors.primary` and `Sk.type.bodySmall` and nothing else.
 */
@Immutable
data class SkColors(
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val secondaryContainer: Color,
    val onSecondaryContainer: Color,
    val tertiaryContainer: Color,
    val onTertiaryContainer: Color,
    val background: Color,
    val onBackground: Color,
    val surface: Color,
    val onSurface: Color,
    val onSurfaceVariant: Color,
    val surfaceContainerLow: Color,
    val surfaceContainerHigh: Color,
    val outline: Color,
    val outlineVariant: Color,
    val error: Color,
    val onError: Color,
    val errorContainer: Color,
    val onErrorContainer: Color,
    val inverseSurface: Color,
    val inverseOnSurface: Color,
    val isDark: Boolean,
) {
    // Names the kit's first components were written with.
    val lime: Color get() = primaryContainer
    val limeSoft: Color get() = secondaryContainer
    val olive: Color get() = primary
    val oliveDeep: Color get() = onPrimaryContainer
    val espresso: Color get() = inverseSurface
    val cream: Color get() = background
    val surfaceHigh: Color get() = surfaceContainerHigh
    val surfaceLow: Color get() = surfaceContainerLow
    val onSurfaceMuted: Color get() = onSurfaceVariant
    val onAccent: Color get() = onPrimary
    val danger: Color get() = error
    val dangerSoft: Color get() = errorContainer
    val warn: Color get() = Color(0xFF7A5B00)
    val warnSoft: Color get() = Color(0xFFFFEFC2)

    /** The readable colour for text drawn on [container]. */
    fun contentFor(container: Color): Color = when (container) {
        primary -> onPrimary
        primaryContainer -> onPrimaryContainer
        secondaryContainer -> onSecondaryContainer
        tertiaryContainer -> onTertiaryContainer
        errorContainer -> onErrorContainer
        error -> onError
        background -> onBackground
        inverseSurface -> inverseOnSurface
        else -> onSurface
    }

    companion object {
        private val Lime = Color(0xFFDBE6A1)
        private val LimeSoft = Color(0xFFEFF4D8)
        private val LimeTint = Color(0xFFF6F9E9)
        private val Olive = Color(0xFF3F4C06)
        private val OliveDeep = Color(0xFF2A3304)
        private val Espresso = Color(0xFF3C3130)
        private val EspressoDeep = Color(0xFF221D1C)
        private val EspressoMid = Color(0xFF2C2523)
        private val EspressoSoft = Color(0xFF4A3F3D)
        private val Cream = Color(0xFFFAFBF4)
        private val Paper = Color(0xFFFFFFFF)
        private val Ink = Color(0xFF23201A)
        private val InkMuted = Color(0xFF5F6152)
        private val Outline = Color(0xFFC9CCB8)
        private val OutlineDark = Color(0xFF4E4A3F)
        private val Clay = Color(0xFFB4593A)
        private val ClaySoft = Color(0xFFF6E3DA)
        private val Berry = Color(0xFFB3261E)

        fun light() = SkColors(
            primary = Olive,
            onPrimary = LimeTint,
            primaryContainer = Lime,
            onPrimaryContainer = OliveDeep,
            secondaryContainer = LimeSoft,
            onSecondaryContainer = OliveDeep,
            tertiaryContainer = ClaySoft,
            onTertiaryContainer = Clay,
            background = Cream,
            onBackground = Ink,
            surface = Paper,
            onSurface = Ink,
            onSurfaceVariant = InkMuted,
            // Brand tint. This used to fall through to a lavender default.
            surfaceContainerLow = LimeTint,
            surfaceContainerHigh = LimeSoft,
            outline = Outline,
            outlineVariant = LimeSoft,
            error = Berry,
            onError = Paper,
            errorContainer = ClaySoft,
            onErrorContainer = Clay,
            inverseSurface = Espresso,
            inverseOnSurface = Cream,
            isDark = false,
        )

        fun dark() = SkColors(
            primary = Lime,
            onPrimary = OliveDeep,
            primaryContainer = Olive,
            onPrimaryContainer = Lime,
            secondaryContainer = OliveDeep,
            onSecondaryContainer = Lime,
            tertiaryContainer = EspressoSoft,
            onTertiaryContainer = ClaySoft,
            background = EspressoDeep,
            onBackground = Cream,
            surface = EspressoMid,
            onSurface = Cream,
            onSurfaceVariant = Color(0xFFB9B7A6),
            surfaceContainerLow = Espresso,
            surfaceContainerHigh = EspressoSoft,
            outline = OutlineDark,
            outlineVariant = EspressoSoft,
            error = Color(0xFFE8A99A),
            onError = EspressoDeep,
            errorContainer = EspressoSoft,
            onErrorContainer = ClaySoft,
            inverseSurface = Cream,
            inverseOnSurface = Ink,
            isDark = true,
        )
    }
}

private val Trim = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.None,
)

@Immutable
data class SkType(
    // The scale the first kit components used.
    val display: TextStyle = TextStyle(
        fontFamily = Outfit, fontWeight = FontWeight.Bold, fontSize = 34.sp, lineHeight = 40.sp,
    ),
    val title: TextStyle = TextStyle(
        fontFamily = Outfit, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 28.sp,
    ),
    val headline: TextStyle = TextStyle(
        fontFamily = Outfit, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, lineHeight = 24.sp,
    ),
    val body: TextStyle = TextStyle(fontFamily = Outfit, fontSize = 15.sp, lineHeight = 21.sp),
    val small: TextStyle = TextStyle(fontFamily = Outfit, fontSize = 13.sp, lineHeight = 18.sp),
    val label: TextStyle = TextStyle(
        fontFamily = Outfit, fontWeight = FontWeight.Medium, fontSize = 13.sp, lineHeight = 16.sp,
    ),
    val eyebrow: TextStyle = TextStyle(
        fontFamily = Outfit, fontWeight = FontWeight.SemiBold, fontSize = 11.sp,
        lineHeight = 14.sp, letterSpacing = 1.1.sp,
    ),
    // The scale the screens use.
    val displayLarge: TextStyle = TextStyle(
        fontFamily = Outfit, fontWeight = FontWeight.Black, fontSize = 46.sp,
        lineHeight = 50.sp, letterSpacing = (-1.4).sp, lineHeightStyle = Trim,
    ),
    val displayMedium: TextStyle = TextStyle(
        fontFamily = Outfit, fontWeight = FontWeight.Black, fontSize = 36.sp,
        lineHeight = 40.sp, letterSpacing = (-1.0).sp, lineHeightStyle = Trim,
    ),
    val displaySmall: TextStyle = TextStyle(
        fontFamily = Outfit, fontWeight = FontWeight.ExtraBold, fontSize = 30.sp,
        lineHeight = 34.sp, letterSpacing = (-0.7).sp, lineHeightStyle = Trim,
    ),
    val headlineLarge: TextStyle = TextStyle(
        fontFamily = Outfit, fontWeight = FontWeight.ExtraBold, fontSize = 26.sp,
        lineHeight = 31.sp, letterSpacing = (-0.5).sp,
    ),
    val headlineMedium: TextStyle = TextStyle(
        fontFamily = Outfit, fontWeight = FontWeight.Bold, fontSize = 22.sp,
        lineHeight = 28.sp, letterSpacing = (-0.3).sp,
    ),
    val headlineSmall: TextStyle = TextStyle(
        fontFamily = Outfit, fontWeight = FontWeight.Bold, fontSize = 19.sp,
        lineHeight = 25.sp, letterSpacing = (-0.2).sp,
    ),
    val titleLarge: TextStyle = TextStyle(
        fontFamily = Outfit, fontWeight = FontWeight.Bold, fontSize = 17.sp,
        lineHeight = 23.sp, letterSpacing = (-0.1).sp,
    ),
    val titleMedium: TextStyle = TextStyle(
        fontFamily = Outfit, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 21.sp,
    ),
    val titleSmall: TextStyle = TextStyle(
        fontFamily = Outfit, fontWeight = FontWeight.SemiBold, fontSize = 13.sp,
        lineHeight = 18.sp, letterSpacing = 0.1.sp,
    ),
    val bodyLarge: TextStyle = TextStyle(
        fontFamily = Outfit, fontWeight = FontWeight.Normal, fontSize = 15.sp,
        lineHeight = 23.sp, letterSpacing = 0.1.sp,
    ),
    val bodyMedium: TextStyle = TextStyle(
        fontFamily = Outfit, fontWeight = FontWeight.Normal, fontSize = 13.5.sp,
        lineHeight = 20.sp, letterSpacing = 0.1.sp,
    ),
    val bodySmall: TextStyle = TextStyle(
        fontFamily = Outfit, fontWeight = FontWeight.Normal, fontSize = 12.sp,
        lineHeight = 17.sp, letterSpacing = 0.2.sp,
    ),
    val labelLarge: TextStyle = TextStyle(
        fontFamily = Outfit, fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
        lineHeight = 18.sp, letterSpacing = 0.2.sp,
    ),
    val labelMedium: TextStyle = TextStyle(
        fontFamily = Outfit, fontWeight = FontWeight.SemiBold, fontSize = 11.5.sp,
        lineHeight = 15.sp, letterSpacing = 0.5.sp,
    ),
    val labelSmall: TextStyle = TextStyle(
        fontFamily = Outfit, fontWeight = FontWeight.Bold, fontSize = 10.sp,
        lineHeight = 13.sp, letterSpacing = 1.0.sp,
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

/** Generous, organic corners: the shape scale the screens ask for by size. */
@Immutable
data class SkShapes(
    val extraSmall: RoundedCornerShape = RoundedCornerShape(20.dp),
    val small: RoundedCornerShape = RoundedCornerShape(24.dp),
    val medium: RoundedCornerShape = RoundedCornerShape(30.dp),
    val large: RoundedCornerShape = RoundedCornerShape(38.dp),
    val extraLarge: RoundedCornerShape = RoundedCornerShape(48.dp),
)

private val LocalColors = staticCompositionLocalOf { SkColors.light() }
private val LocalType = staticCompositionLocalOf { SkType() }
private val LocalRadii = staticCompositionLocalOf { SkRadii() }
private val LocalShapes = staticCompositionLocalOf { SkShapes() }

/** The colour text and icons take when nothing says otherwise; surfaces set it. */
val LocalContentColor = compositionLocalOf { Color.Unspecified }

/** The text style used when a [Text] is given none. */
val LocalTextStyle = compositionLocalOf { TextStyle.Default }

/** Read the tokens: `Sk.colors.primary`, `Sk.type.bodySmall`, `Sk.shapes.small`. */
object Sk {
    val colors: SkColors
        @Composable @ReadOnlyComposable get() = LocalColors.current
    val type: SkType
        @Composable @ReadOnlyComposable get() = LocalType.current
    val radii: SkRadii
        @Composable @ReadOnlyComposable get() = LocalRadii.current
    val shapes: SkShapes
        @Composable @ReadOnlyComposable get() = LocalShapes.current
}

/**
 * Press feedback: a soft wash that comes in fast and leaves slowly. It draws one
 * rectangle in the layer's own bounds, so it follows whatever shape the element
 * was clipped to, and it replaces ripple, which allocates a layer per touch.
 */
object SkIndication : IndicationNodeFactory {
    override fun create(interactionSource: InteractionSource): DelegatableNode =
        SkIndicationNode(interactionSource)

    override fun equals(other: Any?): Boolean = other === SkIndication
    override fun hashCode(): Int = -1
}

private class SkIndicationNode(private val source: InteractionSource) :
    Modifier.Node(), DrawModifierNode {
    private val wash = Animatable(0f)

    override fun onAttach() {
        coroutineScope.launch {
            var presses = 0
            source.interactions.collect { interaction ->
                when (interaction) {
                    is PressInteraction.Press -> {
                        presses++
                        launch { wash.animateTo(1f, tween(70)) }
                    }
                    is PressInteraction.Release, is PressInteraction.Cancel -> {
                        presses = (presses - 1).coerceAtLeast(0)
                        if (presses == 0) launch { wash.animateTo(0f, tween(260)) }
                    }
                }
            }
        }
    }

    override fun ContentDrawScope.draw() {
        drawContent()
        val amount = wash.value
        if (amount > 0f) drawRect(Color(0xFF808078).copy(alpha = 0.16f * amount))
    }
}

@Composable
fun SkTheme(
    colors: SkColors = SkColors.light(),
    type: SkType = SkType(),
    radii: SkRadii = SkRadii(),
    shapes: SkShapes = SkShapes(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalColors provides colors,
        LocalType provides type,
        LocalRadii provides radii,
        LocalShapes provides shapes,
        LocalIndication provides SkIndication,
        LocalContentColor provides colors.onSurface,
        LocalTextStyle provides type.bodyLarge,
        content = content,
    )
}

/** Shorthand used across the kit. */
val SkCardRadius = 28.dp
val SkHeroRadius = 36.dp
val SkSheetRadius = 40.dp
