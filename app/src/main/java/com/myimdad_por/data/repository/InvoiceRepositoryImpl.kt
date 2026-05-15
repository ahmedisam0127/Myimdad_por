package com.myimdad_por.data.repository

import com.myimdad_por.core.dispatchers.AppDispatchers
import com.myimdad_por.core.dispatchers.DefaultAppDispatchers
import com.myimdad_por.data.local.dao.InvoiceDao
import com.myimdad_por.data.local.entity.InvoiceEntity
import com.myimdad_por.data.local.entity.toDomain
import com.myimdad_por.domain.model.CurrencyCode
import com.myimdad_por.domain.model.Invoice
import com.myimdad_por.domain.model.InvoiceLine
import com.myimdad_por.domain.model.InvoiceStatus
import com.myimdad_por.domain.model.PaymentStatus
import com.myimdad_por.domain.repository.InvoiceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDateTime
import javax.inject.Inject
import java.time.ZoneId
import java.util.UUID


class InvoiceRepositoryImpl @Inject constructor(
    private val invoiceDao: InvoiceDao,
    private val dispatchers: AppDispatchers = DefaultAppDispatchers
) : InvoiceRepository {

    override fun observeAllInvoices(): Flow<List<Invoice>> {
        return invoiceDao.observeAll()
            .map { entities -> entities.map { it.toDomain() } }
            .flowOn(dispatchers.io)
    }

    override fun observeInvoicesByCustomer(customerId: String): Flow<List<Invoice>> {
        val normalizedId = customerId.trim()
        if (normalizedId.isBlank()) return flowOf(emptyList())

        return invoiceDao.observeByPartyId(normalizedId)
            .map { entities -> entities.map { it.toDomain() } }
            .flowOn(dispatchers.io)
    }

    override fun observeInvoicesByStatus(status: InvoiceStatus): Flow<List<Invoice>> {
        return invoiceDao.observeByStatus(status.name)
            .map { entities -> entities.map { it.toDomain() } }
            .flowOn(dispatchers.io)
    }

    override fun observeOverdueInvoices(): Flow<List<Invoice>> {
        return invoiceDao.observeOverdue()
            .map { entities -> entities.map { it.toDomain() } }
            .flowOn(dispatchers.io)
    }

    override fun observeInvoicesByEmployee(employeeId: String): Flow<List<Invoice>> {
        val normalizedId = employeeId.trim()
        if (normalizedId.isBlank()) return flowOf(emptyList())

        return invoiceDao.observeByEmployeeId(normalizedId)
            .map { entities -> entities.map { it.toDomain() } }
            .flowOn(dispatchers.io)
    }

    override suspend fun getInvoiceById(id: String): Invoice? = withContext(dispatchers.io) {
        val normalizedId = id.trim()
        if (normalizedId.isBlank()) return@withContext null

        invoiceDao.getById(normalizedId)?.toDomain()
            ?: invoiceDao.getByServerId(normalizedId)?.toDomain()
    }

    override suspend fun getInvoiceByNumber(invoiceNumber: String): Invoice? = withContext(dispatchers.io) {
        val normalizedNum = invoiceNumber.trim()
        if (normalizedNum.isBlank()) return@withContext null

        invoiceDao.getByInvoiceNumber(normalizedNum)?.toDomain()
    }

    override suspend fun getNextInvoiceNumber(): String = withContext(dispatchers.io) {
        val lastInvoice = invoiceDao.observeAll().first().firstOrNull()
        if (lastInvoice != null) {
            val numericPart = lastInvoice.invoiceNumber.filter { it.isDigit() }
            val nextInt = (numericPart.toLongOrNull() ?: 1000L) + 1
            "INV-$nextInt"
        } else {
            "INV-1001"
        }
    }

    override suspend fun getInvoices(
        from: LocalDateTime?,
        to: LocalDateTime?,
        status: InvoiceStatus?,
        customerId: String?,
        employeeId: String?,
        currency: CurrencyCode?
    ): List<Invoice> = withContext(dispatchers.io) {
        val allInvoices = invoiceDao.observeAll().first().map { it.toDomain() }

        allInvoices.asSequence()
            .filter { from == null || !it.issueDate.isBefore(from) }
            .filter { to == null || !it.issueDate.isAfter(to) }
            .filter { status == null || it.status == status }
            .filter { customerId == null || it.partyId == customerId }
            .filter { employeeId == null || it.issuedByEmployeeId == employeeId }
            .toList()
    }

    override suspend fun saveInvoice(invoice: Invoice): Result<Invoice> = withContext(dispatchers.io) {
        runCatching {
            val normalized = invoice.normalize()
            val existing = invoiceDao.getById(normalized.id)
                ?: invoiceDao.getByInvoiceNumber(normalized.invoiceNumber)

            val entity = InvoiceEntity.fromDomain(
                invoice = normalized,
                serverId = existing?.serverId,
                syncState = "PENDING",
                isDeleted = existing?.isDeleted ?: false,
                syncedAtMillis = existing?.syncedAtMillis,
                createdAtMillis = existing?.createdAtMillis ?: System.currentTimeMillis(),
                updatedAtMillis = System.currentTimeMillis()
            )

            invoiceDao.insert(entity)
            normalized
        }
    }

    override suspend fun saveInvoices(invoices: List<Invoice>): Result<List<Invoice>> = withContext(dispatchers.io) {
        runCatching {
            if (invoices.isEmpty()) return@runCatching emptyList()

            invoices.map { invoice ->
                saveInvoice(invoice).getOrElse { invoice }
            }
        }
    }

    override suspend fun updateInvoice(invoice: Invoice): Result<Invoice> {
        return saveInvoice(invoice)
    }

    override suspend fun updateInvoiceStatus(
        invoiceId: String,
        status: InvoiceStatus,
        note: String?
    ): Result<Invoice> = withContext(dispatchers.io) {
        runCatching {
            val normalizedId = invoiceId.trim()
            invoiceDao.getById(normalizedId) ?: error("Invoice not found.")

            invoiceDao.updateStatus(
                id = normalizedId,
                status = status.name,
                updatedAtMillis = System.currentTimeMillis(),
                syncState = "PENDING"
            )

            invoiceDao.getById(normalizedId)!!.toDomain()
        }
    }

    override suspend fun markInvoiceIssued(invoiceId: String): Result<Invoice> {
        return updateInvoiceStatus(invoiceId, InvoiceStatus.ISSUED)
    }

    override suspend fun markInvoicePaid(
        invoiceId: String,
        paidAmount: BigDecimal?
    ): Result<Invoice> = withContext(dispatchers.io) {
        runCatching {
            val normalizedId = invoiceId.trim()
            val current = invoiceDao.getById(normalizedId)?.toDomain()
                ?: error("Invoice not found.")

            val total = current.calculateTotal()
            val newPaidAmount = paidAmount ?: total

            val paymentStatus = when {
                newPaidAmount >= total -> PaymentStatus.PAID
                newPaidAmount > BigDecimal.ZERO -> resolvePaymentStatus(
                    "PARTIALLY_PAID",
                    "PARTIAL_PAID",
                    "PARTIAL"
                )
                else -> PaymentStatus.PENDING
            }

            invoiceDao.updatePaymentSnapshot(
                id = normalizedId,
                paymentStatus = paymentStatus.name,
                paidAmount = newPaidAmount.asMoneyText(),
                updatedAtMillis = System.currentTimeMillis(),
                syncState = "PENDING"
            )

            if (paymentStatus == PaymentStatus.PAID && current.status != InvoiceStatus.PAID) {
                invoiceDao.updateStatus(
                    id = normalizedId,
                    status = InvoiceStatus.PAID.name,
                    updatedAtMillis = System.currentTimeMillis(),
                    syncState = "PENDING"
                )
            }

            invoiceDao.getById(normalizedId)!!.toDomain()
        }
    }

    override suspend fun markInvoiceOverdue(invoiceId: String): Result<Invoice> {
        return updateInvoiceStatus(invoiceId, InvoiceStatus.OVERDUE)
    }

    override suspend fun voidInvoice(
        invoiceId: String,
        reason: String
    ): Result<Invoice> = withContext(dispatchers.io) {
        runCatching {
            val normalizedId = invoiceId.trim()
            val current = invoiceDao.getById(normalizedId)?.toDomain()
                ?: error("Invoice not found.")

            val updatedNotes = buildString {
                if (!current.notes.isNullOrBlank()) append(current.notes).append('\n')
                append("VOID REASON: ").append(reason)
            }

            val voidedInvoice = current.copy(
                status = InvoiceStatus.CANCELLED,
                notes = updatedNotes
            )

            saveInvoice(voidedInvoice).getOrThrow()
        }
    }

    override suspend fun deleteInvoice(id: String): Result<Unit> = withContext(dispatchers.io) {
        runCatching {
            val normalizedId = id.trim()
            invoiceDao.softDelete(normalizedId)
            Unit
        }
    }

    override suspend fun deleteInvoices(ids: List<String>): Result<Int> = withContext(dispatchers.io) {
        runCatching {
            var count = 0
            ids.mapNotNull { it.trim().takeIf(String::isNotBlank) }.distinct().forEach {
                if (deleteInvoice(it).isSuccess) count++
            }
            count
        }
    }

    override suspend fun clearAll(): Result<Unit> = withContext(dispatchers.io) {
        runCatching {
            val allInvoices = invoiceDao.observeAll().first()
            allInvoices.forEach { invoiceDao.softDelete(it.id) }
            invoiceDao.purgeDeleted()
            Unit
        }
    }

    override suspend fun countInvoices(): Long = withContext(dispatchers.io) {
        invoiceDao.countActive().toLong()
    }

    override suspend fun countInvoicesByStatus(status: InvoiceStatus): Long = withContext(dispatchers.io) {
        invoiceDao.observeByStatus(status.name).first().size.toLong()
    }

    override suspend fun countOverdueInvoices(): Long = withContext(dispatchers.io) {
        invoiceDao.observeOverdue().first().size.toLong()
    }

    override suspend fun calculateTotalRevenue(
        from: LocalDateTime?,
        to: LocalDateTime?,
        currency: CurrencyCode?
    ): BigDecimal = withContext(dispatchers.io) {
        getInvoices(from, to, status = InvoiceStatus.PAID)
            .fold(BigDecimal.ZERO) { acc, invoice -> acc + invoice.paidAmount }
            .asMoney()
    }

    override suspend fun calculateOutstandingBalance(
        customerId: String?,
        from: LocalDateTime?,
        to: LocalDateTime?,
        currency: CurrencyCode?
    ): BigDecimal = withContext(dispatchers.io) {
        getInvoices(from, to, customerId = customerId)
            .filter { it.status != InvoiceStatus.CANCELLED && it.status != InvoiceStatus.PAID }
            .fold(BigDecimal.ZERO) { acc, invoice -> acc + (invoice.calculateTotal() - invoice.paidAmount) }
            .coerceAtLeast(BigDecimal.ZERO)
            .asMoney()
    }

    override suspend fun getInvoicesPendingSync(): List<Invoice> = withContext(dispatchers.io) {
        invoiceDao.observePendingSync().first().map { it.toDomain() }
    }

    override suspend fun markInvoiceSynced(invoiceId: String): Result<Invoice> = withContext(dispatchers.io) {
        runCatching {
            val normalizedId = invoiceId.trim()
            invoiceDao.markSynced(normalizedId)
            invoiceDao.getById(normalizedId)?.toDomain() ?: error("Invoice not found after sync mark.")
        }
    }

    private fun Invoice.normalize(): Invoice {
        return copy(
            id = id.trim().ifBlank { UUID.randomUUID().toString() },
            invoiceNumber = invoiceNumber.trim(),
            partyId = partyId?.trim()?.takeIf { it.isNotBlank() },
            partyName = partyName?.trim()?.takeIf { it.isNotBlank() },
            issuedByEmployeeId = issuedByEmployeeId?.trim()?.takeIf { it.isNotBlank() },
            notes = notes?.trim()?.takeIf { it.isNotBlank() },
            termsAndConditions = termsAndConditions?.trim()?.takeIf { it.isNotBlank() }
        )
    }

    private fun Invoice.calculateTotal(): BigDecimal {
        val linesTotal = lines.fold(BigDecimal.ZERO) { acc, line ->
            acc + line.quantity.multiply(line.unitPrice)
        }
        return (linesTotal + taxAmount - discountAmount).coerceAtLeast(BigDecimal.ZERO)
    }

    private fun BigDecimal.asMoney(): BigDecimal {
        return setScale(2, RoundingMode.HALF_UP)
    }

    private fun BigDecimal.asMoneyText(): String {
        return setScale(2, RoundingMode.HALF_UP).toPlainString()
    }

    private fun BigDecimal.coerceAtLeast(min: BigDecimal): BigDecimal {
        return if (this < min) min else this
    }

    private fun resolvePaymentStatus(vararg candidates: String): PaymentStatus {
        for (candidate in candidates) {
            enumValues<PaymentStatus>()
                .firstOrNull { it.name.equals(candidate, ignoreCase = true) }
                ?.let { return it }
        }
        return PaymentStatus.PENDING
    }
}