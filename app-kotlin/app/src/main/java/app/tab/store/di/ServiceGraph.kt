package app.tab.store.di

import app.tab.store.data.repo.AdminRepository
import app.tab.store.data.repo.ApplicationRepository
import app.tab.store.data.repo.AuthRepository
import app.tab.store.data.repo.BookmarkRepository
import app.tab.store.data.repo.CampaignRepository
import app.tab.store.data.repo.MediaRepository
import app.tab.store.data.repo.NotificationRepository
import app.tab.store.data.repo.ProfileRepository
import app.tab.store.data.repo.PublishingRepository
import app.tab.store.data.repo.ReviewRepository

/**
 * A hand-rolled object graph.
 *
 * Deliberately not Hilt: the app has nine repositories and no scopes worth
 * annotating, and skipping the annotation processor keeps the build fast. Swapping
 * in Hilt later means annotating these constructors and deleting this file.
 */
object ServiceGraph {
    val auth: AuthRepository by lazy { AuthRepository() }
    val profile: ProfileRepository by lazy { ProfileRepository() }
    val applications: ApplicationRepository by lazy { ApplicationRepository() }
    val bookmarks: BookmarkRepository by lazy { BookmarkRepository() }
    val notifications: NotificationRepository by lazy { NotificationRepository() }
    val publishing: PublishingRepository by lazy { PublishingRepository() }
    val media: MediaRepository by lazy { MediaRepository() }
    val reviews: ReviewRepository by lazy { ReviewRepository() }
    val campaigns: CampaignRepository by lazy { CampaignRepository() }
    val admin: AdminRepository by lazy { AdminRepository() }
}
