package com.myimdad_por.domain.repository

import com.myimdad_por.domain.model.Purchase
import com.myimdad_por.domain.model.PurchaseItem
import com.myimdad_por.domain.model.PurchasePaymentStatus
import com.myimdad_por.domain.model.PurchaseStatus
import java.math.BigDecimal
import java.time.LocalDateTime
import kotlinx.coroutines.flow.Flow

/**
 * Contract for purchases, supplier debt, and stock receiving workflow.
 */
interface PurchaseRepository {

    fun observeAllPurchases(): Flow<List<Purchase>>

    fun observePurchasesBySupplier(supplierId: String): Flow<List<Purchase>>

    fun observePurchasesByStatus(status: PurchaseStatus): Flow<List<Purchase>>

    fun observePendingPurchases(): Flow<List<Purchase>>

    fun observeOpenPurchases(): Flow<List<Purchase>>

    suspend fun getPurchaseById(id: String): Purchase?

    suspend fun getPurchaseByInvoiceNumber(invoiceNumber: String): Purchase?

    suspend fun getPurchases(
        from: LocalDateTime? = null,
        to: LocalDateTime? = null,
        supplierId: String? = null,
        employeeId: String? = null,
        status: PurchaseStatus? = null
    ): List<Purchase>

    suspend fun searchPurchases(query: String): List<Purchase>

    suspend fun savePurchase(purchase: Purchase): Result<Purchase>

    suspend fun savePurchases(purchases: List<Purchase>): Result<List<Purchase>>

    suspend fun updatePurchase(purchase: Purchase): Result<Purchase>

    suspend fun addItem(
        purchaseId: String,
        item: PurchaseItem
    ): Result<Purchase>

    suspend fun removeItem(
        purchaseId: String,
        itemId: String
    ): Result<Purchase>

    suspend fun confirmPurchase(purchaseId: String): Result<Purchase>

    suspend fun receivePurchase(purchaseId: String): Result<PurchaseReceivingResult>

    suspend fun closePurchase(purchaseId: String): Result<Purchase>

    suspend fun cancelPurchase(
        purchaseId: String,
        reason: String? = null
    ): Result<Purchase>

    suspend fun registerPayment(
        purchaseId: String,
        amount: BigDecimal
    ): Result<Purchase>

    suspend fun getTotalPurchases(
        from: LocalDateTime? = null,
        to: LocalDateTime? = null
    ): BigDecimal

    suspend fun getOutstandingSupplierDebt(
        supplierId: String? = null
    ): BigDecimal

    suspend fun getPaymentsStatus(
        purchaseId: String
    ): PurchasePaymentStatus

    suspend fun deletePurchase(id: String): Result<Unit>

    suspend fun deletePurchases(ids: List<String>): Result<Int>

    suspend fun clearAll(): Result<Unit>

    suspend fun countPurchases(): Long

    suspend fun countPurchasesByStatus(status: PurchaseStatus): Long

    suspend fun countOpenPurchases(): Long
}

/**
 * Result of receiving stock from a purchase order.
 */
data class PurchaseReceivingResult(
    val purchaseId: String,
    val purchaseStatus: PurchaseStatus,
    val receivedItemsCount: Int,
    val accountingEntryIds: List<String> = emptyList(),
    val productUpdatesCount: Int = 0,
    val totalReceivedAmount: BigDecimal = BigDecimal.ZERO,
    val note: String? = null
) {
    init {
        require(purchaseId.isNotBlank()) { "purchaseId cannot be blank." }
        require(receivedItemsCount >= 0) { "receivedItemsCount cannot be negative." }
        require(productUpdatesCount >= 0) { "productUpdatesCount cannot be negative." }
        require(totalReceivedAmount >= BigDecimal.ZERO) { "totalReceivedAmount cannot be negative." }
        note?.let {
            require(it.isNotBlank()) { "note cannot be blank when provided." }
        }
    }
}