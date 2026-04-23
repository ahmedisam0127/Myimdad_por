package com.myimdad_por.domain.model

/**
 * Domain-level payment status used across repositories, use cases, and UI.
 *
 * This mirrors the lifecycle of a payment in the business layer.
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
    PARTIALLY_PAID, 
    EXPIRED
}