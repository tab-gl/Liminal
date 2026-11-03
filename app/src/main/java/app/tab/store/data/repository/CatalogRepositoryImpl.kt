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

    override fun observeFeatured(limit: Int): Flow<List<Product>> =
        activeProducts()
            .whereEqualTo("featured", true)
            .orderBy("unitsSold", Query.Direction.DESCENDING)
            .limit(limit.toLong())
            .snapshotFlow()
            .map { snap ->
                val items = snap.documents.map { it.toProduct() }
                cache(items)
                items
            }
            .orFallback(emptyList())

    override fun observeTrending(limit: Int): Flow<List<Product>> =
        activeProducts()
            .orderBy("unitsSold", Query.Direction.DESCENDING)
            .limit(limit.toLong())
            .snapshotFlow()
            .map { snap -> snap.documents.map { it.toProduct() } }.orFallback(emptyList())

    override fun observeNewArrivals(limit: Int): Flow<List<Product>> =
        activeProducts()
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(limit.toLong())
            .snapshotFlow()
            .map { snap -> snap.documents.map { it.toProduct() } }.orFallback(emptyList())

    override fun observeRecommended(limit: Int): Flow<List<Product>> =
        activeProducts()
            .orderBy("rating", Query.Direction.DESCENDING)
            .limit(limit.toLong())
            .snapshotFlow()
            .map { snap -> snap.documents.map { it.toProduct() } }.orFallback(emptyList())

    override fun observeByCategory(categoryId: String, limit: Int): Flow<List<Product>> =
        activeProducts()
            .whereEqualTo("categoryId", categoryId)
            .orderBy("unitsSold", Query.Direction.DESCENDING)
            .limit(limit.toLong())
            .snapshotFlow()
            .map { snap -> snap.documents.map { it.toProduct() } }.orFallback(emptyList())

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

    override suspend fun reportProduct(productId: String, reason: String): DataResult<Unit> =
        firestoreResult {
            firestore.collection(FirestorePaths.PRODUCTS).document(productId)
                .update(
                    mapOf(
                        "reported" to true,
                        "reportReason" to reason.trim().take(200),
                        "reportCount" to com.google.firebase.firestore.FieldValue.increment(1),
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
        products.whereEqualTo("storeId", storeId)
            .whereEqualTo("status", ProductStatus.ACTIVE.name)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .snapshotFlow()
            .map { snap -> snap.documents.map { it.toProduct() } }.orFallback(emptyList())

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
