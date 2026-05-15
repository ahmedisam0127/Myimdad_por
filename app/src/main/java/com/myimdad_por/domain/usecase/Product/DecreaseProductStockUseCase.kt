package com.myimdad_por.domain.usecase.Product

import com.myimdad_por.domain.repository.ProductRepository
import com.myimdad_por.domain.repository.ProductStockSnapshot
import java.math.BigDecimal
import javax.inject.Inject
class DecreaseProductStockUseCase @Inject constructor(
    private val productRepository: ProductRepository
) {

    suspend operator fun invoke(
        barcode: String?,
        amount: BigDecimal?,
        reason: String? = null
    ): Result<ProductStockSnapshot> {
        val normalizedBarcode = barcode?.trim().orEmpty()
        if (!isValidBarcode(normalizedBarcode)) {
            return Result.failure(IllegalArgumentException("الباركود غير صالح"))
        }

        val safeAmount = amount
            ?: return Result.failure(IllegalArgumentException("قيمة النقصان غير موجودة"))

        if (safeAmount <= BigDecimal.ZERO) {
            return Result.failure(IllegalArgumentException("قيمة النقصان يجب أن تكون أكبر من صفر"))
        }

        if (safeAmount.scale() > MAX_AMOUNT_SCALE) {
            return Result.failure(IllegalArgumentException("دقة الكمية كبيرة جداً"))
        }

        val normalizedReason = reason?.trim()?.takeIf { it.isNotEmpty() }

        return productRepository.syncProductStock(
            barcode = normalizedBarcode,
            quantityDelta = safeAmount.negate(),
            reason = normalizedReason ?: DEFAULT_REASON
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
        private const val MAX_AMOUNT_SCALE = 10
        private const val DEFAULT_REASON = "إنقاص مخزون"
    }
}