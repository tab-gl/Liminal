package app.tab.store.data.repository

import app.tab.store.core.firebase.snapshotFlow
import app.tab.store.core.firebase.orFallback
import app.tab.store.core.firebase.toUserMessage
import app.tab.store.data.remote.FirestorePaths
import app.tab.store.data.remote.stringOr
import app.tab.store.domain.model.InstalledApp
import app.tab.store.domain.repository.AppLibraryRepository
import app.tab.store.domain.repository.DataResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppLibraryRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
) : AppLibraryRepository {
    private fun library(uid: String) = firestore.collection(FirestorePaths.USERS)
        .document(uid).collection(FirestorePaths.LIBRARY)

    override fun observeMyApps(): Flow<List<InstalledApp>> {
        val uid = auth.currentUser?.uid ?: return kotlinx.coroutines.flow.flowOf(emptyList())
        return library(uid).orderBy("installedAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .snapshotFlow()
            .map { snapshot -> snapshot.documents.map { doc ->
                InstalledApp(
                    productId = doc.id,
                    title = doc.getString("title").orEmpty(),
                    imageUrl = doc.getString("imageUrl"),
                    storeName = doc.getString("storeName").orEmpty(),
                    versionCode = (doc.get("versionCode") as? Number)?.toInt() ?: 0,
                    versionName = doc.getString("versionName").orEmpty(),
                    installedAt = (doc.get("installedAt") as? com.google.firebase.Timestamp)?.toDate()?.time
                        ?: (doc.get("installedAt") as? Number)?.toLong() ?: 0L,
                )
            }}.orFallback(emptyList())
    }

    override suspend fun recordInstall(productId: String): DataResult<Unit> {
        val uid = auth.currentUser?.uid ?: return DataResult.Failure("Sign in to add apps to your library.")
        val productRef = firestore.collection(FirestorePaths.PRODUCTS).document(productId)
        val libraryRef = library(uid).document(productId)
        return try {
            firestore.runTransaction { tx ->
                val product = tx.get(productRef)
                if (!product.exists() || product.getString("status") != "ACTIVE") {
                    error("This app is no longer available.")
                }
                val existing = tx.get(libraryRef)
                val images = product.get("images") as? List<Map<String, Any?>> ?: emptyList()
                val firstImage = images.firstOrNull()?.get("url") as? String
                tx.set(libraryRef, mapOf(
                    "productId" to productId,
                    "title" to product.getString("title").orEmpty(),
                    "imageUrl" to firstImage,
                    "storeName" to product.getString("storeName").orEmpty(),
                    "versionCode" to ((product.get("versionCode") as? Number)?.toInt() ?: 0),
                    "versionName" to product.getString("versionName").orEmpty(),
                    "installedAt" to FieldValue.serverTimestamp(),
                ), com.google.firebase.firestore.SetOptions.merge())
                if (!existing.exists()) {
                    val count = (product.get("unitsSold") as? Number)?.toLong() ?: 0L
                    tx.update(productRef, mapOf("unitsSold" to count + 1L, "updatedAt" to FieldValue.serverTimestamp()))
                }
            }.await()
            DataResult.Success(Unit)
        } catch (t: Throwable) {
            DataResult.Failure(t.toUserMessage(), t)
        }
    }
}
