package com.myimdad_por.ui.features.auth

import com.myimdad_por.core.base.UiState

data class ForgotPasswordUiState(
    val identifier: String = "",
    val otpCode: String = "",
    val newPassword: String = "",
    val confirmPassword: String = "",
    val step: ForgotPasswordStep = ForgotPasswordStep.EnterIdentifier,
    val state: UiState<Unit> = UiState.Idle,
    val identifierError: String? = null,
    val otpCodeError: String? = null,
    val newPasswordError: String? = null,
    val confirmPasswordError: String? = null
) {
    val isLoading: Boolean
        get() = state is UiState.Loading

    val isSuccess: Boolean
        get() = state is UiState.Success

    val isError: Boolean
        get() = state is UiState.Error

    val errorMessage: String?
        get() = (state as? UiState.Error)?.message

    val isIdentifierStepValid: Boolean
        get() = identifier.isNotBlank()

    val isOtpStepValid: Boolean
        get() = otpCode.length >= 4

    val isResetStepValid: Boolean
        get() = newPassword.length >= 8 && newPassword == confirmPassword

    val canSubmit: Boolean
        get() = !isLoading && when (step) {
            ForgotPasswordStep.EnterIdentifier -> isIdentifierStepValid
            ForgotPasswordStep.EnterOtp -> isOtpStepValid
            ForgotPasswordStep.ResetPassword -> isResetStepValid
            ForgotPasswordStep.Success -> false
        }

    fun onIdentifierChanged(value: String): ForgotPasswordUiState {
        return copy(
            identifier = value,
            identifierError = null,
            state = UiState.Idle
        )
    }

    fun onOtpCodeChanged(value: String): ForgotPasswordUiState {
        return copy(
            otpCode = value,
            otpCodeError = null,
            state = UiState.Idle
        )
    }

    fun onNewPasswordChanged(value: String): ForgotPasswordUiState {
        return copy(
            newPassword = value,
            newPasswordError = null,
            confirmPasswordError = null,
            state = UiState.Idle
        )
    }

    fun onConfirmPasswordChanged(value: String): ForgotPasswordUiState {
        return copy(
            confirmPassword = value,
            confirmPasswordError = null,
            state = UiState.Idle
        )
    }

    fun goToOtpStep(): ForgotPasswordUiState {
        return copy(
            step = ForgotPasswordStep.EnterOtp,
            state = UiState.Idle
        )
    }

    fun goToResetStep(): ForgotPasswordUiState {
        return copy(
            step = ForgotPasswordStep.ResetPassword,
            state = UiState.Idle
        )
    }

    fun goToSuccessStep(): ForgotPasswordUiState {
        return copy(
            step = ForgotPasswordStep.Success,
            state = UiState.Success(Unit)
        )
    }

    fun setLoading(): ForgotPasswordUiState {
        return copy(state = UiState.Loading)
    }

    fun setSuccess(): ForgotPasswordUiState {
        return copy(state = UiState.Success(Unit))
    }

    fun setError(message: String): ForgotPasswordUiState {
        return copy(state = UiState.Error(message = message))
    }

    fun clearError(): ForgotPasswordUiState {
        return copy(
            state = UiState.Idle,
            identifierError = null,
            otpCodeError = null,
            newPasswordError = null,
            confirmPasswordError = null
        )
    }

    fun validateIdentifier(): ForgotPasswordUiState {
        val error = when {
            identifier.isBlank() -> "البريد الإلكتروني أو اسم المستخدم مطلوب"
            identifier.length < 3 -> "القيمة المدخلة قصيرة جداً"
            else -> null
        }

        return copy(
            identifierError = error,
            state = if (error == null) UiState.Idle else UiState.Error("تحقق من البيانات المدخلة")
        )
    }

    fun validateOtpCode(): ForgotPasswordUiState {
        val error = when {
            otpCode.isBlank() -> "رمز التحقق مطلوب"
            otpCode.length < 4 -> "رمز التحقق غير صحيح"
            else -> null
        }

        return copy(
            otpCodeError = error,
            state = if (error == null) UiState.Idle else UiState.Error("تحقق من رمز التحقق")
        )
    }

    fun validateNewPassword(): ForgotPasswordUiState {
        val passwordError = when {
            newPassword.isBlank() -> "كلمة المرور الجديدة مطلوبة"
            newPassword.length < 8 -> "كلمة المرور يجب أن تكون 8 أحرف على الأقل"
            else -> null
        }

        val confirmError = when {
            confirmPassword.isBlank() -> "تأكيد كلمة المرور مطلوب"
            newPassword.isNotBlank() && confirmPassword.isNotBlank() && newPassword != confirmPassword ->
                "كلمة المرور وتأكيدها غير متطابقتين"
            else -> null
        }

        return copy(
            newPasswordError = passwordError,
            confirmPasswordError = confirmError,
            state = if (passwordError == null && confirmError == null) UiState.Idle
            else UiState.Error("تحقق من كلمة المرور")
        )
    }

    fun resetErrors(): ForgotPasswordUiState {
        return copy(
            identifierError = null,
            otpCodeError = null,
            newPasswordError = null,
            confirmPasswordError = null
        )
    }
}

enum class ForgotPasswordStep {
    EnterIdentifier,
    EnterOtp,
    ResetPassword,
    Success
}