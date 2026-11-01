package app.tab.store.core.firebase

import app.tab.store.core.utils.toDataMessage
import app.tab.store.domain.repository.DataResult
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
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
