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
                    imageUrl = current.primaryImage,
                )
            }
        }

        checkIn()
    }

    /**
     * One nudge a day, and no more.
     *
     * The id is the date, so writing it twice in a day is a no-op and yesterday's
     * is a different document. It carries the newest listing's art, which is the
     * point of a check-in: something to look at, not a bell.
     */
    private suspend fun checkIn() {
        val today = java.time.LocalDate.now().toString()
        val newest = runCatching {
            catalogRepository.observeCatalogue(20).first()
                .sortedByDescending { it.createdAt }
                .firstOrNull()
        }.getOrNull()

        notificationRepository.push(
            id = "checkin_$today",
            type = "CHECK_IN",
            title = if (newest != null) "Something new since yesterday" else "Today on Liminal",
            body = newest?.let {
                "${it.title} from ${it.storeName.ifBlank { "an independent developer" }}."
            } ?: "Have a look at what was published this week.",
            deepLink = newest?.let { "product/${it.id}" } ?: "home",
            imageUrl = newest?.primaryImage,
        )
    }
}
