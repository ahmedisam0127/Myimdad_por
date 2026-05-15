package com.myimdad_por.ui.features.sales

import com.myimdad_por.core.base.UiEvent
import com.myimdad_por.core.payment.models.PaymentResult
import com.myimdad_por.domain.model.Customer
import com.myimdad_por.domain.model.PaymentMethod
import com.myimdad_por.domain.model.SaleInvoice
import com.myimdad_por.ui.features.sales.models.BillUiModel
import java.math.BigDecimal

sealed interface SalesEffect : UiEvent {

    /* -------------------------------------------------------------------------
     * Loading & State
     * ------------------------------------------------------------------------- */

    data object LoadingStarted : SalesEffect

    data object LoadingFinished : SalesEffect

    data object SaleValidationSucceeded : SalesEffect

    data class SaleValidationFailed(
        val reason: String
    ) : SalesEffect

    data object CartUpdated : SalesEffect

    data object CartCleared : SalesEffect

    /* -------------------------------------------------------------------------
     * Messages
     * ------------------------------------------------------------------------- */

    data class ShowSuccess(
        val message: String
    ) : SalesEffect

    data class ShowWarning(
        val message: String
    ) : SalesEffect

    data class ShowInfo(
        val message: String
    ) : SalesEffect

    data class ShowFailure(
        val message: String,
        val throwable: Throwable? = null
    ) : SalesEffect

    /* -------------------------------------------------------------------------
     * Product Effects
     * ------------------------------------------------------------------------- */

    data class ProductAddedToCart(
        val productName: String,
        val quantity: BigDecimal
    ) : SalesEffect

    data class ProductAlreadyExists(
        val productName: String
    ) : SalesEffect

    data class ProductNotFound(
        val barcode: String
    ) : SalesEffect

    data class BarcodeScanned(
        val barcode: String
    ) : SalesEffect

    /* -------------------------------------------------------------------------
     * Cart Effects
     * ------------------------------------------------------------------------- */

    data class CartItemRemoved(
        val productName: String
    ) : SalesEffect

    data class QuantityUpdated(
        val itemId: String,
        val quantity: BigDecimal
    ) : SalesEffect

    data class PriceUpdated(
        val itemId: String,
        val unitPrice: BigDecimal
    ) : SalesEffect

    data class DiscountApplied(
        val itemId: String,
        val discountAmount: BigDecimal
    ) : SalesEffect

    data class TaxApplied(
        val itemId: String,
        val taxAmount: BigDecimal
    ) : SalesEffect

    /* -------------------------------------------------------------------------
     * Customer Effects
     * ------------------------------------------------------------------------- */

    data class CustomerAssigned(
        val customer: Customer
    ) : SalesEffect

    data object CustomerRemoved : SalesEffect

    data class CreditLimitExceeded(
        val customerName: String,
        val availableCredit: BigDecimal
    ) : SalesEffect

    /* -------------------------------------------------------------------------
     * Payment Effects
     * ------------------------------------------------------------------------- */

    data class PaymentMethodSelected(
        val paymentMethod: PaymentMethod
    ) : SalesEffect

    data class PaymentCompleted(
        val result: PaymentResult.Success
    ) : SalesEffect

    data class PaymentPending(
        val result: PaymentResult.Pending
    ) : SalesEffect

    data class PaymentRequiresAction(
        val result: PaymentResult.RequiresAction
    ) : SalesEffect

    data class PaymentFailed(
        val result: PaymentResult.Failure
    ) : SalesEffect

    data class ChangeCalculated(
        val paidAmount: BigDecimal,
        val totalAmount: BigDecimal,
        val changeAmount: BigDecimal
    ) : SalesEffect

    /* -------------------------------------------------------------------------
     * Invoice Effects
     * ------------------------------------------------------------------------- */

    data class DraftInvoiceCreated(
        val invoice: SaleInvoice
    ) : SalesEffect

    data class InvoiceGenerated(
        val invoice: SaleInvoice
    ) : SalesEffect

    data class InvoicePrinted(
        val invoice: BillUiModel
    ) : SalesEffect

    data class InvoiceShared(
        val invoice: BillUiModel
    ) : SalesEffect

    data class InvoicePdfDownloaded(
        val invoice: BillUiModel,
        val filePath: String
    ) : SalesEffect

    /* -------------------------------------------------------------------------
     * Subscription Effects
     * ------------------------------------------------------------------------- */

    data object SubscriptionValid : SalesEffect

    data class SubscriptionExpired(
        val message: String
    ) : SalesEffect

    data object SubscriptionUpgradeRequired : SalesEffect

    /* -------------------------------------------------------------------------
     * Scanner Effects
     * ------------------------------------------------------------------------- */

    data object ScannerOpened : SalesEffect

    data object ScannerClosed : SalesEffect

    /* -------------------------------------------------------------------------
     * Navigation Effects
     * ------------------------------------------------------------------------- */

    data object NavigateBack : SalesEffect

    data object NavigateToProducts : SalesEffect

    data object NavigateToCustomers : SalesEffect

    data object NavigateToReports : SalesEffect

    data class NavigateToInvoiceDetails(
        val invoiceId: String
    ) : SalesEffect

    data class NavigateToSaleDetails(
        val saleId: String
    ) : SalesEffect

    data object NavigateToSubscription : SalesEffect

    /* -------------------------------------------------------------------------
     * External Actions
     * ------------------------------------------------------------------------- */

    data class OpenExternalUrl(
        val url: String
    ) : SalesEffect

    data class ShareText(
        val content: String
    ) : SalesEffect

    data class CopyToClipboard(
        val label: String,
        val value: String
    ) : SalesEffect

    /* -------------------------------------------------------------------------
     * UI Controls
     * ------------------------------------------------------------------------- */

    data object HideKeyboard : SalesEffect

    data object DismissDialogs : SalesEffect

    data object DismissBottomSheets : SalesEffect

    data object FocusBarcodeField : SalesEffect

    data object FocusSearchField : SalesEffect

    /* -------------------------------------------------------------------------
     * Session Effects
     * ------------------------------------------------------------------------- */

    data object SessionExpired : SalesEffect

    data object FinishScreen : SalesEffect

    companion object {

        fun fromUiEvent(
            event: UiEvent
        ): SalesEffect {
            return when (event) {

                is UiEvent.ShowMessage -> {
                    ShowInfo(event.message)
                }

                is UiEvent.ShowError -> {
                    ShowFailure(
                        message = event.message,
                        throwable = event.throwable
                    )
                }

                UiEvent.NavigateBack -> {
                    NavigateBack
                }

                UiEvent.HideKeyboard -> {
                    HideKeyboard
                }

                UiEvent.Finish -> {
                    FinishScreen
                }

                is UiEvent.OpenUrl -> {
                    OpenExternalUrl(event.url)
                }

                is UiEvent.NavigateTo -> {
                    ShowInfo(
                        message = "Navigate to: ${event.route}"
                    )
                }

                else -> {
                    ShowFailure(
                        message = "Unsupported UiEvent"
                    )
                }
            }
        }
    }
}