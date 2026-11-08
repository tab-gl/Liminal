package app.tab.store.data.repository

import app.tab.store.core.firebase.firestoreResult
import app.tab.store.core.firebase.firestoreResultOf
import app.tab.store.core.firebase.orFallback
import app.tab.store.core.firebase.snapshotFlow
import app.tab.store.core.firebase.toUserMessage
import app.tab.store.data.remote.FirestorePaths
import app.tab.store.data.remote.booleanOr
import app.tab.store.data.remote.numberOr
import app.tab.store.data.remote.stringOr
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
            .filter { it.stringOr("status") != "CANCELLED" }
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

    /**
     * Promoting to SELLER requires a store the target user actually owns, so a
     * role can never be granted without a developer profile behind it.
     *
     * Block body (not `= try { ... }`) because the guards return early — Kotlin
     * forbids `return` inside an expression-bodied function.
     */
    override suspend fun setStoreOfficial(storeId: String, official: Boolean): DataResult<Unit> =
        firestoreResult {
            firestore.collection(FirestorePaths.STORES).document(storeId)
                .update(
                    mapOf(
                        "official" to official,
                        "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                    ),
                )
                .await()
        }

    override suspend fun setUserRole(uid: String, role: UserRole): DataResult<Unit> {
        return try {
            if (role == UserRole.SELLER) {
                val target = users.document(uid).get().await()
                if (!target.exists()) {
                    return DataResult.Failure("This user profile no longer exists.")
                }
                val storeId = target.stringOr("storeId")
                    ?: return DataResult.Failure(
                        "This user must create a store before becoming a seller.",
                    )
                val store = stores.document(storeId).get().await()
                if (!store.exists() || store.stringOr("ownerId") != uid) {
                    return DataResult.Failure("This user does not own the linked store.")
                }
            }
            firestoreResult {
                val updates = mutableMapOf<String, Any?>(
                    "role" to role.name,
                    "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                )
                if (role == UserRole.CUSTOMER) updates["storeId"] = null
                users.document(uid).set(updates, SetOptions.merge()).await()
            }
        } catch (t: Throwable) {
            DataResult.Failure(t.toUserMessage(), t)
        }
    }

    override fun observeReportedProducts(): Flow<List<Product>> =
        products.whereEqualTo("reported", true)
            .limit(100)
            .snapshotFlow()
            .map { snap -> snap.documents.map { it.toProduct() } }.orFallback(emptyList())

    override suspend fun resolveReport(productId: String, keepListed: Boolean): DataResult<Unit> =
        firestoreResult {
            val doc = products.document(productId)
            val product = doc.get().await()
            val owner = product.stringOr("storeId")
            doc.update(
                mapOf(
                    "reported" to false,
                    "reportReason" to "",
                    "status" to if (keepListed) "ACTIVE" else "BLOCKED",
                    "moderatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                    "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                ),
            ).await()

            // The developer hears the decision either way.
            val storeOwner = firestore.collection(FirestorePaths.STORES).document(owner)
                .get().await().stringOr("ownerId")
            if (storeOwner.isNotBlank()) {
                firestore.collection(FirestorePaths.NOTIFICATIONS).document(storeOwner)
                    .collection("items").document().set(
                        mapOf(
                            "type" to "MODERATION",
                            "title" to if (keepListed) "Your app is staying up" else "Your app was removed",
                            "body" to if (keepListed) {
                                "A report about ${product.stringOr("title")} was reviewed and dismissed."
                            } else {
                                "A report about ${product.stringOr("title")} was upheld, so it is no longer listed."
                            },
                            "deepLink" to "product/$productId",
                            "read" to false,
                            "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                        ),
                    ).await()
            }
        }

    override suspend fun listTags(): DataResult<List<Pair<String, Int>>> = firestoreResultOf {
        // Tags live on listings as a list, so the catalogue is the index. At this
        // size that is cheaper than maintaining a second collection.
        val snapshot = products.limit(500).get().await()
        snapshot.documents
            .flatMap { doc -> (doc.get("tags") as? List<*>)?.filterIsInstance<String>() ?: emptyList() }
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .groupingBy { it }
            .eachCount()
            .toList()
            .sortedByDescending { it.second }
    }

    override suspend fun renameTag(from: String, to: String): DataResult<Int> = firestoreResultOf {
        val needle = from.trim()
        val replacement = to.trim()
        val affected = products.whereArrayContains("tags", needle).get().await()
        affected.documents.forEach { doc ->
            val tags = (doc.get("tags") as? List<*>)?.filterIsInstance<String>()?.toMutableList()
                ?: return@forEach
            val updated = tags.map { if (it.equals(needle, ignoreCase = true)) replacement else it }
                .distinct()
            doc.reference.update("tags", updated).await()
        }
        affected.size()
    }

    override suspend fun deleteTag(tag: String): DataResult<Int> = firestoreResultOf {
        val needle = tag.trim()
        val affected = products.whereArrayContains("tags", needle).get().await()
        affected.documents.forEach { doc ->
            val tags = (doc.get("tags") as? List<*>)?.filterIsInstance<String>() ?: return@forEach
            doc.reference.update(
                "tags",
                tags.filterNot { it.equals(needle, ignoreCase = true) },
            ).await()
        }
        affected.size()
    }

    override suspend fun removeProduct(productId: String): DataResult<Unit> =
        firestoreResult {
            val doc = products.document(productId)
            val product = doc.get().await()
            val title = product.stringOr("title")
            val storeId = product.stringOr("storeId")

            // The listing goes, its reviews go with it, and the studio is told.
            // An admin needs a way to actually remove an app, not only hide it:
            // blocking leaves a tombstone that still appears in searches.
            firestore.collection(FirestorePaths.PRODUCTS).document(productId)
                .collection("reviews").get().await()
                .documents.forEach { review -> review.reference.delete().await() }
            doc.delete().await()

            if (storeId.isNotBlank()) {
                firestore.collection(FirestorePaths.STORES).document(storeId)
                    .collection("notifications").add(
                        mapOf(
                            "userId" to storeId,
                            "title" to "App removed",
                            "body" to "\"$title\" was removed by a moderator.",
                            "type" to "SYSTEM",
                            "read" to false,
                            "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                        ),
                    ).await()
            }
        }

    override suspend fun setProductStatus(productId: String, status: ProductStatus): DataResult<Unit> =
        firestoreResult {
            val product = products.document(productId).get().await()
            val title = product.stringOr("title")
            products.document(productId).update(
                mapOf(
                    "status" to status.name,
                    "moderatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                    "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                ),
            ).await()

            // Tell the developer, in the app, that a decision was made. The
            // rules allow an admin (and a buyer, for installs) to post a bounded
            // notice into someone's inbox.
            val storeId = product.stringOr("storeId")
            if (storeId.isNotBlank()) {
                runCatching {
                    val ownerId = firestore.collection(FirestorePaths.STORES)
                        .document(storeId).get().await().stringOr("ownerId")
                    if (ownerId.isNotBlank()) {
                        firestore.collection(FirestorePaths.notificationItems(ownerId))
                            .add(
                                mapOf(
                                    "type" to "APPROVAL",
                                    "title" to when (status) {
                                        ProductStatus.ACTIVE -> "Your app is live"
                                        ProductStatus.BLOCKED -> "Your app was blocked"
                                        ProductStatus.ARCHIVED -> "Your app was archived"
                                        else -> "Your app was updated"
                                    },
                                    "body" to "$title was reviewed by our team.",
                                    "deepLink" to "seller/product/$productId",
                                    "read" to false,
                                    "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                                ),
                            ).await()
                    }
                }
            }
        }

    override suspend fun seedDefaultCategories(): DataResult<Int> = try {
        val batch = firestore.batch()
        DEFAULT_CATEGORIES.forEachIndexed { index, (name, slug, iconKey) ->
            batch.set(
                firestore.collection(FirestorePaths.CATEGORIES).document(slug),
                mapOf(
                    "name" to name,
                    "slug" to slug,
                    "iconKey" to iconKey,
                    "order" to (index + 1),
                    "productCount" to 0,
                    "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                ),
                SetOptions.merge(),
            )
        }
        batch.commit().await()
        DataResult.Success(DEFAULT_CATEGORIES.size)
    } catch (t: Throwable) {
        DataResult.Failure(t.toUserMessage(), t)
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

    private companion object {
        /** The standard app-store taxonomy, also used by tools/seed. */
        val DEFAULT_CATEGORIES = listOf(
            Triple("Productivity", "productivity", "check"),
            Triple("Games", "games", "gamepad"),
            Triple("Photography", "photography", "camera"),
            Triple("Finance", "finance", "wallet"),
            Triple("Education", "education", "book"),
            Triple("Health & Fitness", "health-fitness", "heart"),
            Triple("Social", "social", "users"),
            Triple("Tools", "tools", "settings"),
        )
    }
}
