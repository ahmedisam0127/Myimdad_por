package com.myimdad_por.domain.usecase.inventory

import com.myimdad_por.core.dispatchers.AppDispatchers
import com.myimdad_por.domain.repository.StockRepository
import kotlinx.coroutines.withContext
import javax.inject.Inject
/**
 * Deletes a single stock item safely by barcode.
 */
class DeleteStockItemUseCase @Inject constructor(
    private val stockRepository: StockRepository,
    private val dispatchers: AppDispatchers
) {
    suspend operator fun invoke(barcode: String): Result<Unit> = withContext(dispatchers.io) {
        runCatching {
            val normalizedBarcode = barcode.trim()
            require(normalizedBarcode.isNotEmpty()) { "barcode cannot be blank" }
            require(normalizedBarcode.length >= 4) { "barcode is too short" }

            val existing = stockRepository.getStockByBarcode(normalizedBarcode)
                ?: throw NoSuchElementException("Stock item not found for barcode: $normalizedBarcode")

            stockRepository.deleteStockItem(existing.normalizedBarcode).getOrElse { throw it }
        }
    }
}
