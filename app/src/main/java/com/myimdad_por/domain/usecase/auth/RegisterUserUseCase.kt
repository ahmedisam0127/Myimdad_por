package com.myimdad_por.domain.usecase.auth

import com.myimdad_por.core.utils.ValidationUtils
import com.myimdad_por.domain.model.User
import com.myimdad_por.domain.repository.AuthRepository
import com.myimdad_por.domain.repository.EmployeeRegistrationRequest
import com.myimdad_por.domain.repository.ManagerRegistrationRequest
import javax.inject.Inject

sealed class RegisterUserRequest {
    data class Manager(
        val fullName: String?,
        val email: String?,
        val storeName: String?,
        val storeLocation: String?,
        val password: String?,
        val confirmPassword: String?
    ) : RegisterUserRequest()

    data class Employee(
        val managerEmail: String?,
        val employeeFullName: String?,
        val employeeEmail: String?,
        val password: String?,
        val confirmPassword: String?
    ) : RegisterUserRequest()
}

enum class RegistrationKind {
    MANAGER,
    EMPLOYEE
}

data class RegisterUserResult(
    val user: User,
    val kind: RegistrationKind,
    val normalizedEmail: String,
    val note: String? = null
)

class RegisterUserUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val validateEmailUseCase: ValidateEmailUseCase = ValidateEmailUseCase(),
    private val validatePasswordUseCase: ValidatePasswordUseCase = ValidatePasswordUseCase()
) {

    suspend operator fun invoke(request: RegisterUserRequest): Result<RegisterUserResult> {
        return when (request) {
            is RegisterUserRequest.Manager -> registerManager(request)
            is RegisterUserRequest.Employee -> registerEmployee(request)
        }
    }

    private suspend fun registerManager(
        request: RegisterUserRequest.Manager
    ): Result<RegisterUserResult> {
        return runCatching {
            val fullName = request.fullName?.trim().orEmpty()
            val storeName = request.storeName?.trim().orEmpty()
            val storeLocation = request.storeLocation?.trim().orEmpty()

            require(ValidationUtils.isValidName(fullName)) { "fullName is invalid" }
            require(ValidationUtils.isValidName(storeName)) { "storeName is invalid" }
            require(ValidationUtils.isValidName(storeLocation)) { "storeLocation is invalid" }

            val emailCheck = validateEmailUseCase(
                ValidateEmailRequest(email = request.email)
            )
            require(emailCheck.isValid) { emailCheck.reason ?: "email is invalid" }

            val passwordCheck = validatePasswordUseCase(
                ValidatePasswordRequest(
                    password = request.password,
                    confirmPassword = request.confirmPassword,
                    requireSpecialCharacter = false
                )
            )
            require(passwordCheck.isValid) {
                passwordCheck.violations.firstOrNull() ?: "password is invalid"
            }

            val user = authRepository.registerManager(
                ManagerRegistrationRequest(
                    fullName = fullName,
                    email = emailCheck.normalizedEmail,
                    storeName = storeName,
                    storeLocation = storeLocation,
                    password = request.password!!.trim(),
                    confirmPassword = request.confirmPassword!!.trim()
                )
            ).getOrThrow()

            RegisterUserResult(
                user = user,
                kind = RegistrationKind.MANAGER,
                normalizedEmail = emailCheck.normalizedEmail,
                note = "manager account created"
            )
        }
    }

    private suspend fun registerEmployee(
        request: RegisterUserRequest.Employee
    ): Result<RegisterUserResult> {
        return runCatching {
            val managerEmailCheck = validateEmailUseCase(
                ValidateEmailRequest(email = request.managerEmail)
            )
            require(managerEmailCheck.isValid) { managerEmailCheck.reason ?: "managerEmail is invalid" }

            val employeeName = request.employeeFullName?.trim().orEmpty()
            require(ValidationUtils.isValidName(employeeName)) {
                "employeeFullName is invalid"
            }

            val employeeEmailCheck = validateEmailUseCase(
                ValidateEmailRequest(email = request.employeeEmail)
            )
            require(employeeEmailCheck.isValid) { employeeEmailCheck.reason ?: "employeeEmail is invalid" }

            val passwordCheck = validatePasswordUseCase(
                ValidatePasswordRequest(
                    password = request.password,
                    confirmPassword = request.confirmPassword,
                    requireSpecialCharacter = false
                )
            )
            require(passwordCheck.isValid) {
                passwordCheck.violations.firstOrNull() ?: "password is invalid"
            }

            val user = authRepository.registerEmployee(
                EmployeeRegistrationRequest(
                    managerEmail = managerEmailCheck.normalizedEmail,
                    employeeFullName = employeeName,
                    employeeEmail = employeeEmailCheck.normalizedEmail,
                    password = request.password!!.trim(),
                    confirmPassword = request.confirmPassword!!.trim()
                )
            ).getOrThrow()

            RegisterUserResult(
                user = user,
                kind = RegistrationKind.EMPLOYEE,
                normalizedEmail = employeeEmailCheck.normalizedEmail,
                note = "employee account created"
            )
        }
    }
}