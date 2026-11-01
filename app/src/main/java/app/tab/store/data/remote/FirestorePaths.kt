package app.tab.store.data.remote

/** Every Firestore path in Sproutly lives here — one place to audit. */
object FirestorePaths {
    const val USERS = "users"
    const val STORES = "stores"
    const val PRODUCTS = "products"
    const val CATEGORIES = "categories"
    const val ORDERS = "orders"
    const val CARTS = "carts"
    const val WISHLISTS = "wishlists"
    const val ADDRESSES = "addresses"
    const val NOTIFICATIONS = "notifications"
    const val TOKENS = "tokens"
    const val LINES = "lines"
    const val ITEMS = "items"
    const val PLATFORM = "platform"
    const val CONFIG = "config"
    const val STATS = "stats"

    fun user(uid: String) = "$USERS/$uid"
    fun cart(uid: String) = "$CARTS/$uid"
    fun cartLines(uid: String) = "$CARTS/$uid/$LINES"
    fun wishlistItems(uid: String) = "$WISHLISTS/$uid/$ITEMS"
    fun addressItems(uid: String) = "$ADDRESSES/$uid/$ITEMS"
    fun notificationItems(uid: String) = "$NOTIFICATIONS/$uid/$ITEMS"
    fun deviceTokens(uid: String) = "$USERS/$uid/$TOKENS"
    fun platformStats() = "$PLATFORM/$STATS"
}
