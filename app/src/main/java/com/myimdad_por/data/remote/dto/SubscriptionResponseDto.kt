package com.myimdad_por.data.remote.dto

/**
 * [SubscriptionResponseDto] 
 * الدرع الواقي الأول لبيانات الاشتراك القادمة من الـ API.
 * تم تصميمه ليتعامل مع البيانات المفقودة (Nulls) بذكاء وصرامة في نفس الوقت.
 */
data class SubscriptionResponseDto(
    val subscriptionId: String,
    val plan: String,
    val status: String,
    val startDateMillis: Long,
    val expiryDateMillis: Long,
    val maxUsers: Int = 1,
    val maxInvoicesPerMonth: Int? = null,
    val featuresEnabled: List<String> = emptyList(),
    val offlineGracePeriodDays: Int = 0,
    val lastSyncedAtMillis: Long? = null,
    val renewedAtMillis: Long? = null,
    val notes: String? = null,
    val metadataJson: String = "{}"
) {
    init {
        // التحقق الاستباقي (Proactive Validation)
        require(subscriptionId.isNotBlank()) { "FATAL: Subscription ID is missing or blank." }
        require(plan.isNotBlank()) { "FATAL: Subscription Plan is required." }
        require(status.isNotBlank()) { "FATAL: Subscription Status is required." }
        
        // التحقق من المنطق الزمني (Temporal Logic Validation)
        require(startDateMillis > 0L) { "Invalid Start Date: Must be a valid epoch timestamp." }
        require(expiryDateMillis > 0L) { "Invalid Expiry Date: Must be a valid epoch timestamp." }
        require(expiryDateMillis >= startDateMillis) { 
            "Time Paradox: Expiry date ($expiryDateMillis) cannot precede Start date ($startDateMillis)." 
        }
        
        // التحقق من حدود الاستخدام (Usage Limits)
        require(maxUsers > 0) { "A subscription must allow at least 1 user." }
        require(offlineGracePeriodDays >= 0) { "Grace period cannot be a negative value." }
        
        maxInvoicesPerMonth?.let { 
            require(it > 0) { "If max invoices is set, it must be greater than zero." } 
        }
    }
}
