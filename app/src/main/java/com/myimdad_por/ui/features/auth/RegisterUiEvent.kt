package com.myimdad_por.ui.features.auth

import com.myimdad_por.domain.model.Role

sealed interface RegisterUiEvent {

    data class OnFullNameChanged(
        val value: String
    ) : RegisterUiEvent

    data class OnUsernameChanged(
        val value: String
    ) : RegisterUiEvent

    data class OnEmailChanged(
        val value: String
    ) : RegisterUiEvent

    data class OnPhoneNumberChanged(
        val value: String
    ) : RegisterUiEvent

    data class OnPasswordChanged(
        val value: String
    ) : RegisterUiEvent

    data class OnConfirmPasswordChanged(
        val value: String
    ) : RegisterUiEvent

    data class OnRoleSelected(
        val role: Role
    ) : RegisterUiEvent

    data object TogglePasswordVisibility : RegisterUiEvent

    data object ToggleConfirmPasswordVisibility : RegisterUiEvent

    data object SubmitRegistration : RegisterUiEvent

    data object ClearError : RegisterUiEvent

    // أحداث عامة يمكن إرسالها مباشرة لـ UiEvent
    data class ShowMessage(val message: String) : RegisterUiEvent
    data class ShowError(val message: String) : RegisterUiEvent
    data object NavigateBack : RegisterUiEvent
}