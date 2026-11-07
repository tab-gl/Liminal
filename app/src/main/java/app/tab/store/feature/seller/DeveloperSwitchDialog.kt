package app.tab.store.feature.seller

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import app.tab.store.core.ui.AlertDialog
import app.tab.store.core.ui.Button
import app.tab.store.core.ui.ButtonDefaults
import app.tab.store.core.ui.Text
import app.tab.store.core.ui.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.tab.store.core.designsystem.SproutlyIcon
import app.tab.store.core.designsystem.PhosphorIcons
import app.tab.store.core.designsystem.SproutlyRadii

/**
 * Asked before anything is published.
 *
 * Only developers (and admins) can publish, so a customer who taps "Publish an
 * app" is offered the switch first — with what changes and what does not. The
 * confirmation runs the same onboarding the Developer Studio uses, so there is
 * one path to becoming a developer, not two.
 */
@Composable
fun DeveloperSwitchDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = SproutlyRadii.hero,
        containerColor = Tokens.colorScheme.surface,
        icon = {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(Tokens.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                SproutlyIcon(
                    icon = PhosphorIcons.Sparkles,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = Tokens.colorScheme.onPrimaryContainer,
                )
            }
        },
        title = {
            Text(
                text = "Switch to a developer account?",
                style = Tokens.typography.headlineSmall,
            )
        },
        text = {
            Text(
                text = "Publishing needs a developer account. " +
                    "you already have — you also get Developer Studio to publish apps, " +
                    "manage releases and track installs.",
                style = Tokens.typography.bodyMedium,
                color = Tokens.colorScheme.onSurfaceVariant,
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Tokens.colorScheme.primary,
                    contentColor = Tokens.colorScheme.onPrimary,
                ),
            ) {
                Text("Switch to developer", modifier = Modifier.padding(horizontal = 6.dp))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Not now") }
        },
    )
}
