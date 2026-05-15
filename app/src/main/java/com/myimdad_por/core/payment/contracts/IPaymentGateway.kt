package com.myimdad_por.core.payment.contracts

import com.myimdad_por.core.payment.models.PaymentIntent
import com.myimdad_por.core.payment.models.PaymentResult
import com.myimdad_por.core.payment.models.PaymentStatus
import com.myimdad_por.core.payment.models.PaymentVerification
import com.myimdad_por.core.payment.models.RefundRequest

/**
 * Contract for any payment gateway implementation.
 *
 * This abstraction keeps the app independent from the concrete provider:
 * Stripe, PayPal, local gateways, or any future processor.
 */
interface IPaymentGateway {

    val gatewayName: String

    suspend fun createPaymentIntent(
        amount: Double,
        currency: String,
        idempotencyKey: String? = null
    ): Result<PaymentIntent>

    suspend fun processPayment(
        paymentIntent: PaymentIntent,
        idempotencyKey: String? = null
    ): Result<PaymentResult>

    suspend fun verifyTransaction(
        transactionId: String
    ): Result<PaymentVerification>

    suspend fun refund(
        refundRequest: RefundRequest,
        idempotencyKey: String? = null
    ): Result<PaymentResult>

    suspend fun getPaymentStatus(
        transactionId: String
    ): Result<PaymentStatus>
}