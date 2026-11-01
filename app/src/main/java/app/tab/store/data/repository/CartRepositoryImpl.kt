package app.tab.store.data.repository

import app.tab.store.core.firebase.firestoreResult
import app.tab.store.core.firebase.orFallback
import app.tab.store.core.firebase.snapshotFlow
import app.tab.store.core.firebase.toUserMessage
import app.tab.store.data.remote.FirestorePaths
import app.tab.store.domain.model.Cart
import app.tab.store.domain.model.CartLine
import app.tab.store.domain.model.Product
import app.tab.store.domain.model.ProductStatus
import app.tab.store.domain.repository.CartRepository
import app.tab.store.domain.repository.DataResult
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
class CartRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
) : CartRepository {

    private val uid: String? get() = auth.currentUser?.uid

    private fun lines() = uid?.let { firestore.collection(FirestorePaths.cartLines(it)) }

    override fun observeCart(): Flow<Cart> {
        val ref = lines() ?: return flowOf(Cart())
        return ref.snapshotFlow().map { snap ->
            Cart(
                lines = snap.documents.mapNotNull { doc ->
                    val productId = doc.getString("productId") ?: return@mapNotNull null
                    CartLine(
                        productId = productId,
                        storeId = doc.getString("storeId").orEmpty(),
                        storeName = doc.getString("storeName").orEmpty(),
                        title = doc.getString("title").orEmpty(),
                        imageUrl = doc.getString("imageUrl"),
                        unitPrice = (doc.get("unitPrice") as? Number)?.toLong() ?: 0L,
                        quantity = (doc.get("quantity") as? Number)?.toInt() ?: 0,
                        stock = (doc.get("stock") as? Number)?.toInt() ?: 0,
                        status = ProductStatus.from(doc.getString("status")),
                    )
                }.sortedBy { it.title },
            )
        }.orFallback(Cart())
    }

    override suspend fun add(product: Product, quantity: Int): DataResult<Unit> {
        val uid = uid ?: return DataResult.Failure("Sign in to start a cart.")
        if (!product.isPurchasable) return DataResult.Failure("This item is out of stock.")
        return firestoreResult {
            val ref = firestore.collection(FirestorePaths.cartLines(uid)).document(product.id)
            val existing = ref.get().await()
            val current = (existing.get("quantity") as? Number)?.toInt() ?: 0
            val next = (current + quantity).coerceAtMost(product.stock)
            ref.set(
                mapOf(
                    "productId" to product.id,
                    "storeId" to product.storeId,
                    "storeName" to product.storeName,
                    "title" to product.title,
                    "imageUrl" to product.primaryImage,
                    "unitPrice" to product.price,
                    "quantity" to next,
                    "stock" to product.stock,
                    "status" to product.status.name,
                    "addedAt" to System.currentTimeMillis(),
                ),
            ).await()
        }
    }

    override suspend fun setQuantity(productId: String, quantity: Int): DataResult<Unit> {
        val uid = uid ?: return DataResult.Failure("Sign in to use your cart.")
        return firestoreResult {
            val ref = firestore.collection(FirestorePaths.cartLines(uid)).document(productId)
            if (quantity <= 0) {
                ref.delete().await()
            } else {
                val snap = ref.get().await()
                val stock = (snap.get("stock") as? Number)?.toInt() ?: quantity
                ref.update("quantity", quantity.coerceAtMost(stock)).await()
            }
        }
    }

    override suspend fun remove(productId: String): DataResult<Unit> {
        val uid = uid ?: return DataResult.Failure("Sign in to use your cart.")
        return firestoreResult {
            firestore.collection(FirestorePaths.cartLines(uid)).document(productId).delete().await()
        }
    }

    override suspend fun clear(): DataResult<Unit> {
        val uid = uid ?: return DataResult.Failure("Sign in to use your cart.")
        return firestoreResult {
            val snap = firestore.collection(FirestorePaths.cartLines(uid)).get().await()
            snap.documents.forEach { it.reference.delete() }
        }
    }

    override fun itemCount(): Flow<Int> = observeCart().map { it.itemCount }
}
