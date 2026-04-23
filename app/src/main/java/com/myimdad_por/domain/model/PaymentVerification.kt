package com.myimdad_por.domain.model

import java.math.BigDecimal

/**
 * Domain model for verification of a payment transaction or receipt.
 *
 * It captures the business truth after cross-checking:
 * - gateway response
 * - backend settlement
 * - signature integrity
 * - amount matching
 */
data class PaymentVerification(
    val verificationId: String,
    val transactionId: String,
    val authorizationId: String? = null,
    val amount: BigDecimal,
    val currency: CurrencyCode,
    val status: VerificationStatus = VerificationStatus.PENDING,
    val verified: Boolean = false,
    val signatureValid: Boolean = false,
    val amountMatched: Boolean = false,
    val backendConfirmed: Boolean = false,
    val gatewayName: String? = null,
    val providerReference: String? = null,
    val verifiedAtMillis: Long? = null,
    val reason: String? = null,
    val metadata: Map<String, String> = emptyMap()
) {
    init {
        require(verificationId.isNotBlank()) { "verificationId cannot be blank." }
        require(transactionId.isNotBlank()) { "transactionId cannot be blank." }
        require(amount >= BigDecimal.ZERO) { "amount cannot be negative." }

        authorizationId?.let {
            require(it.isNotBlank()) { "authorizationId cannot be blank when provided." }
        }
        gatewayName?.let {
            require(it.isNotBlank()) { "gatewayName cannot be blank when provided." }
        }
        providerReference?.let {
            require(it.isNotBlank()) { "providerReference cannot be blank when provided." }
        }
        verifiedAtMillis?.let {
            require(it > 0L) { "verifiedAtMillis must be greater than zero when provided." }
        }
        reason?.let {
            require(it.isNotBlank()) { "reason cannot be blank when provided." }
        }
    }

    val normalizedCurrencyCode: String
        get() = currency.code

    val isSuccessful: Boolean
        get() = verified && signatureValid && amountMatched && backendConfirmed

    val isFailed: Boolean
        get() = status in setOf(VerificationStatus.FAILED, VerificationStatus.REJECTED)
}

/**
 * Verification lifecycle used by the domain layer.
 */
enum class VerificationStatus {
    PENDING,
    VERIFIED,
    REJECTED,
    FAILED
}