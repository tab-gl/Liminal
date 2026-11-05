package app.tab.store.feature.onboarding

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import app.tab.store.core.designsystem.TablerIcon
import app.tab.store.core.designsystem.TablerIcons

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

    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
        ) {
            Spacer(Modifier.height(64.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(3) { index ->
                    Box(
                        Modifier
                            .weight(1f)
                            .height(4.dp)
                            .clip(SproutlyRadii.pill)
                            .background(
                                if (index <= step) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceContainerHigh,
                            ),
                    )
                }
            }

            Spacer(Modifier.height(40.dp))

            when (step) {
                0 -> Panel(
                    eyebrow = "Welcome",
                    title = "Apps from\nindependent\ndevelopers",
                    body = "Build it. Publish it. Grow it. Install what you like, and " +
                        "publish your own when you are ready.",
                    icon = TablerIcons.Store,
                    primary = "Continue" to { step = 1 },
                    secondary = null,
                )

                1 -> Panel(
                    eyebrow = "Notifications",
                    title = "Know when\nsomething\nchanges",
                    body = "We use notifications for three things: an app you installed " +
                        "got an update, a developer replied to you, and a decision about " +
                        "something you published.",
                    icon = TablerIcons.Notification,
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
                    icon = TablerIcons.Tick,
                    primary = "Open Sproutly" to onDone,
                    secondary = null,
                )
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
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            TablerIcon(
                icon = icon,
                contentDescription = null,
                modifier = Modifier.size(28.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
        Spacer(Modifier.height(24.dp))
        Text(
            eyebrow.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            title,
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.weight(1f))
        SproutlyButton(text = primary.first, onClick = primary.second)
        if (secondary != null) {
            Spacer(Modifier.height(10.dp))
            Surface(
                onClick = secondary.second,
                shape = SproutlyRadii.pill,
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Box(Modifier.padding(vertical = 14.dp), contentAlignment = Alignment.Center) {
                    Text(
                        secondary.first,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        Spacer(Modifier.height(36.dp))
    }
}
