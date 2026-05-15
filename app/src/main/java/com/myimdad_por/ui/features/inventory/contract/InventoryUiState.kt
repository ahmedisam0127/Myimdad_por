package com.myimdad_por.ui.features.inventory.contract

import com.myimdad_por.core.base.UiState
import com.myimdad_por.domain.model.Product
import com.myimdad_por.domain.model.StockItem
import com.myimdad_por.domain.model.UnitOfMeasure
import com.myimdad_por.domain.repository.SortDirection
import java.math.BigDecimal
import java.time.LocalDateTime

data class InventoryUiState(
    val stockItemsState: UiState<List<StockItem>> = UiState.Idle,
    val productsState: UiState<List<Product>> = UiState.Idle,
    val formState: InventoryFormState = InventoryFormState.initial(),
    val searchQuery: String = "",
    val selectedUnitFilter: UnitOfMeasure? = null,
    val sortDirection: SortDirection = SortDirection.ASCENDING,
    val isRefreshing: Boolean = false,
    val isProcessingAction: Boolean = false,
    val showForm: Boolean = false,
    val isMultiSelectMode: Boolean = false,
    val selectedBarcodes: Set<String> = emptySet(),
    val lastUpdatedAt: LocalDateTime? = null,
    val globalMessage: String? = null
) {
    val normalizedSearchQuery: String
        get() = searchQuery.trim()

    val hasSearchQuery: Boolean
        get() = normalizedSearchQuery.isNotEmpty()

    val hasSelectedUnitFilter: Boolean
        get() = selectedUnitFilter != null

    val hasSelectedItems: Boolean
        get() = selectedBarcodes.isNotEmpty()

    val isBusy: Boolean
        get() = isRefreshing || isProcessingAction || formState.isLoadingProduct || formState.isSubmitting

    val isLoading: Boolean
        get() = stockItemsState is UiState.Loading || productsState is UiState.Loading || isBusy

    val hasError: Boolean
        get() = stockItemsState is UiState.Error || productsState is UiState.Error || formState.errorMessage != null

    val isEmptyList: Boolean
        get() = stockItemsState is UiState.Empty

    val stockItems: List<StockItem>
        get() = when (val state = stockItemsState) {
            is UiState.Success -> state.data
            else -> emptyList()
        }

    val products: List<Product>
        get() = when (val state = productsState) {
            is UiState.Success -> state.data
            else -> emptyList()
        }

    val filteredStockItems: List<StockItem>
        get() = stockItems
            .asSequence()
            .filter { item ->
                val matchesQuery = normalizedSearchQuery.isEmpty() ||
                    item.normalizedBarcode.contains(normalizedSearchQuery, ignoreCase = true) ||
                    item.effectiveName.contains(normalizedSearchQuery, ignoreCase = true) ||
                    item.normalizedLocation.contains(normalizedSearchQuery, ignoreCase = true)

                val matchesUnit = selectedUnitFilter == null || item.unitOfMeasure == selectedUnitFilter
                matchesQuery && matchesUnit
            }
            .sortedWith(stockItemComparator(sortDirection))
            .toList()

    val visibleItemsCount: Int
        get() = filteredStockItems.size

    val totalItemsCount: Int
        get() = stockItems.size

    val totalQuantity: BigDecimal
        get() = stockItems.fold(BigDecimal.ZERO) { acc, item ->
            acc.add(BigDecimal.valueOf(item.quantity))
        }

    val selectedItemsCount: Int
        get() = selectedBarcodes.size

    val canShowEmptyState: Boolean
        get() = !isLoading && stockItems.isEmpty() && !hasSearchQuery && !hasSelectedUnitFilter

    val canShowNoSearchResultsState: Boolean
        get() = !isLoading && stockItems.isNotEmpty() && filteredStockItems.isEmpty() && hasSearchQuery

    val activeMessage: String?
        get() = globalMessage ?: formState.successMessage ?: formState.errorMessage

    fun withFormState(newFormState: InventoryFormState): InventoryUiState =
        copy(formState = newFormState)

    fun showForm(initialState: InventoryFormState = formState.copy(isEditing = false)): InventoryUiState =
        copy(
            showForm = true,
            formState = initialState,
            globalMessage = null
        )

    fun hideForm(): InventoryUiState = copy(showForm = false)

    fun startRefreshing(): InventoryUiState = copy(isRefreshing = true)

    fun stopRefreshing(): InventoryUiState = copy(isRefreshing = false)

    fun startProcessing(): InventoryUiState = copy(isProcessingAction = true, globalMessage = null)

    fun stopProcessing(): InventoryUiState = copy(isProcessingAction = false)

    fun clearSelection(): InventoryUiState = copy(
        isMultiSelectMode = false,
        selectedBarcodes = emptySet()
    )

    fun toggleSelection(barcode: String): InventoryUiState {
        val normalized = barcode.trim()
        if (normalized.isEmpty()) return this

        val updated = selectedBarcodes.toMutableSet().apply {
            if (!add(normalized)) remove(normalized)
        }

        return copy(
            selectedBarcodes = updated,
            isMultiSelectMode = updated.isNotEmpty()
        )
    }

    fun selectOnly(barcodes: Set<String>): InventoryUiState {
        val normalized = barcodes.map { it.trim() }.filter { it.isNotEmpty() }.toSet()
        return copy(
            selectedBarcodes = normalized,
            isMultiSelectMode = normalized.isNotEmpty()
        )
    }

    fun updateSearchQuery(query: String): InventoryUiState = copy(searchQuery = query)

    fun updateUnitFilter(unit: UnitOfMeasure?): InventoryUiState = copy(selectedUnitFilter = unit)

    fun updateSortDirection(direction: SortDirection): InventoryUiState = copy(sortDirection = direction)

    fun markMessage(message: String?): InventoryUiState = copy(globalMessage = message)

    fun clearMessage(): InventoryUiState = copy(globalMessage = null)

    fun onStockItemsLoading(): InventoryUiState = copy(stockItemsState = UiState.Loading)

    fun onStockItemsEmpty(): InventoryUiState = copy(stockItemsState = UiState.Empty)

    fun onStockItemsSuccess(items: List<StockItem>): InventoryUiState =
        copy(stockItemsState = if (items.isEmpty()) UiState.Empty else UiState.Success(items))

    fun onStockItemsError(message: String, throwable: Throwable? = null, code: Int? = null): InventoryUiState =
        copy(stockItemsState = UiState.Error(message = message, throwable = throwable, code = code))

    fun onProductsLoading(): InventoryUiState = copy(productsState = UiState.Loading)

    fun onProductsEmpty(): InventoryUiState = copy(productsState = UiState.Empty)

    fun onProductsSuccess(items: List<Product>): InventoryUiState =
        copy(productsState = if (items.isEmpty()) UiState.Empty else UiState.Success(items))

    fun onProductsError(message: String, throwable: Throwable? = null, code: Int? = null): InventoryUiState =
        copy(productsState = UiState.Error(message = message, throwable = throwable, code = code))

    fun resetFilters(): InventoryUiState = copy(
        searchQuery = "",
        selectedUnitFilter = null,
        sortDirection = SortDirection.ASCENDING
    )

    fun resetAll(): InventoryUiState = initial()

    companion object {
        fun initial(): InventoryUiState = InventoryUiState()

        private fun stockItemComparator(direction: SortDirection): Comparator<StockItem> {
            val baseComparator = compareBy<StockItem> { it.effectiveName.lowercase() }
                .thenBy { it.normalizedBarcode.lowercase() }
                .thenBy { it.normalizedLocation.lowercase() }

            return if (direction.isAscending) baseComparator else baseComparator.reversed()
        }
    }
}