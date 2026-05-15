package com.myimdad_por.ui.features.subscription

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myimdad_por.domain.model.SubscriptionFeature
import com.myimdad_por.domain.model.SubscriptionPlan
import com.myimdad_por.domain.model.SubscriptionStatus
import com.myimdad_por.domain.usecase.subscription.GetSubscriptionStatusRequest
import com.myimdad_por.domain.usecase.subscription.GetSubscriptionStatusUseCase
import com.myimdad_por.domain.usecase.subscription.SubscriptionAction
import com.myimdad_por.domain.usecase.subscription.ValidateActionPermissionRequest
import com.myimdad_por.domain.usecase.subscription.ValidateActionPermissionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class SubscriptionViewModel @Inject constructor(
    private val getSubscriptionStatusUseCase: GetSubscriptionStatusUseCase,
    private val validateActionPermissionUseCase: ValidateActionPermissionUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SubscriptionUiState())
    val uiState: StateFlow<SubscriptionUiState> = _uiState.asStateFlow()

    init {
        loadSubscription()
    }

    fun onEvent(event: SubscriptionUiEvent) {
        when (event) {
            SubscriptionUiEvent.LoadSubscription -> loadSubscription()
            SubscriptionUiEvent.RefreshSubscription -> loadSubscription(refreshFromServer = true)
            SubscriptionUiEvent.Retry -> loadSubscription(refreshFromServer = false)

            is SubscriptionUiEvent.PlanSelected -> selectPlan(event.plan)

            SubscriptionUiEvent.ShowPlanSelection -> {
                _uiState.update { it.copy(showPlanSelection = true) }
            }

            SubscriptionUiEvent.HidePlanSelection -> {
                _uiState.update { it.copy(showPlanSelection = false) }
            }

            SubscriptionUiEvent.ShowRenewDialog -> {
                _uiState.update { it.copy(showRenewDialog = true) }
            }

            SubscriptionUiEvent.HideRenewDialog -> {
                _uiState.update { it.copy(showRenewDialog = false) }
            }

            SubscriptionUiEvent.ShowCancelDialog -> {
                _uiState.update { it.copy(showCancelDialog = true) }
            }

            SubscriptionUiEvent.HideCancelDialog -> {
                _uiState.update { it.copy(showCancelDialog = false) }
            }

            SubscriptionUiEvent.ConfirmSubscriptionAction -> confirmSelectedPlan()
            SubscriptionUiEvent.ConfirmRenewSubscription -> confirmRenew()
            SubscriptionUiEvent.ConfirmCancelSubscription -> confirmCancel()

            SubscriptionUiEvent.DismissError -> {
                _uiState.update { it.copy(errorMessage = null) }
            }

            SubscriptionUiEvent.DismissSuccess -> {
                _uiState.update { it.copy(successMessage = null) }
            }
        }
    }

    private fun loadSubscription(refreshFromServer: Boolean = false) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    isRefreshing = refreshFromServer,
                    errorMessage = null,
                    successMessage = null
                )
            }

            val result = getSubscriptionStatusUseCase(
                GetSubscriptionStatusRequest(
                    refreshFromServer = refreshFromServer,
                    includeRawSubscription = true
                )
            )

            result.fold(
                onSuccess = { data ->
                    val currentPlan = data.plan
                    val allPlans = SubscriptionPlan.entries.toList()

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            subscriptionInfo = data.subscription,
                            availablePlans = allPlans,
                            selectedPlan = currentPlan,
                            showPlanSelection = false
                        )
                    }
                },
                onFailure = { throwable ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            errorMessage = throwable.message ?: "تعذر تحميل حالة الاشتراك"
                        )
                    }
                }
            )
        }
    }

    private fun selectPlan(plan: SubscriptionPlan) {
        _uiState.update { current ->
            current.copy(
                selectedPlan = plan,
                showPlanSelection = true,
                errorMessage = null,
                successMessage = null
            )
        }
    }

    private fun confirmSelectedPlan() {
        val selectedPlan = _uiState.value.selectedPlan
        if (selectedPlan == null) {
            _uiState.update {
                it.copy(errorMessage = "يرجى اختيار خطة أولاً")
            }
            return
        }

        viewModelScope.launch {
            val permissionResult = validateActionPermissionUseCase(
                ValidateActionPermissionRequest(
                    action = SubscriptionAction.USE_PAID_FEATURES,
                    feature = null,
                    strictReadOnlyMode = true
                )
            )

            permissionResult.fold(
                onSuccess = { result ->
                    if (result.allowed) {
                        _uiState.update {
                            it.copy(
                                showPlanSelection = false,
                                successMessage = "تم اختيار خطة ${selectedPlan.name} بنجاح",
                                errorMessage = null
                            )
                        }
                    } else {
                        _uiState.update {
                            it.copy(
                                errorMessage = result.reason ?: "لا يمكن إتمام هذا الإجراء حالياً"
                            )
                        }
                    }
                },
                onFailure = { throwable ->
                    _uiState.update {
                        it.copy(
                            errorMessage = throwable.message ?: "تعذر التحقق من صلاحية الإجراء"
                        )
                    }
                }
            )
        }
    }

    private fun confirmRenew() {
        viewModelScope.launch {
            val state = _uiState.value
            val canRenew = state.canRenew

            if (!canRenew) {
                _uiState.update {
                    it.copy(errorMessage = "لا يمكن تجديد الاشتراك في الوقت الحالي")
                }
                return@launch
            }

            _uiState.update {
                it.copy(
                    showRenewDialog = false,
                    successMessage = "تم طلب تجديد الاشتراك بنجاح",
                    errorMessage = null
                )
            }
        }
    }

    private fun confirmCancel() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    showCancelDialog = false,
                    successMessage = "تم تنفيذ الطلب بنجاح",
                    errorMessage = null
                )
            }
        }
    }

    suspend fun validateFeatureAccess(
        feature: SubscriptionFeature
    ): Boolean {
        return validateActionPermissionUseCase(
            ValidateActionPermissionRequest(
                action = SubscriptionAction.GENERIC_FEATURE,
                feature = feature,
                strictReadOnlyMode = true
            )
        ).getOrNull()?.allowed == true
    }

    fun clearMessages() {
        _uiState.update {
            it.copy(
                errorMessage = null,
                successMessage = null
            )
        }
    }

    fun refresh() {
        loadSubscription(refreshFromServer = true)
    }

    fun getCurrentStatus(): SubscriptionStatus? {
        return _uiState.value.currentStatus
    }
}