package com.myimdad_por.domain.repository

import com.myimdad_por.domain.model.CurrencyCode
import com.myimdad_por.domain.model.Invoice
import com.myimdad_por.domain.model.InvoiceStatus
import java.math.BigDecimal
import java.time.LocalDateTime
import kotlinx.coroutines.flow.Flow

/**
 * Contract for invoice persistence, lifecycle tracking, and revenue reporting.
 */
interface InvoiceRepository {

    fun observeAllInvoices(): Flow<List<Invoice>>

    fun observeInvoicesByCustomer(customerId: String): Flow<List<Invoice>>

    fun observeInvoicesByStatus(status: InvoiceStatus): Flow<List<Invoice>>

    fun observeOverdueInvoices(): Flow<List<Invoice>>

    fun observeInvoicesByEmployee(employeeId: String): Flow<List<Invoice>>

    suspend fun getInvoiceById(id: String): Invoice?

    suspend fun getInvoiceByNumber(invoiceNumber: String): Invoice?

    suspend fun getNextInvoiceNumber(): String

    suspend fun getInvoices(
        from: LocalDateTime? = null,
        to: LocalDateTime? = null,
        status: InvoiceStatus? = null,
        customerId: String? = null,
        employeeId: String? = null,
        currency: CurrencyCode? = null
    ): List<Invoice>

    suspend fun saveInvoice(invoice: Invoice): Result<Invoice>

    suspend fun saveInvoices(invoices: List<Invoice>): Result<List<Invoice>>

    suspend fun updateInvoice(invoice: Invoice): Result<Invoice>

    suspend fun updateInvoiceStatus(
        invoiceId: String,
        status: InvoiceStatus,
        note: String? = null
    ): Result<Invoice>

    suspend fun markInvoiceIssued(invoiceId: String): Result<Invoice>

    suspend fun markInvoicePaid(
        invoiceId: String,
        paidAmount: BigDecimal? = null
    ): Result<Invoice>

    suspend fun markInvoiceOverdue(invoiceId: String): Result<Invoice>

    suspend fun voidInvoice(
        invoiceId: String,
        reason: String
    ): Result<Invoice>

    suspend fun deleteInvoice(id: String): Result<Unit>

    suspend fun deleteInvoices(ids: List<String>): Result<Int>

    suspend fun clearAll(): Result<Unit>

    suspend fun countInvoices(): Long

    suspend fun countInvoicesByStatus(status: InvoiceStatus): Long

    suspend fun countOverdueInvoices(): Long

    suspend fun calculateTotalRevenue(
        from: LocalDateTime? = null,
        to: LocalDateTime? = null,
        currency: CurrencyCode? = null
    ): BigDecimal

    suspend fun calculateOutstandingBalance(
        customerId: String? = null,
        from: LocalDateTime? = null,
        to: LocalDateTime? = null,
        currency: CurrencyCode? = null
    ): BigDecimal

    suspend fun getInvoicesPendingSync(): List<Invoice>

    suspend fun markInvoiceSynced(invoiceId: String): Result<Invoice>
}

/**
 * Optional domain snapshot for invoice analytics.
 */
data class InvoiceRevenueSummary(
    val currency: CurrencyCode,
    val totalRevenue: BigDecimal,
    val totalOutstanding: BigDecimal,
    val invoiceCount: Long,
    val paidCount: Long,
    val overdueCount: Long
) {
    init {
        require(totalRevenue >= BigDecimal.ZERO) { "totalRevenue cannot be negative." }
        require(totalOutstanding >= BigDecimal.ZERO) { "totalOutstanding cannot be negative." }
        require(invoiceCount >= 0L) { "invoiceCount cannot be negative." }
        require(paidCount >= 0L) { "paidCount cannot be negative." }
        require(overdueCount >= 0L) { "overdueCount cannot be negative." }
    }
}