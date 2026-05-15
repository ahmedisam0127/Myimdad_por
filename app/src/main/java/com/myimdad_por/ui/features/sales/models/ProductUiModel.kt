package com.myimdad_por.ui.features.sales.models

import androidx.compose.runtime.Immutable
import com.myimdad_por.core.utils.CurrencyFormatter
import com.myimdad_por.domain.model.Product
import com.myimdad_por.domain.model.UnitOfMeasure
import java.math.BigDecimal

@Immutable
data class ProductUiModel(
    val barcode: String,
    val name: String,
    val displayName: String,
    val description: String?,
    val price: BigDecimal,
    val formattedPrice: String,
    val unitOfMeasure: UnitOfMeasure,
    val largeUnit: UnitOfMeasure,
    val smallUnit: UnitOfMeasure,
    val unitFactor: BigDecimal,
    val isActive: Boolean,
    val isAvailableForSale: Boolean,
    val supportsUnitHierarchy: Boolean,
    val hasCustomUnitConversion: Boolean,
    val searchKeywords: String
) {

    val id: String
        get() = barcode

    val primaryUnitLabel: String
        get() = unitOfMeasure.displayName

    val primaryUnitSymbol: String
        get() = unitOfMeasure.symbol

    val conversionLabel: String
        get() = buildConversionLabel()

    val statusLabel: String
        get() = if (isActive) "متوفر" else "غير متوفر"

    val shortDescription: String
        get() = description
            ?.trim()
            .orEmpty()

    fun matches(query: String): Boolean {
        if (query.isBlank()) return true

        val normalizedQuery = query.trim().lowercase()

        return searchKeywords.contains(normalizedQuery)
    }

    private fun buildConversionLabel(): String {
        if (!supportsUnitHierarchy) {
            return smallUnit.displayName
        }

        val factor = CurrencyFormatter.formatPlain(
            amount = unitFactor,
            fractionDigits = 2
        )

        return "1 ${largeUnit.displayName} = $factor ${smallUnit.displayName}"
    }

    companion object {

        fun fromDomain(product: Product): ProductUiModel {
            return ProductUiModel(
                barcode = product.normalizedBarcode,
                name = product.name.trim(),
                displayName = product.effectiveName,
                description = product.description?.trim(),
                price = CurrencyFormatter.round(product.price),
                formattedPrice = CurrencyFormatter.formatSDG(product.price),
                unitOfMeasure = product.unitOfMeasure,
                largeUnit = product.largeUnit,
                smallUnit = product.smallUnit,
                unitFactor = product.unitFactor,
                isActive = product.isActive,
                isAvailableForSale = product.isAvailableForSale,
                supportsUnitHierarchy = product.supportsUnitHierarchy(),
                hasCustomUnitConversion = product.hasCustomUnitConversion,
                searchKeywords = buildSearchKeywords(product)
            )
        }

        private fun buildSearchKeywords(
            product: Product
        ): String {
            return buildString {
                append(product.barcode)
                append(" ")
                append(product.name)
                append(" ")
                append(product.effectiveName)
                append(" ")
                append(product.unitOfMeasure.displayName)
                append(" ")
                append(product.largeUnit.displayName)
                append(" ")
                append(product.smallUnit.displayName)

                product.description
                    ?.takeIf { it.isNotBlank() }
                    ?.let {
                        append(" ")
                        append(it)
                    }
            }.trim().lowercase()
        }
    }
}