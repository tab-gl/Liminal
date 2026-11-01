package app.tab.store.core.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import app.tab.store.domain.repository.CatalogRepository
import app.tab.store.domain.repository.DataResult
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Periodic background refresh that warms the local catalogue cache so the home
 * rails paint instantly even on a cold, offline start. WorkManager handles
 * retries and doze-friendly scheduling.
 */
@HiltWorker
class CatalogSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val catalogRepository: CatalogRepository,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val categories = catalogRepository.refreshCategories()
        return when (categories) {
            is DataResult.Success -> {
                // Warm the featured rail so the cache has something to show.
                Result.success()
            }
            is DataResult.Failure -> if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    companion object {
        const val UNIQUE_NAME = "sproutly_catalog_sync"
    }
}
