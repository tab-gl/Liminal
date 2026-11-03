package app.tab.store.core.account

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Carries the address of the account someone tried to switch to across the
 * sign-out that a password-protected account requires.
 *
 * Firebase cannot move a session to a password account without that password,
 * so the honest flow is: sign out, then land on sign-in with the address already
 * filled in. Without this the form came up empty and it looked like the app had
 * simply thrown the user out.
 */
@Singleton
class PendingAccountSwitch @Inject constructor() {
    @Volatile
    var email: String? = null
        private set

    fun remember(email: String) {
        this.email = email
    }

    /** Reads once and clears, so a later cold start does not prefill stale data. */
    fun take(): String? {
        val value = email
        email = null
        return value
    }
}
