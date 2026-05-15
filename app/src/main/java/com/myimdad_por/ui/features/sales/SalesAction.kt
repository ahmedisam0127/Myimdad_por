package com.myimdad_por.ui.features.sales

import com.myimdad_por.core.utils.toBigDecimalOrZero
import com.myimdad_por.domain.model.Customer
import com.myimdad_por.domain.model.PaymentMethod
import com.myimdad_por.ui.features.sales.models.BillUiModel
import com.myimdad_por.ui.features.sales.models.CartUiModel
import com.myimdad_por.ui.features.sales.models.ProductUiModel
import java.math.BigDecimal

sealed interface SalesAction {

    /* -------------------------------------------------------------------------
     * Lifecycle Actions
     * ------------------------------------------------------------------------- */

    data object Initialize : SalesAction

    data object Refresh : SalesAction

    data object Retry : SalesAction

    data object ClearError : SalesAction

    data object ResetState : SalesAction

    /* -------------------------------------------------------------------------
     * Search Actions
     * ------------------------------------------------------------------------- */

    data class SearchProducts(
        val query: String
    ) : SalesAction {
        val normalizedQuery: String
            get() = query.trim()
    }

    data class SearchCustomers(
        val query: String
    ) : SalesAction {
        val normalizedQuery: String
            get() = query.trim()
    }

    data class SearchBills(
        val query: String
    ) : SalesAction {
        val normalizedQuery: String
            get() = query.trim()
    }

    data object ClearSearch : SalesAction

    /* -------------------------------------------------------------------------
     * Product Actions
     * ------------------------------------------------------------------------- */

    data class SelectProduct(
        val product: ProductUiModel
    ) : SalesAction

    data class ScanBarcode(
        val barcode: String
    ) : SalesAction {
        val normalizedBarcode: String
            get() = barcode.trim()
    }

    data class QuickAddProduct(
        val barcode: String
    ) : SalesAction {
        val normalizedBarcode: String
            get() = barcode.trim()
    }

    data class AddProductToCart(
        val product: ProductUiModel,
        val quantity: BigDecimal = BigDecimal.ONE
    ) : SalesAction {
        init {
            require(quantity > BigDecimal.ZERO) {
                "quantity must be greater than zero"
            }
        }
    }

    /* -------------------------------------------------------------------------
     * Cart Actions
     * ------------------------------------------------------------------------- */

    data class SelectCartItem(
        val item: CartUiModel
    ) : SalesAction

    data class IncreaseQuantity(
        val cartItemId: String
    ) : SalesAction

    data class DecreaseQuantity(
        val cartItemId: String
    ) : SalesAction

    data class UpdateQuantity(
        val cartItemId: String,
        val quantity: BigDecimal
    ) : SalesAction {
        init {
            require(quantity >= BigDecimal.ZERO) {
                "quantity cannot be negative"
            }
        }
    }

    data class UpdateQuantityFromInput(
        val cartItemId: String,
        val quantity: String
    ) : SalesAction {
        val parsedQuantity: BigDecimal
            get() = quantity.toBigDecimalOrZero()
    }

    data class UpdateUnitPrice(
        val cartItemId: String,
        val unitPrice: BigDecimal
    ) : SalesAction {
        init {
            require(unitPrice >= BigDecimal.ZERO) {
                "unitPrice cannot be negative"
            }
        }
    }

    data class UpdateUnitPriceFromInput(
        val cartItemId: String,
        val unitPrice: String
    ) : SalesAction {
        val parsedPrice: BigDecimal
            get() = unitPrice.toBigDecimalOrZero()
    }

    data class UpdateDiscount(
        val cartItemId: String,
        val discountAmount: BigDecimal
    ) : SalesAction {
        init {
            require(discountAmount >= BigDecimal.ZERO) {
                "discountAmount cannot be negative"
            }
        }
    }

    data class UpdateDiscountFromInput(
        val cartItemId: String,
        val discountAmount: String
    ) : SalesAction {
        val parsedDiscount: BigDecimal
            get() = discountAmount.toBigDecimalOrZero()
    }

    data class UpdateTax(
        val cartItemId: String,
        val taxAmount: BigDecimal
    ) : SalesAction {
        init {
            require(taxAmount >= BigDecimal.ZERO) {
                "taxAmount cannot be negative"
            }
        }
    }

    data class UpdateTaxFromInput(
        val cartItemId: String,
        val taxAmount: String
    ) : SalesAction {
        val parsedTax: BigDecimal
            get() = taxAmount.toBigDecimalOrZero()
    }

    data class UpdateItemNote(
        val cartItemId: String,
        val note: String
    ) : SalesAction {
        val normalizedNote: String
            get() = note.trim()
    }

    data class RemoveCartItem(
        val cartItemId: String
    ) : SalesAction

    data object ClearCart : SalesAction

    /* -------------------------------------------------------------------------
     * Customer Actions
     * ------------------------------------------------------------------------- */

    data class SelectCustomer(
        val customer: Customer
    ) : SalesAction

    data object RemoveCustomer : SalesAction

    /* -------------------------------------------------------------------------
     * Payment Actions
     * ------------------------------------------------------------------------- */

    data class SelectPaymentMethod(
        val paymentMethod: PaymentMethod
    ) : SalesAction

    data class UpdatePaidAmount(
        val amount: BigDecimal
    ) : SalesAction {
        init {
            require(amount >= BigDecimal.ZERO) {
                "amount cannot be negative"
            }
        }
    }

    data class UpdatePaidAmountFromInput(
        val amount: String
    ) : SalesAction {
        val parsedAmount: BigDecimal
            get() = amount.toBigDecimalOrZero()
    }

    data class UpdateReferenceNumber(
        val reference: String
    ) : SalesAction {
        val normalizedReference: String
            get() = reference.trim()
    }

    data class ToggleCreditSale(
        val enabled: Boolean
    ) : SalesAction

    /* -------------------------------------------------------------------------
     * Invoice Actions
     * ------------------------------------------------------------------------- */

    data class UpdateNotes(
        val notes: String
    ) : SalesAction {
        val normalizedNotes: String
            get() = notes.trim()
    }

    data class UpdateTaxRate(
        val rate: java.math.BigDecimal
    ) : SalesAction

    data class UpdateInvoiceTerms(
        val terms: String
    ) : SalesAction {
        val normalizedTerms: String
            get() = terms.trim()
    }

    data object CreateDraftInvoice : SalesAction

    data object ValidateSale : SalesAction

    data object ConfirmSale : SalesAction

    data object CompleteSale : SalesAction

    data object CancelSale : SalesAction

    /* -------------------------------------------------------------------------
     * Bill Actions
     * ------------------------------------------------------------------------- */

    data class OpenBillDetails(
        val billId: String
    ) : SalesAction

    data class PrintInvoice(
        val bill: BillUiModel
    ) : SalesAction

    data class ShareInvoice(
        val bill: BillUiModel
    ) : SalesAction

    data class DownloadInvoicePdf(
        val bill: BillUiModel
    ) : SalesAction

    /* -------------------------------------------------------------------------
     * Subscription Actions
     * ------------------------------------------------------------------------- */

    data object CheckSubscriptionStatus : SalesAction

    data object RequestSubscriptionUpgrade : SalesAction

    data object OpenSubscriptionScreen : SalesAction

    /* -------------------------------------------------------------------------
     * UI Actions
     * ------------------------------------------------------------------------- */

    data object OpenScanner : SalesAction

    data object CloseScanner : SalesAction

    data object ToggleProductsSheet : SalesAction

    data object TogglePaymentSheet : SalesAction

    data object ToggleCustomerSheet : SalesAction

    data object DismissDialogs : SalesAction

    /* -------------------------------------------------------------------------
     * Navigation Actions
     * ------------------------------------------------------------------------- */

    data object NavigateBack : SalesAction

    data object NavigateToProducts : SalesAction

    data object NavigateToCustomers : SalesAction

    data object NavigateToReports : SalesAction
}