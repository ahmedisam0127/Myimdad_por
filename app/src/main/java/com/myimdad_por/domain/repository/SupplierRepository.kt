package com.myimdad_por.domain.repository

import com.myimdad_por.domain.model.Supplier
import java.math.BigDecimal
import java.time.LocalDateTime
import kotlinx.coroutines.flow.Flow

/**
 * Contract for supplier management and supply chain visibility.
 */
interface SupplierRepository {

    fun observeAllSuppliers(): Flow<List<Supplier>>

    fun observeSupplierById(id: String): Flow<Supplier?>

    fun observeSupplierByCode(supplierCode: String): Flow<Supplier?>

    fun observeActiveSuppliers(): Flow<List<Supplier>>

    fun observePreferredSuppliers(): Flow<List<Supplier>>

    fun observeSuppliersWithDebt(): Flow<List<Supplier>>

    suspend fun getSupplierById(id: String): Supplier?

    suspend fun getSupplierByCode(supplierCode: String): Supplier?

    suspend fun getSupplierByEmail(email: String): Supplier?

    suspend fun searchSuppliers(query: String): List<Supplier>

    suspend fun getSuppliersWithDebt(): List<Supplier>

    suspend fun getSupplierBalance(supplierId: String): BigDecimal

    suspend fun getSupplierBalanceByCode(supplierCode: String): BigDecimal

    suspend fun observeSupplierPurchases(supplierId: String): Flow<List<com.myimdad_por.domain.model.Purchase>>

    suspend fun getSupplierPurchases(
        supplierId: String,
        from: LocalDateTime? = null,
        to: LocalDateTime? = null
    ): List<com.myimdad_por.domain.model.Purchase>

    suspend fun saveSupplier(supplier: Supplier): Result<Supplier>

    suspend fun saveSuppliers(suppliers: List<Supplier>): Result<List<Supplier>>

    suspend fun updateSupplier(supplier: Supplier): Result<Supplier>

    suspend fun updateSupplierDebt(
        supplierId: String,
        balanceDelta: BigDecimal
    ): Result<Supplier>

    suspend fun markSupplierPreferred(
        supplierId: String,
        preferred: Boolean
    ): Result<Supplier>

    suspend fun markSupplierActive(
        supplierId: String,
        active: Boolean
    ): Result<Supplier>

    suspend fun deleteSupplier(id: String): Result<Unit>

    suspend fun deleteSuppliers(ids: List<String>): Result<Int>

    suspend fun clearAll(): Result<Unit>

    suspend fun countSuppliers(): Long

    suspend fun countActiveSuppliers(): Long

    suspend fun countPreferredSuppliers(): Long

    suspend fun countSuppliersWithDebt(): Long

    suspend fun getSupplierPerformance(
        supplierId: String,
        from: LocalDateTime? = null,
        to: LocalDateTime? = null
    ): SupplierPerformanceSummary
}

/**
 * Optional quality snapshot for suppliers.
 */
data class SupplierPerformanceSummary(
    val supplierId: String,
    val totalPurchases: Long,
    val totalSpent: BigDecimal,
    val totalOutstanding: BigDecimal,
    val onTimeDeliveries: Long = 0L,
    val lateDeliveries: Long = 0L,
    val rejectedOrders: Long = 0L,
    val averageDeliveryDays: Double = 0.0
) {
    init {
        require(supplierId.isNotBlank()) { "supplierId cannot be blank." }
        require(totalPurchases >= 0L) { "totalPurchases cannot be negative." }
        require(totalSpent >= BigDecimal.ZERO) { "totalSpent cannot be negative." }
        require(totalOutstanding >= BigDecimal.ZERO) { "totalOutstanding cannot be negative." }
        require(onTimeDeliveries >= 0L) { "onTimeDeliveries cannot be negative." }
        require(lateDeliveries >= 0L) { "lateDeliveries cannot be negative." }
        require(rejectedOrders >= 0L) { "rejectedOrders cannot be negative." }
        require(averageDeliveryDays >= 0.0) { "averageDeliveryDays cannot be negative." }
    }
}