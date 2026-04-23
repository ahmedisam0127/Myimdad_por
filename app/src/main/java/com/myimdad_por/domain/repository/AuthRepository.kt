package com.myimdad_por.domain.repository

import com.myimdad_por.domain.model.Role
import com.myimdad_por.domain.model.User
import kotlinx.coroutines.flow.Flow

/**
 * Authentication and account lifecycle contract.
 *
 * Rules:
 * - Login is via email + password.
 * - Only one manager account is allowed per email.
 * - Employee accounts must always belong to a manager.
 * - An employee cannot create another employee account.
 */
interface AuthRepository {

    fun observeSession(): Flow<AuthSession?>

    suspend fun login(request: LoginRequest): Result<AuthSession>

    suspend fun logout(): Result<Unit>

    suspend fun getCurrentUser(): User?

    suspend fun refreshCurrentUser(): Result<User>

    suspend fun registerManager(request: ManagerRegistrationRequest): Result<User>

    suspend fun registerEmployee(request: EmployeeRegistrationRequest): Result<User>

    suspend fun changePassword(request: ChangePasswordRequest): Result<Unit>

    suspend fun requestPasswordReset(request: PasswordResetRequest): Result<Unit>

    suspend fun confirmPasswordReset(request: ConfirmPasswordResetRequest): Result<Unit>

    suspend fun isManagerEmailAvailable(email: String): Boolean

    suspend fun isEmployeeEmailAvailable(
        managerEmail: String,
        employeeEmail: String
    ): Boolean

    suspend fun hasManagerAccount(email: String): Boolean

    suspend fun hasEmployeeAccount(
        managerEmail: String,
        employeeEmail: String
    ): Boolean

    suspend fun getUserByEmail(email: String): User?

    suspend fun getUsersByRole(role: Role): List<User>
}

/**
 * Signed-in session snapshot.
 */
data class AuthSession(
    val user: User,
    val accessToken: String,
    val refreshToken: String? = null,
    val expiresAtMillis: Long? = null,
    val issuedAtMillis: Long = System.currentTimeMillis()
) {
    init {
        require(accessToken.isNotBlank()) { "accessToken cannot be blank." }
        expiresAtMillis?.let {
            require(it > 0L) { "expiresAtMillis must be greater than zero when provided." }
        }
    }

    val isExpired: Boolean
        get() = expiresAtMillis?.let { System.currentTimeMillis() > it } == true
}

data class LoginRequest(
    val email: String,
    val password: String
) {
    init {
        require(email.isNotBlank()) { "email cannot be blank." }
        require(password.isNotBlank()) { "password cannot be blank." }
    }
}

data class ManagerRegistrationRequest(
    val fullName: String,
    val email: String,
    val storeName: String,
    val storeLocation: String,
    val password: String,
    val confirmPassword: String
) {
    init {
        require(fullName.isNotBlank()) { "fullName cannot be blank." }
        require(email.isNotBlank()) { "email cannot be blank." }
        require(storeName.isNotBlank()) { "storeName cannot be blank." }
        require(storeLocation.isNotBlank()) { "storeLocation cannot be blank." }
        require(password.isNotBlank()) { "password cannot be blank." }
        require(confirmPassword.isNotBlank()) { "confirmPassword cannot be blank." }
        require(password == confirmPassword) { "password and confirmPassword must match." }
    }
}

data class EmployeeRegistrationRequest(
    val managerEmail: String,
    val employeeFullName: String,
    val employeeEmail: String,
    val password: String,
    val confirmPassword: String
) {
    init {
        require(managerEmail.isNotBlank()) { "managerEmail cannot be blank." }
        require(employeeFullName.isNotBlank()) { "employeeFullName cannot be blank." }
        require(employeeEmail.isNotBlank()) { "employeeEmail cannot be blank." }
        require(password.isNotBlank()) { "password cannot be blank." }
        require(confirmPassword.isNotBlank()) { "confirmPassword cannot be blank." }
        require(password == confirmPassword) { "password and confirmPassword must match." }
    }
}

data class ChangePasswordRequest(
    val userEmail: String,
    val currentPassword: String,
    val newPassword: String,
    val confirmNewPassword: String
) {
    init {
        require(userEmail.isNotBlank()) { "userEmail cannot be blank." }
        require(currentPassword.isNotBlank()) { "currentPassword cannot be blank." }
        require(newPassword.isNotBlank()) { "newPassword cannot be blank." }
        require(confirmNewPassword.isNotBlank()) { "confirmNewPassword cannot be blank." }
        require(newPassword == confirmNewPassword) { "newPassword and confirmNewPassword must match." }
    }
}

data class PasswordResetRequest(
    val email: String
) {
    init {
        require(email.isNotBlank()) { "email cannot be blank." }
    }
}

data class ConfirmPasswordResetRequest(
    val email: String,
    val resetCode: String,
    val newPassword: String,
    val confirmNewPassword: String
) {
    init {
        require(email.isNotBlank()) { "email cannot be blank." }
        require(resetCode.isNotBlank()) { "resetCode cannot be blank." }
        require(newPassword.isNotBlank()) { "newPassword cannot be blank." }
        require(confirmNewPassword.isNotBlank()) { "confirmNewPassword cannot be blank." }
        require(newPassword == confirmNewPassword) { "newPassword and confirmNewPassword must match." }
    }
}