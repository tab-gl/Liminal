package app.tab.store.core.updates

import app.tab.store.domain.repository.AppLibraryRepository
import app.tab.store.domain.repository.CatalogRepository
import app.tab.store.domain.repository.NotificationRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UpdateNotifier @Inject constructor(
    private val appLibraryRepository: AppLibraryRepository,
    private val catalogRepository: CatalogRepository,
    private val notificationRepository: NotificationRepository,
) {
    suspend fun check() {
        val installed = runCatching { appLibraryRepository.observeMyApps().first() }
            .getOrNull().orEmpty()
        installed.forEach { app ->
            val current = runCatching { catalogRepository.observeProduct(app.productId).first() }
                .getOrNull() ?: return@forEach
            if (current.versionCode > app.versionCode && current.status.name == "ACTIVE") {
                val version = current.versionName.ifBlank { current.versionCode.toString() }
                notificationRepository.push(
                    id = "update_${current.id}_${current.versionCode}",
                    type = "UPDATE",
                    title = "${current.title} has an update",
                    body = "Version $version is ready" + if (current.releaseNotes.isNotBlank()) {
                        " · ${current.releaseNotes.take(80)}"
                    } else ".",
                    deepLink = "product/${current.id}",
                )
            }
        }
    }
}
