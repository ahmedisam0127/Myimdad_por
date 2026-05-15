package com.myimdad_por.ui.features.sales.models

import androidx.compose.runtime.Immutable
import com.myimdad_por.core.utils.CurrencyFormatter
import com.myimdad_por.core.utils.orZero
import com.myimdad_por.core.utils.roundTo
import com.myimdad_por.domain.model.SaleItem
import com.myimdad_por.domain.model.UnitOfMeasure
import java.math.BigDecimal

@Immutable
data class CartUiModel(
    val id: String,
    val productId: String,
    val productName: String,
    val productDisplayName: String,
    val barcode: String,
    val unit: UnitOfMeasure,
    val quantity: BigDecimal,
    val unitPrice: BigDecimal,
    val taxAmount: BigDecimal,
    val discountAmount: BigDecimal,
    val subtotal: BigDecimal,
    val totalAmount: BigDecimal,
    val isReturn: Boolean,
    val note: String?,
    val formattedQuantity: String,
    val formattedUnitPrice: String,
    val formattedTaxAmount: String,
    val formattedDiscountAmount: String,
    val formattedSubtotal: String,
    val formattedTotalAmount: String
) {

    val itemCountLabel: String
        get() = "$formattedQuantity ${unit.displayName}"

    val unitLabel: String
        get() = unit.displayName

    val unitSymbol: String
        get() = unit.symbol

    val hasDiscount: Boolean
        get() = discountAmount > BigDecimal.ZERO

    val hasTax: Boolean
        get() = taxAmount > BigDecimal.ZERO

    val isFreeItem: Boolean
        get() = unitPrice.compareTo(BigDecimal.ZERO) == 0

    val noteLabel: String
        get() = note
            ?.trim()
            .orEmpty()

    val itemTypeLabel: String
        get() = if (isReturn) "مرتجع" else "بيع"

    fun matches(query: String): Boolean {
        if (query.isBlank()) return true

        val normalizedQuery = query.trim().lowercase()

        return buildSearchableContent()
            .contains(normalizedQuery)
    }

    private fun buildSearchableContent(): String {
        return buildString {
            append(productId)
            append(" ")
            append(productName)
            append(" ")
            append(productDisplayName)
            append(" ")
            append(barcode)
            append(" ")
            append(unit.displayName)
            append(" ")
            append(unit.symbol)

            note
                ?.takeIf { it.isNotBlank() }
                ?.let {
                    append(" ")
                    append(it)
                }
        }.trim().lowercase()
    }

    companion object {

        fun fromSaleItem(
            saleItem: SaleItem,
            product: ProductUiModel? = null
        ): CartUiModel {

            val subtotal = saleItem.calculateSubtotal()
                .roundTo()

            val total = saleItem.totalPrice
                .roundTo()

            val quantity = saleItem.quantity
                .roundTo(
                    scale = if (saleItem.unit.isDecimalAllowed) 2 else 0
                )

            return CartUiModel(
                id = saleItem.id,
                productId = saleItem.productId,
                productName = saleItem.productName,
                productDisplayName = product?.displayName
                    ?: saleItem.productName,
                barcode = product?.barcode.orEmpty(),
                unit = saleItem.unit,
                quantity = quantity,
                unitPrice = saleItem.unitPrice.roundTo(),
                taxAmount = saleItem.taxAmount.roundTo(),
                discountAmount = saleItem.discountAmount.roundTo(),
                subtotal = subtotal,
                totalAmount = total,
                isReturn = saleItem.isReturn,
                note = saleItem.note?.trim(),
                formattedQuantity = CurrencyFormatter.formatPlain(
                    amount = quantity,
                    fractionDigits = if (saleItem.unit.isDecimalAllowed) 2 else 0
                ),
                formattedUnitPrice = CurrencyFormatter.formatSDG(
                    amount = saleItem.unitPrice
                ),
                formattedTaxAmount = CurrencyFormatter.formatSDG(
                    amount = saleItem.taxAmount
                ),
                formattedDiscountAmount = CurrencyFormatter.formatSDG(
                    amount = saleItem.discountAmount
                ),
                formattedSubtotal = CurrencyFormatter.formatSDG(
                    amount = subtotal
                ),
                formattedTotalAmount = CurrencyFormatter.formatSDG(
                    amount = total
                )
            )
        }

        fun empty(): CartUiModel {
            return CartUiModel(
                id = "",
                productId = "",
                productName = "",
                productDisplayName = "",
                barcode = "",
                unit = UnitOfMeasure.DEFAULT,
                quantity = BigDecimal.ZERO,
                unitPrice = BigDecimal.ZERO,
                taxAmount = BigDecimal.ZERO,
                discountAmount = BigDecimal.ZERO,
                subtotal = BigDecimal.ZERO,
                totalAmount = BigDecimal.ZERO,
                isReturn = false,
                note = null,
                formattedQuantity = "0",
                formattedUnitPrice = CurrencyFormatter.formatSDG(
                    BigDecimal.ZERO
                ),
                formattedTaxAmount = CurrencyFormatter.formatSDG(
                    BigDecimal.ZERO
                ),
                formattedDiscountAmount = CurrencyFormatter.formatSDG(
                    BigDecimal.ZERO
                ),
                formattedSubtotal = CurrencyFormatter.formatSDG(
                    BigDecimal.ZERO
                ),
                formattedTotalAmount = CurrencyFormatter.formatSDG(
                    BigDecimal.ZERO
                )
            )
        }
    }
}