package com.myimdad_por.core.payment.processor

import android.content.Context
import com.myimdad_por.core.payment.contracts.IPaymentGateway
import com.myimdad_por.core.payment.contracts.IPaymentProcessor
import com.myimdad_por.core.payment.contracts.PaymentProcessorState
import com.myimdad_por.core.payment.models.PaymentIntent
import com.myimdad_por.core.payment.models.PaymentResult
import com.myimdad_por.core.payment.models.PaymentStatus
import com.myimdad_por.core.payment.security.PaymentFraudGuard
import com.myimdad_por.core.security.SessionManager
import com.myimdad_por.core.security.TamperProtection
import com.myimdad_por.core.security.app.hardening.HookDetection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class PaymentProcessor(
    context: Context,
    private val gateway: IPaymentGateway,
    private val verificationService: PaymentVerificationService = PaymentVerificationService(context, gateway),
    private val refundService: RefundService = RefundService(context, gateway)
) : IPaymentProcessor {

    private val appContext = context.applicationContext
    private val operationMutex = Mutex()
    private val _processorState = MutableStateFlow<PaymentProcessorState>(PaymentProcessorState.Idle)

    override val processorState: StateFlow<PaymentProcessorState> = _processorState.asStateFlow()

    init {
        PaymentFraudGuard.init(appContext)
        SessionManager.init(appContext)
    }

    override suspend fun executePayment(intent: PaymentIntent): Result<PaymentResult> {
        return operationMutex.withLock {
            withContext(Dispatchers.IO) {
                val expectedProductId = expectedProductId(intent)

                if (!isPaymentAllowed(expectedProductId)) {
                    val reason = buildSecurityReason(expectedProductId)
                    updateState(PaymentProcessorState.Failed(reason))
                    return@withContext Result.failure(IllegalStateException(reason))
                }

                _processorState.value = PaymentProcessorState.Processing

                val idempotencyKey = resolveIdempotencyKey(intent)

                val processed = gateway.processPayment(
                    paymentIntent = intent,
                    idempotencyKey = idempotencyKey
                )

                processed.fold(
                    onSuccess = { result ->
                        when (result) {
                            is PaymentResult.RequiresAction -> {
                                _processorState.value = PaymentProcessorState.RequiresAction(
                                    actionMessage = result.message ?: "Action required."
                                )
                                Result.success(result)
                            }

                            is PaymentResult.Pending -> {
                                _processorState.value = PaymentProcessorState.Verifying
                                Result.success(result)
                            }

                            is PaymentResult.Success -> {
                                val verification = verifyPostPayment(
                                    transactionId = result.transactionId,
                                    intent = intent,
                                    paymentResult = result
                                )

                                verification.fold(
                                    onSuccess = {
                                        _processorState.value = PaymentProcessorState.Success(
                                            transactionId = result.transactionId
                                        )
                                        Result.success(result)
                                    },
                                    onFailure = { error ->
                                        val message = error.message ?: "Payment verification failed."
                                        _processorState.value = PaymentProcessorState.Failed(message)
                                        Result.failure(error)
                                    }
                                )
                            }

                            is PaymentResult.Failure -> {
                                _processorState.value = PaymentProcessorState.Failed(
                                    result.message.ifBlank { "Payment failed." }
                                )
                                Result.success(result)
                            }
                        }
                    },
                    onFailure = { error ->
                        val message = error.message ?: "Payment processing failed."
                        _processorState.value = PaymentProcessorState.Failed(message)
                        Result.failure(error)
                    }
                )
            }
        }
    }

    override suspend fun resumePayment(transactionId: String): Result<PaymentResult> {
        require(transactionId.isNotBlank()) { "transactionId must not be blank." }

        return operationMutex.withLock {
            withContext(Dispatchers.IO) {
                if (!isPaymentAllowed(null)) {
                    val reason = buildSecurityReason(null)
                    updateState(PaymentProcessorState.Failed(reason))
                    return@withContext Result.failure(IllegalStateException(reason))
                }

                _processorState.value = PaymentProcessorState.Verifying

                val statusResult = gateway.getPaymentStatus(transactionId)
                statusResult.fold(
                    onSuccess = { status ->
                        val result = mapStatusToResult(transactionId, status)

                        when (result) {
                            is PaymentResult.Success -> {
                                _processorState.value = PaymentProcessorState.Success(transactionId)
                            }

                            is PaymentResult.RequiresAction -> {
                                _processorState.value = PaymentProcessorState.RequiresAction(
                                    actionMessage = result.message ?: "Action required."
                                )
                            }

                            is PaymentResult.Pending -> {
                                _processorState.value = PaymentProcessorState.Verifying
                            }

                            is PaymentResult.Failure -> {
                                _processorState.value = PaymentProcessorState.Failed(
                                    result.message.ifBlank { "Payment could not be resumed." }
                                )
                            }
                        }

                        Result.success(result)
                    },
                    onFailure = { error ->
                        val message = error.message ?: "Failed to resume payment."
                        _processorState.value = PaymentProcessorState.Failed(message)
                        Result.failure(error)
                    }
                )
            }
        }
    }

    fun getRefundService(): RefundService = refundService

    fun getVerificationService(): PaymentVerificationService = verificationService

    fun resetState() {
        _processorState.value = PaymentProcessorState.Idle
    }

    private suspend fun verifyPostPayment(
        transactionId: String,
        intent: PaymentIntent,
        paymentResult: PaymentResult.Success
    ): Result<Unit> {
        val productId = expectedProductId(intent)
        val purchaseToken = intent.metadata["purchaseToken"]?.takeIf { it.isNotBlank() }
        val orderId = intent.metadata["orderId"]?.takeIf { it.isNotBlank() }
        val expiresAtMillis = intent.metadata["expiresAtMillis"]?.toLongOrNull()
        val serverTimeMillis = intent.metadata["serverTimeMillis"]?.toLongOrNull()

        return when {
            !purchaseToken.isNullOrBlank() && !productId.isNullOrBlank() -> {
                verificationService.verifyAndCache(
                    transactionId = transactionId,
                    purchaseToken = purchaseToken,
                    productId = productId,
                    orderId = orderId,
                    expiresAtMillis = expiresAtMillis,
                    serverTimeMillis = serverTimeMillis,
                    expectedProductId = productId
                ).map { Unit }
            }

            else -> {
                verificationService.verifyTransaction(
                    transactionId = transactionId,
                    expectedProductId = productId
                ).map { Unit }
            }
        }
    }

    private fun isPaymentAllowed(expectedProductId: String?): Boolean {
        return SessionManager.hasValidSession() &&
            PaymentFraudGuard.canProceed(appContext, expectedProductId) &&
            !TamperProtection.shouldRestrictSensitiveOperations(appContext) &&
            !HookDetection.shouldRestrictSensitiveOperations()
    }

    private fun buildSecurityReason(expectedProductId: String?): String {
        val reasons = PaymentFraudGuard.getRiskReasons(appContext, expectedProductId)
        return when {
            reasons.isNotEmpty() -> reasons.joinToString(separator = ",")
            !SessionManager.hasValidSession() -> "no_session"
            TamperProtection.shouldRestrictSensitiveOperations(appContext) -> "tamper"
            HookDetection.shouldRestrictSensitiveOperations() -> "hook"
            else -> "security_gate_blocked"
        }
    }

    private fun updateState(state: PaymentProcessorState) {
        _processorState.value = state
    }

    private fun expectedProductId(intent: PaymentIntent): String? {
        return intent.metadata["productId"]?.takeIf { it.isNotBlank() }
            ?: intent.metadata["sku"]?.takeIf { it.isNotBlank() }
            ?: intent.metadata["subscriptionId"]?.takeIf { it.isNotBlank() }
    }

    private fun resolveIdempotencyKey(intent: PaymentIntent): String {
        return intent.idempotencyKey
            ?: intent.metadata["idempotencyKey"]
            ?: "${intent.transactionId}:${intent.intentId}"
    }

    private fun mapStatusToResult(
        transactionId: String,
        status: PaymentStatus
    ): PaymentResult {
        return when (status) {
            PaymentStatus.PAID,
            PaymentStatus.AUTHORIZED -> {
                PaymentResult.Success(transactionId = transactionId)
            }

            PaymentStatus.PENDING,
            PaymentStatus.PROCESSING -> {
                PaymentResult.Pending(
                    transactionId = transactionId,
                    status = status
                )
            }

            PaymentStatus.REQUIRES_ACTION -> {
                PaymentResult.RequiresAction(
                    transactionId = transactionId,
                    actionType = com.myimdad_por.core.payment.models.PaymentActionType.UNKNOWN,
                    message = "Additional verification required."
                )
            }

            PaymentStatus.REFUNDED,
            PaymentStatus.PARTIALLY_REFUNDED -> {
                PaymentResult.Failure(
                    transactionId = transactionId,
                    errorCode = "payment_refunded",
                    message = "Payment has been refunded."
                )
            }

            PaymentStatus.FAILED,
            PaymentStatus.CANCELED,
            PaymentStatus.EXPIRED -> {
                PaymentResult.Failure(
                    transactionId = transactionId,
                    errorCode = status.name.lowercase(),
                    message = "Payment status: ${status.name.lowercase().replace('_', ' ')}"
                )
            }
        }
    }
}