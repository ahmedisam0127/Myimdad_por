package com.myimdad_por.domain.usecase.subscription

import com.myimdad_por.domain.model.SubscriptionInfo
import com.myimdad_por.domain.model.SubscriptionPlan
import com.myimdad_por.domain.model.SubscriptionStatus
import com.myimdad_por.domain.repository.SubscriptionRepository
import com.myimdad_por.domain.repository.SubscriptionStatusSummary
import javax.inject.Inject

data class GetSubscriptionStatusRequest(
    val refreshFromServer: Boolean = false,
    val includeRawSubscription: Boolean = true
)

data class GetSubscriptionStatusResult(
    val summary: SubscriptionStatusSummary,
    val subscription: SubscriptionInfo? = null
) {
    val isActive: Boolean
        get() = summary.isActive

    val isExpired: Boolean
        get() = summary.isExpired

    val isInGracePeriod: Boolean
        get() = summary.isInGracePeriod

    val canOperateOffline: Boolean
        get() = summary.canOperateOffline

    val canUsePaidFeatures: Boolean
        get() = summary.canUsePaidFeatures

    val isBlocked: Boolean
        get() = summary.isBlocked

    val plan: SubscriptionPlan?
        get() = summary.plan

    val status: SubscriptionStatus?
        get() = summary.status
}

class GetSubscriptionStatusUseCase @Inject constructor(
    private val subscriptionRepository: SubscriptionRepository
) {

    suspend operator fun invoke(
        request: GetSubscriptionStatusRequest = GetSubscriptionStatusRequest()
    ): Result<GetSubscriptionStatusResult> {
        return runCatching {
            if (request.refreshFromServer) {
                subscriptionRepository.refreshSubscriptionFromServer().getOrThrow()
            }

            val summary = subscriptionRepository.getSubscriptionStatusSummary()
            val subscription = if (request.includeRawSubscription) {
                subscriptionRepository.getSubscription()
            } else {
                null
            }

            GetSubscriptionStatusResult(
                summary = summary,
                subscription = subscription
            )
        }
    }
}