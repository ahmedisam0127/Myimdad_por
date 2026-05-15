package com.myimdad_por.ui.features.inventory.contract

import com.myimdad_por.core.base.UiEvent
import com.myimdad_por.domain.model.Product
import com.myimdad_por.domain.model.StockItem
import com.myimdad_por.domain.model.UnitOfMeasure
import com.myimdad_por.domain.repository.SortDirection
import java.time.LocalDate

/**
 * All user and screen actions for the inventory feature.
 *
 * The naming is intentionally explicit to make the ViewModel contract easy
 * to read and maintain in larger screens.
 */
sealed interface InventoryUiEvent : UiEvent {

    data object LoadInventory : InventoryUiEvent
    data object LoadProducts : InventoryUiEvent
    data object Refresh : InventoryUiEvent
    data object Retry : InventoryUiEvent

    data class SearchQueryChanged(
        val query: String
    ) : InventoryUiEvent

    data class SortDirectionChanged(
        val direction: SortDirection
    ) : InventoryUiEvent

    data class UnitFilterChanged(
        val unit: UnitOfMeasure?
    ) : InventoryUiEvent

    data object ClearFilters : InventoryUiEvent

    data object ShowForm : InventoryUiEvent
    data object HideForm : InventoryUiEvent
    data object ClearForm : InventoryUiEvent

    data class BarcodeChanged(
        val barcode: String
    ) : InventoryUiEvent

    data class ProductNameChanged(
        val name: String
    ) : InventoryUiEvent

    data class DescriptionChanged(
        val description: String
    ) : InventoryUiEvent

    data class LocationChanged(
        val location: String
    ) : InventoryUiEvent

    data class QuantityChanged(
        val quantity: String
    ) : InventoryUiEvent

    data class UnitOfMeasureChanged(
        val unitOfMeasure: UnitOfMeasure
    ) : InventoryUiEvent

    data class ExpiryDateChanged(
        val expiryDate: LocalDate?
    ) : InventoryUiEvent

    data class ProductSelected(
        val product: Product
    ) : InventoryUiEvent

    data class StockItemSelected(
        val stockItem: StockItem
    ) : InventoryUiEvent

    data class BarcodeResolved(
        val barcode: String
    ) : InventoryUiEvent

    data object SubmitForm : InventoryUiEvent
    data object SaveForm : InventoryUiEvent
    data object DeleteSelected : InventoryUiEvent
    data class DeleteStockItem(
        val barcode: String
    ) : InventoryUiEvent

    data class ToggleSelection(
        val barcode: String
    ) : InventoryUiEvent

    data class SelectOnly(
        val barcodes: Set<String>
    ) : InventoryUiEvent

    data object ClearSelection : InventoryUiEvent
    data object StartMultiSelect : InventoryUiEvent
    data object StopMultiSelect : InventoryUiEvent

    data object ClearMessage : InventoryUiEvent
    data class ShowMessage(
        val message: String
    ) : InventoryUiEvent

    data class ShowError(
        val message: String,
        val throwable: Throwable? = null
    ) : InventoryUiEvent
}
