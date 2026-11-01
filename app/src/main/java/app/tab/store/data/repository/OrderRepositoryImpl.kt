package app.tab.store.data.repository

import app.tab.store.core.firebase.snapshotFlow
import app.tab.store.core.network.FunctionsClient
import app.tab.store.data.remote.FirestorePaths
import app.tab.store.data.remote.toOrder
import app.tab.store.domain.model.Order
import app.tab.store.domain.model.OrderStatus
import app.tab.store.domain.repository.DataResult
import app.tab.store.domain.repository.OrderRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

@Singleton
class OrderRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val functionsClient: FunctionsClient,
) : OrderRepository {

    private val orders get() = firestore.collection(FirestorePaths.ORDERS)
    private val uid: String? get() = auth.currentUser?.uid

    override fun observeBuyerOrders(): Flow<List<Order>> {
        val uid = uid ?: return flowOf(emptyList())
        return orders.whereEqualTo("buyerId", uid)
            .orderBy("placedAt", Query.Direction.DESCENDING)
            .snapshotFlow()
            .map { snap -> snap.documents.map { it.toOrder() } }
    }

    override fun observeOrder(orderId: String): Flow<Order?> =
        orders.document(orderId).snapshotFlow()
            .map { snap -> if (snap.exists()) snap.toOrder() else null }

    /**
     * Order creation is a Cloud Function call: the server re-reads the cart,
     * re-prices every line, validates stock, writes the order, decrements
     * inventory and empties the cart in one transaction. The client cannot
     * choose prices or totals.
     */
    override suspend fun placeOrder(addressId: String, note: String?): DataResult<String> =
        functionsClient.callString(
            "createOrder",
            buildMap {
                put("addressId", addressId)
                if (!note.isNullOrBlank()) put("note", note)
            },
            key = "orderId",
        )

    override suspend fun cancelOrder(orderId: String): DataResult<Unit> =
        when (val res = functionsClient.call("cancelOrder", mapOf("orderId" to orderId))) {
            is DataResult.Failure -> res
            is DataResult.Success -> DataResult.Success(Unit)
        }

    override fun observeSellerOrders(storeId: String): Flow<List<Order>> =
        orders.whereArrayContains("storeIds", storeId)
            .orderBy("placedAt", Query.Direction.DESCENDING)
            .snapshotFlow()
            .map { snap -> snap.documents.map { it.toOrder() } }

    override suspend fun updateOrderStatus(orderId: String, status: OrderStatus): DataResult<Unit> =
        when (
            val res = functionsClient.call(
                "updateOrderStatus",
                mapOf("orderId" to orderId, "status" to status.name),
            )
        ) {
            is DataResult.Failure -> res
            is DataResult.Success -> DataResult.Success(Unit)
        }

    override fun observeAllOrders(): Flow<List<Order>> =
        orders.orderBy("placedAt", Query.Direction.DESCENDING)
            .limit(200)
            .snapshotFlow()
            .map { snap -> snap.documents.map { it.toOrder() } }
}
