package com.myimdad_por.domain.model

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

/**
 * فاتورة مالية عامة.
 * يمكن استخدامها للمبيعات أو المشتريات أو الفواتير الداخلية حسب نوعها.
 */
data class Invoice(
    val id: String = UUID.randomUUID().toString(),
    val invoiceNumber: String,
    val invoiceType: InvoiceType,
    val status: InvoiceStatus = InvoiceStatus.DRAFT,

    val issueDate: LocalDateTime,
    val dueDate: LocalDateTime? = null,

    val partyId: String? = null,
    val partyName: String? = null,
    val partyTaxNumber: String? = null,

    val issuedByEmployeeId: String? = null,
    val lines: List<InvoiceLine>,

    val taxAmount: BigDecimal = BigDecimal.ZERO,
    val discountAmount: BigDecimal = BigDecimal.ZERO,
    val paidAmount: BigDecimal = BigDecimal.ZERO,

    val notes: String? = null,
    val termsAndConditions: String? = null
) {

    init {
        require(invoiceNumber.isNotBlank()) { "invoiceNumber cannot be blank" }
        require(lines.isNotEmpty()) { "lines cannot be empty" }
        require(issueDate.isBefore(LocalDateTime.now().plusSeconds(5))) {
            "issueDate cannot be in the future"
        }
        require(taxAmount >= BigDecimal.ZERO) { "taxAmount cannot be negative" }
        require(discountAmount >= BigDecimal.ZERO) { "discountAmount cannot be negative" }
        require(paidAmount >= BigDecimal.ZERO) { "paidAmount cannot be negative" }

        dueDate?.let {
            require(!it.isBefore(issueDate)) { "dueDate cannot be before issueDate" }
        }
    }

    val subtotalAmount: BigDecimal
        get() = lines.fold(BigDecimal.ZERO) { acc: BigDecimal, line: InvoiceLine ->
            acc.add(line.subtotalAmount)
        }.money()

    val totalAmount: BigDecimal
        get() = subtotalAmount
            .add(taxAmount.money())
            .subtract(discountAmount.money())
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

    fun isFullyPaid(): Boolean = paymentStatus == PaymentStatus.PAID

    fun isOverdue(today: LocalDateTime = LocalDateTime.now()): Boolean {
        val due = dueDate ?: return false
        return paymentStatus != PaymentStatus.PAID && today.isAfter(due)
    }

    fun isTaxInvoice(): Boolean {
        return partyTaxNumber?.isNotBlank() == true && taxAmount > BigDecimal.ZERO
    }

    fun hasDiscount(): Boolean = discountAmount > BigDecimal.ZERO

    fun withPayment(amount: BigDecimal): Invoice {
        require(amount >= BigDecimal.ZERO) { "amount cannot be negative" }
        return copy(paidAmount = paidAmount.add(amount).money())
    }

    fun markIssued(): Invoice = copy(status = InvoiceStatus.ISSUED)

    fun markPaid(): Invoice = copy(
        status = InvoiceStatus.PAID,
        paidAmount = totalAmount.money()
    )

    fun markCancelled(): Invoice = copy(status = InvoiceStatus.CANCELLED)

    fun markOverdue(today: LocalDateTime = LocalDateTime.now()): Invoice {
        return if (isOverdue(today)) copy(status = InvoiceStatus.OVERDUE) else this
    }

    companion object {
        fun fromSaleLikeDocument(
            invoiceNumber: String,
            invoiceType: InvoiceType,
            issueDate: LocalDateTime,
            lines: List<InvoiceLine>,
            dueDate: LocalDateTime? = null,
            partyId: String? = null,
            partyName: String? = null,
            partyTaxNumber: String? = null,
            issuedByEmployeeId: String? = null,
            taxAmount: BigDecimal = BigDecimal.ZERO,
            discountAmount: BigDecimal = BigDecimal.ZERO,
            paidAmount: BigDecimal = BigDecimal.ZERO,
            notes: String? = null,
            termsAndConditions: String? = null
        ): Invoice {
            return Invoice(
                invoiceNumber = invoiceNumber,
                invoiceType = invoiceType,
                status = InvoiceStatus.DRAFT,
                issueDate = issueDate,
                dueDate = dueDate,
                partyId = partyId,
                partyName = partyName,
                partyTaxNumber = partyTaxNumber,
                issuedByEmployeeId = issuedByEmployeeId,
                lines = lines,
                taxAmount = taxAmount,
                discountAmount = discountAmount,
                paidAmount = paidAmount,
                notes = notes,
                termsAndConditions = termsAndConditions
            )
        }
    }
}

/**
 * سطر الفاتورة.
 * هنا نضع بيانات المنتج/الخدمة بشكل دقيق.
 */
data class InvoiceLine(
    val id: String = UUID.randomUUID().toString(),

    // بيانات المنتج
    val barcode: String? = null,
    val productName: String,
    val displayName: String? = null,
    val unitOfMeasure: UnitOfMeasure = UnitOfMeasure.UNIT,

    // بيانات المخزون/البيع
    val quantity: BigDecimal,
    val unitPrice: BigDecimal,

    // معلومات إضافية اختيارية
    val location: String? = null,
    val expiryDate: LocalDate? = null,

    // مالية
    val taxAmount: BigDecimal = BigDecimal.ZERO,
    val discountAmount: BigDecimal = BigDecimal.ZERO,
    val note: String? = null
) {

    init {
        require(productName.isNotBlank()) { "productName cannot be blank" }
        require(quantity > BigDecimal.ZERO) { "quantity must be greater than zero" }
        require(unitPrice >= BigDecimal.ZERO) { "unitPrice cannot be negative" }
        require(taxAmount >= BigDecimal.ZERO) { "taxAmount cannot be negative" }
        require(discountAmount >= BigDecimal.ZERO) { "discountAmount cannot be negative" }

        if (!unitOfMeasure.isDecimalAllowed) {
            require(quantity.stripTrailingZeros().scale() <= 0) {
                "Unit ${unitOfMeasure.name} does not allow decimal quantities"
            }
        }

        location?.let {
            require(it.isNotBlank()) { "location cannot be blank when provided" }
        }
    }

    val effectiveName: String
        get() = displayName?.trim().takeUnless { it.isNullOrBlank() } ?: productName.trim()

    val subtotalAmount: BigDecimal
        get() = quantity.multiply(unitPrice).money()

    val totalAmount: BigDecimal
        get() = subtotalAmount
            .add(taxAmount.money())
            .subtract(discountAmount.money())
            .money()

    val hasTax: Boolean
        get() = taxAmount > BigDecimal.ZERO

    val hasDiscount: Boolean
        get() = discountAmount > BigDecimal.ZERO
}

enum class InvoiceType {
    SALE,
    PURCHASE,
    INTERNAL,
    PROFORMA,
    CREDIT_NOTE,
    DEBIT_NOTE
}

enum class InvoiceStatus {
    DRAFT,
    ISSUED,
    PAID,
    PARTIALLY_PAID,
    OVERDUE,
    CANCELLED
}


private fun BigDecimal.money(): BigDecimal = this.setScale(2, RoundingMode.HALF_UP)