package com.myimdad_por.ui.features.sales

import com.myimdad_por.core.base.BaseViewModel
import com.myimdad_por.core.dispatchers.AppDispatchers
import com.myimdad_por.core.dispatchers.DefaultAppDispatchers
import com.myimdad_por.domain.model.Customer
import com.myimdad_por.domain.model.PaymentMethod
import com.myimdad_por.domain.model.Product
import com.myimdad_por.domain.model.Sale
import com.myimdad_por.domain.model.SaleItem
import com.myimdad_por.domain.repository.SalesRepository
import com.myimdad_por.domain.usecase.GetCustomersUseCase
import java.math.BigDecimal
import java.util.Locale
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class SalesViewModel @Inject constructor(
    private val salesRepository: SalesRepository,
    private val getCustomersUseCase: GetCustomersUseCase,
    dispatchers: AppDispatchers = DefaultAppDispatchers
) : BaseViewModel<SalesUiState>(dispatchers) {

    private var salesCollectorJob: Job? = null
    private var customersCollectorJob: Job? = null
    private var searchJob: Job? = null

    private var cachedSales: List<Sale> = emptyList()
    private var cachedCustomers: List<Customer> = emptyList()

    init {
        setSuccess(SalesUiState(isLoading = true))
        startCollectors()
        loadSnapshot(forceRefresh = true)
    }

    fun onEvent(event: SalesUiEvent) {
        when (event) {
            SalesUiEvent.Load -> loadSnapshot(forceRefresh = true)
            SalesUiEvent.Refresh -> refresh()
            SalesUiEvent.Retry -> {
                clearErrorState()
                loadSnapshot(forceRefresh = true)
            }

            SalesUiEvent.ClearError -> clearErrorState()

            is SalesUiEvent.ChangeSearchQuery -> updateSearchQuery(event.query)
            is SalesUiEvent.SelectCustomer -> mutateState { copy(selectedCustomer = event.customer) }
            is SalesUiEvent.SelectPaymentMethod -> mutateState { copy(selectedPaymentMethod = event.paymentMethod) }
            is SalesUiEvent.SelectSale -> selectSale(event.saleId)

            is SalesUiEvent.AddProductToDraft -> addProductToDraft(event.product, event.quantity)
            is SalesUiEvent.AddDraftItem -> addDraftItem(event.item)
            is SalesUiEvent.UpdateDraftItem -> updateDraftItem(event.item)
            is SalesUiEvent.RemoveDraftItem -> removeDraftItem(event.itemId)
            SalesUiEvent.ClearDraftItems -> mutateState { copy(draftItems = emptyList()) }

            is SalesUiEvent.UpdateInvoiceNumber -> mutateState { copy(invoiceNumber = event.invoiceNumber.trim()) }
            is SalesUiEvent.UpdateNote -> mutateState { copy(note = event.note) }
            is SalesUiEvent.UpdatePaidAmount -> updatePaidAmount(event.amount)

            SalesUiEvent.SubmitSale -> submitSale()
            SalesUiEvent.SaveDraft -> saveDraft()
            SalesUiEvent.DeleteSelectedSale -> deleteSelectedSale()
            SalesUiEvent.ResetForm -> resetForm()
            SalesUiEvent.NavigateBack -> Unit
        }
    }

    override fun onCleared() {
        salesCollectorJob?.cancel()
        customersCollectorJob?.cancel()
        searchJob?.cancel()
        super.onCleared()
    }

    private fun startCollectors() {
        if (salesCollectorJob?.isActive == true && customersCollectorJob?.isActive == true) return

        salesCollectorJob = launch(dispatchers.io) {
            salesRepository.observeAllSales().collect { sales ->
                cachedSales = sales
                if (currentQuery().isBlank()) {
                    syncLiveSnapshot()
                }
            }
        }

        customersCollectorJob = launch(dispatchers.io) {
            getCustomersUseCase.observeAll().collect { customers ->
                cachedCustomers = customers
                if (currentQuery().isBlank()) {
                    syncLiveSnapshot()
                }
            }
        }
    }

    private fun loadSnapshot(forceRefresh: Boolean = false) {
        mutateState {
            copy(
                isLoading = true,
                isRefreshing = forceRefresh,
                isSubmitting = false,
                errorMessage = null,
                validationErrors = emptyMap()
            )
        }

        launch(dispatchers.io) {
            try {
                val query = currentQuery()

                val snapshot = if (query.isBlank()) {
                    val sales = salesRepository.observeAllSales().first()
                    val customers = getCustomersUseCase.observeAll().first()
                    cachedSales = sales
                    cachedCustomers = customers
                    Snapshot(sales = sales, customers = customers)
                } else {
                    Snapshot(
                        sales = salesRepository.searchSales(query),
                        customers = getCustomersUseCase.search(query)
                    )
                }

                applySnapshot(
                    sales = snapshot.sales,
                    customers = snapshot.customers,
                    keepDraft = true
                )
            } catch (throwable: Throwable) {
                if (throwable is CancellationException) throw throwable
                mutateState {
                    copy(
                        isLoading = false,
                        isRefreshing = false,
                        isSubmitting = false,
                        errorMessage = throwable.message ?: DEFAULT_ERROR_MESSAGE
                    )
                }
            }
        }
    }

    private fun refresh() {
        val query = currentQuery()
        if (query.isBlank()) {
            loadSnapshot(forceRefresh = true)
        } else {
            scheduleSearch(query, isRefresh = true)
        }
    }

    private fun updateSearchQuery(query: String) {
        val normalized = query.trim()

        mutateState {
            copy(
                searchQuery = normalized,
                errorMessage = null,
                validationErrors = emptyMap()
            )
        }

        searchJob?.cancel()
        if (normalized.isBlank()) {
            syncLiveSnapshot()
        } else {
            scheduleSearch(normalized, isRefresh = false)
        }
    }

    private fun scheduleSearch(query: String, isRefresh: Boolean) {
        searchJob?.cancel()
        searchJob = launch(dispatchers.io) {
            try {
                if (!isRefresh) delay(250L)

                mutateState {
                    copy(
                        isRefreshing = true,
                        isLoading = false,
                        errorMessage = null
                    )
                }

                val sales = salesRepository.searchSales(query)
                val customers = getCustomersUseCase.search(query)

                applySnapshot(
                    sales = sales,
                    customers = customers,
                    keepDraft = true
                )
            } catch (throwable: Throwable) {
                if (throwable is CancellationException) throw throwable
                mutateState {
                    copy(
                        isLoading = false,
                        isRefreshing = false,
                        isSubmitting = false,
                        errorMessage = throwable.message ?: DEFAULT_ERROR_MESSAGE
                    )
                }
            }
        }
    }

    private fun selectSale(saleId: String) {
        if (saleId.isBlank()) return

        launch(dispatchers.io) {
            try {
                val sale = salesRepository.getSaleById(saleId)
                if (sale == null) {
                    mutateState {
                        copy(
                            errorMessage = "لم يتم العثور على عملية البيع المطلوبة",
                            selectedSale = null
                        )
                    }
                    return@launch
                }

                val selectedCustomer = sale.customerId
                    ?.takeIf { it.isNotBlank() }
                    ?.let { getCustomersUseCase.getById(it) }

                mutateState {
                    copy(
                        selectedSale = sale,
                        selectedCustomer = selectedCustomer ?: this.selectedCustomer,
                        invoiceNumber = sale.invoiceNumber,
                        note = sale.note.orEmpty(),
                        draftItems = sale.items,
                        validationErrors = emptyMap(),
                        errorMessage = null
                    )
                }
            } catch (throwable: Throwable) {
                if (throwable is CancellationException) throw throwable
                mutateState {
                    copy(
                        errorMessage = throwable.message ?: "تعذر تحميل عملية البيع",
                        isLoading = false,
                        isRefreshing = false,
                        isSubmitting = false
                    )
                }
            }
        }
    }

    private fun addProductToDraft(product: Product, quantity: Int) {
        if (quantity <= 0) return
        addDraftItem(product.toDraftItem(quantity))
    }

    private fun addDraftItem(item: SaleItem) {
        mutateState {
            copy(
                draftItems = draftItems.mergeDraftItem(item),
                validationErrors = validationErrors - DRAFT_ITEMS_KEY,
                errorMessage = null
            )
        }
    }

    private fun updateDraftItem(item: SaleItem) {
        mutateState {
            copy(
                draftItems = draftItems.replaceDraftItem(item),
                validationErrors = validationErrors - DRAFT_ITEMS_KEY,
                errorMessage = null
            )
        }
    }

    private fun removeDraftItem(itemId: String) {
        if (itemId.isBlank()) return

        mutateState {
            copy(
                draftItems = draftItems.filterNot { draftItemKey(it) == itemId },
                validationErrors = validationErrors - DRAFT_ITEMS_KEY
            )
        }
    }

    private fun updatePaidAmount(amount: String) {
        val parsed = amount.trim()
            .replace(',', '.')
            .toBigDecimalOrNull()
            ?.takeIf { it >= BigDecimal.ZERO }
            ?: BigDecimal.ZERO

        mutateState { copy(paidAmount = parsed) }
    }

    private fun submitSale() {
        val state = currentUiState()

        if (state.draftItems.isEmpty()) {
            mutateState {
                copy(
                    validationErrors = validationErrors + (DRAFT_ITEMS_KEY to "أضف صنفًا واحدًا على الأقل"),
                    errorMessage = "لا يمكن إرسال عملية بيع فارغة"
                )
            }
            return
        }

        val sale = state.selectedSale ?: run {
            mutateState {
                copy(
                    validationErrors = validationErrors + (SALE_KEY to "اختر عملية بيع أو أنشئ مسودة قبل الإرسال"),
                    errorMessage = "لا توجد عملية بيع جاهزة للإرسال"
                )
            }
            return
        }

        val updatedSale = sale.copy(
            items = state.draftItems,
            invoiceNumber = state.invoiceNumber.ifBlank { sale.invoiceNumber },
            note = state.note.ifBlank { sale.note.orEmpty() }
        )

        launch(dispatchers.io) {
            try {
                mutateState { copy(isSubmitting = true, errorMessage = null) }

                val savedSale = salesRepository.updateSale(updatedSale).getOrThrow()

                cachedSales = cachedSales.upsert(savedSale)

                mutateState {
                    copy(
                        sales = cachedSales,
                        selectedSale = savedSale,
                        draftItems = emptyList(),
                        note = "",
                        invoiceNumber = savedSale.invoiceNumber,
                        isSubmitting = false,
                        isLoading = false,
                        isRefreshing = false,
                        errorMessage = null,
                        validationErrors = emptyMap()
                    )
                }
            } catch (throwable: Throwable) {
                if (throwable is CancellationException) throw throwable
                mutateState {
                    copy(
                        isSubmitting = false,
                        errorMessage = throwable.message ?: "تعذر إتمام عملية البيع"
                    )
                }
            }
        }
    }

    private fun saveDraft() {
        val state = currentUiState()
        val sale = state.selectedSale ?: run {
            mutateState {
                copy(errorMessage = "الحفظ كمسودة يتطلب تحديد عملية بيع موجودة للتعديل")
            }
            return
        }

        val draftSale = sale.copy(
            items = state.draftItems,
            invoiceNumber = state.invoiceNumber.ifBlank { sale.invoiceNumber },
            note = state.note.ifBlank { sale.note.orEmpty() }
        )

        launch(dispatchers.io) {
            try {
                mutateState { copy(isSubmitting = true, errorMessage = null) }

                val saved = salesRepository.updateSale(draftSale).getOrThrow()
                cachedSales = cachedSales.upsert(saved)

                mutateState {
                    copy(
                        sales = cachedSales,
                        selectedSale = saved,
                        isSubmitting = false,
                        errorMessage = null
                    )
                }
            } catch (throwable: Throwable) {
                if (throwable is CancellationException) throw throwable
                mutateState {
                    copy(
                        isSubmitting = false,
                        errorMessage = throwable.message ?: "تعذر حفظ المسودة"
                    )
                }
            }
        }
    }

    private fun deleteSelectedSale() {
        val saleId = currentUiState().selectedSale?.id?.takeIf { it.isNotBlank() } ?: run {
            mutateState { copy(errorMessage = "لا توجد عملية بيع محددة للحذف") }
            return
        }

        launch(dispatchers.io) {
            try {
                mutateState { copy(isSubmitting = true, errorMessage = null) }

                salesRepository.deleteSale(saleId).getOrThrow()

                cachedSales = cachedSales.filterNot { it.id == saleId }

                mutateState {
                    copy(
                        sales = cachedSales,
                        selectedSale = null,
                        draftItems = emptyList(),
                        isSubmitting = false,
                        errorMessage = null
                    )
                }
            } catch (throwable: Throwable) {
                if (throwable is CancellationException) throw throwable
                mutateState {
                    copy(
                        isSubmitting = false,
                        errorMessage = throwable.message ?: "تعذر حذف عملية البيع"
                    )
                }
            }
        }
    }

    private fun resetForm() {
        mutateState {
            copy(
                selectedCustomer = null,
                selectedPaymentMethod = null,
                selectedSale = null,
                invoiceNumber = "",
                note = "",
                draftItems = emptyList(),
                paidAmount = BigDecimal.ZERO,
                validationErrors = emptyMap(),
                errorMessage = null,
                isLoading = false,
                isRefreshing = false,
                isSubmitting = false
            )
        }
    }

    private fun clearErrorState() {
        mutateState {
            copy(
                errorMessage = null,
                validationErrors = emptyMap()
            )
        }
    }

    private fun syncLiveSnapshot() {
        val state = currentUiState()

        val refreshedSelectedSale = state.selectedSale?.let { selected ->
            cachedSales.firstOrNull { it.id == selected.id } ?: selected
        }

        val refreshedSelectedCustomer = refreshedSelectedSale?.customerId
            ?.takeIf { it.isNotBlank() }
            ?.let { customerId ->
                cachedCustomers.firstOrNull { it.id == customerId } ?: state.selectedCustomer
            } ?: state.selectedCustomer

        mutateState {
            copy(
                sales = cachedSales,
                customers = cachedCustomers,
                selectedSale = refreshedSelectedSale,
                selectedCustomer = refreshedSelectedCustomer,
                isLoading = false,
                isRefreshing = false,
                errorMessage = null
            )
        }
    }

    private fun applySnapshot(
        sales: List<Sale>,
        customers: List<Customer>,
        keepDraft: Boolean
    ) {
        val state = currentUiState()

        val refreshedSelectedSale = state.selectedSale?.let { selected ->
            sales.firstOrNull { it.id == selected.id } ?: selected
        }

        val refreshedSelectedCustomer = refreshedSelectedSale?.customerId
            ?.takeIf { it.isNotBlank() }
            ?.let { customerId ->
                customers.firstOrNull { it.id == customerId } ?: state.selectedCustomer
            } ?: state.selectedCustomer

        mutateState {
            copy(
                sales = sales,
                customers = customers,
                selectedSale = refreshedSelectedSale,
                selectedCustomer = refreshedSelectedCustomer,
                draftItems = if (keepDraft) draftItems else emptyList(),
                isLoading = false,
                isRefreshing = false,
                isSubmitting = false,
                errorMessage = null
            )
        }
    }

    private fun currentQuery(): String {
        return currentUiState().searchQuery.trim()
    }

    private fun currentUiState(): SalesUiState {
        return currentData ?: SalesUiState()
    }

    private inline fun mutateState(reducer: SalesUiState.() -> SalesUiState) {
        setSuccess(currentUiState().reducer())
    }

    private fun List<Sale>.upsert(updatedSale: Sale): List<Sale> {
        val index = indexOfFirst { it.id == updatedSale.id }
        return if (index >= 0) {
            toMutableList().apply { this[index] = updatedSale }.toList()
        } else {
            this + updatedSale
        }
    }

    private fun List<SaleItem>.mergeDraftItem(newItem: SaleItem): List<SaleItem> {
        val key = draftItemKey(newItem)
        val index = indexOfFirst { draftItemKey(it) == key }
        if (index < 0) return this + newItem

        val existing = this[index]
        val merged = existing.copy(
            quantity = existing.quantity + newItem.quantity
        )

        return toMutableList().apply { this[index] = merged }.toList()
    }

    private fun List<SaleItem>.replaceDraftItem(newItem: SaleItem): List<SaleItem> {
        val key = draftItemKey(newItem)
        val index = indexOfFirst { draftItemKey(it) == key }
        return if (index >= 0) {
            toMutableList().apply { this[index] = newItem }.toList()
        } else {
            this + newItem
        }
    }

    private fun Product.toDraftItem(quantity: Int): SaleItem {
        val productId = reflectText("id", "productId", "barcode")
        val productName = reflectText("effectiveName", "displayName", "name", "title")
        val unitPrice = reflectBigDecimal("price", "salePrice", "unitPrice")
        // السطر 587: استخدمنا ?: لإعطاء قيمة افتر
        val unit = reflectUnit("unitOfMeasure", "unit") ?: com.myimdad_por.domain.model.UnitOfMeasure.DEFAULT

        return SaleItem(
            id = "${productId}_$quantity",
            productId = productId,
            productName = productName,
            unit = unit,
            quantity = BigDecimal.valueOf(quantity.toLong()),
            unitPrice = unitPrice,
            taxAmount = BigDecimal.ZERO,
            discountAmount = BigDecimal.ZERO,
            note = null,
            isReturn = false
        )
    }

    private fun draftItemKey(item: SaleItem): String {
        return item.id.ifBlank { item.productId }
    }

    private fun Any.reflectText(vararg names: String): String {
        return reflectAny(*names)?.toString().orEmpty()
    }

    private fun Any.reflectBigDecimal(vararg names: String): BigDecimal {
        return when (val value = reflectAny(*names)) {
            is BigDecimal -> value
            is Number -> BigDecimal.valueOf(value.toDouble())
            else -> value?.toString()?.toBigDecimalOrNull() ?: BigDecimal.ZERO
        }
    }

    private fun Any.reflectUnit(vararg names: String): com.myimdad_por.domain.model.UnitOfMeasure? {
        return reflectAny(*names) as? com.myimdad_por.domain.model.UnitOfMeasure
    }

    private fun Any.reflectAny(vararg names: String): Any? {
        val clazz = javaClass

        names.forEach { name ->
            val getter = clazz.methods.firstOrNull { method ->
                method.parameterCount == 0 &&
                    (method.name == name || method.name == "get${name.replaceFirstChar { ch -> ch.uppercase(Locale.getDefault()) }}")
            }

            if (getter != null) {
                return runCatching { getter.invoke(this) }.getOrNull()
            }
        }

        return null
    }

    companion object {
        private const val DEFAULT_ERROR_MESSAGE = "حدث خطأ غير متوقع"
        private const val DRAFT_ITEMS_KEY = "draftItems"
        private const val SALE_KEY = "sale"
    }

    private data class Snapshot(
        val sales: List<Sale>,
        val customers: List<Customer>
    )
}