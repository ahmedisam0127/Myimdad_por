package com.myimdad_por.ui.features.sales.contract

import com.myimdad_por.core.base.UiState
import com.myimdad_por.core.utils.orZero
import com.myimdad_por.core.utils.roundTo
import com.myimdad_por.ui.features.sales.SalesAction
import com.myimdad_por.ui.features.sales.SalesConstants
import com.myimdad_por.ui.features.sales.SalesUiState
import com.myimdad_por.ui.features.sales.SalesValidator
import com.myimdad_por.ui.features.sales.models.CartUiModel
import com.myimdad_por.ui.features.sales.models.ProductUiModel
import com.myimdad_por.ui.features.sales.utils.asMoney
import com.myimdad_por.ui.features.sales.utils.calculateDiscountAmount
import com.myimdad_por.ui.features.sales.utils.calculateGrandTotal
import com.myimdad_por.ui.features.sales.utils.calculateSubtotal
import com.myimdad_por.ui.features.sales.utils.calculateTaxAmount
import com.myimdad_por.ui.features.sales.utils.formatCurrency
import com.myimdad_por.ui.features.sales.utils.formatQuantity
import com.myimdad_por.ui.features.sales.utils.increaseQuantity
import com.myimdad_por.ui.features.sales.utils.totalItemsCount
import java.math.BigDecimal

object SalesReducer {

    fun reduce(
        currentState: SalesUiState,
        action: SalesAction
    ): SalesUiState {
        return when (action) {

            is SalesAction.Initialize -> {
                currentState.copy(
                    uiState = UiState.Loading,
                    errorMessage = null,
                    message = null
                )
            }

            is SalesAction.Refresh -> {
                currentState.copy(
                    uiState = UiState.Loading,
                    errorMessage = null
                )
            }

            is SalesAction.Retry -> {
                currentState.copy(
                    uiState = UiState.Loading,
                    errorMessage = null
                )
            }

            is SalesAction.ClearError -> {
                currentState.copy(
                    errorMessage = null
                )
            }

            is SalesAction.ResetState -> {
                SalesUiState.initial()
            }

            is SalesAction.SearchProducts -> {
                reduceSearchProducts(
                    currentState = currentState,
                    query = action.normalizedQuery
                )
            }

            is SalesAction.ClearSearch -> {
                currentState.copy(
                    searchQuery = "",
                    filteredProducts = currentState.products,
                    isSearching = false
                )
            }

            is SalesAction.SelectProduct -> {
                addProductToCart(
                    currentState = currentState,
                    product = action.product,
                    quantity = BigDecimal.ONE
                )
            }

            is SalesAction.AddProductToCart -> {
                addProductToCart(
                    currentState = currentState,
                    product = action.product,
                    quantity = action.quantity
                )
            }

            is SalesAction.IncreaseQuantity -> {
                updateQuantity(
                    currentState = currentState,
                    cartItemId = action.cartItemId,
                    operation = { item ->
                        item.increaseQuantity()
                    }
                )
            }

            is SalesAction.DecreaseQuantity -> {
                decreaseQuantity(
                    currentState = currentState,
                    cartItemId = action.cartItemId
                )
            }

            is SalesAction.UpdateQuantity -> {
                changeItemQuantity(
                    currentState = currentState,
                    cartItemId = action.cartItemId,
                    quantity = action.quantity
                )
            }

            is SalesAction.UpdateQuantityFromInput -> {
                changeItemQuantity(
                    currentState = currentState,
                    cartItemId = action.cartItemId,
                    quantity = action.parsedQuantity
                )
            }

            is SalesAction.UpdateUnitPrice -> {
                updateItemPrice(
                    currentState = currentState,
                    cartItemId = action.cartItemId,
                    unitPrice = action.unitPrice
                )
            }

            is SalesAction.UpdateUnitPriceFromInput -> {
                updateItemPrice(
                    currentState = currentState,
                    cartItemId = action.cartItemId,
                    unitPrice = action.parsedPrice
                )
            }

            is SalesAction.UpdateDiscount -> {
                updateItemDiscount(
                    currentState = currentState,
                    cartItemId = action.cartItemId,
                    discountAmount = action.discountAmount
                )
            }

            is SalesAction.UpdateDiscountFromInput -> {
                updateItemDiscount(
                    currentState = currentState,
                    cartItemId = action.cartItemId,
                    discountAmount = action.parsedDiscount
                )
            }

            is SalesAction.UpdateTax -> {
                updateItemTax(
                    currentState = currentState,
                    cartItemId = action.cartItemId,
                    taxAmount = action.taxAmount
                )
            }

            is SalesAction.UpdateTaxFromInput -> {
                updateItemTax(
                    currentState = currentState,
                    cartItemId = action.cartItemId,
                    taxAmount = action.parsedTax
                )
            }

            is SalesAction.UpdateItemNote -> {
                updateItemNote(
                    currentState = currentState,
                    cartItemId = action.cartItemId,
                    note = action.normalizedNote
                )
            }

            is SalesAction.RemoveCartItem -> {
                removeCartItem(
                    currentState = currentState,
                    cartItemId = action.cartItemId
                )
            }

            is SalesAction.ClearCart -> {
                clearCart(currentState)
            }

            is SalesAction.SelectCustomer -> {
                currentState.copy(
                    selectedCustomer = action.customer,
                    message = "تم اختيار العميل بنجاح",
                    errorMessage = null
                )
            }

            is SalesAction.RemoveCustomer -> {
                currentState.copy(
                    selectedCustomer = null
                )
            }

            is SalesAction.SelectPaymentMethod -> {
                currentState.copy(
                    selectedPaymentMethod = action.paymentMethod
                )
            }

            is SalesAction.UpdatePaidAmount -> {
                updatePaidAmount(
                    currentState = currentState,
                    amount = action.amount
                )
            }

            is SalesAction.UpdatePaidAmountFromInput -> {
                updatePaidAmount(
                    currentState = currentState,
                    amount = action.parsedAmount
                )
            }

            is SalesAction.TogglePaymentSheet -> {
                currentState.copy(
                    isPaymentSheetVisible =
                        !currentState.isPaymentSheetVisible
                )
            }

            is SalesAction.ToggleCustomerSheet -> {
                currentState.copy(
                    isCustomerSheetVisible =
                        !currentState.isCustomerSheetVisible
                )
            }

            is SalesAction.OpenScanner -> {
                currentState.copy(
                    isScannerEnabled = true
                )
            }

            is SalesAction.CloseScanner -> {
                currentState.copy(
                    isScannerEnabled = false
                )
            }

            is SalesAction.ValidateSale -> {
                validateSale(currentState)
            }

            is SalesAction.CancelSale -> {
                SalesUiState.initial().copy(
                    message = "تم إلغاء عملية البيع"
                )
            }

            else -> currentState
        }
    }

    // ✅ الدالة المضافة بدلاً من filterByQuery / filterProductsByQuery
    private fun List<ProductUiModel>.filterProductsByQuery(
        query: String
    ): List<ProductUiModel> {
        if (query.isBlank()) return this
        val normalizedQuery = query.trim().lowercase()
        return filter { product ->
            product.name.lowercase().contains(normalizedQuery) ||
            product.displayName.lowercase().contains(normalizedQuery) ||
            product.barcode?.lowercase()?.contains(normalizedQuery) == true
        }
    }

    private fun reduceSearchProducts(
        currentState: SalesUiState,
        query: String
    ): SalesUiState {

        val filteredProducts = currentState.products
            .filterProductsByQuery(query)

        return currentState.copy(
            searchQuery = query,
            filteredProducts = filteredProducts,
            isSearching = false,
            message = null,
            errorMessage = null
        )
    }

    private fun addProductToCart(
        currentState: SalesUiState,
        product: ProductUiModel,
        quantity: BigDecimal
    ): SalesUiState {

        val validation = SalesValidator
            .validateProductUiModel(product)

        if (!validation.isValid) {
            return currentState.copy(
                errorMessage = validation.firstError
            )
        }

        val existingItem = currentState.cartItems
            .firstOrNull { item ->
                item.productId == product.id
            }

        val updatedCart = if (existingItem != null) {

            currentState.cartItems.map { item ->
                if (item.id == existingItem.id) {
                    item.increaseQuantity(quantity)
                } else {
                    item
                }
            }

        } else {

            currentState.cartItems + createCartItem(
                product = product,
                quantity = quantity
            )
        }

        return calculateCartState(
            currentState = currentState,
            cartItems = updatedCart,
            message = SalesConstants.Ui.ADD_TO_CART_SUCCESS
        )
    }

    private fun createCartItem(
        product: ProductUiModel,
        quantity: BigDecimal
    ): CartUiModel {

        val subtotal = product.price
            .multiply(quantity)
            .asMoney()

        return CartUiModel(
            id = "${product.id}_${System.currentTimeMillis()}",
            productId = product.id,
            productName = product.name,
            productDisplayName = product.displayName,
            barcode = product.barcode,
            unit = product.unitOfMeasure,
            quantity = quantity.asMoney(),
            unitPrice = product.price.asMoney(),
            taxAmount = BigDecimal.ZERO,
            discountAmount = BigDecimal.ZERO,
            subtotal = subtotal,
            totalAmount = subtotal,
            isReturn = false,
            note = null,
            formattedQuantity = quantity.formatQuantity(
                product.unitOfMeasure
            ),
            formattedUnitPrice = product.price.formatCurrency(),
            formattedTaxAmount = BigDecimal.ZERO.formatCurrency(),
            formattedDiscountAmount = BigDecimal.ZERO.formatCurrency(),
            formattedSubtotal = subtotal.formatCurrency(),
            formattedTotalAmount = subtotal.formatCurrency()
        )
    }

    private fun updateQuantity(
        currentState: SalesUiState,
        cartItemId: String,
        operation: (CartUiModel) -> CartUiModel
    ): SalesUiState {

        val updatedCart = currentState.cartItems.map { item ->
            if (item.id == cartItemId) {
                operation(item)
            } else {
                item
            }
        }

        return calculateCartState(
            currentState = currentState,
            cartItems = updatedCart
        )
    }

    private fun decreaseQuantity(
        currentState: SalesUiState,
        cartItemId: String
    ): SalesUiState {

        val updatedCart = currentState.cartItems.map { item ->

            if (item.id != cartItemId) {
                return@map item
            }

            val updatedQuantity = item.quantity
                .subtract(BigDecimal.ONE)
                .max(BigDecimal.ONE)

            recalculateCartItem(
                item = item,
                quantity = updatedQuantity
            )
        }

        return calculateCartState(
            currentState = currentState,
            cartItems = updatedCart
        )
    }

    private fun changeItemQuantity(
        currentState: SalesUiState,
        cartItemId: String,
        quantity: BigDecimal
    ): SalesUiState {

        val validation = SalesValidator
            .validateQuantity(quantity)

        if (!validation.isValid) {
            return currentState.copy(
                errorMessage = validation.firstError
            )
        }

        val updatedCart = currentState.cartItems.map { item ->
            if (item.id == cartItemId) {
                recalculateCartItem(
                    item = item,
                    quantity = quantity
                )
            } else {
                item
            }
        }

        return calculateCartState(
            currentState = currentState,
            cartItems = updatedCart
        )
    }

    private fun updateItemPrice(
        currentState: SalesUiState,
        cartItemId: String,
        unitPrice: BigDecimal
    ): SalesUiState {

        val validation = SalesValidator
            .validatePrice(unitPrice)

        if (!validation.isValid) {
            return currentState.copy(
                errorMessage = validation.firstError
            )
        }

        val updatedCart = currentState.cartItems.map { item ->
            if (item.id == cartItemId) {
                recalculateCartItem(
                    item = item,
                    unitPrice = unitPrice
                )
            } else {
                item
            }
        }

        return calculateCartState(
            currentState = currentState,
            cartItems = updatedCart
        )
    }

    private fun updateItemDiscount(
        currentState: SalesUiState,
        cartItemId: String,
        discountAmount: BigDecimal
    ): SalesUiState {

        val updatedCart = currentState.cartItems.map { item ->
            if (item.id == cartItemId) {
                recalculateCartItem(
                    item = item,
                    discountAmount = discountAmount
                )
            } else {
                item
            }
        }

        return calculateCartState(
            currentState = currentState,
            cartItems = updatedCart
        )
    }

    private fun updateItemTax(
        currentState: SalesUiState,
        cartItemId: String,
        taxAmount: BigDecimal
    ): SalesUiState {

        val updatedCart = currentState.cartItems.map { item ->
            if (item.id == cartItemId) {
                recalculateCartItem(
                    item = item,
                    taxAmount = taxAmount
                )
            } else {
                item
            }
        }

        return calculateCartState(
            currentState = currentState,
            cartItems = updatedCart
        )
    }

    private fun updateItemNote(
        currentState: SalesUiState,
        cartItemId: String,
        note: String
    ): SalesUiState {

        val updatedCart = currentState.cartItems.map { item ->
            if (item.id == cartItemId) {
                item.copy(note = note)
            } else {
                item
            }
        }

        return currentState.copy(
            cartItems = updatedCart
        )
    }

    private fun removeCartItem(
        currentState: SalesUiState,
        cartItemId: String
    ): SalesUiState {

        val updatedCart = currentState.cartItems
            .filterNot { item ->
                item.id == cartItemId
            }

        return calculateCartState(
            currentState = currentState,
            cartItems = updatedCart,
            message = SalesConstants.Ui.REMOVE_FROM_CART_SUCCESS
        )
    }

    private fun clearCart(
        currentState: SalesUiState
    ): SalesUiState {

        return calculateCartState(
            currentState = currentState,
            cartItems = emptyList()
        ).copy(
            message = "تم تفريغ السلة"
        )
    }

    private fun updatePaidAmount(
        currentState: SalesUiState,
        amount: BigDecimal
    ): SalesUiState {

        val paidAmount = amount
            .max(BigDecimal.ZERO)
            .asMoney()

        val remainingAmount = currentState.totalAmount
            .subtract(paidAmount)
            .max(BigDecimal.ZERO)
            .asMoney()

        return currentState.copy(
            paidAmount = paidAmount,
            remainingAmount = remainingAmount
        )
    }

    private fun validateSale(
        currentState: SalesUiState
    ): SalesUiState {

        if (currentState.cartItems.isEmpty()) {
            return currentState.copy(
                errorMessage = SalesConstants.Error.EMPTY_CART
            )
        }

        if (!currentState.canCompleteSale) {
            return currentState.copy(
                errorMessage = currentState.subscriptionMessage
                    ?: "لا يمكن إكمال عملية البيع"
            )
        }

        return currentState.copy(
            message = "البيانات جاهزة لإتمام عملية البيع",
            errorMessage = null
        )
    }

    private fun calculateCartState(
        currentState: SalesUiState,
        cartItems: List<CartUiModel>,
        message: String? = null
    ): SalesUiState {

        val subtotal = cartItems
            .calculateSubtotal()

        val taxAmount = cartItems
            .calculateTaxAmount()

        val discountAmount = cartItems
            .calculateDiscountAmount()

        val totalAmount = cartItems
            .calculateGrandTotal()

        val paidAmount = currentState.paidAmount
            .orZero()
            .roundTo()

        val remainingAmount = totalAmount
            .subtract(paidAmount)
            .max(BigDecimal.ZERO)
            .roundTo()

        return currentState.copy(
            cartItems = cartItems,
            subtotalAmount = subtotal,
            taxAmount = taxAmount,
            discountAmount = discountAmount,
            totalAmount = totalAmount,
            paidAmount = paidAmount,
            remainingAmount = remainingAmount,
            totalItemsCount = cartItems.totalItemsCount(),
            canCompleteSale = cartItems.isNotEmpty(),
            message = message,
            errorMessage = null
        )
    }

    private fun recalculateCartItem(
        item: CartUiModel,
        quantity: BigDecimal = item.quantity,
        unitPrice: BigDecimal = item.unitPrice,
        taxAmount: BigDecimal = item.taxAmount,
        discountAmount: BigDecimal = item.discountAmount
    ): CartUiModel {

        val subtotal = quantity
            .multiply(unitPrice)
            .asMoney()

        val total = subtotal
            .add(taxAmount)
            .subtract(discountAmount)
            .asMoney()

        return item.copy(
            quantity = quantity.asMoney(),
            unitPrice = unitPrice.asMoney(),
            taxAmount = taxAmount.asMoney(),
            discountAmount = discountAmount.asMoney(),
            subtotal = subtotal,
            totalAmount = total,
            formattedQuantity = quantity.formatQuantity(item.unit),
            formattedUnitPrice = unitPrice.formatCurrency(),
            formattedTaxAmount = taxAmount.formatCurrency(),
            formattedDiscountAmount = discountAmount.formatCurrency(),
            formattedSubtotal = subtotal.formatCurrency(),
            formattedTotalAmount = total.formatCurrency()
        )
    }
}