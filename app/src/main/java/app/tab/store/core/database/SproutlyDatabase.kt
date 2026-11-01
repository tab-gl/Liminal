package app.tab.store.core.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [RecentlyViewedEntity::class, CachedProductEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class SproutlyDatabase : RoomDatabase() {
    abstract fun recentlyViewedDao(): RecentlyViewedDao
    abstract fun cachedProductDao(): CachedProductDao
}
