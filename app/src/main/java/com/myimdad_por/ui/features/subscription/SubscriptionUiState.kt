package com.myimdad_por.ui.features.subscription

import com.myimdad_por.domain.model.SubscriptionInfo
import com.myimdad_por.domain.model.SubscriptionPlan
import com.myimdad_por.domain.model.SubscriptionStatus

data class SubscriptionUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isSubmitting: Boolean = false,

    val subscriptionInfo: SubscriptionInfo? = null,
    val availablePlans: List<SubscriptionPlan> = emptyList(),
    val selectedPlan: SubscriptionPlan? = null,

    val showPlanSelection: Boolean = false,
    val showRenewDialog: Boolean = false,
    val showCancelDialog: Boolean = false,

    val errorMessage: String? = null,
    val successMessage: String? = null
) {
    val hasSubscription: Boolean
        get() = subscriptionInfo != null

    val isSubscriptionActive: Boolean
        get() = subscriptionInfo?.status == SubscriptionStatus.ACTIVE

    val isSubscriptionExpired: Boolean
        get() = subscriptionInfo?.isExpired == true

    val isInGracePeriod: Boolean
        get() = subscriptionInfo?.isInGracePeriod == true

    val canOperateOffline: Boolean
        get() = subscriptionInfo?.canOperateOffline == true

    val currentPlan: SubscriptionPlan?
        get() = subscriptionInfo?.plan

    val currentStatus: SubscriptionStatus?
        get() = subscriptionInfo?.status

    val maxUsers: Int
        get() = subscriptionInfo?.maxUsers ?: 0

    val maxInvoicesPerMonth: Int?
        get() = subscriptionInfo?.maxInvoicesPerMonth

    val expiryDateMillis: Long?
        get() = subscriptionInfo?.expiryDateMillis

    val startDateMillis: Long?
        get() = subscriptionInfo?.startDateMillis

    val hasSelectedPlan: Boolean
        get() = selectedPlan != null

    val canRenew: Boolean
        get() = isSubscriptionExpired || isInGracePeriod || currentStatus == SubscriptionStatus.CANCELED

    val canUpgrade: Boolean
        get() = currentPlan != null &&
            selectedPlan != null &&
            selectedPlan != currentPlan &&
            currentPlan != SubscriptionPlan.ENTERPRISE

    val canDowngrade: Boolean
        get() = currentPlan != null &&
            selectedPlan != null &&
            selectedPlan != currentPlan &&
            selectedPlan != SubscriptionPlan.ENTERPRISE
}