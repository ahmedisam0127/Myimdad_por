package com.myimdad_por.ui.features.sales

import com.myimdad_por.domain.model.Customer
import com.myimdad_por.domain.model.PaymentMethod
import com.myimdad_por.domain.model.Product
import com.myimdad_por.domain.model.Sale
import com.myimdad_por.domain.model.SaleItem
import java.math.BigDecimal

data class SalesUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
    val currencyCode: String = "SDG",

    val sales: List<Sale> = emptyList(),
    val availableProducts: List<Product> = emptyList(),
    val customers: List<Customer> = emptyList(),

    val selectedCustomer: Customer? = null,
    val selectedPaymentMethod: PaymentMethod? = null,
    val selectedSale: Sale? = null,

    val invoiceNumber: String = "",
    val searchQuery: String = "",
    val note: String = "",

    val draftItems: List<SaleItem> = emptyList(),
    val paidAmount: BigDecimal = BigDecimal.ZERO,

    val validationErrors: Map<String, String> = emptyMap(),
    val canAccessCreditSales: Boolean = false,
    val canAccessReturns: Boolean = false,
    val isReadOnlyMode: Boolean = false
) {
    val subtotalAmount: BigDecimal
        get() = draftItems.fold(BigDecimal.ZERO) { acc, item ->
            acc.add(item.calculateSubtotal())
        }.money()

    val taxAmount: BigDecimal
        get() = draftItems.fold(BigDecimal.ZERO) { acc, item ->
            acc.add(item.netTaxAmount)
        }.money()

    val discountAmount: BigDecimal
        get() = draftItems.fold(BigDecimal.ZERO) { acc, item ->
            acc.add(item.netDiscountAmount)
        }.money()

    val totalAmount: BigDecimal
        get() = subtotalAmount
            .add(taxAmount)
            .subtract(discountAmount)
            .money()

    val remainingAmount: BigDecimal
        get() = (totalAmount - paidAmount).takeIf { it > BigDecimal.ZERO }?.money()
            ?: BigDecimal.ZERO.setScale(2)

    val changeAmount: BigDecimal
        get() = (paidAmount - totalAmount).takeIf { it > BigDecimal.ZERO }?.money()
            ?: BigDecimal.ZERO.setScale(2)

    val itemCount: Int
        get() = draftItems.size

    val totalQuantity: BigDecimal
        get() = draftItems.fold(BigDecimal.ZERO) { acc, item ->
            acc.add(item.quantity)
        }.money()

    val hasDraftItems: Boolean
        get() = draftItems.isNotEmpty()

    val hasContent: Boolean
        get() = sales.isNotEmpty() ||
            availableProducts.isNotEmpty() ||
            customers.isNotEmpty() ||
            draftItems.isNotEmpty()

    val hasError: Boolean
        get() = !errorMessage.isNullOrBlank()

    val canSubmit: Boolean
        get() = draftItems.isNotEmpty() &&
            selectedPaymentMethod != null &&
            !isLoading &&
            !isSubmitting &&
            validationErrors.isEmpty()

    val canUseCredit: Boolean
        get() = canAccessCreditSales && selectedCustomer != null

    val customerName: String
        get() = selectedCustomer?.displayName.orEmpty()

    val paymentMethodName: String
        get() = selectedPaymentMethod?.displayName.orEmpty()
}

enum class SalesMode {
    Browse,
    Create,
    Edit,
    Return
}

private fun BigDecimal.money(): BigDecimal = setScale(2, java.math.RoundingMode.HALF_UP)