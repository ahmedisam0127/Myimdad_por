package com.myimdad_por.domain.usecase

import com.myimdad_por.domain.model.Purchase
import com.myimdad_por.domain.model.PurchaseItem
import com.myimdad_por.domain.model.PurchasePaymentStatus
import com.myimdad_por.domain.model.PurchaseStatus
import com.myimdad_por.domain.repository.PurchaseReceivingResult
import com.myimdad_por.domain.repository.PurchaseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.math.BigDecimal
import java.time.LocalDateTime
import javax.inject.Inject

data class PurchaseQuery(
    val from: LocalDateTime? = null,
    val to: LocalDateTime? = null,
    val supplierId: String? = null,
    val employeeId: String? = null,
    val status: PurchaseStatus? = null,
    val paymentStatus: PurchasePaymentStatus? = null,
    val openOnly: Boolean = false,
    val pendingOnly: Boolean = false,
    val text: String? = null
)

class GetPurchasesUseCase @Inject constructor(
    private val purchaseRepository: PurchaseRepository
) {

    suspend operator fun invoke(query: PurchaseQuery = PurchaseQuery()): List<Purchase> {
        val effectiveStatus = when {
            query.openOnly && query.status == null -> PurchaseStatus.CONFIRMED
            query.pendingOnly && query.status == null -> PurchaseStatus.DRAFT
            else -> query.status
        }

        val base = when {
            query.openOnly &&
                query.from == null &&
                query.to == null &&
                query.supplierId == null &&
                query.employeeId == null &&
                query.status == null &&
                query.paymentStatus == null &&
                query.text.isNullOrBlank() -> purchaseRepository.observeOpenPurchases().first()

            query.pendingOnly &&
                query.from == null &&
                query.to == null &&
                query.supplierId == null &&
                query.employeeId == null &&
                query.status == null &&
                query.paymentStatus == null &&
                query.text.isNullOrBlank() -> purchaseRepository.observePendingPurchases().first()

            query.from == null &&
                query.to == null &&
                query.supplierId == null &&
                query.employeeId == null &&
                effectiveStatus == null &&
                query.paymentStatus == null &&
                query.text.isNullOrBlank() -> purchaseRepository.observeAllPurchases().first()

            else -> purchaseRepository.getPurchases(
                from = query.from,
                to = query.to,
                supplierId = query.supplierId,
                employeeId = query.employeeId,
                status = effectiveStatus
            )
        }

        val filtered = base
            .filter { query.text.isNullOrBlank() || matchesText(it, query.text) }
            .filter { query.paymentStatus == null || runBlockingPaymentStatus(it) == query.paymentStatus }

        return filtered
    }

    fun observeAll(): Flow<List<Purchase>> = purchaseRepository.observeAllPurchases()

    fun observeBySupplier(supplierId: String): Flow<List<Purchase>> {
        require(supplierId.isNotBlank()) { "supplierId cannot be blank" }
        return purchaseRepository.observePurchasesBySupplier(supplierId)
    }

    fun observeByStatus(status: PurchaseStatus): Flow<List<Purchase>> =
        purchaseRepository.observePurchasesByStatus(status)

    fun observePending(): Flow<List<Purchase>> = purchaseRepository.observePendingPurchases()

    fun observeOpen(): Flow<List<Purchase>> = purchaseRepository.observeOpenPurchases()

    suspend fun getById(id: String): Purchase? {
        require(id.isNotBlank()) { "id cannot be blank" }
        return purchaseRepository.getPurchaseById(id)
    }

    suspend fun getByInvoiceNumber(invoiceNumber: String): Purchase? {
        require(invoiceNumber.isNotBlank()) { "invoiceNumber cannot be blank" }
        return purchaseRepository.getPurchaseByInvoiceNumber(invoiceNumber)
    }

    suspend fun search(query: String): List<Purchase> {
        require(query.isNotBlank()) { "query cannot be blank" }
        return purchaseRepository.searchPurchases(query)
    }

    suspend fun save(purchase: Purchase) = purchaseRepository.savePurchase(purchase)

    suspend fun saveAll(purchases: List<Purchase>) = purchaseRepository.savePurchases(purchases)

    suspend fun update(purchase: Purchase) = purchaseRepository.updatePurchase(purchase)

    suspend fun addItem(purchaseId: String, item: PurchaseItem) =
        purchaseRepository.addItem(purchaseId, item)

    suspend fun removeItem(purchaseId: String, itemId: String) =
        purchaseRepository.removeItem(purchaseId, itemId)

    suspend fun confirm(purchaseId: String) = purchaseRepository.confirmPurchase(purchaseId)

    suspend fun receive(purchaseId: String) = purchaseRepository.receivePurchase(purchaseId)

    suspend fun close(purchaseId: String) = purchaseRepository.closePurchase(purchaseId)

    suspend fun cancel(purchaseId: String, reason: String? = null) =
        purchaseRepository.cancelPurchase(purchaseId, reason)

    suspend fun registerPayment(purchaseId: String, amount: BigDecimal) =
        purchaseRepository.registerPayment(purchaseId, amount)

    suspend fun totalPurchases(
        from: LocalDateTime? = null,
        to: LocalDateTime? = null
    ): BigDecimal = purchaseRepository.getTotalPurchases(from, to)

    suspend fun outstandingSupplierDebt(supplierId: String? = null): BigDecimal =
        purchaseRepository.getOutstandingSupplierDebt(supplierId)

    suspend fun paymentStatus(purchaseId: String): PurchasePaymentStatus =
        purchaseRepository.getPaymentsStatus(purchaseId)

    suspend fun countAll(): Long = purchaseRepository.countPurchases()

    suspend fun countByStatus(status: PurchaseStatus): Long =
        purchaseRepository.countPurchasesByStatus(status)

    suspend fun countOpen(): Long = purchaseRepository.countOpenPurchases()

    private fun matchesText(purchase: Purchase, text: String): Boolean {
        val q = text.trim()
        return purchase.id.contains(q, ignoreCase = true) ||
            purchase.invoiceNumber.contains(q, ignoreCase = true) ||
            purchase.supplierName.contains(q, ignoreCase = true) ||
            purchase.note?.contains(q, ignoreCase = true) == true
    }

    private suspend fun runBlockingPaymentStatus(purchase: Purchase): PurchasePaymentStatus {
        return purchaseRepository.getPaymentsStatus(purchase.id)
    }
}