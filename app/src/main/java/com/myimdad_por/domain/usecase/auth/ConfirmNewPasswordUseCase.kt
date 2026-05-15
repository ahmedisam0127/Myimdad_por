package com.myimdad_por.domain.usecase.auth

import com.myimdad_por.core.utils.ValidationUtils
import com.myimdad_por.domain.repository.AuthRepository
import com.myimdad_por.domain.repository.ConfirmPasswordResetRequest
import javax.inject.Inject

data class ConfirmNewPasswordRequest(
    val email: String?,
    val resetCode: String?,
    val newPassword: String?,
    val confirmNewPassword: String?,
    val normalizeEmail: Boolean = true
)

data class ConfirmNewPasswordResult(
    val email: String,
    val resetCode: String,
    val passwordChanged: Boolean,
    val note: String? = null
) {
    val isSuccessful: Boolean
        get() = passwordChanged
}

class ConfirmNewPasswordUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val verifyResetCodeUseCase: VerifyResetCodeUseCase = VerifyResetCodeUseCase(),
    private val validatePasswordUseCase: ValidatePasswordUseCase = ValidatePasswordUseCase()
) {

    suspend operator fun invoke(request: ConfirmNewPasswordRequest): Result<ConfirmNewPasswordResult> {
        return runCatching {
            val normalizedEmail = normalizeEmail(request.email, request.normalizeEmail)
            require(ValidationUtils.isValidEmail(normalizedEmail)) { "email is invalid" }

            val codeCheck = verifyResetCodeUseCase(
                VerifyResetCodeRequest(
                    resetCode = request.resetCode
                )
            )
            require(codeCheck.isVerified) {
                codeCheck.reason ?: "reset code is invalid"
            }

            val passwordCheck = validatePasswordUseCase(
                ValidatePasswordRequest(
                    password = request.newPassword,
                    confirmPassword = request.confirmNewPassword
                )
            )
            require(passwordCheck.isValid) {
                passwordCheck.violations.firstOrNull() ?: "password is invalid"
            }

            authRepository.confirmPasswordReset(
                ConfirmPasswordResetRequest(
                    email = normalizedEmail,
                    resetCode = codeCheck.normalizedCode,
                    newPassword = request.newPassword!!.trim(),
                    confirmNewPassword = request.confirmNewPassword!!.trim()
                )
            ).getOrThrow()

            ConfirmNewPasswordResult(
                email = normalizedEmail,
                resetCode = codeCheck.normalizedCode,
                passwordChanged = true,
                note = "password reset completed successfully"
            )
        }
    }

    private fun normalizeEmail(email: String?, normalize: Boolean): String {
        val value = email?.trim().orEmpty()
        return if (normalize) value.lowercase() else value
    }
}