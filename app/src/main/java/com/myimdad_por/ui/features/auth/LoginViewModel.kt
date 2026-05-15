package com.myimdad_por.ui.features.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myimdad_por.core.base.UiState
import com.myimdad_por.domain.repository.AuthRepository
import com.myimdad_por.domain.repository.LoginRequest
import com.myimdad_por.domain.usecase.auth.ValidateEmailRequest
import com.myimdad_por.domain.usecase.auth.ValidateEmailUseCase
import com.myimdad_por.domain.usecase.auth.ValidatePasswordRequest
import com.myimdad_por.domain.usecase.auth.ValidatePasswordUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val validateEmailUseCase: ValidateEmailUseCase,
    private val validatePasswordUseCase: ValidatePasswordUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onEvent(event: LoginUiEvent) {
        when (event) {
            is LoginUiEvent.UsernameChanged -> onUsernameChanged(event.value)
            is LoginUiEvent.PasswordChanged -> onPasswordChanged(event.value)
            LoginUiEvent.TogglePasswordVisibility -> togglePasswordVisibility()
            LoginUiEvent.ToggleRememberMe -> toggleRememberMe()
            LoginUiEvent.Submit -> submit()
            LoginUiEvent.Validate -> validate()
            LoginUiEvent.NavigateToRegister -> Unit
            LoginUiEvent.NavigateToForgotPassword -> Unit
            LoginUiEvent.ClearError -> clearError()
        }
    }

    private fun onUsernameChanged(value: String) {
        _uiState.value = _uiState.value.onUsernameChanged(value)
    }

    private fun onPasswordChanged(value: String) {
        _uiState.value = _uiState.value.onPasswordChanged(value)
    }

    private fun togglePasswordVisibility() {
        _uiState.value = _uiState.value.togglePasswordVisibility()
    }

    private fun toggleRememberMe() {
        _uiState.value = _uiState.value.toggleRememberMe()
    }

    private fun validate(): Boolean {
        val current = _uiState.value
        val baseValidated = current.validate()

        val emailValidation = validateEmailUseCase(
            ValidateEmailRequest(
                email = baseValidated.username,
                normalize = true,
                allowBlank = false
            )
        )

        val passwordValidation = validatePasswordUseCase(
            ValidatePasswordRequest(
                password = baseValidated.password,
                minLength = 6,
                requireUppercase = false,
                requireLowercase = false,
                requireDigit = false,
                requireSpecialCharacter = false,
                allowSpaces = false
            )
        )

        val updated = baseValidated.copy(
            usernameError = emailValidation.reason?.let {
                if (baseValidated.username.isBlank()) "اسم المستخدم مطلوب" else "البريد الإلكتروني غير صحيح"
            },
            passwordError = passwordValidation.violations.firstOrNull()?.let { violation ->
                when {
                    violation.contains("blank", ignoreCase = true) -> "كلمة المرور مطلوبة"
                    violation.contains("short", ignoreCase = true) -> "كلمة المرور يجب أن تكون 6 أحرف على الأقل"
                    else -> "كلمة المرور غير صحيحة"
                }
            },
            state = if (emailValidation.isValid && passwordValidation.isValid) UiState.Idle else UiState.Error("تحقق من البيانات المدخلة")
        )

        _uiState.value = updated
        return emailValidation.isValid && passwordValidation.isValid
    }

    private fun submit() {
        if (_uiState.value.isLoading) return

        val isValid = validate()
        if (!isValid) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(state = UiState.Loading)

            val state = _uiState.value
            val email = validateEmailUseCase(
                ValidateEmailRequest(email = state.username, normalize = true, allowBlank = false)
            ).normalizedEmail

            val result = runCatching {
                authRepository.login(
                    LoginRequest(
                        email = email,
                        password = state.password
                    )
                )
            }.getOrElse { throwable ->
                _uiState.value = _uiState.value.copy(
                    state = UiState.Error(
                        message = throwable.message ?: "حدث خطأ غير متوقع"
                    )
                )
                return@launch
            }

            result.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        state = UiState.Success(Unit),
                        usernameError = null,
                        passwordError = null
                    )
                },
                onFailure = { throwable ->
                    _uiState.value = _uiState.value.copy(
                        state = UiState.Error(
                            message = throwable.message ?: "فشل تسجيل الدخول"
                        )
                    )
                }
            )
        }
    }

    private fun clearError() {
        val current = _uiState.value
        _uiState.value = current.copy(
            state = UiState.Idle,
            usernameError = null,
            passwordError = null
        )
    }
}