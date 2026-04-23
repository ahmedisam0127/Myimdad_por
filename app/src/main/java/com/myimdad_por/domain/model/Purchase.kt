package com.myimdad_por.domain.model

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDateTime
import java.util.UUID

/**
 * عملية الشراء نفسها.
 * تمثل إدخال بضاعة إلى المخزون مع التزام مالي تجاه المورد.
 */
data class Purchase(
    val id: String = UUID.randomUUID().toString(),
    val invoiceNumber: String,
    val supplierId: String,
    val supplierName: String,
    val employeeId: String,
    val items: List<PurchaseItem>,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val dueDate: LocalDateTime? = null,
    val paidAmount: BigDecimal = BigDecimal.ZERO,
    val status: PurchaseStatus = PurchaseStatus.DRAFT,
    val note: String? = null
) {

    init {
        require(invoiceNumber.isNotBlank()) { "invoiceNumber cannot be blank" }
        require(supplierId.isNotBlank()) { "supplierId cannot be blank" }
        require(supplierName.isNotBlank()) { "supplierName cannot be blank" }
        require(employeeId.isNotBlank()) { "employeeId cannot be blank" }
        require(items.isNotEmpty()) { "items cannot be empty" }
        require(paidAmount >= BigDecimal.ZERO) { "paidAmount cannot be negative" }
        dueDate?.let {
            require(!it.isBefore(createdAt)) { "dueDate cannot be before createdAt" }
        }
    }

    val subtotalAmount: BigDecimal
        get() = items.fold(BigDecimal.ZERO) { acc: BigDecimal, item: PurchaseItem ->
            acc.add(item.calculateSubtotal())
        }.money()

    val taxAmount: BigDecimal
        get() = items.fold(BigDecimal.ZERO) { acc: BigDecimal, item: PurchaseItem ->
            acc.add(item.taxAmount)
        }.money()

    val discountAmount: BigDecimal
        get() = items.fold(BigDecimal.ZERO) { acc: BigDecimal, item: PurchaseItem ->
            acc.add(item.discountAmount)
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

    val paymentStatus: PurchasePaymentStatus
        get() = when {
            totalAmount <= BigDecimal.ZERO -> PurchasePaymentStatus.PAID
            remainingAmount == BigDecimal.ZERO && paidAmount > BigDecimal.ZERO -> PurchasePaymentStatus.PAID
            paidAmount > BigDecimal.ZERO -> PurchasePaymentStatus.PARTIALLY_PAID
            else -> PurchasePaymentStatus.PENDING
        }

    fun isFullyPaid(): Boolean = paymentStatus == PurchasePaymentStatus.PAID

    fun hasDiscounts(): Boolean = items.any { it.hasDiscount }

    fun hasTaxedItems(): Boolean = items.any { it.hasTax }

    fun withAdditionalPayment(amount: BigDecimal): Purchase {
        require(amount >= BigDecimal.ZERO) { "amount cannot be negative" }
        return copy(paidAmount = paidAmount.add(amount).money())
    }

    fun markConfirmed(): Purchase = copy(status = PurchaseStatus.CONFIRMED)

    fun markReceived(): Purchase = copy(status = PurchaseStatus.RECEIVED)

    fun markClosed(): Purchase = copy(status = PurchaseStatus.CLOSED)

    fun cancel(): Purchase = copy(status = PurchaseStatus.CANCELLED)
}

/**
 * سطر شراء واحد.
 * يمثل منتجًا تم شراؤه من المورد بسعر الشراء وقت العملية.
 */
data class PurchaseItem(
    val id: String = UUID.randomUUID().toString(),
    val purchaseId: String? = null,
    val productId: String,
    val productBarcode: String? = null,
    val productName: String,
    val unit: UnitOfMeasure = UnitOfMeasure.UNIT,
    val quantity: BigDecimal,
    val unitCost: BigDecimal,
    val taxAmount: BigDecimal = BigDecimal.ZERO,
    val discountAmount: BigDecimal = BigDecimal.ZERO,
    val note: String? = null
) {

    init {
        require(productId.isNotBlank()) { "productId cannot be blank" }
        require(productName.isNotBlank()) { "productName cannot be blank" }
        require(quantity > BigDecimal.ZERO) { "quantity must be greater than zero" }
        require(unitCost >= BigDecimal.ZERO) { "unitCost cannot be negative" }
        require(taxAmount >= BigDecimal.ZERO) { "taxAmount cannot be negative" }
        require(discountAmount >= BigDecimal.ZERO) { "discountAmount cannot be negative" }

        if (!unit.isDecimalAllowed) {
            require(quantity.stripTrailingZeros().scale() <= 0) {
                "Unit ${unit.name} does not allow decimal quantities"
            }
        }
    }

    fun calculateSubtotal(): BigDecimal {
        return quantity.multiply(unitCost).money()
    }

    val totalCost: BigDecimal
        get() = calculateSubtotal()
            .add(taxAmount.money())
            .subtract(discountAmount.money())
            .money()

    val hasTax: Boolean
        get() = taxAmount > BigDecimal.ZERO

    val hasDiscount: Boolean
        get() = discountAmount > BigDecimal.ZERO
}

enum class PurchaseStatus {
    DRAFT,
    CONFIRMED,
    RECEIVED,
    CLOSED,
    CANCELLED
}

enum class PurchasePaymentStatus {
    PAID,
    PARTIALLY_PAID,
    PENDING
}

private fun BigDecimal.money(): BigDecimal = this.setScale(2, RoundingMode.HALF_UP)