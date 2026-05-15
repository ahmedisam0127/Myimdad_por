package com.myimdad_por.ui.features.sales.contract

import androidx.compose.runtime.Immutable
import com.myimdad_por.core.base.UiEvent
import com.myimdad_por.core.base.UiState
import com.myimdad_por.core.payment.models.PaymentResult
import com.myimdad_por.domain.model.Customer
import com.myimdad_por.domain.model.PaymentMethod
import com.myimdad_por.domain.model.SaleInvoice
import com.myimdad_por.domain.model.SubscriptionInfo
import com.myimdad_por.ui.features.sales.SalesAction
import com.myimdad_por.ui.features.sales.SalesEffect
import com.myimdad_por.ui.features.sales.SalesUiEvent
import com.myimdad_por.ui.features.sales.SalesUiState
import com.myimdad_por.ui.features.sales.models.BillUiModel
import com.myimdad_por.ui.features.sales.models.CartUiModel
import com.myimdad_por.ui.features.sales.models.ProductUiModel
import java.math.BigDecimal

typealias SalesState = SalesUiState
typealias SalesEvent = SalesUiEvent
typealias SalesActionContract = SalesAction
typealias SalesEffectContract = SalesEffect

object SalesContract {

    @Immutable
    data class ViewState(
        val uiState: UiState<Unit> = UiState.Idle,
        val subscriptionInfo: SubscriptionInfo? = null,
        val canCompleteSale: Boolean = false,
        val isSubscriptionExpired: Boolean = false,
        val requiresSubscription: Boolean = true,
        val subscriptionMessage: String? = null,
        val searchQuery: String = "",
        val barcodeQuery: String = "",
        val products: List<ProductUiModel> = emptyList(),
        val filteredProducts: List<ProductUiModel> = emptyList(),
        val cartItems: List<CartUiModel> = emptyList(),
        val selectedCustomer: Customer? = null,
        val selectedPaymentMethod: PaymentMethod = PaymentMethod.CASH,
        val currentBill: BillUiModel? = null,
        val isSearching: Boolean = false,
        val isSubmittingSale: Boolean = false,
        val isLoadingProducts: Boolean = false,
        val isLoadingCustomers: Boolean = false,
        val isCartExpanded: Boolean = false,
        val isPaymentSheetVisible: Boolean = false,
        val isCustomerSheetVisible: Boolean = false,
        val isScannerEnabled: Boolean = false,
        val message: String? = null,
        val errorMessage: String? = null,
        val subtotalAmount: BigDecimal = BigDecimal.ZERO,
        val taxAmount: BigDecimal = BigDecimal.ZERO,
        val discountAmount: BigDecimal = BigDecimal.ZERO,
        val totalAmount: BigDecimal = BigDecimal.ZERO,
        val paidAmount: BigDecimal = BigDecimal.ZERO,
        val remainingAmount: BigDecimal = BigDecimal.ZERO,
        val totalItemsCount: Int = 0,
        val lastCompletedBill: BillUiModel? = null
    ) {

        val isCartEmpty: Boolean
            get() = cartItems.isEmpty()

        val hasCustomer: Boolean
            get() = selectedCustomer != null

        val hasProducts: Boolean
            get() = products.isNotEmpty()

        val hasSearchResults: Boolean
            get() = filteredProducts.isNotEmpty()

        val hasError: Boolean
            get() = !errorMessage.isNullOrBlank()

        val hasMessage: Boolean
            get() = !message.isNullOrBlank()

        val isSaleReady: Boolean
            get() = canCompleteSale &&
                !isCartEmpty &&
                totalAmount > BigDecimal.ZERO &&
                !isSubmittingSale

        val isPaymentCompleted: Boolean
            get() = paidAmount >= totalAmount &&
                totalAmount > BigDecimal.ZERO

        val hasReturns: Boolean
            get() = cartItems.any { it.isReturn }

        val totalQuantity: BigDecimal
            get() = cartItems.fold(BigDecimal.ZERO) { acc, item ->
                acc + item.quantity
            }

        companion object {

            fun initial(): ViewState {
                return ViewState()
            }

            fun fromState(
                state: SalesUiState
            ): ViewState {
                return ViewState(
                    uiState = state.uiState,
                    subscriptionInfo = state.subscriptionInfo,
                    canCompleteSale = state.canCompleteSale,
                    isSubscriptionExpired = state.isSubscriptionExpired,
                    requiresSubscription = state.requiresSubscription,
                    subscriptionMessage = state.subscriptionMessage,
                    searchQuery = state.searchQuery,
                    barcodeQuery = state.barcodeQuery,
                    products = state.products,
                    filteredProducts = state.filteredProducts,
                    cartItems = state.cartItems,
                    selectedCustomer = state.selectedCustomer,
                    selectedPaymentMethod = state.selectedPaymentMethod,
                    currentBill = state.currentBill,
                    isSearching = state.isSearching,
                    isSubmittingSale = state.isSubmittingSale,
                    isLoadingProducts = state.isLoadingProducts,
                    isLoadingCustomers = state.isLoadingCustomers,
                    isCartExpanded = state.isCartExpanded,
                    isPaymentSheetVisible = state.isPaymentSheetVisible,
                    isCustomerSheetVisible = state.isCustomerSheetVisible,
                    isScannerEnabled = state.isScannerEnabled,
                    message = state.message,
                    errorMessage = state.errorMessage,
                    subtotalAmount = state.subtotalAmount,
                    taxAmount = state.taxAmount,
                    discountAmount = state.discountAmount,
                    totalAmount = state.totalAmount,
                    paidAmount = state.paidAmount,
                    remainingAmount = state.remainingAmount,
                    totalItemsCount = state.totalItemsCount,
                    lastCompletedBill = state.lastCompletedBill
                )
            }
        }
    }

    interface Reducer {

        fun reduce(
            currentState: ViewState,
            action: SalesAction
        ): ViewState
    }

    interface Processor {

        suspend fun process(
            action: SalesAction,
            currentState: ViewState
        ): SalesEffect?
    }

    interface Mapper {

        fun mapEventToAction(
            event: SalesUiEvent
        ): SalesAction?

        fun mapUiEventToEffect(
            event: UiEvent
        ): SalesEffect {
            return SalesEffect.fromUiEvent(event)
        }
    }

    interface Validator {

        fun validateSale(
            state: ViewState
        ): ValidationResult

        fun validateCart(
            cartItems: List<CartUiModel>
        ): ValidationResult

        fun validatePayment(
            totalAmount: BigDecimal,
            paidAmount: BigDecimal,
            paymentMethod: PaymentMethod
        ): ValidationResult
    }

    data class ValidationResult(
        val isValid: Boolean,
        val message: String? = null,
        val throwable: Throwable? = null
    ) {

        companion object {

            fun success(): ValidationResult {
                return ValidationResult(
                    isValid = true
                )
            }

            fun failure(
                message: String,
                throwable: Throwable? = null
            ): ValidationResult {
                return ValidationResult(
                    isValid = false,
                    message = message,
                    throwable = throwable
                )
            }
        }
    }

    data class SaleCompletionResult(
        val invoice: SaleInvoice? = null,
        val paymentResult: PaymentResult? = null,
        val bill: BillUiModel? = null,
        val success: Boolean,
        val message: String
    ) {

        companion object {

            fun success(
                invoice: SaleInvoice,
                bill: BillUiModel? = null,
                paymentResult: PaymentResult? = null,
                message: String = "تمت عملية البيع بنجاح"
            ): SaleCompletionResult {
                return SaleCompletionResult(
                    invoice = invoice,
                    paymentResult = paymentResult,
                    bill = bill,
                    success = true,
                    message = message
                )
            }

            fun failure(
                message: String,
                paymentResult: PaymentResult? = null
            ): SaleCompletionResult {
                return SaleCompletionResult(
                    success = false,
                    message = message,
                    paymentResult = paymentResult
                )
            }
        }
    }
}