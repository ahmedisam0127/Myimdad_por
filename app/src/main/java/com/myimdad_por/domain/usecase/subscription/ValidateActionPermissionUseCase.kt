package com.myimdad_por.domain.usecase.subscription

import com.myimdad_por.domain.model.SubscriptionFeature
import com.myimdad_por.domain.repository.SubscriptionRepository
import javax.inject.Inject

enum class SubscriptionAction {
    VIEW_INVOICES,
    VIEW_REPORTS,
    USE_OFFLINE_MODE,
    USE_PAID_FEATURES,
    SYNC_WITH_SERVER,
    EXPORT_EXCEL,
    EXPORT_PDF,
    ACCESS_AUDIT_LOGS,
    USE_PAYMENT_GATEWAY,
    MANAGE_BRANCHES,
    ACCESS_API,
    GENERIC_FEATURE
}

data class ValidateActionPermissionRequest(
    val action: SubscriptionAction,
    val feature: SubscriptionFeature? = null,
    val strictReadOnlyMode: Boolean = true
)

data class ValidateActionPermissionResult(
    val action: SubscriptionAction,
    val allowed: Boolean,
    val reason: String? = null,
    val feature: SubscriptionFeature? = null
) {
    val denied: Boolean
        get() = !allowed
}

class ValidateActionPermissionUseCase @Inject constructor(
    private val subscriptionRepository: SubscriptionRepository
) {

    suspend operator fun invoke(
        request: ValidateActionPermissionRequest
    ): Result<ValidateActionPermissionResult> {
        return runCatching {
            val readOnlyMode = subscriptionRepository.isReadOnlyMode()
            val subscriptionActive = subscriptionRepository.isSubscriptionActive()
            val inGracePeriod = subscriptionRepository.isInGracePeriod()
            val canOperateOffline = subscriptionRepository.canOperateOffline()
            val canUsePaidFeatures = subscriptionRepository.canUsePaidFeatures()

            val allowed = when (request.action) {
                SubscriptionAction.VIEW_INVOICES -> subscriptionRepository.canAccessInvoices()

                SubscriptionAction.VIEW_REPORTS -> subscriptionRepository.canAccessReports()

                SubscriptionAction.USE_OFFLINE_MODE -> canOperateOffline

                SubscriptionAction.USE_PAID_FEATURES -> canUsePaidFeatures

                SubscriptionAction.SYNC_WITH_SERVER -> subscriptionRepository.syncUsageWithServer().isSuccess

                SubscriptionAction.EXPORT_EXCEL ->
                    subscriptionRepository.checkFeatureAccess(SubscriptionFeature.EXPORT_EXCEL)

                SubscriptionAction.EXPORT_PDF ->
                    subscriptionRepository.checkFeatureAccess(SubscriptionFeature.EXPORT_PDF)

                SubscriptionAction.ACCESS_AUDIT_LOGS ->
                    subscriptionRepository.checkFeatureAccess(SubscriptionFeature.AUDIT_LOGS)

                SubscriptionAction.USE_PAYMENT_GATEWAY ->
                    subscriptionRepository.checkFeatureAccess(SubscriptionFeature.PAYMENT_GATEWAY)

                SubscriptionAction.MANAGE_BRANCHES ->
                    subscriptionRepository.checkFeatureAccess(SubscriptionFeature.MULTI_BRANCH)

                SubscriptionAction.ACCESS_API ->
                    subscriptionRepository.checkFeatureAccess(SubscriptionFeature.API_ACCESS)

                SubscriptionAction.GENERIC_FEATURE -> {
                    val feature = request.feature
                    feature != null && subscriptionRepository.checkFeatureAccess(feature)
                }
            }

            val reason = when {
                allowed -> null
                !subscriptionActive && !inGracePeriod -> "subscription is inactive or expired"
                request.strictReadOnlyMode && readOnlyMode -> "subscription is in read-only mode"
                request.action == SubscriptionAction.GENERIC_FEATURE && request.feature == null ->
                    "feature is required for generic feature checks"
                else -> "access denied by subscription policy"
            }

            ValidateActionPermissionResult(
                action = request.action,
                allowed = allowed,
                reason = reason,
                feature = request.feature
            )
        }
    }
}