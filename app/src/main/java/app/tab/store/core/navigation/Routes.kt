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
    const val ONBOARDING = "onboarding"
    const val LEGAL = "legal?terms={terms}"

    fun legal(terms: Boolean = false) = "legal?terms=$terms"
    const val LIBRARY = "library"
    const val PROFILE = "profile"

    // Customer detail destinations
    const val PRODUCT = "product/{productId}"
    const val STORE = "store/{storeId}"
    const val WISHLIST = "wishlist"
    const val PROFILE_EDIT = "profile-edit"
    const val NOTIFICATIONS = "notifications"
    const val SETTINGS = "settings"

    // Seller
    const val SELLER_SETUP = "seller/setup"
    const val SELLER_DASHBOARD = "seller/dashboard"
    const val SELLER_PRODUCTS = "seller/products"
    const val SELLER_PRODUCT_EDIT = "seller/product-edit?productId={productId}"
    const val SELLER_ANALYTICS = "seller/analytics"
    const val SELLER_STORE_EDIT = "seller/store-edit"

    // Admin
    const val ADMIN_DASHBOARD = "admin/dashboard"
    const val ADMIN_STORES = "admin/stores"
    const val ADMIN_USERS = "admin/users"
    const val ADMIN_PRODUCTS = "admin/products"
    const val ADMIN_CATEGORIES = "admin/categories"
    const val ADMIN_TAGS = "admin/tags"

    fun product(id: String) = "product/$id"
    fun store(id: String) = "store/$id"
    fun sellerProductEdit(id: String?) = "seller/product-edit?productId=${id ?: ""}"
    fun searchInCategory(categoryId: String) = "search?categoryId=${android.net.Uri.encode(categoryId)}"

    /** Opens search already filled in, e.g. when a tag is tapped. */
    fun search(query: String = "") =
        if (query.isBlank()) "search" else "search?query=${android.net.Uri.encode(query.trim())}"

    /** Public link for an app; the manifest claims the same pattern. */
    const val productLink = "https://sproutly.app/app/{productId}"

    fun shareLink(productId: String) = "https://sproutly.app/app/$productId"
}
