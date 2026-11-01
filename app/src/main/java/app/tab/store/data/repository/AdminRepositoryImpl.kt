package app.tab.store.data.repository

import app.tab.store.core.firebase.firestoreResult
import app.tab.store.core.firebase.snapshotFlow
import app.tab.store.core.firebase.toUserMessage
import app.tab.store.data.remote.FirestorePaths
import app.tab.store.data.remote.toProduct
import app.tab.store.data.remote.toStore
import app.tab.store.data.remote.toUser
import app.tab.store.domain.model.AdminOverview
import app.tab.store.domain.model.Product
import app.tab.store.domain.model.ProductStatus
import app.tab.store.domain.model.Store
import app.tab.store.domain.model.StoreStatus
import app.tab.store.domain.model.User
import app.tab.store.domain.model.UserRole
import app.tab.store.domain.repository.AdminRepository
import app.tab.store.domain.repository.DataResult
import com.google.firebase.firestore.AggregateSource
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.Query
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

@Singleton
class AdminRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
) : AdminRepository {

    private val stores get() = firestore.collection(FirestorePaths.STORES)
    private val products get() = firestore.collection(FirestorePaths.PRODUCTS)
    private val users get() = firestore.collection(FirestorePaths.USERS)

    /**
     * Computed from the collections themselves with count() aggregations, so the
     * dashboard needs no scheduled Cloud Function. GMV is summed over the most
     * recent 300 orders — a sample, labelled as such on the screen.
     */
    override fun observeOverview(): Flow<AdminOverview> = flow {
        emit(computeOverview())
    }

    private suspend fun computeOverview(): AdminOverview = try {
        val userCount = users.count().get(AggregateSource.SERVER).await().count
        val sellerCount = users.whereEqualTo("role", "SELLER")
            .count().get(AggregateSource.SERVER).await().count
        val storeCount = stores.count().get(AggregateSource.SERVER).await().count
        val pendingStores = stores.whereEqualTo("status", StoreStatus.PENDING.name)
            .count().get(AggregateSource.SERVER).await().count
        val productCount = products.count().get(AggregateSource.SERVER).await().count
        val blockedProducts = products.whereEqualTo("status", ProductStatus.BLOCKED.name)
            .count().get(AggregateSource.SERVER).await().count
        val orderCount = firestore.collection(FirestorePaths.ORDERS)
            .count().get(AggregateSource.SERVER).await().count

        val recent = firestore.collection(FirestorePaths.ORDERS)
            .orderBy("placedAt", Query.Direction.DESCENDING)
            .limit(300)
            .get()
            .await()
        val gmv = recent.documents
            .filter { it.getString("status") != "CANCELLED" }
            .sumOf { (it.get("total") as? Number)?.toLong() ?: 0L }

        AdminOverview(
            userCount = userCount.toInt(),
            sellerCount = sellerCount.toInt(),
            storeCount = storeCount.toInt(),
            pendingStores = pendingStores.toInt(),
            productCount = productCount.toInt(),
            blockedProducts = blockedProducts.toInt(),
            orderCount = orderCount.toInt(),
            grossMerchandiseValue = gmv,
        )
    } catch (t: Throwable) {
        AdminOverview()
    }

    override fun observePendingStores(): Flow<List<Store>> =
        stores.whereEqualTo("status", StoreStatus.PENDING.name)
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .snapshotFlow()
            .map { snap -> snap.documents.map { it.toStore() } }.orFallback(emptyList())

    override fun observeAllStores(): Flow<List<Store>> =
        stores.orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(100)
            .snapshotFlow()
            .map { snap -> snap.documents.map { it.toStore() } }.orFallback(emptyList())

    override suspend fun setStoreStatus(storeId: String, status: StoreStatus): DataResult<Unit> =
        firestoreResult {
            stores.document(storeId).update(
                mapOf(
                    "status" to status.name,
                    "moderatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                    "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                ),
            ).await()
        }

    override fun observeUsers(): Flow<List<User>> =
        users.orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(100)
            .snapshotFlow()
            .map { snap -> snap.documents.map { it.toUser() } }.orFallback(emptyList())

    override suspend fun setUserRole(uid: String, role: UserRole): DataResult<Unit> =
        firestoreResult {
            val updates = mutableMapOf<String, Any?>(
                "role" to role.name,
                "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
            )
            if (role == UserRole.CUSTOMER) updates["storeId"] = null
            users.document(uid).set(updates, SetOptions.merge()).await()
        }

    override fun observeReportedProducts(): Flow<List<Product>> =
        products.whereEqualTo("reported", true)
            .limit(100)
            .snapshotFlow()
            .map { snap -> snap.documents.map { it.toProduct() } }.orFallback(emptyList())

    override suspend fun setProductStatus(productId: String, status: ProductStatus): DataResult<Unit> =
        firestoreResult {
            products.document(productId).update(
                mapOf(
                    "status" to status.name,
                    "moderatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                    "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                ),
            ).await()
        }

    override suspend fun createCategory(name: String, slug: String, iconKey: String): DataResult<Unit> {
        return firestoreResult {
            val snap = firestore.collection(FirestorePaths.CATEGORIES).orderBy("order", Query.Direction.DESCENDING).limit(1).get().await()
            val nextOrder = ((snap.documents.firstOrNull()?.get("order") as? Number)?.toInt() ?: 0) + 1
            firestore.collection(FirestorePaths.CATEGORIES).document().set(
                mapOf(
                    "name" to name.trim(),
                    "slug" to slug.trim(),
                    "iconKey" to iconKey,
                    "order" to nextOrder,
                    "productCount" to 0,
                ),
            ).await()
        }
    }
}
