package app.tab.store.core.kit

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Sproutly's components, built here rather than inherited.
 *
 * Everything in this file is composed from Compose Foundation: BasicText,
 * BasicTextField, Box and Row. No Material. That is what lets every corner be
 * round by construction — a card is a rounded rectangle because that is what it
 * is, not because a shape token was overridden.
 */

@Composable
fun SkText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle? = null,
    color: Color? = null,
    maxLines: Int = Int.MAX_VALUE,
    align: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Ellipsis,
) {
    BasicText(
        text = text,
        modifier = modifier,
        style = (style ?: Sk.type.body).copy(
            color = color ?: Sk.colors.onSurface,
            textAlign = align ?: TextAlign.Start,
        ),
        maxLines = maxLines,
        overflow = overflow,
    )
}

/** A surface: rounded, with an optional press ripple-free feedback. */
@Composable
fun SkSurface(
    modifier: Modifier = Modifier,
    color: Color = Sk.colors.surface,
    radius: Dp = SkCardRadius,
    borderColor: Color? = null,
    elevation: Dp = 0.dp,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.985f else 1f,
        animationSpec = tween(90),
        label = "skPress",
    )

    Column(
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(radius))
            .background(color)
            .then(
                if (borderColor != null) {
                    Modifier.border(1.dp, borderColor, RoundedCornerShape(radius))
                } else {
                    Modifier
                },
            )
            .then(
                if (onClick != null && enabled) {
                    Modifier.clickable(
                        interactionSource = interaction,
                        indication = null,
                        onClick = onClick,
                    )
                } else {
                    Modifier
                },
            )
            .then(if (elevation > 0.dp) Modifier else Modifier),
        content = content,
    )
}

@Composable
fun SkButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    filled: Boolean = true,
) {
    val background = when {
        !enabled -> Sk.colors.surfaceHigh
        filled -> Sk.colors.olive
        else -> Sk.colors.limeSoft
    }
    val content = when {
        !enabled -> Sk.colors.onSurfaceMuted
        filled -> Sk.colors.lime
        else -> Sk.colors.olive
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(CircleShape)
            .background(background)
            .clickable(enabled = enabled && !loading, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (loading) {
                SkSpinner(size = 20.dp, color = content)
                Spacer(Modifier.width(10.dp))
            }
            SkText(text, style = Sk.type.label, color = content, maxLines = 1)
        }
    }
}

/** A small round spinner drawn with three rotating dots — no Material progress. */
@Composable
fun SkSpinner(size: Dp = 20.dp, color: Color = Sk.colors.olive) {
    val transition = androidx.compose.animation.core.rememberInfiniteTransition(label = "spin")
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = tween(900, easing = androidx.compose.animation.core.LinearEasing),
        ),
        label = "angle",
    )
    androidx.compose.foundation.Canvas(Modifier.size(size)) {
        val stroke = this.size.minDimension * 0.16f
        rotate(angle) {
            drawArc(
                color = color,
                startAngle = 0f,
                sweepAngle = 280f,
                useCenter = false,
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = stroke,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round,
                ),
            )
        }
    }
}

@Composable
fun SkChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(if (selected) Sk.colors.olive else Sk.colors.surface)
            .border(
                1.dp,
                if (selected) Sk.colors.olive else Sk.colors.outline,
                CircleShape,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        SkText(
            text,
            style = Sk.type.label,
            color = if (selected) Sk.colors.lime else Sk.colors.onSurface,
            maxLines = 1,
        )
    }
}

@Composable
fun SkTag(text: String, filled: Boolean = false, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(if (filled) Sk.colors.olive else Sk.colors.limeSoft)
            .padding(horizontal = 10.dp, vertical = 5.dp),
    ) {
        SkText(
            text,
            style = Sk.type.eyebrow,
            color = if (filled) Sk.colors.lime else Sk.colors.olive,
            maxLines = 1,
        )
    }
}

@Composable
fun SkDivider(modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Sk.colors.outline.copy(alpha = 0.6f)),
    )
}

/** A determinate bar; [progress] null means "working, size unknown". */
@Composable
fun SkProgress(progress: Float?, modifier: Modifier = Modifier) {
    val radius = CircleShape
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(radius)
            .background(Sk.colors.surfaceHigh),
    ) {
        if (progress == null) {
            val transition = androidx.compose.animation.core.rememberInfiniteTransition(label = "bar")
            val shift by transition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = androidx.compose.animation.core.infiniteRepeatable(
                    animation = tween(1100, easing = androidx.compose.animation.core.LinearEasing),
                ),
                label = "shift",
            )
            Box(
                Modifier
                    .fillMaxWidth(0.4f)
                    .height(8.dp)
                    .padding(start = (shift * 100).dp)
                    .clip(radius)
                    .background(Sk.colors.olive),
            )
        } else {
            Box(
                Modifier
                    .fillMaxWidth(progress.coerceIn(0f, 1f))
                    .height(8.dp)
                    .clip(radius)
                    .background(Sk.colors.olive),
            )
        }
    }
}

@Composable
fun SkTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String? = null,
    placeholder: String? = null,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true,
    minHeight: Dp = 56.dp,
) {
    Column(modifier) {
        if (label != null) {
            SkText(label, style = Sk.type.eyebrow, color = Sk.colors.onSurfaceMuted)
            Spacer(Modifier.height(6.dp))
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(minHeight)
                .clip(RoundedCornerShape(SkCardRadius))
                .background(Sk.colors.surfaceHigh)
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            if (value.isEmpty() && placeholder != null) {
                SkText(placeholder, style = Sk.type.body, color = Sk.colors.onSurfaceMuted)
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = singleLine,
                textStyle = Sk.type.body.copy(color = Sk.colors.onSurface),
                cursorBrush = SolidColor(Sk.colors.olive),
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** A rounded avatar: an image if there is one, otherwise the person's initial. */
@Composable
fun SkAvatar(initial: String, size: Dp = 44.dp, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(Sk.colors.limeSoft),
        contentAlignment = Alignment.Center,
    ) {
        SkText(
            initial.take(1).uppercase(),
            style = Sk.type.headline,
            color = Sk.colors.olive,
        )
    }
}

@Composable
fun SkSectionHeader(title: String, action: String? = null, onAction: (() -> Unit)? = null) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        SkText(title, style = Sk.type.headline)
        if (action != null && onAction != null) {
            Box(Modifier.clip(CircleShape).clickable(onClick = onAction).padding(6.dp)) {
                SkText(action, style = Sk.type.label, color = Sk.colors.olive)
            }
        }
    }
}

@Composable
fun SkEmpty(title: String, message: String, action: String? = null, onAction: (() -> Unit)? = null) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        SkText(title, style = Sk.type.headline, align = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        SkText(
            message,
            style = Sk.type.small,
            color = Sk.colors.onSurfaceMuted,
            align = TextAlign.Center,
        )
        if (action != null && onAction != null) {
            Spacer(Modifier.height(14.dp))
            SkChip(action, selected = true, onClick = onAction)
        }
    }
}
