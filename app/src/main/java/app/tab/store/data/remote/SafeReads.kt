package app.tab.store.data.remote

import com.google.firebase.firestore.DocumentSnapshot

/**
 * Type-tolerant field reads.
 *
 * Firestore's typed accessors (getString, getLong, getBoolean) throw when the
 * field holds a different type, and a document that mixes a server timestamp with
 * a number is enough to crash a snapshot listener. get() returns whatever is
 * stored and a failed cast is a null, so a mismatch becomes a default instead of
 * a crash.
 */
internal fun DocumentSnapshot.stringOr(key: String, fallback: String = ""): String =
    (get(key) as? String) ?: fallback

internal fun DocumentSnapshot.numberOr(key: String, fallback: Long = 0L): Long {
    val value = get(key) ?: return fallback
    return when (value) {
        is Number -> value.toLong()
        is com.google.firebase.Timestamp -> value.toDate().time
        else -> fallback
    }
}

internal fun DocumentSnapshot.booleanOr(key: String, fallback: Boolean = false): Boolean =
    (get(key) as? Boolean) ?: fallback
