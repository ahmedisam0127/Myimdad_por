package com.myimdad_por.data.repository

import com.myimdad_por.core.network.ApiException
import com.myimdad_por.core.network.NetworkResult
import com.myimdad_por.core.security.SessionManager
import com.myimdad_por.data.mapper.AuthMapper
import com.myimdad_por.data.remote.datasource.AuthRemoteDataSource
import com.myimdad_por.data.remote.dto.auth.AuthResponseDto
import com.myimdad_por.data.remote.dto.auth.ForgotPasswordRequest
import com.myimdad_por.data.remote.dto.auth.LoginRequestDto
import com.myimdad_por.data.remote.dto.auth.RegisterRequestDto
import com.myimdad_por.data.remote.dto.auth.ResetPasswordRequest
import com.myimdad_por.domain.model.Role
import com.myimdad_por.domain.model.User
import com.myimdad_por.domain.repository.AuthRepository
import com.myimdad_por.domain.repository.AuthSession
import com.myimdad_por.domain.repository.ChangePasswordRequest
import com.myimdad_por.domain.repository.ConfirmPasswordResetRequest
import com.myimdad_por.domain.repository.EmployeeRegistrationRequest
import com.myimdad_por.domain.repository.LoginRequest
import com.myimdad_por.domain.repository.ManagerRegistrationRequest
import com.myimdad_por.domain.repository.PasswordResetRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val remoteDataSource: AuthRemoteDataSource
) : AuthRepository {

    private val sessionMutex = Mutex()
    private val sessionState = MutableStateFlow<AuthSession?>(null)
    private val currentUserState = MutableStateFlow<User?>(null)

    override fun observeSession(): Flow<AuthSession?> = sessionState.asStateFlow()

    override suspend fun login(request: LoginRequest): Result<AuthSession> {
        return withContext(Dispatchers.IO) {
            when (val result = remoteDataSource.login(request.toDto())) {
                is NetworkResult.Success -> result.toAuthSessionResult()
                is NetworkResult.Error -> Result.failure(result.exception)
                NetworkResult.Loading -> Result.failure(
                    ApiException.unexpected("Unexpected loading state")
                )
            }
        }
    }

    override suspend fun logout(): Result<Unit> {
        return withContext(Dispatchers.IO) {
            val authorization = currentAuthorizationHeader()

            val remoteResult = if (authorization != null) {
                remoteDataSource.logout(authorization)
            } else {
                NetworkResult.Success(Unit)
            }

            clearLocalSession()

            when (remoteResult) {
                is NetworkResult.Success -> Result.success(Unit)
                is NetworkResult.Error -> Result.failure(remoteResult.exception)
                NetworkResult.Loading -> Result.failure(
                    ApiException.unexpected("Unexpected loading state")
                )
            }
        }
    }

    override suspend fun getCurrentUser(): User? {
        currentUserState.value?.let { return it }

        sessionState.value?.user?.let { cachedUser ->
            currentUserState.value = cachedUser
            return cachedUser
        }

        val authorization = currentAuthorizationHeader() ?: return null

        return when (
            val result = withContext(Dispatchers.IO) {
                remoteDataSource.getCurrentUser(authorization)
            }
        ) {
            is NetworkResult.Success -> {
                val response = result.data
                if (!response.success) return null

                val user = response.toDomainUserOrNull() ?: return null
                syncSessionFromResponse(
                    user = user,
                    response = response,
                    fallbackAccessToken = currentAccessToken()
                )
                user
            }

            is NetworkResult.Error -> {
                if (result.exception.code == 401) {
                    clearLocalSession()
                }
                null
            }

            NetworkResult.Loading -> null
        }
    }

    override suspend fun refreshCurrentUser(): Result<User> {
        val authorization = currentAuthorizationHeader()
            ?: return Result.failure(ApiException.unauthorized("غير مصرح بالدخول"))

        return when (
            val result = withContext(Dispatchers.IO) {
                remoteDataSource.getCurrentUser(authorization)
            }
        ) {
            is NetworkResult.Success -> {
                val response = result.data
                if (!response.success) {
                    return Result.failure(
                        ApiException.unexpected(
                            response.message?.takeIf { it.isNotBlank() }
                                ?: "فشل تحديث بيانات المستخدم"
                        )
                    )
                }

                val user = response.toDomainUserOrNull()
                    ?: return Result.failure(
                        ApiException.unexpected("الخادم لم يُرجع بيانات المستخدم")
                    )

                syncSessionFromResponse(
                    user = user,
                    response = response,
                    fallbackAccessToken = currentAccessToken()
                )
                Result.success(user)
            }

            is NetworkResult.Error -> {
                if (result.exception.code == 401) {
                    clearLocalSession()
                }
                Result.failure(result.exception)
            }

            NetworkResult.Loading -> Result.failure(
                ApiException.unexpected("Unexpected loading state")
            )
        }
    }

    override suspend fun registerManager(request: ManagerRegistrationRequest): Result<User> {
        return withContext(Dispatchers.IO) {
            when (val result = remoteDataSource.register(request.toDto())) {
                is NetworkResult.Success -> result.toUserResult(
                    fallbackAccessToken = null,
                    persistSession = true
                )
                is NetworkResult.Error -> Result.failure(result.exception)
                NetworkResult.Loading -> Result.failure(
                    ApiException.unexpected("Unexpected loading state")
                )
            }
        }
    }

    override suspend fun registerEmployee(request: EmployeeRegistrationRequest): Result<User> {
        return withContext(Dispatchers.IO) {
            when (val result = remoteDataSource.register(request.toDto())) {
                is NetworkResult.Success -> result.toUserResult(
                    fallbackAccessToken = null,
                    persistSession = true
                )
                is NetworkResult.Error -> Result.failure(result.exception)
                NetworkResult.Loading -> Result.failure(
                    ApiException.unexpected("Unexpected loading state")
                )
            }
        }
    }

    override suspend fun changePassword(request: ChangePasswordRequest): Result<Unit> {
        return Result.failure(
            ApiException.unexpected(
                "لا يوجد endpoint مباشر لتغيير كلمة المرور في طبقة الشبكة الحالية"
            )
        )
    }

    override suspend fun requestPasswordReset(request: PasswordResetRequest): Result<Unit> {
        return withContext(Dispatchers.IO) {
            when (val result = remoteDataSource.forgotPassword(request.toDto())) {
                is NetworkResult.Success -> result.toUnitResult(
                    successMessageFallback = "تم إرسال طلب إعادة تعيين كلمة المرور"
                )
                is NetworkResult.Error -> Result.failure(result.exception)
                NetworkResult.Loading -> Result.failure(
                    ApiException.unexpected("Unexpected loading state")
                )
            }
        }
    }

    override suspend fun confirmPasswordReset(request: ConfirmPasswordResetRequest): Result<Unit> {
        return withContext(Dispatchers.IO) {
            when (val result = remoteDataSource.resetPassword(request.toDto())) {
                is NetworkResult.Success -> result.toUnitResult(
                    successMessageFallback = "تم تغيير كلمة المرور بنجاح"
                )
                is NetworkResult.Error -> Result.failure(result.exception)
                NetworkResult.Loading -> Result.failure(
                    ApiException.unexpected("Unexpected loading state")
                )
            }
        }
    }

    override suspend fun isManagerEmailAvailable(email: String): Boolean {
        return getUserByEmail(email)?.isManager != true
    }

    override suspend fun isEmployeeEmailAvailable(
        managerEmail: String,
        employeeEmail: String
    ): Boolean {
        return getUserByEmail(managerEmail)?.isManager == true &&
            getUserByEmail(employeeEmail)?.isEmployee != true
    }

    override suspend fun hasManagerAccount(email: String): Boolean {
        return getUserByEmail(email)?.isManager == true
    }

    override suspend fun hasEmployeeAccount(
        managerEmail: String,
        employeeEmail: String
    ): Boolean {
        return getUserByEmail(managerEmail)?.isManager == true &&
            getUserByEmail(employeeEmail)?.isEmployee == true
    }

    override suspend fun getUserByEmail(email: String): User? {
        val normalizedEmail = email.normalizeEmail() ?: return null

        currentUserState.value?.takeIf { it.matchesIdentifier(normalizedEmail) }?.let {
            return it
        }

        sessionState.value?.user?.takeIf { it.matchesIdentifier(normalizedEmail) }?.let {
            return it
        }

        return getCurrentUser()?.takeIf { it.matchesIdentifier(normalizedEmail) }
    }

    override suspend fun getUsersByRole(role: Role): List<User> {
        val user = getCurrentUser()
        return if (user?.role == role) listOf(user) else emptyList()
    }

    private suspend fun NetworkResult<AuthResponseDto>.toAuthSessionResult(): Result<AuthSession> {
        return when (this) {
            is NetworkResult.Success -> {
                val response = data
                if (!response.success) {
                    return Result.failure(
                        ApiException.unexpected(
                            response.message?.takeIf { it.isNotBlank() }
                                ?: "فشلت عملية المصادقة"
                        )
                    )
                }

                val user = response.toDomainUserOrNull()
                    ?: return Result.failure(
                        ApiException.unexpected("الخادم لم يُرجع بيانات المستخدم")
                    )

                val session = syncSessionFromResponse(
                    user = user,
                    response = response,
                    fallbackAccessToken = null
                ) ?: return Result.failure(
                    ApiException.unexpected("الخادم لم يُرجع رمز الدخول")
                )

                Result.success(session)
            }

            is NetworkResult.Error -> Result.failure(exception)

            NetworkResult.Loading -> Result.failure(
                ApiException.unexpected("Unexpected loading state")
            )
        }
    }

    private suspend fun NetworkResult<AuthResponseDto>.toUserResult(
        fallbackAccessToken: String?,
        persistSession: Boolean
    ): Result<User> {
        return when (this) {
            is NetworkResult.Success -> {
                val response = data
                if (!response.success) {
                    return Result.failure(
                        ApiException.unexpected(
                            response.message?.takeIf { it.isNotBlank() }
                                ?: "فشلت العملية"
                        )
                    )
                }

                val user = response.toDomainUserOrNull()
                    ?: return Result.failure(
                        ApiException.unexpected("الخادم لم يُرجع بيانات المستخدم")
                    )

                if (persistSession) {
                    syncSessionFromResponse(
                        user = user,
                        response = response,
                        fallbackAccessToken = fallbackAccessToken
                    )
                }

                Result.success(user)
            }

            is NetworkResult.Error -> Result.failure(exception)

            NetworkResult.Loading -> Result.failure(
                ApiException.unexpected("Unexpected loading state")
            )
        }
    }

    private fun NetworkResult<AuthResponseDto>.toUnitResult(
        successMessageFallback: String
    ): Result<Unit> {
        return when (this) {
            is NetworkResult.Success -> {
                val response = data
                if (response.success) {
                    Result.success(Unit)
                } else {
                    Result.failure(
                        ApiException.unexpected(
                            response.message?.takeIf { it.isNotBlank() }
                                ?: successMessageFallback
                        )
                    )
                }
            }

            is NetworkResult.Error -> Result.failure(exception)

            NetworkResult.Loading -> Result.failure(
                ApiException.unexpected("Unexpected loading state")
            )
        }
    }

    private fun AuthResponseDto.toDomainUserOrNull(): User? {
        return AuthMapper.run { toDomainUser() }
    }

    private suspend fun syncSessionFromResponse(
        user: User,
        response: AuthResponseDto,
        fallbackAccessToken: String?
    ): AuthSession? {
        return sessionMutex.withLock {
            val accessToken = response.accessToken
                ?.trim()
                ?.takeIf { it.isNotBlank() }
                ?: fallbackAccessToken
                    ?.trim()
                    ?.takeIf { it.isNotBlank() }
                ?: sessionState.value?.accessToken

            val refreshToken = response.refreshToken
                ?.trim()
                ?.takeIf { it.isNotBlank() }
                ?: sessionState.value?.refreshToken

            val expiresAtMillis = response.expiresInSeconds?.let { seconds ->
                response.serverTimeMillis + (seconds * 1000L)
            } ?: sessionState.value?.expiresAtMillis

            val session = if (!accessToken.isNullOrBlank()) {
                AuthSession(
                    user = user,
                    accessToken = accessToken,
                    refreshToken = refreshToken,
                    expiresAtMillis = expiresAtMillis
                )
            } else {
                sessionState.value?.copy(user = user)
            }

            currentUserState.value = user
            sessionState.value = session

            if (session != null) {
                runCatching {
                    SessionManager.saveSession(
                        token = session.accessToken,
                        userId = session.user.id,
                        refreshToken = session.refreshToken
                    )
                }
            }

            session
        }
    }

    private fun LoginRequest.toDto(): LoginRequestDto {
        return LoginRequestDto(
            identifier = email.trim(),
            password = password
        )
    }

    // داخل AuthRepositoryImpl.kt قم باستبدال دوال التحويل بهذه:

private fun ManagerRegistrationRequest.toDto(): RegisterRequestDto {
    return RegisterRequestDto(
        fullName = fullName.trim(),
        email = email.trim(),
        password = password,
        confirmPassword = confirmPassword,
        role = "MANAGER", // تأكد أنها تطابق ما يتوقعه السيرفر (MANAGER أو manager)
        storeName = storeName.trim(),
        storeLocation = storeLocation.trim(),
        username = email.trim() // غالباً السيرفر يحتاج username، نستخدم الايميل كافتراضي
    )
}

private fun EmployeeRegistrationRequest.toDto(): RegisterRequestDto {
    return RegisterRequestDto(
        fullName = employeeFullName.trim(),
        email = employeeEmail.trim(),
        password = password,
        confirmPassword = confirmPassword,
        role = "EMPLOYEE",
        managerEmail = managerEmail.trim(),
        username = employeeEmail.trim()
    )
}

    private fun PasswordResetRequest.toDto(): ForgotPasswordRequest {
        return ForgotPasswordRequest(
            identifier = email.trim(),
            deliveryMethod = "otp"
        )
    }

    private fun ConfirmPasswordResetRequest.toDto(): ResetPasswordRequest {
        return ResetPasswordRequest(
            identifier = email.trim(),
            otpCode = resetCode.trim(),
            newPassword = newPassword,
            confirmPassword = confirmNewPassword
        )
    }

    private fun currentAccessToken(): String? {
        return sessionState.value?.accessToken?.takeIf { it.isNotBlank() }
            ?: runCatching { SessionManager.getAuthToken() }
                .getOrNull()
                ?.takeIf { it.isNotBlank() }
    }

    private fun currentAuthorizationHeader(): String? {
        return currentAccessToken()?.let { token ->
            if (token.startsWith("Bearer ", ignoreCase = true)) token
            else "Bearer $token"
        }
    }

    private fun clearLocalSession() {
        sessionMutex.tryLock().takeIf { it }?.let {
            try {
                sessionState.value = null
                currentUserState.value = null
                runCatching { SessionManager.clearSession() }
            } finally {
                sessionMutex.unlock()
            }
        } ?: run {
            sessionState.value = null
            currentUserState.value = null
            runCatching { SessionManager.clearSession() }
        }
    }

    private fun User.matchesIdentifier(identifier: String): Boolean {
        val normalizedIdentifier = identifier.normalizeEmail() ?: return false
        return email.normalizeEmail() == normalizedIdentifier ||
            username.normalizeEmail() == normalizedIdentifier
    }

    private fun String?.normalizeEmail(): String? {
        return this
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?.lowercase()
    }
}