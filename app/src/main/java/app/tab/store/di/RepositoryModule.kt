package app.tab.store.di

import app.tab.store.core.media.CloudinaryUploader
import app.tab.store.data.repository.AppLibraryRepositoryImpl
import app.tab.store.data.repository.AdminRepositoryImpl
import app.tab.store.data.repository.AuthRepositoryImpl
import app.tab.store.data.repository.CatalogRepositoryImpl
import app.tab.store.data.repository.NotificationRepositoryImpl
import app.tab.store.data.repository.OrderRepositoryImpl
import app.tab.store.data.repository.ProfileRepositoryImpl
import app.tab.store.data.repository.SellerRepositoryImpl
import app.tab.store.data.repository.WishlistRepositoryImpl
import app.tab.store.domain.repository.AppLibraryRepository
import app.tab.store.domain.repository.AdminRepository
import app.tab.store.domain.repository.AuthRepository
import app.tab.store.domain.repository.CatalogRepository
import app.tab.store.domain.repository.MediaRepository
import app.tab.store.domain.repository.NotificationRepository
import app.tab.store.domain.repository.OrderRepository
import app.tab.store.domain.repository.ProfileRepository
import app.tab.store.domain.repository.SellerRepository
import app.tab.store.domain.repository.WishlistRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds @Singleton
    abstract fun bindProfileRepository(impl: ProfileRepositoryImpl): ProfileRepository

    @Binds @Singleton
    abstract fun bindCatalogRepository(impl: CatalogRepositoryImpl): CatalogRepository

    @Binds @Singleton
    abstract fun bindWishlistRepository(impl: WishlistRepositoryImpl): WishlistRepository

    @Binds @Singleton
    abstract fun bindAppLibraryRepository(impl: AppLibraryRepositoryImpl): AppLibraryRepository

    @Binds @Singleton
    abstract fun bindOrderRepository(impl: OrderRepositoryImpl): OrderRepository

    @Binds @Singleton
    abstract fun bindSellerRepository(impl: SellerRepositoryImpl): SellerRepository

    @Binds @Singleton
    abstract fun bindAdminRepository(impl: AdminRepositoryImpl): AdminRepository

    @Binds @Singleton
    abstract fun bindNotificationRepository(impl: NotificationRepositoryImpl): NotificationRepository

    @Binds @Singleton
    abstract fun bindMediaRepository(impl: CloudinaryUploader): MediaRepository
}
