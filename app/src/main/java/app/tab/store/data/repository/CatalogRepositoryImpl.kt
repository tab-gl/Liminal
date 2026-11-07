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
import kotlinx.coroutines.flow.first
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
        // Nothing is cleared here. This used to wipe the product cache, which meant
        // the six-hourly sync deleted the offline catalogue it exists to keep.
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

    override fun observeCatalogue(limit: Int): Flow<List<Product>> = activePage(limit)

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
    /**
     * Search runs over one page of active listings, in the app.
     *
     * A Firestore range scan on `titleLower` needs a composite index with status;
     * without that index the query fails and search silently returns nothing. It
     * also cannot look inside a list, so tags were never searchable. Filtering a
     * page here costs nothing at this size, matches titles, studios and tags, and
     * cannot fail that way.
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
        val needle = query.trim().lowercase()
        var items = activePage(limit = 200).first()

        if (needle.isNotBlank()) {
            items = items.filter { product ->
                product.title.lowercase().contains(needle) ||
                    product.storeName.lowercase().contains(needle) ||
                    product.categoryLabel.lowercase().contains(needle) ||
                    product.tags.any { it.lowercase().contains(needle) }
            }
        }
        if (categoryId != null) items = items.filter { it.categoryId == categoryId }
        if (minPricePaise != null) items = items.filter { it.price >= minPricePaise }
        if (maxPricePaise != null) items = items.filter { it.price <= maxPricePaise }

        items = when (sort) {
            ProductSort.PRICE_LOW_HIGH -> items.sortedBy { it.price }
            ProductSort.PRICE_HIGH_LOW -> items.sortedByDescending { it.price }
            ProductSort.NEWEST -> items.sortedByDescending { it.createdAt }
            ProductSort.TOP_RATED -> items.sortedByDescending { it.rating }
            ProductSort.RELEVANCE -> items.sortedByDescending { it.unitsSold }
        }

        val offset = pageToken?.toIntOrNull() ?: 0
        val page = items.drop(offset).take(pageSize)
        val next = if (offset + page.size < items.size) (offset + page.size).toString() else null
        cache(page)
        DataResult.Success(ProductPage(page, next))
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
        // The rules only allow a public listing query that constrains `status`, so
        // this filters on status and narrows to the studio here. Querying by
        // storeId alone was denied outright, which is why a studio page could come
        // back empty.
        products.whereEqualTo("status", ProductStatus.ACTIVE.name)
            .snapshotFlow()
            .map { snap ->
                snap.documents.map { it.toProduct() }
                    .filter { it.storeId == storeId }
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
