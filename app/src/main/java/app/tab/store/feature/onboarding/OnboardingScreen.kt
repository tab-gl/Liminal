package app.tab.store.feature.onboarding

import app.tab.store.core.ui.Tokens
import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.togetherWith
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
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
import androidx.compose.foundation.shape.CircleShape
import app.tab.store.core.ui.Surface
import app.tab.store.core.ui.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import app.tab.store.core.designsystem.SproutlyButton
import app.tab.store.core.designsystem.SproutlyRadii
import app.tab.store.core.designsystem.SproutlyIcon
import app.tab.store.core.designsystem.PhosphorIcons

/**
 * The two things worth asking for before the store opens.
 *
 * Only permissions the app actually uses are requested, each with a sentence
 * saying why. Notifications is the only one asked here; the install-source
 * permission is asked at the moment someone installs, where it makes sense.
 */
@Composable
fun OnboardingScreen(onDone: () -> Unit) {
    var step by remember { mutableStateOf(0) }

    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { step = 2 }

    Surface(color = Tokens.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
        ) {
            Spacer(Modifier.height(64.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(3) { index ->
                    val active = index <= step
                    Box(
                        Modifier
                            .weight(1f)
                            .height(4.dp)
                            .clip(SproutlyRadii.pill)
                            .background(Tokens.colorScheme.surfaceContainerHigh),
                    ) {
                        if (active) {
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(SproutlyRadii.pill)
                                    .background(Tokens.colorScheme.primary),
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(40.dp))

            AnimatedContent(
                targetState = step,
                transitionSpec = {
                    (slideInHorizontally(tween(320)) { it / 6 } + fadeIn(tween(280)))
                        .togetherWith(
                            slideOutHorizontally(tween(220)) { -it / 8 } + fadeOut(tween(180)),
                        )
                },
                label = "onboardingStep",
                modifier = Modifier.weight(1f),
            ) { current ->
            when (current) {
                0 -> Panel(
                    eyebrow = "Welcome",
                    title = "Apps from\nindependent\ndevelopers",
                    body = "Build it. Publish it. Grow it. Install what you like, and " +
                        "publish your own when you are ready.",
                    icon = PhosphorIcons.Store,
                    primary = "Continue" to { step = 1 },
                    secondary = null,
                )

                1 -> Panel(
                    eyebrow = "Notifications",
                    title = "Know when\nsomething\nchanges",
                    body = "We use notifications for three things: an app you installed " +
                        "got an update, a developer replied to you, and a decision about " +
                        "something you published.",
                    icon = PhosphorIcons.Notification,
                    primary = "Allow notifications" to {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            step = 2
                        }
                    },
                    secondary = "Not now" to { step = 2 },
                )

                else -> Panel(
                    eyebrow = "Ready",
                    title = "That is\nall we\nneed",
                    body = "You can change every one of these later in Settings.",
                    icon = PhosphorIcons.Tick,
                    primary = "Open Sproutly" to onDone,
                    secondary = null,
                )
            }
            }
        }
    }
}

@Composable
private fun Panel(
    eyebrow: String,
    title: String,
    body: String,
    icon: Int,
    primary: Pair<String, () -> Unit>,
    secondary: Pair<String, () -> Unit>?,
) {
    Column(Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(Tokens.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            SproutlyIcon(
                icon = icon,
                contentDescription = null,
                modifier = Modifier.size(28.dp),
                tint = Tokens.colorScheme.onPrimaryContainer,
            )
        }
        Spacer(Modifier.height(24.dp))
        Text(
            eyebrow.uppercase(),
            style = Tokens.typography.labelSmall,
            color = Tokens.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            title,
            style = Tokens.typography.displaySmall,
            color = Tokens.colorScheme.onSurface,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            body,
            style = Tokens.typography.bodyMedium,
            color = Tokens.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.weight(1f))
        SproutlyButton(text = primary.first, onClick = primary.second)
        if (secondary != null) {
            Spacer(Modifier.height(10.dp))
            Surface(
                onClick = secondary.second,
                shape = SproutlyRadii.pill,
                color = Tokens.colorScheme.surfaceContainerLow,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Box(Modifier.padding(vertical = 14.dp), contentAlignment = Alignment.Center) {
                    Text(
                        secondary.first,
                        style = Tokens.typography.labelLarge,
                        color = Tokens.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        Spacer(Modifier.height(36.dp))
    }
}
