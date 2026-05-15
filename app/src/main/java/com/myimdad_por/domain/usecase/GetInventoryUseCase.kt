package com.myimdad_por.domain.usecase

import com.myimdad_por.domain.model.StockItem
import com.myimdad_por.domain.model.UnitOfMeasure
import com.myimdad_por.domain.repository.StockRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject

data class InventoryQuery(
    val query: String? = null,
    val location: String? = null,
    val unitOfMeasure: UnitOfMeasure? = null,
    val barcode: String? = null,
    val lowStockOnly: Boolean = false,
    val expiredOnly: Boolean = false,
    val includeOutOfStock: Boolean = true
)

data class InventorySnapshot(
    val items: List<StockItem>,
    val totalItems: Int,
    val totalQuantityByLocation: Double? = null
)

class GetInventoryUseCase @Inject constructor(
    private val stockRepository: StockRepository
) {

    suspend operator fun invoke(query: InventoryQuery = InventoryQuery()): List<StockItem> {
        query.barcode?.trim()?.takeIf { it.isNotBlank() }?.let {
            return listOfNotNull(stockRepository.getStockByBarcode(it))
                .filterByFlags(query)
        }

        var items = stockRepository.getStockItems(
            query = query.query,
            location = query.location,
            unitOfMeasure = query.unitOfMeasure
        )

        if (query.lowStockOnly) {
            val lowStockBarcodes = stockRepository.observeLowStockItems().first()
                .map { it.normalizedBarcode }
                .toSet()
            items = items.filter { it.normalizedBarcode in lowStockBarcodes }
        }

        if (query.expiredOnly) {
            val today = java.time.LocalDate.now()
            items = items.filter { it.isExpired(today) }
        }

        if (!query.includeOutOfStock) {
            items = items.filter { !it.isOutOfStock }
        }

        return items
    }

    fun observeAll(): Flow<List<StockItem>> = stockRepository.observeAllStock()

    fun observeByBarcode(barcode: String): Flow<StockItem?> {
        require(barcode.isNotBlank()) { "barcode cannot be blank" }
        return stockRepository.observeStockByBarcode(barcode)
    }

    fun observeByLocation(location: String): Flow<List<StockItem>> {
        require(location.isNotBlank()) { "location cannot be blank" }
        return stockRepository.observeStockByLocation(location)
    }

    fun observeLowStock(): Flow<List<StockItem>> = stockRepository.observeLowStockItems()

    fun observeExpired(): Flow<List<StockItem>> = stockRepository.observeExpiredStockItems()

    suspend fun getByBarcode(barcode: String): StockItem? {
        require(barcode.isNotBlank()) { "barcode cannot be blank" }
        return stockRepository.getStockByBarcode(barcode)
    }

    suspend fun countItems(): Long = stockRepository.countItems()

    suspend fun countLowStockItems(): Long = stockRepository.countLowStockItems()

    suspend fun countExpiredItems(today: java.time.LocalDate = java.time.LocalDate.now()): Long {
        return stockRepository.countExpiredStockItems(today)
    }

    suspend fun getAvailableQuantity(barcode: String): Double {
        require(barcode.isNotBlank()) { "barcode cannot be blank" }
        return stockRepository.getAvailableQuantity(barcode)
    }

    suspend fun getTotalQuantityByLocation(location: String): Double {
        require(location.isNotBlank()) { "location cannot be blank" }
        return stockRepository.getTotalQuantityByLocation(location)
    }

    private fun List<StockItem>.filterByFlags(query: InventoryQuery): List<StockItem> {
        val today = java.time.LocalDate.now()
        return asSequence()
            .filter { query.includeOutOfStock || !it.isOutOfStock }
            .filter { !query.expiredOnly || it.isExpired(today) }
            .toList()
    }
}