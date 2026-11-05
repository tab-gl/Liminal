package app.tab.store.core.account

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Remembers that the two opening screens have been seen. */
@Singleton
class FirstRunStore @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs = context.getSharedPreferences("sproutly_first_run", Context.MODE_PRIVATE)

    fun isDone(): Boolean = prefs.getBoolean(KEY, false)

    fun markDone() = prefs.edit { putBoolean(KEY, true) }

    /** The tab someone was last on, so opening the app returns them there. */
    fun lastTab(): String? = prefs.getString(TAB_KEY, null)

    fun rememberTab(route: String) = prefs.edit { putString(TAB_KEY, route) }

    private companion object {
        const val KEY = "onboarded"
        const val TAB_KEY = "last_tab"
    }
}
