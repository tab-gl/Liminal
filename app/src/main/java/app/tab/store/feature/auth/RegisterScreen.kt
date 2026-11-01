package app.tab.store.feature.auth

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.tab.store.core.designsystem.SproutlyButton
import app.tab.store.core.designsystem.SproutlyTextField
import app.tab.store.core.designsystem.SproutlyTopBar
import app.tab.store.core.utils.Validators

@Composable
fun RegisterScreen(
    onBack: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // The nav graph rebuilds when the session changes — see LoginScreen.

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding(),
    ) {
        SproutlyTopBar(title = "Create your account", onBack = onBack)
        Column(Modifier.padding(horizontal = 24.dp)) {
            Text(
                "One account to publish apps — and install everyone else's.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(22.dp))

            SproutlyTextField(
                value = state.name,
                onValueChange = viewModel::onNameChange,
                label = "Full name",
                placeholder = "Yash Gupta",
            )
            SproutlyTextField(
                value = state.email,
                onValueChange = viewModel::onEmailChange,
                label = "Email",
                placeholder = "you@example.com",
                keyboardType = KeyboardType.Email,
                isError = state.email.isNotEmpty() && !Validators.isValidEmail(state.email),
                errorText = "That email does not look right.",
            )
            SproutlyTextField(
                value = state.password,
                onValueChange = viewModel::onPasswordChange,
                label = "Password",
                isPassword = true,
                passwordVisible = state.passwordVisible,
                keyboardType = KeyboardType.Password,
                helperText = "At least 8 characters.",
                isError = state.password.isNotEmpty() && !Validators.isValidPassword(state.password),
                errorText = "Use at least 8 characters.",
                trailing = {
                    IconButton(onClick = viewModel::togglePasswordVisibility) {
                        Icon(
                            if (state.passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = "Toggle password visibility",
                        )
                    }
                },
            )

            if (state.password.isNotEmpty()) {
                val strength = Validators.passwordStrength(state.password)
                LinearProgressIndicator(
                    progress = { strength / 4f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = when (strength) {
                        0, 1 -> "Weak — add length and a number."
                        2 -> "Getting there — mix in a capital or symbol."
                        3 -> "Good password."
                        else -> "Strong password."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
            }

            if (state.error != null) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        state.error.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(12.dp),
                    )
                }
                Spacer(Modifier.height(12.dp))
            }

            SproutlyButton(
                text = "Create account",
                onClick = viewModel::register,
                enabled = state.canSubmitRegister,
                loading = state.isLoading,
            )
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Want to publish? You can open a developer profile right after signing up.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}
