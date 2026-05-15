package com.myimdad_por.data.remote.datasource

import com.myimdad_por.core.network.ApiException
import com.myimdad_por.core.network.HttpApiException
import com.myimdad_por.core.network.NetworkResult
import com.myimdad_por.core.network.NetworkUnavailableException
import com.myimdad_por.core.network.RequestTimeoutException
import com.myimdad_por.data.remote.api.PaymentApiService
import com.myimdad_por.data.remote.dto.PaymentDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject


class PaymentRemoteDataSource @Inject constructor(
    private val apiService: PaymentApiService
) {

    suspend fun listPayments(
        page: Int? = null,
        limit: Int? = null,
        status: String? = null,
        transactionId: String? = null,
        invoiceId: String? = null,
        filters: Map<String, String> = emptyMap()
    ): NetworkResult<List<PaymentDto>> {
        return call {
            apiService.listPayments(
                page = page,
                limit = limit,
                status = status,
                transactionId = transactionId,
                invoiceId = invoiceId,
                filters = filters
            )
        }
    }

    suspend fun getPayment(id: String): NetworkResult<PaymentDto> {
        return call { apiService.getPayment(id) }
    }

    suspend fun createPayment(request: PaymentDto): NetworkResult<PaymentDto> {
        return call { apiService.createPayment(request) }
    }

    suspend fun updatePayment(id: String, request: PaymentDto): NetworkResult<PaymentDto> {
        return call { apiService.updatePayment(id, request) }
    }

    suspend fun deletePayment(id: String): NetworkResult<Unit> {
        return callUnit { apiService.deletePayment(id) }
    }

    suspend fun <T> call(
        request: suspend PaymentApiService.() -> Response<T>
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

    suspend fun callUnit(
        request: suspend PaymentApiService.() -> Response<Unit>
    ): NetworkResult<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.request()
            if (response.isSuccessful) {
                NetworkResult.success(Unit)
            } else {
                NetworkResult.error(response.toApiException())
            }
        } catch (throwable: Throwable) {
            NetworkResult.error(throwable.toApiException())
        }
    }

    suspend fun execute(
        request: suspend PaymentApiService.() -> Response<PaymentDto>
    ): PaymentDto {
        return when (val result = call(request)) {
            is NetworkResult.Success -> result.data
            is NetworkResult.Error -> throw result.exception
            NetworkResult.Loading -> throw ApiException.unexpected("Unexpected loading state")
        }
    }

    private fun <T> Response<T>.toApiException(): ApiException {
        val errorText = errorBody()?.string().orEmpty()
        val safeMessage = when {
            errorText.isNotBlank() -> errorText.trim().replace(Regex("\\s+"), " ").take(500)
            message().isNotBlank() -> message()
            else -> "حدث خطأ في الخادم"
        }

        return when (code()) {
            400 -> HttpApiException(code = 400, message = safeMessage, userMessage = "الطلب غير صحيح")
            401 -> HttpApiException(code = 401, message = safeMessage, userMessage = "غير مصرح بالدخول")
            403 -> HttpApiException(code = 403, message = safeMessage, userMessage = "لا تملك صلاحية الوصول")
            404 -> HttpApiException(code = 404, message = safeMessage, userMessage = "العنصر غير موجود")
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
}