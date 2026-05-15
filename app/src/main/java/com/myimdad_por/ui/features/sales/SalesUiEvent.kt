package com.myimdad_por.ui.features.sales

import com.myimdad_por.core.base.UiEvent
import com.myimdad_por.domain.model.Customer
import com.myimdad_por.domain.model.PaymentMethod
import com.myimdad_por.ui.features.sales.models.BillUiModel
import com.myimdad_por.ui.features.sales.models.CartUiModel
import com.myimdad_por.ui.features.sales.models.ProductUiModel
import java.math.BigDecimal

sealed interface SalesUiEvent : UiEvent {

    /*
     * =========================
     * Lifecycle & Initialization
     * =========================
     */

    data object Initialize : SalesUiEvent

    data object RefreshData : SalesUiEvent

    data object Retry : SalesUiEvent

    data object ClearError : SalesUiEvent

    /*
     * =========================
     * Search & Filtering
     * =========================
     */

    data class SearchProducts(
        val query: String
    ) : SalesUiEvent

    data class SearchBills(
        val query: String
    ) : SalesUiEvent

    data object ClearSearch : SalesUiEvent

    /*
     * =========================
     * Product Actions
     * =========================
     */

    data class SelectProduct(
        val product: ProductUiModel
    ) : SalesUiEvent

    data class ScanBarcode(
        val barcode: String
    ) : SalesUiEvent

    data class AddProductToCart(
        val product: ProductUiModel,
        val quantity: BigDecimal = BigDecimal.ONE
    ) : SalesUiEvent

    data class QuickAddProduct(
        val barcode: String
    ) : SalesUiEvent

    /*
     * =========================
     * Cart Actions
     * =========================
     */

    data class IncreaseCartItemQuantity(
        val cartItemId: String
    ) : SalesUiEvent

    data class DecreaseCartItemQuantity(
        val cartItemId: String
    ) : SalesUiEvent

    data class UpdateCartItemQuantity(
        val cartItemId: String,
        val quantity: BigDecimal
    ) : SalesUiEvent

    data class UpdateCartItemPrice(
        val cartItemId: String,
        val unitPrice: BigDecimal
    ) : SalesUiEvent

    data class UpdateCartItemDiscount(
        val cartItemId: String,
        val discountAmount: BigDecimal
    ) : SalesUiEvent

    data class UpdateCartItemTax(
        val cartItemId: String,
        val taxAmount: BigDecimal
    ) : SalesUiEvent

    data class UpdateCartItemNote(
        val cartItemId: String,
        val note: String
    ) : SalesUiEvent

    data class RemoveCartItem(
        val cartItemId: String
    ) : SalesUiEvent

    data class SelectCartItem(
        val cartItem: CartUiModel
    ) : SalesUiEvent

    data object ClearCart : SalesUiEvent

    /*
     * =========================
     * Customer Actions
     * =========================
     */

    data class SelectCustomer(
        val customer: Customer
    ) : SalesUiEvent

    data object RemoveCustomer : SalesUiEvent

    data class SearchCustomers(
        val query: String
    ) : SalesUiEvent

    /*
     * =========================
     * Payment Actions
     * =========================
     */

    data class SelectPaymentMethod(
        val paymentMethod: PaymentMethod
    ) : SalesUiEvent

    data class UpdatePaidAmount(
        val amount: BigDecimal
    ) : SalesUiEvent

    data class UpdateReferenceNumber(
        val reference: String
    ) : SalesUiEvent

    data class ToggleCreditSale(
        val enabled: Boolean
    ) : SalesUiEvent

    /*
     * =========================
     * Invoice & Notes
     * =========================
     */

    // تغيير الاسم من UpdateInvoiceNotes إلى UpdateNotes ليطابق الـ ViewModel
    data class UpdateNotes(
        val notes: String
    ) : SalesUiEvent

    // إضافة هذا الحدث لأنه مفقود تماماً وكان يسبب خطأ
    data class UpdateTaxRate(
        val rate: BigDecimal
    ) : SalesUiEvent

    data class UpdateInvoiceTerms(
        val terms: String
    ) : SalesUiEvent

    /*
     * =========================
     * Checkout Flow
     * =========================
     */

    data object ValidateSale : SalesUiEvent

    data object CompleteSale : SalesUiEvent

    data object ConfirmSale : SalesUiEvent

    data object CancelSale : SalesUiEvent

    data object CreateDraftInvoice : SalesUiEvent

    /*
     * =========================
     * Subscription Flow
     * =========================
     */

    data object CheckSubscriptionStatus : SalesUiEvent

    data object RequestSubscriptionUpgrade : SalesUiEvent

    data object OpenSubscriptionScreen : SalesUiEvent

    /*
     * =========================
     * Bills & Receipts
     * =========================
     */

    data class OpenBillDetails(
        val billId: String
    ) : SalesUiEvent

    data class PrintInvoice(
        val bill: BillUiModel
    ) : SalesUiEvent

    data class ShareInvoice(
        val bill: BillUiModel
    ) : SalesUiEvent

    data class DownloadInvoicePdf(
        val bill: BillUiModel
    ) : SalesUiEvent

    /*
     * =========================
     * UI Actions
     * =========================
     */

    data object OpenScanner : SalesUiEvent

    data object CloseScanner : SalesUiEvent

    data object ToggleProductsSheet : SalesUiEvent

    data object TogglePaymentSheet : SalesUiEvent

    data object ToggleCustomerSheet : SalesUiEvent

    data object DismissDialogs : SalesUiEvent

    /*
     * =========================
     * Navigation
     * =========================
     */

    data object NavigateBack : SalesUiEvent

    data object NavigateToReports : SalesUiEvent

    data object NavigateToCustomers : SalesUiEvent

    data object NavigateToProducts : SalesUiEvent

    /*
     * =========================
     * Feedback Events
     * =========================
     */

    data class ShowSuccessMessage(
        val message: String
    ) : SalesUiEvent

    data class ShowErrorMessage(
        val message: String,
        val throwable: Throwable? = null
    ) : SalesUiEvent
}