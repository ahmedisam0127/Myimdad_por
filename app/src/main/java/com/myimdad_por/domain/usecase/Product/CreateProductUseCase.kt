package com.myimdad_por.domain.usecase.Product

import com.myimdad_por.domain.model.Product
import com.myimdad_por.domain.repository.ProductRepository
import javax.inject.Inject
class CreateProductUseCase @Inject constructor(
    private val productRepository: ProductRepository,
    private val validateProductUseCase: ValidateProductUseCase = ValidateProductUseCase()
) {

    suspend operator fun invoke(product: Product?): Result<Product> {
        val safeProduct = product
            ?: return Result.failure(IllegalArgumentException("المنتج غير موجود"))

        val validationResult = validateProductUseCase(safeProduct)
        if (!validationResult.isValid) {
            return Result.failure(
                IllegalArgumentException(
                    validationResult.errors.joinToString(separator = "، ")
                )
            )
        }

        return productRepository.saveProduct(safeProduct)
    }
}