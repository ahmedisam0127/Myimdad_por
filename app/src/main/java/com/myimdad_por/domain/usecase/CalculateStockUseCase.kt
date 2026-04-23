package com.myimdad_por.domain.usecase

import com.myimdad_por.domain.model.StockItem
import com.myimdad_por.domain.repository.StockRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import kotlin.math.max

data class StockCalculationRequest(
    val barcode: String,
    val requestedQuantity: Double? = null,
    val location: String? = null,
    val lowStockThreshold: Double? = null
) {
    init {
        require(barcode.isNotBlank()) { "barcode cannot be blank" }
        requestedQuantity?.let {
            require(it >= 0.0) { "requestedQuantity cannot be negative" }
        }
        lowStockThreshold?.let {
            require(it >= 0.0) { "lowStockThreshold cannot be negative" }
        }
    }
}

data class StockCalculationResult(
    val barcode: String,
    val stockItem: StockItem?,
    val availableQuantity: Double,
    val requestedQuantity: Double? = null,
    val remainingQuantity: Double? = null,
    val shortageQuantity: Double = 0.0,
    val isEnough: Boolean = true,
    val isLowStock: Boolean = false,
    val isExpired: Boolean = false,
    val location: String? = null
)

class CalculateStockUseCase @Inject constructor(
    private val stockRepository: StockRepository
) {

    suspend operator fun invoke(request: StockCalculationRequest): StockCalculationResult {
        val stockItem = stockRepository.getStockByBarcode(request.barcode)
        val availableQuantity = when {
            request.location != null -> stockRepository.getTotalQuantityByLocation(request.location)
            else -> stockRepository.getAvailableQuantity(request.barcode)
        }

        val today = java.time.LocalDate.now()
        val isExpired = stockItem?.isExpired(today) == true

        val isLowStock = if (request.lowStockThreshold != null) {
            availableQuantity <= request.lowStockThreshold
        } else {
            stockRepository.observeLowStockItems().first()
                .any { it.normalizedBarcode == request.barcode }
        }

        val requested = request.requestedQuantity
        val remaining = requested?.let { max(0.0, availableQuantity - it) }
        val shortage = requested?.let { max(0.0, it - availableQuantity) } ?: 0.0
        val enough = requested?.let { availableQuantity >= it } ?: true

        return StockCalculationResult(
            barcode = request.barcode,
            stockItem = stockItem,
            availableQuantity = availableQuantity,
            requestedQuantity = requested,
            remainingQuantity = remaining,
            shortageQuantity = shortage,
            isEnough = enough,
            isLowStock = isLowStock,
            isExpired = isExpired,
            location = request.location ?: stockItem?.normalizedLocation
        )
    }

    suspend fun forBarcode(barcode: String): StockCalculationResult {
        return invoke(StockCalculationRequest(barcode = barcode))
    }

    suspend fun canFulfill(
        barcode: String,
        quantity: Double,
        location: String? = null
    ): Boolean {
        val result = invoke(
            StockCalculationRequest(
                barcode = barcode,
                requestedQuantity = quantity,
                location = location
            )
        )
        return result.isEnough && !result.isExpired
    }
}