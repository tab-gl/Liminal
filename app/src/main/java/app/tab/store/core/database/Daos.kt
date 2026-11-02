package app.tab.store.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RecentlyViewedDao {
    @Query("SELECT * FROM recently_viewed ORDER BY viewedAt DESC LIMIT :limit")
    fun observe(limit: Int): Flow<List<RecentlyViewedEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: RecentlyViewedEntity)

    @Query("DELETE FROM recently_viewed WHERE productId NOT IN (SELECT productId FROM recently_viewed ORDER BY viewedAt DESC LIMIT 50)")
    suspend fun trim()

    @Query("DELETE FROM recently_viewed")
    suspend fun clear()
}

@Dao
interface CachedProductDao {
    @Query("SELECT * FROM cached_products WHERE status = 'ACTIVE' ORDER BY cachedAt DESC LIMIT :limit")
    fun observeAll(limit: Int): Flow<List<CachedProductEntity>>

    @Query("SELECT * FROM cached_products WHERE categoryId = :categoryId AND status = 'ACTIVE' LIMIT :limit")
    fun observeByCategory(categoryId: String, limit: Int): Flow<List<CachedProductEntity>>

    @Query("SELECT * FROM cached_products WHERE productId = :productId LIMIT 1")
    fun observeById(productId: String): Flow<CachedProductEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<CachedProductEntity>)

    @Query("DELETE FROM cached_products")
    suspend fun clear()
}
