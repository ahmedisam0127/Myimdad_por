package com.myimdad_por.ui.features.sales.utils

import com.myimdad_por.core.utils.CurrencyFormatter
import com.myimdad_por.core.utils.orZero
import com.myimdad_por.core.utils.roundTo
import com.myimdad_por.domain.model.PaymentMethod
import com.myimdad_por.domain.model.Product
import com.myimdad_por.domain.model.SaleItem
import com.myimdad_por.domain.model.UnitOfMeasure
import com.myimdad_por.ui.features.sales.models.CartUiModel
import com.myimdad_por.ui.features.sales.models.ProductUiModel
import java.math.BigDecimal
import java.math.RoundingMode

private const val MONEY_SCALE = 2

// ---------------------------------------------------------------------------
// BigDecimal Extensions
// ---------------------------------------------------------------------------

fun BigDecimal.asMoney(): BigDecimal =
    setScale(MONEY_SCALE, RoundingMode.HALF_UP)

fun BigDecimal?.asMoneyOrZero(): BigDecimal =
    this.orZero().asMoney()

fun BigDecimal.formatCurrency(): String =
    CurrencyFormatter.formatSDG(asMoney())

fun BigDecimal?.formatCurrencyOrZero(): String =
    asMoneyOrZero().formatCurrency()

fun BigDecimal.formatQuantity(
    unit: UnitOfMeasure
): String {
    val fractionDigits = unit.quantityFractionDigits()

    return CurrencyFormatter.formatPlain(
        amount = roundTo(scale = fractionDigits),
        fractionDigits = fractionDigits
    )
}

// ---------------------------------------------------------------------------
// String Extensions
// ---------------------------------------------------------------------------

fun String?.normalized(): String =
    this?.trim().orEmpty()

fun String?.normalizedLowercase(): String =
    normalized().lowercase()

fun String?.matchesSearch(
    query: String
): Boolean {
    val normalizedQuery = query.normalizedLowercase()

    if (normalizedQuery.isBlank()) return true

    return normalizedLowercase().contains(normalizedQuery)
}

// ---------------------------------------------------------------------------
// Cart Calculations
// ---------------------------------------------------------------------------

fun List<CartUiModel>.calculateSubtotal(): BigDecimal =
    sumOfMoney { it.subtotal }

fun List<CartUiModel>.calculateTaxAmount(): BigDecimal =
    sumOfMoney { it.taxAmount }

fun List<CartUiModel>.calculateDiscountAmount(): BigDecimal =
    sumOfMoney { it.discountAmount }

fun List<CartUiModel>.calculateGrandTotal(): BigDecimal =
    sumOfMoney { it.totalAmount }

fun List<CartUiModel>.totalItemsCount(): Int =
    size

fun List<CartUiModel>.totalQuantity(): BigDecimal =
    fold(BigDecimal.ZERO) { total, item ->
        total + item.quantity
    }.asMoney()

private inline fun List<CartUiModel>.sumOfMoney(
    selector: (CartUiModel) -> BigDecimal
): BigDecimal {
    return fold(BigDecimal.ZERO) { total, item ->
        total + selector(item)
    }.asMoney()
}

// ---------------------------------------------------------------------------
// Cart Search & Helpers
// ---------------------------------------------------------------------------

fun List<CartUiModel>.containsProduct(
    productId: String
): Boolean =
    any { it.productId == productId }

fun List<CartUiModel>.findCartItem(
    productId: String
): CartUiModel? =
    firstOrNull { it.productId == productId }

/**
 * تم تغيير الاسم لتفادي Platform Declaration Clash
 */
fun List<CartUiModel>.filterCartByQuery(
    query: String
): List<CartUiModel> {
    if (query.isBlank()) return this

    return filter { it.matches(query) }
}

// ---------------------------------------------------------------------------
// Product Extensions
// ---------------------------------------------------------------------------

fun List<ProductUiModel>.filterAvailable(): List<ProductUiModel> =
    filter { it.isAvailableForSale && it.isActive }

/**
 * تم تغيير الاسم لتفادي Platform Declaration Clash
 */
fun List<ProductUiModel>.filterProductsByQuery(
    query: String
): List<ProductUiModel> {
    if (query.isBlank()) return this

    return filter { it.matches(query) }
}

fun Product.toUiModel(): ProductUiModel =
    ProductUiModel.fromDomain(this)

// ---------------------------------------------------------------------------
// SaleItem Mapping
// ---------------------------------------------------------------------------

fun SaleItem.toCartUiModel(
    product: ProductUiModel? = null
): CartUiModel {
    return CartUiModel.fromSaleItem(
        saleItem = this,
        product = product
    )
}

// ---------------------------------------------------------------------------
// PaymentMethod Extensions
// ---------------------------------------------------------------------------

fun PaymentMethod.toReadableName(): String =
    displayName

fun PaymentMethod.isCashPayment(): Boolean =
    this == PaymentMethod.CASH

fun PaymentMethod.requiresReferenceNumber(): Boolean =
    requiresReference

// ---------------------------------------------------------------------------
// UnitOfMeasure Extensions
// ---------------------------------------------------------------------------

fun UnitOfMeasure.quantityFractionDigits(): Int =
    if (isDecimalAllowed) 2 else 0

fun UnitOfMeasure.displayQuantity(
    quantity: BigDecimal
): String {
    val fractionDigits = quantityFractionDigits()

    return CurrencyFormatter.formatPlain(
        amount = quantity.roundTo(scale = fractionDigits),
        fractionDigits = fractionDigits
    )
}

// ---------------------------------------------------------------------------
// Cart Quantity Manipulation
// ---------------------------------------------------------------------------

fun CartUiModel.increaseQuantity(
    amount: BigDecimal = BigDecimal.ONE
): CartUiModel {
    val updatedQuantity = (quantity + amount).asMoney()

    return recalculate(
        newQuantity = updatedQuantity
    )
}

fun CartUiModel.decreaseQuantity(
    amount: BigDecimal = BigDecimal.ONE
): CartUiModel {
    val updatedQuantity = quantity
        .subtract(amount)
        .coerceAtLeast(BigDecimal.ONE)
        .asMoney()

    return recalculate(
        newQuantity = updatedQuantity
    )
}

private fun CartUiModel.recalculate(
    newQuantity: BigDecimal
): CartUiModel {

    val updatedSubtotal = newQuantity
        .multiply(unitPrice)
        .asMoney()

    val updatedTotal = updatedSubtotal
        .add(taxAmount)
        .subtract(discountAmount)
        .asMoney()

    return copy(
        quantity = newQuantity,
        subtotal = updatedSubtotal,
        totalAmount = updatedTotal,

        formattedQuantity = newQuantity.formatQuantity(unit),
        formattedSubtotal = updatedSubtotal.formatCurrency(),
        formattedTotalAmount = updatedTotal.formatCurrency()
    )
}