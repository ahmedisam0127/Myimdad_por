package com.myimdad_por.ui.features.inventory

import com.myimdad_por.core.base.UiEvent
import com.myimdad_por.domain.model.UnitOfMeasure

sealed interface InventoryUiEvent : UiEvent {

    data object LoadInventory : InventoryUiEvent
    data object RefreshInventory : InventoryUiEvent
    data object Retry : InventoryUiEvent

    data class SearchQueryChanged(
        val query: String
    ) : InventoryUiEvent

    data class UnitFilterChanged(
        val unit: UnitOfMeasure?
    ) : InventoryUiEvent

    data class CategoryFilterChanged(
        val category: UnitOfMeasure.Category?
    ) : InventoryUiEvent

    data class DimensionFilterChanged(
        val dimension: UnitOfMeasure.Dimension?
    ) : InventoryUiEvent

    data class LocationFilterChanged(
        val location: String
    ) : InventoryUiEvent

    data class ShowOnlyActiveProductsChanged(
        val enabled: Boolean
    ) : InventoryUiEvent

    data class ShowOnlyInStockChanged(
        val enabled: Boolean
    ) : InventoryUiEvent

    data class ShowOnlyExpiredChanged(
        val enabled: Boolean
    ) : InventoryUiEvent

    data class SortChanged(
        val sortBy: InventorySortBy,
        val direction: com.myimdad_por.domain.repository.SortDirection
    ) : InventoryUiEvent

    data class DisplayModeChanged(
        val mode: InventoryDisplayMode
    ) : InventoryUiEvent

    data class ProductSelected(
        val barcode: String
    ) : InventoryUiEvent

    data class StockItemSelected(
        val barcode: String
    ) : InventoryUiEvent

    data object ClearSelection : InventoryUiEvent

    data object OpenProductDialog : InventoryUiEvent
    data class OpenProductDialogForEdit(
        val barcode: String
    ) : InventoryUiEvent
    data object CloseProductDialog : InventoryUiEvent

    data object OpenStockDialog : InventoryUiEvent
    data class OpenStockDialogForEdit(
        val barcode: String
    ) : InventoryUiEvent
    data object CloseStockDialog : InventoryUiEvent

    data class BarcodeChanged(
        val value: String
    ) : InventoryUiEvent

    data class NameChanged(
        val value: String
    ) : InventoryUiEvent

    data class DisplayNameChanged(
        val value: String
    ) : InventoryUiEvent

    data class DescriptionChanged(
        val value: String
    ) : InventoryUiEvent

    data class PriceChanged(
        val value: String
    ) : InventoryUiEvent

    data class QuantityChanged(
        val value: String
    ) : InventoryUiEvent

    data class LocationChanged(
        val value: String
    ) : InventoryUiEvent

    data class ExpiryDateChanged(
        val value: String
    ) : InventoryUiEvent

    data class UnitOfMeasureChanged(
        val value: UnitOfMeasure
    ) : InventoryUiEvent

    data class ProductActiveChanged(
        val isActive: Boolean
    ) : InventoryUiEvent

    data object SaveProduct : InventoryUiEvent
    data object SaveStockItem : InventoryUiEvent

    data class DeleteProductRequested(
        val barcode: String
    ) : InventoryUiEvent

    data class DeleteStockItemRequested(
        val barcode: String
    ) : InventoryUiEvent

    data class ConfirmDeleteProduct(
        val barcode: String
    ) : InventoryUiEvent

    data class ConfirmDeleteStockItem(
        val barcode: String
    ) : InventoryUiEvent

    data object CancelDelete : InventoryUiEvent

    data class ShowMessageEvent(
        val message: String
    ) : InventoryUiEvent

    data class ShowErrorEvent(
        val message: String,
        val throwable: Throwable? = null
    ) : InventoryUiEvent

    data class NavigateToProductDetails(
        val barcode: String
    ) : InventoryUiEvent

    data class NavigateToStockDetails(
        val barcode: String
    ) : InventoryUiEvent

    data object HideKeyboard : InventoryUiEvent
}