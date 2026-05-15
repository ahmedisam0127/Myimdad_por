package com.myimdad_por.domain.usecase.Product

import com.myimdad_por.core.utils.ValidationUtils
import com.myimdad_por.domain.model.Product
import com.myimdad_por.domain.repository.ProductRepository
import javax.inject.Inject
class SearchProductsUseCase @Inject constructor(
    private val productRepository: ProductRepository
) {

    suspend operator fun invoke(query: String?): List<Product> {
        val normalizedQuery = query?.trim().orEmpty()
        if (normalizedQuery.isBlank()) return emptyList()
        if (!isValidQuery(normalizedQuery)) return emptyList()

        return productRepository.searchProducts(normalizedQuery)
    }

    private fun isValidQuery(value: String): Boolean {
        if (value.length < MIN_QUERY_LENGTH) return false
        if (value.length > MAX_QUERY_LENGTH) return false
        return ValidationUtils.isNotEmpty(value)
    }

    companion object {
        private const val MIN_QUERY_LENGTH = 2
        private const val MAX_QUERY_LENGTH = 100
    }
}