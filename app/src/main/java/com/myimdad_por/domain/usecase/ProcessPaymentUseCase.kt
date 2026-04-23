package com.myimdad_por.domain.usecase

import com.myimdad_por.domain.model.CurrencyCode
import com.myimdad_por.domain.model.PaymentRecord
import com.myimdad_por.domain.model.PaymentVerification
import com.myimdad_por.domain.repository.PaymentRepository
import java.math.BigDecimal
import javax.inject.Inject

data class ProcessPaymentRequest(
    val payment: PaymentRecord,
    val paymentId: String? = null,
    val verifyAfterProcess: Boolean = false,
    val transactionId: String? = null,
    val expectedAmount: BigDecimal? = null,
    val expectedCurrency: CurrencyCode? = null,
    val recordVerificationOnPayment: Boolean = true
)

data class ProcessPaymentResult(
    val payment: PaymentRecord,
    val verification: PaymentVerification? = null,
    val verified: Boolean = false
) {
    val isVerified: Boolean
        get() = verified && verification?.isSuccessful == true
}

class ProcessPaymentUseCase @Inject constructor(
    private val paymentRepository: PaymentRepository,
    private val verifyPaymentUseCase: VerifyPaymentUseCase
) {

    suspend operator fun invoke(request: ProcessPaymentRequest): Result<ProcessPaymentResult> {
        return runCatching {
            val processedPayment = paymentRepository
                .processPayment(request.payment)
                .getOrThrow()

            val verification = resolveVerification(request, processedPayment)

            ProcessPaymentResult(
                payment = processedPayment,
                verification = verification,
                verified = verification?.isSuccessful == true
            )
        }
    }

    private suspend fun resolveVerification(
        request: ProcessPaymentRequest,
        processedPayment: PaymentRecord
    ): PaymentVerification? {
        if (!request.verifyAfterProcess) return null

        val transactionId = request.transactionId?.trim().orEmpty()
        if (transactionId.isBlank()) return null

        val verificationResult = verifyPaymentUseCase(
            VerifyPaymentRequest(
                transactionId = transactionId,
                paymentId = request.paymentId?.trim()?.takeIf { it.isNotBlank() },
                expectedAmount = request.expectedAmount,
                expectedCurrency = request.expectedCurrency,
                recordVerification = request.recordVerificationOnPayment
            )
        ).getOrNull() ?: return null

        return verificationResult.verification
    }

    suspend fun quickPay(payment: PaymentRecord): Result<PaymentRecord> {
        return paymentRepository.processPayment(payment)
    }

    suspend fun save(payment: PaymentRecord): Result<PaymentRecord> {
        return paymentRepository.savePayment(payment)
    }
}