package com.myimdad_por.core.payment.models

import java.math.BigDecimal

/**
 * Result of verifying a receipt or a payment event.
 */
data class PaymentVerification(
    val verified: Boolean,
    val transactionId: String,
    val amount: BigDecimal,
    val currency: String,
    val status: PaymentStatus,
    val providerReference: String? = null,
    val signatureValid: Boolean = false,
    val amountMatched: Boolean = false,
    val backendConfirmed: Boolean = false,
    val verifiedAtMillis: Long = System.currentTimeMillis(),
    val reason: String? = null,
    val metadata: Map<String, String> = emptyMap()
) {
    init {
        require(transactionId.isNotBlank()) { "transactionId must not be blank." }
        require(amount >= BigDecimal.ZERO) { "amount must not be negative." }
        require(currency.isNotBlank()) { "currency must not be blank." }
    }

    val normalizedCurrency: String
        get() = currency.trim().uppercase()
}