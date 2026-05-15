package com.myimdad_por.ui.features.sales.models

import androidx.compose.runtime.Immutable
import com.myimdad_por.core.utils.CurrencyFormatter
import com.myimdad_por.core.utils.DateTimeUtils
import com.myimdad_por.core.utils.roundTo
import com.myimdad_por.domain.model.Customer
import com.myimdad_por.domain.model.PaymentMethod
import com.myimdad_por.domain.model.PaymentStatus
import com.myimdad_por.domain.model.SaleInvoice
import com.myimdad_por.domain.model.SaleInvoiceStatus
import java.math.BigDecimal
import java.time.LocalDateTime

@Immutable
data class BillUiModel(
    val id: String,
    val invoiceNumber: String,
    val saleId: String,
    val status: SaleInvoiceStatus,
    val paymentStatus: PaymentStatus,
    val issueDate: LocalDateTime,
    val dueDate: LocalDateTime?,
    val employeeId: String,
    val customerName: String?,
    val customerPhone: String?,
    val customerAddress: String?,
    val customerTaxNumber: String?,
    val items: List<CartUiModel>,
    val paymentMethod: PaymentMethod?,
    val subtotalAmount: BigDecimal,
    val taxAmount: BigDecimal,
    val discountAmount: BigDecimal,
    val totalAmount: BigDecimal,
    val paidAmount: BigDecimal,
    val remainingAmount: BigDecimal,
    val notes: String?,
    val termsAndConditions: String?,
    val qrPayload: String?,
    val formattedSubtotalAmount: String,
    val formattedTaxAmount: String,
    val formattedDiscountAmount: String,
    val formattedTotalAmount: String,
    val formattedPaidAmount: String,
    val formattedRemainingAmount: String,
    val formattedIssueDate: String,
    val formattedDueDate: String?,
    val statusLabel: String,
    val paymentStatusLabel: String
) {

    val itemCount: Int
        get() = items.size

    val totalQuantity: BigDecimal
        get() = items.fold(BigDecimal.ZERO) { acc, item ->
            acc.add(item.quantity)
        }.roundTo()

    val formattedTotalQuantity: String
        get() = CurrencyFormatter.formatPlain(totalQuantity)

    val isPaid: Boolean
        get() = paymentStatus == PaymentStatus.PAID

    val isPending: Boolean
        get() = paymentStatus == PaymentStatus.PENDING

    val isPartiallyPaid: Boolean
        get() = paymentStatus == PaymentStatus.PARTIALLY_PAID

    val hasCustomer: Boolean
        get() = !customerName.isNullOrBlank()

    val hasNotes: Boolean
        get() = !notes.isNullOrBlank()

    val hasDiscount: Boolean
        get() = discountAmount > BigDecimal.ZERO

    val hasTax: Boolean
        get() = taxAmount > BigDecimal.ZERO

    val canBeEdited: Boolean
        get() = status.isEditable

    val isOverdue: Boolean
        get() = dueDate != null &&
            remainingAmount > BigDecimal.ZERO &&
            LocalDateTime.now().isAfter(dueDate)

    val dueStatusLabel: String
        get() = when {
            isPaid -> "مدفوعة"
            isOverdue -> "متأخرة"
            dueDate != null -> "مستحقة"
            else -> "فورية"
        }

    fun matches(query: String): Boolean {
        if (query.isBlank()) return true

        val normalizedQuery = query
            .trim()
            .lowercase()

        return searchableContent.contains(normalizedQuery)
    }

    private val searchableContent: String by lazy {
        buildString {
            append(invoiceNumber)
            append(" ")
            append(saleId)
            append(" ")
            append(customerName.orEmpty())
            append(" ")
            append(customerPhone.orEmpty())
            append(" ")
            append(customerAddress.orEmpty())
            append(" ")
            append(customerTaxNumber.orEmpty())
            append(" ")
            append(statusLabel)
            append(" ")
            append(paymentStatusLabel)

            notes
                ?.takeIf { it.isNotBlank() }
                ?.let {
                    append(" ")
                    append(it)
                }

            items.forEach { item ->
                append(" ")
                append(item.productName)
                append(" ")
                append(item.barcode)
            }
        }
            .trim()
            .lowercase()
    }

    companion object {

        fun fromInvoice(
            invoice: SaleInvoice,
            customer: Customer? = null,
            paymentMethod: PaymentMethod? = null,
            products: List<ProductUiModel> = emptyList()
        ): BillUiModel {

            val productMap = products.associateBy { it.barcode }

            val cartItems = invoice.items.map { saleItem ->
                CartUiModel.fromSaleItem(
                    saleItem = saleItem,
                    product = productMap[saleItem.productId]
                )
            }

            val subtotal = invoice.subtotalAmount.roundTo()
            val tax = invoice.taxAmount.roundTo()
            val discount = invoice.discountAmount.roundTo()
            val total = invoice.totalAmount.roundTo()
            val paid = invoice.paidAmount.roundTo()
            val remaining = invoice.remainingAmount.roundTo()

            return BillUiModel(
                id = invoice.id,
                invoiceNumber = invoice.invoiceNumber,
                saleId = invoice.saleId,
                status = invoice.status,
                paymentStatus = invoice.paymentStatus,
                issueDate = invoice.issueDate,
                dueDate = invoice.dueDate,
                employeeId = invoice.employeeId,
                customerName = customer?.displayName
                    ?: invoice.customerSnapshot?.name,
                customerPhone = customer?.phoneNumber
                    ?: invoice.customerSnapshot?.phone,
                customerAddress = customer?.address
                    ?: invoice.customerSnapshot?.address,
                customerTaxNumber = customer?.taxNumber
                    ?: invoice.customerSnapshot?.taxNumber,
                items = cartItems,
                paymentMethod = paymentMethod,
                subtotalAmount = subtotal,
                taxAmount = tax,
                discountAmount = discount,
                totalAmount = total,
                paidAmount = paid,
                remainingAmount = remaining,
                notes = invoice.notes?.trim(),
                termsAndConditions = invoice.termsAndConditions?.trim(),
                qrPayload = invoice.qrPayload,
                formattedSubtotalAmount = CurrencyFormatter.formatSDG(subtotal),
                formattedTaxAmount = CurrencyFormatter.formatSDG(tax),
                formattedDiscountAmount = CurrencyFormatter.formatSDG(discount),
                formattedTotalAmount = CurrencyFormatter.formatSDG(total),
                formattedPaidAmount = CurrencyFormatter.formatSDG(paid),
                formattedRemainingAmount = CurrencyFormatter.formatSDG(remaining),
                formattedIssueDate = DateTimeUtils.formatForDisplay(
                    invoice.issueDate
                ),
                formattedDueDate = invoice.dueDate?.let {
                    DateTimeUtils.formatForDisplay(it)
                },
                statusLabel = mapInvoiceStatus(invoice.status),
                paymentStatusLabel = mapPaymentStatus(invoice.paymentStatus)
            )
        }

        private fun mapInvoiceStatus(
            status: SaleInvoiceStatus
        ): String {
            return when (status) {
                SaleInvoiceStatus.DRAFT -> "مسودة"
                SaleInvoiceStatus.OPEN -> "مفتوحة"
                SaleInvoiceStatus.ISSUED -> "صادرة"
                SaleInvoiceStatus.PAID -> "مدفوعة"
                SaleInvoiceStatus.PARTIALLY_PAID -> "مدفوعة جزئياً"
                SaleInvoiceStatus.OVERDUE -> "متأخرة"
                SaleInvoiceStatus.CANCELLED -> "ملغية"
                SaleInvoiceStatus.VOID -> "باطلة"
                SaleInvoiceStatus.EXPIRED -> "منتهية" 
            }
        }

        private fun mapPaymentStatus(
            status: PaymentStatus
        ): String {
            return when (status) {

                PaymentStatus.PENDING ->
                    "معلق"

                PaymentStatus.PROCESSING ->
                    "قيد المعالجة"

                PaymentStatus.REQUIRES_ACTION ->
                    "يتطلب إجراء"

                PaymentStatus.AUTHORIZED ->
                    "مصرح"

                PaymentStatus.PAID ->
                    "مدفوع"

                PaymentStatus.PARTIALLY_PAID ->
                    "مدفوع جزئياً"

                PaymentStatus.FAILED ->
                    "فشلت العملية"

                PaymentStatus.CANCELED ->
                    "ملغي"

                PaymentStatus.REFUNDED ->
                    "تم الاسترجاع"

                PaymentStatus.PARTIALLY_REFUNDED ->
                    "استرجاع جزئي"

                else ->
                    "غير معروف"
            }
        }

        fun empty(): BillUiModel {
            return BillUiModel(
                id = "",
                invoiceNumber = "",
                saleId = "",
                status = SaleInvoiceStatus.DRAFT,
                paymentStatus = PaymentStatus.PENDING,
                issueDate = DateTimeUtils.now(),
                dueDate = null,
                employeeId = "",
                customerName = null,
                customerPhone = null,
                customerAddress = null,
                customerTaxNumber = null,
                items = emptyList(),
                paymentMethod = null,
                subtotalAmount = BigDecimal.ZERO,
                taxAmount = BigDecimal.ZERO,
                discountAmount = BigDecimal.ZERO,
                totalAmount = BigDecimal.ZERO,
                paidAmount = BigDecimal.ZERO,
                remainingAmount = BigDecimal.ZERO,
                notes = null,
                termsAndConditions = null,
                qrPayload = null,
                formattedSubtotalAmount = CurrencyFormatter.formatSDG(BigDecimal.ZERO),
                formattedTaxAmount = CurrencyFormatter.formatSDG(BigDecimal.ZERO),
                formattedDiscountAmount = CurrencyFormatter.formatSDG(BigDecimal.ZERO),
                formattedTotalAmount = CurrencyFormatter.formatSDG(BigDecimal.ZERO),
                formattedPaidAmount = CurrencyFormatter.formatSDG(BigDecimal.ZERO),
                formattedRemainingAmount = CurrencyFormatter.formatSDG(BigDecimal.ZERO),
                formattedIssueDate = "",
                formattedDueDate = null,
                statusLabel = "مسودة",
                paymentStatusLabel = "معلق"
            )
        }
    }
}