package app.tab.store

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.lifecycleScope
import app.tab.store.core.designsystem.SproutlyTheme
import app.tab.store.core.navigation.SproutlyNavHost
import app.tab.store.core.notifications.DeviceTokenRegistrar
import app.tab.store.feature.auth.AuthViewModel
import dagger.hilt.android.AndroidEntryPoint
import androidx.activity.viewModels
import javax.inject.Inject
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val authViewModel: AuthViewModel by viewModels()

    @javax.inject.Inject
    lateinit var themePreference: app.tab.store.core.theme.ThemePreference

    @Inject lateinit var deviceTokenRegistrar: DeviceTokenRegistrar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val state by authViewModel.uiState.collectAsState()
            val themeMode by themePreference.mode.collectAsState()
            SproutlyTheme(mode = themeMode) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    // While the session is being restored there is no user yet.
                    // Rendering the nav host here would flash the sign-in screen
                    // at someone who is already signed in, so we hold on a plain
                    // surface for the few hundred milliseconds it takes.
                    if (state.isRestoringSession) {
                        Box(Modifier.fillMaxSize())
                    } else {
                        SproutlyNavHost(
                            isSignedIn = state.user != null,
                            role = state.user?.role,
                        )
                    }
                }
            }
        }

        lifecycleScope.launch {
            authViewModel.awaitSession()
            deviceTokenRegistrar.syncToken()
        }
    }
}
