package com.myimdad_por.ui.features.inventory

import com.myimdad_por.core.base.BaseViewModel
import com.myimdad_por.core.base.UiEvent
import com.myimdad_por.core.dispatchers.AppDispatchers
import com.myimdad_por.domain.model.Product
import com.myimdad_por.domain.model.StockItem
import com.myimdad_por.domain.model.UnitOfMeasure
import com.myimdad_por.domain.repository.SortDirection
import com.myimdad_por.domain.usecase.GetInventoryUseCase
import com.myimdad_por.domain.usecase.InventoryQuery
import java.math.BigDecimal
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.withContext
import kotlin.math.abs

class InventoryViewModel @Inject constructor(
    private val getInventoryUseCase: GetInventoryUseCase,
    dispatchers: AppDispatchers
) : BaseViewModel<InventoryUiState>(dispatchers) {

    init {
        setSuccess(InventoryUiState(isLoading = true))
        loadInventory(showRefreshing = false)
    }

    fun onEvent(event: InventoryUiEvent) {
        when (event) {
            InventoryUiEvent.LoadInventory -> loadInventory(showRefreshing = false)
            InventoryUiEvent.RefreshInventory -> loadInventory(showRefreshing = true)
            InventoryUiEvent.Retry -> loadInventory(showRefreshing = false)

            is InventoryUiEvent.SearchQueryChanged -> updateFilter {
                it.copy(query = event.query)
            }

            is InventoryUiEvent.UnitFilterChanged -> updateFilter {
                it.copy(selectedUnit = event.unit)
            }

            is InventoryUiEvent.CategoryFilterChanged -> updateFilter {
                it.copy(selectedCategory = event.category)
            }

            is InventoryUiEvent.DimensionFilterChanged -> updateFilter {
                it.copy(selectedDimension = event.dimension)
            }

            is InventoryUiEvent.LocationFilterChanged -> updateFilter {
                it.copy(selectedLocation = event.location)
            }

            is InventoryUiEvent.ShowOnlyActiveProductsChanged -> updateFilter {
                it.copy(showOnlyActiveProducts = event.enabled)
            }

            is InventoryUiEvent.ShowOnlyInStockChanged -> updateFilter {
                it.copy(showOnlyInStock = event.enabled)
            }

            is InventoryUiEvent.ShowOnlyExpiredChanged -> updateFilter {
                it.copy(showOnlyExpired = event.enabled)
            }

            is InventoryUiEvent.SortChanged -> updateFilter {
                it.copy(
                    sortBy = event.sortBy,
                    sortDirection = event.direction
                )
            }

            is InventoryUiEvent.DisplayModeChanged -> mutateState {
                it.copy(displayMode = event.mode)
            }

            is InventoryUiEvent.ProductSelected -> mutateState {
                it.copy(
                    selectedProductBarcode = event.barcode.trim(),
                    selectedStockBarcode = null
                )
            }

            is InventoryUiEvent.StockItemSelected -> mutateState {
                it.copy(
                    selectedStockBarcode = event.barcode.trim(),
                    selectedProductBarcode = null
                )
            }

            InventoryUiEvent.ClearSelection -> mutateState {
                it.copy(
                    selectedProductBarcode = null,
                    selectedStockBarcode = null
                )
            }

            InventoryUiEvent.OpenProductDialog -> mutateState {
                it.copy(
                    showProductDialog = true,
                    showStockDialog = false,
                    selectedProductBarcode = null,
                    selectedStockBarcode = null,
                    form = InventoryFormState(
                        barcode = "",
                        name = "",
                        displayName = "",
                        description = "",
                        price = BigDecimal.ZERO,
                        quantity = 0.0,
                        location = "",
                        expiryDate = null,
                        unitOfMeasure = UnitOfMeasure.UNIT,
                        isActive = true
                    ),
                    errorMessage = null,
                    successMessage = null
                )
            }

            is InventoryUiEvent.OpenProductDialogForEdit -> openProductDialogForEdit(event.barcode)

            InventoryUiEvent.CloseProductDialog -> mutateState {
                it.copy(showProductDialog = false)
            }

            InventoryUiEvent.OpenStockDialog -> mutateState {
                it.copy(
                    showStockDialog = true,
                    showProductDialog = false,
                    selectedProductBarcode = null,
                    selectedStockBarcode = null,
                    form = InventoryFormState(
                        barcode = "",
                        name = "",
                        displayName = "",
                        description = "",
                        price = BigDecimal.ZERO,
                        quantity = 0.0,
                        location = "",
                        expiryDate = null,
                        unitOfMeasure = UnitOfMeasure.UNIT,
                        isActive = true
                    ),
                    errorMessage = null,
                    successMessage = null
                )
            }

            is InventoryUiEvent.OpenStockDialogForEdit -> openStockDialogForEdit(event.barcode)

            InventoryUiEvent.CloseStockDialog -> mutateState {
                it.copy(showStockDialog = false)
            }

            is InventoryUiEvent.BarcodeChanged -> mutateForm {
                it.copy(barcode = event.value)
            }

            is InventoryUiEvent.NameChanged -> mutateForm {
                it.copy(name = event.value)
            }

            is InventoryUiEvent.DisplayNameChanged -> mutateForm {
                it.copy(displayName = event.value)
            }

            is InventoryUiEvent.DescriptionChanged -> mutateForm {
                it.copy(description = event.value)
            }

            is InventoryUiEvent.PriceChanged -> updatePrice(event.value)

            is InventoryUiEvent.QuantityChanged -> updateQuantity(event.value)

            is InventoryUiEvent.LocationChanged -> mutateForm {
                it.copy(location = event.value)
            }

            is InventoryUiEvent.ExpiryDateChanged -> updateExpiryDate(event.value)

            is InventoryUiEvent.UnitOfMeasureChanged -> mutateForm {
                it.copy(unitOfMeasure = event.value)
            }

            is InventoryUiEvent.ProductActiveChanged -> mutateForm {
                it.copy(isActive = event.isActive)
            }

            InventoryUiEvent.SaveProduct -> saveProduct()
            InventoryUiEvent.SaveStockItem -> saveStockItem()

            is InventoryUiEvent.DeleteProductRequested -> mutateState {
                it.copy(
                    showDeleteConfirmDialog = true,
                    pendingDeleteBarcode = event.barcode.trim()
                )
            }

            is InventoryUiEvent.DeleteStockItemRequested -> mutateState {
                it.copy(
                    showDeleteConfirmDialog = true,
                    pendingDeleteBarcode = event.barcode.trim()
                )
            }

            is InventoryUiEvent.ConfirmDeleteProduct -> confirmDeleteProduct(event.barcode)
            is InventoryUiEvent.ConfirmDeleteStockItem -> confirmDeleteStockItem(event.barcode)

            InventoryUiEvent.CancelDelete -> mutateState {
                it.copy(
                    showDeleteConfirmDialog = false,
                    pendingDeleteBarcode = null
                )
            }

            is InventoryUiEvent.ShowMessageEvent -> sendEvent(UiEvent.ShowMessage(event.message))
            is InventoryUiEvent.ShowErrorEvent -> sendEvent(
                UiEvent.ShowError(
                    message = event.message,
                    throwable = event.throwable
                )
            )

            is InventoryUiEvent.NavigateToProductDetails -> sendEvent(
                UiEvent.NavigateTo(route = "inventory/product/${event.barcode.trim()}")
            )

            is InventoryUiEvent.NavigateToStockDetails -> sendEvent(
                UiEvent.NavigateTo(route = "inventory/stock/${event.barcode.trim()}")
            )

            InventoryUiEvent.HideKeyboard -> sendEvent(UiEvent.HideKeyboard)
        }
    }

    private fun loadInventory(showRefreshing: Boolean) {
        mutateState {
            it.copy(
                isLoading = !showRefreshing,
                isRefreshing = showRefreshing,
                errorMessage = null,
                successMessage = null
            )
        }

        launch(
            dispatcher = dispatchers.main,
            onError = { throwable ->
                mutateState {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        errorMessage = throwable.message ?: "تعذر تحميل المخزون",
                        successMessage = null
                    )
                }
                sendEvent(
                    UiEvent.ShowError(
                        message = throwable.message ?: "تعذر تحميل المخزون",
                        throwable = throwable
                    )
                )
            }
        ) {
            val items = withContext(dispatchers.io) {
                getInventoryUseCase(
                    InventoryQuery(
                        includeOutOfStock = true
                    )
                )
            }

            val derivedProducts = items
                .map { it.toFallbackProduct() }
                .distinctBy { it.normalizedBarcode }

            mutateState {
                it.copy(
                    isLoading = false,
                    isRefreshing = false,
                    stockItems = items,
                    products = derivedProducts,
                    lastUpdated = LocalDate.now(),
                    errorMessage = null,
                    successMessage = if (showRefreshing) "تم تحديث المخزون" else null
                )
            }
        }
    }

    private fun openProductDialogForEdit(barcode: String) {
        val normalized = barcode.trim()
        val state = currentInventoryState()

        val product = state.products.firstOrNull {
            it.normalizedBarcode.equals(normalized, ignoreCase = true)
        } ?: state.stockItems.firstOrNull {
            it.normalizedBarcode.equals(normalized, ignoreCase = true)
        }?.toFallbackProduct()

        if (product == null) {
            sendEvent(UiEvent.ShowError("المنتج غير موجود"))
            return
        }

        mutateState {
            it.copy(
                showProductDialog = true,
                showStockDialog = false,
                selectedProductBarcode = product.normalizedBarcode,
                selectedStockBarcode = null,
                form = InventoryFormState(
                    barcode = product.barcode,
                    name = product.name,
                    displayName = product.displayName.orEmpty(),
                    description = product.description.orEmpty(),
                    price = product.price,
                    quantity = 0.0,
                    location = "",
                    expiryDate = null,
                    unitOfMeasure = product.unitOfMeasure,
                    isActive = product.isActive
                ),
                errorMessage = null,
                successMessage = null
            )
        }
    }

    private fun openStockDialogForEdit(barcode: String) {
        val normalized = barcode.trim()
        val stockItem = currentInventoryState().stockItems.firstOrNull {
            it.normalizedBarcode.equals(normalized, ignoreCase = true)
        }

        if (stockItem == null) {
            sendEvent(UiEvent.ShowError("عنصر المخزون غير موجود"))
            return
        }

        mutateState {
            it.copy(
                showStockDialog = true,
                showProductDialog = false,
                selectedStockBarcode = stockItem.normalizedBarcode,
                selectedProductBarcode = null,
                form = InventoryFormState(
                    barcode = stockItem.productBarcode,
                    name = stockItem.productName,
                    displayName = stockItem.displayName.orEmpty(),
                    description = "",
                    price = BigDecimal.ZERO,
                    quantity = stockItem.quantity,
                    location = stockItem.location,
                    expiryDate = stockItem.expiryDate,
                    unitOfMeasure = stockItem.unitOfMeasure,
                    isActive = true
                ),
                errorMessage = null,
                successMessage = null
            )
        }
    }

    private fun saveProduct() {
        val state = currentInventoryState()
        val form = state.form

        if (!isProductFormValid(form)) {
            showInvalidFormMessage("تحقق من بيانات المنتج قبل الحفظ")
            return
        }

        val product = Product(
            barcode = form.barcode.trim(),
            name = form.name.trim(),
            price = form.price,
            unitOfMeasure = form.unitOfMeasure,
            displayName = form.displayName.trim().takeUnless { it.isBlank() },
            description = form.description.trim().takeUnless { it.isBlank() },
            isActive = form.isActive
        )

        mutateState {
            it.copy(
                products = upsertProduct(it.products, product),
                showProductDialog = false,
                selectedProductBarcode = product.normalizedBarcode,
                form = InventoryFormState(
                    barcode = "",
                    name = "",
                    displayName = "",
                    description = "",
                    price = BigDecimal.ZERO,
                    quantity = 0.0,
                    location = "",
                    expiryDate = null,
                    unitOfMeasure = UnitOfMeasure.UNIT,
                    isActive = true
                ),
                errorMessage = null,
                successMessage = "تم حفظ المنتج بنجاح"
            )
        }

        sendEvent(UiEvent.HideKeyboard)
        sendEvent(UiEvent.ShowMessage("تم حفظ المنتج بنجاح"))
    }

    private fun saveStockItem() {
        val state = currentInventoryState()
        val form = state.form

        if (!isStockFormValid(form)) {
            showInvalidFormMessage("تحقق من بيانات المخزون قبل الحفظ")
            return
        }

        val stockItem = StockItem(
            productBarcode = form.barcode.trim(),
            productName = form.name.trim(),
            quantity = form.quantity,
            location = form.location.trim(),
            unitOfMeasure = form.unitOfMeasure,
            displayName = form.displayName.trim().takeUnless { it.isBlank() },
            expiryDate = form.expiryDate
        )

        mutateState {
            it.copy(
                stockItems = upsertStockItem(it.stockItems, stockItem),
                products = upsertProduct(
                    it.products,
                    stockItem.toFallbackProduct()
                ),
                showStockDialog = false,
                selectedStockBarcode = stockItem.normalizedBarcode,
                form = InventoryFormState(
                    barcode = "",
                    name = "",
                    displayName = "",
                    description = "",
                    price = BigDecimal.ZERO,
                    quantity = 0.0,
                    location = "",
                    expiryDate = null,
                    unitOfMeasure = UnitOfMeasure.UNIT,
                    isActive = true
                ),
                errorMessage = null,
                successMessage = "تم حفظ عنصر المخزون بنجاح"
            )
        }

        sendEvent(UiEvent.HideKeyboard)
        sendEvent(UiEvent.ShowMessage("تم حفظ عنصر المخزون بنجاح"))
    }

    private fun confirmDeleteProduct(barcode: String) {
        val normalized = barcode.trim()

        mutateState { state ->
            state.copy(
                products = state.products.filterNot {
                    it.normalizedBarcode.equals(normalized, ignoreCase = true)
                },
                showDeleteConfirmDialog = false,
                pendingDeleteBarcode = null,
                selectedProductBarcode = state.selectedProductBarcode
                    ?.takeUnless { it.equals(normalized, ignoreCase = true) },
                successMessage = "تم حذف المنتج"
            )
        }

        sendEvent(UiEvent.ShowMessage("تم حذف المنتج"))
    }

    private fun confirmDeleteStockItem(barcode: String) {
        val normalized = barcode.trim()

        mutateState { state ->
            state.copy(
                stockItems = state.stockItems.filterNot {
                    it.normalizedBarcode.equals(normalized, ignoreCase = true)
                },
                products = state.products.filterNot {
                    it.normalizedBarcode.equals(normalized, ignoreCase = true)
                },
                showDeleteConfirmDialog = false,
                pendingDeleteBarcode = null,
                selectedStockBarcode = state.selectedStockBarcode
                    ?.takeUnless { it.equals(normalized, ignoreCase = true) },
                successMessage = "تم حذف عنصر المخزون"
            )
        }

        sendEvent(UiEvent.ShowMessage("تم حذف عنصر المخزون"))
    }

    private fun updateFilter(reducer: (InventoryFilterState) -> InventoryFilterState) {
        mutateState { state ->
            state.copy(filter = reducer(state.filter))
        }
    }

    private fun mutateForm(reducer: (InventoryFormState) -> InventoryFormState) {
        mutateState { state ->
            state.copy(
                form = reducer(state.form),
                errorMessage = null
            )
        }
    }

    private fun updatePrice(value: String) {
        val normalized = value.trim()

        if (normalized.isBlank()) {
            mutateForm { it.copy(price = BigDecimal.ZERO) }
            return
        }

        val parsed = normalized.toBigDecimalOrNull()
        if (parsed == null) {
            mutateState { it.copy(errorMessage = "قيمة السعر غير صحيحة") }
            return
        }

        mutateForm { it.copy(price = parsed) }
    }

    private fun updateQuantity(value: String) {
        val normalized = value.trim()

        if (normalized.isBlank()) {
            mutateForm { it.copy(quantity = 0.0) }
            return
        }

        val parsed = normalized.toDoubleOrNull()
        if (parsed == null) {
            mutateState { it.copy(errorMessage = "قيمة الكمية غير صحيحة") }
            return
        }

        mutateForm { it.copy(quantity = parsed) }
    }

    private fun updateExpiryDate(value: String) {
        val normalized = value.trim()

        if (normalized.isBlank()) {
            mutateForm { it.copy(expiryDate = null) }
            return
        }

        val parsed = runCatching { LocalDate.parse(normalized) }.getOrNull()
        if (parsed == null) {
            mutateState { it.copy(errorMessage = "صيغة تاريخ الانتهاء غير صحيحة") }
            return
        }

        mutateForm { it.copy(expiryDate = parsed) }
    }

    private fun showInvalidFormMessage(message: String) {
        mutateState {
            it.copy(
                errorMessage = message,
                successMessage = null
            )
        }
        sendEvent(UiEvent.ShowError(message))
    }

    private fun isProductFormValid(form: InventoryFormState): Boolean {
        return form.barcode.trim().isNotBlank() &&
            form.name.trim().isNotBlank() &&
            form.price >= BigDecimal.ZERO
    }

    private fun isStockFormValid(form: InventoryFormState): Boolean {
        val barcodeValid = form.barcode.trim().isNotBlank()
        val nameValid = form.name.trim().isNotBlank()
        val locationValid = form.location.trim().isNotBlank()
        val quantityValid = form.quantity.isFinite() && form.quantity >= 0.0
        val unitValid = form.unitOfMeasure.isDecimalAllowed || isWholeNumber(form.quantity)

        return barcodeValid && nameValid && locationValid && quantityValid && unitValid
    }

    private fun upsertProduct(
        current: List<Product>,
        updated: Product
    ): List<Product> {
        val filtered = current.filterNot {
            it.normalizedBarcode.equals(updated.normalizedBarcode, ignoreCase = true)
        }
        return (filtered + updated).sortedBy { it.effectiveName.lowercase() }
    }

    private fun upsertStockItem(
        current: List<StockItem>,
        updated: StockItem
    ): List<StockItem> {
        val filtered = current.filterNot { item ->
            item.normalizedBarcode.equals(updated.normalizedBarcode, ignoreCase = true) &&
                item.normalizedLocation.equals(updated.normalizedLocation, ignoreCase = true)
        }
        return (filtered + updated).sortedWith(
            compareBy<StockItem> { it.effectiveName.lowercase() }
                .thenBy { it.normalizedLocation.lowercase() }
        )
    }

    private fun currentInventoryState(): InventoryUiState {
        return currentData ?: InventoryUiState()
    }

    private fun mutateState(reducer: (InventoryUiState) -> InventoryUiState) {
        val state = currentInventoryState()
        val newState = reducer(state)
        setSuccess(newState)
    }

    private fun StockItem.toFallbackProduct(): Product {
        return Product(
            barcode = productBarcode,
            name = productName,
            price = BigDecimal.ZERO,
            unitOfMeasure = unitOfMeasure,
            displayName = displayName,
            description = null,
            isActive = true
        )
    }

    private fun isWholeNumber(value: Double): Boolean {
        return abs(value - value.toLong().toDouble()) < 1e-9
    }
}