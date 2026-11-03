package app.tab.store.data.repository

import app.tab.store.core.firebase.firestoreResult
import app.tab.store.core.firebase.orFallback
import app.tab.store.core.firebase.snapshotFlow
import app.tab.store.core.firebase.toUserMessage
import app.tab.store.data.remote.FirestorePaths
import app.tab.store.data.remote.toProduct
import app.tab.store.data.remote.toStore
import app.tab.store.domain.model.Order
import app.tab.store.domain.model.OrderStatus
import app.tab.store.domain.model.Product
import app.tab.store.domain.model.ProductPerformance
import app.tab.store.domain.model.ProductStatus
import app.tab.store.domain.model.SellerStats
import app.tab.store.domain.model.Store
import app.tab.store.domain.repository.DataResult
import app.tab.store.domain.repository.SellerRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

@Singleton
class SellerRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
) : SellerRepository {

    private val uidFlow = MutableStateFlow(auth.currentUser?.uid)

    init {
        auth.addAuthStateListener { uidFlow.value = it.currentUser?.uid }
    }

    private fun storeIdFlow(): Flow<String?> = uidFlow.flatMapLatest { uid ->
        if (uid == null) {
            flowOf(null)
        } else {
            firestore.collection(FirestorePaths.USERS).document(uid)
                .snapshotFlow()
                .map { it.getString("storeId") }.orFallback(null)
        }
    }

    override fun observeMyStore(): Flow<Store?> = storeIdFlow().flatMapLatest { storeId ->
        if (storeId.isNullOrBlank()) {
            flowOf(null)
        } else {
            firestore.collection(FirestorePaths.STORES).document(storeId)
                .snapshotFlow()
                .map { if (it.exists()) it.toStore() else null }.orFallback(null)
        }
    }

    /**
     * Creates the store and promotes the caller in one batch. `firestore.rules`
     * re-checks both writes: the store must be owned by the caller and start
     * PENDING, and the user's role may only move CUSTOMER → SELLER with a
     * storeId the caller actually owns.
     */
    override suspend fun createStore(name: String, tagline: String, city: String): DataResult<String> {
        val uid = auth.currentUser?.uid ?: return DataResult.Failure("You are signed out.")
        val storeRef = firestore.collection(FirestorePaths.STORES).document()
        return try {
            val userRef = firestore.collection(FirestorePaths.USERS).document(uid)
            // An admin (or an existing developer opening a second profile) must
            // not be demoted to SELLER: only a shopper is promoted.
            val currentRole = userRef.get().await().getString("role")
            val promote = currentRole == null || currentRole.equals("CUSTOMER", ignoreCase = true)

            val batch = firestore.batch()
            batch.set(
                storeRef,
                mapOf(
                    "ownerId" to uid,
                    "name" to name.trim(),
                    "slug" to app.tab.store.core.utils.Validators.slugify(name),
                    "tagline" to tagline.trim(),
                    "description" to "",
                    "logoUrl" to null,
                    "logoPublicId" to null,
                    "bannerUrl" to null,
                    "bannerPublicId" to null,
                    "city" to city.trim(),
                    "status" to "PENDING",
                    "rating" to 0.0,
                    "ratingCount" to 0,
                    "productCount" to 0,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp(),
                ),
            )
            val userFields = mutableMapOf<String, Any?>(
                "storeId" to storeRef.id,
                "updatedAt" to FieldValue.serverTimestamp(),
            )
            if (promote) userFields["role"] = "SELLER"
            batch.set(userRef, userFields, SetOptions.merge())
            batch.commit().await()
            DataResult.Success(storeRef.id)
        } catch (t: Throwable) {
            DataResult.Failure(t.toUserMessage(), t)
        }
    }

    override suspend fun updateStore(store: Store): DataResult<Unit> {
        val uid = auth.currentUser?.uid ?: return DataResult.Failure("You are signed out.")
        return firestoreResult {
            firestore.collection(FirestorePaths.STORES).document(store.id).set(
                mapOf(
                    "name" to store.name.trim(),
                    "tagline" to store.tagline.trim(),
                    "description" to store.description.trim(),
                    "city" to store.city.trim(),
                    "logoUrl" to store.logoUrl,
                    "logoPublicId" to store.logoPublicId,
                    "bannerUrl" to store.bannerUrl,
                    "bannerPublicId" to store.bannerPublicId,
                    "ownerId" to uid,
                    "updatedAt" to FieldValue.serverTimestamp(),
                ),
                SetOptions.merge(),
            ).await()
        }
    }

    override fun observeStoreProducts(storeId: String): Flow<List<Product>> =
        firestore.collection(FirestorePaths.PRODUCTS)
            .whereEqualTo("storeId", storeId)
            .orderBy("updatedAt", Query.Direction.DESCENDING)
            .snapshotFlow()
            .map { snap -> snap.documents.map { it.toProduct() } }.orFallback(emptyList())

    override suspend fun upsertProduct(product: Product): DataResult<String> = try {
        val ref = firestore.collection(FirestorePaths.PRODUCTS)
        val docRef = if (product.id.isBlank()) ref.document() else ref.document(product.id)
        val payload = mutableMapOf<String, Any?>(
            "storeId" to product.storeId,
            "storeName" to product.storeName,
            "title" to product.title.trim(),
            "titleLower" to product.title.trim().lowercase(),
            "description" to product.description.trim(),
            "categoryId" to product.categoryId,
            "categoryLabel" to product.categoryLabel,
            "images" to product.images.map { mapOf("url" to it.url, "publicId" to it.publicId) },
            "price" to product.price,
            "compareAtPrice" to product.compareAtPrice,
            "currency" to "INR",
            "stock" to product.stock,
            "status" to product.status.name,
            "tags" to product.tags,
            "updatedAt" to FieldValue.serverTimestamp(),
        )
        if (product.id.isBlank()) {
            payload["createdAt"] = FieldValue.serverTimestamp()
            payload["rating"] = 0.0
            payload["ratingCount"] = 0
            payload["unitsSold"] = 0
        }
        docRef.set(payload, SetOptions.merge()).await()
        DataResult.Success(docRef.id)
    } catch (t: Throwable) {
        DataResult.Failure(t.toUserMessage(), t)
    }

    override suspend fun deleteProduct(productId: String): DataResult<Unit> =
        // The document is removed here; Cloudinary assets need the api_secret to
        // destroy, so on the Spark plan they are left for manual cleanup in the
        // Cloudinary console (or purged by the deleteProduct function once
        // Cloud Functions are deployed).
        firestoreResult {
            firestore.collection(FirestorePaths.PRODUCTS).document(productId).delete().await()
        }

    override suspend fun setStock(productId: String, stock: Int): DataResult<Unit> =
        firestoreResult {
            firestore.collection(FirestorePaths.PRODUCTS).document(productId)
                .update(
                    mapOf(
                        "stock" to stock.coerceAtLeast(0),
                        "updatedAt" to FieldValue.serverTimestamp(),
                    ),
                ).await()
        }

    override suspend fun setProductStatus(productId: String, status: ProductStatus): DataResult<Unit> =
        firestoreResult {
            firestore.collection(FirestorePaths.PRODUCTS).document(productId)
                .update(
                    mapOf(
                        "status" to status.name,
                        "updatedAt" to FieldValue.serverTimestamp(),
                    ),
                ).await()
        }

    /**
     * Seller analytics are derived from the seller's real order stream — the
     * same documents the buyer sees — so the dashboard can never drift from
     * actual sales.
     */
    override fun observeStats(storeId: String): Flow<SellerStats> {
        val orders = firestore.collection(FirestorePaths.ORDERS)
            .whereArrayContains("storeIds", storeId)
            .orderBy("placedAt", Query.Direction.DESCENDING)
            .limit(300)
            .snapshotFlow()
            .map { snap -> snap.documents.map { doc ->
                @Suppress("UNCHECKED_CAST")
                val items = doc.get("items") as? List<Map<String, Any?>> ?: emptyList()
                val mine = items.filter { it["storeId"] == storeId }
                Triple(
                    doc.getString("status").orEmpty(),
                    (doc.get("placedAt") as? Number)?.toLong() ?: 0L,
                    mine,
                )
            } }
            .orFallback(emptyList<Triple<String, Long, List<Map<String, Any?>>>>())

        val products = firestore.collection(FirestorePaths.PRODUCTS)
            .whereEqualTo("storeId", storeId)
            .snapshotFlow()
            .map { snap -> snap.documents.map { it.toProduct() } }.orFallback(emptyList())

        return combine(orders, products) { orderRows, productRows ->
            val valid = orderRows.filter { it.first != OrderStatus.CANCELLED.name }
            var revenue = 0L
            var units = 0
            val perProduct = mutableMapOf<String, ProductPerformance>()
            val byDay = mutableMapOf<String, Long>()
            val dayFormat = SimpleDateFormat("d MMM", Locale("en", "IN"))
            val cutoff = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -6) }.timeInMillis

            valid.forEach { (status, placedAt, items) ->
                items.forEach { item ->
                    val qty = (item["quantity"] as? Number)?.toInt() ?: 0
                    val price = (item["unitPrice"] as? Number)?.toLong() ?: 0L
                    val line = price * qty
                    revenue += line
                    units += qty
                    val id = item["productId"] as? String ?: return@forEach
                    val existing = perProduct[id]
                    perProduct[id] = ProductPerformance(
                        productId = id,
                        title = item["title"] as? String ?: existing?.title.orEmpty(),
                        imageUrl = item["imageUrl"] as? String,
                        unitsSold = (existing?.unitsSold ?: 0) + qty,
                        revenue = (existing?.revenue ?: 0L) + line,
                    )
                    if (placedAt >= cutoff) {
                        val key = dayFormat.format(Date(placedAt))
                        byDay[key] = (byDay[key] ?: 0L) + line
                    }
                }
            }

            val days = (0..6).map { offset ->
                val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, offset - 6) }
                val key = dayFormat.format(cal.time)
                key to (byDay[key] ?: 0L)
            }

            SellerStats(
                totalRevenue = revenue,
                orderCount = valid.size,
                unitsSold = units,
                activeProducts = productRows.count { it.status == ProductStatus.ACTIVE },
                lowStockCount = productRows.count { it.stock in 1..5 },
                averageOrderValue = if (valid.isEmpty()) 0 else revenue / valid.size,
                pendingOrders = orderRows.count { it.first == OrderStatus.PENDING.name },
                topProducts = perProduct.values.sortedByDescending { it.revenue }.take(5),
                revenueByDay = days,
            )
        }
    }
}
