package com.myimdad_por.domain.model

import java.math.BigDecimal

/**
 * Persistable business record for a payment.
 *
 * This is the domain-friendly representation of what was paid,
 * when it was paid, and how it should be audited later.
 */
data class PaymentRecord(
    val recordId: String,
    val transactionId: String,
    val invoiceId: String? = null,
    val customerId: String? = null,
    val paymentMethod: PaymentMethod? = null,
    val amount: BigDecimal,
    val currency: CurrencyCode,
    val status: PaymentStatus = PaymentStatus.PENDING,
    val providerName: String? = null,
    val providerReference: String? = null,
    val receiptNumber: String? = null,
    val note: String? = null,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis(),
    val metadata: Map<String, String> = emptyMap()
) {
    init {
        require(recordId.isNotBlank()) { "recordId cannot be blank." }
        require(transactionId.isNotBlank()) { "transactionId cannot be blank." }
        require(amount >= BigDecimal.ZERO) { "amount cannot be negative." }

        invoiceId?.let {
            require(it.isNotBlank()) { "invoiceId cannot be blank when provided." }
        }
        customerId?.let {
            require(it.isNotBlank()) { "customerId cannot be blank when provided." }
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
        note?.let {
            require(it.isNotBlank()) { "note cannot be blank when provided." }
        }
        require(createdAtMillis > 0L) { "createdAtMillis must be greater than zero." }
        require(updatedAtMillis > 0L) { "updatedAtMillis must be greater than zero." }
    }

    val normalizedCurrencyCode: String
        get() = currency.code

    val isSettled: Boolean
        get() = status == PaymentStatus.PAID

    val isRefunded: Boolean
        get() = status == PaymentStatus.REFUNDED || status == PaymentStatus.PARTIALLY_REFUNDED
}