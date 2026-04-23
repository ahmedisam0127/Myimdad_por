package com.myimdad_por.domain.model

import java.math.BigDecimal

/**
 * تعريف المنتج الأساسي.
 * هذا الكائن لا يمثل المخزون، بل يمثل بيانات المنتج نفسها.
 */
data class Product(
    val barcode: String,
    val name: String,
    val price: BigDecimal,
    val unitOfMeasure: UnitOfMeasure = UnitOfMeasure.UNIT,
    val displayName: String? = null,
    val description: String? = null,
    val isActive: Boolean = true
) {

    init {
        require(isValidBarcode(barcode)) { "barcode is invalid" }
        require(name.isNotBlank()) { "name cannot be blank" }
        require(price >= BigDecimal.ZERO) { "price cannot be negative" }
        require(price.scale() <= 10) { "price scale is too large" }
    }

    val normalizedBarcode: String
        get() = barcode.trim()

    val effectiveName: String
        get() = displayName?.trim().takeUnless { it.isNullOrBlank() } ?: name.trim()

    val isAvailableForSale: Boolean
        get() = isActive

    companion object {
        private fun isValidBarcode(value: String): Boolean {
            val normalized = value.trim()
            if (normalized.isEmpty()) return false
            if (normalized.length !in 4..64) return false
            return normalized.all { ch ->
                ch.isLetterOrDigit() || ch == '-' || ch == '_' || ch == '.'
            }
        }
    }
}