package app.tab.store.data.repository

import app.tab.store.core.firebase.orFallback
import app.tab.store.core.firebase.snapshotFlow
import app.tab.store.core.firebase.toUserMessage
import app.tab.store.data.remote.FirestorePaths
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

    override fun observeBuyerOrders(): Flow<List<Order>> {
        val uid = uid ?: return flowOf(emptyList())
        return orders.whereEqualTo("buyerId", uid)
            .orderBy("placedAt", Query.Direction.DESCENDING)
            .snapshotFlow()
            .map { snap -> snap.documents.map { it.toOrder() } }.orFallback(emptyList())
    }

    override fun observeOrder(orderId: String): Flow<Order?> =
        orders.document(orderId).snapshotFlow()
            .map { snap -> if (snap.exists()) snap.toOrder() else null }.orFallback(null)

    override suspend fun placeOrder(addressId: String, note: String?): DataResult<String> {
        val uid = uid ?: return DataResult.Failure("Sign in to place an order.")
        val orderRef = orders.document()
        return try {
            // The cart is read outside the transaction (Transaction has no
            // collection read); products are re-read *inside* it, so stock and
            // price are always the transactional values.
            val cartSnap = firestore.collection(FirestorePaths.cartLines(uid)).get().await()
            if (cartSnap.isEmpty) return DataResult.Failure("Your cart is empty.")

            val lines = cartSnap.documents.mapNotNull { doc ->
                val qty = (doc.get("quantity") as? Number)?.toInt() ?: 0
                if (qty > 0) doc.id to qty else null
            }
            if (lines.isEmpty()) return DataResult.Failure("Your cart is empty.")
            val cartRefs = cartSnap.documents.map { it.reference }

            // Captured for the seller notification below.
            var placedItems: List<Map<String, Any?>> = emptyList()
            var placedSubtotal = 0L

            val orderId = firestore.runTransaction { tx ->
                val addressSnap = tx.get(
                    firestore.collection(FirestorePaths.addressItems(uid)).document(addressId),
                )
                if (!addressSnap.exists()) error("That delivery address no longer exists.")

                val productRefs = lines.map {
                    firestore.collection(FirestorePaths.PRODUCTS).document(it.first)
                }
                // This SDK's Transaction only exposes get(DocumentReference),
                // so each product is read transactionally, one by one.
                val productSnaps = productRefs.map { ref -> tx.get(ref) }

                val items = mutableListOf<Map<String, Any?>>()
                var subtotal = 0L
                val storeIds = linkedSetOf<String>()

                productSnaps.forEachIndexed { index, snap ->
                    val qty = lines[index].second
                    if (!snap.exists()) error("An item in your cart is no longer available.")

                    val status = snap.getString("status").orEmpty()
                    val stock = (snap.get("stock") as? Number)?.toInt() ?: 0
                    val price = (snap.get("price") as? Number)?.toLong() ?: 0L
                    val title = snap.getString("title").orEmpty()
                    if (status != "ACTIVE") error("$title is no longer for sale.")
                    if (stock < qty) error("Only $stock left of $title. Please update your cart.")

                    @Suppress("UNCHECKED_CAST")
                    val images = snap.get("images") as? List<Map<String, Any?>>
                    val storeId = snap.getString("storeId").orEmpty()
                    storeIds += storeId

                    items += mapOf(
                        "productId" to snap.id,
                        "title" to title,
                        "imageUrl" to images?.firstOrNull()?.get("url"),
                        "unitPrice" to price,
                        "quantity" to qty,
                        "storeId" to storeId,
                        "storeName" to snap.getString("storeName").orEmpty(),
                    )
                    subtotal += price * qty

                    tx.update(
                        snap.reference,
                        mapOf(
                            "stock" to stock - qty,
                            "unitsSold" to ((snap.get("unitsSold") as? Number)?.toInt() ?: 0) + qty,
                            "updatedAt" to FieldValue.serverTimestamp(),
                        ),
                    )
                }

                val shipping = if (subtotal >= FREE_SHIPPING_PAISE) 0L else FLAT_SHIPPING_PAISE
                val address = addressSnap.data ?: emptyMap()

                tx.set(
                    orderRef,
                    mapOf(
                        "buyerId" to uid,
                        "buyerName" to (auth.currentUser?.displayName
                            ?: auth.currentUser?.email?.substringBefore("@").orEmpty()),
                        "items" to items,
                        "subtotal" to subtotal,
                        "shipping" to shipping,
                        "total" to subtotal + shipping,
                        "currency" to "INR",
                        "status" to OrderStatus.PENDING.name,
                        "note" to note.orEmpty(),
                        "address" to mapOf(
                            "id" to addressId,
                            "label" to address["label"],
                            "fullName" to address["fullName"],
                            "phone" to address["phone"],
                            "line1" to address["line1"],
                            "line2" to address["line2"],
                            "city" to address["city"],
                            "state" to address["state"],
                            "pincode" to address["pincode"],
                            "isDefault" to address["isDefault"],
                        ),
                        "storeIds" to storeIds.toList(),
                        "placedAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp(),
                        "timeline" to listOf(
                            mapOf(
                                "status" to OrderStatus.PENDING.name,
                                "at" to System.currentTimeMillis(),
                                "note" to "Order placed",
                            ),
                        ),
                        "payment" to mapOf("method" to "COD", "status" to "PENDING"),
                    ),
                )

                cartRefs.forEach { tx.delete(it) }
                placedItems = items
                placedSubtotal = subtotal
                orderRef.id
            }.await()

            notifySeller(orderId, placedItems, placedSubtotal)
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
                val items = snap.get("items") as? List<Map<String, Any?>> ?: emptyList()
                items.forEach { item ->
                    val productRef = firestore.collection(FirestorePaths.PRODUCTS)
                        .document(item["productId"] as? String ?: return@forEach)
                    val productSnap = tx.get(productRef)
                    if (productSnap.exists()) {
                        val qty = (item["quantity"] as? Number)?.toInt() ?: 0
                        tx.update(
                            productRef,
                            mapOf(
                                "stock" to ((productSnap.get("stock") as? Number)?.toInt() ?: 0) + qty,
                                "unitsSold" to (((productSnap.get("unitsSold") as? Number)?.toInt() ?: 0) - qty)
                                    .coerceAtLeast(0),
                                "updatedAt" to FieldValue.serverTimestamp(),
                            ),
                        )
                    }
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

    private companion object {
        const val FREE_SHIPPING_PAISE = 99_900L
        const val FLAT_SHIPPING_PAISE = 4_900L
    }
}
