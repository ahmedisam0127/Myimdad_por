package com.myimdad_por.domain.model

/**
 * Legal and tax submission state for an invoice.
 *
 * This helps track whether the invoice is:
 * - local only
 * - submitted
 * - accepted by authority
 * - rejected with a reason
 */
data class LegalInvoiceStatus(
    val invoiceId: String,
    val legalStatus: LegalStatus,
    val submissionId: String? = null,
    val qrCodeData: String? = null,
    val hash: String? = null,
    val authorityName: String? = null,
    val submittedAtMillis: Long? = null,
    val reviewedAtMillis: Long? = null,
    val approvedAtMillis: Long? = null,
    val rejectedAtMillis: Long? = null,
    val errorLog: String? = null,
    val metadata: Map<String, String> = emptyMap()
) {
    init {
        require(invoiceId.isNotBlank()) { "invoiceId cannot be blank." }
        submissionId?.let {
            require(it.isNotBlank()) { "submissionId cannot be blank when provided." }
        }
        qrCodeData?.let {
            require(it.isNotBlank()) { "qrCodeData cannot be blank when provided." }
        }
        hash?.let {
            require(it.isNotBlank()) { "hash cannot be blank when provided." }
        }
        authorityName?.let {
            require(it.isNotBlank()) { "authorityName cannot be blank when provided." }
        }
        submittedAtMillis?.let {
            require(it > 0L) { "submittedAtMillis must be greater than zero when provided." }
        }
        reviewedAtMillis?.let {
            require(it > 0L) { "reviewedAtMillis must be greater than zero when provided." }
        }
        approvedAtMillis?.let {
            require(it > 0L) { "approvedAtMillis must be greater than zero when provided." }
        }
        rejectedAtMillis?.let {
            require(it > 0L) { "rejectedAtMillis must be greater than zero when provided." }
        }
    }

    val isSubmitted: Boolean
        get() = legalStatus != LegalStatus.NOT_SUBMITTED

    val isFinalized: Boolean
        get() = legalStatus == LegalStatus.APPROVED || legalStatus == LegalStatus.REJECTED

    val requiresAction: Boolean
        get() = legalStatus == LegalStatus.REJECTED || legalStatus == LegalStatus.PENDING_REVIEW
}

enum class LegalStatus {
    NOT_SUBMITTED,
    PENDING_REVIEW,
    SUBMITTED,
    APPROVED,
    REJECTED,
    AMENDED,
    CANCELED
}