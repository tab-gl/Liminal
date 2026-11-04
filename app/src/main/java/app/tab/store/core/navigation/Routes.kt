package app.tab.store.core.navigation

/** Single source of truth for every destination in Sproutly. */
object Routes {
    // Auth
    const val LOGIN = "auth/login"
    const val REGISTER = "auth/register"
    const val FORGOT_PASSWORD = "auth/forgot"

    // Customer tabs
    const val HOME = "home"
    const val SEARCH = "search"
    const val GAMES = "games"
    const val OFFERS = "offers"
    const val RANKINGS = "rankings"
    const val LEGAL = "legal?terms={terms}"

    fun legal(terms: Boolean = false) = "legal?terms=$terms"
    const val ORDERS = "orders"
    const val PROFILE = "profile"

    // Customer detail destinations
    const val PRODUCT = "product/{productId}"
    const val STORE = "store/{storeId}"
    const val ORDER_DETAIL = "order/{orderId}"
    const val ORDER_PLACED = "order-placed/{orderId}"
    const val WISHLIST = "wishlist"
    const val PROFILE_EDIT = "profile-edit"
    const val NOTIFICATIONS = "notifications"
    const val SETTINGS = "settings"

    // Seller
    const val SELLER_SETUP = "seller/setup"
    const val SELLER_DASHBOARD = "seller/dashboard"
    const val SELLER_PRODUCTS = "seller/products"
    const val SELLER_PRODUCT_EDIT = "seller/product-edit?productId={productId}"
    const val SELLER_ORDERS = "seller/orders"
    const val SELLER_ANALYTICS = "seller/analytics"
    const val SELLER_STORE_EDIT = "seller/store-edit"

    // Admin
    const val ADMIN_DASHBOARD = "admin/dashboard"
    const val ADMIN_STORES = "admin/stores"
    const val ADMIN_USERS = "admin/users"
    const val ADMIN_PRODUCTS = "admin/products"
    const val ADMIN_CATEGORIES = "admin/categories"

    fun product(id: String) = "product/$id"
    fun store(id: String) = "store/$id"
    fun orderDetail(id: String) = "order/$id"
    fun orderPlaced(id: String) = "order-placed/$id"
    fun sellerProductEdit(id: String?) = "seller/product-edit?productId=${id ?: ""}"
    fun searchInCategory(categoryId: String) = "search?categoryId=$categoryId"
}
