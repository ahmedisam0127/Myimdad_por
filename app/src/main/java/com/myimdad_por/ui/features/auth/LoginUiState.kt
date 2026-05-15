package com.myimdad_por.ui.features.auth

import com.myimdad_por.core.base.UiState

data class LoginUiState(

    // =========================
    // Inputs
    // =========================
    val username: String = "",
    val password: String = "",

    // =========================
    // UI التحكم
    // =========================
    val isPasswordVisible: Boolean = false,
    val rememberMe: Boolean = false,

    // =========================
    // الحالة العامة
    // =========================
    val state: UiState<Unit> = UiState.Idle,

    // =========================
    // Validation
    // =========================
    val usernameError: String? = null,
    val passwordError: String? = null

) {

    // =========================
    // Derived State (بدون extensions)
    // =========================

    val isLoading: Boolean
        get() = state is UiState.Loading

    val isSuccess: Boolean
        get() = state is UiState.Success

    val isError: Boolean
        get() = state is UiState.Error

    val errorMessage: String?
        get() = (state as? UiState.Error)?.message

    val isFormValid: Boolean
        get() = username.isNotBlank() && password.isNotBlank()

    val canSubmit: Boolean
        get() = isFormValid && !isLoading

    // =========================
    // Actions (Reducers Style)
    // =========================

    fun onUsernameChanged(value: String): LoginUiState {
        return copy(
            username = value,
            usernameError = null,
            state = UiState.Idle
        )
    }

    fun onPasswordChanged(value: String): LoginUiState {
        return copy(
            password = value,
            passwordError = null,
            state = UiState.Idle
        )
    }

    fun togglePasswordVisibility(): LoginUiState {
        return copy(isPasswordVisible = !isPasswordVisible)
    }

    fun toggleRememberMe(): LoginUiState {
        return copy(rememberMe = !rememberMe)
    }

    fun setLoading(): LoginUiState {
        return copy(state = UiState.Loading)
    }

    fun setSuccess(): LoginUiState {
        return copy(state = UiState.Success(Unit))
    }

    fun setError(message: String): LoginUiState {
        return copy(state = UiState.Error(message = message))
    }

    fun validate(): LoginUiState {
        val usernameError = when {
            username.isBlank() -> "اسم المستخدم مطلوب"
            username.length < 3 -> "اسم المستخدم قصير جداً"
            else -> null
        }

        val passwordError = when {
            password.isBlank() -> "كلمة المرور مطلوبة"
            password.length < 6 -> "كلمة المرور يجب أن تكون 6 أحرف على الأقل"
            else -> null
        }

        return copy(
            usernameError = usernameError,
            passwordError = passwordError
        )
    }
}