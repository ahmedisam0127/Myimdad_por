package com.myimdad_por.ui.features.sales
import com.myimdad_por.core.utils.CurrencyFormatter
import androidx.lifecycle.viewModelScope
import com.myimdad_por.core.base.BaseViewModel
import com.myimdad_por.core.base.UiState
import com.myimdad_por.domain.model.PaymentStatus
import com.myimdad_por.domain.model.Sale
import com.myimdad_por.domain.model.SaleInvoice
import com.myimdad_por.domain.model.SaleInvoiceStatus
import com.myimdad_por.domain.model.SaleItem
import com.myimdad_por.domain.model.SaleStatus
import com.myimdad_por.domain.usecase.GetCustomersUseCase
import com.myimdad_por.domain.usecase.ProcessSaleRequest
import com.myimdad_por.domain.usecase.ProcessSaleResult
import com.myimdad_por.domain.usecase.ProcessSaleUseCase
import com.myimdad_por.domain.usecase.Product.GetProductsUseCase
import com.myimdad_por.domain.usecase.Product.SearchProductsUseCase
import com.myimdad_por.ui.features.sales.contract.SalesReducer
import com.myimdad_por.domain.usecase.GetInventoryUseCase

import com.myimdad_por.ui.features.sales.models.BillUiModel
import com.myimdad_por.ui.features.sales.models.CartUiModel
import com.myimdad_por.ui.features.sales.models.ProductUiModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID
import javax.inject.Inject

// ─────────────────────────────────────────────────────────────────────────────
// SalesViewModel
// ─────────────────────────────────────────────────────────────────────────────

@HiltViewModel
class SalesViewModel @Inject constructor(
    private val getProductsUseCase: GetProductsUseCase,
    private val getInventoryUseCase: GetInventoryUseCase,
    private val searchProductsUseCase: SearchProductsUseCase,
    @Suppress("UnusedPrivateMember")
    private val getCustomersUseCase: GetCustomersUseCase,
    private val processSaleUseCase: ProcessSaleUseCase
) : BaseViewModel<Unit>() {

    // ── State ─────────────────────────────────────────────────────────────────

    private val _salesState = MutableStateFlow(SalesUiState.initial())
    val salesState: StateFlow<SalesUiState> = _salesState.asStateFlow()

    // ── One-shot effect channel ───────────────────────────────────────────────

    private val _effect = MutableSharedFlow<SalesEffect>(
        replay = 0,
        extraBufferCapacity = 16
    )
    val effect: SharedFlow<SalesEffect> = _effect.asSharedFlow()

    // ── Internal jobs ─────────────────────────────────────────────────────────

    private var searchJob: Job? = null

    // ── Init ──────────────────────────────────────────────────────────────────

    init {
        onEvent(SalesUiEvent.Initialize)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Public API
    // ─────────────────────────────────────────────────────────────────────────

    fun onEvent(event: SalesUiEvent) {
        val action = mapEventToAction(event) ?: return
        _salesState.value = SalesReducer.reduce(_salesState.value, action)
        handleSideEffects(action, _salesState.value)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Event → Action mapping
    // ─────────────────────────────────────────────────────────────────────────

@Suppress("CyclomaticComplexMethod", "LongMethod")
private fun mapEventToAction(
    event: SalesUiEvent
): SalesAction? = when (event) {

    // ─────────────────────────────────────────────────────────────
    // Lifecycle
    // ─────────────────────────────────────────────────────────────

    SalesUiEvent.Initialize ->
        SalesAction.Initialize

    SalesUiEvent.RefreshData ->
        SalesAction.Refresh

    SalesUiEvent.Retry ->
        SalesAction.Retry

    SalesUiEvent.ClearError ->
        SalesAction.ClearError

    // ─────────────────────────────────────────────────────────────
    // Search / Scan
    // ─────────────────────────────────────────────────────────────

    is SalesUiEvent.SearchProducts ->
        SalesAction.SearchProducts(
            query = event.query
        )

    SalesUiEvent.ClearSearch ->
        SalesAction.ClearSearch

    is SalesUiEvent.ScanBarcode ->
        SalesAction.ScanBarcode(
            barcode = event.barcode
        )

    is SalesUiEvent.QuickAddProduct ->
        SalesAction.QuickAddProduct(
            barcode = event.barcode
        )

    // ─────────────────────────────────────────────────────────────
    // Cart
    // ─────────────────────────────────────────────────────────────

    is SalesUiEvent.SelectProduct ->
        SalesAction.SelectProduct(
            event.product
        )

    is SalesUiEvent.AddProductToCart ->
        SalesAction.AddProductToCart(
            event.product,
            event.quantity
        )

    is SalesUiEvent.IncreaseCartItemQuantity ->
        SalesAction.IncreaseQuantity(
            event.cartItemId
        )

    is SalesUiEvent.DecreaseCartItemQuantity ->
        SalesAction.DecreaseQuantity(
            event.cartItemId
        )

    is SalesUiEvent.UpdateCartItemQuantity ->
        SalesAction.UpdateQuantity(
            event.cartItemId,
            event.quantity
        )

    is SalesUiEvent.UpdateCartItemPrice ->
        SalesAction.UpdateUnitPrice(
            event.cartItemId,
            event.unitPrice
        )

    is SalesUiEvent.UpdateCartItemDiscount ->
        SalesAction.UpdateDiscount(
            event.cartItemId,
            event.discountAmount
        )

    is SalesUiEvent.UpdateCartItemTax ->
        SalesAction.UpdateTax(
            event.cartItemId,
            event.taxAmount
        )

    is SalesUiEvent.UpdateCartItemNote ->
        SalesAction.UpdateItemNote(
            cartItemId = event.cartItemId,
            note = event.note
        )

    is SalesUiEvent.RemoveCartItem ->
        SalesAction.RemoveCartItem(
            event.cartItemId
        )

    SalesUiEvent.ClearCart ->
        SalesAction.ClearCart

    // ─────────────────────────────────────────────────────────────
    // Customer
    // ─────────────────────────────────────────────────────────────

    is SalesUiEvent.SelectCustomer ->
        SalesAction.SelectCustomer(
            event.customer
        )

    SalesUiEvent.RemoveCustomer ->
        SalesAction.RemoveCustomer

    is SalesUiEvent.SearchCustomers ->
        null

    // ─────────────────────────────────────────────────────────────
    // Payment
    // ─────────────────────────────────────────────────────────────

    is SalesUiEvent.SelectPaymentMethod ->
        SalesAction.SelectPaymentMethod(
            event.paymentMethod
        )

    is SalesUiEvent.UpdatePaidAmount ->
        SalesAction.UpdatePaidAmount(
            event.amount
        )

    is SalesUiEvent.UpdateReferenceNumber ->
        SalesAction.UpdateReferenceNumber(
            event.reference
        )

    is SalesUiEvent.ToggleCreditSale ->
        SalesAction.ToggleCreditSale(
            event.enabled
        )

    is SalesUiEvent.UpdateTaxRate ->
        SalesAction.UpdateTaxRate(
            event.rate
        )

    // ─────────────────────────────────────────────────────────────
    // Invoice Meta
    // ─────────────────────────────────────────────────────────────

    

    is SalesUiEvent.UpdateNotes ->
        SalesAction.UpdateNotes(
            event.notes
        )

    is SalesUiEvent.UpdateInvoiceTerms ->
        SalesAction.UpdateInvoiceTerms(
            event.terms
        )

    SalesUiEvent.CreateDraftInvoice ->
        SalesAction.CreateDraftInvoice

    // ─────────────────────────────────────────────────────────────
    // UI Toggles
    // ─────────────────────────────────────────────────────────────

    SalesUiEvent.TogglePaymentSheet ->
        SalesAction.TogglePaymentSheet

    SalesUiEvent.ToggleCustomerSheet ->
        SalesAction.ToggleCustomerSheet

    SalesUiEvent.ToggleProductsSheet ->
        SalesAction.ToggleProductsSheet

    SalesUiEvent.OpenScanner ->
        SalesAction.OpenScanner

    SalesUiEvent.CloseScanner ->
        SalesAction.CloseScanner

    SalesUiEvent.DismissDialogs ->
        SalesAction.ClearError

    // ─────────────────────────────────────────────────────────────
    // Sale Operations
    // ─────────────────────────────────────────────────────────────

    SalesUiEvent.ValidateSale ->
        SalesAction.ValidateSale

    SalesUiEvent.CompleteSale ->
        SalesAction.CompleteSale

    SalesUiEvent.ConfirmSale ->
        SalesAction.ConfirmSale

    SalesUiEvent.CancelSale ->
        SalesAction.CancelSale

    // ─────────────────────────────────────────────────────────────
    // Subscription
    // ─────────────────────────────────────────────────────────────

    SalesUiEvent.CheckSubscriptionStatus ->
        SalesAction.CheckSubscriptionStatus

    SalesUiEvent.RequestSubscriptionUpgrade ->
        SalesAction.RequestSubscriptionUpgrade

    SalesUiEvent.OpenSubscriptionScreen ->
        SalesAction.OpenSubscriptionScreen

    // ─────────────────────────────────────────────────────────────
    // Navigation — Side Effects
    // ─────────────────────────────────────────────────────────────

    SalesUiEvent.NavigateBack ->
        null.also {
            emitEffect(
                SalesEffect.NavigateBack
            )
        }

    SalesUiEvent.NavigateToReports ->
        null.also {
            emitEffect(
                SalesEffect.NavigateToReports
            )
        }

    SalesUiEvent.NavigateToCustomers ->
        null.also {
            emitEffect(
                SalesEffect.NavigateToCustomers
            )
        }

    SalesUiEvent.NavigateToProducts ->
        null.also {
            emitEffect(
                SalesEffect.NavigateToProducts
            )
        }

    // ─────────────────────────────────────────────────────────────
    // Bills / Invoices
    // ─────────────────────────────────────────────────────────────

    is SalesUiEvent.SearchBills ->
        null

    is SalesUiEvent.SelectCartItem ->
        null

    is SalesUiEvent.OpenBillDetails ->
        null.also {
            emitEffect(
                SalesEffect.NavigateToInvoiceDetails(
                    event.billId
                )
            )
        }

    is SalesUiEvent.PrintInvoice ->
        null.also {
            emitEffect(
                SalesEffect.InvoicePrinted(
                    event.bill
                )
            )
        }

    is SalesUiEvent.ShareInvoice ->
        null.also {
            emitEffect(
                SalesEffect.InvoiceShared(
                    event.bill
                )
            )
        }

    is SalesUiEvent.DownloadInvoicePdf ->
        null.also {
            emitEffect(
                SalesEffect.InvoicePdfDownloaded(
                    event.bill,
                    ""
                )
            )
        }

    // ─────────────────────────────────────────────────────────────
    // Feedback
    // ─────────────────────────────────────────────────────────────

    is SalesUiEvent.ShowSuccessMessage ->
        null.also {
            emitEffect(
                SalesEffect.ShowSuccess(
                    event.message
                )
            )
        }

    is SalesUiEvent.ShowErrorMessage ->
        null.also {
            emitEffect(
                SalesEffect.ShowFailure(
                    event.message,
                    event.throwable
                )
            )
        }
}
    // ─────────────────────────────────────────────────────────────────────────
    // Side-effect dispatcher
    // ─────────────────────────────────────────────────────────────────────────

    private fun handleSideEffects(action: SalesAction, state: SalesUiState) {
        when (action) {
            SalesAction.Initialize -> initialize()
            SalesAction.Refresh    -> loadProducts()
            SalesAction.Retry      -> loadProducts()

            is SalesAction.SearchProducts ->
                scheduleSearch(action.normalizedQuery)

            is SalesAction.ScanBarcode ->
                handleBarcodeScanned(action.barcode)

            is SalesAction.QuickAddProduct ->
                quickAddByBarcode(action.barcode)

            is SalesAction.SelectCustomer ->
                emitEffect(SalesEffect.CustomerAssigned(action.customer))

            SalesAction.RemoveCustomer ->
                emitEffect(SalesEffect.CustomerRemoved)

            is SalesAction.SelectProduct ->
                emitEffect(
                    SalesEffect.ProductAddedToCart(
                        productName = action.product.name,
                        quantity    = BigDecimal.ONE
                    )
                )

            is SalesAction.AddProductToCart ->
                emitEffect(
                    SalesEffect.ProductAddedToCart(
                        productName = action.product.name,
                        quantity    = action.quantity
                    )
                )

            is SalesAction.RemoveCartItem ->
                emitEffect(SalesEffect.CartItemRemoved(productName = ""))

            SalesAction.ClearCart ->
                emitEffect(SalesEffect.CartCleared)

            SalesAction.ValidateSale ->
                handleValidateSale(state)

            SalesAction.CompleteSale,
            SalesAction.ConfirmSale ->
                completeSale(state)

            SalesAction.CancelSale -> {
                emitEffect(SalesEffect.ShowInfo("تم إلغاء عملية البيع"))
                emitEffect(SalesEffect.DismissBottomSheets)
            }

            SalesAction.CreateDraftInvoice ->
                createDraftInvoice(state)

            SalesAction.CheckSubscriptionStatus ->
                checkSubscription()

            SalesAction.RequestSubscriptionUpgrade ->
                emitEffect(SalesEffect.SubscriptionUpgradeRequired)

            SalesAction.OpenSubscriptionScreen ->
                emitEffect(SalesEffect.NavigateToSubscription)

            SalesAction.OpenScanner ->
                emitEffect(SalesEffect.ScannerOpened)

            SalesAction.CloseScanner ->
                emitEffect(SalesEffect.ScannerClosed)

            else -> Unit
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Initialization
    // ─────────────────────────────────────────────────────────────────────────

    private fun initialize() {
        checkSubscription()
        loadProducts()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Subscription  (stub)
    // ─────────────────────────────────────────────────────────────────────────

    private fun checkSubscription() {

    // يمكن لاحقاً استبدال هذا الشرط
    // بالتحقق الحقيقي من SubscriptionRepository
    val isUserSubscribed = true

    _salesState.update { state ->

        val canComplete =
            isUserSubscribed &&
            state.cartItems.isNotEmpty()

        state.copy(

            // حالة الاشتراك
            isSubscriptionExpired = !isUserSubscribed,

            // لم نعد نطلب الاشتراك بشكل قسري
            requiresSubscription = false,

            // السماح بإكمال البيع
            canCompleteSale = canComplete,

            // رسالة الاشتراك
            subscriptionMessage = if (isUserSubscribed) {
                null
            } else {
                "يجب تفعيل الاشتراك لإكمال عملية البيع"
            }
        )
    }

    if (isUserSubscribed) {
        emitEffect(
            SalesEffect.SubscriptionValid
        )
    }
}
    // ─────────────────────────────────────────────────────────────────────────
    // Product loading
    // ─────────────────────────────────────────────────────────────────────────
// ─────────────────────────────────────────────────────────────────────────
// Product loading
// ─────────────────────────────────────────────────────────────────────────

private fun loadProducts() {

    viewModelScope.launch(dispatchers.main) {

        _salesState.update { state ->

            state.copy(
                isLoadingProducts = true,
                errorMessage = null,
                uiState = UiState.Loading
            )
        }

        runCatching {

            withContext(dispatchers.io) {

                val products =
                    getProductsUseCase()

                // ترجع List<StockItem>
                val inventoryItems =
                    getInventoryUseCase()

                /*
                 * =====================================================
                 * تنظيف بيانات المخزون
                 * =====================================================
                 */
                val normalizedInventoryMap =
                    inventoryItems.associateBy { item ->

                        item.normalizedBarcode
                            .trim()
                            .lowercase()
                    }

                /*
                 * =====================================================
                 * المنتجات الرسمية المرتبطة بالمخزون
                 * =====================================================
                 */
                val officialProducts =
                    products.mapNotNull { product ->

                        val officialProduct =
                            ProductUiModel.fromDomain(product)

                        val normalizedBarcode =
                            officialProduct.barcode
                                .trim()
                                .lowercase()

                        val stockItem =
                            normalizedInventoryMap[
                                normalizedBarcode
                            ]

                        /*
                         * لا تعرض منتج غير موجود بالمخزون
                         */
                        if (stockItem == null) {

                            null

                        } else {

                            val productName =
                                stockItem.productName
                                    .trim()

                            val effectiveName =
                                stockItem.effectiveName
                                    .trim()

                            /*
                             * =================================================
                             * StockItem لا يحتوي على أسعار
                             * لذلك نعتمد على السعر الرسمي للمنتج
                             * =================================================
                             */
                            val effectivePrice =
                                if (
                                    officialProduct.price >
                                    BigDecimal.ZERO
                                ) {

                                    officialProduct.price

                                } else {

                                    BigDecimal.ONE
                                }

                            officialProduct.copy(

                                barcode =
                                    stockItem.normalizedBarcode
                                        .trim(),

                                name =
                                    productName.ifBlank {
                                        officialProduct.name
                                    },

                                displayName =
                                    effectiveName.ifBlank {
                                        officialProduct.displayName
                                    },

                                price =
                                    effectivePrice,

                                formattedPrice =
                                    CurrencyFormatter.formatSDG(
                                        effectivePrice
                                    ),

                                /*
                                 * quantity نوعها Double
                                 */
                                isAvailableForSale =
                                    stockItem.quantity > 0.0,

                                searchKeywords =
                                    buildString {

                                        append(
                                            stockItem.normalizedBarcode
                                                .trim()
                                                .lowercase()
                                        )

                                        append(" ")

                                        append(
                                            productName.lowercase()
                                        )

                                        append(" ")

                                        append(
                                            effectiveName.lowercase()
                                        )

                                        append(" ")

                                        append(
                                            stockItem.unitOfMeasure
                                                .displayName
                                                .lowercase()
                                        )

                                    }.trim()
                            )
                        }
                    }

                /*
                 * =====================================================
                 * منتجات المخزون غير الرسمية
                 * =====================================================
                 */
                val officialBarcodes =
                    officialProducts
                        .map {
                            it.barcode
                                .trim()
                                .lowercase()
                        }
                        .toSet()

                val inventoryOnlyProducts =
                    inventoryItems
                        .filter { item ->

                            item.normalizedBarcode
                                .trim()
                                .lowercase() !in officialBarcodes
                        }
                        .map { item ->

                            val normalizedBarcode =
                                item.normalizedBarcode
                                    .trim()

                            val productName =
                                item.productName
                                    .trim()

                            val effectiveName =
                                item.effectiveName
                                    .trim()

                            /*
                             * =================================================
                             * StockItem لا يحتوي على أسعار
                             * نستخدم قيمة افتراضية آمنة
                             * =================================================
                             */
                            val effectivePrice =
                                BigDecimal.ONE

                            ProductUiModel(

                                barcode =
                                    normalizedBarcode,

                                name =
                                    productName,

                                displayName =
                                    effectiveName,

                                description = null,

                                price =
                                    effectivePrice,

                                formattedPrice =
                                    CurrencyFormatter.formatSDG(
                                        effectivePrice
                                    ),

                                unitOfMeasure =
                                    item.unitOfMeasure,

                                largeUnit =
                                    item.unitOfMeasure,

                                smallUnit =
                                    item.unitOfMeasure,

                                unitFactor =
                                    BigDecimal.ONE,

                                isActive = true,

                                /*
                                 * quantity نوعها Double
                                 */
                                isAvailableForSale =
                                    item.quantity > 0.0,

                                supportsUnitHierarchy =
                                    false,

                                hasCustomUnitConversion =
                                    false,

                                searchKeywords =
                                    buildString {

                                        append(
                                            normalizedBarcode
                                                .lowercase()
                                        )

                                        append(" ")

                                        append(
                                            productName
                                                .lowercase()
                                        )

                                        append(" ")

                                        append(
                                            effectiveName
                                                .lowercase()
                                        )

                                        append(" ")

                                        append(
                                            item.unitOfMeasure
                                                .displayName
                                                .lowercase()
                                        )

                                    }.trim()
                            )
                        }

                /*
                 * =====================================================
                 * الدمج النهائي
                 * =====================================================
                 */
                val combinedList =
                    (officialProducts + inventoryOnlyProducts)
                        .distinctBy {
                            it.barcode
                                .trim()
                                .lowercase()
                        }
                        .sortedBy {
                            it.displayName.lowercase()
                        }

                /*
                 * =====================================================
                 * البحث الحالي
                 * =====================================================
                 */
                val currentQuery =
                    _salesState.value.searchQuery
                        .trim()

                val filteredList =
                    if (currentQuery.isBlank()) {

                        combinedList

                    } else {

                        combinedList.filter { product ->
                            product.matches(currentQuery)
                        }
                    }

                combinedList to filteredList
            }

        }.onSuccess { (combinedList, filteredList) ->

            _salesState.update { state ->

                state.copy(

                    products =
                        combinedList,

                    filteredProducts =
                        filteredList,

                    isLoadingProducts =
                        false,

                    errorMessage =
                        null,

                    uiState =
                        UiState.Success(Unit)
                )
            }

            /*
             * =========================================================
             * إعادة فحص الاشتراك بعد تحميل المنتجات
             * =========================================================
             */
            checkSubscription()

        }.onFailure { throwable ->

            val errorMessage =
                throwable.message
                    ?.takeIf {
                        it.isNotBlank()
                    }
                    ?: "حدث خطأ أثناء تحميل المنتجات"

            _salesState.update { state ->

                state.copy(

                    isLoadingProducts =
                        false,

                    errorMessage =
                        errorMessage,

                    uiState =
                        UiState.Error(errorMessage)
                )
            }

            emitEffect(

                SalesEffect.ShowFailure(

                    message =
                        errorMessage,

                    throwable =
                        throwable
                )
            )
        }
    }
}
    // ─────────────────────────────────────────────────────────────────────────
    // Debounced search
    // ─────────────────────────────────────────────────────────────────────────

    private fun scheduleSearch(query: String) {
        searchJob?.cancel()

        if (query.isBlank()) {
            _salesState.update { state ->
                state.copy(filteredProducts = state.products, isSearching = false)
            }
            return
        }

        searchJob = viewModelScope.launch(dispatchers.main) {
            _salesState.update { it.copy(isSearching = true) }
            delay(SEARCH_DEBOUNCE_MS)

            val results = runCatching {
                withContext(dispatchers.io) { searchProductsUseCase(query) }
            }.getOrElse { emptyList() }

            val uiModels = results.map { ProductUiModel.fromDomain(it) }
            _salesState.update { state ->
                state.copy(filteredProducts = uiModels, isSearching = false)
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Barcode scanning
    // ─────────────────────────────────────────────────────────────────────────

    private fun handleBarcodeScanned(barcode: String) {
        val validation = SalesValidator.validateBarcode(barcode)
        if (!validation.isValid) {
            emitEffect(SalesEffect.ShowWarning(validation.firstError ?: "باركود غير صالح"))
            return
        }
        emitEffect(SalesEffect.BarcodeScanned(barcode))
        quickAddByBarcode(barcode)
    }

    private fun quickAddByBarcode(barcode: String) {

    viewModelScope.launch(dispatchers.io) {

        val normalized = barcode
            .trim()

        if (normalized.isBlank()) {
            return@launch
        }

        /*
         * =========================================================
         * البحث عن المنتج مع تجاهل المسافات وحالة الأحرف
         * =========================================================
         */
        val product = _salesState.value.products
            .firstOrNull { item ->

                item.barcode
                    .trim()
                    .lowercase() == normalized.lowercase()
            }

        /*
         * =========================================================
         * إضافة المنتج للسلة
         * =========================================================
         */
        if (product != null) {

            /*
             * نستخدم AddProductToCart مع كمية ابتدائية = 1
             * حتى يتمكن المستخدم لاحقاً من تعديلها
             * إلى 5 أو 10 أو أي كمية من واجهة السلة
             */
            onEvent(
                SalesUiEvent.AddProductToCart(
                    product = product,
                    quantity = BigDecimal.ONE
                )
            )

            emitEffect(
                SalesEffect.ShowSuccess(
                    "تمت إضافة ${product.displayName} إلى السلة"
                )
            )

        } else {

            emitEffect(
                SalesEffect.ProductNotFound(
                    normalized
                )
            )
        }
    }
}

    // ─────────────────────────────────────────────────────────────────────────
    // Sale validation
    // ─────────────────────────────────────────────────────────────────────────

    private fun handleValidateSale(state: SalesUiState) {
        when {
            state.cartItems.isEmpty() ->
                emitEffect(SalesEffect.SaleValidationFailed(SalesConstants.Error.EMPTY_CART))

            !state.canCompleteSale ->
                emitEffect(
                    SalesEffect.SaleValidationFailed(
                        state.subscriptionMessage ?: "لا يمكن إكمال عملية البيع"
                    )
                )

            else ->
                emitEffect(SalesEffect.SaleValidationSucceeded)
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Complete sale
    // ─────────────────────────────────────────────────────────────────────────

    private fun completeSale(state: SalesUiState) {

    /*
     * =========================================================
     * منع تنفيذ أكثر من عملية بيع بنفس الوقت
     * =========================================================
     */
    if (state.isSubmittingSale) {
        return
    }

    /*
     * =========================================================
     * التحقق من وجود عناصر بالسلة
     * =========================================================
     */
    if (state.cartItems.isEmpty()) {

        emitEffect(
            SalesEffect.SaleValidationFailed(
                SalesConstants.Error.EMPTY_CART
            )
        )

        return
    }

    /*
     * =========================================================
     * تحويل عناصر السلة إلى عناصر بيع
     * =========================================================
     */
    val domainItems = state.cartItems
        .toSaleItems()

    if (domainItems.isEmpty()) {

        emitEffect(
            SalesEffect.SaleValidationFailed(
                "تعذّر تحويل بنود السلة"
            )
        )

        return
    }

    /*
     * =========================================================
     * التحقق من الكميات
     * =========================================================
     */
    val invalidItem = domainItems
        .firstOrNull { item ->

            item.quantity.toDouble() <= 0.0
        }

    if (invalidItem != null) {

        emitEffect(
            SalesEffect.SaleValidationFailed(
                "الكمية غير صالحة للصنف: ${invalidItem.productName}"
            )
        )

        return
    }

    /*
     * =========================================================
     * بدء التحميل
     * =========================================================
     */
    _salesState.update {

        it.copy(
            isSubmittingSale = true,
            errorMessage = null
        )
    }

    emitEffect(
        SalesEffect.LoadingStarted
    )

    viewModelScope.launch(dispatchers.main) {

        /*
         * =====================================================
         * إنشاء الفاتورة وطلب المعالجة
         * =====================================================
         */
        val sale = buildSale(
            items = domainItems,
            state = state
        )

        val request = buildProcessRequest(
            sale = sale,
            state = state
        )

        runCatching {

            withContext(dispatchers.io) {

                processSaleUseCase(request)
                    .getOrThrow()
            }

        }.onSuccess { result ->

            /*
             * =================================================
             * إنشاء بيانات الفاتورة للعرض والطباعة
             * =================================================
             */
            val bill = buildBillFromResult(
                result = result,
                state = state
            )

            /*
             * =================================================
             * إعادة تهيئة الشاشة مع الاحتفاظ بالمنتجات
             * =================================================
             */
            _salesState.update { current ->

                SalesUiState.initial().copy(

                    /*
                     * الاحتفاظ بالمنتجات المحملة
                     */
                    products = current.products,

                    filteredProducts = current.filteredProducts,

                    /*
                     * منع تفعيل زر الإكمال بعد النجاح
                     */
                    canCompleteSale = false,

                    /*
                     * حفظ آخر فاتورة
                     */
                    lastCompletedBill = bill,

                    /*
                     * رسالة نجاح
                     */
                    message = "تمت عملية البيع بنجاح"
                )
            }

            /*
             * =================================================
             * إعادة تحميل المنتجات لتحديث الكميات بالمخزون
             * =================================================
             */
            loadProducts()

            /*
             * =================================================
             * إرسال تأثيرات النجاح
             * =================================================
             */
            emitEffect(
                SalesEffect.ShowSuccess(
                    "تمت عملية البيع بنجاح"
                )
            )

            emitEffect(
                SalesEffect.LoadingFinished
            )

            /*
             * =================================================
             * إنشاء الفاتورة النهائية
             * =================================================
             */
            bill?.let { completedBill ->

                emitEffect(
                    SalesEffect.InvoiceGenerated(
                        completedBill.toDomainInvoice(
                            domainItems
                        )
                    )
                )
            }

        }.onFailure { throwable ->

            /*
             * =================================================
             * معالجة الخطأ
             * =================================================
             */
            val message = throwable.message
                ?.takeIf { it.isNotBlank() }
                ?: "حدث خطأ أثناء إتمام البيع"

            _salesState.update {

                it.copy(
                    isSubmittingSale = false,
                    errorMessage = message
                )
            }

            emitEffect(
                SalesEffect.ShowFailure(
                    message = message,
                    throwable = throwable
                )
            )

            emitEffect(
                SalesEffect.LoadingFinished
            )
        }
    }
}
    // ─────────────────────────────────────────────────────────────────────────
    // Draft invoice
    // ─────────────────────────────────────────────────────────────────────────

    private fun createDraftInvoice(state: SalesUiState) {
        if (state.cartItems.isEmpty()) {
            emitEffect(SalesEffect.ShowWarning(SalesConstants.Error.EMPTY_CART))
            return
        }
        viewModelScope.launch(dispatchers.io) {
            val domainItems = state.cartItems.toSaleItems()
            if (domainItems.isEmpty()) return@launch
            val sale    = buildSale(domainItems, state)
            val invoice = buildDraftInvoice(sale, state)
            emitEffect(SalesEffect.DraftInvoiceCreated(invoice))
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Domain builders
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Builds a domain [Sale].
     *
     * Only the parameters declared in the attached [Sale] data class are used:
     * id, invoiceNumber, customerId, employeeId, items, paidAmount,
     * saleStatus, createdAt, note.
     *
     * Fields that do NOT exist in [Sale] (paymentStatus, paymentMethod,
     * createdAtMillis) are intentionally omitted.
     */
    private fun buildSale(
        items: List<SaleItem>,
        state: SalesUiState
    ): Sale = Sale(
        id            = UUID.randomUUID().toString(),
        invoiceNumber = generateInvoiceNumber(),
        customerId    = state.selectedCustomer?.id,
        employeeId    = resolveEmployeeId(),
        items         = items,
        paidAmount    = state.paidAmount,
        saleStatus    = SaleStatus.COMPLETED,
        createdAt     = LocalDateTime.now(),
        note          = state.currentBill?.notes
    )

    /**
     * Builds a [ProcessSaleRequest] from a validated [Sale] and current state.
     */
    private fun buildProcessRequest(
        sale: Sale,
        state: SalesUiState
    ): ProcessSaleRequest = ProcessSaleRequest(
        sale                      = sale,
        defaultStockLocation      = DEFAULT_STOCK_LOCATION,
        createInvoice             = true,
        issueInvoiceNow           = true,
        invoiceNotes              = state.currentBill?.notes,
        invoiceTermsAndConditions = state.currentBill?.termsAndConditions,
        invoicePartyId            = state.selectedCustomer?.id,
        invoicePartyName          = state.selectedCustomer?.displayName,
        invoicePartyTaxNumber     = state.selectedCustomer?.taxNumber
    )

    /**
     * Builds a draft [SaleInvoice].
     *
     * Only the parameters declared in the attached [SaleInvoice] data class
     * are used. paymentStatus is NOT a direct constructor parameter of
     * [SaleInvoice]; it is derived from paidAmount / totalAmount at runtime.
     */
    private fun buildDraftInvoice(
        sale: Sale,
        state: SalesUiState
    ): SaleInvoice = SaleInvoice(
        id                 = UUID.randomUUID().toString(),
        invoiceNumber      = "DRAFT-${System.currentTimeMillis()}",
        saleId             = sale.id,
        employeeId         = sale.employeeId,
        status             = SaleInvoiceStatus.DRAFT,
        issueDate          = LocalDateTime.now(),
        dueDate            = null,
        items              = sale.items,
        paidAmount         = state.paidAmount,
        notes              = state.currentBill?.notes,
        termsAndConditions = state.currentBill?.termsAndConditions
    )

    /**
     * Converts a [ProcessSaleResult] to a [BillUiModel].
     * Returns null if the result does not contain a valid invoice id / number.
     */
    private fun buildBillFromResult(
        result: ProcessSaleResult,
        state: SalesUiState
    ): BillUiModel? {
        val invoiceId     = result.invoiceId     ?: return null
        val invoiceNumber = result.invoiceNumber  ?: return null

        val invoice = SaleInvoice(
            id                 = invoiceId,
            invoiceNumber      = invoiceNumber,
            saleId             = result.savedSale.id,
            employeeId         = result.savedSale.employeeId,
            status             = SaleInvoiceStatus.ISSUED,
            issueDate          = LocalDateTime.now(),
            dueDate            = null,
            items              = result.savedSale.items,
            paidAmount         = state.paidAmount,
            notes              = state.currentBill?.notes,
            termsAndConditions = state.currentBill?.termsAndConditions
        )

        return SalesMapper.toBillUiModel(
            invoice       = invoice,
            customer      = state.selectedCustomer,
            paymentMethod = state.selectedPaymentMethod,
            products      = state.products
        )
    }

    /** Generates a sequential invoice number based on current timestamp. */
    private fun generateInvoiceNumber(): String =
        "INV-${System.currentTimeMillis()}"

    /** Returns the authenticated employee id. Replace with a session use-case. */
    private fun resolveEmployeeId(): String = FALLBACK_EMPLOYEE_ID

    // ─────────────────────────────────────────────────────────────────────────
    // Effect emitter (always on Main)
    // ─────────────────────────────────────────────────────────────────────────

    private fun emitEffect(effect: SalesEffect) {
        viewModelScope.launch(dispatchers.main) {
            _effect.emit(effect)
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Lifecycle
    // ─────────────────────────────────────────────────────────────────────────

    override fun onCleared() {
        super.onCleared()
        searchJob?.cancel()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Constants
    // ─────────────────────────────────────────────────────────────────────────

    private companion object {
        const val SEARCH_DEBOUNCE_MS    = 300L
        const val DEFAULT_STOCK_LOCATION = "MAIN"
        const val FALLBACK_EMPLOYEE_ID   = "EMPLOYEE_SESSION"
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Private extension helpers
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Maps a [CartUiModel] list to validated domain [SaleItem] objects.
 * Items that fail basic sanity checks are silently dropped.
 */
private fun List<CartUiModel>.toSaleItems(): List<SaleItem> =
    mapNotNull { cart ->
        if (cart.productId.isBlank() || cart.productName.isBlank()) return@mapNotNull null
        if (cart.quantity <= BigDecimal.ZERO) return@mapNotNull null
        SaleItem(
            id             = cart.id,
            productId      = cart.productId,
            productName    = cart.productName,
            unit           = cart.unit,
            quantity       = cart.quantity,
            unitPrice      = cart.unitPrice,
            taxAmount      = cart.taxAmount,
            discountAmount = cart.discountAmount,
            isReturn       = cart.isReturn,
            note           = cart.note
        )
    }

/**
 * Reconstructs a [SaleInvoice] from a [BillUiModel] for use in
 * [SalesEffect.InvoiceGenerated]. Only constructor parameters confirmed in the
 * attached [SaleInvoice] source are set.
 */
private fun BillUiModel.toDomainInvoice(items: List<SaleItem>): SaleInvoice =
    SaleInvoice(
        id                 = id,
        invoiceNumber      = invoiceNumber,
        saleId             = saleId,
        employeeId         = employeeId,
        status             = status,
        issueDate          = issueDate,
        dueDate            = dueDate,
        items              = items,
        paidAmount         = paidAmount,
        notes              = notes,
        termsAndConditions = termsAndConditions
    )

/**
 * Case-insensitive substring match across name, barcode, and displayName.
 */
private fun ProductUiModel.matchesQuery(query: String): Boolean {
    val q = query.trim().lowercase()
    return q.isBlank()
        || name.lowercase().contains(q)
        || barcode.lowercase().contains(q)
        || displayName?.lowercase()?.contains(q) == true
}
