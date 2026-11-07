package app.tab.store.data.repository

import app.tab.store.core.firebase.orFallback
import app.tab.store.core.firebase.snapshotFlow
import app.tab.store.core.firebase.toUserMessage
import app.tab.store.data.remote.FirestorePaths
import app.tab.store.data.remote.booleanOr
import app.tab.store.data.remote.numberOr
import app.tab.store.data.remote.stringOr
import app.tab.store.data.remote.uidFlow
import app.tab.store.data.remote.toOrder
import app.tab.store.domain.model.Order
import app.tab.store.domain.model.OrderStatus
import app.tab.store.domain.repository.DataResult
import app.tab.store.domain.repository.OrderRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

/**
 * Orders run on Firestore transactions rather than Cloud Functions so the
 * marketplace works on the Firebase Spark plan.
 *
 * A Firestore transaction is still server-side and atomic — the SDK retries it
 * against current data and the commit is rejected if anything changed. What
 * moves to the client is *deciding* the write, so `firestore.rules` re-checks
 * every number that matters: the buyer owns the order, each line's price equals
 * the product's current price, stock covers the quantity, and the status
 * machine only moves forward. See firestore.rules → `match /orders`.
 */
@Singleton
class OrderRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
) : OrderRepository {

    private val orders get() = firestore.collection(FirestorePaths.ORDERS)
    private val uid: String? get() = auth.currentUser?.uid

    override fun observeBuyerOrders(): Flow<List<Order>> =
        auth.uidFlow().flatMapLatest { uid ->
            if (uid == null) return@flatMapLatest flowOf(emptyList())
            orders.whereEqualTo("buyerId", uid)
                .orderBy("placedAt", Query.Direction.DESCENDING)
                .snapshotFlow()
                .map { snap -> snap.documents.map { it.toOrder() } }.orFallback(emptyList())
        }

    override fun observeOrder(orderId: String): Flow<Order?> =
        orders.document(orderId).snapshotFlow()
            .map { snap -> if (snap.exists()) snap.toOrder() else null }.orFallback(null)

    override suspend fun cancelOrder(orderId: String): DataResult<Unit> {
        val uid = uid ?: return DataResult.Failure("Sign in to cancel an order.")
        val ref = orders.document(orderId)
        return try {
            firestore.runTransaction { tx ->
                val snap = tx.get(ref)
                if (!snap.exists()) error("That order no longer exists.")
                val status = snap.stringOr("status")
                if (status != OrderStatus.PENDING.name && status != OrderStatus.CONFIRMED.name) {
                    error("This order can no longer be cancelled.")
                }

                @Suppress("UNCHECKED_CAST")
                val timeline = snap.get("timeline") as? List<Map<String, Any?>> ?: emptyList()
                tx.update(
                    ref,
                    mapOf(
                        "status" to OrderStatus.CANCELLED.name,
                        "cancelledBy" to "BUYER",
                        "updatedAt" to FieldValue.serverTimestamp(),
                        "timeline" to timeline + mapOf(
                            "status" to OrderStatus.CANCELLED.name,
                            "at" to System.currentTimeMillis(),
                            "note" to "Cancelled by shopper",
                        ),
                    ),
                )
            }.await()
            DataResult.Success(Unit)
        } catch (t: Throwable) {
            DataResult.Failure(t.toUserMessage(), t)
        }
    }

    override fun observeSellerOrders(storeId: String): Flow<List<Order>> =
        orders.whereArrayContains("storeIds", storeId)
            .orderBy("placedAt", Query.Direction.DESCENDING)
            .snapshotFlow()
            .map { snap -> snap.documents.map { it.toOrder() } }.orFallback(emptyList())

    override suspend fun updateOrderStatus(orderId: String, status: OrderStatus): DataResult<Unit> {
        val ref = orders.document(orderId)
        return try {
            firestore.runTransaction { tx ->
                val snap = tx.get(ref)
                if (!snap.exists()) error("That order no longer exists.")
                @Suppress("UNCHECKED_CAST")
                val timeline = snap.get("timeline") as? List<Map<String, Any?>> ?: emptyList()
                tx.update(
                    ref,
                    mapOf(
                        "status" to status.name,
                        "updatedAt" to FieldValue.serverTimestamp(),
                        "timeline" to timeline + mapOf(
                            "status" to status.name,
                            "at" to System.currentTimeMillis(),
                            "note" to null,
                        ),
                    ),
                )
            }.await()
            DataResult.Success(Unit)
        } catch (t: Throwable) {
            DataResult.Failure(t.toUserMessage(), t)
        }
    }

    override fun observeAllOrders(): Flow<List<Order>> =
        orders.orderBy("placedAt", Query.Direction.DESCENDING)
            .limit(200)
            .snapshotFlow()
            .map { snap -> snap.documents.map { it.toOrder() } }.orFallback(emptyList())

}
