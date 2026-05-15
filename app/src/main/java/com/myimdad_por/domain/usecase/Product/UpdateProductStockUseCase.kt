package com.myimdad_por.domain.usecase.Product

import com.myimdad_por.domain.repository.ProductRepository
import com.myimdad_por.domain.repository.ProductStockSnapshot
import java.math.BigDecimal
import javax.inject.Inject
class UpdateProductStockUseCase @Inject constructor(
    private val productRepository: ProductRepository
) {

    suspend operator fun invoke(
        barcode: String?,
        quantityDelta: BigDecimal?,
        reason: String? = null
    ): Result<ProductStockSnapshot> {
        val normalizedBarcode = barcode?.trim().orEmpty()
        if (!isValidBarcode(normalizedBarcode)) {
            return Result.failure(IllegalArgumentException("الباركود غير صالح"))
        }

        val safeQuantityDelta = quantityDelta
            ?: return Result.failure(IllegalArgumentException("قيمة التغيير في المخزون غير موجودة"))

        if (safeQuantityDelta.compareTo(BigDecimal.ZERO) == 0) {
            return Result.failure(IllegalArgumentException("قيمة التغيير في المخزون لا يمكن أن تكون صفراً"))
        }

        if (safeQuantityDelta.scale() > MAX_QUANTITY_SCALE) {
            return Result.failure(IllegalArgumentException("دقة كمية المخزون كبيرة جداً"))
        }

        val normalizedReason = reason?.trim()?.takeIf { it.isNotEmpty() }

        return productRepository.syncProductStock(
            barcode = normalizedBarcode,
            quantityDelta = safeQuantityDelta,
            reason = normalizedReason
        )
    }

    private fun isValidBarcode(value: String): Boolean {
        if (value.isBlank()) return false
        if (value.length !in MIN_BARCODE_LENGTH..MAX_BARCODE_LENGTH) return false

        return value.all { ch ->
            ch.isLetterOrDigit() || ch == '-' || ch == '_' || ch == '.'
        }
    }

    companion object {
        private const val MIN_BARCODE_LENGTH = 4
        private const val MAX_BARCODE_LENGTH = 64
        private const val MAX_QUANTITY_SCALE = 10
    }
}