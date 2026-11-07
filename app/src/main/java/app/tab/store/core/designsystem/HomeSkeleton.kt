package app.tab.store.core.designsystem

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * What the storefront looks like before we know who you are.
 *
 * It used to be an empty surface. A skeleton is honest about what is coming and
 * makes the first second feel like the app opening rather than the app hesitating
 * — and it is what shows instead of the sign-in screen when someone is already
 * signed in.
 */
@Composable
fun HomeSkeleton(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "skeleton")
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(900),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "skeletonAlpha",
    )
    val bone = Tokens.colorScheme.surfaceContainerHigh.copy(alpha = alpha)

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(18.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).clip(CircleShape).background(bone))
            Spacer(Modifier.width(10.dp))
            Box(Modifier.width(120.dp).height(20.dp).clip(SproutlyRadii.pill).background(bone))
            Spacer(Modifier.weight(1f))
            Box(Modifier.size(28.dp).clip(CircleShape).background(bone))
        }

        Spacer(Modifier.height(26.dp))
        Box(Modifier.fillMaxWidth(0.7f).height(30.dp).clip(SproutlyRadii.pill).background(bone))
        Spacer(Modifier.height(10.dp))
        Box(Modifier.fillMaxWidth(0.45f).height(16.dp).clip(SproutlyRadii.pill).background(bone))

        Spacer(Modifier.height(22.dp))
        Box(Modifier.fillMaxWidth().height(52.dp).clip(SproutlyRadii.pill).background(bone))

        Spacer(Modifier.height(20.dp))
        Box(Modifier.fillMaxWidth().height(168.dp).clip(SproutlyRadii.hero).background(bone))

        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(3) {
                Box(Modifier.width(96.dp).height(38.dp).clip(SproutlyRadii.pill).background(bone))
            }
        }

        Spacer(Modifier.height(22.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            repeat(2) {
                Column(Modifier.weight(1f)) {
                    Box(Modifier.fillMaxWidth().height(120.dp).clip(SproutlyRadii.card).background(bone))
                    Spacer(Modifier.height(8.dp))
                    Box(Modifier.fillMaxWidth(0.8f).height(14.dp).clip(SproutlyRadii.pill).background(bone))
                }
            }
        }
    }
}
