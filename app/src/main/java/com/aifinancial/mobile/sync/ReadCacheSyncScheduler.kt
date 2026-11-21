package com.aifinancial.mobile.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Covers both halves of specs/phases/stage-3-offline-read-cache.md's
 * "periodic/triggered" requirement: [scheduleImmediate] for "the user just
 * opened a screen that reads this cache, and might be online right now",
 * [schedulePeriodic] for "keep the cache reasonably fresh in the
 * background even if no screen is open". Both share the same network
 * constraint and worker as [SyncScheduler] does for the write path.
 */
@Singleton
class ReadCacheSyncScheduler @Inject constructor(@ApplicationContext private val context: Context) {

    private val networkConstraint = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

    fun scheduleImmediate() {
        val request = OneTimeWorkRequestBuilder<ReadCacheSyncWorker>()
            .setConstraints(networkConstraint)
            .build()
        WorkManager.getInstance(context)
            .enqueueUniqueWork(IMMEDIATE_WORK_NAME, ExistingWorkPolicy.KEEP, request)
    }

    fun schedulePeriodic() {
        val request = PeriodicWorkRequestBuilder<ReadCacheSyncWorker>(PERIODIC_INTERVAL_MINUTES, TimeUnit.MINUTES)
            .setConstraints(networkConstraint)
            .build()
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(PERIODIC_WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    private companion object {
        const val IMMEDIATE_WORK_NAME = "read-cache-sync-immediate"
        const val PERIODIC_WORK_NAME = "read-cache-sync-periodic"

        // WorkManager's own floor for PeriodicWorkRequest is 15 minutes;
        // twice that is a reasonable background-freshness target for data
        // that's also refreshed on-demand every time the Overview screen
        // opens (scheduleImmediate), not this cache's only path to freshness.
        const val PERIODIC_INTERVAL_MINUTES = 30L
    }
}
