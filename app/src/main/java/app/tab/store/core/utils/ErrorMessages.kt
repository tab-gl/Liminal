package app.tab.store.core.utils

import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.firestore.FirebaseFirestoreException

/**
 * Turns Firebase failures into something a human can act on.
 *
 * The previous version of this file collapsed every unrecognised error into
 * "Something went wrong", which is useless when the real cause is a project
 * setting ("Email/Password sign-in is not enabled") or a rules deployment
 * ("permission-denied"). Each message now names the cause and, where the cause
 * is configuration, says exactly what to change. The raw text is appended as a
 * short suffix so a screenshot is enough to diagnose.
 */
private fun Throwable.rawHint(): String {
    val raw = message?.replace('\n', ' ')?.trim().orEmpty()
    if (raw.isBlank()) return ""
    return " · ${raw.take(140)}"
}

fun Throwable.toAuthMessage(): String {
    val code = (this as? FirebaseAuthException)?.errorCode ?: 0
    return when {
        code == FirebaseAuthException.ERROR_EMAIL_ALREADY_IN_USE ->
            "That email is already registered. Sign in instead."

        code == FirebaseAuthException.ERROR_CREDENTIAL_ALREADY_IN_USE ->
            "That email is already linked to another sign-in method."

        code == FirebaseAuthException.ERROR_INVALID_EMAIL ->
            "That email address does not look right."

        code == FirebaseAuthException.ERROR_WEAK_PASSWORD ->
            "That password is too weak — use at least 8 characters."

        code == FirebaseAuthException.ERROR_WRONG_PASSWORD ||
            code == FirebaseAuthException.ERROR_INVALID_CREDENTIAL ->
            "Email or password is incorrect."

        code == FirebaseAuthException.ERROR_USER_NOT_FOUND ->
            "No account found for that email."

        code == FirebaseAuthException.ERROR_USER_DISABLED ->
            "This account has been disabled."

        code == FirebaseAuthException.ERROR_TOO_MANY_REQUESTS ->
            "Too many attempts. Wait a minute and try again."

        code == FirebaseAuthException.ERROR_NETWORK_REQUEST_FAILED ->
            "Network error. Check your connection and try again."

        code == FirebaseAuthException.ERROR_OPERATION_NOT_ALLOWED ->
            "Email sign-in is switched off for this Firebase project. Enable " +
                "Email/Password in Firebase Console → Authentication → Sign-in method." +
                rawHint()

        code == FirebaseAuthException.ERROR_INVALID_API_KEY ->
            "Firebase rejected this app's API key. Check the key in " +
                "google-services.json and any API restrictions in Google Cloud." + rawHint()

        code == FirebaseAuthException.ERROR_APP_NOT_AUTHORIZED ->
            "This build is not authorised for the Firebase project. Add the " +
                "release SHA-1 fingerprint in Firebase Console → Project settings → Your apps." +
                rawHint()

        code == FirebaseAuthException.ERROR_REQUIRES_RECENT_LOGIN ->
            "Please sign in again to continue."

        message.orEmpty().contains("app check", ignoreCase = true) ->
            "Firebase App Check rejected this build. A sideloaded release APK " +
                "cannot pass Play Integrity — turn App Check enforcement off for now, " +
                "or install the build from Play." + rawHint()

        message.orEmpty().contains("Identity Toolkit", ignoreCase = true) ->
            "The Identity Toolkit API is not enabled for this Google Cloud project." + rawHint()

        message.orEmpty().contains("API key", ignoreCase = true) ->
            "Firebase rejected the API key. Check google-services.json and API restrictions." +
                rawHint()

        else -> "Sign-in failed." + rawHint()
    }
}

fun Throwable.toDataMessage(): String {
    val code = (this as? FirebaseFirestoreException)?.code
    return when {
        code == FirebaseFirestoreException.Code.PERMISSION_DENIED ->
            "Firestore denied that write. Deploy the rules " +
                "(firebase deploy --only firestore:rules) or check the user's role." + rawHint()

        code == FirebaseFirestoreException.Code.UNAUTHENTICATED ->
            "Your session expired. Please sign in again." + rawHint()

        code == FirebaseFirestoreException.Code.FAILED_PRECONDITION ->
            "Firestore needs an index for this query, or the document state blocks it." + rawHint()

        code == FirebaseFirestoreException.Code.UNAVAILABLE ->
            "You appear to be offline. Check your connection." + rawHint()

        code == FirebaseFirestoreException.Code.RESOURCE_EXHAUSTED ->
            "Too many requests just now. Try again shortly." + rawHint()

        code == FirebaseFirestoreException.Code.NOT_FOUND ->
            "That item no longer exists." + rawHint()

        code == FirebaseFirestoreException.Code.DEADLINE_EXCEEDED ->
            "The request timed out. Try again." + rawHint()

        message.orEmpty().contains("PERMISSION_DENIED", ignoreCase = true) ->
            "Firestore denied that write. Deploy the security rules." + rawHint()

        message.orEmpty().contains("app check", ignoreCase = true) ->
            "Firebase App Check rejected this build — see the App Check settings " +
                "for the project." + rawHint()

        else -> "Request failed." + rawHint()
    }
}
