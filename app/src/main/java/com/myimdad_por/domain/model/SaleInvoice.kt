package com.myimdad_por.domain.model

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

/**
 * فاتورة المبيعات: المستند القانوني والضريبي.
 */
data class SaleInvoice(
    val id: String = UUID.randomUUID().toString(),
    val invoiceNumber: String,
    val saleId: String,
    val status: SaleInvoiceStatus = SaleInvoiceStatus.DRAFT,
    val issueDate: LocalDateTime,
    val dueDate: LocalDateTime? = null,
    val taxReference: String? = null,
    val customerSnapshot: CustomerSnapshot? = null,
    val employeeId: String,
    val items: List<SaleItem>,
    val paidAmount: BigDecimal = BigDecimal.ZERO,
    val notes: String? = null,
    val termsAndConditions: String? = null,
    val qrPayload: String? = null
) {

    init {
        require(invoiceNumber.isNotBlank()) { "invoiceNumber cannot be blank" }
        require(saleId.isNotBlank()) { "saleId cannot be blank" }
        require(employeeId.isNotBlank()) { "employeeId cannot be blank" }
        require(items.isNotEmpty()) { "items cannot be empty" }
        require(paidAmount >= BigDecimal.ZERO) { "paidAmount cannot be negative" }
        require(!issueDate.isAfter(LocalDateTime.now().plusSeconds(5))) {
            "issueDate cannot be in the future"
        }
        dueDate?.let {
            require(!it.isBefore(issueDate)) { "dueDate cannot be before issueDate" }
        }
    }

    val subtotalAmount: BigDecimal
        get() = items.fold(BigDecimal.ZERO) { acc: BigDecimal, item: SaleItem ->
            acc.add(item.calculateSubtotal())
        }.money()

    val taxAmount: BigDecimal
        get() = items.fold(BigDecimal.ZERO) { acc: BigDecimal, item: SaleItem ->
            acc.add(item.netTaxAmount)
        }.money()

    val discountAmount: BigDecimal
        get() = items.fold(BigDecimal.ZERO) { acc: BigDecimal, item: SaleItem ->
            acc.add(item.netDiscountAmount)
        }.money()

    val totalAmount: BigDecimal
        get() = subtotalAmount
            .add(taxAmount)
            .subtract(discountAmount)
            .money()

    val remainingAmount: BigDecimal
        get() {
            val remaining = totalAmount.subtract(paidAmount)
            return if (remaining > BigDecimal.ZERO) remaining.money() else BigDecimal.ZERO.setScale(2)
        }

    val paymentStatus: PaymentStatus
        get() = when {
            totalAmount <= BigDecimal.ZERO -> PaymentStatus.PAID
            remainingAmount == BigDecimal.ZERO && paidAmount > BigDecimal.ZERO -> PaymentStatus.PAID
            paidAmount > BigDecimal.ZERO -> PaymentStatus.PARTIALLY_PAID
            else -> PaymentStatus.PENDING
        }

    fun isTaxInvoice(): Boolean {
        return !taxReference.isNullOrBlank() &&
            taxAmount > BigDecimal.ZERO &&
            customerSnapshot?.taxNumber?.isNotBlank() == true
    }

    fun daysUntilDue(today: LocalDateTime = LocalDateTime.now()): Long? {
        val due = dueDate ?: return null
        return ChronoUnit.DAYS.between(today.toLocalDate(), due.toLocalDate())
    }

    fun isOverdue(today: LocalDateTime = LocalDateTime.now()): Boolean {
        val due = dueDate ?: return false
        return paymentStatus != PaymentStatus.PAID && today.isAfter(due)
    }

    fun isFullyPaid(): Boolean = paymentStatus == PaymentStatus.PAID

    fun isCancelled(): Boolean =
        status == SaleInvoiceStatus.VOID || status == SaleInvoiceStatus.CANCELLED

    fun canBeEdited(): Boolean = status.isEditable

    fun requiresInventoryReversalOnCancel(): Boolean = status.reversesInventoryOnCancel

    fun withPayment(amount: BigDecimal): SaleInvoice {
        require(amount >= BigDecimal.ZERO) { "amount cannot be negative" }
        return copy(paidAmount = paidAmount.add(amount).money())
    }

    fun markIssued(): SaleInvoice = copy(status = SaleInvoiceStatus.ISSUED)

    fun markPaid(): SaleInvoice = copy(
        status = SaleInvoiceStatus.PAID,
        paidAmount = totalAmount.money()
    )

    fun markOverdue(today: LocalDateTime = LocalDateTime.now()): SaleInvoice {
        return if (isOverdue(today)) copy(status = SaleInvoiceStatus.OVERDUE) else this
    }

    fun toQrText(): String {
        val customerPart = customerSnapshot?.let {
            "${it.name}|${it.taxNumber ?: ""}|${it.address ?: ""}"
        } ?: ""

        return buildString {
            append("INV=").append(invoiceNumber)
            append(";SALE=").append(saleId)
            append(";DATE=").append(issueDate)
            append(";DUE=").append(dueDate ?: "")
            append(";TOTAL=").append(totalAmount)
            append(";PAID=").append(paidAmount)
            append(";TAXREF=").append(taxReference ?: "")
            append(";CUSTOMER=").append(customerPart)
        }
    }

    companion object {
        fun fromSale(
            sale: Sale,
            invoiceNumber: String,
            dueDate: LocalDateTime? = null,
            taxReference: String? = null,
            customerSnapshot: CustomerSnapshot? = null,
            termsAndConditions: String? = null,
            qrPayload: String? = null
        ): SaleInvoice {
            return SaleInvoice(
                invoiceNumber = invoiceNumber,
                saleId = sale.id,
                status = when {
                    sale.isFullyPaid() -> SaleInvoiceStatus.PAID
                    sale.remainingAmount > BigDecimal.ZERO && sale.paidAmount > BigDecimal.ZERO ->
                        SaleInvoiceStatus.PARTIALLY_PAID
                    dueDate != null -> SaleInvoiceStatus.OPEN
                    else -> SaleInvoiceStatus.ISSUED
                },
                issueDate = sale.createdAt,
                dueDate = dueDate,
                taxReference = taxReference,
                customerSnapshot = customerSnapshot,
                employeeId = sale.employeeId,
                items = sale.items,
                paidAmount = sale.paidAmount,
                notes = sale.note,
                termsAndConditions = termsAndConditions,
                qrPayload = qrPayload ?: buildQrPayload(
                    invoiceNumber = invoiceNumber,
                    sale = sale,
                    dueDate = dueDate,
                    taxReference = taxReference,
                    customerSnapshot = customerSnapshot
                )
            )
        }

        private fun buildQrPayload(
            invoiceNumber: String,
            sale: Sale,
            dueDate: LocalDateTime?,
            taxReference: String?,
            customerSnapshot: CustomerSnapshot?
        ): String {
            val customerPart = customerSnapshot?.let {
                "${it.name}|${it.taxNumber ?: ""}|${it.address ?: ""}"
            } ?: ""

            return buildString {
                append("INV=").append(invoiceNumber)
                append(";SALE=").append(sale.id)
                append(";DATE=").append(sale.createdAt)
                append(";DUE=").append(dueDate ?: "")
                append(";TOTAL=").append(sale.totalAmount)
                append(";PAID=").append(sale.paidAmount)
                append(";TAXREF=").append(taxReference ?: "")
                append(";CUSTOMER=").append(customerPart)
            }
        }
    }
}

data class CustomerSnapshot(
    val name: String,
    val address: String? = null,
    val taxNumber: String? = null,
    val phone: String? = null
) {
    init {
        require(name.isNotBlank()) { "customer name cannot be blank" }
    }
}

private fun BigDecimal.money(): BigDecimal = this.setScale(2, RoundingMode.HALF_UP)