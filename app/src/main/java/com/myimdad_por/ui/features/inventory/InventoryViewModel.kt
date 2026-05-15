package com.myimdad_por.ui.features.inventory
import com.myimdad_por.domain.usecase.Product.GetProductsUseCase

import androidx.lifecycle.viewModelScope
import com.myimdad_por.core.base.BaseViewModel
import com.myimdad_por.core.base.UiState
import com.myimdad_por.core.dispatchers.AppDispatchers
import com.myimdad_por.domain.model.Product
import com.myimdad_por.domain.model.StockItem
import com.myimdad_por.domain.repository.SortDirection
import com.myimdad_por.domain.usecase.GetInventoryUseCase
import com.myimdad_por.domain.usecase.InventoryQuery
import com.myimdad_por.domain.usecase.inventory.DeleteStockItemUseCase
import com.myimdad_por.domain.usecase.inventory.SaveStockItemUseCase
import com.myimdad_por.ui.features.inventory.contract.InventoryFormState
import com.myimdad_por.ui.features.inventory.contract.InventoryUiEffect
import com.myimdad_por.ui.features.inventory.contract.InventoryUiEvent
import com.myimdad_por.ui.features.inventory.contract.InventoryUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class InventoryViewModel @Inject constructor(
    private val getInventoryUseCase: GetInventoryUseCase,
    private val saveStockItemUseCase: SaveStockItemUseCase,
    private val getProductsUseCase: GetProductsUseCase, 
    private val deleteStockItemUseCase:
   DeleteStockItemUseCase,
    dispatchers: AppDispatchers
) : BaseViewModel<List<StockItem>>(dispatchers) {

    private val _screenState = MutableStateFlow(InventoryUiState.initial())
    val screenState: StateFlow<InventoryUiState> = _screenState.asStateFlow()

    private val _effects = MutableSharedFlow<InventoryUiEffect>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val effects: SharedFlow<InventoryUiEffect> = _effects.asSharedFlow()

    private var observeJob: Job? = null
    private var barcodeResolveJob: Job? = null
    private var submitJob: Job? = null

    init {
        observeInventory()
    }

    fun onEvent(event: InventoryUiEvent) {
        when (event) {
            InventoryUiEvent.LoadInventory -> observeInventory()
            InventoryUiEvent.LoadProducts -> loadProducts()
            InventoryUiEvent.Refresh -> refresh()
            InventoryUiEvent.Retry -> retry()

            is InventoryUiEvent.SearchQueryChanged -> updateScreen { updateSearchQuery(event.query) }
            is InventoryUiEvent.SortDirectionChanged -> updateScreen { updateSortDirection(event.direction) }
            is InventoryUiEvent.UnitFilterChanged -> updateScreen { updateUnitFilter(event.unit) }
            InventoryUiEvent.ClearFilters -> updateScreen { resetFilters() }

            InventoryUiEvent.ShowForm -> openForm()
            InventoryUiEvent.HideForm -> closeForm()
            InventoryUiEvent.ClearForm -> updateForm { InventoryFormState.initial() }

            is InventoryUiEvent.BarcodeChanged -> onBarcodeChanged(event.barcode)
            is InventoryUiEvent.ProductNameChanged -> updateForm { copy(productName = event.name) }
            is InventoryUiEvent.DescriptionChanged -> updateForm { copy(description = event.description) }
            is InventoryUiEvent.LocationChanged -> updateForm { copy(location = event.location) }
            is InventoryUiEvent.QuantityChanged -> updateForm { copy(quantity = event.quantity) }
            is InventoryUiEvent.UnitOfMeasureChanged -> updateForm { copy(unitOfMeasure = event.unitOfMeasure) }
            is InventoryUiEvent.ExpiryDateChanged -> updateForm { copy(expiryDate = event.expiryDate) }

            is InventoryUiEvent.ProductSelected -> onProductSelected(event.product)
            is InventoryUiEvent.StockItemSelected -> onStockItemSelected(event.stockItem)
            is InventoryUiEvent.BarcodeResolved -> scheduleBarcodeLookup(event.barcode)

            InventoryUiEvent.SubmitForm,
            InventoryUiEvent.SaveForm -> submitForm()

            InventoryUiEvent.DeleteSelected -> deleteSelectedItems()
            is InventoryUiEvent.DeleteStockItem -> deleteSingleItem(event.barcode)

            is InventoryUiEvent.ToggleSelection -> updateScreen { toggleSelection(event.barcode) }
            is InventoryUiEvent.SelectOnly -> updateScreen { selectOnly(event.barcodes) }
            InventoryUiEvent.ClearSelection -> updateScreen { clearSelection() }
            InventoryUiEvent.StartMultiSelect -> updateScreen { copy(isMultiSelectMode = true) }
            InventoryUiEvent.StopMultiSelect -> updateScreen { clearSelection() }

            InventoryUiEvent.ClearMessage -> updateScreen { clearMessage() }
            is InventoryUiEvent.ShowMessage -> updateScreen { markMessage(event.message) }
            is InventoryUiEvent.ShowError -> handleShowError(event.message, event.throwable)
        }
    }

    private fun observeInventory() {
        observeJob?.cancel()
        observeJob = viewModelScope.launch(dispatchers.main) {
            updateScreen { onStockItemsLoading() }
            getInventoryUseCase.observeAll()
                .catch { t -> handleStockLoadError(t) }
                .onEach { items -> updateScreen { onStockItemsSuccess(items) } }
                .launchIn(this)
        }
    }

    private fun loadProducts() {
    updateScreen { onProductsLoading() }

    viewModelScope.launch(dispatchers.io) {
        try {
            // التصحيح هنا: استدعاء الكائن مباشرة لأنه يستخدم invoke
            val products = getProductsUseCase() 

            withContext(dispatchers.main) {
                updateScreen { onProductsSuccess(products) }
            }
        } catch (t: Throwable) {
            if (t is CancellationException) throw t

            withContext(dispatchers.main) {
                updateScreen {
                    onProductsError(
                        message = t.message ?: "فشل تحميل المنتجات",
                        throwable = t
                    )
                }
            }
        }
    }
}


    private fun refresh() {
        updateScreen { startRefreshing() }
        viewModelScope.launch(dispatchers.io) {
            try {
                val items = getInventoryUseCase(InventoryQuery())
                withContext(dispatchers.main) {
                    updateScreen { stopRefreshing().onStockItemsSuccess(items) }
                }
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                withContext(dispatchers.main) {
                    updateScreen {
                        stopRefreshing().onStockItemsError(
                            message = t.message ?: "فشل تحديث المخزون",
                            throwable = t
                        )
                    }
                }
            }
        }
    }

    private fun retry() {
        if (_screenState.value.stockItemsState is UiState.Error) observeInventory()
        if (_screenState.value.productsState is UiState.Error) loadProducts()
    }

    private fun openForm(preloaded: InventoryFormState = InventoryFormState.initial()) {
        updateScreen { showForm(preloaded) }
    }

    private fun closeForm() {
        submitJob?.cancel()
        barcodeResolveJob?.cancel()
        updateScreen { hideForm() }
        emitEffect(InventoryUiEffect.CloseForm)
    }

    private fun onBarcodeChanged(raw: String) {
        updateForm { copy(barcode = raw) }
        val trimmed = raw.trim()
        if (trimmed.length >= 4) scheduleBarcodeLookup(trimmed)
        else barcodeResolveJob?.cancel()
    }

    /**
     * Best-effort barcode lookup.
     * لا نوقف إدخال المستخدم إذا فشل البحث أو الشبكة.
     */
    private fun scheduleBarcodeLookup(barcode: String) {
        barcodeResolveJob?.cancel()
        barcodeResolveJob = viewModelScope.launch(dispatchers.io) {
            try {
                val existing = getInventoryUseCase.getByBarcode(barcode)
                if (existing != null) {
                    withContext(dispatchers.main) {
                        updateForm { withResolvedStockItem(existing) }
                    }
                }
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
            }
        }
    }

    private fun onProductSelected(product: Product) {
        updateForm { withResolvedProduct(product) }
    }

    private fun onStockItemSelected(stockItem: StockItem) {
        val preloaded = InventoryFormState.initial().withResolvedStockItem(stockItem)
        openForm(preloaded)
    }

    private fun submitForm() {
        val form = _screenState.value.formState

        val validationError = form.validationErrorMessage
        if (validationError != null) {
            updateForm { markError(validationError) }
            return
        }

        val stockItem = form.toStockItemOrNull()
        if (stockItem == null) {
            updateForm { markError("تعذّر تحويل النموذج إلى صنف مخزون") }
            return
        }

        submitJob?.cancel()
        submitJob = viewModelScope.launch(dispatchers.main) {
            updateForm { startSubmitting() }

            val result = withContext(dispatchers.io) {
                saveStockItemUseCase(stockItem)
            }

            result.fold(
                onSuccess = {
                    val msg = if (form.isEditing) "تم تحديث الصنف بنجاح" else "تمت إضافة الصنف بنجاح"
                    updateForm { markSuccess(msg) }
                    emitEffect(InventoryUiEffect.ShowMessage(msg))
                    closeForm()
                },
                onFailure = { t ->
                    val msg = t.message ?: "حدث خطأ أثناء حفظ الصنف"
                    updateForm { markError(msg) }
                    emitEffect(InventoryUiEffect.ShowError(msg, t))
                }
            )
        }
    }

    private fun deleteSelectedItems() {
        val barcodes = _screenState.value.selectedBarcodes
        if (barcodes.isEmpty()) return

        viewModelScope.launch(dispatchers.main) {
            updateScreen { startProcessing() }

            val failures = mutableListOf<String>()

            withContext(dispatchers.io) {
                barcodes.forEach { barcode ->
                    deleteStockItemUseCase(barcode).onFailure {
                        failures += barcode
                    }
                }
            }

            updateScreen { stopProcessing().clearSelection() }

            if (failures.isEmpty()) {
                val msg = "تم حذف ${barcodes.size} صنف بنجاح"
                updateScreen { markMessage(msg) }
                emitEffect(InventoryUiEffect.ShowMessage(msg))
            } else {
                val msg = "فشل حذف ${failures.size} من ${barcodes.size} أصناف"
                updateScreen { markMessage(msg) }
                emitEffect(InventoryUiEffect.ShowError(msg))
            }
        }
    }

    private fun deleteSingleItem(barcode: String) {
        viewModelScope.launch(dispatchers.main) {
            updateScreen { startProcessing() }

            val result = withContext(dispatchers.io) {
                deleteStockItemUseCase(barcode)
            }

            updateScreen { stopProcessing() }

            result.fold(
                onSuccess = {
                    val msg = "تم حذف الصنف بنجاح"
                    updateScreen { markMessage(msg) }
                    emitEffect(InventoryUiEffect.ShowMessage(msg))
                },
                onFailure = { t ->
                    val msg = when (t) {
                        is NoSuchElementException -> "الصنف غير موجود"
                        else -> t.message ?: "فشل حذف الصنف"
                    }
                    updateScreen { markMessage(msg) }
                    emitEffect(InventoryUiEffect.ShowError(msg, t))
                }
            )
        }
    }

    private fun handleStockLoadError(t: Throwable) {
        if (t is CancellationException) throw t
        updateScreen {
            onStockItemsError(
                message = t.message ?: "فشل تحميل المخزون",
                throwable = t
            )
        }
    }

    private fun handleShowError(message: String, throwable: Throwable? = null) {
        updateScreen { markMessage(message) }
        emitEffect(InventoryUiEffect.ShowError(message, throwable))
    }

    private inline fun updateScreen(reducer: InventoryUiState.() -> InventoryUiState) {
        _screenState.update { it.reducer() }
    }

    private inline fun updateForm(reducer: InventoryFormState.() -> InventoryFormState) {
        _screenState.update { screen ->
            screen.copy(formState = screen.formState.reducer())
        }
    }

    private fun emitEffect(effect: InventoryUiEffect) {
        _effects.tryEmit(effect)
    }
}