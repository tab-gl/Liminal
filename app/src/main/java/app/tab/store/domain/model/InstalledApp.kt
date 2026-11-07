package app.tab.store.domain.model

data class InstalledApp(
    val productId: String = "",
    val title: String = "",
    val imageUrl: String? = null,
    val storeName: String = "",
    val versionCode: Int = 0,
    val versionName: String = "",
    val installedAt: Long = 0L,
)
