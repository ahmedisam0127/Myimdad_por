package com.myimdad_por.domain.usecase.Product

import com.myimdad_por.domain.model.Product
import com.myimdad_por.domain.repository.ProductRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
class GetProductsUseCase @Inject constructor(
    private val productRepository: ProductRepository
) {

    suspend operator fun invoke(query: String? = null): List<Product> {
        val normalizedQuery = query?.trim().orEmpty()

        return if (normalizedQuery.isBlank()) {
            productRepository.observeAllProducts().first()
        } else {
            productRepository.searchProducts(normalizedQuery)
        }
    }
}