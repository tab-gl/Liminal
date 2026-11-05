package app.tab.store.core.utils

import android.content.Context
import androidx.core.content.edit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Keeps the last crash on disk.
 *
 * A crash on a phone with no console attached is invisible: the app closes and
 * nobody learns why. This records the trace of the last one so it can be read
 * from Settings, which turns "it crashes when I switch tabs" into a line number.
 *
 * It does not swallow anything — the default handler still runs, so the app still
 * stops the way it should.
 */
@Singleton
class CrashLogger @Inject constructor(
    private val context: Context,
) {
    private val prefs = context.getSharedPreferences("sproutly_crash", Context.MODE_PRIVATE)

    fun install() {
        val existing = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            runCatching {
                val trace = error.stackTraceToString().take(4000)
                prefs.edit {
                    putString("last", "${error.javaClass.simpleName}: ${error.message}\n\n$trace")
                }
            }
            CrashLogHolder.last = prefs.getString("last", null)
            existing?.uncaughtException(thread, error)
        }
    }

    fun last(): String? = prefs.getString("last", null).also { CrashLogHolder.last = it }

    fun clear() = prefs.edit { remove("last") }
}

/** The most recent crash, readable from a composable without a view model. */
object CrashLogHolder {
    @Volatile
    var last: String? = null
}
