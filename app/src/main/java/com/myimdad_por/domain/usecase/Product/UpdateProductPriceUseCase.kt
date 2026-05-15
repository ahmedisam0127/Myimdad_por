package com.myimdad_por.domain.usecase.Product

import com.myimdad_por.core.utils.ValidationUtils
import com.myimdad_por.core.utils.isAtLeast
import com.myimdad_por.domain.model.Product
import com.myimdad_por.domain.repository.ProductRepository
import java.math.BigDecimal
import javax.inject.Inject
class UpdateProductPriceUseCase @Inject constructor(
    private val productRepository: ProductRepository
) {

    suspend operator fun invoke(
        barcode: String?,
        newPrice: BigDecimal?
    ): Result<Product> {
        val normalizedBarcode = barcode?.trim().orEmpty()

        if (!isValidBarcode(normalizedBarcode)) {
            return Result.failure(IllegalArgumentException("الباركود غير صالح"))
        }

        if (!ValidationUtils.isValidAmount(newPrice)) {
            return Result.failure(IllegalArgumentException("السعر غير صالح"))
        }

        val safePrice = newPrice ?: BigDecimal.ZERO

        if (!safePrice.isAtLeast(BigDecimal.ZERO)) {
            return Result.failure(IllegalArgumentException("السعر لا يمكن أن يكون سالباً"))
        }

        return productRepository.updateProductPrice(
            barcode = normalizedBarcode,
            price = safePrice
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
    }
}