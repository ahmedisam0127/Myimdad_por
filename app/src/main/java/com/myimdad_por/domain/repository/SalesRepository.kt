package com.myimdad_por.domain.repository

import com.myimdad_por.domain.model.PaymentStatus
import com.myimdad_por.domain.model.Sale
import com.myimdad_por.domain.model.SaleStatus
import java.math.BigDecimal
import java.time.LocalDateTime
import kotlinx.coroutines.flow.Flow

/**
 * Contract for POS sales orchestration.
 */
interface SalesRepository {

    fun observeAllSales(): Flow<List<Sale>>

    fun observeSalesByCustomer(customerId: String): Flow<List<Sale>>

    fun observeSalesByEmployee(employeeId: String): Flow<List<Sale>>

    fun observeSalesByStatus(status: SaleStatus): Flow<List<Sale>>

    fun observePendingSales(): Flow<List<Sale>>

    suspend fun getSaleById(id: String): Sale?

    suspend fun getSaleByInvoiceNumber(invoiceNumber: String): Sale?

    suspend fun getSales(
        from: LocalDateTime? = null,
        to: LocalDateTime? = null,
        customerId: String? = null,
        employeeId: String? = null,
        status: SaleStatus? = null,
        paymentStatus: PaymentStatus? = null
    ): List<Sale>

    suspend fun searchSales(query: String): List<Sale>

    suspend fun saveSale(sale: Sale): Result<Sale>

    suspend fun saveSales(sales: List<Sale>): Result<List<Sale>>

    suspend fun updateSale(sale: Sale): Result<Sale>

    suspend fun validateSale(sale: Sale): Result<SaleValidationResult>

    suspend fun holdSale(sale: Sale): Result<SaleHold>

    suspend fun resumeSale(holdId: String): Result<Sale>

    suspend fun cancelSale(saleId: String, reason: String? = null): Result<Sale>

    suspend fun markSaleCompleted(saleId: String): Result<Sale>

    suspend fun markSaleRefunded(saleId: String): Result<Sale>

    suspend fun applyDiscount(
        saleId: String,
        discountAmount: BigDecimal,
        reason: String? = null
    ): Result<Sale>

    suspend fun getTotalSalesAmount(
        from: LocalDateTime? = null,
        to: LocalDateTime? = null
    ): BigDecimal

    suspend fun getTopSellingProducts(
        limit: Int = 10,
        from: LocalDateTime? = null,
        to: LocalDateTime? = null,
        sortDirection: SortDirection = SortDirection.DESCENDING
    ): List<SaleProductRanking>

    suspend fun getSalesPendingSync(): List<Sale>

    suspend fun markSaleSynced(saleId: String): Result<Sale>

    suspend fun deleteSale(id: String): Result<Unit>

    suspend fun deleteSales(ids: List<String>): Result<Int>

    suspend fun clearAll(): Result<Unit>

    suspend fun countSales(): Long

    suspend fun countSalesByStatus(status: SaleStatus): Long

    suspend fun countCompletedSales(): Long

    suspend fun countPendingSales(): Long
}

/**
 * POS validation result.
 */
data class SaleValidationResult(
    val valid: Boolean,
    val saleId: String? = null,
    val message: String? = null,
    val missingStockCount: Int = 0,
    val lowStockCount: Int = 0,
    val totalAmount: BigDecimal = BigDecimal.ZERO
) {
    init {
        require(missingStockCount >= 0) { "missingStockCount cannot be negative." }
        require(lowStockCount >= 0) { "lowStockCount cannot be negative." }
        require(totalAmount >= BigDecimal.ZERO) { "totalAmount cannot be negative." }
    }
}

/**
 * Stored hold for temporarily suspended sales.
 */
data class SaleHold(
    val holdId: String,
    val saleId: String,
    val reason: String? = null,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val expiresAtMillis: Long? = null,
    val metadata: Map<String, String> = emptyMap()
) {
    init {
        require(holdId.isNotBlank()) { "holdId cannot be blank." }
        require(saleId.isNotBlank()) { "saleId cannot be blank." }
        expiresAtMillis?.let {
            require(it > 0L) { "expiresAtMillis must be greater than zero when provided." }
        }
        reason?.let {
            require(it.isNotBlank()) { "reason cannot be blank when provided." }
        }
    }
}

/**
 * Ranking snapshot for top-selling products.
 */
data class SaleProductRanking(
    val productBarcode: String,
    val productName: String,
    val quantitySold: BigDecimal,
    val revenue: BigDecimal,
    val rank: Int
) {
    init {
        require(productBarcode.isNotBlank()) { "productBarcode cannot be blank." }
        require(productName.isNotBlank()) { "productName cannot be blank." }
        require(quantitySold >= BigDecimal.ZERO) { "quantitySold cannot be negative." }
        require(revenue >= BigDecimal.ZERO) { "revenue cannot be negative." }
        require(rank > 0) { "rank must be greater than zero." }
    }
}