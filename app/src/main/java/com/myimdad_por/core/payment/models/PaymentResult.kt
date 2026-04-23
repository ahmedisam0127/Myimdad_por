package com.myimdad_por.core.payment.models

/**
 * Result returned after attempting a payment operation.
 */
sealed class PaymentResult {

    abstract val transactionId: String?
    abstract val providerReference: String?
    abstract val timestampMillis: Long

    data class Success(
        override val transactionId: String,
        override val providerReference: String? = null,
        val receipt: PaymentReceipt? = null,
        override val timestampMillis: Long = System.currentTimeMillis()
    ) : PaymentResult()

    data class RequiresAction(
        override val transactionId: String,
        val actionUrl: String? = null,
        val actionType: PaymentActionType = PaymentActionType.UNKNOWN,
        val message: String? = null,
        override val providerReference: String? = null,
        override val timestampMillis: Long = System.currentTimeMillis()
    ) : PaymentResult()

    data class Pending(
        override val transactionId: String,
        override val providerReference: String? = null,
        val status: PaymentStatus = PaymentStatus.PENDING,
        override val timestampMillis: Long = System.currentTimeMillis()
    ) : PaymentResult()

    data class Failure(
        override val transactionId: String? = null,
        override val providerReference: String? = null,
        val errorCode: String? = null,
        val message: String,
        val causeMessage: String? = null,
        override val timestampMillis: Long = System.currentTimeMillis()
    ) : PaymentResult()
}

/**
 * Action that may be required before a payment can complete.
 */
enum class PaymentActionType {
    UNKNOWN,
    OTP,
    REDIRECT,
    THREE_DS,
    BANK_APP_CONFIRMATION,
    BIOMETRIC_CONFIRMATION
}