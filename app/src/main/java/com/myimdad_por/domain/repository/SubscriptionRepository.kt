package com.myimdad_por.domain.repository

import com.myimdad_por.domain.model.SubscriptionFeature
import com.myimdad_por.domain.model.SubscriptionInfo
import com.myimdad_por.domain.model.SubscriptionPlan
import com.myimdad_por.domain.model.SubscriptionStatus
import kotlinx.coroutines.flow.Flow

/**
 * Contract for subscription, licensing, and entitlement enforcement.
 *
 * Important business rule:
 * - When the subscription expires, paid features must stop immediately,
 *   even if the device is offline.
 * - In expired or inactive mode, the app should remain in read-only mode
 *   for allowed modules only.
 * - Invoices and reports can be blocked entirely by policy when subscription
 *   is not active.
 */
interface SubscriptionRepository {

    fun observeSubscription(): Flow<SubscriptionInfo?>

    fun observeSubscriptionStatus(): Flow<SubscriptionStatus?>

    suspend fun getSubscription(): SubscriptionInfo?

    suspend fun refreshSubscriptionFromServer(): Result<SubscriptionInfo>

    suspend fun saveSubscription(subscriptionInfo: SubscriptionInfo): Result<SubscriptionInfo>

    suspend fun clearSubscription(): Result<Unit>

    suspend fun isSubscriptionActive(): Boolean

    suspend fun isSubscriptionExpired(): Boolean

    suspend fun isInGracePeriod(): Boolean

    suspend fun canOperateOffline(): Boolean

    suspend fun canUsePaidFeatures(): Boolean

    suspend fun checkFeatureAccess(feature: SubscriptionFeature): Boolean

    suspend fun canAccessInvoices(): Boolean

    suspend fun canAccessReports(): Boolean

    suspend fun isReadOnlyMode(): Boolean

    suspend fun getAllowedPlan(): SubscriptionPlan?

    suspend fun getRemainingUsersCount(): Int?

    suspend fun getRemainingInvoicesCount(): Int?

    suspend fun recordInvoiceUsage(count: Int = 1): Result<Unit>

    suspend fun recordFeatureUsage(feature: SubscriptionFeature): Result<Unit>

    suspend fun syncUsageWithServer(): Result<Unit>

    suspend fun getSubscriptionStatusSummary(): SubscriptionStatusSummary
}

/**
 * Compact snapshot for UI and policy decisions.
 */
data class SubscriptionStatusSummary(
    val subscriptionId: String? = null,
    val plan: SubscriptionPlan? = null,
    val status: SubscriptionStatus? = null,
    val isActive: Boolean = false,
    val isExpired: Boolean = false,
    val isInGracePeriod: Boolean = false,
    val canOperateOffline: Boolean = false,
    val canUsePaidFeatures: Boolean = false,
    val canAccessInvoices: Boolean = false,
    val canAccessReports: Boolean = false,
    val readOnlyMode: Boolean = true,
    val remainingUsers: Int? = null,
    val remainingInvoices: Int? = null
) {
    val isBlocked: Boolean
        get() = !isActive || readOnlyMode
}