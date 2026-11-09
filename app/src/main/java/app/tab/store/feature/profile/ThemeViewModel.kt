package app.tab.store.feature.profile

import androidx.lifecycle.ViewModel
import app.tab.store.core.theme.ThemeMode
import app.tab.store.core.theme.ThemePreference
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/** Thin bridge so composables can read and set the theme. */
@HiltViewModel
class ThemeViewModel @Inject constructor(
    private val preference: ThemePreference,
) : ViewModel() {
    val mode: StateFlow<ThemeMode> = preference.mode
    val accent: StateFlow<app.tab.store.core.theme.AccentOption> = preference.accent

    fun set(mode: ThemeMode) = preference.set(mode)

    fun setAccent(accent: app.tab.store.core.theme.AccentOption) = preference.setAccent(accent)
}
