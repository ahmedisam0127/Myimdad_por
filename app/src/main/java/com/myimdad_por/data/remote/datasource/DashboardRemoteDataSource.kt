package com.myimdad_por.data.remote.datasource

import com.myimdad_por.core.network.ApiException
import com.myimdad_por.core.network.HttpApiException
import com.myimdad_por.core.network.NetworkResult
import com.myimdad_por.core.network.NetworkUnavailableException
import com.myimdad_por.core.network.RequestTimeoutException
import com.myimdad_por.data.remote.api.DashboardApiService
import com.myimdad_por.data.remote.dto.DashboardAnalyticsDto
import com.myimdad_por.data.remote.dto.DashboardDto
import com.myimdad_por.data.remote.dto.DashboardSummaryDto
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject

class DashboardRemoteDataSource @Inject constructor(
    private val apiService: DashboardApiService
) {

    suspend fun getDashboard(
        storeId: String? = null,
        branchId: String? = null,
        currency: String? = null,
        timezone: String? = null
    ): NetworkResult<DashboardDto> = safeCall {
        apiService.getDashboard(
            storeId = storeId,
            branchId = branchId,
            currency = currency,
            timezone = timezone
        )
    }

    suspend fun getDashboardSummary(
        storeId: String? = null,
        branchId: String? = null,
        currency: String? = null,
        timezone: String? = null
    ): NetworkResult<DashboardSummaryDto> = safeCall {
        apiService.getDashboardSummary(
            storeId = storeId,
            branchId = branchId,
            currency = currency,
            timezone = timezone
        )
    }

    suspend fun getDashboardAnalytics(
        storeId: String? = null,
        branchId: String? = null,
        from: String? = null,
        to: String? = null,
        period: String? = null,
        groupBy: String? = null,
        currency: String? = null,
        timezone: String? = null
    ): NetworkResult<DashboardAnalyticsDto> = safeCall {
        apiService.getDashboardAnalytics(
            storeId = storeId,
            branchId = branchId,
            from = from,
            to = to,
            period = period,
            groupBy = groupBy,
            currency = currency,
            timezone = timezone
        )
    }

    suspend fun refreshDashboard(
        storeId: String? = null,
        branchId: String? = null,
        currency: String? = null,
        timezone: String? = null
    ): NetworkResult<DashboardDto> = safeCall {
        apiService.refreshDashboard(
            storeId = storeId,
            branchId = branchId,
            currency = currency,
            timezone = timezone
        )
    }

    suspend fun <T> execute(
        request: suspend () -> T
    ): T {
        return when (val result = safeCall(request)) {
            is NetworkResult.Success -> result.data
            is NetworkResult.Error -> throw result.exception
            NetworkResult.Loading -> throw ApiException.unexpected("Unexpected loading state")
        }
    }

    private suspend fun <T> safeCall(
        request: suspend () -> T
    ): NetworkResult<T> = withContext(Dispatchers.IO) {
        try {
            val data = request()
            NetworkResult.success(data)
        } catch (e: CancellationException) {
            throw e
        } catch (e: HttpException) {
            NetworkResult.error(e.toApiException())
        } catch (e: Throwable) {
            NetworkResult.error(e.toApiException())
        }
    }

    private fun HttpException.toApiException(): ApiException {
        val rawError = response()?.errorBody()?.string().orEmpty()
        val safeMessage = when {
            rawError.isNotBlank() -> rawError.sanitizeErrorMessage()
            message().isNotBlank() -> message()
            else -> "حدث خطأ في الخادم"
        }

        return when (code()) {
            400 -> HttpApiException(
                code = 400,
                message = safeMessage,
                userMessage = "الطلب غير صحيح"
            )

            401 -> HttpApiException(
                code = 401,
                message = safeMessage,
                userMessage = "غير مصرح بالدخول"
            )

            403 -> HttpApiException(
                code = 403,
                message = safeMessage,
                userMessage = "لا تملك صلاحية الوصول"
            )

            404 -> HttpApiException(
                code = 404,
                message = safeMessage,
                userMessage = "العنصر غير موجود"
            )

            409 -> HttpApiException(
                code = 409,
                message = safeMessage,
                userMessage = "يوجد تعارض في البيانات"
            )

            422 -> HttpApiException(
                code = 422,
                message = safeMessage,
                userMessage = "البيانات المدخلة غير صالحة"
            )

            in 500..599 -> HttpApiException(
                code = code(),
                message = safeMessage,
                userMessage = "حدث خطأ في الخادم"
            )

            else -> HttpApiException(
                code = code(),
                message = safeMessage,
                userMessage = "حدث خطأ غير متوقع"
            )
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
            is HttpException -> toApiException()
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