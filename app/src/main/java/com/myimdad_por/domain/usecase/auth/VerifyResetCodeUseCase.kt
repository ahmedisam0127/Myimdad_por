package com.myimdad_por.domain.usecase.auth

import com.myimdad_por.core.utils.ValidationUtils
import javax.inject.Inject

data class VerifyResetCodeRequest(
    val resetCode: String?,
    val expectedCode: String? = null,
    val minLength: Int = 4,
    val maxLength: Int = 8,
    val digitsOnly: Boolean = true,
    val allowWhitespace: Boolean = false,
    val normalizeArabicDigits: Boolean = true
) {
    init {
        require(minLength > 0) { "minLength must be greater than zero." }
        require(maxLength >= minLength) { "maxLength must be greater than or equal to minLength." }
    }
}

data class VerifyResetCodeResult(
    val inputCode: String,
    val normalizedCode: String,
    val isValid: Boolean,
    val matchesExpected: Boolean? = null,
    val reason: String? = null
) {
    val isVerified: Boolean
        get() = isValid && (matchesExpected ?: true)
}

class VerifyResetCodeUseCase @Inject constructor() {

    operator fun invoke(request: VerifyResetCodeRequest): VerifyResetCodeResult {
        val input = request.resetCode.orEmpty()
        val normalized = normalizeCode(input, request.normalizeArabicDigits)

        if (normalized.isBlank()) {
            return VerifyResetCodeResult(
                inputCode = input,
                normalizedCode = normalized,
                isValid = false,
                matchesExpected = null,
                reason = "reset code is blank"
            )
        }

        if (!request.allowWhitespace && input.any(Char::isWhitespace)) {
            return VerifyResetCodeResult(
                inputCode = input,
                normalizedCode = normalized,
                isValid = false,
                matchesExpected = null,
                reason = "reset code must not contain whitespace"
            )
        }

        if (normalized.length !in request.minLength..request.maxLength) {
            return VerifyResetCodeResult(
                inputCode = input,
                normalizedCode = normalized,
                isValid = false,
                matchesExpected = null,
                reason = "reset code length is invalid"
            )
        }

        if (request.digitsOnly && !ValidationUtils.isDigitsOnly(normalized)) {
            return VerifyResetCodeResult(
                inputCode = input,
                normalizedCode = normalized,
                isValid = false,
                matchesExpected = null,
                reason = "reset code must contain digits only"
            )
        }

        val expected = request.expectedCode?.let { normalizeCode(it, request.normalizeArabicDigits) }
        val matchesExpected = expected?.let { it == normalized }

        return VerifyResetCodeResult(
            inputCode = input,
            normalizedCode = normalized,
            isValid = true,
            matchesExpected = matchesExpected,
            reason = when {
                matchesExpected == false -> "reset code does not match"
                else -> null
            }
        )
    }

    private fun normalizeCode(value: String, normalizeArabicDigits: Boolean): String {
        val trimmed = value.trim()
        return if (normalizeArabicDigits) {
            ValidationUtils.normalizeNumbers(trimmed)
        } else {
            trimmed
        }
    }
}