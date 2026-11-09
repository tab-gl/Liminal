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
 * The accent choice.
 *
 * The app is monochrome by design; the accent is the one colour, and it is the
 * person's to pick. Monet is the default because it follows the wallpaper on
 * Android 12+, which is what people already expect a well-behaved app to do.
 */
enum class AccentOption(val label: String, val hex: Long) {
    MONET("Match wallpaper", 0L),
    INDIGO("Indigo", 0xFF4F5BD5),
    TEAL("Teal", 0xFF0F766E),
    AMBER("Amber", 0xFFB45309),
    ROSE("Rose", 0xFFBE123C),
    VIOLET("Violet", 0xFF7C3AED);

    companion object {
        fun from(value: String?): AccentOption =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: MONET
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

    private val _accent = MutableStateFlow(AccentOption.from(prefs.getString(ACCENT_KEY, null)))
    val accent: StateFlow<AccentOption> = _accent.asStateFlow()

    fun set(mode: ThemeMode) {
        _mode.value = mode
        prefs.edit { putString(KEY, mode.name) }
    }

    fun setAccent(accent: AccentOption) {
        _accent.value = accent
        prefs.edit { putString(ACCENT_KEY, accent.name) }
    }

    private companion object {
        const val KEY = "theme_mode"
        const val ACCENT_KEY = "accent"
    }
}
