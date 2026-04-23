package com.myimdad_por.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.myimdad_por.core.network.ApiException
import com.myimdad_por.core.network.NetworkUnavailableException
import com.myimdad_por.core.network.RequestTimeoutException
import com.myimdad_por.data.local.dao.PendingSyncDao
import com.myimdad_por.data.local.entity.PendingSyncEntity
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val pendingSyncDao: PendingSyncDao,
    private val syncProcessor: SyncProcessor
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            releaseStaleLocks()

            val readyItems = pendingSyncDao
                .observeReadyToProcess(System.currentTimeMillis())
                .first()
                .take(MAX_ITEMS_PER_RUN)

            if (readyItems.isEmpty()) {
                return@withContext Result.success()
            }

            var hadRetryableFailure = false
            var processedCount = 0

            for (item in readyItems) {
                if (!ensureStillActive()) break

                val locked = pendingSyncDao.markInProgress(item.id)
                if (locked <= 0) continue

                val latestItem = pendingSyncDao.getById(item.id) ?: continue

                when (val result = syncProcessor.process(latestItem)) {
                    is SyncOutcome.Success -> {
                        pendingSyncDao.markCompleted(latestItem.id)
                        processedCount++
                    }

                    is SyncOutcome.Retry -> {
                        hadRetryableFailure = true
                        pendingSyncDao.markRetry(
                            id = latestItem.id,
                            nextAttemptAtMillis = result.nextAttemptAtMillis ?: computeNextAttemptMillis(latestItem.attemptCount + 1),
                            errorMessage = result.message
                        )
                    }

                    is SyncOutcome.Failure -> {
                        pendingSyncDao.markFailed(
                            id = latestItem.id,
                            errorMessage = result.message
                        )
                    }
                }
            }

            when {
                hadRetryableFailure -> Result.retry()
                processedCount > 0 -> Result.success()
                else -> Result.success()
            }
        } catch (t: Throwable) {
            when (t.toApiException()) {
                is NetworkUnavailableException,
                is RequestTimeoutException -> Result.retry()

                else -> Result.failure()
            }
        }
    }

    private suspend fun releaseStaleLocks() {
        val threshold = System.currentTimeMillis() - STALE_LOCK_TIMEOUT_MILLIS
        pendingSyncDao.releaseStaleLocks(thresholdMillis = threshold)
    }

    private fun computeNextAttemptMillis(attemptCount: Int): Long {
        val safeAttempt = attemptCount.coerceAtLeast(1)
        val backoff = BASE_RETRY_DELAY_MILLIS shl (safeAttempt - 1)
        return System.currentTimeMillis() + backoff.coerceAtMost(MAX_RETRY_DELAY_MILLIS)
    }

    private fun ensureStillActive(): Boolean {
        return !isStopped
    }

    private fun Throwable.toApiException(): ApiException {
        return when (this) {
            is ApiException -> this
            is SocketTimeoutException -> RequestTimeoutException(cause = this)
            is UnknownHostException -> NetworkUnavailableException(cause = this)
            else -> ApiException.unexpected(
                message = message ?: "حدث خطأ غير متوقع",
                cause = this
            )
        }
    }

    companion object {
        private const val MAX_ITEMS_PER_RUN = 20
        private val BASE_RETRY_DELAY_MILLIS = TimeUnit.SECONDS.toMillis(30)
        private val MAX_RETRY_DELAY_MILLIS = TimeUnit.MINUTES.toMillis(30)
        private val STALE_LOCK_TIMEOUT_MILLIS = TimeUnit.MINUTES.toMillis(15)
    }
}

fun interface SyncProcessor {
    suspend fun process(item: PendingSyncEntity): SyncOutcome
}

sealed class SyncOutcome {
    data object Success : SyncOutcome()

    data class Retry(
        val message: String? = null,
        val nextAttemptAtMillis: Long? = null
    ) : SyncOutcome()

    data class Failure(
        val message: String? = null
    ) : SyncOutcome()
}