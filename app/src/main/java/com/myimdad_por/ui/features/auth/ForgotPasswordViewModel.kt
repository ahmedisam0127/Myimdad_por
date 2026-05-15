package com.myimdad_por.ui.features.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myimdad_por.core.base.UiState
import com.myimdad_por.domain.usecase.auth.ConfirmNewPasswordRequest
import com.myimdad_por.domain.usecase.auth.ConfirmNewPasswordUseCase
import com.myimdad_por.domain.usecase.auth.RequestPasswordResetRequest
import com.myimdad_por.domain.usecase.auth.RequestPasswordResetUseCase
import com.myimdad_por.domain.usecase.auth.VerifyResetCodeRequest
import com.myimdad_por.domain.usecase.auth.VerifyResetCodeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class ForgotPasswordViewModel @Inject constructor(
    private val requestPasswordResetUseCase: RequestPasswordResetUseCase,
    private val verifyResetCodeUseCase: VerifyResetCodeUseCase,
    private val confirmNewPasswordUseCase: ConfirmNewPasswordUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ForgotPasswordUiState())
    val uiState: StateFlow<ForgotPasswordUiState> = _uiState.asStateFlow()

    fun onEvent(event: ForgotPasswordUiEvent) {
        when (event) {
            is ForgotPasswordUiEvent.IdentifierChanged -> updateState {
                it.onIdentifierChanged(event.value)
            }

            is ForgotPasswordUiEvent.OtpCodeChanged -> updateState {
                it.onOtpCodeChanged(event.value)
            }

            is ForgotPasswordUiEvent.NewPasswordChanged -> updateState {
                it.onNewPasswordChanged(event.value)
            }

            is ForgotPasswordUiEvent.ConfirmPasswordChanged -> updateState {
                it.onConfirmPasswordChanged(event.value)
            }

            ForgotPasswordUiEvent.SubmitIdentifier -> submitIdentifier()
            ForgotPasswordUiEvent.SubmitOtpCode -> submitOtpCode()
            ForgotPasswordUiEvent.SubmitNewPassword -> submitNewPassword()
            ForgotPasswordUiEvent.GoBack -> goBack()
            ForgotPasswordUiEvent.ClearError -> updateState { it.clearError() }
            ForgotPasswordUiEvent.Validate -> validateCurrentStep()
            ForgotPasswordUiEvent.ResetForm -> resetForm()
        }
    }

    private fun submitIdentifier() {
        val current = _uiState.value.validateIdentifier()
        if (current.identifierError != null) {
            updateState { current }
            return
        }

        updateState { current.setLoading() }

        viewModelScope.launch {
            runCatching {
                requestPasswordResetUseCase(
                    RequestPasswordResetRequest(
                        email = current.identifier,
                        normalizeEmail = true,
                        requireExistingAccount = false
                    )
                ).getOrThrow()
            }.onSuccess { result ->
                updateState {
                    it.copy(
                        identifier = result.email,
                        step = ForgotPasswordStep.EnterOtp,
                        state = UiState.Idle,
                        identifierError = null,
                        otpCodeError = null
                    )
                }
            }.onFailure { throwable ->
                updateState {
                    it.setError(throwable.message ?: "تعذر إرسال رمز التحقق")
                }
            }
        }
    }

    private fun submitOtpCode() {
        val current = _uiState.value.validateOtpCode()
        if (current.otpCodeError != null) {
            updateState { current }
            return
        }

        val verification = verifyResetCodeUseCase(
            VerifyResetCodeRequest(
                resetCode = current.otpCode,
                digitsOnly = true,
                allowWhitespace = false,
                normalizeArabicDigits = true
            )
        )

        if (!verification.isValid) {
            updateState {
                it.copy(
                    otpCodeError = verification.reason ?: "رمز التحقق غير صحيح",
                    state = UiState.Error(message = verification.reason ?: "تحقق من رمز التحقق")
                )
            }
            return
        }

        updateState {
            it.copy(
                otpCode = verification.normalizedCode,
                step = ForgotPasswordStep.ResetPassword,
                state = UiState.Idle,
                otpCodeError = null,
                newPasswordError = null,
                confirmPasswordError = null
            )
        }
    }

    private fun submitNewPassword() {
        val current = _uiState.value.validateNewPassword()
        if (current.newPasswordError != null || current.confirmPasswordError != null) {
            updateState { current }
            return
        }

        updateState { current.setLoading() }

        viewModelScope.launch {
            runCatching {
                confirmNewPasswordUseCase(
                    ConfirmNewPasswordRequest(
                        email = current.identifier,
                        resetCode = current.otpCode,
                        newPassword = current.newPassword,
                        confirmNewPassword = current.confirmPassword,
                        normalizeEmail = true
                    )
                ).getOrThrow()
            }.onSuccess {
                updateState {
                    it.copy(
                        newPassword = "",
                        confirmPassword = "",
                        step = ForgotPasswordStep.Success,
                        state = UiState.Success(Unit),
                        newPasswordError = null,
                        confirmPasswordError = null
                    )
                }
            }.onFailure { throwable ->
                updateState {
                    it.setError(throwable.message ?: "تعذر تغيير كلمة المرور")
                }
            }
        }
    }

    private fun validateCurrentStep() {
        updateState {
            when (it.step) {
                ForgotPasswordStep.EnterIdentifier -> it.validateIdentifier()
                ForgotPasswordStep.EnterOtp -> it.validateOtpCode()
                ForgotPasswordStep.ResetPassword -> it.validateNewPassword()
                ForgotPasswordStep.Success -> it
            }
        }
    }

    private fun goBack() {
        updateState { state ->
            when (state.step) {
                ForgotPasswordStep.EnterIdentifier -> state
                ForgotPasswordStep.EnterOtp -> state.copy(
                    step = ForgotPasswordStep.EnterIdentifier,
                    state = UiState.Idle,
                    otpCode = "",
                    otpCodeError = null
                )

                ForgotPasswordStep.ResetPassword -> state.copy(
                    step = ForgotPasswordStep.EnterOtp,
                    state = UiState.Idle,
                    newPassword = "",
                    confirmPassword = "",
                    newPasswordError = null,
                    confirmPasswordError = null
                )

                ForgotPasswordStep.Success -> state.copy(
                    step = ForgotPasswordStep.ResetPassword,
                    state = UiState.Idle
                )
            }
        }
    }

    private fun resetForm() {
        updateState { ForgotPasswordUiState() }
    }

    private fun updateState(reducer: (ForgotPasswordUiState) -> ForgotPasswordUiState) {
        _uiState.update(reducer)
    }
}
