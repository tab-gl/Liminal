package app.tab.store.data.remote

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * The signed-in uid, as a stream.
 *
 * Repositories used to resolve `users/{uid}/…` once, when a flow was created.
 * After switching accounts the listener kept pointing at the previous user's
 * collection while the session belonged to the new one, so Firestore answered
 * PERMISSION_DENIED. Flows now re-resolve whenever the session changes.
 */
fun FirebaseAuth.uidFlow(): Flow<String?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser?.uid) }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}
