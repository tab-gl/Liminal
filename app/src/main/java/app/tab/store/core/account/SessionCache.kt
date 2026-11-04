package app.tab.store.core.account

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Remembers that someone is signed in, on disk, so the first frame of the app
 * knows it too.
 *
 * FirebaseAuth.currentUser is not guaranteed to be populated the instant the
 * view model is built. Asking it at that moment gave "no session", the sign-in
 * screen drew, and then the real session arrived — the flash. This flag is
 * written whenever a session starts and cleared when it ends, and it is readable
 * synchronously before Firebase has finished warming up.
 */
@Singleton
class SessionCache @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs = context.getSharedPreferences("sproutly_session", Context.MODE_PRIVATE)

    fun remember(uid: String) = prefs.edit { putString(KEY, uid) }

    fun clear() = prefs.edit { remove(KEY) }

    /** True when the last known state of this install was "signed in". */
    fun hasSession(): Boolean = !prefs.getString(KEY, null).isNullOrBlank()

    private companion object {
        const val KEY = "uid"
    }
}
