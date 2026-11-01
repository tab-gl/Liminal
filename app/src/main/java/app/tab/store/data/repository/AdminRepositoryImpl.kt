package app.tab.store.data.repository

import app.tab.store.core.firebase.firestoreResult
import app.tab.store.core.firebase.snapshotFlow
import app.tab.store.core.firebase.toUserMessage
import app.tab.store.core.network.FunctionsClient
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
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

@Singleton
class AdminRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val functionsClient: FunctionsClient,
) : AdminRepository {

    private val stores get() = firestore.collection(FirestorePaths.STORES)
    private val products get() = firestore.collection(FirestorePaths.PRODUCTS)
    private val users get() = firestore.collection(FirestorePaths.USERS)

    override fun observeOverview(): Flow<AdminOverview> =
        firestore.document(FirestorePaths.platformStats()).snapshotFlow().map { snap ->
            AdminOverview(
                userCount = (snap.get("userCount") as? Number)?.toInt() ?: 0,
                sellerCount = (snap.get("sellerCount") as? Number)?.toInt() ?: 0,
                storeCount = (snap.get("storeCount") as? Number)?.toInt() ?: 0,
                pendingStores = (snap.get("pendingStores") as? Number)?.toInt() ?: 0,
                productCount = (snap.get("productCount") as? Number)?.toInt() ?: 0,
                blockedProducts = (snap.get("blockedProducts") as? Number)?.toInt() ?: 0,
                orderCount = (snap.get("orderCount") as? Number)?.toInt() ?: 0,
                grossMerchandiseValue = (snap.get("gmv") as? Number)?.toLong() ?: 0L,
            )
        }

    override fun observePendingStores(): Flow<List<Store>> =
        stores.whereEqualTo("status", StoreStatus.PENDING.name)
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .snapshotFlow()
            .map { snap -> snap.documents.map { it.toStore() } }

    override fun observeAllStores(): Flow<List<Store>> =
        stores.orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(100)
            .snapshotFlow()
            .map { snap -> snap.documents.map { it.toStore() } }

    override suspend fun setStoreStatus(storeId: String, status: StoreStatus): DataResult<Unit> =
        when (
            val res = functionsClient.call(
                "adminSetStoreStatus",
                mapOf("storeId" to storeId, "status" to status.name),
            )
        ) {
            is DataResult.Failure -> res
            is DataResult.Success -> DataResult.Success(Unit)
        }

    override fun observeUsers(): Flow<List<User>> =
        users.orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(100)
            .snapshotFlow()
            .map { snap -> snap.documents.map { it.toUser() } }

    override suspend fun setUserRole(uid: String, role: UserRole): DataResult<Unit> =
        when (val res = functionsClient.call("setUserRole", mapOf("uid" to uid, "role" to role.name))) {
            is DataResult.Failure -> res
            is DataResult.Success -> DataResult.Success(Unit)
        }

    override fun observeReportedProducts(): Flow<List<Product>> =
        products.whereEqualTo("reported", true)
            .limit(100)
            .snapshotFlow()
            .map { snap -> snap.documents.map { it.toProduct() } }

    override suspend fun setProductStatus(productId: String, status: ProductStatus): DataResult<Unit> =
        when (
            val res = functionsClient.call(
                "adminSetProductStatus",
                mapOf("productId" to productId, "status" to status.name),
            )
        ) {
            is DataResult.Failure -> res
            is DataResult.Success -> DataResult.Success(Unit)
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
