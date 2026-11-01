package app.tab.store.core.firebase

import android.util.Log
import app.tab.store.core.utils.AppErrors
import app.tab.store.core.utils.toDataMessage
import app.tab.store.domain.repository.DataResult
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.tasks.await

/** Converts a Firestore query into a cold [Flow] that emits on every server change. */
fun Query.snapshotFlow(): Flow<QuerySnapshot> = callbackFlow {
    val registration = addSnapshotListener { snapshot, error ->
        if (error != null) {
            close(error)
            return@addSnapshotListener
        }
        if (snapshot != null) trySend(snapshot)
    }
    awaitClose { registration.remove() }
}

/** Same as the query overload, for a single document. */
fun DocumentReference.snapshotFlow(): Flow<DocumentSnapshot> = callbackFlow {
    val registration = addSnapshotListener { snapshot, error ->
        if (error != null) {
            close(error)
            return@addSnapshotListener
        }
        if (snapshot != null) trySend(snapshot)
    }
    awaitClose { registration.remove() }
}

/**
 * Keeps a Firestore stream alive when the listener fails.
 *
 * A snapshot listener can error for reasons the user cannot control — a missing
 * composite index while it builds, a rules denial, going offline mid-listen.
 * Previously that error was rethrown into the collecting ViewModel, where an
 * uncaught exception killed the app (the crash on the sign-in → home handoff).
 * Now the stream degrades to [fallback] and the screen shows its empty state.
 */
fun <T> Flow<T>.orFallback(fallback: T): Flow<T> = catch { throwable ->
    Log.w("SproutlyFirestore", "stream failed, falling back to empty state", throwable)
    AppErrors.report(throwable)
    emit(fallback)
}

fun DocumentSnapshot.referencePath(): String = reference.path

/**
 * Runs a Firestore write and normalises every failure mode (offline, permission,
 * missing index) into a [DataResult.Failure] with a message a human can read.
 */
suspend fun firestoreResult(block: suspend () -> Unit): DataResult<Unit> = try {
    block()
    DataResult.Success(Unit)
} catch (t: Throwable) {
    DataResult.Failure(t.toUserMessage(), t)
}

suspend fun <T> firestoreResultOf(block: suspend () -> T): DataResult<T> = try {
    DataResult.Success(block())
} catch (t: Throwable) {
    DataResult.Failure(t.toUserMessage(), t)
}

fun Throwable.toUserMessage(): String = toDataMessage()
