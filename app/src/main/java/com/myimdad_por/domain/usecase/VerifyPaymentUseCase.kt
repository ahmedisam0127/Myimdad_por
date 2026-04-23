package com.myimdad_por.domain.usecase

import com.myimdad_por.domain.model.CurrencyCode
import com.myimdad_por.domain.model.PaymentRecord
import com.myimdad_por.domain.model.PaymentVerification
import com.myimdad_por.domain.model.VerificationStatus
import com.myimdad_por.domain.repository.PaymentRepository
import java.math.BigDecimal
import javax.inject.Inject

data class VerifyPaymentRequest(
    val transactionId: String,
    val paymentId: String? = null,
    val expectedAmount: BigDecimal? = null,
    val expectedCurrency: CurrencyCode? = null,
    val recordVerification: Boolean = true,
    val strictAmountMatch: Boolean = true
) {
    init {
        require(transactionId.isNotBlank()) { "transactionId cannot be blank" }
        expectedAmount?.let {
            require(it >= BigDecimal.ZERO) { "expectedAmount cannot be negative" }
        }
    }
}

data class VerifyPaymentResult(
    val verification: PaymentVerification,
    val linkedPaymentId: String? = null
) {
    val isVerified: Boolean
        get() = verification.isSuccessful
}

class VerifyPaymentUseCase @Inject constructor(
    private val paymentRepository: PaymentRepository
) {

    suspend operator fun invoke(request: VerifyPaymentRequest): Result<VerifyPaymentResult> {
        return runCatching {
            val remoteVerification = paymentRepository
                .verifyElectronicPayment(request.transactionId)
                .getOrThrow()

            val amountMatched = resolveAmountMatch(
                verification = remoteVerification,
                expectedAmount = request.expectedAmount,
                strictAmountMatch = request.strictAmountMatch
            )

            val currencyMatched = resolveCurrencyMatch(
                verification = remoteVerification,
                expectedCurrency = request.expectedCurrency
            )

            val finalVerification = remoteVerification.copy(
                verified = remoteVerification.verified && amountMatched && currencyMatched,
                amountMatched = amountMatched,
                status = resolveFinalStatus(
                    currentStatus = remoteVerification.status,
                    verified = remoteVerification.verified && amountMatched && currencyMatched
                ),
                reason = buildReason(
                    baseReason = remoteVerification.reason,
                    amountMatched = amountMatched,
                    currencyMatched = currencyMatched
                )
            )

            val linkedPaymentId = request.paymentId
                ?.trim()
                ?.takeIf { it.isNotBlank() }
                ?.let { paymentId ->
                    if (request.recordVerification) {
                        paymentRepository.recordVerification(
                            paymentId = paymentId,
                            verification = finalVerification
                        ).getOrThrow()
                    }
                    paymentId
                }

            VerifyPaymentResult(
                verification = finalVerification,
                linkedPaymentId = linkedPaymentId
            )
        }
    }

    suspend fun byTransactionId(transactionId: String): Result<PaymentVerification> {
        return invoke(VerifyPaymentRequest(transactionId = transactionId))
            .map { it.verification }
    }

    suspend fun attachVerification(
        paymentId: String,
        verification: PaymentVerification
    ): Result<PaymentRecord> {
        require(paymentId.isNotBlank()) { "paymentId cannot be blank" }
        return paymentRepository.recordVerification(paymentId, verification)
    }

    private fun resolveAmountMatch(
        verification: PaymentVerification,
        expectedAmount: BigDecimal?,
        strictAmountMatch: Boolean
    ): Boolean {
        return when {
            expectedAmount == null -> verification.amountMatched
            strictAmountMatch -> verification.amount == expectedAmount
            else -> verification.amount == expectedAmount || verification.amountMatched
        }
    }

    private fun resolveCurrencyMatch(
        verification: PaymentVerification,
        expectedCurrency: CurrencyCode?
    ): Boolean {
        return expectedCurrency == null ||
            verification.normalizedCurrencyCode == expectedCurrency.code
    }

    private fun resolveFinalStatus(
        currentStatus: VerificationStatus,
        verified: Boolean
    ): VerificationStatus {
        return when {
            verified -> VerificationStatus.VERIFIED
            currentStatus == VerificationStatus.FAILED -> VerificationStatus.FAILED
            currentStatus == VerificationStatus.REJECTED -> VerificationStatus.REJECTED
            else -> VerificationStatus.PENDING
        }
    }

    private fun buildReason(
        baseReason: String?,
        amountMatched: Boolean,
        currencyMatched: Boolean
    ): String? {
        val notes = buildList {
            baseReason?.trim()?.takeIf { it.isNotBlank() }?.let { add(it) }
            if (!amountMatched) add("amount mismatch")
            if (!currencyMatched) add("currency mismatch")
        }

        return notes.takeIf { it.isNotEmpty() }?.joinToString(separator = " | ")
    }
}