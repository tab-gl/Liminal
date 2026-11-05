package app.tab.store.domain.model

enum class OrderStatus { PENDING, CONFIRMED, PACKED, SHIPPED, DELIVERED, CANCELLED;
    companion object {
        fun from(value: String?): OrderStatus =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: PENDING
    }
}

data class OrderItem(
    val productId: String,
    val title: String,
    val imageUrl: String?,
    val unitPrice: Long,
    val quantity: Int,
    val storeId: String,
    val storeName: String,
    /** The build that was installed, so an update can be recognised later. */
    val versionCode: Int = 0,
    val versionName: String = "",
) {
    val lineTotal: Long get() = unitPrice * quantity
}

data class OrderEvent(val status: OrderStatus, val at: Long, val note: String? = null)

data class Order(
    val id: String = "",
    val buyerId: String = "",
    val buyerName: String = "",
    val items: List<OrderItem> = emptyList(),
    val subtotal: Long = 0L,
    val shipping: Long = 0L,
    val total: Long = 0L,
    val status: OrderStatus = OrderStatus.PENDING,
    val storeIds: List<String> = emptyList(),
    val placedAt: Long = 0L,
    val updatedAt: Long = 0L,
    val timeline: List<OrderEvent> = emptyList(),
) {
    val itemCount: Int get() = items.sumOf { it.quantity }
    val canCancel: Boolean get() = status == OrderStatus.PENDING || status == OrderStatus.CONFIRMED
    val primaryImage: String? get() = items.firstOrNull()?.imageUrl
}
