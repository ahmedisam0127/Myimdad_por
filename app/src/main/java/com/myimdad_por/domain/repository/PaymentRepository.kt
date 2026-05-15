package com.myimdad_por.domain.repository

import com.myimdad_por.domain.model.CurrencyCode
import com.myimdad_por.domain.model.Invoice
import com.myimdad_por.domain.model.PaymentMethod
import com.myimdad_por.domain.model.PaymentRecord
import com.myimdad_por.domain.model.PaymentStatus
import com.myimdad_por.domain.model.PaymentTransaction
import com.myimdad_por.domain.model.PaymentVerification
import java.math.BigDecimal
import java.time.LocalDateTime
import kotlinx.coroutines.flow.Flow

/**
 * Contract for cash flow tracking, electronic payment verification,
 * refunds, and allocation management.
 */
interface PaymentRepository {

    fun observeAllPayments(): Flow<List<PaymentRecord>>

    fun observePaymentsByInvoice(invoiceId: String): Flow<List<PaymentRecord>>

    fun observePaymentsByCustomer(customerId: String): Flow<List<PaymentRecord>>

    fun observePaymentsByStatus(status: PaymentStatus): Flow<List<PaymentRecord>>

    fun observeRefunds(): Flow<List<PaymentRecord>>

    fun observeUnallocatedPayments(customerId: String? = null): Flow<List<PaymentRecord>>

    suspend fun getPaymentById(id: String): PaymentRecord?

    suspend fun getPaymentByTransactionId(transactionId: String): PaymentRecord?

    suspend fun getPayments(
        from: LocalDateTime? = null,
        to: LocalDateTime? = null,
        invoiceId: String? = null,
        customerId: String? = null,
        status: PaymentStatus? = null,
        paymentMethod: PaymentMethod? = null,
        currency: CurrencyCode? = null
    ): List<PaymentRecord>

    suspend fun processPayment(payment: PaymentRecord): Result<PaymentRecord>

    suspend fun savePayment(payment: PaymentRecord): Result<PaymentRecord>

    suspend fun savePayments(payments: List<PaymentRecord>): Result<List<PaymentRecord>>

    suspend fun updatePayment(payment: PaymentRecord): Result<PaymentRecord>

    suspend fun linkPaymentToInvoice(
        paymentId: String,
        invoiceId: String
    ): Result<PaymentRecord>

    suspend fun unlinkPaymentFromInvoice(paymentId: String): Result<PaymentRecord>

    suspend fun refundPayment(
        paymentId: String,
        amount: BigDecimal? = null,
        reason: String? = null
    ): Result<PaymentRecord>

    suspend fun verifyElectronicPayment(transactionId: String): Result<PaymentVerification>

    suspend fun recordVerification(
        paymentId: String,
        verification: PaymentVerification
    ): Result<PaymentRecord>

    suspend fun getPaymentSummary(
        from: LocalDateTime? = null,
        to: LocalDateTime? = null,
        currency: CurrencyCode? = null
    ): PaymentSummary

    suspend fun getTotalReceived(
        from: LocalDateTime? = null,
        to: LocalDateTime? = null,
        currency: CurrencyCode? = null
    ): BigDecimal

    suspend fun getUnallocatedPayments(customerId: String): List<PaymentRecord>

    suspend fun getPendingElectronicPayments(): List<PaymentRecord>

    suspend fun deletePayment(id: String): Result<Unit>

    suspend fun deletePayments(ids: List<String>): Result<Int>

    suspend fun clearAll(): Result<Unit>

    suspend fun countPayments(): Long

    suspend fun countPaymentsByStatus(status: PaymentStatus): Long

    suspend fun countRefundedPayments(): Long

    suspend fun getPaymentsPendingSync(): List<PaymentRecord>

    suspend fun markPaymentSynced(paymentId: String): Result<PaymentRecord>
}

/**
 * High-level payment flow summary for dashboards and reports.
 */
data class PaymentSummary(
    val currency: CurrencyCode,
    val totalReceived: BigDecimal,
    val totalRefunded: BigDecimal,
    val totalPending: BigDecimal,
    val cashTotal: BigDecimal,
    val bankTotal: BigDecimal,
    val walletTotal: BigDecimal,
    val cardTotal: BigDecimal,
    val paymentCount: Long,
    val refundedCount: Long,
    val pendingCount: Long
) {
    init {
        require(totalReceived >= BigDecimal.ZERO) { "totalReceived cannot be negative." }
        require(totalRefunded >= BigDecimal.ZERO) { "totalRefunded cannot be negative." }
        require(totalPending >= BigDecimal.ZERO) { "totalPending cannot be negative." }
        require(cashTotal >= BigDecimal.ZERO) { "cashTotal cannot be negative." }
        require(bankTotal >= BigDecimal.ZERO) { "bankTotal cannot be negative." }
        require(walletTotal >= BigDecimal.ZERO) { "walletTotal cannot be negative." }
        require(cardTotal >= BigDecimal.ZERO) { "cardTotal cannot be negative." }
        require(paymentCount >= 0L) { "paymentCount cannot be negative." }
        require(refundedCount >= 0L) { "refundedCount cannot be negative." }
        require(pendingCount >= 0L) { "pendingCount cannot be negative." }
    }

    val netCashFlow: BigDecimal
        get() = totalReceived.subtract(totalRefunded)
}