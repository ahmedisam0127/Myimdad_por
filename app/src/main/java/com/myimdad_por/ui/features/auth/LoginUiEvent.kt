package com.myimdad_por.ui.features.auth

sealed interface LoginUiEvent {

    // =========================
    // Input Changes
    // =========================
    data class UsernameChanged(val value: String) : LoginUiEvent
    data class PasswordChanged(val value: String) : LoginUiEvent

    // =========================
    // UI Actions
    // =========================
    data object TogglePasswordVisibility : LoginUiEvent
    data object ToggleRememberMe : LoginUiEvent

    // =========================
    // Actions
    // =========================
    data object Submit : LoginUiEvent
    data object Validate : LoginUiEvent

    // =========================
    // Side Effects / Navigation
    // =========================
    data object NavigateToRegister : LoginUiEvent
    data object NavigateToForgotPassword : LoginUiEvent

    // =========================
    // Error Handling
    // =========================
    data object ClearError : LoginUiEvent
}