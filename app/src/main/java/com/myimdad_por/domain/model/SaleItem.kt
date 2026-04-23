package com.myimdad_por.domain.model

import java.math.BigDecimal
import java.math.RoundingMode
import java.util.UUID

/**
 * سطر مبيعات واحد داخل الفاتورة.
 * يمثل منتجًا واحدًا مع سعره وقت البيع والكمية والضريبة والخصم.
 */
data class SaleItem(
    val id: String = UUID.randomUUID().toString(),
    val saleId: String? = null,
    val productId: String,
    val productName: String,
    val unit: UnitOfMeasure = UnitOfMeasure.UNIT,
    val quantity: BigDecimal,
    val unitPrice: BigDecimal,
    val taxAmount: BigDecimal = BigDecimal.ZERO,
    val discountAmount: BigDecimal = BigDecimal.ZERO,
    val isReturn: Boolean = false,
    val note: String? = null
) {

    init {
        require(productId.isNotBlank()) { "productId cannot be blank" }
        require(productName.isNotBlank()) { "productName cannot be blank" }
        require(quantity > BigDecimal.ZERO) { "quantity must be greater than zero" }
        require(unitPrice >= BigDecimal.ZERO) { "unitPrice cannot be negative" }
        require(taxAmount >= BigDecimal.ZERO) { "taxAmount cannot be negative" }
        require(discountAmount >= BigDecimal.ZERO) { "discountAmount cannot be negative" }

        if (!unit.isDecimalAllowed) {
            require(quantity.stripTrailingZeros().scale() <= 0) {
                "Unit ${unit.name} does not allow decimal quantities"
            }
        }
    }

    val signedQuantity: BigDecimal
        get() = if (isReturn) quantity.negate() else quantity

    fun calculateSubtotal(): BigDecimal {
        val subtotal = quantity.multiply(unitPrice)
        return if (isReturn) subtotal.negate().money() else subtotal.money()
    }

    val netTaxAmount: BigDecimal
        get() = if (isReturn) taxAmount.negate().money() else taxAmount.money()

    val netDiscountAmount: BigDecimal
        get() = if (isReturn) discountAmount.negate().money() else discountAmount.money()

    val totalPrice: BigDecimal
        get() = calculateSubtotal()
            .add(netTaxAmount)
            .subtract(netDiscountAmount)
            .money()

    val hasTax: Boolean
        get() = taxAmount > BigDecimal.ZERO

    val hasDiscount: Boolean
        get() = discountAmount > BigDecimal.ZERO
}

private fun BigDecimal.money(): BigDecimal = this.setScale(2, RoundingMode.HALF_UP)