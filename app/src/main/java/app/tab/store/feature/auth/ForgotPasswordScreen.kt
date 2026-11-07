package app.tab.store.feature.auth

import app.tab.store.core.ui.Tokens
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import app.tab.store.core.ui.Surface
import app.tab.store.core.ui.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.tab.store.core.designsystem.SproutlyButton
import app.tab.store.core.designsystem.SproutlyTextField
import app.tab.store.core.designsystem.SproutlyTopBar

@Composable
fun ForgotPasswordScreen(
    onBack: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(),
    ) {
        SproutlyTopBar(title = "Reset password", onBack = onBack)
        Column(Modifier.padding(horizontal = 24.dp)) {
            Text(
                "We will email you a reset link.",
                style = Tokens.typography.bodyMedium,
                color = Tokens.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(22.dp))
            SproutlyTextField(
                value = state.email,
                onValueChange = viewModel::onEmailChange,
                label = "Email",
                keyboardType = KeyboardType.Email,
            )
            if (state.info != null) {
                Surface(
                    color = Tokens.colorScheme.primaryContainer,
                    shape = Tokens.shapes.small,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        state.info.orEmpty(),
                        style = Tokens.typography.bodySmall,
                        color = Tokens.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(12.dp),
                    )
                }
                Spacer(Modifier.height(12.dp))
            }
            if (state.error != null) {
                Surface(
                    color = Tokens.colorScheme.errorContainer,
                    shape = Tokens.shapes.small,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        state.error.orEmpty(),
                        style = Tokens.typography.bodySmall,
                        color = Tokens.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(12.dp),
                    )
                }
                Spacer(Modifier.height(12.dp))
            }
            SproutlyButton(
                text = "Send reset link",
                onClick = viewModel::sendPasswordReset,
                loading = state.isLoading,
            )
        }
    }
}
