package app.tab.store.core.utils

/** Generic screen state shared by every ViewModel in the app. */
sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Ready<T>(val data: T) : UiState<T>
    data class Empty(val message: String = "Nothing here yet") : UiState<Nothing>
    data class Error(val message: String) : UiState<Nothing>
}

/** One-shot messages (snackbars, navigation triggers) that must not repeat on recomposition. */
data class UiMessage(val id: Long = System.nanoTime(), val text: String)
