package com.myimdad_por.domain.model

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDateTime
import java.util.UUID

/**
 * عملية الاسترجاع العامة.
 * تدعم:
 * - استرجاع مبيعات من العميل
 * - استرجاع مشتريات إلى المورد
 */
data class Return(
    val id: String = UUID.randomUUID().toString(),
    val returnNumber: String,
    val returnType: ReturnType,
    val originalDocumentId: String? = null,
    val originalDocumentNumber: String? = null,
    val partyId: String? = null,
    val partyName: String? = null,
    val processedByEmployeeId: String,
    val items: List<ReturnItem>,
    val returnDate: LocalDateTime = LocalDateTime.now(),
    val status: ReturnStatus = ReturnStatus.DRAFT,
    val refundedAmount: BigDecimal = BigDecimal.ZERO,
    val reason: String? = null,
    val note: String? = null
) {

    init {
        require(returnNumber.isNotBlank()) { "returnNumber cannot be blank" }
        require(processedByEmployeeId.isNotBlank()) { "processedByEmployeeId cannot be blank" }
        require(items.isNotEmpty()) { "items cannot be empty" }
        require(refundedAmount >= BigDecimal.ZERO) { "refundedAmount cannot be negative" }
    }

    val subtotalAmount: BigDecimal
        get() = items.fold(BigDecimal.ZERO) { acc: BigDecimal, item: ReturnItem ->
            acc.add(item.calculateSubtotal())
        }.money()

    val taxAmount: BigDecimal
        get() = items.fold(BigDecimal.ZERO) { acc: BigDecimal, item: ReturnItem ->
            acc.add(item.taxAmount)
        }.money()

    val discountAmount: BigDecimal
        get() = items.fold(BigDecimal.ZERO) { acc: BigDecimal, item: ReturnItem ->
            acc.add(item.discountAmount)
        }.money()

    /**
     * مبلغ الاسترجاع المستحق قبل أي مبالغ صُرفت فعلاً.
     */
    val totalRefundAmount: BigDecimal
        get() = subtotalAmount
            .add(taxAmount)
            .subtract(discountAmount)
            .money()

    val remainingRefundAmount: BigDecimal
        get() {
            val remaining = totalRefundAmount.subtract(refundedAmount)
            return if (remaining > BigDecimal.ZERO) remaining.money() else BigDecimal.ZERO.setScale(2)
        }

    val refundStatus: RefundStatus
        get() = when {
            totalRefundAmount <= BigDecimal.ZERO -> RefundStatus.REFUNDED
            remainingRefundAmount == BigDecimal.ZERO && refundedAmount > BigDecimal.ZERO -> RefundStatus.REFUNDED
            refundedAmount > BigDecimal.ZERO -> RefundStatus.PARTIALLY_REFUNDED
            else -> RefundStatus.PENDING
        }

    fun isFullyRefunded(): Boolean = refundStatus == RefundStatus.REFUNDED

    fun hasRestockableItems(): Boolean = items.any { it.isRestockable }

    fun affectsInventory(): Boolean = items.any { it.isRestockable }

    fun approve(): Return = copy(status = ReturnStatus.APPROVED)

    fun complete(): Return = copy(status = ReturnStatus.COMPLETED)

    fun reject(): Return = copy(status = ReturnStatus.REJECTED)

    fun cancel(): Return = copy(status = ReturnStatus.CANCELLED)

    fun withRefund(amount: BigDecimal): Return {
        require(amount >= BigDecimal.ZERO) { "amount cannot be negative" }
        return copy(refundedAmount = refundedAmount.add(amount).money())
    }
}

/**
 * سطر استرجاع واحد.
 */
data class ReturnItem(
    val id: String = UUID.randomUUID().toString(),
    val returnId: String? = null,
    val originalItemId: String? = null,
    val productId: String,
    val productBarcode: String? = null,
    val productName: String,
    val unit: UnitOfMeasure = UnitOfMeasure.UNIT,
    val quantity: BigDecimal,
    val unitPriceAtSource: BigDecimal,
    val taxAmount: BigDecimal = BigDecimal.ZERO,
    val discountAmount: BigDecimal = BigDecimal.ZERO,
    val isRestockable: Boolean = true,
    val note: String? = null
) {

    init {
        require(productId.isNotBlank()) { "productId cannot be blank" }
        require(productName.isNotBlank()) { "productName cannot be blank" }
        require(quantity > BigDecimal.ZERO) { "quantity must be greater than zero" }
        require(unitPriceAtSource >= BigDecimal.ZERO) { "unitPriceAtSource cannot be negative" }
        require(taxAmount >= BigDecimal.ZERO) { "taxAmount cannot be negative" }
        require(discountAmount >= BigDecimal.ZERO) { "discountAmount cannot be negative" }

        if (!unit.isDecimalAllowed) {
            require(quantity.stripTrailingZeros().scale() <= 0) {
                "Unit ${unit.name} does not allow decimal quantities"
            }
        }
    }

    fun calculateSubtotal(): BigDecimal {
        return quantity.multiply(unitPriceAtSource).money()
    }

    val lineTotal: BigDecimal
        get() = calculateSubtotal()
            .add(taxAmount.money())
            .subtract(discountAmount.money())
            .money()

    val hasTax: Boolean
        get() = taxAmount > BigDecimal.ZERO

    val hasDiscount: Boolean
        get() = discountAmount > BigDecimal.ZERO
}

enum class ReturnType {
    SALE_RETURN,
    PURCHASE_RETURN
}

enum class ReturnStatus {
    DRAFT,
    APPROVED,
    COMPLETED,
    REJECTED,
    CANCELLED
}

enum class RefundStatus {
    REFUNDED,
    PARTIALLY_REFUNDED,
    PENDING
}

private fun BigDecimal.money(): BigDecimal = this.setScale(2, RoundingMode.HALF_UP)