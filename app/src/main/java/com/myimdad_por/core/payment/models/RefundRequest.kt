package com.myimdad_por.core.payment.models

import java.math.BigDecimal

/**
 * Request model for refunding a previous payment.
 */
data class RefundRequest(
    val transactionId: String,
    val amount: BigDecimal? = null,
    val currency: String? = null,
    val reason: String? = null,
    val refundId: String? = null,
    val idempotencyKey: String? = null,
    val requestedBy: String? = null,
    val metadata: Map<String, String> = emptyMap()
) {
    init {
        require(transactionId.isNotBlank()) { "transactionId must not be blank." }
        amount?.let {
            require(it > BigDecimal.ZERO) { "amount must be greater than zero when provided." }
        }
        currency?.let {
            require(it.isNotBlank()) { "currency must not be blank when provided." }
        }
        refundId?.let {
            require(it.isNotBlank()) { "refundId must not be blank when provided." }
        }
        idempotencyKey?.let {
            require(it.isNotBlank()) { "idempotencyKey must not be blank when provided." }
        }
    }

    val normalizedCurrency: String?
        get() = currency?.trim()?.uppercase()

    val isPartialRefund: Boolean
        get() = amount != null
}