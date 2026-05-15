package com.myimdad_por.data.remote.datasource

import com.myimdad_por.core.network.ApiException
import com.myimdad_por.core.network.HttpApiException
import com.myimdad_por.core.network.NetworkResult
import com.myimdad_por.core.network.NetworkUnavailableException
import com.myimdad_por.core.network.RequestTimeoutException
import com.myimdad_por.data.remote.api.ApiService
import com.myimdad_por.data.remote.api.AuthApiService.RefreshTokenRequest
import com.myimdad_por.data.remote.dto.auth.AuthResponseDto
import com.myimdad_por.data.remote.dto.auth.ForgotPasswordRequest
import com.myimdad_por.data.remote.dto.auth.LoginRequestDto
import com.myimdad_por.data.remote.dto.auth.RegisterRequestDto
import com.myimdad_por.data.remote.dto.auth.ResetPasswordRequest
import com.myimdad_por.data.remote.dto.auth.VerifyOtpRequest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject

class RemoteDataSource @Inject constructor(
    private val apiService: ApiService
) {

    suspend fun login(request: LoginRequestDto): NetworkResult<AuthResponseDto> =
        safeCall { login(request) }

    suspend fun register(request: RegisterRequestDto): NetworkResult<AuthResponseDto> =
        safeCall { register(request) }

    suspend fun forgotPassword(request: ForgotPasswordRequest): NetworkResult<AuthResponseDto> =
        safeCall { forgotPassword(request) }

    suspend fun verifyOtp(request: VerifyOtpRequest): NetworkResult<AuthResponseDto> =
        safeCall { verifyOtp(request) }

    suspend fun resetPassword(request: ResetPasswordRequest): NetworkResult<AuthResponseDto> =
        safeCall { resetPassword(request) }

    suspend fun refreshToken(
        refreshToken: String,
        deviceId: String? = null
    ): NetworkResult<AuthResponseDto> = safeCall {
        refreshToken(
            RefreshTokenRequest(
                refreshToken = refreshToken,
                deviceId = deviceId
            )
        )
    }

    suspend fun getCurrentUser(authorization: String): NetworkResult<AuthResponseDto> =
        safeCall { getCurrentUser(authorization) }

    suspend fun logout(authorization: String): NetworkResult<Unit> =
        safeCall { logout(authorization) }

    suspend fun revokeAllSessions(authorization: String): NetworkResult<Unit> =
        safeCall { revokeAllSessions(authorization) }

    suspend fun <T> execute(
        request: suspend ApiService.() -> Response<T>
    ): T {
        return when (val result = safeCall(request)) {
            is NetworkResult.Success -> result.data
            is NetworkResult.Error -> throw result.exception
            NetworkResult.Loading -> throw ApiException.unexpected("Unexpected loading state")
        }
    }

    private suspend fun <T> safeCall(
        request: suspend ApiService.() -> Response<T>
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