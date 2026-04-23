package com.myimdad_por.domain.repository

import com.myimdad_por.domain.model.StockItem
import com.myimdad_por.domain.model.UnitOfMeasure
import java.time.LocalDate
import java.time.LocalDateTime
import kotlinx.coroutines.flow.Flow

/**
 * Contract for stock movements, adjustments, transfers, and audit-friendly history.
 */
interface StockRepository {

    fun observeAllStock(): Flow<List<StockItem>>

    fun observeStockByBarcode(barcode: String): Flow<StockItem?>

    fun observeStockByLocation(location: String): Flow<List<StockItem>>

    fun observeLowStockItems(): Flow<List<StockItem>>

    fun observeExpiredStockItems(): Flow<List<StockItem>>

    suspend fun getStockByBarcode(barcode: String): StockItem?

    suspend fun getStockItems(
        query: String? = null,
        location: String? = null,
        unitOfMeasure: UnitOfMeasure? = null
    ): List<StockItem>

    suspend fun getStockHistory(
        barcode: String,
        from: LocalDateTime? = null,
        to: LocalDateTime? = null
    ): List<StockMovement>

    suspend fun adjustStock(
        barcode: String,
        quantityDelta: Double,
        reason: String,
        location: String? = null
    ): Result<StockItem>

    suspend fun transferStock(
        barcode: String,
        fromLocation: String,
        toLocation: String,
        quantity: Double,
        reason: String? = null
    ): Result<StockTransferResult>

    suspend fun receiveStock(
        barcode: String,
        quantity: Double,
        location: String,
        sourceDocumentId: String? = null,
        reason: String? = null
    ): Result<StockItem>

    suspend fun consumeStock(
        barcode: String,
        quantity: Double,
        location: String,
        reason: String? = null
    ): Result<StockItem>

    suspend fun reserveStock(
        barcode: String,
        quantity: Double,
        referenceId: String? = null
    ): Result<StockReservation>

    suspend fun releaseReservation(reservationId: String): Result<Unit>

    suspend fun setReorderLevel(
        barcode: String,
        reorderLevel: Double
    ): Result<StockItem>

    suspend fun setStockExpiry(
        barcode: String,
        expiryDate: LocalDate
    ): Result<StockItem>

    suspend fun clearExpiredStock(today: LocalDate = LocalDate.now()): Result<Int>

    suspend fun deleteStockItem(barcode: String): Result<Unit>

    suspend fun deleteStockItems(barcodes: List<String>): Result<Int>

    suspend fun clearAll(): Result<Unit>

    suspend fun countItems(): Long

    suspend fun countLowStockItems(): Long

    suspend fun countExpiredStockItems(today: LocalDate = LocalDate.now()): Long

    suspend fun getAvailableQuantity(barcode: String): Double

    suspend fun getTotalQuantityByLocation(location: String): Double
}

/**
 * Immutable movement record for stock traceability.
 */
data class StockMovement(
    val movementId: String,
    val barcode: String,
    val quantityDelta: Double,
    val sourceLocation: String? = null,
    val targetLocation: String? = null,
    val reason: String? = null,
    val referenceId: String? = null,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val metadata: Map<String, String> = emptyMap()
) {
    init {
        require(movementId.isNotBlank()) { "movementId cannot be blank." }
        require(barcode.isNotBlank()) { "barcode cannot be blank." }
        require(quantityDelta.isFinite()) { "quantityDelta must be finite." }
    }
}

/**
 * Result of a stock transfer operation.
 */
data class StockTransferResult(
    val transferId: String,
    val barcode: String,
    val fromLocation: String,
    val toLocation: String,
    val quantity: Double,
    val completedAtMillis: Long = System.currentTimeMillis(),
    val metadata: Map<String, String> = emptyMap()
) {
    init {
        require(transferId.isNotBlank()) { "transferId cannot be blank." }
        require(barcode.isNotBlank()) { "barcode cannot be blank." }
        require(fromLocation.isNotBlank()) { "fromLocation cannot be blank." }
        require(toLocation.isNotBlank()) { "toLocation cannot be blank." }
        require(quantity > 0.0) { "quantity must be greater than zero." }
    }
}

/**
 * Reservation snapshot for held stock.
 */
data class StockReservation(
    val reservationId: String,
    val barcode: String,
    val quantity: Double,
    val referenceId: String? = null,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val expiresAtMillis: Long? = null,
    val metadata: Map<String, String> = emptyMap()
) {
    init {
        require(reservationId.isNotBlank()) { "reservationId cannot be blank." }
        require(barcode.isNotBlank()) { "barcode cannot be blank." }
        require(quantity > 0.0) { "quantity must be greater than zero." }
        expiresAtMillis?.let {
            require(it > 0L) { "expiresAtMillis must be greater than zero when provided." }
        }
    }
}