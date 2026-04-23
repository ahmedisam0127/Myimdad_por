package com.myimdad_por.domain.usecase.auth

import com.myimdad_por.core.utils.ValidationUtils
import javax.inject.Inject

enum class PasswordStrength {
    VERY_WEAK,
    WEAK,
    FAIR,
    GOOD,
    STRONG,
    EXCELLENT
}

data class ValidatePasswordRequest(
    val password: String?,
    val confirmPassword: String? = null,
    val minLength: Int = 8,
    val maxLength: Int = 128,
    val requireUppercase: Boolean = true,
    val requireLowercase: Boolean = true,
    val requireDigit: Boolean = true,
    val requireSpecialCharacter: Boolean = false,
    val allowSpaces: Boolean = false
) {
    init {
        require(minLength > 0) { "minLength must be greater than zero." }
        require(maxLength >= minLength) { "maxLength must be greater than or equal to minLength." }
    }
}

data class ValidatePasswordResult(
    val originalPassword: String?,
    val normalizedPassword: String,
    val isValid: Boolean,
    val strength: PasswordStrength,
    val violations: List<String> = emptyList()
) {
    val hasViolations: Boolean
        get() = violations.isNotEmpty()

    val isStrongEnough: Boolean
        get() = strength == PasswordStrength.STRONG || strength == PasswordStrength.EXCELLENT
}

class ValidatePasswordUseCase @Inject constructor() {

    operator fun invoke(request: ValidatePasswordRequest): ValidatePasswordResult {
        val rawPassword = request.password
        val normalizedPassword = rawPassword?.trim().orEmpty()
        val violations = mutableListOf<String>()

        if (!ValidationUtils.isNotEmpty(rawPassword)) {
            violations += "password is blank"
        } else {
            if (normalizedPassword.length < request.minLength) {
                violations += "password is too short"
            }
            if (normalizedPassword.length > request.maxLength) {
                violations += "password is too long"
            }
            if (!request.allowSpaces && normalizedPassword.any(Char::isWhitespace)) {
                violations += "password must not contain spaces"
            }
            if (request.requireUppercase && normalizedPassword.none(Char::isUpperCase)) {
                violations += "password must contain an uppercase letter"
            }
            if (request.requireLowercase && normalizedPassword.none(Char::isLowerCase)) {
                violations += "password must contain a lowercase letter"
            }
            if (request.requireDigit && normalizedPassword.none(Char::isDigit)) {
                violations += "password must contain a digit"
            }
            if (request.requireSpecialCharacter && normalizedPassword.none(::isSpecialCharacter)) {
                violations += "password must contain a special character"
            }
        }

        if (request.confirmPassword != null &&
            !ValidationUtils.isSamePassword(rawPassword, request.confirmPassword)
        ) {
            violations += "password confirmation does not match"
        }

        val strength = computeStrength(
            password = normalizedPassword,
            minLength = request.minLength,
            allowSpaces = request.allowSpaces
        )

        return ValidatePasswordResult(
            originalPassword = rawPassword,
            normalizedPassword = normalizedPassword,
            isValid = violations.isEmpty(),
            strength = if (violations.isEmpty()) strength else PasswordStrength.VERY_WEAK,
            violations = violations
        )
    }

    fun isValid(password: String?): Boolean {
        return invoke(ValidatePasswordRequest(password = password)).isValid
    }

    private fun computeStrength(
        password: String,
        minLength: Int,
        allowSpaces: Boolean
    ): PasswordStrength {
        if (password.isBlank()) return PasswordStrength.VERY_WEAK

        var score = 0

        when {
            password.length >= minLength + 8 -> score += 2
            password.length >= minLength + 4 -> score += 1
        }

        if (password.any(Char::isLowerCase)) score += 1
        if (password.any(Char::isUpperCase)) score += 1
        if (password.any(Char::isDigit)) score += 1
        if (password.any(::isSpecialCharacter)) score += 1
        if (allowSpaces || password.none(Char::isWhitespace)) score += 1

        return when (score) {
            0, 1 -> PasswordStrength.VERY_WEAK
            2 -> PasswordStrength.WEAK
            3 -> PasswordStrength.FAIR
            4 -> PasswordStrength.GOOD
            5 -> PasswordStrength.STRONG
            else -> PasswordStrength.EXCELLENT
        }
    }

    private fun isSpecialCharacter(ch: Char): Boolean {
        return !ch.isLetterOrDigit() && !ch.isWhitespace()
    }
}