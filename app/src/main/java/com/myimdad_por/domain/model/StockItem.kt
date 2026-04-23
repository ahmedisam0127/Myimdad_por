package com.myimdad_por.domain.model

import java.time.LocalDate
import kotlin.math.abs

/**
 * عنصر مخزون فعلي داخل المستودع أو نقطة البيع.
 * يمثل الكمية الموجودة فعليًا مع الموقع وتاريخ الصلاحية.
 */
data class StockItem(
    val productBarcode: String,
    val productName: String,
    val quantity: Double,
    val location: String,
    val unitOfMeasure: UnitOfMeasure = UnitOfMeasure.UNIT,
    val displayName: String? = null,
    val expiryDate: LocalDate? = null
) {

    init {
        require(isValidBarcode(productBarcode)) { "productBarcode is invalid" }
        require(productName.isNotBlank()) { "productName cannot be blank" }
        require(location.isNotBlank()) { "location cannot be blank" }
        require(quantity.isFinite()) { "quantity must be finite" }
        require(quantity >= 0.0) { "quantity cannot be negative" }

        if (!unitOfMeasure.isDecimalAllowed) {
            require(isWholeNumber(quantity)) {
                "Unit ${unitOfMeasure.name} does not allow decimal quantities"
            }
        }

        expiryDate?.let {
            require(!it.isBefore(LocalDate.of(2000, 1, 1))) {
                "expiryDate is invalid"
            }
        }
    }

    val normalizedBarcode: String
        get() = productBarcode.trim()

    val effectiveName: String
        get() = displayName?.trim().takeUnless { it.isNullOrBlank() } ?: productName.trim()

    val normalizedLocation: String
        get() = location.trim()

    val isOutOfStock: Boolean
        get() = quantity == 0.0

    fun isExpired(today: LocalDate = LocalDate.now()): Boolean {
        return expiryDate?.isBefore(today) == true
    }

    fun expiresWithin(days: Long, today: LocalDate = LocalDate.now()): Boolean {
        require(days >= 0) { "days cannot be negative" }
        val expiry = expiryDate ?: return false
        return !expiry.isBefore(today) && !expiry.isAfter(today.plusDays(days))
    }

    companion object {
        private const val EPSILON = 1e-9

        private fun isWholeNumber(value: Double): Boolean {
            return abs(value - value.toLong().toDouble()) < EPSILON
        }

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