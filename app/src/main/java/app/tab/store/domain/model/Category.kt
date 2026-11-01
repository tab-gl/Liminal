package app.tab.store.domain.model

data class Category(
    val id: String = "",
    val name: String = "",
    val slug: String = "",
    val iconKey: String = "leaf",
    val order: Int = 0,
    val productCount: Int = 0,
    val imageUrl: String? = null,
)
