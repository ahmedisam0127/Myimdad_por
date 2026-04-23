package com.myimdad_por.domain.repository

import com.myimdad_por.domain.model.Product
import com.myimdad_por.domain.model.UnitOfMeasure
import java.math.BigDecimal
import kotlinx.coroutines.flow.Flow

/**
 * Contract for product catalog and live stock visibility.
 *
 * Keep implementation in the data layer and bind it with Hilt.
 */
interface ProductRepository {

    fun observeAllProducts(): Flow<List<Product>>

    fun observeProductByBarcode(barcode: String): Flow<Product?>

    fun observeProductsByName(query: String): Flow<List<Product>>

    fun observeLowStockProducts(
        threshold: BigDecimal? = null
    ): Flow<List<ProductStockSnapshot>>

    suspend fun getProductByBarcode(barcode: String): Product?

    suspend fun getProductByName(name: String): Product?

    suspend fun searchProducts(query: String): List<Product>

    suspend fun getProductsByIds(ids: List<String>): List<Product>

    suspend fun getProductsByUnit(unitOfMeasure: UnitOfMeasure): List<Product>

    suspend fun saveProduct(product: Product): Result<Product>

    suspend fun saveProducts(products: List<Product>): Result<List<Product>>

    suspend fun updateProduct(product: Product): Result<Product>

    suspend fun updateProductPrice(
        barcode: String,
        price: BigDecimal
    ): Result<Product>

    suspend fun updateProductState(
        barcode: String,
        isActive: Boolean
    ): Result<Product>

    suspend fun deleteProduct(barcode: String): Result<Unit>

    suspend fun deleteProducts(barcodes: List<String>): Result<Int>

    suspend fun clearAll(): Result<Unit>

    suspend fun countProducts(): Long

    suspend fun countActiveProducts(): Long

    suspend fun countLowStockProducts(
        threshold: BigDecimal? = null
    ): Long

    suspend fun getStockSnapshot(barcode: String): ProductStockSnapshot?

    suspend fun getLowStockProducts(
        threshold: BigDecimal? = null
    ): List<ProductStockSnapshot>

    suspend fun syncProductStock(
        barcode: String,
        quantityDelta: BigDecimal,
        reason: String? = null
    ): Result<ProductStockSnapshot>
}

/**
 * Lightweight inventory snapshot for fast POS and alert screens.
 */
data class ProductStockSnapshot(
    val barcode: String,
    val productName: String,
    val displayName: String? = null,
    val unitOfMeasure: UnitOfMeasure,
    val quantityOnHand: BigDecimal,
    val reorderLevel: BigDecimal,
    val lastUpdatedMillis: Long = System.currentTimeMillis()
) {
    init {
        require(barcode.isNotBlank()) { "barcode cannot be blank." }
        require(productName.isNotBlank()) { "productName cannot be blank." }
        require(quantityOnHand >= BigDecimal.ZERO) { "quantityOnHand cannot be negative." }
        require(reorderLevel >= BigDecimal.ZERO) { "reorderLevel cannot be negative." }
    }

    val isLowStock: Boolean
        get() = quantityOnHand <= reorderLevel

    val effectiveName: String
        get() = displayName?.trim().takeUnless { it.isNullOrBlank() } ?: productName.trim()
}