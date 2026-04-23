package com.myimdad_por.ui.features.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myimdad_por.core.base.UiState
import com.myimdad_por.core.utils.ValidationUtils
import com.myimdad_por.domain.model.Role
import com.myimdad_por.domain.model.User
import com.myimdad_por.domain.repository.AuthRepository
import com.myimdad_por.domain.repository.EmployeeRegistrationRequest
import com.myimdad_por.domain.repository.ManagerRegistrationRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    fun onFullNameChanged(value: String) = updateState {
        it.copy(fullName = value, errorMessage = null)
    }

    fun onUsernameChanged(value: String) = updateState {
        it.copy(username = value, errorMessage = null)
    }

    fun onEmailChanged(value: String) = updateState {
        it.copy(email = value, errorMessage = null)
    }

    fun onPhoneNumberChanged(value: String) = updateState {
        it.copy(phoneNumber = value, errorMessage = null)
    }

    fun onPasswordChanged(value: String) = updateState {
        it.copy(password = value, errorMessage = null)
    }

    fun onConfirmPasswordChanged(value: String) = updateState {
        it.copy(confirmPassword = value, errorMessage = null)
    }

    fun onRoleSelected(role: Role) = updateState {
        it.copy(
            selectedRole = role,
            errorMessage = null,
            registrationState = UiState.Idle
        )
    }

    fun togglePasswordVisibility() = updateState {
        it.copy(isPasswordVisible = !it.isPasswordVisible)
    }

    fun toggleConfirmPasswordVisibility() = updateState {
        it.copy(isConfirmPasswordVisible = !it.isConfirmPasswordVisible)
    }

    fun clearError() = updateState {
        it.copy(errorMessage = null, registrationState = UiState.Idle)
    }

    fun validate(): Boolean {
        val error = validateInput(_uiState.value)
        updateState {
            it.copy(
                errorMessage = error,
                registrationState = if (error == null) UiState.Idle else UiState.Error(error)
            )
        }
        return error == null
    }

    fun submit() {
        val current = _uiState.value
        if (current.isSubmitting) return

        val validationError = validateInput(current)
        if (validationError != null) {
            updateState {
                it.copy(
                    errorMessage = validationError,
                    registrationState = UiState.Error(validationError)
                )
            }
            return
        }

        val role = current.selectedRole
        if (role == null) {
            updateState {
                it.copy(
                    errorMessage = "يرجى اختيار نوع الحساب",
                    registrationState = UiState.Error("يرجى اختيار نوع الحساب")
                )
            }
            return
        }

        val normalizedEmail = normalizeEmail(current.email)

        viewModelScope.launch {
            updateState {
                it.copy(
                    isSubmitting = true,
                    errorMessage = null,
                    registrationState = UiState.Loading
                )
            }

            val result = runCatching {
                when {
                    role.isManager() -> authRepository.registerManager(
                        ManagerRegistrationRequest(
                            fullName = current.fullName.trim(),
                            email = normalizedEmail,
                            storeName = resolveStoreName(current),
                            storeLocation = resolveStoreLocation(current),
                            password = current.password,
                            confirmPassword = current.confirmPassword
                        )
                    )

                    role.isEmployee() -> authRepository.registerEmployee(
                        EmployeeRegistrationRequest(
                            managerEmail = resolveManagerEmail(current),
                            employeeFullName = current.fullName.trim(),
                            employeeEmail = normalizedEmail,
                            password = current.password,
                            confirmPassword = current.confirmPassword
                        )
                    )

                    else -> throw IllegalStateException("نوع الحساب غير مدعوم: $role")
                }
            }.getOrElse { throwable ->
                updateState {
                    it.copy(
                        isSubmitting = false,
                        errorMessage = throwable.message ?: "حدث خطأ غير متوقع",
                        registrationState = UiState.Error(throwable.message ?: "حدث خطأ غير متوقع")
                    )
                }
                return@launch
            }

            result.fold(
                onSuccess = { user ->
                    onRegistrationSuccess(user)
                },
                onFailure = { throwable ->
                    val message = throwable.message ?: "فشل إنشاء الحساب"
                    updateState {
                        it.copy(
                            isSubmitting = false,
                            errorMessage = message,
                            registrationState = UiState.Error(message)
                        )
                    }
                }
            )
        }
    }

    private fun onRegistrationSuccess(user: User) {
        updateState {
            it.copy(
                isSubmitting = false,
                errorMessage = null,
                registrationState = UiState.Success(user)
            )
        }
    }

    private fun validateInput(state: RegisterUiState): String? {
        if (!ValidationUtils.isValidName(state.fullName)) return "الاسم الكامل غير صالح"
        if (!ValidationUtils.isValidEmail(state.email)) return "البريد الإلكتروني غير صالح"
        if (state.phoneNumber.isNotBlank() && !ValidationUtils.isValidPhone(state.phoneNumber)) {
            return "رقم الهاتف غير صالح"
        }
        if (state.password.isBlank()) return "كلمة المرور مطلوبة"
        if (state.password.length < 6) return "كلمة المرور يجب أن تكون 6 أحرف على الأقل"
        if (!ValidationUtils.isSamePassword(state.password, state.confirmPassword)) {
            return "كلمة المرور وتأكيدها غير متطابقين"
        }
        if (state.selectedRole == null) return "يرجى اختيار نوع الحساب"
        return null
    }

    private fun resolveManagerEmail(state: RegisterUiState): String {
        val candidate = normalizeEmail(state.username)
        return if (ValidationUtils.isValidEmail(candidate)) candidate else normalizeEmail(state.email)
    }

    private fun resolveStoreName(state: RegisterUiState): String {
        return state.username.trim().ifBlank { state.fullName.trim() }
    }

    private fun resolveStoreLocation(state: RegisterUiState): String {
        return state.phoneNumber.trim().ifBlank { "غير محدد" }
    }

    private fun normalizeEmail(value: String): String {
        return ValidationUtils.normalizeText(value)
    }

    private fun updateState(reducer: (RegisterUiState) -> RegisterUiState) {
        _uiState.update(reducer)
    }
}
