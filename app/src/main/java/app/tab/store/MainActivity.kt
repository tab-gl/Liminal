package app.tab.store

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import app.tab.store.core.ui.Surface
import androidx.compose.ui.Modifier
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.lifecycleScope
import app.tab.store.core.designsystem.HomeSkeleton
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
    @Inject lateinit var updateNotifier: app.tab.store.core.updates.UpdateNotifier

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val state by authViewModel.uiState.collectAsState()
            val themeMode by themePreference.mode.collectAsState()
            val accent by themePreference.accent.collectAsState()
            SproutlyTheme(mode = themeMode, accent = accent) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    // While the session is being restored there is no user yet.
                    // Rendering the nav host here would flash the sign-in screen
                    // at someone who is already signed in, so we hold on a plain
                    // surface for the few hundred milliseconds it takes.
                    if (state.isRestoringSession) {
                        // Not a blank screen: the storefront's own skeleton, so
                        // the first second looks like the app opening. The
                        // sign-in screen only appears once we know it is needed.
                        HomeSkeleton()
                    } else {
                        SproutlyNavHost(
                            // A session on the device is enough: the profile can
                            // arrive a moment later without costing a sign-in
                            // screen.
                            isSignedIn = state.user != null || state.hasDeviceSession,
                            role = state.user?.role,
                        )
                    }
                }
            }
        }

        lifecycleScope.launch {
            authViewModel.awaitSession()
            deviceTokenRegistrar.syncToken()
            // Tell people about updates for what they installed, once per launch.
            updateNotifier.check()
        }
    }
}
