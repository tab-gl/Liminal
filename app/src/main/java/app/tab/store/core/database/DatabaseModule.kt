package app.tab.store.core.database

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): SproutlyDatabase =
        Room.databaseBuilder(context, SproutlyDatabase::class.java, "sproutly.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideRecentlyViewedDao(db: SproutlyDatabase): RecentlyViewedDao = db.recentlyViewedDao()

    @Provides
    fun provideCachedProductDao(db: SproutlyDatabase): CachedProductDao = db.cachedProductDao()
}
