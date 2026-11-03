package app.tab.store.data.repository

import app.tab.store.core.firebase.orFallback
import app.tab.store.core.firebase.snapshotFlow
import app.tab.store.core.firebase.toUserMessage
import app.tab.store.data.remote.FirestorePaths
import app.tab.store.data.remote.uidFlow
import app.tab.store.data.remote.toOrder
import app.tab.store.domain.model.Order
import app.tab.store.domain.model.OrderStatus
import app.tab.store.domain.repository.DataResult
import app.tab.store.domain.repository.OrderRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
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

    /**
     * Installs an app: one record, no cart, no address, no shipping.
     *
     * A digital install has nothing to fulfil, so the record is written as
     * already installed. Paid apps will need the payment step in front of this
     * once Razorpay is wired; today everything is a free install.
     */
    override suspend fun install(productId: String): DataResult<String> {
        val uid = uid ?: return DataResult.Failure("Sign in to install apps.")
        val productRef = firestore.collection(FirestorePaths.PRODUCTS).document(productId)
        val orderRef = orders.document()
        var item: Map<String, Any?> = emptyMap()
        return try {
            val orderId = firestore.runTransaction { tx ->
                val snap = tx.get(productRef)
                if (!snap.exists()) error("That app is no longer available.")
                if (snap.getString("status") != "ACTIVE") error("That app is not published.")

                @Suppress("UNCHECKED_CAST")
                val images = snap.get("images") as? List<Map<String, Any?>>
                val title = snap.getString("title").orEmpty()
                val price = (snap.get("price") as? Number)?.toLong() ?: 0L
                val storeId = snap.getString("storeId").orEmpty()
                val now = System.currentTimeMillis()

                item = mapOf(
                    "productId" to snap.id,
                    "title" to title,
                    "imageUrl" to images?.firstOrNull()?.get("url"),
                    "unitPrice" to price,
                    "quantity" to 1,
                    "storeId" to storeId,
                    "storeName" to snap.getString("storeName").orEmpty(),
                )

                tx.set(
                    orderRef,
                    mapOf(
                        "buyerId" to uid,
                        "buyerName" to (auth.currentUser?.displayName
                            ?: auth.currentUser?.email?.substringBefore("@").orEmpty()),
                        "items" to listOf(item),
                        "subtotal" to price,
                        "shipping" to 0L,
                        "total" to price,
                        "currency" to "INR",
                        "status" to OrderStatus.DELIVERED.name,
                        "note" to "",
                        "storeIds" to listOf(storeId),
                        "placedAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp(),
                        "timeline" to listOf(
                            mapOf(
                                "status" to OrderStatus.PENDING.name,
                                "at" to now,
                                "note" to "Install requested",
                            ),
                            mapOf(
                                "status" to OrderStatus.DELIVERED.name,
                                "at" to now,
                                "note" to "Added to library",
                            ),
                        ),
                        "payment" to mapOf("method" to "NONE", "status" to "PAID"),
                    ),
                )

                tx.update(
                    productRef,
                    mapOf(
                        "unitsSold" to ((snap.get("unitsSold") as? Number)?.toInt() ?: 0) + 1,
                        "updatedAt" to FieldValue.serverTimestamp(),
                    ),
                )
                orderRef.id
            }.await()

            notifySeller(orderId, listOf(item), 0L)
            DataResult.Success(orderId)
        } catch (t: Throwable) {
            DataResult.Failure(t.toUserMessage(), t)
        }
    }

    /**
     * In-app notification for the seller. Cloud Functions would also push an FCM
     * message; on the Spark plan the notification document is written directly
     * and the seller sees it next time they open the app.
     */
    private suspend fun notifySeller(
        orderId: String,
        items: List<Map<String, Any?>>,
        subtotal: Long,
    ) {
        val storeIds = items.mapNotNull { it["storeId"] as? String }.distinct()
        val shortId = orderId.take(8).uppercase()
        storeIds.forEach { storeId ->
            runCatching {
                val store = firestore.collection(FirestorePaths.STORES).document(storeId).get().await()
                val ownerId = store.getString("ownerId") ?: return@forEach
                firestore.collection(FirestorePaths.notificationItems(ownerId)).document()
                    .set(
                        mapOf(
                            "title" to "New order",
                            "body" to "Order #$shortId for ₹${subtotal / 100} needs your confirmation.",
                            "type" to "ORDER",
                            "read" to false,
                            "deepLink" to "order/$orderId",
                            "createdAt" to FieldValue.serverTimestamp(),
                        ),
                        SetOptions.merge(),
                    ).await()
            }
        }
    }

    override suspend fun cancelOrder(orderId: String): DataResult<Unit> {
        val uid = uid ?: return DataResult.Failure("Sign in to cancel an order.")
        val ref = orders.document(orderId)
        return try {
            firestore.runTransaction { tx ->
                val snap = tx.get(ref)
                if (!snap.exists()) error("That order no longer exists.")
                val status = snap.getString("status").orEmpty()
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
