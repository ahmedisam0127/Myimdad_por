package com.myimdad_por.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.myimdad_por.core.network.ApiException
import com.myimdad_por.core.network.NetworkResult
import com.myimdad_por.core.network.NetworkUnavailableException
import com.myimdad_por.core.network.RequestTimeoutException
import com.myimdad_por.data.local.dao.PendingSyncDao
import com.myimdad_por.data.local.entity.PendingSyncEntity
import com.myimdad_por.data.local.entity.PendingSyncOperation
import com.myimdad_por.data.remote.datasource.InventoryRemoteDataSource
import com.myimdad_por.data.remote.dto.StockDto
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit
import kotlin.random.Random

@HiltWorker
class InventorySyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val pendingSyncDao: PendingSyncDao,
    private val inventoryRemoteDataSource: InventoryRemoteDataSource
) : CoroutineWorker(appContext, workerParams) {

    private val gson = Gson()

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            releaseStaleLocks()

            val readyItems = pendingSyncDao
                .observeReadyToProcess(System.currentTimeMillis())
                .first()
                .asSequence()
                .filter { it.entityType.equals(ENTITY_TYPE_STOCK, ignoreCase = true) }
                .take(MAX_ITEMS_PER_RUN)
                .toList()

            if (readyItems.isEmpty()) {
                return@withContext Result.success()
            }

            var processedCount = 0
            var retryRequested = false

            for (item in readyItems) {
                if (isStopped) break

                when (val outcome = processItem(item)) {
                    ItemOutcome.Completed -> processedCount++
                    ItemOutcome.Retry -> retryRequested = true
                    ItemOutcome.Ignored -> Unit
                }
            }

            when {
                retryRequested -> Result.retry()
                processedCount > 0 -> Result.success()
                else -> Result.success()
            }
        } catch (throwable: Throwable) {
            when (throwable.toApiException()) {
                is NetworkUnavailableException,
                is RequestTimeoutException -> Result.retry()

                else -> Result.failure()
            }
        }
    }

    private suspend fun processItem(item: PendingSyncEntity): ItemOutcome {
        val locked = pendingSyncDao.markInProgress(item.id)
        if (locked <= 0) return ItemOutcome.Ignored

        val freshItem = pendingSyncDao.getById(item.id) ?: return ItemOutcome.Ignored
        val dto = freshItem.decodeStockDtoOrNull()

        if (dto == null) {
            pendingSyncDao.markFailed(
                id = freshItem.id,
                errorMessage = "payload_json غير صالح أو لا يطابق StockDto"
            )
            return ItemOutcome.Ignored
        }

        val operation = freshItem.resolveOperation()
        val remoteResult = executeRemoteOperation(operation, dto)

        return when (remoteResult) {
            RemoteAction.Success -> {
                pendingSyncDao.markCompleted(freshItem.id)
                ItemOutcome.Completed
            }

            RemoteAction.Retry -> {
                val nextAttemptAt = computeNextAttemptAtMillis(freshItem.attemptCount + 1)
                pendingSyncDao.markRetry(
                    id = freshItem.id,
                    nextAttemptAtMillis = nextAttemptAt,
                    errorMessage = freshItem.lastErrorMessage ?: "تعذر تنفيذ المزامنة"
                )
                ItemOutcome.Retry
            }

            RemoteAction.Failure -> {
                pendingSyncDao.markFailed(
                    id = freshItem.id,
                    errorMessage = freshItem.lastErrorMessage ?: "فشلت المزامنة"
                )
                ItemOutcome.Completed
            }
        }
    }

    private suspend fun executeRemoteOperation(
        operation: PendingSyncOperation,
        dto: StockDto
    ): RemoteAction {
        return when (operation) {
            PendingSyncOperation.CREATE -> {
                inventoryRemoteDataSource
                    .createStockEntry(dto)
                    .toRemoteAction()
            }

            PendingSyncOperation.UPDATE -> {
                val id = dto.syncIdentityOrNull() ?: return RemoteAction.Failure
                inventoryRemoteDataSource
                    .updateStockEntry(id, dto)
                    .toRemoteAction()
            }

            PendingSyncOperation.DELETE -> {
                val id = dto.syncIdentityOrNull() ?: return RemoteAction.Failure
                inventoryRemoteDataSource
                    .deleteStockEntry(id)
                    .toRemoteAction()
            }

            PendingSyncOperation.UPSERT -> {
                val id = dto.syncIdentityOrNull()
                if (id == null) {
                    inventoryRemoteDataSource.createStockEntry(dto).toRemoteAction()
                } else {
                    inventoryRemoteDataSource.updateStockEntry(id, dto).toRemoteAction()
                }
            }

            PendingSyncOperation.REPAIR -> {
                val id = dto.syncIdentityOrNull()
                if (id == null) {
                    inventoryRemoteDataSource.createStockEntry(dto).toRemoteAction()
                } else {
                    inventoryRemoteDataSource.updateStockEntry(id, dto).toRemoteAction()
                }
            }
        }
    }

    private fun PendingSyncEntity.decodeStockDtoOrNull(): StockDto? {
        return try {
            gson.fromJson(payloadJson, StockDto::class.java)
        } catch (_: JsonSyntaxException) {
            null
        } catch (_: Throwable) {
            null
        }
    }

    private fun PendingSyncEntity.resolveOperation(): PendingSyncOperation {
        return runCatching {
            PendingSyncOperation.valueOf(operation.trim().uppercase())
        }.getOrDefault(PendingSyncOperation.REPAIR)
    }

    private fun StockDto.syncIdentityOrNull(): String? {
        return when {
            serverId.isNullOrBlank().not() -> serverId
            stockId.isNotBlank() -> stockId
            else -> null
        }
    }

    private suspend fun releaseStaleLocks() {
        val thresholdMillis = System.currentTimeMillis() - STALE_LOCK_TIMEOUT_MILLIS
        pendingSyncDao.releaseStaleLocks(thresholdMillis = thresholdMillis)
    }

    private fun computeNextAttemptAtMillis(attemptNumber: Int): Long {
        val safeAttempt = attemptNumber.coerceAtLeast(1)
        val exponentialDelay = BASE_RETRY_DELAY_MILLIS shl (safeAttempt - 1)
        val cappedDelay = exponentialDelay.coerceAtMost(MAX_RETRY_DELAY_MILLIS)
        val jitter = Random.nextLong(0, JITTER_MAX_MILLIS + 1)
        return System.currentTimeMillis() + cappedDelay + jitter
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

    private fun <T> NetworkResult<T>.toRemoteAction(): RemoteAction {
        return when (this) {
            is NetworkResult.Success -> RemoteAction.Success
            is NetworkResult.Error -> when {
                shouldRetry(exception) -> RemoteAction.Retry
                else -> RemoteAction.Failure
            }
            NetworkResult.Loading -> RemoteAction.Retry
        }
    }

    private fun shouldRetry(exception: ApiException): Boolean {
        val code = exception.code
        return when {
            code == 408 -> true
            code == 429 -> true
            code != null && code in 500..599 -> true
            exception is NetworkUnavailableException -> true
            exception is RequestTimeoutException -> true
            else -> false
        }
    }

    private sealed class RemoteAction {
        data object Success : RemoteAction()
        data object Retry : RemoteAction()
        data object Failure : RemoteAction()
    }

    private sealed class ItemOutcome {
        data object Completed : ItemOutcome()
        data object Retry : ItemOutcome()
        data object Ignored : ItemOutcome()
    }

    companion object {
        private const val ENTITY_TYPE_STOCK = "STOCK"
        private const val MAX_ITEMS_PER_RUN = 25

        private val BASE_RETRY_DELAY_MILLIS = TimeUnit.SECONDS.toMillis(20)
        private val MAX_RETRY_DELAY_MILLIS = TimeUnit.MINUTES.toMillis(20)
        private val STALE_LOCK_TIMEOUT_MILLIS = TimeUnit.MINUTES.toMillis(15)
        private const val JITTER_MAX_MILLIS = 2_500L
    }
}