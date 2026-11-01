package app.tab.store.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sign

/**
 * A placeholder block that pulses rather than sweeps.
 *
 * A shimmer sweep is a moving gradient, and this design system has no gradients;
 * a slow alpha pulse reads the same way ("content is coming") and stays flat.
 */
@Composable
fun Skeleton(
    modifier: Modifier = Modifier,
    height: Dp = 12.dp,
    width: Dp? = null,
    shape: RoundedCornerShape = SproutlyShapes.Sm,
) {
    val transition = rememberInfiniteTransition(label = "skeleton")
    val alpha by transition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "skeletonAlpha",
    )
    Box(
        modifier = modifier
            .then(if (width != null) Modifier.width(width) else Modifier.fillMaxWidth())
            .height(height)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = alpha), shape),
    )
}

@Composable
fun SkeletonLines(lines: Int = 3, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        repeat(lines) { index ->
            Skeleton(
                height = 9.dp,
                width = if (index == lines - 1) 120.dp else null,
            )
            if (index != lines - 1) Spacer(Modifier.height(7.dp))
        }
    }
}

/** Feed card placeholder: icon, title, two lines, meta row. */
@Composable
fun AppCardSkeleton(modifier: Modifier = Modifier) {
    SproutlyCard(modifier = modifier) {
        Row(verticalAlignment = Alignment.Top) {
            Skeleton(height = 56.dp, width = 56.dp, shape = RoundedCornerShape(16.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.fillMaxWidth()) {
                Skeleton(height = 13.dp, width = 130.dp)
                Spacer(Modifier.height(8.dp))
                SkeletonLines(lines = 2)
                Spacer(Modifier.height(10.dp))
                Row {
                    Skeleton(height = 16.dp, width = 58.dp, shape = SproutlyShapes.Pill)
                    Spacer(Modifier.width(6.dp))
                    Skeleton(height = 16.dp, width = 44.dp, shape = SproutlyShapes.Pill)
                }
            }
        }
    }
}

/**
 * The shape-morph loader: a superellipse sampled at 64 points with its exponent
 * animated between 2 (ellipse) and 5 (squircle), plus a slow quarter turn.
 *
 * A real continuous morph rather than a cross-fade, and it needs no dependency.
 * `LocalContext` is consulted so the loader collapses to a static shape when the
 * system asks for reduced motion.
 */
@Composable
fun ShapeMorphLoader(
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    color: Color = MaterialTheme.colorScheme.primary,
    strokeWidth: Dp = 0.dp,
    label: String? = null,
) {
    val reduceMotion = android.provider.Settings.Global.getFloat(
        LocalContext.current.contentResolver,
        android.provider.Settings.Global.ANIMATOR_DURATION_SCALE,
        1f,
    ) == 0f

    val transition = rememberInfiniteTransition(label = "morph")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "morphProgress",
    )

    val t = if (reduceMotion) 0.5f else progress
    val wave = 0.5f - 0.5f * cos(t * 2 * PI).toFloat()
    val exponent = 2f + 3f * wave

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Canvas(
            modifier = Modifier
                .size(size)
                .semantics { contentDescription = label ?: "Loading" },
        ) {
            val radius = this.size.minDimension / 2f
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val rotation = t * PI.toFloat() / 2f
            val path = Path()
            val segments = 64
            for (i in 0..segments) {
                val theta = (i.toFloat() / segments) * 2f * PI.toFloat() + rotation
                val c = cos(theta)
                val s = sin(theta)
                val x = center.x + radius * c.sign * abs(c).pow(2f / exponent)
                val y = center.y + radius * s.sign * abs(s).pow(2f / exponent)
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            path.close()
            if (strokeWidth > 0.dp) {
                drawPath(path, color, style = Stroke(width = strokeWidth.toPx()))
            } else {
                drawPath(path, color)
            }
        }
        if (label != null) {
            Spacer(Modifier.height(14.dp))
            Text(label, style = MaterialTheme.typography.labelSmall)
        }
    }
}
