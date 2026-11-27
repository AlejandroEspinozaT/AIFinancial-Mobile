package com.aifinancial.mobile

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.aifinancial.mobile.sync.ReadCacheSyncScheduler
import com.aifinancial.mobile.sync.SyncScheduler
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class AiFinancialApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    // Catches any PendingDocument left QUEUED from a previous process —
    // normally redundant with the enqueue-on-capture call in
    // CaptureViewModel, since WorkManager persists that request across
    // process death too, but cheap enough to keep as a second, independent
    // path to the same end state rather than relying on exactly one trigger.
    @Inject
    lateinit var syncScheduler: SyncScheduler

    @Inject
    lateinit var readCacheSyncScheduler: ReadCacheSyncScheduler

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    override fun onCreate() {
        super.onCreate()
        syncScheduler.scheduleSync()
        readCacheSyncScheduler.schedulePeriodic()
    }
}
