package com.myimdad_por.core.payment.models

import java.math.BigDecimal

/**
 * A compact receipt returned to the app or persisted locally.
 */
data class PaymentReceipt(
    val receiptId: String,
    val transactionId: String,
    val amount: BigDecimal,
    val currency: String,
    val merchantName: String,
    val providerReference: String? = null,
    val paidAtMillis: Long = System.currentTimeMillis(),
    val status: PaymentStatus = PaymentStatus.PAID,
    val signature: String? = null,
    val rawPayload: String? = null,
    val metadata: Map<String, String> = emptyMap()
) {
    init {
        require(receiptId.isNotBlank()) { "receiptId must not be blank." }
        require(transactionId.isNotBlank()) { "transactionId must not be blank." }
        require(amount >= BigDecimal.ZERO) { "amount must not be negative." }
        require(currency.isNotBlank()) { "currency must not be blank." }
        require(merchantName.isNotBlank()) { "merchantName must not be blank." }
    }

    val normalizedCurrency: String
        get() = currency.trim().uppercase()
}