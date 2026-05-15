package com.myimdad_por.domain.usecase.Product

import com.myimdad_por.domain.model.Product
import javax.inject.Inject
class GetTopSellingProductsUseCase @Inject constructor( ){

    operator fun invoke(
        products: List<Product>,
        salesCountsByBarcode: Map<String, Long>,
        limit: Int = DEFAULT_LIMIT,
        activeOnly: Boolean = true
    ): List<Product> {
        if (products.isEmpty() || limit <= 0) return emptyList()

        return products.asSequence()
            .filter { product ->
                !activeOnly || product.isActive
            }
            .sortedWith(
                compareByDescending<Product> { salesCountsByBarcode[it.barcode.trim()] ?: 0L }
                    .thenBy { it.effectiveName.lowercase() }
            )
            .take(limit)
            .toList()
    }

    fun rankBySales(
        products: List<Product>,
        salesCountsByBarcode: Map<String, Long>
    ): List<TopSellingProduct> {
        if (products.isEmpty()) return emptyList()

        return products.asSequence()
            .map { product ->
                TopSellingProduct(
                    product = product,
                    salesCount = salesCountsByBarcode[product.barcode.trim()] ?: 0L
                )
            }
            .sortedWith(
                compareByDescending<TopSellingProduct> { it.salesCount }
                    .thenBy { it.product.effectiveName.lowercase() }
            )
            .toList()
    }

    data class TopSellingProduct(
        val product: Product,
        val salesCount: Long
    )

    companion object {
        private const val DEFAULT_LIMIT = 10
    }
}