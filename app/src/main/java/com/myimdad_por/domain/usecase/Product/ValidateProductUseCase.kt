package com.myimdad_por.domain.usecase.Product

import com.myimdad_por.core.utils.ValidationUtils
import com.myimdad_por.domain.model.Product
import java.math.BigDecimal
import javax.inject.Inject
class ValidateProductUseCase @Inject constructor( ){

    operator fun invoke(product: Product?): ValidationResult {
        val errors = mutableListOf<String>()

        if (product == null) {
            errors += "المنتج غير موجود"
            return ValidationResult(
                isValid = false,
                errors = errors
            )
        }

        val barcode = product.barcode.trim()
        val name = product.name.trim()
        val displayName = product.displayName?.trim().orEmpty()
        val description = product.description?.trim().orEmpty()
        val price = product.price

        if (!isValidBarcode(barcode)) {
            errors += "الباركود غير صالح"
        }

        if (name.isBlank()) {
            errors += "اسم المنتج لا يمكن أن يكون فارغاً"
        } else if (!ValidationUtils.isValidName(name)) {
            errors += "اسم المنتج غير صالح"
        }

        if (!ValidationUtils.isValidAmount(price)) {
            errors += "سعر المنتج غير صالح"
        } else if (price.scale() > MAX_PRICE_SCALE) {
            errors += "دقة السعر كبيرة جداً"
        }

        if (product.unitFactor <= BigDecimal.ZERO) {
            errors += "معامل التحويل يجب أن يكون أكبر من صفر"
        }

        if (product.largeUnit == product.smallUnit) {
            errors += "الوحدة الكبيرة والوحدة الصغيرة يجب أن تكونا مختلفتين"
        }

        if (product.largeUnit.dimension != product.smallUnit.dimension) {
            errors += "الوحدتان الكبيره والصغيره يجب أن تكونا من نفس النوع"
        }

        if (product.unitOfMeasure.dimension != product.smallUnit.dimension) {
            errors += "وحدة القياس الأساسية يجب أن تكون من نفس نوع الوحدات"
        }

        if (displayName.length > MAX_TEXT_LENGTH) {
            errors += "اسم العرض طويل جداً"
        }

        if (description.length > MAX_TEXT_LENGTH) {
            errors += "الوصف طويل جداً"
        }

        return ValidationResult(
            isValid = errors.isEmpty(),
            errors = errors
        )
    }

    private fun isValidBarcode(value: String): Boolean {
        if (value.isBlank()) return false
        if (value.length !in MIN_BARCODE_LENGTH..MAX_BARCODE_LENGTH) return false

        return value.all { ch ->
            ch.isLetterOrDigit() || ch == '-' || ch == '_' || ch == '.'
        }
    }

    data class ValidationResult(
        val isValid: Boolean,
        val errors: List<String> = emptyList()
    )

    companion object {
        private const val MIN_BARCODE_LENGTH = 4
        private const val MAX_BARCODE_LENGTH = 64
        private const val MAX_TEXT_LENGTH = 255
        private const val MAX_PRICE_SCALE = 10
    }
}