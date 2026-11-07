package app.tab.store.core.kit

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp

/*
 * The building blocks every screen sits on. They are small on purpose: a Box, a
 * background, a clip. Nothing here reads Material, allocates a ripple, or animates
 * on the main thread when it could draw.
 */

/** Text. Colour falls back to the surface's content colour, style to the theme's body. */
@Composable
fun Text(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    onTextLayout: ((TextLayoutResult) -> Unit)? = null,
    style: TextStyle = LocalTextStyle.current,
) {
    val resolved = color.takeOrElse { style.color.takeOrElse { LocalContentColor.current } }
    val merged = style.merge(
        TextStyle(
            color = resolved,
            fontSize = fontSize,
            fontWeight = fontWeight,
            fontStyle = fontStyle,
            fontFamily = fontFamily,
            letterSpacing = letterSpacing,
            textDecoration = textDecoration,
            textAlign = textAlign ?: TextAlign.Unspecified,
            lineHeight = lineHeight,
        ),
    )
    BasicText(
        text = text,
        modifier = modifier,
        style = merged,
        onTextLayout = onTextLayout,
        overflow = overflow,
        softWrap = softWrap,
        maxLines = maxLines,
        minLines = minLines,
    )
}

/** A plain surface: a shape, a fill, a border, a shadow, and the content colour for its fill. */
@Composable
fun Surface(
    modifier: Modifier = Modifier,
    shape: Shape = RectangleShape,
    color: Color = Sk.colors.surface,
    contentColor: Color = Sk.colors.contentFor(color),
    tonalElevation: Dp = 0.dp,
    shadowElevation: Dp = 0.dp,
    border: BorderStroke? = null,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .surfaceLook(shape, color, shadowElevation, border),
        propagateMinConstraints = true,
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor, content = content)
    }
}

/** A surface you can press. */
@Composable
fun Surface(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = RectangleShape,
    color: Color = Sk.colors.surface,
    contentColor: Color = Sk.colors.contentFor(color),
    tonalElevation: Dp = 0.dp,
    shadowElevation: Dp = 0.dp,
    border: BorderStroke? = null,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable () -> Unit,
) {
    val source = interactionSource ?: remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .surfaceLook(shape, color, shadowElevation, border)
            .clickable(
                interactionSource = source,
                indication = androidx.compose.foundation.LocalIndication.current,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            ),
        propagateMinConstraints = true,
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor, content = content)
    }
}

private fun Modifier.surfaceLook(
    shape: Shape,
    color: Color,
    shadowElevation: Dp,
    border: BorderStroke?,
): Modifier = this
    .then(if (shadowElevation > 0.dp) Modifier.shadow(shadowElevation, shape, clip = false) else Modifier)
    .then(if (border != null) Modifier.border(border, shape) else Modifier)
    .background(color, shape)
    .clip(shape)

/** A vector icon. Sized 24dp unless the modifier says otherwise. */
@Composable
fun Icon(
    imageVector: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
) = Icon(rememberVectorPainter(imageVector), contentDescription, modifier, tint)

/** A painter icon. Sized 24dp unless the modifier says otherwise. */
@Composable
fun Icon(
    painter: Painter,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
) {
    Image(
        painter = painter,
        contentDescription = contentDescription,
        modifier = modifier.size(24.dp),
        colorFilter = if (tint == Color.Unspecified) null else ColorFilter.tint(tint),
    )
}

/** A round, press-able container for one icon. */
@Composable
fun IconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(CircleShape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        val tint = LocalContentColor.current
        CompositionLocalProvider(
            LocalContentColor provides if (enabled) tint else tint.copy(alpha = 0.38f),
            content = content,
        )
    }
}

@Immutable
class ButtonColors(
    val containerColor: Color,
    val contentColor: Color,
    val disabledContainerColor: Color,
    val disabledContentColor: Color,
)

object ButtonDefaults {
    @Composable
    fun buttonColors(
        containerColor: Color = Sk.colors.primary,
        contentColor: Color = Sk.colors.onPrimary,
        disabledContainerColor: Color = Sk.colors.onSurface.copy(alpha = 0.12f),
        disabledContentColor: Color = Sk.colors.onSurface.copy(alpha = 0.38f),
    ) = ButtonColors(containerColor, contentColor, disabledContainerColor, disabledContentColor)

    @Composable
    fun textButtonColors(
        containerColor: Color = Color.Transparent,
        contentColor: Color = Sk.colors.primary,
        disabledContainerColor: Color = Color.Transparent,
        disabledContentColor: Color = Sk.colors.onSurface.copy(alpha = 0.38f),
    ) = ButtonColors(containerColor, contentColor, disabledContainerColor, disabledContentColor)

    val ContentPadding = PaddingValues(horizontal = 24.dp, vertical = 10.dp)
    val TextButtonContentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
}

@Composable
fun Button(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = CircleShape,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    content: @Composable RowScope.() -> Unit,
) {
    val container = if (enabled) colors.containerColor else colors.disabledContainerColor
    val content_ = if (enabled) colors.contentColor else colors.disabledContentColor
    Row(
        modifier = modifier
            .defaultMinSize(minHeight = 44.dp)
            .background(container, shape)
            .clip(shape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(contentPadding),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CompositionLocalProvider(
            LocalContentColor provides content_,
            LocalTextStyle provides Sk.type.labelLarge,
        ) { content() }
    }
}

@Composable
fun TextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = CircleShape,
    colors: ButtonColors = ButtonDefaults.textButtonColors(),
    contentPadding: PaddingValues = ButtonDefaults.TextButtonContentPadding,
    content: @Composable RowScope.() -> Unit,
) = Button(onClick, modifier, enabled, shape, colors, contentPadding, content)

/** A one-pixel rule. */
@Composable
fun HorizontalDivider(
    modifier: Modifier = Modifier,
    thickness: Dp = 1.dp,
    color: Color = Sk.colors.outlineVariant,
) {
    Box(modifier.fillMaxWidth().height(thickness).background(color))
}

/** A determinate bar. The fill is drawn, not composed, so progress never recomposes. */
@Composable
fun LinearProgressIndicator(
    progress: () -> Float,
    modifier: Modifier = Modifier,
    color: Color = Sk.colors.primary,
    trackColor: Color = Sk.colors.secondaryContainer,
) {
    Canvas(modifier.fillMaxWidth().height(4.dp)) {
        val radius = size.height / 2f
        drawRoundRect(trackColor, cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius))
        val fraction = progress().coerceIn(0f, 1f)
        if (fraction > 0f) {
            drawRoundRect(
                color,
                size = Size(maxOf(size.height, size.width * fraction), size.height),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius),
            )
        }
    }
}

/** An indeterminate bar: a pill that slides across the track. */
@Composable
fun LinearProgressIndicator(
    modifier: Modifier = Modifier,
    color: Color = Sk.colors.primary,
    trackColor: Color = Sk.colors.secondaryContainer,
) {
    val transition = rememberInfiniteTransition(label = "bar")
    val shift by transition.animateFloat(
        initialValue = -0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1100, easing = LinearEasing), RepeatMode.Restart),
        label = "bar-shift",
    )
    Canvas(modifier.fillMaxWidth().height(4.dp)) {
        val radius = androidx.compose.ui.geometry.CornerRadius(size.height / 2f)
        drawRoundRect(trackColor, cornerRadius = radius)
        val width = size.width * 0.4f
        val start = (size.width * shift).coerceAtLeast(0f)
        val end = (size.width * shift + width).coerceAtMost(size.width)
        if (end > start) {
            drawRoundRect(color, topLeft = Offset(start, 0f), size = Size(end - start, size.height), cornerRadius = radius)
        }
    }
}

/** A spinning arc. Rotation is read while drawing, so nothing recomposes per frame. */
@Composable
fun LoadingIndicator(
    modifier: Modifier = Modifier,
    color: Color = Sk.colors.primary,
) {
    val transition = rememberInfiniteTransition(label = "spin")
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Restart),
        label = "spin-angle",
    )
    Canvas(modifier.size(40.dp)) {
        val stroke = size.minDimension * 0.14f
        val inset = stroke / 2f
        rotate(angle) {
            drawArc(
                color = color,
                startAngle = 0f,
                sweepAngle = 270f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = Size(size.width - stroke, size.height - stroke),
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
        }
    }
}

/** Fades a composable when it is disabled. */
fun Modifier.dimmedWhen(disabled: Boolean): Modifier = if (disabled) alpha(0.38f) else this
