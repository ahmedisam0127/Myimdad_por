package com.myimdad_por.data.remote.api

import com.myimdad_por.data.remote.dto.auth.AuthResponseDto
import com.myimdad_por.data.remote.dto.auth.ForgotPasswordRequest
import com.myimdad_por.data.remote.dto.auth.LoginRequestDto
import com.myimdad_por.data.remote.dto.auth.RegisterRequestDto
import com.myimdad_por.data.remote.dto.auth.ResetPasswordRequest
import com.myimdad_por.data.remote.dto.auth.VerifyOtpRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

interface AuthApiService {

    @POST(Paths.LOGIN)
    suspend fun login(
        @Body request: LoginRequestDto
    ): Response<AuthResponseDto>

    @POST(Paths.REGISTER)
    suspend fun register(
        @Body request: RegisterRequestDto
    ): Response<AuthResponseDto>

    @POST(Paths.FORGOT_PASSWORD)
    suspend fun forgotPassword(
        @Body request: ForgotPasswordRequest
    ): Response<AuthResponseDto>

    @POST(Paths.VERIFY_OTP)
    suspend fun verifyOtp(
        @Body request: VerifyOtpRequest
    ): Response<AuthResponseDto>

    @POST(Paths.RESET_PASSWORD)
    suspend fun resetPassword(
        @Body request: ResetPasswordRequest
    ): Response<AuthResponseDto>

    @POST(Paths.REFRESH_TOKEN)
    suspend fun refreshToken(
        @Body request: RefreshTokenRequest
    ): Response<AuthResponseDto>

    @GET(Paths.ME)
    suspend fun getCurrentUser(
        @Header("Authorization") authorization: String
    ): Response<AuthResponseDto>

    @DELETE(Paths.LOGOUT)
    suspend fun logout(
        @Header("Authorization") authorization: String
    ): Response<Unit>

    @POST(Paths.REVOKE_ALL_SESSIONS)
    suspend fun revokeAllSessions(
        @Header("Authorization") authorization: String
    ): Response<Unit>

    data class RefreshTokenRequest(
        val refreshToken: String,
        val deviceId: String? = null
    ) {
        init {
            require(refreshToken.isNotBlank()) { "refreshToken cannot be blank." }
            deviceId?.let {
                require(it.isNotBlank()) { "deviceId cannot be blank when provided." }
            }
        }
    }

    object Paths {
        const val LOGIN = "auth/login"
        const val REGISTER = "auth/register"
        const val FORGOT_PASSWORD = "auth/forgot-password"
        const val VERIFY_OTP = "auth/verify-otp"
        const val RESET_PASSWORD = "auth/reset-password"
        const val REFRESH_TOKEN = "auth/refresh-token"
        const val ME = "auth/me"
        const val LOGOUT = "auth/logout"
        const val REVOKE_ALL_SESSIONS = "auth/sessions/revoke-all"
    }
}