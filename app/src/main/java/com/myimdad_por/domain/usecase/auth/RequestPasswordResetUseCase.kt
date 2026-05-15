package com.myimdad_por.domain.usecase.auth

import com.myimdad_por.core.utils.ValidationUtils
import com.myimdad_por.domain.repository.AuthRepository
import com.myimdad_por.domain.repository.PasswordResetRequest
import javax.inject.Inject

data class RequestPasswordResetRequest(
    val email: String?,
    val normalizeEmail: Boolean = true,
    val requireExistingAccount: Boolean = false
)

data class RequestPasswordResetResult(
    val email: String,
    val requested: Boolean,
    val accountExists: Boolean? = null,
    val note: String? = null
) {
    val isAccepted: Boolean
        get() = requested
}

class RequestPasswordResetUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {

    suspend operator fun invoke(request: RequestPasswordResetRequest): Result<RequestPasswordResetResult> {
        return runCatching {
            val normalizedEmail = normalizeEmail(request.email, request.normalizeEmail)

            require(ValidationUtils.isValidEmail(normalizedEmail)) {
                "email is invalid"
            }

            val accountExists = if (request.requireExistingAccount) {
                authRepository.getUserByEmail(normalizedEmail) != null
            } else {
                null
            }

            if (request.requireExistingAccount && accountExists == false) {
                return@runCatching RequestPasswordResetResult(
                    email = normalizedEmail,
                    requested = false,
                    accountExists = false,
                    note = "no account found for this email"
                )
            }

            authRepository.requestPasswordReset(
                PasswordResetRequest(email = normalizedEmail)
            ).getOrThrow()

            RequestPasswordResetResult(
                email = normalizedEmail,
                requested = true,
                accountExists = accountExists,
                note = "password reset request accepted"
            )
        }
    }

    private fun normalizeEmail(email: String?, normalize: Boolean): String {
        val value = email?.trim().orEmpty()
        return if (normalize) value.lowercase() else value
    }
}