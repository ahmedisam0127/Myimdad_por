package com.myimdad_por.domain.usecase.auth

import com.myimdad_por.core.utils.ValidationUtils
import java.util.Locale
import javax.inject.Inject

data class ValidateEmailRequest(
    val email: String?,
    val normalize: Boolean = true,
    val maxLength: Int = 254,
    val allowBlank: Boolean = false
) {
    init {
        require(maxLength > 0) { "maxLength must be greater than zero." }
    }
}

data class ValidateEmailResult(
    val originalEmail: String?,
    val normalizedEmail: String,
    val isValid: Boolean,
    val reason: String? = null
) {
    val isBlank: Boolean
        get() = normalizedEmail.isBlank()
}

class ValidateEmailUseCase @Inject constructor() {

    operator fun invoke(request: ValidateEmailRequest): ValidateEmailResult {
        val original = request.email
        val trimmed = original?.trim().orEmpty()
        val normalized = if (request.normalize) {
            trimmed.lowercase(Locale.ROOT)
        } else {
            trimmed
        }

        if (trimmed.isBlank()) {
            return ValidateEmailResult(
                originalEmail = original,
                normalizedEmail = normalized,
                isValid = request.allowBlank,
                reason = if (request.allowBlank) null else "email is blank"
            )
        }

        if (normalized.length > request.maxLength) {
            return ValidateEmailResult(
                originalEmail = original,
                normalizedEmail = normalized,
                isValid = false,
                reason = "email exceeds maximum length"
            )
        }

        if (!ValidationUtils.isValidEmail(normalized)) {
            return ValidateEmailResult(
                originalEmail = original,
                normalizedEmail = normalized,
                isValid = false,
                reason = "email format is invalid"
            )
        }

        return ValidateEmailResult(
            originalEmail = original,
            normalizedEmail = normalized,
            isValid = true,
            reason = null
        )
    }

    fun isValid(email: String?): Boolean {
        return invoke(ValidateEmailRequest(email = email)).isValid
    }
}