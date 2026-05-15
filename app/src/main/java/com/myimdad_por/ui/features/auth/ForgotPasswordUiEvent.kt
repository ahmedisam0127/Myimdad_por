package com.myimdad_por.ui.features.auth

sealed interface ForgotPasswordUiEvent {
    data class IdentifierChanged(val value: String) : ForgotPasswordUiEvent
    data class OtpCodeChanged(val value: String) : ForgotPasswordUiEvent
    data class NewPasswordChanged(val value: String) : ForgotPasswordUiEvent
    data class ConfirmPasswordChanged(val value: String) : ForgotPasswordUiEvent

    data object SubmitIdentifier : ForgotPasswordUiEvent
    data object SubmitOtpCode : ForgotPasswordUiEvent
    data object SubmitNewPassword : ForgotPasswordUiEvent

    data object GoBack : ForgotPasswordUiEvent
    data object ClearError : ForgotPasswordUiEvent
    data object Validate : ForgotPasswordUiEvent
    data object ResetForm : ForgotPasswordUiEvent
}