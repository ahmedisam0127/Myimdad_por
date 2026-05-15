package com.myimdad_por.core.payment.contracts

import com.myimdad_por.core.payment.models.PaymentReceipt
import com.myimdad_por.core.payment.models.PaymentVerification

/**
 * Verifies the financial truth of a payment result.
 *
 * This layer should be used to:
 * - validate receipts
 * - cross-check transaction details
 * - confirm backend settlement
 */
interface IPaymentVerifier {

    suspend fun verifyReceipt(receipt: PaymentReceipt): Result<PaymentVerification>

    suspend fun doubleCheckWithBackend(transactionId: String): Boolean
}