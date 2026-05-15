package com.myimdad_por.domain.model

import java.math.BigDecimal

/**
 * Represents an authorization stage for a payment.
 *
 * This is useful for flows where money is not captured immediately,
 * such as bank approvals, wallet confirmation, or delayed capture.
 */
data class PaymentAuthorization(
    val authorizationId: String,
    val transactionId: String,
    val paymentMethod: PaymentMethod? = null,
    val amount: BigDecimal,
    val currency: CurrencyCode,
    val status: AuthorizationStatus = AuthorizationStatus.PENDING,
    val providerName: String? = null,
    val providerReference: String? = null,
    val expiresAtMillis: Long? = null,
    val authorizedAtMillis: Long? = null,
    val capturedAtMillis: Long? = null,
    val canceledAtMillis: Long? = null,
    val failureReason: String? = null,
    val metadata: Map<String, String> = emptyMap()
) {
    init {
        require(authorizationId.isNotBlank()) { "authorizationId cannot be blank." }
        require(transactionId.isNotBlank()) { "transactionId cannot be blank." }
        require(amount > BigDecimal.ZERO) { "amount must be greater than zero." }

        providerName?.let {
            require(it.isNotBlank()) { "providerName cannot be blank when provided." }
        }
        providerReference?.let {
            require(it.isNotBlank()) { "providerReference cannot be blank when provided." }
        }
        expiresAtMillis?.let {
            require(it > 0L) { "expiresAtMillis must be greater than zero when provided." }
        }
        authorizedAtMillis?.let {
            require(it > 0L) { "authorizedAtMillis must be greater than zero when provided." }
        }
        capturedAtMillis?.let {
            require(it > 0L) { "capturedAtMillis must be greater than zero when provided." }
        }
        canceledAtMillis?.let {
            require(it > 0L) { "canceledAtMillis must be greater than zero when provided." }
        }
        failureReason?.let {
            require(it.isNotBlank()) { "failureReason cannot be blank when provided." }
        }
    }

    val normalizedCurrencyCode: String
        get() = currency.code

    val isActive: Boolean
        get() = status in setOf(
            AuthorizationStatus.PENDING,
            AuthorizationStatus.AUTHORIZED,
            AuthorizationStatus.REQUIRES_ACTION
        )

    val isFinal: Boolean
        get() = status in setOf(
            AuthorizationStatus.CAPTURED,
            AuthorizationStatus.CANCELED,
            AuthorizationStatus.EXPIRED,
            AuthorizationStatus.FAILED
        )
}

/**
 * Lifecycle of a payment authorization.
 */
enum class AuthorizationStatus {
    PENDING,
    REQUIRES_ACTION,
    AUTHORIZED,
    CAPTURED,
    CANCELED,
    FAILED,
    EXPIRED
}