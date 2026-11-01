package app.tab.store.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

enum class ButtonVariant { Primary, Tonal, Outline, Ghost, Destructive }

enum class ButtonSize { Small, Medium, Large }

/**
 * The Sproutly button: pill shaped, scales to 0.96 while held, deepens its fill
 * on press, and swaps the label for a spinner without changing width so the
 * layout never jumps.
 */
@Composable
fun SproutlyButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: ButtonVariant = ButtonVariant.Primary,
    size: ButtonSize = ButtonSize.Medium,
    icon: ImageVector? = null,
    loading: Boolean = false,
    enabled: Boolean = true,
    fillWidth: Boolean = false,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = tween(durationMillis = 90),
        label = "buttonScale",
    )

    val scheme = MaterialTheme.colorScheme
    val container: Color
    val content: Color
    val border: BorderStroke?
    when (variant) {
        ButtonVariant.Primary -> { container = scheme.primary; content = scheme.onPrimary; border = null }
        ButtonVariant.Tonal -> { container = scheme.primaryContainer; content = scheme.onPrimaryContainer; border = null }
        ButtonVariant.Outline -> { container = Color.Transparent; content = scheme.onSurface; border = BorderStroke(1.dp, scheme.outline) }
        ButtonVariant.Ghost -> { container = Color.Transparent; content = scheme.onSurface; border = null }
        ButtonVariant.Destructive -> { container = scheme.error; content = scheme.onError; border = null }
    }

    val height = when (size) {
        ButtonSize.Small -> 40.dp
        ButtonSize.Medium -> 52.dp
        ButtonSize.Large -> 60.dp
    }
    val padding = when (size) {
        ButtonSize.Small -> PaddingValues(horizontal = 16.dp)
        ButtonSize.Medium -> PaddingValues(horizontal = 22.dp)
        ButtonSize.Large -> PaddingValues(horizontal = 28.dp)
    }
    val active = enabled && !loading

    Surface(
        onClick = onClick,
        modifier = modifier
            .scale(scale)
            .then(if (fillWidth) Modifier.fillMaxWidth() else Modifier)
            .defaultMinSize(minHeight = height),
        enabled = active,
        shape = CircleShape,
        color = if (active) container else container.copy(alpha = 0.35f),
        contentColor = content,
        border = border,
        interactionSource = interaction,
    ) {
        Row(
            modifier = Modifier.padding(padding),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.2.dp,
                    color = content,
                )
            } else if (icon != null) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(19.dp))
                Spacer(Modifier.width(8.dp))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = if (active) content else content.copy(alpha = 0.5f),
            )
        }
    }
}

@Composable
fun SproutlyTextButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = CircleShape,
        color = Color.Transparent,
        contentColor = LocalContentColor.current,
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(PaddingValues(horizontal = 12.dp, vertical = 8.dp)),
            style = MaterialTheme.typography.labelLarge,
        )
    }
}
