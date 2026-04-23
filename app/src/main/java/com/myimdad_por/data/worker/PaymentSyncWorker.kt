package com.myimdad_por.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.myimdad_por.core.network.ApiException
import com.myimdad_por.core.network.NetworkUnavailableException
import com.myimdad_por.core.network.RequestTimeoutException
import com.myimdad_por.data.local.dao.PaymentDao
import com.myimdad_por.data.local.dao.PendingSyncDao
import com.myimdad_por.data.local.entity.PendingSyncEntity
import com.myimdad_por.data.local.entity.PendingSyncOperation
import com.myimdad_por.data.remote.datasource.PaymentRemoteDataSource
import com.myimdad_por.data.remote.dto.PaymentDto
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit
import kotlin.random.Random

@HiltWorker
class PaymentSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParameters: WorkerParameters,
    private val pendingSyncDao: PendingSyncDao,
    private val paymentDao: PaymentDao,
    private val paymentRemoteDataSource: PaymentRemoteDataSource
) : CoroutineWorker(appContext, workerParameters) {

    private val gson = Gson()

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            releaseStaleLocks()

            val readyItems = pendingSyncDao
                .observeReadyToProcess(System.currentTimeMillis())
                .first()
                .asSequence()
                .filter { it.entityType.equals(ENTITY_TYPE_PAYMENT, ignoreCase = true) }
                .take(MAX_ITEMS_PER_RUN)
                .toList()

            if (readyItems.isEmpty()) {
                return@withContext Result.success()
            }

            var retryRequested = false
            var processedCount = 0

            for (item in readyItems) {
                if (isStopped) break

                when (processItem(item)) {
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
            if (throwable is CancellationException) throw throwable

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

        val freshItem = pendingSyncDao.getById(item.id)
            ?: run {
                pendingSyncDao.markFailed(
                    id = item.id,
                    errorMessage = "لم يتم العثور على عنصر المزامنة بعد القفل"
                )
                return ItemOutcome.Ignored
            }

        val operation = freshItem.resolveOperation()
        val dto = freshItem.decodePaymentDtoOrNull()

        if (operation != PendingSyncOperation.DELETE && dto == null) {
            pendingSyncDao.markFailed(
                id = freshItem.id,
                errorMessage = "payload_json غير صالح أو لا يطابق PaymentDto"
            )
            return ItemOutcome.Ignored
        }

        return when (val remoteOutcome = executeRemoteOperation(freshItem, operation, dto)) {
            RemoteOutcome.Success -> {
                val localUpdated = applyLocalPaymentState(freshItem, operation, dto)
                if (localUpdated) {
                    pendingSyncDao.markCompleted(freshItem.id)
                    ItemOutcome.Completed
                } else {
                    ItemOutcome.Ignored
                }
            }

            RemoteOutcome.Retry -> {
                pendingSyncDao.markRetry(
                    id = freshItem.id,
                    nextAttemptAtMillis = computeNextAttemptAtMillis(freshItem.attemptCount + 1),
                    errorMessage = freshItem.lastErrorMessage ?: "تعذر مزامنة الدفعة الآن"
                )
                ItemOutcome.Retry
            }

            RemoteOutcome.Failure -> {
                pendingSyncDao.markFailed(
                    id = freshItem.id,
                    errorMessage = freshItem.lastErrorMessage ?: "فشلت مزامنة الدفعة"
                )
                ItemOutcome.Completed
            }
        }
    }

    private suspend fun executeRemoteOperation(
        item: PendingSyncEntity,
        operation: PendingSyncOperation,
        dto: PaymentDto?
    ): RemoteOutcome {
        return try {
            when (operation) {
                PendingSyncOperation.CREATE -> {
                    val request = dto ?: return RemoteOutcome.Failure
                    paymentRemoteDataSource.createPayment(request).toRemoteOutcome()
                }

                PendingSyncOperation.UPDATE -> {
                    val request = dto ?: return RemoteOutcome.Failure
                    val remoteId = request.remoteIdOrFallback(item.entityId)
                    if (remoteId.isBlank()) return RemoteOutcome.Failure
                    paymentRemoteDataSource.updatePayment(remoteId, request).toRemoteOutcome()
                }

                PendingSyncOperation.DELETE -> {
                    val remoteId = dto?.remoteIdOrFallback(item.entityId)
                        ?.takeIf { it.isNotBlank() }
                        ?: item.entityId.takeIf { it.isNotBlank() }
                        ?: return RemoteOutcome.Failure

                    paymentRemoteDataSource.deletePayment(remoteId).toRemoteOutcome()
                }

                PendingSyncOperation.UPSERT,
                PendingSyncOperation.REPAIR -> {
                    val request = dto ?: return RemoteOutcome.Failure
                    val remoteId = request.remoteIdOrFallback(item.entityId)
                    if (remoteId.isBlank()) {
                        paymentRemoteDataSource.createPayment(request).toRemoteOutcome()
                    } else {
                        paymentRemoteDataSource.updatePayment(remoteId, request).toRemoteOutcome()
                    }
                }
            }
        } catch (throwable: Throwable) {
            if (throwable is CancellationException) throw throwable

            when (throwable.toApiException()) {
                is NetworkUnavailableException,
                is RequestTimeoutException -> RemoteOutcome.Retry

                else -> RemoteOutcome.Failure
            }
        }
    }

    private suspend fun applyLocalPaymentState(
        item: PendingSyncEntity,
        operation: PendingSyncOperation,
        dto: PaymentDto?
    ): Boolean {
        val paymentId = resolveLocalPaymentId(item, dto)
        if (paymentId.isBlank()) {
            pendingSyncDao.markFailed(
                id = item.id,
                errorMessage = "تعذر تحديد المعرّف المحلي للدفعة"
            )
            return false
        }

        return when (operation) {
            PendingSyncOperation.DELETE -> {
                val affected = paymentDao.softDelete(
                    id = paymentId,
                    syncState = SYNCED_STATE
                )
                if (affected > 0) {
                    true
                } else {
                    pendingSyncDao.markFailed(
                        id = item.id,
                        errorMessage = "لم يتم العثور على الدفعة المحلية لحذفها"
                    )
                    false
                }
            }

            else -> {
                val synced = paymentDao.markSynced(
                    id = paymentId,
                    syncState = SYNCED_STATE
                )

                if (synced <= 0) {
                    pendingSyncDao.markFailed(
                        id = item.id,
                        errorMessage = "لم يتم العثور على الدفعة المحلية لتحديثها"
                    )
                    return false
                }

                dto?.status?.trim()
                    ?.takeIf { it.isNotBlank() }
                    ?.let { status ->
                        paymentDao.updateStatus(
                            id = paymentId,
                            status = status,
                            syncState = SYNCED_STATE
                        )
                    }

                paymentDao.updateProviderSnapshot(
                    id = paymentId,
                    providerName = dto?.providerName,
                    providerReference = dto?.providerReference,
                    receiptNumber = dto?.receiptNumber,
                    syncState = SYNCED_STATE
                )

                true
            }
        }
    }

    private fun PendingSyncEntity.decodePaymentDtoOrNull(): PaymentDto? {
        return try {
            gson.fromJson(payloadJson, PaymentDto::class.java)
        } catch (_: JsonSyntaxException) {
            null
        } catch (_: CancellationException) {
            throw CancellationException("Cancelled while decoding payload")
        } catch (_: Throwable) {
            null
        }
    }

    private fun PendingSyncEntity.resolveOperation(): PendingSyncOperation {
        return runCatching {
            PendingSyncOperation.valueOf(operation.trim().uppercase())
        }.getOrDefault(PendingSyncOperation.REPAIR)
    }

    private fun PaymentDto.remoteIdOrFallback(fallbackId: String): String {
        return recordId?.trim().takeIf { !it.isNullOrBlank() }
            ?: transactionId?.trim().takeIf { !it.isNullOrBlank() }
            ?: fallbackId.trim()
    }

    private fun resolveLocalPaymentId(item: PendingSyncEntity, dto: PaymentDto?): String {
        return dto?.transactionId?.trim().takeIf { !it.isNullOrBlank() }
            ?: dto?.recordId?.trim().takeIf { !it.isNullOrBlank() }
            ?: item.entityId.trim()
    }

    private suspend fun releaseStaleLocks() {
        val thresholdMillis = System.currentTimeMillis() - STALE_LOCK_TIMEOUT_MILLIS
        pendingSyncDao.releaseStaleLocks(thresholdMillis = thresholdMillis)
    }

    private fun computeNextAttemptAtMillis(attemptNumber: Int): Long {
        val safeAttempt = attemptNumber.coerceAtLeast(1)
        val exponent = (safeAttempt - 1).coerceAtMost(10)
        val exponentialDelay = BASE_RETRY_DELAY_MILLIS * (1L shl exponent)
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

    private fun <T> com.myimdad_por.core.network.NetworkResult<T>.toRemoteOutcome(): RemoteOutcome {
        return when (this) {
            is com.myimdad_por.core.network.NetworkResult.Success -> RemoteOutcome.Success
            is com.myimdad_por.core.network.NetworkResult.Error -> {
                if (shouldRetry(exception)) RemoteOutcome.Retry else RemoteOutcome.Failure
            }
            com.myimdad_por.core.network.NetworkResult.Loading -> RemoteOutcome.Retry
        }
    }

    private fun shouldRetry(exception: ApiException): Boolean {
        val code = exception.code
        return when {
            exception is NetworkUnavailableException -> true
            exception is RequestTimeoutException -> true
            code == 408 -> true
            code == 429 -> true
            code != null && code in 500..599 -> true
            else -> false
        }
    }

    private sealed class RemoteOutcome {
        data object Success : RemoteOutcome()
        data object Retry : RemoteOutcome()
        data object Failure : RemoteOutcome()
    }

    private sealed class ItemOutcome {
        data object Completed : ItemOutcome()
        data object Retry : ItemOutcome()
        data object Ignored : ItemOutcome()
    }

    companion object {
        private const val ENTITY_TYPE_PAYMENT = "PAYMENT"
        private const val SYNCED_STATE = "SYNCED"

        private const val MAX_ITEMS_PER_RUN = 20

        private val BASE_RETRY_DELAY_MILLIS = TimeUnit.SECONDS.toMillis(25)
        private val MAX_RETRY_DELAY_MILLIS = TimeUnit.MINUTES.toMillis(20)
        private val STALE_LOCK_TIMEOUT_MILLIS = TimeUnit.MINUTES.toMillis(15)
        private const val JITTER_MAX_MILLIS = 2_500L
    }
}