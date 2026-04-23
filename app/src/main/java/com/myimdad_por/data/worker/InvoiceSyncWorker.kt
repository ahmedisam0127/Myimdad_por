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
import com.myimdad_por.data.local.dao.InvoiceDao
import com.myimdad_por.data.local.dao.PendingSyncDao
import com.myimdad_por.data.local.entity.InvoiceEntity
import com.myimdad_por.data.local.entity.PendingSyncEntity
import com.myimdad_por.data.local.entity.PendingSyncOperation
import com.myimdad_por.data.remote.api.ApiService
import com.myimdad_por.data.remote.dto.SaleInvoiceDto
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import retrofit2.Response
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit
import kotlin.random.Random

@HiltWorker
class InvoiceSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val pendingSyncDao: PendingSyncDao,
    private val invoiceDao: InvoiceDao,
    private val apiService: ApiService
) : CoroutineWorker(appContext, workerParams) {

    private val gson = Gson()

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            releaseStaleLocks()

            val readyItems = pendingSyncDao
                .observeReadyToProcess(System.currentTimeMillis())
                .first()
                .asSequence()
                .filter { it.entityType.equals(ENTITY_TYPE_INVOICE, ignoreCase = true) }
                .take(MAX_ITEMS_PER_RUN)
                .toList()

            if (readyItems.isEmpty()) {
                return@withContext Result.success()
            }

            var retryRequired = false
            var processedCount = 0

            for (item in readyItems) {
                if (isStopped) break

                when (val outcome = processItem(item)) {
                    ItemOutcome.Completed -> processedCount++
                    ItemOutcome.Retry -> retryRequired = true
                    ItemOutcome.Ignored -> Unit
                }
            }

            when {
                retryRequired -> Result.retry()
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
        val operation = freshItem.resolveOperation()

        return when (val remoteResult = executeRemoteOperation(freshItem, operation)) {
            RemoteOutcome.Success -> {
                applyLocalSyncState(freshItem, operation)
                pendingSyncDao.markCompleted(freshItem.id)
                ItemOutcome.Completed
            }

            RemoteOutcome.Retry -> {
                val nextAttemptAtMillis = computeNextAttemptAtMillis(freshItem.attemptCount + 1)
                pendingSyncDao.markRetry(
                    id = freshItem.id,
                    nextAttemptAtMillis = nextAttemptAtMillis,
                    errorMessage = freshItem.lastErrorMessage ?: "تعذر مزامنة الفاتورة الآن"
                )
                ItemOutcome.Retry
            }

            RemoteOutcome.Failure -> {
                pendingSyncDao.markFailed(
                    id = freshItem.id,
                    errorMessage = freshItem.lastErrorMessage ?: "فشلت مزامنة الفاتورة"
                )
                ItemOutcome.Completed
            }
        }
    }

    private suspend fun executeRemoteOperation(
        item: PendingSyncEntity,
        operation: PendingSyncOperation
    ): RemoteOutcome {
        return try {
            when (operation) {
                PendingSyncOperation.DELETE -> {
                    val remoteId = item.remoteIdOrEntityId()
                    if (remoteId.isBlank()) {
                        return RemoteOutcome.Failure
                    }
                    apiService.deleteInvoice(remoteId).toDeleteOutcome()
                }

                PendingSyncOperation.CREATE -> {
                    val dto = item.decodeInvoiceDtoOrNull() ?: return RemoteOutcome.Failure
                    val normalized = dto.normalizedForRequest(item.entityId)
                    apiService.createInvoice(normalized).toInvoiceOutcome()
                }

                PendingSyncOperation.UPDATE -> {
                    val dto = item.decodeInvoiceDtoOrNull() ?: return RemoteOutcome.Failure
                    val remoteId = item.remoteIdOrEntityId()
                    if (remoteId.isBlank()) return RemoteOutcome.Failure
                    val normalized = dto.normalizedForRequest(item.entityId)
                    apiService.updateInvoice(remoteId, normalized).toInvoiceOutcome()
                }

                PendingSyncOperation.UPSERT,
                PendingSyncOperation.REPAIR -> {
                    val dto = item.decodeInvoiceDtoOrNull() ?: return RemoteOutcome.Failure
                    val remoteId = item.remoteIdOrEntityId()
                    val normalized = dto.normalizedForRequest(item.entityId)

                    if (remoteId.isBlank()) {
                        apiService.createInvoice(normalized).toInvoiceOutcome()
                    } else {
                        apiService.updateInvoice(remoteId, normalized).toInvoiceOutcome()
                    }
                }
            }
        } catch (throwable: Throwable) {
            when (throwable.toApiException()) {
                is NetworkUnavailableException,
                is RequestTimeoutException -> RemoteOutcome.Retry

                else -> RemoteOutcome.Failure
            }
        }
    }

    private suspend fun applyLocalSyncState(
        item: PendingSyncEntity,
        operation: PendingSyncOperation
    ) {
        when (operation) {
            PendingSyncOperation.DELETE -> {
                invoiceDao.softDelete(
                    id = item.entityId,
                    syncState = "SYNCED"
                )
            }

            else -> {
                invoiceDao.markSynced(
                    id = item.entityId,
                    syncState = "SYNCED"
                )
            }
        }
    }

    private fun PendingSyncEntity.decodeInvoiceDtoOrNull(): SaleInvoiceDto? {
        return try {
            gson.fromJson(payloadJson, SaleInvoiceDto::class.java)
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

    private fun PendingSyncEntity.remoteIdOrEntityId(): String {
        val dto = decodeInvoiceDtoOrNull()
        return when {
            !dto?.id.isNullOrBlank() -> dto!!.id!!
            entityId.isNotBlank() -> entityId
            else -> ""
        }
    }

    private fun SaleInvoiceDto.normalizedForRequest(fallbackId: String): SaleInvoiceDto {
        return copy(
            id = id ?: fallbackId
        )
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

    private fun Response<SaleInvoiceDto>.toInvoiceOutcome(): RemoteOutcome {
        return when {
            isSuccessful && body() != null -> RemoteOutcome.Success
            isSuccessful && body() == null -> RemoteOutcome.Failure
            else -> toApiException().toRemoteOutcome()
        }
    }

    private fun Response<Unit>.toDeleteOutcome(): RemoteOutcome {
        return when {
            isSuccessful -> RemoteOutcome.Success
            else -> toApiException().toRemoteOutcome()
        }
    }

    private fun Response<*>.toApiException(): ApiException {
        val errorText = errorBody()?.string().orEmpty()
        val safeMessage = when {
            errorText.isNotBlank() -> errorText.trim().replace(Regex("\\s+"), " ").take(500)
            message().isNotBlank() -> message()
            else -> "حدث خطأ في الخادم"
        }

        return when (code()) {
            400 -> ApiException.badRequest(message = safeMessage)
            401 -> ApiException.unauthorized(message = safeMessage)
            403 -> ApiException.forbidden(message = safeMessage)
            404 -> ApiException(code = 404, message = safeMessage, userMessage = "الفاتورة غير موجودة")
            409 -> ApiException(code = 409, message = safeMessage, userMessage = "يوجد تعارض في البيانات")
            422 -> ApiException(code = 422, message = safeMessage, userMessage = "البيانات المدخلة غير صالحة")
            in 500..599 -> ApiException.serverError(message = safeMessage)
            else -> ApiException.unexpected(message = safeMessage)
        }
    }

    private fun ApiException.toRemoteOutcome(): RemoteOutcome {
        return when {
            shouldRetry(this) -> RemoteOutcome.Retry
            else -> RemoteOutcome.Failure
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
        private const val ENTITY_TYPE_INVOICE = "INVOICE"
        private const val MAX_ITEMS_PER_RUN = 20

        private val BASE_RETRY_DELAY_MILLIS = TimeUnit.SECONDS.toMillis(30)
        private val MAX_RETRY_DELAY_MILLIS = TimeUnit.MINUTES.toMillis(30)
        private val STALE_LOCK_TIMEOUT_MILLIS = TimeUnit.MINUTES.toMillis(15)
        private const val JITTER_MAX_MILLIS = 2_500L
    }
}