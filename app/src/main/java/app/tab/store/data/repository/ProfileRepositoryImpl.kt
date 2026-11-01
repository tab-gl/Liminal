package app.tab.store.data.repository

import app.tab.store.core.datastore.SessionStore
import app.tab.store.core.firebase.firestoreResult
import app.tab.store.core.network.FunctionsClient
import app.tab.store.data.remote.FirestorePaths
import app.tab.store.data.remote.toUser
import app.tab.store.domain.model.User
import app.tab.store.domain.repository.DataResult
import app.tab.store.domain.repository.ProfileRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

@Singleton
class ProfileRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val sessionStore: SessionStore,
    private val functionsClient: FunctionsClient,
) : ProfileRepository {

    override fun observeProfile(): Flow<User?> = sessionStore.cachedUser

    override suspend fun updateProfile(
        displayName: String,
        phone: String?,
        photoUrl: String?,
        photoPublicId: String?,
    ): DataResult<Unit> {
        val uid = auth.currentUser?.uid ?: return DataResult.Failure("You are signed out.")
        val result = firestoreResult {
            val updates = mutableMapOf<String, Any?>(
                "displayName" to displayName.trim(),
                "phone" to phone?.trim()?.takeIf { it.isNotBlank() },
                "updatedAt" to FieldValue.serverTimestamp(),
            )
            if (photoUrl != null) updates["photoUrl"] = photoUrl
            if (photoPublicId != null) updates["photoPublicId"] = photoPublicId
            firestore.collection(FirestorePaths.USERS).document(uid)
                .set(updates, SetOptions.merge()).await()
            auth.currentUser?.updateProfile(
                UserProfileChangeRequest.Builder().setDisplayName(displayName.trim()).build(),
            )?.await()
        }
        if (result is DataResult.Success) {
            // Refresh the local mirror so headers and avatars update immediately.
            val snap = firestore.collection(FirestorePaths.USERS).document(uid).get().await()
            if (snap.exists()) sessionStore.save(snap.toUser())
        }
        return result
    }

    override suspend fun registerAsSeller(storeName: String): DataResult<String> =
        functionsClient.callString(
            "registerSeller",
            mapOf("storeName" to storeName.trim()),
            key = "storeId",
        )
}
