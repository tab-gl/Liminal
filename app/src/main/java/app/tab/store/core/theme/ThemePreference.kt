package app.tab.store.core.theme

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

enum class ThemeMode(val label: String) {
    SYSTEM("System"),
    LIGHT("Light"),
    DARK("Dark");

    companion object {
        fun from(value: String?): ThemeMode =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: SYSTEM
    }
}

/**
 * The theme choice, persisted and observable.
 *
 * Kept deliberately small: a StateFlow the activity reads, and a setter the
 * settings sheet writes. Changing it recomposes the whole tree, because
 * SproutlyTheme is fed from this flow.
 */
@Singleton
class ThemePreference @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs = context.getSharedPreferences("sproutly_settings", Context.MODE_PRIVATE)

    private val _mode = MutableStateFlow(ThemeMode.from(prefs.getString(KEY, null)))
    val mode: StateFlow<ThemeMode> = _mode.asStateFlow()

    fun set(mode: ThemeMode) {
        _mode.value = mode
        prefs.edit { putString(KEY, mode.name) }
    }

    private companion object {
        const val KEY = "theme_mode"
    }
}
