package app.tab.store.feature.legal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.unit.dp
import app.tab.store.core.designsystem.SproutlyChip
import app.tab.store.core.designsystem.SproutlyRadii
import app.tab.store.core.designsystem.SproutlyTopBar
import app.tab.store.core.designsystem.TablerIcons
import app.tab.store.core.designsystem.TablerIcon

private data class Section(val heading: String, val body: String)

private val privacy: List<Section> = listOf(
    Section(
        "What we collect",
        "Your name, email address and profile photo from the sign-in you choose. " +
            "What you install, what you save and what you publish. Nothing else.",
    ),
    Section(
        "Why we hold it",
        "To run your library across devices, show developers how their apps are doing, " +
            "and keep the store free of harmful listings. We do not sell it and we do not " +
            "build advertising profiles from it.",
    ),
    Section(
        "Who can see what",
        "Your profile is visible to you and to store moderators. Developers see install " +
            "counts, never who installed. Reports you file are anonymous to the developer.",
    ),
    Section(
        "Where it lives",
        "Account and catalogue data are stored in Google Cloud Firestore. Images and app " +
            "builds are stored in Cloudinary. Both are encrypted in transit.",
    ),
    Section(
        "Your choices",
        "You can edit your profile, remove saved apps, and delete your account by writing " +
            "to support@sproutly.app. Deleting the account removes your profile and library; " +
            "install records kept by developers become anonymous.",
    ),
)

private val terms: List<Section> = listOf(
    Section(
        "Using Sproutly",
        "You need an account to install or publish. Keep your credentials to yourself; " +
            "you are responsible for what happens under your account.",
    ),
    Section(
        "For developers",
        "Publish only apps you have the right to distribute. Paid apps require a verified " +
            "developer account. You keep ownership of your app; you grant us permission to " +
            "host and deliver the build to people who install it.",
    ),
    Section(
        "Not allowed",
        "Malware, spyware, apps that mislead people about what they do, apps that collect " +
            "data without saying so, and anything unlawful. Listings that break these rules " +
            "are removed and repeat offenders lose publishing rights.",
    ),
    Section(
        "Purchases",
        "Prices are set by developers and shown before you install. Refunds follow the " +
            "payment provider's policy; write to support@sproutly.app and we will help.",
    ),
    Section(
        "Changes",
        "We will tell you in the app before these terms change in a way that affects you. " +
            "Continuing to use Sproutly after that means you accept the new terms.",
    ),
)

/** Privacy policy and terms, in one place, reachable from sign-in and settings. */
@Composable
fun LegalScreen(
    startOnTerms: Boolean = false,
    onBack: () -> Unit,
) {
    var showTerms by remember { mutableStateOf(startOnTerms) }
    val sections = if (showTerms) terms else privacy

    Column(Modifier.fillMaxSize()) {
        SproutlyTopBar(
            title = if (showTerms) "Terms of service" else "Privacy policy",
            onBack = onBack,
        )
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SproutlyChip(text = "Privacy", selected = !showTerms, onClick = { showTerms = false })
                SproutlyChip(text = "Terms", selected = showTerms, onClick = { showTerms = true })
            }
            Spacer(Modifier.height(16.dp))

            Surface(
                shape = SproutlyRadii.card,
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TablerIcon(
                        TablerIcons.Verified,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "Written to be read, not to hide things. Last updated November 2026.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            sections.forEach { section ->
                Spacer(Modifier.height(20.dp))
                Text(section.heading, style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(4.dp))
                Text(
                    section.body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}
