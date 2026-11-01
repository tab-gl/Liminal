package app.tab.store.core.utils

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

/**
 * One place for background failures the user should know about.
 *
 * Data streams degrade to an empty state rather than crashing, which is right —
 * but silently showing "no products" when the real cause is an undeployed index
 * is not. Repositories report here and the shell shows a snackbar with the
 * actionable message from [toDataMessage].
 */
object AppErrors {

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val messages: SharedFlow<String> = _messages

    private var lastMessage: String? = null
    private var lastAt: Long = 0L

    @Synchronized
    fun report(throwable: Throwable) {
        val message = throwable.toDataMessage()
        val now = System.currentTimeMillis()
        // Collapse a burst from several listeners failing for the same reason.
        if (message == lastMessage && now - lastAt < 15_000) return
        lastMessage = message
        lastAt = now
        _messages.tryEmit(message)
    }

    fun report(message: String) {
        val now = System.currentTimeMillis()
        if (message == lastMessage && now - lastAt < 15_000) return
        lastMessage = message
        lastAt = now
        _messages.tryEmit(message)
    }
}
