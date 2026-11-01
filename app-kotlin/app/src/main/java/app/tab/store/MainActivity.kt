package app.tab.store

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import app.tab.store.push.PushManager
import app.tab.store.ui.nav.Routes
import app.tab.store.ui.nav.SproutlyNavHost
import app.tab.store.ui.theme.SproutlyTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        PushManager.createChannel(this)

        // An app link can launch the process directly, so the initial route has
        // to be resolved before the graph is composed.
        val initialRoute = Routes.fromDeepLink(intent?.data)
        setContent {
            SproutlyTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
                ) {
                    SproutlyNavHost(initialRoute = initialRoute)
                }
            }
        }
    }
}
