package com.myimdad_por.domain.model

/**
 * Subscription and licensing snapshot for the customer account.
 *
 * Supports online and offline enforcement with a grace period.
 */
data class SubscriptionInfo(
    val subscriptionId: String,
    val plan: SubscriptionPlan,
    val status: SubscriptionStatus,
    val startDateMillis: Long,
    val expiryDateMillis: Long,
    val maxUsers: Int,
    val maxInvoicesPerMonth: Int? = null,
    val featuresEnabled: Set<SubscriptionFeature> = emptySet(),
    val offlineGracePeriodDays: Int = 0,
    val lastSyncedAtMillis: Long? = null,
    val renewedAtMillis: Long? = null,
    val notes: String? = null,
    val metadata: Map<String, String> = emptyMap()
) {
    init {
        require(subscriptionId.isNotBlank()) { "subscriptionId cannot be blank." }
        require(startDateMillis > 0L) { "startDateMillis must be greater than zero." }
        require(expiryDateMillis > 0L) { "expiryDateMillis must be greater than zero." }
        require(startDateMillis <= expiryDateMillis) {
            "startDateMillis must be less than or equal to expiryDateMillis."
        }
        require(maxUsers > 0) { "maxUsers must be greater than zero." }
        require(offlineGracePeriodDays >= 0) { "offlineGracePeriodDays cannot be negative." }
        maxInvoicesPerMonth?.let {
            require(it > 0) { "maxInvoicesPerMonth must be greater than zero when provided." }
        }
        lastSyncedAtMillis?.let {
            require(it > 0L) { "lastSyncedAtMillis must be greater than zero when provided." }
        }
        renewedAtMillis?.let {
            require(it > 0L) { "renewedAtMillis must be greater than zero when provided." }
        }
        notes?.let {
            require(it.isNotBlank()) { "notes cannot be blank when provided." }
        }
    }

    val isExpired: Boolean
        get() = System.currentTimeMillis() > expiryDateMillis

    val isInGracePeriod: Boolean
        get() {
            if (!isExpired || offlineGracePeriodDays == 0) return false
            val graceEnd = expiryDateMillis + offlineGracePeriodDays.toLong() * 24L * 60L * 60L * 1000L
            return System.currentTimeMillis() <= graceEnd
        }

    val canOperateOffline: Boolean
        get() = status == SubscriptionStatus.ACTIVE && !isExpired || isInGracePeriod

    val isActive: Boolean
        get() = status == SubscriptionStatus.ACTIVE && (System.currentTimeMillis() <= expiryDateMillis || isInGracePeriod)
}

enum class SubscriptionPlan {
    STARTER,
    PRO,
    BUSINESS,
    ENTERPRISE,
    CUSTOM
}

enum class SubscriptionStatus {
    ACTIVE,
    EXPIRED,
    GRACE_PERIOD,
    SUSPENDED,
    CANCELED,
    PENDING
}

enum class SubscriptionFeature {
    ADVANCED_REPORTS,
    MULTI_BRANCH,
    API_ACCESS,
    PAYMENT_GATEWAY,
    TAX_INVOICE,
    LEGAL_INVOICE,
    OFFLINE_MODE,
    AUDIT_LOGS,
    EXPORT_EXCEL,
    EXPORT_PDF,
    ROLE_BASED_ACCESS,
    BACKUP_SYNC
}