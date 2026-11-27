package com.aifinancial.mobile.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The one place [DocumentSyncWorker] gets enqueued. Covers both triggers
 * specs/phases/stage-2-background-sync.md asks for with a single mechanism:
 * a network-constrained unique work request runs immediately if connectivity
 * is already present, and otherwise sits enqueued — surviving process death,
 * since WorkManager persists it — until WorkManager's own constraint
 * tracking sees connectivity return.
 */
@Singleton
class SyncScheduler @Inject constructor(@ApplicationContext private val context: Context) {

    fun scheduleSync() {
        val request = OneTimeWorkRequestBuilder<DocumentSyncWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, WorkRequest.MIN_BACKOFF_MILLIS, TimeUnit.MILLISECONDS)
            .build()

        // KEEP, not REPLACE: if a sync is already enqueued (running, or
        // waiting on connectivity/backoff), it will drain whatever is
        // QUEUED at the time it actually runs — including rows captured
        // after it was scheduled — so replacing it would only discard retry
        // backoff state for no benefit.
        WorkManager.getInstance(context)
            .enqueueUniqueWork(SYNC_WORK_NAME, ExistingWorkPolicy.KEEP, request)
    }

    private companion object {
        const val SYNC_WORK_NAME = "document-sync"
    }
}
