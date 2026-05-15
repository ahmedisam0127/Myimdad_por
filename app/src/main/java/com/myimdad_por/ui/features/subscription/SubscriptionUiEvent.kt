package com.myimdad_por.ui.features.subscription

import com.myimdad_por.domain.model.SubscriptionPlan

sealed interface SubscriptionUiEvent {
    data object LoadSubscription : SubscriptionUiEvent
    data object RefreshSubscription : SubscriptionUiEvent
    data object Retry : SubscriptionUiEvent

    data class PlanSelected(val plan: SubscriptionPlan) : SubscriptionUiEvent

    data object ShowPlanSelection : SubscriptionUiEvent
    data object HidePlanSelection : SubscriptionUiEvent

    data object ShowRenewDialog : SubscriptionUiEvent
    data object HideRenewDialog : SubscriptionUiEvent

    data object ShowCancelDialog : SubscriptionUiEvent
    data object HideCancelDialog : SubscriptionUiEvent

    data object ConfirmSubscriptionAction : SubscriptionUiEvent
    data object ConfirmRenewSubscription : SubscriptionUiEvent
    data object ConfirmCancelSubscription : SubscriptionUiEvent

    data object DismissError : SubscriptionUiEvent
    data object DismissSuccess : SubscriptionUiEvent
}