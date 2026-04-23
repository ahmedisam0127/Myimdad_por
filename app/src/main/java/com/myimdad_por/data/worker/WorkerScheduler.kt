package com.myimdad_por.data.worker

import android.content.Context
import androidx.annotation.VisibleForTesting
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkerScheduler @Inject constructor(
    private val context: Context
) {

    private val workManager: WorkManager
        get() = WorkManager.getInstance(context)

    fun scheduleImmediateSync(
        replacePendingWork: Boolean = true
    ) {
        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(defaultConstraints())
            .apply {
                setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            }
            .build()

        workManager.enqueueUniqueWork(
            UNIQUE_SYNC_WORK_NAME,
            if (replacePendingWork) ExistingWorkPolicy.REPLACE else ExistingWorkPolicy.KEEP,
            request
        )
    }

    fun schedulePeriodicSync(
        repeatIntervalHours: Long = DEFAULT_PERIODIC_INTERVAL_HOURS
    ) {
        val request = PeriodicWorkRequestBuilder<SyncWorker>(
            repeatIntervalHours,
            TimeUnit.HOURS
        )
            .setConstraints(defaultConstraints())
            .build()

        workManager.enqueueUniquePeriodicWork(
            UNIQUE_PERIODIC_SYNC_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun cancelSync() {
        workManager.cancelUniqueWork(UNIQUE_SYNC_WORK_NAME)
        workManager.cancelUniqueWork(UNIQUE_PERIODIC_SYNC_WORK_NAME)
    }

    fun cancelImmediateSync() {
        workManager.cancelUniqueWork(UNIQUE_SYNC_WORK_NAME)
    }

    fun cancelPeriodicSync() {
        workManager.cancelUniqueWork(UNIQUE_PERIODIC_SYNC_WORK_NAME)
    }

    fun isSyncScheduled(): Boolean {
        val infos = workManager.getWorkInfosForUniqueWork(UNIQUE_SYNC_WORK_NAME).get()
        return infos.any { it.state == WorkInfo.State.ENQUEUED || it.state == WorkInfo.State.RUNNING }
    }

    fun isPeriodicSyncScheduled(): Boolean {
        val infos = workManager.getWorkInfosForUniqueWork(UNIQUE_PERIODIC_SYNC_WORK_NAME).get()
        return infos.any { it.state == WorkInfo.State.ENQUEUED || it.state == WorkInfo.State.RUNNING }
    }

    @VisibleForTesting
    internal fun defaultConstraints(): Constraints {
        return Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
    }

    companion object {
        private const val UNIQUE_SYNC_WORK_NAME = "pending_sync_once"
        private const val UNIQUE_PERIODIC_SYNC_WORK_NAME = "pending_sync_periodic"
        private const val DEFAULT_PERIODIC_INTERVAL_HOURS = 6L
    }
}