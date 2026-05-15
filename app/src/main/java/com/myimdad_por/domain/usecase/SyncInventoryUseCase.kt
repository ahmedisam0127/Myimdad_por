package com.myimdad_por.domain.usecase

import com.myimdad_por.domain.model.StockItem
import com.myimdad_por.domain.repository.StockMovement
import com.myimdad_por.domain.repository.StockRepository
import java.time.LocalDate
import javax.inject.Inject

data class SyncInventoryItem(
    val barcode: String,
    val targetQuantity: Double,
    val location: String? = null,
    val expiryDate: LocalDate? = null,
    val reason: String? = null
) {
    init {
        require(barcode.isNotBlank()) { "barcode cannot be blank." }
        require(targetQuantity.isFinite()) { "targetQuantity must be finite." }
        require(targetQuantity >= 0.0) { "targetQuantity cannot be negative." }
        reason?.let {
            require(it.isNotBlank()) { "reason cannot be blank when provided." }
        }
    }
}

data class SyncInventoryRequest(
    val items: List<SyncInventoryItem>,
    val defaultLocation: String? = null,
    val cleanupExpiredStock: Boolean = true,
    val today: LocalDate = LocalDate.now(),
    val strictMatching: Boolean = false
) {
    init {
        require(items.isNotEmpty()) { "items cannot be empty." }
        defaultLocation?.let {
            require(it.isNotBlank()) { "defaultLocation cannot be blank when provided." }
        }
    }
}

data class SyncInventoryResult(
    val processedCount: Int,
    val receivedCount: Int,
    val consumedCount: Int,
    val adjustedCount: Int,
    val expiredClearedCount: Int,
    val updatedItems: List<StockItem>,
    val movements: List<StockMovement>,
    val failures: List<String> = emptyList()
) {
    val hasFailures: Boolean
        get() = failures.isNotEmpty()

    val hasChanges: Boolean
        get() = receivedCount > 0 || consumedCount > 0 || adjustedCount > 0 || expiredClearedCount > 0
}

class SyncInventoryUseCase @Inject constructor(
    private val stockRepository: StockRepository
) {

    suspend operator fun invoke(request: SyncInventoryRequest): Result<SyncInventoryResult> {
        return runCatching {
            val failures = mutableListOf<String>()
            val updatedItems = mutableListOf<StockItem>()
            val movements = mutableListOf<StockMovement>()

            var receivedCount = 0
            var consumedCount = 0
            var adjustedCount = 0

            val expiredClearedCount = if (request.cleanupExpiredStock) {
                stockRepository.clearExpiredStock(request.today).getOrElse { error ->
                    failures += "clearExpiredStock failed: ${error.message ?: error::class.java.simpleName}"
                    0
                }
            } else {
                0
            }

            for (item in request.items) {
                val resolvedLocation = resolveLocation(item.location, request.defaultLocation)
                if (resolvedLocation == null) {
                    failures += "Missing location for barcode=${item.barcode}"
                    continue
                }

                try {
                    val current = stockRepository.getStockByBarcode(item.barcode)

                    when {
                        current == null -> {
                            if (item.targetQuantity > 0.0) {
                                val updated = stockRepository.receiveStock(
                                    barcode = item.barcode,
                                    quantity = item.targetQuantity,
                                    location = resolvedLocation,
                                    sourceDocumentId = null,
                                    reason = item.reason ?: "inventory sync: initial stock"
                                ).getOrThrow()

                                updatedItems += updated
                                receivedCount++
                            }
                        }

                        current.quantity == item.targetQuantity -> {
                            updatedItems += current

                            if (item.expiryDate != null) {
                                val refreshed = stockRepository.setStockExpiry(
                                    barcode = item.barcode,
                                    expiryDate = item.expiryDate
                                ).getOrThrow()

                                updatedItems += refreshed
                                adjustedCount++
                            } else if (request.strictMatching) {
                                val rechecked = stockRepository.adjustStock(
                                    barcode = item.barcode,
                                    quantityDelta = 0.0,
                                    reason = item.reason ?: "inventory sync: verification",
                                    location = resolvedLocation
                                ).getOrThrow()

                                updatedItems += rechecked
                                adjustedCount++
                            }
                        }

                        current.quantity < item.targetQuantity -> {
                            val delta = item.targetQuantity - current.quantity
                            val updated = stockRepository.receiveStock(
                                barcode = item.barcode,
                                quantity = delta,
                                location = resolvedLocation,
                                sourceDocumentId = null,
                                reason = item.reason ?: "inventory sync: quantity increase"
                            ).getOrThrow()

                            updatedItems += updated
                            receivedCount++
                        }

                        current.quantity > item.targetQuantity -> {
                            val delta = current.quantity - item.targetQuantity
                            val updated = stockRepository.consumeStock(
                                barcode = item.barcode,
                                quantity = delta,
                                location = resolvedLocation,
                                reason = item.reason ?: "inventory sync: quantity decrease"
                            ).getOrThrow()

                            updatedItems += updated
                            consumedCount++
                        }
                    }

                    if (item.expiryDate != null && current?.quantity != 0.0) {
                        val refreshed = stockRepository.setStockExpiry(
                            barcode = item.barcode,
                            expiryDate = item.expiryDate
                        ).getOrThrow()

                        updatedItems += refreshed
                        adjustedCount++
                    }

                    val history: List<StockMovement> = stockRepository.getStockHistory(item.barcode)
                    movements.addAll(history)

                    stockRepository.getStockByBarcode(item.barcode)?.let { latest ->
                        updatedItems += latest
                    }
                } catch (t: Throwable) {
                    failures += "Sync failed for ${item.barcode}: ${t.message ?: t::class.java.simpleName}"
                }
            }

            SyncInventoryResult(
                processedCount = request.items.size,
                receivedCount = receivedCount,
                consumedCount = consumedCount,
                adjustedCount = adjustedCount,
                expiredClearedCount = expiredClearedCount,
                updatedItems = updatedItems.distinctBy { it.normalizedBarcode },
                movements = movements.distinctBy { it.movementId },
                failures = failures
            )
        }
    }

    private fun resolveLocation(itemLocation: String?, defaultLocation: String?): String? {
        return itemLocation?.trim()?.takeIf { it.isNotBlank() }
            ?: defaultLocation?.trim()?.takeIf { it.isNotBlank() }
    }
}