package app.tab.store.core.updates

import app.tab.store.domain.repository.CatalogRepository
import app.tab.store.domain.repository.NotificationRepository
import app.tab.store.domain.repository.OrderRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Tells people when an app they installed has a newer build.
 *
 * The install records the version that was installed, so "an update exists" is a
 * comparison rather than a guess. The notice is written with a document id that
 * includes the version, which makes it idempotent: opening the app twice does not
 * produce two notifications for the same release.
 */
@Singleton
class UpdateNotifier @Inject constructor(
    private val orderRepository: OrderRepository,
    private val catalogRepository: CatalogRepository,
    private val notificationRepository: NotificationRepository,
) {
    suspend fun check() {
        val installed = runCatching {
            orderRepository.observeBuyerOrders().first()
        }.getOrNull().orEmpty()

        val seen = mutableSetOf<String>()
        installed.forEach { order ->
            order.items.forEach { item ->
                if (!seen.add(item.productId)) return@forEach

                val current = runCatching {
                    catalogRepository.observeProduct(item.productId).first()
                }.getOrNull() ?: return@forEach

                if (current.versionCode > item.versionCode && current.status.name == "ACTIVE") {
                    val version = current.versionName.ifBlank { current.versionCode.toString() }
                    notificationRepository.push(
                        id = "update_${current.id}_${current.versionCode}",
                        type = "UPDATE",
                        title = "${current.title} has an update",
                        body = "Version $version is ready" +
                            if (current.releaseNotes.isNotBlank()) {
                                " — ${current.releaseNotes.take(80)}"
                            } else {
                                "."
                            },
                        deepLink = "product/${current.id}",
                    )
                }
            }
        }
    }
}
