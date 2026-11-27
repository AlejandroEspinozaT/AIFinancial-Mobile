package com.aifinancial.mobile.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.aifinancial.mobile.data.repository.DocumentSyncRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Deliberately thin — see [DocumentSyncRepository.drainQueue]'s KDoc. This
 * class exists only to satisfy WorkManager's API; it holds no decision logic
 * of its own, matching this project's "no business logic in the platform
 * wiring layer" rule (specs/architecture/overview.md), already applied to
 * controllers on the backend and now to this worker here.
 */
@HiltWorker
class DocumentSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val repository: DocumentSyncRepository,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val anyStillQueued = repository.drainQueue()
        return if (anyStillQueued) Result.retry() else Result.success()
    }
}
