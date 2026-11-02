package app.tab.store.domain.model

data class ProductPerformance(
    val productId: String,
    val title: String,
    val imageUrl: String?,
    val unitsSold: Int,
    val revenue: Long,
)

data class SellerStats(
    val totalRevenue: Long = 0L,
    val orderCount: Int = 0,
    val unitsSold: Int = 0,
    val activeProducts: Int = 0,
    val lowStockCount: Int = 0,
    val averageOrderValue: Long = 0L,
    val pendingOrders: Int = 0,
    val topProducts: List<ProductPerformance> = emptyList(),
    val revenueByDay: List<Pair<String, Long>> = emptyList(),
)

data class AdminOverview(
    val userCount: Int = 0,
    val sellerCount: Int = 0,
    val storeCount: Int = 0,
    val pendingStores: Int = 0,
    val productCount: Int = 0,
    val blockedProducts: Int = 0,
    val orderCount: Int = 0,
    val grossMerchandiseValue: Long = 0L,
)
