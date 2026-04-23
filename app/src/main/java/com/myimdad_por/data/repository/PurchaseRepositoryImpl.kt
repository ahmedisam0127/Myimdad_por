package com.myimdad_por.data.repository

import com.myimdad_por.core.dispatchers.AppDispatchers
import com.myimdad_por.core.dispatchers.DefaultAppDispatchers
import com.myimdad_por.data.local.dao.PurchaseDao
import com.myimdad_por.data.local.entity.PurchaseEntity
import com.myimdad_por.data.local.entity.toDomain
import com.myimdad_por.domain.model.Purchase
import com.myimdad_por.domain.model.PurchaseItem
import com.myimdad_por.domain.model.PurchasePaymentStatus
import com.myimdad_por.domain.model.PurchaseStatus
import com.myimdad_por.domain.repository.PurchaseReceivingResult
import com.myimdad_por.domain.repository.PurchaseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.time.LocalDateTime

class PurchaseRepositoryImpl(
    private val purchaseDao: PurchaseDao,
    private val dispatchers: AppDispatchers = DefaultAppDispatchers
) : PurchaseRepository {

    override fun observeAllPurchases(): Flow<List<Purchase>> {
        return purchaseDao.observeAll()
            .map { entities -> entities.map { it.toDomain() } }
            .flowOn(dispatchers.io)
    }

    override fun observePurchasesBySupplier(supplierId: String): Flow<List<Purchase>> {
        return purchaseDao.observeBySupplierId(supplierId.trim())
            .map { entities -> entities.map { it.toDomain() } }
            .flowOn(dispatchers.io)
    }

    override fun observePurchasesByStatus(status: PurchaseStatus): Flow<List<Purchase>> {
        return purchaseDao.observeByStatus(status.name)
            .map { entities -> entities.map { it.toDomain() } }
            .flowOn(dispatchers.io)
    }

    override fun observePendingPurchases(): Flow<List<Purchase>> {
        return purchaseDao.observeAll()
            .map { list -> 
                list.map { it.toDomain() }.filter {
                    it.status.name == "PENDING" || it.status == PurchaseStatus.DRAFT
                }
            }.flowOn(dispatchers.io)
    }

    override fun observeOpenPurchases(): Flow<List<Purchase>> {
        return purchaseDao.observeAll()
            .map { list ->
                list.map { it.toDomain() }.filter {
                    it.status != enumValueOf<PurchaseStatus>("CLOSED") &&
                    it.status != enumValueOf<PurchaseStatus>("CANCELLED") &&
                    it.status != enumValueOf<PurchaseStatus>("RECEIVED")
                }
            }.flowOn(dispatchers.io)
    }

    override suspend fun getPurchaseById(id: String): Purchase? = withContext(dispatchers.io) {
        val normalizedId = id.trim()
        if (normalizedId.isBlank()) return@withContext null
        purchaseDao.getById(normalizedId)?.toDomain()
    }

    override suspend fun getPurchaseByInvoiceNumber(invoiceNumber: String): Purchase? = withContext(dispatchers.io) {
        val normalized = invoiceNumber.trim()
        if (normalized.isBlank()) return@withContext null
        purchaseDao.getByInvoiceNumber(normalized)?.toDomain()
    }

    override suspend fun getPurchases(
        from: LocalDateTime?,
        to: LocalDateTime?,
        supplierId: String?,
        employeeId: String?,
        status: PurchaseStatus?
    ): List<Purchase> = withContext(dispatchers.io) {
        var purchases = purchaseDao.observeAll().first().map { it.toDomain() }

        if (from != null) purchases = purchases.filter { it.createdAt >= from }
        if (to != null) purchases = purchases.filter { it.createdAt <= to }
        if (!supplierId.isNullOrBlank()) purchases = purchases.filter { it.supplierId == supplierId.trim() }
        if (!employeeId.isNullOrBlank()) purchases = purchases.filter { it.employeeId == employeeId.trim() }
        if (status != null) purchases = purchases.filter { it.status == status }

        purchases
    }

    override suspend fun searchPurchases(query: String): List<Purchase> = withContext(dispatchers.io) {
        val normalized = query.trim()
        if (normalized.isBlank()) return@withContext emptyList()
        purchaseDao.search(normalized).first().map { it.toDomain() }
    }

    override suspend fun savePurchase(purchase: Purchase): Result<Purchase> = withContext(dispatchers.io) {
        runCatching {
            val existing = purchaseDao.getById(purchase.id)
            val entityToSave = PurchaseEntity.fromDomain(
                purchase = purchase,
                serverId = existing?.serverId,
                syncState = "PENDING",
                isDeleted = existing?.isDeleted ?: false,
                syncedAtMillis = existing?.syncedAtMillis,
                updatedAtMillis = System.currentTimeMillis()
            )
            purchaseDao.insert(entityToSave)
            purchase
        }
    }

    override suspend fun savePurchases(purchases: List<Purchase>): Result<List<Purchase>> = withContext(dispatchers.io) {
        runCatching {
            purchases.map { savePurchase(it).getOrThrow() }
        }
    }

    override suspend fun updatePurchase(purchase: Purchase): Result<Purchase> {
        return savePurchase(purchase)
    }

    override suspend fun addItem(purchaseId: String, item: PurchaseItem): Result<Purchase> = withContext(dispatchers.io) {
        runCatching {
            val purchase = getPurchaseById(purchaseId) ?: error("Purchase with id $purchaseId not found")
            val updatedItems = purchase.items + item
            // تغيير العناصر فقط، نموذج الـ Domain سيتكفل بحساب الإجماليات (Totals)
            val updatedPurchase = purchase.copy(items = updatedItems)
            savePurchase(updatedPurchase).getOrThrow()
        }
    }

    override suspend fun removeItem(purchaseId: String, itemId: String): Result<Purchase> = withContext(dispatchers.io) {
        runCatching {
            val purchase = getPurchaseById(purchaseId) ?: error("Purchase with id $purchaseId not found")
            val updatedItems = purchase.items.filterNot { it.id == itemId }
            val updatedPurchase = purchase.copy(items = updatedItems)
            savePurchase(updatedPurchase).getOrThrow()
        }
    }

    override suspend fun confirmPurchase(purchaseId: String): Result<Purchase> = withContext(dispatchers.io) {
        runCatching {
            val purchase = getPurchaseById(purchaseId) ?: error("Purchase not found")
            val updated = purchase.copy(status = enumValueOf<PurchaseStatus>("CONFIRMED"))
            savePurchase(updated).getOrThrow()
        }
    }

    override suspend fun receivePurchase(purchaseId: String): Result<PurchaseReceivingResult> = withContext(dispatchers.io) {
        runCatching {
            val purchase = getPurchaseById(purchaseId) ?: error("Purchase not found")
            val receivedStatus = enumValueOf<PurchaseStatus>("RECEIVED")
            
            val updated = purchase.copy(status = receivedStatus)
            savePurchase(updated).getOrThrow()

            PurchaseReceivingResult(
                purchaseId = updated.id,
                purchaseStatus = updated.status,
                receivedItemsCount = updated.items.size,
                totalReceivedAmount = updated.totalAmount, // يتم قراءته تلقائياً من الموديل
                note = "Purchase received automatically."
            )
        }
    }

    override suspend fun closePurchase(purchaseId: String): Result<Purchase> = withContext(dispatchers.io) {
        runCatching {
            val purchase = getPurchaseById(purchaseId) ?: error("Purchase not found")
            val updated = purchase.copy(status = enumValueOf<PurchaseStatus>("CLOSED"))
            savePurchase(updated).getOrThrow()
        }
    }

    override suspend fun cancelPurchase(purchaseId: String, reason: String?): Result<Purchase> = withContext(dispatchers.io) {
        runCatching {
            val purchase = getPurchaseById(purchaseId) ?: error("Purchase not found")
            val updatedNote = if (reason.isNullOrBlank()) purchase.note else "${purchase.note ?: ""}\nCancellation Reason: $reason".trim()
            val updated = purchase.copy(
                status = enumValueOf<PurchaseStatus>("CANCELLED"),
                note = updatedNote
            )
            savePurchase(updated).getOrThrow()
        }
    }

    override suspend fun registerPayment(purchaseId: String, amount: BigDecimal): Result<Purchase> = withContext(dispatchers.io) {
        runCatching {
            require(amount > BigDecimal.ZERO) { "Payment amount must be positive" }
            val purchase = getPurchaseById(purchaseId) ?: error("Purchase not found")
            
            val newPaidAmount = purchase.paidAmount + amount
            // تغيير المبلغ المدفوع فقط، وسيقوم الموديل باستنتاج الـ paymentStatus والـ remainingAmount تلقائياً
            val updated = purchase.copy(paidAmount = newPaidAmount)
            savePurchase(updated).getOrThrow()
        }
    }

    override suspend fun getTotalPurchases(from: LocalDateTime?, to: LocalDateTime?): BigDecimal = withContext(dispatchers.io) {
        val purchases = getPurchases(from = from, to = to)
        purchases.filter { it.status != enumValueOf<PurchaseStatus>("CANCELLED") }
            .fold(BigDecimal.ZERO) { acc, purchase -> acc + purchase.totalAmount }
    }

    override suspend fun getOutstandingSupplierDebt(supplierId: String?): BigDecimal = withContext(dispatchers.io) {
        val purchases = getPurchases(supplierId = supplierId)
        purchases.filter { 
            it.status != enumValueOf<PurchaseStatus>("CANCELLED") && 
            it.paymentStatus != PurchasePaymentStatus.PAID 
        }.fold(BigDecimal.ZERO) { acc, purchase -> acc + purchase.remainingAmount }
    }

    override suspend fun getPaymentsStatus(purchaseId: String): PurchasePaymentStatus = withContext(dispatchers.io) {
        val purchase = getPurchaseById(purchaseId) ?: error("Purchase not found")
        purchase.paymentStatus
    }

    override suspend fun deletePurchase(id: String): Result<Unit> = withContext(dispatchers.io) {
        runCatching {
            val existing = purchaseDao.getById(id) ?: return@runCatching
            purchaseDao.softDelete(
                id = existing.id,
                updatedAtMillis = System.currentTimeMillis(),
                syncState = "PENDING"
            )
            Unit
        }
    }

    override suspend fun deletePurchases(ids: List<String>): Result<Int> = withContext(dispatchers.io) {
        runCatching {
            var count = 0
            ids.mapNotNull { it.trim().takeIf(String::isNotBlank) }.distinct().forEach { id ->
                if (deletePurchase(id).isSuccess) count++
            }
            count
        }
    }

    override suspend fun clearAll(): Result<Unit> = withContext(dispatchers.io) {
        runCatching {
            val all = purchaseDao.observeAll().first()
            all.forEach { purchaseDao.softDelete(it.id) }
            purchaseDao.purgeDeleted()
            Unit
        }
    }

    override suspend fun countPurchases(): Long = withContext(dispatchers.io) {
        purchaseDao.countActive().toLong()
    }

    override suspend fun countPurchasesByStatus(status: PurchaseStatus): Long = withContext(dispatchers.io) {
        purchaseDao.observeByStatus(status.name).first().size.toLong()
    }

    override suspend fun countOpenPurchases(): Long = withContext(dispatchers.io) {
        observeOpenPurchases().first().size.toLong()
    }
}
