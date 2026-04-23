package com.myimdad_por.data.remote.datasource

import com.myimdad_por.core.network.ApiException
import com.myimdad_por.core.network.HttpApiException
import com.myimdad_por.core.network.NetworkResult
import com.myimdad_por.core.network.NetworkUnavailableException
import com.myimdad_por.core.network.RequestTimeoutException
import com.myimdad_por.data.remote.api.CountResponse
import com.myimdad_por.data.remote.api.DeleteSuppliersRequest
import com.myimdad_por.data.remote.api.MarkSupplierActiveRequest
import com.myimdad_por.data.remote.api.MarkSupplierPreferredRequest
import com.myimdad_por.data.remote.api.MoneyDto
import com.myimdad_por.data.remote.api.SupplierApiService
import com.myimdad_por.data.remote.api.SupplierPerformanceDto
import com.myimdad_por.data.remote.api.UpdateSupplierDebtRequest
import com.myimdad_por.data.remote.dto.SupplierDto
import com.myimdad_por.domain.repository.SupplierPerformanceSummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response
import java.io.IOException
import java.math.BigDecimal
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject
/**
 * Remote datasource خاصة بالموردين.
 *
 * هذه الطبقة تتولى:
 * - استدعاء API
 * - تحويل الأخطاء إلى ApiException موحّد
 * - إعادة النتائج كـ NetworkResult
 */
class SupplierRemoteDataSource @Inject constructor(
    private val apiService: SupplierApiService
) {

    suspend fun getSuppliers(): NetworkResult<List<SupplierDto>> =
        call { getSuppliers() }

    suspend fun getSupplierById(id: String): NetworkResult<SupplierDto> =
        call { getSupplierById(id) }

    suspend fun getSupplierByCode(supplierCode: String): NetworkResult<SupplierDto> =
        call { getSupplierByCode(supplierCode) }

    suspend fun searchSuppliers(query: String): NetworkResult<List<SupplierDto>> =
        call { searchSuppliers(query) }

    suspend fun getSuppliersWithDebt(): NetworkResult<List<SupplierDto>> =
        call { getSuppliersWithDebt() }

    suspend fun getPreferredSuppliers(): NetworkResult<List<SupplierDto>> =
        call { getPreferredSuppliers() }

    suspend fun getActiveSuppliers(): NetworkResult<List<SupplierDto>> =
        call { getActiveSuppliers() }

    suspend fun saveSupplier(supplier: SupplierDto): NetworkResult<SupplierDto> =
        call { saveSupplier(supplier) }

    suspend fun saveSuppliers(suppliers: List<SupplierDto>): NetworkResult<List<SupplierDto>> =
        call { saveSuppliers(suppliers) }

    suspend fun updateSupplier(
        id: String,
        supplier: SupplierDto
    ): NetworkResult<SupplierDto> = call { updateSupplier(id, supplier) }

    suspend fun updateSupplierDebt(
        id: String,
        balanceDelta: BigDecimal
    ): NetworkResult<SupplierDto> = call {
        updateSupplierDebt(
            id,
            UpdateSupplierDebtRequest(balanceDelta = balanceDelta.toPlainString())
        )
    }

    suspend fun markSupplierPreferred(
        id: String,
        preferred: Boolean
    ): NetworkResult<SupplierDto> = call {
        markSupplierPreferred(
            id,
            MarkSupplierPreferredRequest(preferred = preferred)
        )
    }

    suspend fun markSupplierActive(
        id: String,
        active: Boolean
    ): NetworkResult<SupplierDto> = call {
        markSupplierActive(
            id,
            MarkSupplierActiveRequest(active = active)
        )
    }

    suspend fun deleteSupplier(id: String): NetworkResult<Unit> =
        call { deleteSupplier(id) }

    suspend fun deleteSuppliers(ids: List<String>): NetworkResult<Int> {
        return call {
            deleteSuppliers(DeleteSuppliersRequest(ids = ids))
        }.map { response ->
            response.deletedCount
        }
    }

    suspend fun clearAll(): NetworkResult<Unit> =
        call { clearAll() }

    suspend fun countSuppliers(): NetworkResult<Long> {
        return call { countSuppliers() }.map { it.count }
    }

    suspend fun countActiveSuppliers(): NetworkResult<Long> {
        return call { countActiveSuppliers() }.map { it.count }
    }

    suspend fun countPreferredSuppliers(): NetworkResult<Long> {
        return call { countPreferredSuppliers() }.map { it.count }
    }

    suspend fun countSuppliersWithDebt(): NetworkResult<Long> {
        return call { countSuppliersWithDebt() }.map { it.count }
    }

    suspend fun getSupplierBalance(supplierId: String): NetworkResult<BigDecimal> {
        return call { getSupplierBalance(supplierId) }.map { it.amount.toBigDecimalOrZero() }
    }

    suspend fun getSupplierBalanceByCode(supplierCode: String): NetworkResult<BigDecimal> {
        return call { getSupplierBalanceByCode(supplierCode) }.map { it.amount.toBigDecimalOrZero() }
    }

    suspend fun getSupplierPerformance(
        supplierId: String,
        fromMillis: Long? = null,
        toMillis: Long? = null
    ): NetworkResult<SupplierPerformanceSummary> {
        return call { getSupplierPerformance(supplierId, fromMillis, toMillis) }
            .map { it.toDomain() }
    }

    private suspend fun <T, R> NetworkResult<T>.map(transform: (T) -> R): NetworkResult<R> {
        return when (this) {
            is NetworkResult.Success -> NetworkResult.success(transform(data))
            is NetworkResult.Error -> NetworkResult.error(exception)
            NetworkResult.Loading -> NetworkResult.Loading
        }
    }

    private suspend fun <T> call(
        request: suspend SupplierApiService.() -> Response<T>
    ): NetworkResult<T> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.request()
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    NetworkResult.success(body)
                } else {
                    NetworkResult.error(
                        HttpApiException(
                            code = response.code(),
                            message = "Empty response body",
                            userMessage = "الخادم لم يُرجع بيانات"
                        )
                    )
                }
            } else {
                NetworkResult.error(response.toApiException())
            }
        } catch (throwable: Throwable) {
            NetworkResult.error(throwable.toApiException())
        }
    }

    private fun <T> Response<T>.toApiException(): ApiException {
        val errorText = errorBody()?.string().orEmpty()
        val safeMessage = when {
            errorText.isNotBlank() -> errorText.sanitizeErrorMessage()
            message().isNotBlank() -> message()
            else -> "حدث خطأ في الخادم"
        }

        return when (code()) {
            400 -> HttpApiException(code = 400, message = safeMessage, userMessage = "الطلب غير صحيح")
            401 -> HttpApiException(code = 401, message = safeMessage, userMessage = "غير مصرح بالدخول")
            403 -> HttpApiException(code = 403, message = safeMessage, userMessage = "لا تملك صلاحية الوصول")
            404 -> HttpApiException(code = 404, message = safeMessage, userMessage = "المورد غير موجود")
            409 -> HttpApiException(code = 409, message = safeMessage, userMessage = "يوجد تعارض في البيانات")
            422 -> HttpApiException(code = 422, message = safeMessage, userMessage = "البيانات المدخلة غير صالحة")
            in 500..599 -> HttpApiException(code = code(), message = safeMessage, userMessage = "حدث خطأ في الخادم")
            else -> HttpApiException(code = code(), message = safeMessage, userMessage = "حدث خطأ غير متوقع")
        }
    }

    private fun Throwable.toApiException(): ApiException {
        return when (this) {
            is ApiException -> this
            is SocketTimeoutException -> RequestTimeoutException(cause = this)
            is UnknownHostException -> NetworkUnavailableException(cause = this)
            is IOException -> NetworkUnavailableException(
                message = message ?: "لا يوجد اتصال بالإنترنت",
                cause = this
            )
            else -> ApiException.unexpected(
                message = message ?: "حدث خطأ غير متوقع",
                cause = this
            )
        }
    }

    private fun String.sanitizeErrorMessage(): String {
        return trim()
            .replace(Regex("\\s+"), " ")
            .take(500)
    }

    private fun String.toBigDecimalOrZero(): BigDecimal {
        return runCatching { BigDecimal(this) }.getOrElse { BigDecimal.ZERO }
    }
}