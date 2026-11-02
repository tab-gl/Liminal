package app.tab.store

import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory

/**
 * Debug builds use the debug App Check provider so local builds work without
 * Play Integrity. The debug token is printed to logcat and must be registered
 * in Firebase Console → App Check → Manage debug tokens.
 */
object SproutlyAppCheck {
    fun install() {
        FirebaseAppCheck.getInstance()
            .installAppCheckProviderFactory(DebugAppCheckProviderFactory.getInstance())
    }
}
