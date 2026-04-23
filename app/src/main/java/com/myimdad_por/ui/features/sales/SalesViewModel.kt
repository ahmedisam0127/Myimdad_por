package com.myimdad_por.ui.features.sales

import com.myimdad_por.core.base.BaseViewModel
import com.myimdad_por.domain.model.Customer
import com.myimdad_por.domain.model.PaymentMethod
import com.myimdad_por.domain.model.Product
import com.myimdad_por.domain.model.Sale
import com.myimdad_por.domain.model.SaleItem
import com.myimdad_por.domain.usecase.GetCustomersUseCase
import com.myimdad_por.domain.usecase.ProcessSaleRequest
import com.myimdad_por.domain.usecase.ProcessSaleUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject
import kotlin.math.max
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay

private const val DEFAULT_EMPLOYEE_ID = "system"
private const val DEFAULT_STOCK_LOCATION = "main"
private const val DEFAULT_ERROR_MESSAGE = "حدث خطأ غير متوقع"
private const val SEARCH_DEBOUNCE_MS = 300L
private val ZERO_MONEY: BigDecimal = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)

@HiltViewModel
class SalesViewModel @Inject constructor(
    private val getCustomersUseCase: GetCustomersUseCase,
    private val processSaleUseCase: ProcessSaleUseCase
) : BaseViewModel<SalesUiState>() {

    private var searchJob: Job? = null

    init {
        setSuccess(SalesUiState())
        loadCustomers()
    }

    fun onEvent(event: SalesUiEvent) {
        when (event) {
            SalesUiEvent.Load -> loadCustomers()
            SalesUiEvent.Refresh -> loadCustomers(forceRefresh = true)
            SalesUiEvent.Retry -> loadCustomers()

            SalesUiEvent.ClearError -> clearErrors()

            is SalesUiEvent.ChangeSearchQuery -> updateSearchQuery(event.query)
            is SalesUiEvent.SelectCustomer -> mutateState { copy(selectedCustomer = event.customer) }
            is SalesUiEvent.SelectPaymentMethod -> mutateState { copy(selectedPaymentMethod = event.paymentMethod) }
            is SalesUiEvent.SelectSale -> selectSale(event.saleId)

            is SalesUiEvent.AddProductToDraft -> addProductToDraft(event.product, event.quantity)
            is SalesUiEvent.AddDraftItem -> upsertDraftItem(event.item)
            is SalesUiEvent.UpdateDraftItem -> updateDraftItem(event.item)
            is SalesUiEvent.RemoveDraftItem -> removeDraftItem(event.itemId)
            SalesUiEvent.ClearDraftItems -> mutateState { copy(draftItems = emptyList()) }

            is SalesUiEvent.UpdateInvoiceNumber -> mutateState {
                copy(
                    invoiceNumber = event.invoiceNumber.trim(),
                    validationErrors = emptyMap(),
                    errorMessage = null
                )
            }

            is SalesUiEvent.UpdateNote -> mutateState {
                copy(
                    note = event.note,
                    validationErrors = emptyMap(),
                    errorMessage = null
                )
            }

            is SalesUiEvent.UpdatePaidAmount -> updatePaidAmount(event.amount)

            SalesUiEvent.SubmitSale -> submitSale()
            SalesUiEvent.SaveDraft -> saveDraft()
            SalesUiEvent.DeleteSelectedSale -> deleteSelectedSale()
            SalesUiEvent.ResetForm -> resetForm()

            SalesUiEvent.NavigateBack -> Unit
        }
    }

    private fun loadCustomers(forceRefresh: Boolean = false) {
        launch(dispatcher = dispatchers.io) {
            mutateState {
                copy(
                    isLoading = !forceRefresh,
                    isRefreshing = forceRefresh,
                    errorMessage = null,
                    validationErrors = emptyMap()
                )
            }

            runCatching {
                val query = currentSalesState().searchQuery.trim()
                if (query.isBlank()) {
                    getCustomersUseCase()
                } else {
                    getCustomersUseCase.search(query)
                }
            }.onSuccess { customers ->
                mutateState {
                    copy(
                        isLoading = false,
                        isRefreshing = false,
                        customers = customers,
                        errorMessage = null
                    )
                }
            }.onFailure { throwable ->
                mutateState {
                    copy(
                        isLoading = false,
                        isRefreshing = false,
                        errorMessage = throwable.toUserMessage()
                    )
                }
            }
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
        searchJob = launch(dispatcher = dispatchers.io) {
            delay(SEARCH_DEBOUNCE_MS)

            mutateState {
                copy(
                    isLoading = true,
                    errorMessage = null
                )
            }

            runCatching {
                if (normalized.isBlank()) {
                    getCustomersUseCase()
                } else {
                    getCustomersUseCase.search(normalized)
                }
            }.onSuccess { customers ->
                mutateState {
                    copy(
                        isLoading = false,
                        customers = customers,
                        errorMessage = null
                    )
                }
            }.onFailure { throwable ->
                mutateState {
                    copy(
                        isLoading = false,
                        errorMessage = throwable.toUserMessage()
                    )
                }
            }
        }
    }

    private fun selectSale(saleId: String) {
        val normalizedId = saleId.trim()
        if (normalizedId.isBlank()) return

        val state = currentSalesState()
        val sale = state.sales.firstOrNull { it.id == normalizedId }

        if (sale == null) {
            mutateState {
                copy(
                    errorMessage = "لم يتم العثور على الفاتورة المحددة",
                    validationErrors = mapOf("sale" to "لم يتم العثور على الفاتورة المحددة")
                )
            }
            return
        }

        val customer = sale.customerId?.let { customerId ->
            state.customers.firstOrNull { it.id == customerId }
        }

        mutateState {
            copy(
                selectedSale = sale,
                selectedCustomer = customer,
                invoiceNumber = sale.invoiceNumber,
                note = sale.note.orEmpty(),
                draftItems = sale.items,
                paidAmount = sale.paidAmount.money(),
                validationErrors = emptyMap(),
                errorMessage = null
            )
        }
    }

    private fun addProductToDraft(product: Product, quantity: Int) {
        val safeQuantity = max(1, quantity)

        val item = SaleItem(
            productId = product.normalizedBarcode,
            productName = product.effectiveName,
            unit = product.unitOfMeasure,
            quantity = BigDecimal.valueOf(safeQuantity.toLong()),
            unitPrice = product.price.money(),
            taxAmount = ZERO_MONEY,
            discountAmount = ZERO_MONEY,
            isReturn = false,
            note = null
        )

        upsertDraftItem(item)
    }

    private fun upsertDraftItem(item: SaleItem) {
        mutateState {
            val updatedItems = draftItems.toMutableList()
            val existingIndex = updatedItems.indexOfFirst {
                it.productId == item.productId &&
                    it.unit == item.unit &&
                    it.isReturn == item.isReturn &&
                    it.unitPrice == item.unitPrice &&
                    it.taxAmount == item.taxAmount &&
                    it.discountAmount == item.discountAmount
            }

            if (existingIndex >= 0) {
                val existing = updatedItems[existingIndex]
                updatedItems[existingIndex] = existing.copy(
                    quantity = (existing.quantity + item.quantity).money()
                )
            } else {
                updatedItems.add(item)
            }

            copy(
                draftItems = updatedItems,
                validationErrors = emptyMap(),
                errorMessage = null
            )
        }
    }

    private fun updateDraftItem(item: SaleItem) {
        mutateState {
            copy(
                draftItems = draftItems.map { existing ->
                    if (existing.id == item.id) item else existing
                },
                validationErrors = emptyMap(),
                errorMessage = null
            )
        }
    }

    private fun removeDraftItem(itemId: String) {
        val normalizedId = itemId.trim()
        if (normalizedId.isBlank()) return

        mutateState {
            copy(
                draftItems = draftItems.filterNot { it.id == normalizedId },
                validationErrors = emptyMap(),
                errorMessage = null
            )
        }
    }

    private fun updatePaidAmount(amountText: String) {
        val parsedAmount = amountText
            .trim()
            .replace(",", "")
            .toBigDecimalOrNull()
            ?.money()
            ?: ZERO_MONEY

        mutateState {
            copy(
                paidAmount = parsedAmount,
                validationErrors = emptyMap(),
                errorMessage = null
            )
        }
    }

    private fun submitSale() {
        val state = currentSalesState()
        if (state.isSubmitting) return

        val validationErrors = validateForSubmit(state)
        if (validationErrors.isNotEmpty()) {
            mutateState {
                copy(
                    validationErrors = validationErrors,
                    errorMessage = validationErrors.values.firstOrNull()
                )
            }
            return
        }

        launch(dispatcher = dispatchers.io) {
            mutateState {
                copy(
                    isSubmitting = true,
                    errorMessage = null,
                    validationErrors = emptyMap()
                )
            }

            val sale = buildSale(state)
            val request = ProcessSaleRequest(
                sale = sale,
                defaultStockLocation = DEFAULT_STOCK_LOCATION,
                itemBarcodes = state.draftItems.associate { it.productId to it.productId },
                itemLocations = emptyMap(),
                createInvoice = true,
                issueInvoiceNow = true,
                allowReturnItems = state.draftItems.any { it.isReturn },
                invoicePartyId = state.selectedCustomer?.id,
                invoicePartyName = state.selectedCustomer?.displayName,
                invoicePartyTaxNumber = state.selectedCustomer?.taxNumber,
                invoiceNotes = state.note.trim().takeIf { it.isNotBlank() },
                invoiceTermsAndConditions = null
            )

            runCatching {
                processSaleUseCase(request).getOrThrow()
            }.onSuccess { result ->
                val savedSale = result.savedSale

                mutateState {
                    copy(
                        sales = listOf(savedSale) + sales.filterNot { it.id == savedSale.id },
                        selectedSale = savedSale,
                        draftItems = emptyList(),
                        paidAmount = ZERO_MONEY,
                        note = "",
                        invoiceNumber = "",
                        selectedPaymentMethod = null,
                        selectedCustomer = null,
                        isSubmitting = false,
                        validationErrors = emptyMap(),
                        errorMessage = null
                    )
                }
            }.onFailure { throwable ->
                mutateState {
                    copy(
                        isSubmitting = false,
                        errorMessage = throwable.toUserMessage()
                    )
                }
            }
        }
    }

    private fun saveDraft() {
        mutateState {
            copy(
                errorMessage = null,
                validationErrors = emptyMap()
            )
        }
    }

    private fun deleteSelectedSale() {
        val sale = currentSalesState().selectedSale ?: return

        mutateState {
            copy(
                sales = sales.filterNot { it.id == sale.id },
                selectedSale = null,
                validationErrors = emptyMap(),
                errorMessage = null
            )
        }
    }

    private fun resetForm() {
        searchJob?.cancel()
        mutateState {
            copy(
                isLoading = false,
                isRefreshing = false,
                isSubmitting = false,
                selectedCustomer = null,
                selectedPaymentMethod = null,
                selectedSale = null,
                invoiceNumber = "",
                searchQuery = "",
                note = "",
                draftItems = emptyList(),
                paidAmount = ZERO_MONEY,
                validationErrors = emptyMap(),
                errorMessage = null
            )
        }
        loadCustomers()
    }

    private fun clearErrors() {
        mutateState {
            copy(
                errorMessage = null,
                validationErrors = emptyMap()
            )
        }
    }

    private fun validateForSubmit(state: SalesUiState): Map<String, String> {
        val errors = linkedMapOf<String, String>()

        if (state.draftItems.isEmpty()) {
            errors["draftItems"] = "أضف صنفاً واحداً على الأقل"
        }

        if (state.selectedPaymentMethod == null) {
            errors["paymentMethod"] = "اختر طريقة الدفع"
        }

        if (state.totalAmount <= ZERO_MONEY) {
            errors["totalAmount"] = "إجمالي الفاتورة يجب أن يكون أكبر من الصفر"
        }

        if (state.invoiceNumber.isBlank()) {
            errors["invoiceNumber"] = "رقم الفاتورة مطلوب"
        }

        if (state.remainingAmount > ZERO_MONEY && !state.canUseCredit) {
            errors["credit"] = "العميل غير مؤهل للبيع الآجل أو يوجد رصيد غير مغطى"
        }

        if (state.remainingAmount > ZERO_MONEY && state.selectedCustomer == null) {
            errors["customer"] = "اختر عميلاً صالحاً قبل البيع الآجل"
        }

        return errors
    }

    private fun buildSale(state: SalesUiState): Sale {
        return Sale(
            invoiceNumber = state.invoiceNumber.trim(),
            customerId = state.selectedCustomer?.id,
            employeeId = state.selectedSale?.employeeId ?: DEFAULT_EMPLOYEE_ID,
            items = state.draftItems,
            paidAmount = state.paidAmount.money(),
            note = state.note.trim().takeIf { it.isNotBlank() }
        )
    }

    private fun currentSalesState(): SalesUiState {
        return currentData ?: SalesUiState()
    }

    private fun mutateState(transform: SalesUiState.() -> SalesUiState) {
        val current = currentSalesState()
        setSuccess(current.transform())
    }
}

private fun BigDecimal.money(): BigDecimal = setScale(2, RoundingMode.HALF_UP)

private fun String.toBigDecimalOrNull(): BigDecimal? {
    return runCatching { trim().toBigDecimal() }.getOrNull()
}

private fun Throwable.toUserMessage(): String {
    val msg = message?.trim().orEmpty()
    return when {
        msg.isNotBlank() -> msg
        this is IllegalArgumentException -> "البيانات المدخلة غير صحيحة"
        this is IllegalStateException -> "تعذر إكمال العملية الحالية"
        else -> DEFAULT_ERROR_MESSAGE
    }
}