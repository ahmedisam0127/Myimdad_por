package com.myimdad_por.domain.usecase.Product

import com.myimdad_por.core.utils.ValidationUtils
import com.myimdad_por.domain.model.Product
import com.myimdad_por.domain.model.UnitOfMeasure
import java.math.BigDecimal
import javax.inject.Inject
class FilterProductsUseCase @Inject constructor( ){

    operator fun invoke(
        products: List<Product>,
        query: String? = null,
        isActive: Boolean? = null,
        unitOfMeasure: UnitOfMeasure? = null,
        minPrice: BigDecimal? = null,
        maxPrice: BigDecimal? = null
    ): List<Product> {
        if (products.isEmpty()) return emptyList()

        val normalizedQuery = ValidationUtils.normalizeText(query)

        return products.asSequence()
            .filter { product ->
                isActive == null || product.isActive == isActive
            }
            .filter { product ->
                unitOfMeasure == null || product.unitOfMeasure == unitOfMeasure
            }
            .filter { product ->
                minPrice == null || product.price >= minPrice
            }
            .filter { product ->
                maxPrice == null || product.price <= maxPrice
            }
            .filter { product ->
                normalizedQuery.isBlank() || matchesQuery(product, normalizedQuery)
            }
            .toList()
    }

    private fun matchesQuery(product: Product, query: String): Boolean {
        return product.barcode.contains(query, ignoreCase = true) ||
            product.name.contains(query, ignoreCase = true) ||
            product.effectiveName.contains(query, ignoreCase = true) ||
            (product.displayName?.contains(query, ignoreCase = true) == true) ||
            (product.description?.contains(query, ignoreCase = true) == true)
    }
}