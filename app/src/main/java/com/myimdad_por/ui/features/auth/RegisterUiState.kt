package com.myimdad_por.ui.features.auth

import com.myimdad_por.core.base.UiState
import com.myimdad_por.core.utils.ValidationUtils
import com.myimdad_por.domain.model.Role
import com.myimdad_por.domain.model.User

data class RegisterUiState(
    val fullName: String = "",
    val username: String = "",
    val email: String = "",
    val phoneNumber: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val selectedRole: Role? = null,

    val isPasswordVisible: Boolean = false,
    val isConfirmPasswordVisible: Boolean = false,

    val isSubmitting: Boolean = false,

    val registrationState: UiState<User> = UiState.Idle,

    val errorMessage: String? = null
) {

    val isFullNameValid: Boolean
        get() = ValidationUtils.isValidName(fullName)

    val isEmailValid: Boolean
        get() = ValidationUtils.isValidEmail(email)

    val isPhoneValid: Boolean
        get() = ValidationUtils.isValidPhone(phoneNumber)

    val isPasswordValid: Boolean
        get() = ValidationUtils.isValidPassword(password)

    val isConfirmPasswordValid: Boolean
        get() = ValidationUtils.isSamePassword(password, confirmPassword)

    val isRoleSelected: Boolean
        get() = selectedRole != null

    val isFormValid: Boolean
        get() = isFullNameValid &&
                isEmailValid &&
                isPhoneValid &&
                isPasswordValid &&
                isConfirmPasswordValid &&
                isRoleSelected
}