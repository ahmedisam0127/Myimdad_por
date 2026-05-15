package com.myimdad_por.domain.usecase

import com.myimdad_por.domain.model.PaymentRecord
import com.myimdad_por.domain.repository.PaymentRepository
import java.math.BigDecimal
import javax.inject.Inject

data class RefundPaymentRequest(
    val paymentId: String,
    val amount: BigDecimal? = null,
    val reason: String? = null
) {
    init {
        require(paymentId.isNotBlank()) { "paymentId cannot be blank" }
        amount?.let {
            require(it >= BigDecimal.ZERO) { "amount cannot be negative" }
        }
        reason?.let {
            require(it.isNotBlank()) { "reason cannot be blank when provided" }
        }
    }
}

data class RefundPaymentResult(
    val payment: PaymentRecord,
    val refundedAmount: BigDecimal? = null
) {
    val isPartialRefund: Boolean
        get() = refundedAmount != null && refundedAmount > BigDecimal.ZERO
}

class RefundPaymentUseCase @Inject constructor(
    private val paymentRepository: PaymentRepository
) {

    suspend operator fun invoke(request: RefundPaymentRequest): Result<RefundPaymentResult> {
        return runCatching {
            val updatedPayment = paymentRepository
                .refundPayment(
                    paymentId = request.paymentId.trim(),
                    amount = request.amount,
                    reason = request.reason?.trim()?.takeIf { it.isNotBlank() }
                )
                .getOrThrow()

            RefundPaymentResult(
                payment = updatedPayment,
                refundedAmount = request.amount
            )
        }
    }

    suspend fun fullRefund(
        paymentId: String,
        reason: String? = null
    ): Result<PaymentRecord> {
        require(paymentId.isNotBlank()) { "paymentId cannot be blank" }

        return paymentRepository.refundPayment(
            paymentId = paymentId.trim(),
            amount = null,
            reason = reason?.trim()?.takeIf { it.isNotBlank() }
        )
    }

    suspend fun partialRefund(
        paymentId: String,
        amount: BigDecimal,
        reason: String? = null
    ): Result<PaymentRecord> {
        require(paymentId.isNotBlank()) { "paymentId cannot be blank" }
        require(amount > BigDecimal.ZERO) { "amount must be greater than zero" }

        return paymentRepository.refundPayment(
            paymentId = paymentId.trim(),
            amount = amount,
            reason = reason?.trim()?.takeIf { it.isNotBlank() }
        )
    }
}