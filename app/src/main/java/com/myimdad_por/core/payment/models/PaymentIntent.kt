package com.myimdad_por.core.payment.models

import java.math.BigDecimal

/**
 * Represents a payment initiation payload returned by the gateway.
 */
data class PaymentIntent(
    val intentId: String,
    val transactionId: String,
    val amount: BigDecimal,
    val currency: String,
    val clientSecret: String? = null,
    val providerReference: String? = null,
    val status: PaymentStatus = PaymentStatus.PENDING,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val expiresAtMillis: Long? = null,
    val idempotencyKey: String? = null,
    val metadata: Map<String, String> = emptyMap()
) {
    init {
        require(intentId.isNotBlank()) { "intentId must not be blank." }
        require(transactionId.isNotBlank()) { "transactionId must not be blank." }
        require(amount > BigDecimal.ZERO) { "amount must be greater than zero." }
        require(currency.isNotBlank()) { "currency must not be blank." }
        expiresAtMillis?.let {
            require(it > 0L) { "expiresAtMillis must be greater than zero when provided." }
        }
    }

    val normalizedCurrency: String
        get() = currency.trim().uppercase()
}

/**
 * High-level status of a payment lifecycle.
 */
enum class PaymentStatus {
    PENDING,
    PROCESSING,
    REQUIRES_ACTION,
    AUTHORIZED,
    PAID,
    FAILED,
    CANCELED,
    REFUNDED,
    PARTIALLY_REFUNDED,
    EXPIRED
}