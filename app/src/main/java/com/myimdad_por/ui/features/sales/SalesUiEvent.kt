package com.myimdad_por.ui.features.sales

import com.myimdad_por.domain.model.Customer
import com.myimdad_por.domain.model.PaymentMethod
import com.myimdad_por.domain.model.Product
import com.myimdad_por.domain.model.SaleItem

sealed interface SalesUiEvent {
    data object Load : SalesUiEvent
    data object Refresh : SalesUiEvent
    data object Retry : SalesUiEvent
    data object ClearError : SalesUiEvent

    data class ChangeSearchQuery(val query: String) : SalesUiEvent
    data class SelectCustomer(val customer: Customer?) : SalesUiEvent
    data class SelectPaymentMethod(val paymentMethod: PaymentMethod?) : SalesUiEvent
    data class SelectSale(val saleId: String) : SalesUiEvent

    data class AddProductToDraft(val product: Product, val quantity: Int = 1) : SalesUiEvent
    data class AddDraftItem(val item: SaleItem) : SalesUiEvent
    data class UpdateDraftItem(val item: SaleItem) : SalesUiEvent
    data class RemoveDraftItem(val itemId: String) : SalesUiEvent
    data object ClearDraftItems : SalesUiEvent

    data class UpdateInvoiceNumber(val invoiceNumber: String) : SalesUiEvent
    data class UpdateNote(val note: String) : SalesUiEvent
    data class UpdatePaidAmount(val amount: String) : SalesUiEvent

    data object SubmitSale : SalesUiEvent
    data object SaveDraft : SalesUiEvent
    data object DeleteSelectedSale : SalesUiEvent
    data object ResetForm : SalesUiEvent

    data object NavigateBack : SalesUiEvent
}