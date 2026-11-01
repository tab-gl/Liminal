package app.tab.store.data.repository

import app.tab.store.core.firebase.firestoreResult
import app.tab.store.core.firebase.snapshotFlow
import app.tab.store.data.remote.FirestorePaths
import app.tab.store.data.remote.toProduct
import app.tab.store.domain.model.Product
import app.tab.store.domain.repository.DataResult
import app.tab.store.domain.repository.WishlistRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

@Singleton
class WishlistRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
) : WishlistRepository {

    private val uid: String? get() = auth.currentUser?.uid

    private fun items() = uid?.let { firestore.collection(FirestorePaths.wishlistItems(it)) }

    /**
     * Wishlist rows keep a lightweight product snapshot so the wishlist screen
     * renders from a single query instead of N document reads.
     */
    override fun observeWishlist(): Flow<List<Product>> {
        val ref = items() ?: return flowOf(emptyList())
        return ref.snapshotFlow().map { snap ->
            snap.documents.mapNotNull { doc ->
                val productId = doc.getString("productId") ?: return@mapNotNull null
                Product(
                    id = productId,
                    storeId = doc.getString("storeId").orEmpty(),
                    storeName = doc.getString("storeName").orEmpty(),
                    title = doc.getString("title").orEmpty(),
                    price = (doc.get("price") as? Number)?.toLong() ?: 0L,
                    compareAtPrice = (doc.get("compareAtPrice") as? Number)?.toLong(),
                    stock = (doc.get("stock") as? Number)?.toInt() ?: 0,
                    status = app.tab.store.domain.model.ProductStatus.from(doc.getString("status")),
                    images = listOfNotNull(
                        doc.getString("imageUrl")?.let {
                            app.tab.store.domain.model.ProductImage(it, null)
                        },
                    ),
                )
            }.sortedByDescending { it.title }
        }
    }

    override fun observeWishlistIds(): Flow<Set<String>> {
        val ref = items() ?: return flowOf(emptySet())
        return ref.snapshotFlow().map { snap -> snap.documents.mapNotNull { it.id }.toSet() }
    }

    override suspend fun toggle(product: Product): DataResult<Boolean> {
        val uid = uid ?: return DataResult.Failure("Sign in to save items.")
        return try {
            val ref = firestore.collection(FirestorePaths.wishlistItems(uid)).document(product.id)
            val existing = ref.get().await()
            if (existing.exists()) {
                ref.delete().await()
                DataResult.Success(false)
            } else {
                ref.set(
                    mapOf(
                        "productId" to product.id,
                        "storeId" to product.storeId,
                        "storeName" to product.storeName,
                        "title" to product.title,
                        "imageUrl" to product.primaryImage,
                        "price" to product.price,
                        "compareAtPrice" to product.compareAtPrice,
                        "stock" to product.stock,
                        "status" to product.status.name,
                        "addedAt" to System.currentTimeMillis(),
                    ),
                ).await()
                DataResult.Success(true)
            }
        } catch (t: Throwable) {
            DataResult.Failure(t.message ?: "Could not update your wishlist.", t)
        }
    }

    override suspend fun remove(productId: String): DataResult<Unit> {
        val uid = uid ?: return DataResult.Failure("Sign in to save items.")
        return firestoreResult {
            firestore.collection(FirestorePaths.wishlistItems(uid)).document(productId).delete().await()
        }
    }
}
