package app.tab.store.data.repository

import app.tab.store.core.database.CachedProductEntity
import app.tab.store.core.database.CachedProductDao
import app.tab.store.core.database.RecentlyViewedDao
import app.tab.store.core.database.RecentlyViewedEntity
import app.tab.store.core.firebase.orFallback
import app.tab.store.core.firebase.snapshotFlow
import app.tab.store.core.firebase.toUserMessage
import app.tab.store.core.firebase.firestoreResult
import app.tab.store.data.remote.FirestorePaths
import app.tab.store.data.remote.booleanOr
import app.tab.store.data.remote.numberOr
import app.tab.store.data.remote.stringOr
import app.tab.store.data.remote.toCategory
import app.tab.store.data.remote.toProduct
import app.tab.store.data.remote.toStore
import app.tab.store.domain.model.Category
import app.tab.store.domain.model.Product
import app.tab.store.domain.model.ProductImage
import app.tab.store.domain.model.ProductStatus
import app.tab.store.domain.model.Store
import app.tab.store.domain.repository.CatalogRepository
import app.tab.store.domain.repository.DataResult
import app.tab.store.domain.repository.ProductPage
import app.tab.store.domain.repository.ProductSort
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

@Singleton
class CatalogRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val recentlyViewedDao: RecentlyViewedDao,
    private val cachedProductDao: CachedProductDao,
) : CatalogRepository {

    private val products get() = firestore.collection(FirestorePaths.PRODUCTS)

    private fun activeProducts(): Query = products.whereEqualTo("status", ProductStatus.ACTIVE.name)

    override fun observeCategories(): Flow<List<Category>> =
        firestore.collection(FirestorePaths.CATEGORIES)
            .orderBy("order")
            .snapshotFlow()
            .map { snap -> snap.documents.map { it.toCategory() } }.orFallback(emptyList())

    override suspend fun refreshCategories(): DataResult<Unit> = try {
        val snap = firestore.collection(FirestorePaths.CATEGORIES).orderBy("order").get().await()
        cachedProductDao.clear()
        DataResult.Success(Unit)
    } catch (t: Throwable) {
        DataResult.Failure(t.toUserMessage(), t)
    }

    /**
     * Every rail reads one equality filter — status — and sorts here.
     *
     * Adding an orderBy alongside a status filter needs a composite index. When
     * that index is not deployed the query fails, the fallback returns an empty
     * list, and the storefront looks broken rather than misconfigured. Sorting a
     * page of results in the app costs nothing at this size and cannot fail that
     * way.
     */
    private fun activePage(limit: Int = 60): Flow<List<Product>> =
        products.whereEqualTo("status", ProductStatus.ACTIVE.name)
            .limit(limit.toLong())
            .snapshotFlow()
            .map { snap -> snap.documents.map { it.toProduct() } }
            .orFallback(emptyList())

    override fun observeFeatured(limit: Int): Flow<List<Product>> =
        activePage()
            .map { items ->
                items.filter { it.featured }.sortedByDescending { it.unitsSold }.take(limit)
                    .ifEmpty { items.sortedByDescending { it.unitsSold }.take(limit) }
                    .also { cache(it) }
            }

    override fun observeTrending(limit: Int): Flow<List<Product>> =
        activePage().map { items -> items.sortedByDescending { it.unitsSold }.take(limit) }

    override fun observeNewArrivals(limit: Int): Flow<List<Product>> =
        activePage().map { items -> items.sortedByDescending { it.createdAt }.take(limit) }

    override fun observeRecommended(limit: Int): Flow<List<Product>> =
        activePage().map { items -> items.sortedByDescending { it.rating }.take(limit) }

    override fun observeByCategory(categoryId: String, limit: Int): Flow<List<Product>> =
        activePage().map { items ->
            items.filter { it.categoryId == categoryId }
                .sortedByDescending { it.unitsSold }
                .take(limit)
        }

    /**
     * Search is a Firestore range scan on the lower-cased title (`titleLower`),
     * which gives real prefix matching with no extra infrastructure. It is
     * documented in docs/ARCHITECTURE.md as the seam where a dedicated search
     * index (Algolia / Typesense) would slot in for typo tolerance.
     */
    override suspend fun search(
        query: String,
        categoryId: String?,
        minPricePaise: Long?,
        maxPricePaise: Long?,
        sort: ProductSort,
        pageSize: Int,
        pageToken: String?,
    ): DataResult<ProductPage> = try {
        var q: Query = activeProducts()

        if (query.isNotBlank()) {
            val needle = query.trim().lowercase()
            q = q.whereGreaterThanOrEqualTo("titleLower", needle)
                .whereLessThan("titleLower", needle + "\uF8FF")
        }
        if (categoryId != null) q = q.whereEqualTo("categoryId", categoryId)
        if (minPricePaise != null) q = q.whereGreaterThanOrEqualTo("price", minPricePaise)
        if (maxPricePaise != null) q = q.whereLessThanOrEqualTo("price", maxPricePaise)

        q = when (sort) {
            ProductSort.PRICE_LOW_HIGH -> q.orderBy("price", Query.Direction.ASCENDING)
            ProductSort.PRICE_HIGH_LOW -> q.orderBy("price", Query.Direction.DESCENDING)
            ProductSort.NEWEST -> q.orderBy("createdAt", Query.Direction.DESCENDING)
            ProductSort.TOP_RATED -> q.orderBy("rating", Query.Direction.DESCENDING)
            ProductSort.RELEVANCE -> q.orderBy("unitsSold", Query.Direction.DESCENDING)
        }

        if (pageToken != null) {
            val anchor = products.document(pageToken).get().await()
            if (anchor.exists()) q = q.startAfter(anchor)
        }

        val snapshot = q.limit(pageSize.toLong()).get().await()
        val items = snapshot.documents.map { it.toProduct() }
        val next = if (snapshot.size() == pageSize) snapshot.documents.lastOrNull()?.id else null
        cache(items)
        DataResult.Success(ProductPage(items, next))
    } catch (t: Throwable) {
        DataResult.Failure(t.toUserMessage(), t)
    }

    override suspend fun searchStores(
        query: String,
        limit: Int,
    ): List<app.tab.store.domain.model.Store> = runCatching {
        val text = query.trim()
        if (text.isBlank()) {
            // With no query, show the studios that actually have something.
            firestore.collection(FirestorePaths.STORES)
                .whereEqualTo("status", "ACTIVE")
                .limit(limit.toLong())
                .get().await()
        } else {
            firestore.collection(FirestorePaths.STORES)
                .whereGreaterThanOrEqualTo("name", text)
                .whereLessThan("name", text + "\uf8ff")
                .limit(limit.toLong())
                .get().await()
        }
    }.getOrNull()?.documents?.map { it.toStore() }.orEmpty()

    override fun observeReviews(
        productId: String,
    ): Flow<List<app.tab.store.domain.model.Review>> = callbackFlow {
        val registration = firestore.collection(FirestorePaths.PRODUCTS).document(productId)
            .collection("reviews")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                trySend(
                    snapshot?.documents.orEmpty().mapNotNull { doc ->
                        runCatching {
                    app.tab.store.domain.model.Review(
                        reviewerId = doc.id,
                        reviewerName = doc.stringOr("reviewerName"),
                        rating = (doc.get("rating") as? Number)?.toInt() ?: 0,
                        text = doc.stringOr("text"),
                        // createdAt is a server timestamp, and getLong() throws on a
                        // Timestamp — that was the crash when a review was added.
                        createdAt = (doc.get("createdAt") as? com.google.firebase.Timestamp)
                            ?.toDate()?.time
                            ?: (doc.get("createdAt") as? Number)?.toLong()
                            ?: 0L,
                    )
                        }.getOrNull()
                    },
                )
            }
        awaitClose { registration.remove() }
    }

    override suspend fun rate(productId: String, rating: Int, text: String): DataResult<Unit> =
        firestoreResult {
            val user = auth.currentUser ?: error("Sign in to rate an app.")
            firestore.collection(FirestorePaths.PRODUCTS).document(productId)
                .collection("reviews").document(user.uid)
                .set(
                    mapOf(
                        "reviewerId" to user.uid,
                        "reviewerName" to (user.displayName ?: "Someone"),
                        "rating" to rating.coerceIn(1, 5),
                        "text" to text.trim().take(500),
                        "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                    ),
                )
                .await()
        }

    override suspend fun reportProduct(productId: String, reason: String): DataResult<Unit> =
        firestoreResult {
            val current = firestore.collection(FirestorePaths.PRODUCTS)
                .document(productId).get().await()
                .get("reportCount") as? Long ?: 0L
            firestore.collection(FirestorePaths.PRODUCTS).document(productId)
                .update(
                    mapOf(
                        "reported" to true,
                        "reportReason" to reason.trim().take(200),
                        // A literal, not an increment transform: the rules compare
                        // the resulting value and a transform is not resolved when
                        // they run.
                        "reportCount" to current + 1,
                        "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                    ),
                )
                .await()
        }

    override fun observeProduct(productId: String): Flow<Product?> =
        products.document(productId).snapshotFlow()
            .map { snap -> if (snap.exists()) snap.toProduct() else null }.orFallback(null)

    override suspend fun recordProductView(productId: String) {
        val uid = auth.currentUser?.uid ?: return
        val snap = products.document(productId).get().await()
        if (!snap.exists()) return
        val product = snap.toProduct()
        recentlyViewedDao.upsert(
            RecentlyViewedEntity(
                productId = product.id,
                title = product.title,
                storeName = product.storeName,
                imageUrl = product.primaryImage,
                price = product.price,
                compareAtPrice = product.compareAtPrice,
                categoryId = product.categoryId,
                categoryLabel = product.categoryLabel,
                viewedAt = System.currentTimeMillis(),
            ),
        )
        recentlyViewedDao.trim()
    }

    override fun observeRecentlyViewed(limit: Int): Flow<List<Product>> =
        recentlyViewedDao.observe(limit).map { rows ->
            rows.map { row ->
                Product(
                    id = row.productId,
                    title = row.title,
                    storeName = row.storeName,
                    images = listOfNotNull(row.imageUrl?.let { ProductImage(it, null) }),
                    price = row.price,
                    compareAtPrice = row.compareAtPrice,
                    categoryId = row.categoryId,
                    categoryLabel = row.categoryLabel,
                )
            }
        }

    override fun observeStore(storeId: String): Flow<Store?> =
        firestore.collection(FirestorePaths.STORES).document(storeId)
            .snapshotFlow()
            .map { snap -> if (snap.exists()) snap.toStore() else null }.orFallback(null)

    override fun observeStoreProducts(storeId: String): Flow<List<Product>> =
        // One equality filter and sorting done here: adding status and an orderBy
        // needs a composite index, and without that index the query fails and the
        // fallback shows "no apps" — which looked like the profile was broken.
        products.whereEqualTo("storeId", storeId)
            .snapshotFlow()
            .map { snap ->
                snap.documents.map { it.toProduct() }
                    .filter { it.status == ProductStatus.ACTIVE }
                    .sortedByDescending { it.createdAt }
            }.orFallback(emptyList())

    private suspend fun cache(items: List<Product>) {
        if (items.isEmpty()) return
        val now = System.currentTimeMillis()
        cachedProductDao.upsertAll(
            items.map { p ->
                CachedProductEntity(
                    productId = p.id,
                    storeId = p.storeId,
                    storeName = p.storeName,
                    title = p.title,
                    imageUrl = p.primaryImage,
                    price = p.price,
                    compareAtPrice = p.compareAtPrice,
                    stock = p.stock,
                    status = p.status.name,
                    rating = p.rating,
                    ratingCount = p.ratingCount,
                    unitsSold = p.unitsSold,
                    categoryId = p.categoryId,
                    categoryLabel = p.categoryLabel,
                    description = p.description,
                    tags = p.tags.joinToString(","),
                    cachedAt = now,
                )
            },
        )
    }
}
