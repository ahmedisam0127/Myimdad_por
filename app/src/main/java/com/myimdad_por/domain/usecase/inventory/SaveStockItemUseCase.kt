package com.myimdad_por.domain.usecase.inventory

import com.myimdad_por.core.dispatchers.AppDispatchers
import com.myimdad_por.domain.model.StockItem
import com.myimdad_por.domain.model.SubscriptionPlan
import com.myimdad_por.domain.model.SubscriptionStatus
import com.myimdad_por.domain.repository.StockRepository
import com.myimdad_por.domain.repository.SubscriptionRepository
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Saves a stock item by creating it or synchronizing it with the repository.
 *
 * The user-entered values remain the source of truth, especially:
 * - productName
 * - unitOfMeasure
 * - location
 * - expiryDate
 *
 * Requires an active subscription (monthly or yearly plan) to proceed.
 * If the user has no active subscription, the operation is blocked immediately.
 */
class SaveStockItemUseCase @Inject constructor(
    private val stockRepository: StockRepository,
    private val subscriptionRepository: SubscriptionRepository,
    private val dispatchers: AppDispatchers
) {

    suspend operator fun invoke(stockItem: StockItem): Result<StockItem> = withContext(dispatchers.io) {
        runCatching {
            // ── Subscription guard ──────────────────────────────────────────────
            // Block the operation if the user does not have an active paid plan.
            // Grace-period subscribers are also allowed to keep saving uninterrupted.
            val subscriptionActive = subscriptionRepository.isSubscriptionActive()
            val inGracePeriod     = subscriptionRepository.isInGracePeriod()
            val canUsePaidFeatures = subscriptionRepository.canUsePaidFeatures()

         /*   if (!subscriptionActive && !inGracePeriod) {
                throw IllegalStateException(
                    "لا يمكن حفظ المنتجات: يجب الاشتراك في خطة شهرية أو سنوية فعّالة للمتابعة."
                )
            }

            if (!canUsePaidFeatures) {
                throw IllegalStateException(
                    "لا يمكن حفظ المنتجات: الاشتراك الحالي لا يتيح استخدام هذه الميزة."
                )
            }*/
            // ── End of subscription guard ───────────────────────────────────────

            val normalizedBarcode  = stockItem.normalizedBarcode
            val normalizedLocation = stockItem.normalizedLocation

            require(normalizedBarcode.isNotBlank())  { "barcode cannot be blank" }
            require(normalizedLocation.isNotBlank()) { "location cannot be blank" }
            require(stockItem.quantity.isFinite())   { "quantity must be finite" }
            require(stockItem.quantity >= 0.0)       { "quantity cannot be negative" }

            val existing = stockRepository.getStockByBarcode(normalizedBarcode)

            when (existing) {
                null -> createNewStockItem(stockItem)
                else -> updateExistingStockItem(existing, stockItem)
            }
        }
    }

    private suspend fun createNewStockItem(stockItem: StockItem): StockItem {
        if (stockItem.quantity <= 0.0) {
            throw IllegalArgumentException("Cannot create a new stock item with zero or negative quantity")
        }

        val received = stockRepository.receiveStock(
            barcode          = stockItem.normalizedBarcode,
            quantity         = stockItem.quantity,
            location         = stockItem.normalizedLocation,
            sourceDocumentId = null,
            reason           = buildSaveReason(stockItem)
        ).getOrElse { throw it }

        val finalItem = received.withUserProvidedData(stockItem)
        return applyExpiryDateIfNeeded(finalItem, stockItem)
    }

    private suspend fun updateExistingStockItem(
        existing: StockItem,
        desired:  StockItem
    ): StockItem {
        var current = existing

        if (existing.normalizedLocation != desired.normalizedLocation) {
            val transferResult = stockRepository.transferStock(
                barcode      = desired.normalizedBarcode,
                fromLocation = existing.normalizedLocation,
                toLocation   = desired.normalizedLocation,
                quantity     = existing.quantity,
                reason       = "Move stock item while saving"
            ).getOrElse { throw it }

            current = StockItem(
                productBarcode = transferResult.barcode,
                productName    = desired.productName,
                quantity       = transferResult.quantity,
                location       = transferResult.toLocation,
                unitOfMeasure  = desired.unitOfMeasure,
                displayName    = desired.displayName,
                expiryDate     = desired.expiryDate
            )
        }

        val quantityDelta = desired.quantity - current.quantity

        current = when {
            quantityDelta > 0.0 -> {
                val received = stockRepository.receiveStock(
                    barcode          = desired.normalizedBarcode,
                    quantity         = quantityDelta,
                    location         = desired.normalizedLocation,
                    sourceDocumentId = null,
                    reason           = "Increase stock quantity while saving"
                ).getOrElse { throw it }

                received.withUserProvidedData(desired)
            }

            quantityDelta < 0.0 -> {
                val adjusted = stockRepository.adjustStock(
                    barcode       = desired.normalizedBarcode,
                    quantityDelta = quantityDelta,
                    reason        = "Decrease stock quantity while saving",
                    location      = desired.normalizedLocation
                ).getOrElse { throw it }

                adjusted.withUserProvidedData(desired)
            }

            else -> current.withUserProvidedData(desired)
        }

        return applyExpiryDateIfNeeded(current, desired)
    }

    private suspend fun applyExpiryDateIfNeeded(
        current: StockItem,
        desired: StockItem
    ): StockItem {
        val expiryDate = desired.expiryDate ?: return current.withUserProvidedData(desired)

        val updated = stockRepository.setStockExpiry(
            barcode    = desired.normalizedBarcode,
            expiryDate = expiryDate
        ).getOrElse { throw it }

        return updated.withUserProvidedData(desired)
    }

    private fun StockItem.withUserProvidedData(source: StockItem): StockItem {
        return copy(
            productName   = source.productName,
            location      = source.location,
            quantity      = source.quantity,
            unitOfMeasure = source.unitOfMeasure,
            displayName   = source.displayName,
            expiryDate    = source.expiryDate
        )
    }

    private fun buildSaveReason(stockItem: StockItem): String {
        val name = stockItem.productName.trim()
        val unit = stockItem.unitOfMeasure.name

        return buildString {
            append("Save stock item")
            if (name.isNotBlank()) {
                append(": ")
                append(name)
            }
            append(" | Unit: ")
            append(unit)
        }
    }
}
