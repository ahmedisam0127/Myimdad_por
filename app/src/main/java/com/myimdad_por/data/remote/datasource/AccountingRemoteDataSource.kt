package com.myimdad_por.data.remote.datasource

import com.myimdad_por.core.network.ApiException
import com.myimdad_por.core.network.HttpApiException
import com.myimdad_por.core.network.NetworkResult
import com.myimdad_por.core.network.NetworkUnavailableException
import com.myimdad_por.core.network.RequestTimeoutException
import com.myimdad_por.data.remote.api.AccountingApiService
import com.myimdad_por.data.remote.dto.AccountingDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Remote datasource dedicated to accounting ledgers.
 *
 * This layer keeps the Retrofit contract isolated and gives the app:
 * - typed access to accounting operations
 * - a single error translation pipeline
 * - predictable network result wrapping
 */
class AccountingRemoteDataSource(
    private val apiService: AccountingApiService
) {

    suspend fun listEntries(
        page: Int? = null,
        limit: Int? = null,
        status: String? = null,
        source: String? = null,
        referenceId: String? = null,
        filters: Map<String, String> = emptyMap()
    ): NetworkResult<List<AccountingDto>> {
        return call {
            listEntries(
                page = page,
                limit = limit,
                status = status,
                source = source,
                referenceId = referenceId,
                filters = filters
            )
        }
    }

    suspend fun getEntry(id: String): NetworkResult<AccountingDto> {
        return call { getEntry(id) }
    }

    suspend fun createEntry(request: AccountingDto): NetworkResult<AccountingDto> {
        return call { createEntry(request) }
    }

    suspend fun updateEntry(id: String, request: AccountingDto): NetworkResult<AccountingDto> {
        return call { updateEntry(id, request) }
    }

    suspend fun deleteEntry(id: String): NetworkResult<Unit> {
        return callUnit { deleteEntry(id) }
    }

    suspend fun postEntry(id: String): NetworkResult<AccountingDto> {
        return call { postEntry(id) }
    }

    suspend fun <T> call(
        request: suspend AccountingApiService.() -> Response<T>
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
        request: suspend AccountingApiService.() -> Response<Unit>
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
        request: suspend AccountingApiService.() -> Response<AccountingDto>
    ): AccountingDto {
        return when (val result = call(request)) {
            is NetworkResult.Success -> result.data
            is NetworkResult.Error -> throw result.exception
            NetworkResult.Loading -> throw ApiException.unexpected("Unexpected loading state")
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

    private fun String.sanitizeErrorMessage(): String {
        return trim()
            .replace(Regex("\\s+"), " ")
            .take(500)
    }
}