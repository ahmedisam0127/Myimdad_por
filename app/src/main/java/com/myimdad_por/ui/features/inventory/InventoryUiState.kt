package com.myimdad_por.ui.features.inventory

import com.myimdad_por.domain.model.Product
import com.myimdad_por.domain.model.StockItem
import com.myimdad_por.domain.model.UnitOfMeasure
import com.myimdad_por.domain.repository.SortDirection
import java.math.BigDecimal
import java.time.LocalDate

data class InventoryUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isSubmitting: Boolean = false,

    val products: List<Product> = emptyList(),
    val stockItems: List<StockItem> = emptyList(),

    val selectedProductBarcode: String? = null,
    val selectedStockBarcode: String? = null,

    val filter: InventoryFilterState = InventoryFilterState(),
    val form: InventoryFormState = InventoryFormState(),

    val displayMode: InventoryDisplayMode = InventoryDisplayMode.BY_CATEGORY,

    val showProductDialog: Boolean = false,
    val showStockDialog: Boolean = false,
    val showDeleteConfirmDialog: Boolean = false,

    val pendingDeleteBarcode: String? = null,

    val errorMessage: String? = null,
    val successMessage: String? = null,
    val lastUpdated: LocalDate? = null
) {
    val normalizedQuery: String
        get() = filter.query.trim()

    val selectedProduct: Product?
        get() = selectedProductBarcode
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?.let { barcode ->
                products.firstOrNull { it.normalizedBarcode.equals(barcode, ignoreCase = true) }
            }

    val selectedStockItem: StockItem?
        get() = selectedStockBarcode
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?.let { barcode ->
                stockItems.firstOrNull { it.normalizedBarcode.equals(barcode, ignoreCase = true) }
            }

    val visibleProducts: List<Product>
        get() = products
            .asSequence()
            .filter { product -> product.matchesFilter(filter) }
            .sortedBy { it.effectiveName.lowercase() }
            .toList()

    val visibleStockItems: List<StockItem>
        get() = stockItems
            .asSequence()
            .filter { item -> item.matchesFilter(filter) }
            .sortedWith(buildStockComparator(filter.sortBy, filter.sortDirection))
            .toList()

    val stockDisplayItems: List<InventoryStockDisplayItem>
        get() = visibleStockItems.map { it.toDisplayItem() }

    val activeProductsCount: Int
        get() = products.count { it.isActive }

    val inactiveProductsCount: Int
        get() = products.size - activeProductsCount

    val outOfStockCount: Int
        get() = stockItems.count { it.isOutOfStock }

    val expiredItemsCount: Int
        get() = stockItems.count { it.isExpired() }

    val totalStockQuantity: Double
        get() = stockItems.sumOf { it.quantity }

    val hasContent: Boolean
        get() = products.isNotEmpty() || stockItems.isNotEmpty()

    val hasError: Boolean
        get() = !errorMessage.isNullOrBlank()

    val isEmpty: Boolean
        get() = products.isEmpty() && stockItems.isEmpty() && !isLoading && !isRefreshing

    val canSaveForm: Boolean
        get() = form.isValid
}

data class InventoryFilterState(
    val query: String = "",
    val selectedUnit: UnitOfMeasure? = null,
    val selectedCategory: UnitOfMeasure.Category? = null,
    val selectedDimension: UnitOfMeasure.Dimension? = null,
    val selectedLocation: String = "",
    val showOnlyActiveProducts: Boolean = false,
    val showOnlyInStock: Boolean = false,
    val showOnlyExpired: Boolean = false,
    val sortBy: InventorySortBy = InventorySortBy.NAME,
    val sortDirection: SortDirection = SortDirection.ASCENDING
)

data class InventoryFormState(
    val barcode: String = "",
    val name: String = "",
    val displayName: String = "",
    val description: String = "",
    val price: BigDecimal = BigDecimal.ZERO,
    val unitOfMeasure: UnitOfMeasure = UnitOfMeasure.UNIT,
    val quantity: Double = 0.0,
    val location: String = "",
    val expiryDate: LocalDate? = null,
    val isActive: Boolean = true
) {
    val normalizedBarcode: String
        get() = barcode.trim()

    val normalizedName: String
        get() = name.trim()

    val normalizedDisplayName: String
        get() = displayName.trim()

    val normalizedDescription: String
        get() = description.trim()

    val normalizedLocation: String
        get() = location.trim()

    val isNew: Boolean
        get() = normalizedBarcode.isBlank()

    val hasExpiryDate: Boolean
        get() = expiryDate != null

    val isValid: Boolean
        get() = normalizedBarcode.isNotBlank() &&
            normalizedName.isNotBlank() &&
            price >= BigDecimal.ZERO &&
            quantity >= 0.0 &&
            normalizedLocation.isNotBlank() &&
            (unitOfMeasure.isDecimalAllowed || quantity == quantity.toLong().toDouble())
}

data class InventoryStockDisplayItem(
    val barcode: String,
    val productName: String,
    val displayText: String,
    val quantityText: String,
    val unitText: String,
    val location: String,
    val expiryDate: LocalDate?,
    val isLargeUnit: Boolean,
    val isLowQuantity: Boolean
)

enum class InventoryDisplayMode {
    BY_CATEGORY,
    COMPACT
}

enum class InventorySortBy {
    NAME,
    BARCODE,
    QUANTITY,
    LOCATION,
    EXPIRY_DATE
}

private const val SMALL_QUANTITY_THRESHOLD = 30.0

private fun Product.matchesFilter(filter: InventoryFilterState): Boolean {
    if (filter.showOnlyActiveProducts && !isActive) return false

    val query = filter.query.trim()
    if (query.isNotEmpty()) {
        val q = query.lowercase()
        val matchesText =
            normalizedBarcode.lowercase().contains(q) ||
            name.lowercase().contains(q) ||
            effectiveName.lowercase().contains(q) ||
            description?.lowercase()?.contains(q) == true ||
            unitOfMeasure.displayName.lowercase().contains(q) ||
            unitOfMeasure.symbol.lowercase().contains(q)

        if (!matchesText) return false
    }

    filter.selectedUnit?.let { if (unitOfMeasure != it) return false }
    filter.selectedCategory?.let { if (unitOfMeasure.category != it) return false }
    filter.selectedDimension?.let { if (unitOfMeasure.dimension != it) return false }

    return true
}

private fun StockItem.matchesFilter(filter: InventoryFilterState): Boolean {
    if (filter.showOnlyInStock && isOutOfStock) return false
    if (filter.showOnlyExpired && !isExpired()) return false

    val selectedLocation = filter.selectedLocation.trim()
    if (selectedLocation.isNotEmpty() && !normalizedLocation.contains(selectedLocation, ignoreCase = true)) {
        return false
    }

    val query = filter.query.trim()
    if (query.isNotEmpty()) {
        val q = query.lowercase()
        val matchesText =
            normalizedBarcode.lowercase().contains(q) ||
            productName.lowercase().contains(q) ||
            effectiveName.lowercase().contains(q) ||
            normalizedLocation.lowercase().contains(q) ||
            unitOfMeasure.displayName.lowercase().contains(q) ||
            unitOfMeasure.symbol.lowercase().contains(q)

        if (!matchesText) return false
    }

    filter.selectedUnit?.let { if (unitOfMeasure != it) return false }
    filter.selectedCategory?.let { if (unitOfMeasure.category != it) return false }
    filter.selectedDimension?.let { if (unitOfMeasure.dimension != it) return false }

    return true
}

private fun buildStockComparator(
    sortBy: InventorySortBy,
    direction: SortDirection
): Comparator<StockItem> {
    val baseComparator = when (sortBy) {
        InventorySortBy.NAME ->
            compareBy<StockItem> { it.effectiveName.lowercase() }

        InventorySortBy.BARCODE ->
            compareBy<StockItem> { it.normalizedBarcode.lowercase() }

        InventorySortBy.QUANTITY ->
            compareBy<StockItem> { it.quantity }

        InventorySortBy.LOCATION ->
            compareBy<StockItem> { it.normalizedLocation.lowercase() }

        InventorySortBy.EXPIRY_DATE ->
            compareBy<StockItem> { it.expiryDate ?: LocalDate.MAX }
    }

    return if (direction.isDescending) baseComparator.reversed() else baseComparator
}

private fun StockItem.toDisplayItem(): InventoryStockDisplayItem {
    val quantityText = quantity.toInventoryText()
    val unitText = unitOfMeasure.displayName
    val isLargeUnit = unitOfMeasure.category == UnitOfMeasure.Category.LARGE
    val isLowQuantity = quantity < SMALL_QUANTITY_THRESHOLD

    val displayText = when {
        isLargeUnit -> "$quantityText $unitText ${effectiveName}".trim()
        else -> "$quantityText $unitText"
    }

    return InventoryStockDisplayItem(
        barcode = normalizedBarcode,
        productName = effectiveName,
        displayText = displayText,
        quantityText = quantityText,
        unitText = unitText,
        location = normalizedLocation,
        expiryDate = expiryDate,
        isLargeUnit = isLargeUnit,
        isLowQuantity = isLowQuantity
    )
}

private fun Double.toInventoryText(): String {
    val text = BigDecimal.valueOf(this).stripTrailingZeros().toPlainString()
    return if (text == "-0") "0" else text
}