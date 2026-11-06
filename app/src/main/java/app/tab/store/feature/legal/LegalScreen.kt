package app.tab.store.feature.legal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.tab.store.core.kit.Sk
import app.tab.store.core.kit.SkChip
import app.tab.store.core.kit.SkDivider
import app.tab.store.core.kit.SkSurface
import app.tab.store.core.kit.SkText
import app.tab.store.core.kit.SkTheme
import app.tab.store.core.designsystem.SproutlyTopBar

/**
 * Privacy and terms.
 *
 * Written to be read: short paragraphs, no clause numbers, nothing that exists
 * only to protect the person who wrote it. Rendered with Sproutly's own
 * components — no Material.
 */
@Composable
fun LegalScreen(startOnTerms: Boolean, onBack: () -> Unit) {
    var showTerms by remember { mutableStateOf(startOnTerms) }

    SkTheme {
        Column(Modifier.fillMaxSize().background(Sk.colors.background)) {
            SproutlyTopBar(title = "Legal", onBack = onBack)

            Column(Modifier.padding(horizontal = 20.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SkChip("Privacy", selected = !showTerms) { showTerms = false }
                    SkChip("Terms", selected = showTerms) { showTerms = true }
                }
                Spacer(Modifier.height(18.dp))
            }

            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
            ) {
                SkText(
                    if (showTerms) "Terms of service" else "Privacy policy",
                    style = Sk.type.display,
                )
                Spacer(Modifier.height(6.dp))
                SkText(
                    "Last updated 5 November 2026",
                    style = Sk.type.small,
                    color = Sk.colors.onSurfaceMuted,
                )
                Spacer(Modifier.height(18.dp))

                val sections = if (showTerms) terms else privacy
                sections.forEach { (heading, body) ->
                    SkSurface(modifier = Modifier.padding(bottom = 10.dp)) {
                        Column(Modifier.padding(16.dp)) {
                            SkText(heading, style = Sk.type.headline)
                            Spacer(Modifier.height(6.dp))
                            SkText(body, style = Sk.type.body, color = Sk.colors.onSurfaceMuted)
                        }
                    }
                }
                Spacer(Modifier.height(40.dp))
            }
        }
    }
}

private val privacy = listOf(
    "What we keep" to
        "Your email, your display name, the apps you install, the reviews you write " +
        "and the apps you publish. Nothing else.",
    "Where builds live" to
        "App builds are stored on Cloudflare R2. Pictures are stored on Cloudinary. " +
        "Both are reached over HTTPS.",
    "Who can see what" to
        "Your profile is visible to signed-in people. Your email is visible to admins " +
        "only. Your installs are visible to you and to the developer of the app.",
    "Deleting" to
        "Ask and your account is removed along with its installs and reviews. " +
        "Listings you published are removed too.",
)

private val terms = listOf(
    "Publishing" to
        "Publish only what you have the right to publish, and keep it working. " +
        "Every build is scanned when it is uploaded; the scan is shown to people " +
        "before they install.",
    "Installing" to
        "Installs happen through Android's own installer. Sproutly hands it the " +
        "build and asks Android for permission; it never installs anything quietly.",
    "Moderation" to
        "A listing can be removed if it breaks these terms or if it is reported and " +
        "the report holds up. You will be told which and why.",
    "Money" to
        "Paid apps need a verified developer profile. Sproutly takes no cut today.",
)
