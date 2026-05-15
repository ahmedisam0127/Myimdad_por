package com.myimdad_por.domain.usecase.Product

import com.myimdad_por.domain.model.Product
import com.myimdad_por.domain.repository.ProductRepository
import javax.inject.Inject
class SyncProductsUseCase @Inject constructor(
    private val productRepository: ProductRepository,
    private val validateProductUseCase: ValidateProductUseCase = ValidateProductUseCase()
) {

    suspend operator fun invoke(products: List<Product>?): Result<List<Product>> {
        val safeProducts = products ?: return Result.failure(
            IllegalArgumentException("قائمة المنتجات غير موجودة")
        )

        if (safeProducts.isEmpty()) {
            return Result.success(emptyList())
        }

        val invalidMessages = safeProducts.mapNotNull { product ->
            val validation = validateProductUseCase(product)
            if (validation.isValid) null else {
                val barcode = product.barcode.trim().ifBlank { "UNKNOWN" }
                "$barcode: ${validation.errors.joinToString(separator = "، ")}"
            }
        }

        if (invalidMessages.isNotEmpty()) {
            return Result.failure(
                IllegalArgumentException(invalidMessages.joinToString(separator = "\n"))
            )
        }

        return productRepository.saveProducts(safeProducts)
    }
}