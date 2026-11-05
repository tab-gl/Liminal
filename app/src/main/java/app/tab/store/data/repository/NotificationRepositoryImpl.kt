package app.tab.store.data.repository

import app.tab.store.core.firebase.firestoreResult
import app.tab.store.core.firebase.orFallback
import app.tab.store.core.firebase.snapshotFlow
import app.tab.store.core.firebase.toUserMessage
import app.tab.store.data.remote.FirestorePaths
import app.tab.store.data.remote.uidFlow
import app.tab.store.data.remote.toNotification
import app.tab.store.domain.model.AppNotification
import app.tab.store.domain.repository.DataResult
import app.tab.store.domain.repository.NotificationRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

@Singleton
class NotificationRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
) : NotificationRepository {

    private val uid: String? get() = auth.currentUser?.uid

    private fun items(uid: String) = firestore.collection(FirestorePaths.notificationItems(uid))

    override fun observeNotifications(): Flow<List<AppNotification>> =
        auth.uidFlow().flatMapLatest { uid ->
            if (uid == null) return@flatMapLatest flowOf(emptyList())
            items(uid).snapshotFlow().map { snap ->
                snap.documents.map { it.toNotification() }.sortedByDescending { it.createdAt }
            }.orFallback(emptyList())
        }

    override fun observeUnreadCount(): Flow<Int> =
        observeNotifications().map { list -> list.count { !it.read } }

    override suspend fun markRead(id: String): DataResult<Unit> {
        val ref = uid?.let { items(it) } ?: return DataResult.Failure("Sign in to see notifications.")
        return firestoreResult { ref.document(id).update("read", true).await() }
    }

    override suspend fun markAllRead(): DataResult<Unit> {
        val ref = uid?.let { items(it) } ?: return DataResult.Failure("Sign in to see notifications.")
        return firestoreResult {
            ref.get().await().documents.filter { it.getBoolean("read") != true }
                .forEach { it.reference.update("read", true).await() }
        }
    }

    override suspend fun push(
        id: String,
        type: String,
        title: String,
        body: String,
        deepLink: String,
    ): DataResult<Unit> {
        val uid = uid ?: return DataResult.Failure("Sign in to see this.")
        return try {
            firestore.collection(FirestorePaths.NOTIFICATIONS).document(uid)
                .collection("items").document(id)
                .set(
                    mapOf(
                        "type" to type,
                        "title" to title,
                        "body" to body.take(200),
                        "deepLink" to deepLink,
                        "read" to false,
                        "createdAt" to System.currentTimeMillis(),
                    ),
                )
                .await()
            DataResult.Success(Unit)
        } catch (t: Throwable) {
            DataResult.Failure(t.toUserMessage(), t)
        }
    }

    override suspend fun registerDeviceToken(token: String): DataResult<Unit> {
        val uid = uid ?: return DataResult.Failure("Sign in to receive updates.")
        return try {
            firestore.collection(FirestorePaths.deviceTokens(uid)).document(token).set(
                mapOf(
                    "token" to token,
                    "platform" to "android",
                    "updatedAt" to System.currentTimeMillis(),
                ),
                SetOptions.merge(),
            ).await()
            DataResult.Success(Unit)
        } catch (t: Throwable) {
            DataResult.Failure(t.toUserMessage(), t)
        }
    }
}
