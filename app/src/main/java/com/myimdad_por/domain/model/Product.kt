package com.myimdad_por.domain.model

import java.math.BigDecimal

/**
 * تعريف المنتج الأساسي.
 * يمثل بيانات المنتج مع دعم الوحدات والتحويل بينها.
 */
data class Product(
    val barcode: String,
    val name: String,
    val price: BigDecimal,

    /**
     * الوحدة الأساسية المستخدمة لعرض/بيع المنتج.
     */
    val unitOfMeasure: UnitOfMeasure = UnitOfMeasure.UNIT,

    /**
     * الوحدة الكبرى (مثل كرتونة / صندوق / شوال)
     */
    val largeUnit: UnitOfMeasure = UnitOfMeasure.CARTON,

    /**
     * الوحدة الصغرى (مثل قطعة / كيلو / لتر)
     */
    val smallUnit: UnitOfMeasure = UnitOfMeasure.PIECE,

    /**
     * معامل التحويل بين الكبيرة والصغيرة
     */
    val unitFactor: BigDecimal = BigDecimal.ONE,

    val displayName: String? = null,
    val description: String? = null,
    val isActive: Boolean = true
) {

    init {
        // التحقق الأساسي فقط (بدون كسر التطبيق)
        require(isValidBarcode(barcode)) { "barcode is invalid" }
        require(name.isNotBlank()) { "name cannot be blank" }
        require(price >= BigDecimal.ZERO) { "price cannot be negative" }
        require(price.scale() <= 10) { "price scale is too large" }
        require(unitFactor > BigDecimal.ZERO) { "unitFactor must be greater than zero" }

        /**
         * تم حذف التحقق الصارم الخاص بـ dimensions
         * لأنه كان سبب انهيار التطبيق عند اختلاف البيانات القادمة من DB.
         *
         * الآن يتم السماح بمرونة أكبر في الوحدات.
         */
    }

    val normalizedBarcode: String
        get() = barcode.trim()

    val effectiveName: String
        get() = displayName?.trim().takeUnless { it.isNullOrBlank() } ?: name.trim()

    val isAvailableForSale: Boolean
        get() = isActive

    /**
     * هل المنتج لديه نظام تحويل وحدات مخصص؟
     */
    val hasCustomUnitConversion: Boolean
        get() = unitFactor != BigDecimal.ONE ||
                largeUnit != unitOfMeasure ||
                smallUnit != unitOfMeasure

    /**
     * تعريف التحويل بين الوحدات
     */
    fun getConversion(): UnitConversion {
        return UnitConversion(
            fromUnit = largeUnit,
            toUnit = smallUnit,
            multiplier = unitFactor
        )
    }

    /**
     * هل يستخدم نظام وحدات هرمية (كبير -> صغير)
     */
    fun supportsUnitHierarchy(): Boolean {
        return unitFactor > BigDecimal.ONE
    }

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