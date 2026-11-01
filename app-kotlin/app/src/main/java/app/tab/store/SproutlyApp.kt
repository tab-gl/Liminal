package app.tab.store

import android.app.Application
import com.google.android.gms.ads.MobileAds
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.PersistentCacheSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Application entry point.
 *
 * Startup is deliberately split: Firebase and the offline cache are set up here
 * because every screen depends on them, while AdMob initialises on a background
 * coroutine so it never delays the first frame. There is no splash screen - the
 * launch theme is a flat brand colour.
 */
class SproutlyApp : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()

        FirebaseApp.initializeApp(this)

        // Offline-first: the feed survives a commute, and a cold start paints from
        // cache instead of waiting on the network.
        // Persistent cache only: this Firebase version has no separate
        // memory-cache setter on the builder.
        FirebaseFirestore.getInstance().firestoreSettings =
            FirebaseFirestoreSettings.Builder()
                .setLocalCacheSettings(PersistentCacheSettings.newBuilder().build())
                .build()

        appScope.launch {
            runCatching { MobileAds.initialize(this@SproutlyApp) }
        }
    }
}
