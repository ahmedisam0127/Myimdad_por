package com.myimdad_por.domain.usecase.Product

import com.myimdad_por.domain.model.Product
import com.myimdad_por.domain.repository.ProductRepository
import javax.inject.Inject
class GetProductUseCase @Inject constructor(
    private val productRepository: ProductRepository
) {

    suspend operator fun invoke(barcode: String?): Product? {
        val normalizedBarcode = barcode?.trim().orEmpty()
        if (!isValidBarcode(normalizedBarcode)) return null

        return productRepository.getProductByBarcode(normalizedBarcode)
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