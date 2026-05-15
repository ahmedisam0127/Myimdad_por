package com.myimdad_por.core.payment.contracts

import com.myimdad_por.core.payment.models.PaymentIntent
import com.myimdad_por.core.payment.models.PaymentResult
import kotlinx.coroutines.flow.StateFlow

/**
 * Internal orchestration layer for the payment flow.
 *
 * The processor coordinates:
 * - payment gateway
 * - business rules
 * - retry/resume flow
 * - UI state updates
 */
interface IPaymentProcessor {

    val processorState: StateFlow<PaymentProcessorState>

    suspend fun executePayment(intent: PaymentIntent): Result<PaymentResult>

    suspend fun resumePayment(transactionId: String): Result<PaymentResult>
}

/**
 * High-level state of the payment processor.
 */
sealed class PaymentProcessorState {
    data object Idle : PaymentProcessorState()
    data object Processing : PaymentProcessorState()
    data class Success(val transactionId: String? = null) : PaymentProcessorState()
    data class Failed(val message: String) : PaymentProcessorState()
    data class RequiresAction(val actionMessage: String) : PaymentProcessorState()
    data object Verifying : PaymentProcessorState()
}