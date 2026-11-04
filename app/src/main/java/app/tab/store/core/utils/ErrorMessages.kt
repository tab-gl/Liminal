package app.tab.store.core.utils

/**
 * Turns Firebase failures into something a human can act on.
 *
 * The earlier version collapsed every unrecognised error into "Something went
 * wrong", which is useless when the real cause is a project setting (Email/
 * Password sign-in switched off) or an undeployed rule. Each message now names
 * the cause and the console fix, and appends a short tail of the raw Firebase
 * text so a screenshot is enough to diagnose.
 *
 * Matching is done on the message text rather than on
 * `FirebaseAuthException.ERROR_*` constants: firebase-auth 23 no longer exposes
 * them, and the message strings have been stable across versions.
 */

private fun Throwable.rawText(): String =
    message?.replace('\n', ' ')?.replace(Regex("\\s+"), " ")?.trim().orEmpty()

private fun String.hint(): String = if (isBlank()) "" else " · ${take(120)}"

fun Throwable.toAuthMessage(): String {
    val raw = rawText()
    val text = raw.lowercase()
    val base = when {
        text.contains("already in use") ->
            "That email is already registered. Sign in instead."

        text.contains("badly formatted") || text.contains("invalid email") ->
            "That email address does not look right."

        text.contains("no user record") ->
            "No account found for that email."

        text.contains("password is invalid") || text.contains("incorrect password") ||
            text.contains("invalid credential") ->
            "Email or password is incorrect."

        text.contains("at least 6 characters") || text.contains("weak password") ||
            text.contains("password should be") ->
            "That password is too weak — use at least 8 characters."

        text.contains("operation_not_allowed") || text.contains("operation is not allowed") ->
            "Email sign-in is switched off for this Firebase project. Enable " +
                "Email/Password in Firebase Console → Authentication → Sign-in method."

        text.contains("identity toolkit") ->
            "The Identity Toolkit API is not enabled for this Google Cloud project. " +
                "Enable it in Google Cloud Console → APIs."

        text.contains("api key") ->
            "Firebase rejected this app's API key. Check google-services.json and any " +
                "key restrictions in Google Cloud Console."

        text.contains("app check") ->
            "Firebase App Check rejected this build. A sideloaded release APK cannot " +
                "pass Play Integrity — switch App Check enforcement off for now."

        text.contains("network error") || text.contains("timeout") ||
            text.contains("unreachable") ->
            "Network error. Check your connection and try again."

        text.contains("too many requests") || text.contains("blocked all requests") ->
            "Too many attempts. Wait a minute and try again."

        text.contains("disabled") && text.contains("user") ->
            "This account has been disabled."

        text.contains("requires-recent-login") || text.contains("recent login") ->
            "Please sign in again to continue."

        else -> "Sign-in failed."
    }
    return base + raw.hint()
}

fun Throwable.toDataMessage(): String {
    val raw = rawText()
    val text = raw.lowercase()
    val base = when {
        text.contains("permission_denied") || text.contains("insufficient permissions") ->
            // The three things that actually cause this, most likely first.
            "Firestore refused that write. Either your developer profile is not " +
                "approved yet, or the rules on the project are older than this build " +
                "(firebase deploy --only firestore:rules)."

        text.contains("unauthenticated") ->
            "Your session expired. Please sign in again."

        text.contains("failed_precondition") || text.contains("index") ->
            "Firestore needs an index for this query, or the document state blocks it. " +
                "Deploy the indexes: firebase deploy --only firestore:indexes"

        text.contains("unavailable") || text.contains("offline") || text.contains("network") ->
            "You appear to be offline. Check your connection."

        text.contains("resource_exhausted") || text.contains("quota") ->
            "Too many requests just now. Try again shortly."

        text.contains("not_found") || text.contains("no document to update") ->
            "That item no longer exists."

        text.contains("deadline") ->
            "The request timed out. Try again."

        text.contains("app check") ->
            "Firebase App Check rejected this build. Switch App Check enforcement off " +
                "for now, or install the build from Play."

        else -> "Request failed."
    }
    return base + raw.hint()
}
