package app.tab.store.domain.model

data class CartLine(
    val productId: String,
    val storeId: String,
    val storeName: String,
    val title: String,
    val imageUrl: String?,
    val unitPrice: Long,
    val quantity: Int,
    val stock: Int,
    val status: ProductStatus = ProductStatus.ACTIVE,
) {
    val lineTotal: Long get() = unitPrice * quantity
    val isAvailable: Boolean get() = status == ProductStatus.ACTIVE && stock > 0
    val exceedsStock: Boolean get() = quantity > stock
}

data class Cart(
    val lines: List<CartLine> = emptyList(),
) {
    val itemCount: Int get() = lines.sumOf { it.quantity }
    val subtotal: Long get() = lines.filter { it.isAvailable }.sumOf { it.lineTotal }
    val isEmpty: Boolean get() = lines.isEmpty()
    val hasUnavailable: Boolean get() = lines.any { !it.isAvailable || it.exceedsStock }
    val groupedByStore: Map<String, List<CartLine>>
        get() = lines.filter { it.isAvailable }.groupBy { it.storeId }
}
