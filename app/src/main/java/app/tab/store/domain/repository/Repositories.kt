package app.tab.store.domain.repository

import app.tab.store.domain.model.Address
import app.tab.store.domain.model.AdminOverview
import app.tab.store.domain.model.AppNotification
import app.tab.store.domain.model.Cart
import app.tab.store.domain.model.Category
import app.tab.store.domain.model.Order
import app.tab.store.domain.model.OrderStatus
import app.tab.store.domain.model.Product
import app.tab.store.domain.model.ProductStatus
import app.tab.store.domain.model.SellerStats
import app.tab.store.domain.model.Store
import app.tab.store.domain.model.User
import kotlinx.coroutines.flow.Flow

/** Result wrapper used across the data layer so the UI never sees exceptions. */
sealed interface DataResult<out T> {
    data class Success<T>(val value: T) : DataResult<T>
    data class Failure(val message: String, val cause: Throwable? = null) : DataResult<Nothing>

    fun getOrNull(): T? = (this as? Success)?.value
}

inline fun <T, R> DataResult<T>.map(transform: (T) -> R): DataResult<R> = when (this) {
    is DataResult.Success -> DataResult.Success(transform(value))
    is DataResult.Failure -> this
}

interface AuthRepository {
    val currentUser: Flow<User?>
    fun isSignedIn(): Boolean
    suspend fun signInWithEmail(email: String, password: String): DataResult<User>
    suspend fun registerWithEmail(name: String, email: String, password: String): DataResult<User>
    suspend fun signInWithGoogle(idToken: String): DataResult<User>
    suspend fun sendPasswordReset(email: String): DataResult<Unit>
    suspend fun signOut()
    suspend fun refreshProfile(): DataResult<User>
}

interface ProfileRepository {
    suspend fun updateProfile(displayName: String, phone: String?, photoUrl: String?, photoPublicId: String?): DataResult<Unit>
    suspend fun registerAsSeller(storeName: String): DataResult<String>
    fun observeProfile(): Flow<User?>
}

interface CatalogRepository {
    fun observeCategories(): Flow<List<Category>>
    suspend fun refreshCategories(): DataResult<Unit>
    fun observeFeatured(limit: Int = 10): Flow<List<Product>>
    fun observeTrending(limit: Int = 10): Flow<List<Product>>
    fun observeNewArrivals(limit: Int = 10): Flow<List<Product>>
    fun observeRecommended(limit: Int = 10): Flow<List<Product>>
    fun observeByCategory(categoryId: String, limit: Int = 20): Flow<List<Product>>
    suspend fun search(
        query: String,
        categoryId: String? = null,
        minPricePaise: Long? = null,
        maxPricePaise: Long? = null,
        sort: ProductSort = ProductSort.RELEVANCE,
        pageSize: Int = 20,
        pageToken: String? = null,
    ): DataResult<ProductPage>
    fun observeProduct(productId: String): Flow<Product?>
    suspend fun recordProductView(productId: String)
    fun observeRecentlyViewed(limit: Int = 10): Flow<List<Product>>
    fun observeStore(storeId: String): Flow<Store?>
    fun observeStoreProducts(storeId: String): Flow<List<Product>>
}

enum class ProductSort { RELEVANCE, PRICE_LOW_HIGH, PRICE_HIGH_LOW, NEWEST, TOP_RATED }

data class ProductPage(val products: List<Product>, val nextPageToken: String?)

interface CartRepository {
    fun observeCart(): Flow<Cart>
    suspend fun add(product: Product, quantity: Int = 1): DataResult<Unit>
    suspend fun setQuantity(productId: String, quantity: Int): DataResult<Unit>
    suspend fun remove(productId: String): DataResult<Unit>
    suspend fun clear(): DataResult<Unit>
    fun itemCount(): Flow<Int>
}

interface WishlistRepository {
    fun observeWishlist(): Flow<List<Product>>
    fun observeWishlistIds(): Flow<Set<String>>
    suspend fun toggle(product: Product): DataResult<Boolean>
    suspend fun remove(productId: String): DataResult<Unit>
}

interface AddressRepository {
    fun observeAddresses(): Flow<List<Address>>
    suspend fun upsert(address: Address): DataResult<String>
    suspend fun delete(addressId: String): DataResult<Unit>
    suspend fun setDefault(addressId: String): DataResult<Unit>
}

interface OrderRepository {
    fun observeBuyerOrders(): Flow<List<Order>>
    fun observeOrder(orderId: String): Flow<Order?>
    suspend fun placeOrder(addressId: String, note: String? = null): DataResult<String>
    suspend fun cancelOrder(orderId: String): DataResult<Unit>
    fun observeSellerOrders(storeId: String): Flow<List<Order>>
    suspend fun updateOrderStatus(orderId: String, status: OrderStatus): DataResult<Unit>
    fun observeAllOrders(): Flow<List<Order>>
}

interface SellerRepository {
    fun observeMyStore(): Flow<Store?>
    suspend fun createStore(name: String, tagline: String, city: String): DataResult<String>
    suspend fun updateStore(store: Store): DataResult<Unit>
    fun observeStoreProducts(storeId: String): Flow<List<Product>>
    suspend fun upsertProduct(product: Product): DataResult<String>
    suspend fun deleteProduct(productId: String): DataResult<Unit>
    suspend fun setStock(productId: String, stock: Int): DataResult<Unit>
    suspend fun setProductStatus(productId: String, status: ProductStatus): DataResult<Unit>
    fun observeStats(storeId: String): Flow<SellerStats>
}

interface AdminRepository {
    fun observeOverview(): Flow<AdminOverview>
    fun observePendingStores(): Flow<List<Store>>
    fun observeAllStores(): Flow<List<Store>>
    suspend fun setStoreStatus(storeId: String, status: app.tab.store.domain.model.StoreStatus): DataResult<Unit>
    fun observeUsers(): Flow<List<User>>
    suspend fun setUserRole(uid: String, role: app.tab.store.domain.model.UserRole): DataResult<Unit>
    fun observeReportedProducts(): Flow<List<Product>>
    suspend fun setProductStatus(productId: String, status: ProductStatus): DataResult<Unit>
    suspend fun createCategory(name: String, slug: String, iconKey: String): DataResult<Unit>
}

interface NotificationRepository {
    fun observeNotifications(): Flow<List<AppNotification>>
    fun observeUnreadCount(): Flow<Int>
    suspend fun markRead(id: String): DataResult<Unit>
    suspend fun markAllRead(): DataResult<Unit>
    suspend fun registerDeviceToken(token: String): DataResult<Unit>
}

interface MediaRepository {
    /** Uploads bytes straight to Cloudinary using a server-signed payload. */
    suspend fun upload(
        bytes: ByteArray,
        fileName: String,
        folder: String,
        onProgress: (Float) -> Unit = {},
    ): DataResult<UploadedAsset>

    suspend fun delete(publicId: String): DataResult<Unit>
}

data class UploadedAsset(val url: String, val publicId: String, val width: Int, val height: Int, val bytes: Long)
