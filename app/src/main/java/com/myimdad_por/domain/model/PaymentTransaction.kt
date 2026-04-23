package com.myimdad_por.domain.model

import java.math.BigDecimal

/**
 * Business-level transaction model.
 *
 * Represents one payment attempt or one settled transaction in the domain layer.
 */
data class PaymentTransaction(
    val transactionId: String,
    val paymentIntentId: String? = null,
    val referenceNumber: String? = null,
    val paymentMethod: PaymentMethod? = null,
    val amount: BigDecimal,
    val currency: CurrencyCode,
    val status: PaymentStatus = PaymentStatus.PENDING,
    val providerName: String? = null,
    val providerReference: String? = null,
    val receiptNumber: String? = null,
    val authorizedAtMillis: Long? = null,
    val capturedAtMillis: Long? = null,
    val refundedAtMillis: Long? = null,
    val failureReason: String? = null,
    val metadata: Map<String, String> = emptyMap()
) {
    init {
        require(transactionId.isNotBlank()) { "transactionId cannot be blank." }
        require(amount > BigDecimal.ZERO) { "amount must be greater than zero." }

        paymentIntentId?.let {
            require(it.isNotBlank()) { "paymentIntentId cannot be blank when provided." }
        }
        referenceNumber?.let {
            require(it.isNotBlank()) { "referenceNumber cannot be blank when provided." }
        }
        providerName?.let {
            require(it.isNotBlank()) { "providerName cannot be blank when provided." }
        }
        providerReference?.let {
            require(it.isNotBlank()) { "providerReference cannot be blank when provided." }
        }
        receiptNumber?.let {
            require(it.isNotBlank()) { "receiptNumber cannot be blank when provided." }
        }
        authorizedAtMillis?.let {
            require(it > 0L) { "authorizedAtMillis must be greater than zero when provided." }
        }
        capturedAtMillis?.let {
            require(it > 0L) { "capturedAtMillis must be greater than zero when provided." }
        }
        refundedAtMillis?.let {
            require(it > 0L) { "refundedAtMillis must be greater than zero when provided." }
        }
        failureReason?.let {
            require(it.isNotBlank()) { "failureReason cannot be blank when provided." }
        }
    }

    val normalizedCurrencyCode: String
        get() = currency.code

    val isSuccessful: Boolean
        get() = status == PaymentStatus.PAID

    val isRefunded: Boolean
        get() = status == PaymentStatus.REFUNDED || status == PaymentStatus.PARTIALLY_REFUNDED

    val isTerminalState: Boolean
        get() = status in setOf(
            PaymentStatus.PAID,
            PaymentStatus.FAILED,
            PaymentStatus.CANCELED,
            PaymentStatus.REFUNDED,
            PaymentStatus.PARTIALLY_REFUNDED,
            PaymentStatus.EXPIRED
        )
}