package com.myimdad_por.domain.model

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDateTime
import java.util.UUID

/**
 * عملية البيع نفسها.
 * تمثل الحدث المالي والمخزني الفعلي.
 */
data class Sale(
    val id: String = UUID.randomUUID().toString(),
    val invoiceNumber: String,
    val customerId: String? = null,
    val employeeId: String,
    val items: List<SaleItem>,
    val paidAmount: BigDecimal = BigDecimal.ZERO,
    val saleStatus: SaleStatus = SaleStatus.COMPLETED,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val note: String? = null
) {

    init {
        require(invoiceNumber.isNotBlank()) { "invoiceNumber cannot be blank" }
        require(employeeId.isNotBlank()) { "employeeId cannot be blank" }
        require(items.isNotEmpty()) { "items cannot be empty" }
        require(paidAmount >= BigDecimal.ZERO) { "paidAmount cannot be negative" }
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

    fun isFullyPaid(): Boolean = paymentStatus == PaymentStatus.PAID

    fun hasReturns(): Boolean = items.any { it.isReturn }

    fun containsDiscounts(): Boolean = items.any { it.hasDiscount }

    fun containsTaxedItems(): Boolean = items.any { it.hasTax }

    fun withAdditionalPayment(amount: BigDecimal): Sale {
        require(amount >= BigDecimal.ZERO) { "amount cannot be negative" }
        return copy(paidAmount = paidAmount.add(amount).money())
    }
}

enum class SaleStatus {
    COMPLETED,
    CANCELLED,
    REFUNDED
}

private fun BigDecimal.money(): BigDecimal = this.setScale(2, RoundingMode.HALF_UP)