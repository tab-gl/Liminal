package app.tab.store.feature.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.tab.store.data.FirestorePaths
import app.tab.store.di.ServiceGraph
import app.tab.store.ui.components.ButtonSize
import app.tab.store.ui.components.SproutlyButton
import app.tab.store.ui.components.SproutlyCard
import app.tab.store.ui.components.SproutlyPill
import app.tab.store.ui.theme.LocalSproutlyTokens
import com.google.firebase.firestore.FieldValue
import kotlinx.coroutines.launch

private data class Step(val title: String, val body: String, val cta: String)

private val Steps = listOf(
    Step(
        "Build it. Publish it. Grow it.",
        "Sproutly is where independent developers put their apps in front of people " +
            "who are actively looking for something new. Publishing is free, forever.",
        "Show me around",
    ),
    Step(
        "Built on real signals",
        "Ratings, views and rankings come from real events, counted on the server. " +
            "Nobody can buy a rating, and paid placements are always labelled.",
        "Next",
    ),
    Step(
        "You are set",
        "Save apps to come back to, follow developers, and publish your own listing in " +
            "about five minutes.",
        "Start exploring",
    ),
)

/**
 * First-run introduction.
 *
 * Gated on `onboarded` in the profile document rather than a local flag, so a
 * reinstall does not replay it and a second device does not skip it.
 */
@Composable
fun OnboardingScreen(onDone: () -> Unit) {
    var index by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    val step = Steps[index]

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        SproutlyPill(label = "${index + 1} of ${Steps.size}")
        Spacer(Modifier.height(18.dp))
        Text(step.title, style = MaterialTheme.typography.displaySmall)
        Spacer(Modifier.height(12.dp))
        Text(
            text = step.body,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(24.dp))
        if (index == 0) {
            SproutlyCard(color = LocalSproutlyTokens.current.heroSurface) {
                Text("Free to publish", style = MaterialTheme.typography.titleSmall)
                Text(
                    text = "No listing fee, no revenue share, no paywall on discovery.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(24.dp))
        }
        SproutlyButton(
            label = step.cta,
            size = ButtonSize.Large,
            fillWidth = true,
            onClick = {
                if (index < Steps.size - 1) {
                    index++
                } else {
                    val uid = ServiceGraph.auth.uid
                    if (uid != null) {
                        scope.launch {
                            runCatching {
                                FirestorePaths.user(uid).update("onboarded", true, "updatedAt", FieldValue.serverTimestamp())
                            }
                            onDone()
                        }
                    } else {
                        onDone()
                    }
                }
            },
        )
    }
}
