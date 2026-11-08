package app.tab.store.feature.auth

import app.tab.store.core.ui.Tokens
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import app.tab.store.core.ui.IconButton
import app.tab.store.core.ui.Surface
import app.tab.store.core.ui.Text
import app.tab.store.core.ui.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.tab.store.R
import app.tab.store.core.designsystem.PhosphorIcons
import app.tab.store.core.designsystem.SproutlyIcon
import app.tab.store.core.designsystem.BrandMark
import app.tab.store.core.designsystem.SproutlyButton
import app.tab.store.core.designsystem.SproutlyPalette
import app.tab.store.core.designsystem.SproutlyRadii
import app.tab.store.core.designsystem.SproutlyTextField
import app.tab.store.core.designsystem.SproutlyTheme

@Composable
fun LoginScreen(
    onRegister: () -> Unit,
    onForgotPassword: () -> Unit,
    onLegal: (Boolean) -> Unit = {},
    viewModel: AuthViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val launchGoogle = rememberGoogleSignIn(
        onToken = viewModel::signInWithGoogle,
        onError = { viewModel.showError(it) },
    )

    // No navigation here on success: SproutlyNavHost rebuilds the graph when the
    // session changes, so navigating as well would race the controller being
    // replaced (that race crashed the app right after sign-in).

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Tokens.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .imePadding(),
    ) {
        // Brand hero — the lime panel with the monstera mark.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(SproutlyPalette.Lime)
                .padding(horizontal = 24.dp)
                .padding(top = 20.dp, bottom = 26.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BrandMark(size = 40.dp)
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "Liminal",
                    style = Tokens.typography.headlineSmall,
                    color = SproutlyPalette.OliveDeep,
                )
            }
            Spacer(Modifier.height(22.dp))
            Text(
                text = "Build it.\nPublish it.\nGrow it.",
                style = Tokens.typography.displaySmall,
                color = SproutlyPalette.OliveDeep,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = "An app store for independent developers.",
                style = Tokens.typography.bodyMedium,
                color = SproutlyPalette.Olive,
            )
        }

        Column(Modifier.padding(horizontal = 24.dp, vertical = 22.dp)) {
            Text("Welcome back", style = Tokens.typography.headlineLarge)
            Spacer(Modifier.height(4.dp))
            Text(
                "Sign in to install and publish.",
                style = Tokens.typography.bodyMedium,
                color = Tokens.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(20.dp))

            SproutlyTextField(
                value = state.email,
                onValueChange = viewModel::onEmailChange,
                label = "Email",
                placeholder = "you@example.com",
                keyboardType = KeyboardType.Email,
            )
            SproutlyTextField(
                value = state.password,
                onValueChange = viewModel::onPasswordChange,
                label = "Password",
                isPassword = true,
                passwordVisible = state.passwordVisible,
                keyboardType = KeyboardType.Password,
                trailing = {
                    IconButton(onClick = viewModel::togglePasswordVisibility) {
                        SproutlyIcon(
                            if (state.passwordVisible) PhosphorIcons.EyeOff else PhosphorIcons.Eye,
                            contentDescription = "Toggle password visibility",
                        )
                    }
                },
            )

            if (state.error != null) {
                Surface(
                    color = Tokens.colorScheme.errorContainer,
                    shape = Tokens.shapes.small,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = state.error.orEmpty(),
                        style = Tokens.typography.bodySmall,
                        color = Tokens.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(12.dp),
                    )
                }
                Spacer(Modifier.height(12.dp))
            }

            // Always tappable: validation happens on submit and reports inline,
            // which is friendlier than a permanently disabled primary button.
            SproutlyButton(
                text = "Sign in",
                onClick = viewModel::signIn,
                loading = state.isLoading,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onForgotPassword) {
                    Text("Forgot password?", style = Tokens.typography.labelLarge)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .weight(1f)
                        .height(1.dp)
                        .background(Tokens.colorScheme.outlineVariant),
                )
                Text(
                    "or",
                    style = Tokens.typography.labelMedium,
                    color = Tokens.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 12.dp),
                )
                Box(
                    Modifier
                        .weight(1f)
                        .height(1.dp)
                        .background(Tokens.colorScheme.outlineVariant),
                )
            }
            Spacer(Modifier.height(16.dp))

            Surface(
                onClick = { launchGoogle() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = SproutlyRadii.pill,
                color = Tokens.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp, Tokens.colorScheme.outline,
                ),
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_google_g),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(10.dp))
                    Text("Continue with Google", style = Tokens.typography.labelLarge)
                }
            }

            Spacer(Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "New to Liminal?",
                    style = Tokens.typography.bodyMedium,
                    color = Tokens.colorScheme.onSurfaceVariant,
                )
                TextButton(onClick = onRegister) {
                    Text("Create account", style = Tokens.typography.labelLarge)
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                "By continuing you agree to Liminal's Terms of Service and Privacy Policy. " +
                    "Tap to read either.",
                style = Tokens.typography.bodySmall,
                color = Tokens.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onLegal(false) },
            )
            Spacer(Modifier.height(12.dp))
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun LoginPreview() {
    SproutlyTheme {
        LoginScreen(onRegister = {}, onForgotPassword = {})
    }
}
