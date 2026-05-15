package com.myimdad_por.data.remote.datasource

import com.myimdad_por.core.network.ApiException
import com.myimdad_por.core.network.HttpApiException
import com.myimdad_por.core.network.NetworkResult
import com.myimdad_por.core.network.NetworkUnavailableException
import com.myimdad_por.core.network.RequestTimeoutException
import com.myimdad_por.data.remote.api.ReportApiService
import com.myimdad_por.data.remote.dto.ReportDto
import com.myimdad_por.data.remote.dto.ReportExportRequestDto
import com.myimdad_por.data.remote.dto.ReportGenerateRequestDto
import com.myimdad_por.data.remote.dto.ReportListResponseDto
import com.myimdad_por.data.remote.dto.ReportSyncRequestDto
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject

class ReportRemoteDataSource @Inject constructor(
    private val reportApiService: ReportApiService
) {

    suspend fun getReports(
        page: Int? = null,
        limit: Int? = null,
        type: String? = null,
        exported: Boolean? = null,
        generatedByUserId: String? = null,
        fromMillis: Long? = null,
        toMillis: Long? = null,
        query: String? = null,
        sortBy: String? = null,
        sortOrder: String? = null
    ): NetworkResult<ReportListResponseDto> = safeCall {
        getReports(
            page = page,
            limit = limit,
            type = type,
            exported = exported,
            generatedByUserId = generatedByUserId,
            fromMillis = fromMillis,
            toMillis = toMillis,
            query = query,
            sortBy = sortBy,
            sortOrder = sortOrder
        )
    }

    suspend fun getReportById(reportId: String): NetworkResult<ReportDto> = safeCall {
        getReportById(reportId)
    }

    suspend fun createReport(request: ReportGenerateRequestDto): NetworkResult<ReportDto> = safeCall {
        createReport(request)
    }

    suspend fun updateReport(
        reportId: String,
        request: ReportDto
    ): NetworkResult<ReportDto> = safeCall {
        updateReport(reportId, request)
    }

    suspend fun deleteReport(reportId: String): NetworkResult<Unit> = safeCall {
        deleteReport(reportId)
    }

    suspend fun exportReport(
        reportId: String,
        request: ReportExportRequestDto
    ): NetworkResult<ReportDto> = safeCall {
        exportReport(reportId, request)
    }

    suspend fun syncReports(
        request: ReportSyncRequestDto
    ): NetworkResult<ReportListResponseDto> = safeCall {
        syncReports(request)
    }

    suspend fun deleteReportsBulk(
        reportIds: List<String>
    ): NetworkResult<Unit> = safeCall {
        deleteReportsBulk(reportIds)
    }

    suspend fun <T> execute(
        request: suspend ReportApiService.() -> Response<T>
    ): T {
        return when (val result = safeCall(request)) {
            is NetworkResult.Success -> result.data
            is NetworkResult.Error -> throw result.exception
            NetworkResult.Loading -> throw ApiException.unexpected("Unexpected loading state")
        }
    }

    private suspend fun <T> safeCall(
        request: suspend ReportApiService.() -> Response<T>
    ): NetworkResult<T> = withContext(Dispatchers.IO) {
        try {
            val response = reportApiService.request()
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
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            NetworkResult.error(e.toApiException())
        }
    }

    private fun <T> Response<T>.toApiException(): ApiException {
        val rawError = errorBody()?.string().orEmpty()
        val safeMessage = when {
            rawError.isNotBlank() -> rawError.sanitizeErrorMessage()
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