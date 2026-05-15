package com.myimdad_por.ui.features.sales
import android.util.Log
import com.myimdad_por.ui.features.sales.components.SaleProductList 
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.rounded.AddShoppingCart
import androidx.compose.material.icons.rounded.Payment
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PointOfSale
import androidx.compose.material.icons.rounded.Receipt
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.myimdad_por.core.base.UiState
import com.myimdad_por.domain.model.Customer
import com.myimdad_por.domain.model.PaymentMethod
import com.myimdad_por.ui.components.AppButton
import com.myimdad_por.ui.components.AppButtonSize
import com.myimdad_por.ui.components.AppButtonVariant
import com.myimdad_por.ui.components.AppConfirmDialog
import com.myimdad_por.ui.components.AppDialogVariant
import com.myimdad_por.ui.components.AppTextField
import com.myimdad_por.ui.components.AppTextFieldSize
import com.myimdad_por.ui.components.AppTextFieldVariant
import com.myimdad_por.ui.features.sales.components.CartItem
import com.myimdad_por.ui.features.sales.components.CustomerSection
import com.myimdad_por.ui.features.sales.components.DiscountSection
import com.myimdad_por.ui.features.sales.components.EmptySales
import com.myimdad_por.ui.features.sales.components.PaymentSection
import com.myimdad_por.ui.features.sales.components.SaleTransactionPanel
import com.myimdad_por.ui.features.sales.components.ProductItem
import com.myimdad_por.ui.features.sales.components.SalesBottomBar
import com.myimdad_por.ui.features.sales.components.SalesError
import com.myimdad_por.ui.features.sales.components.SalesLoading
import com.myimdad_por.ui.features.sales.components.SalesSummary
import com.myimdad_por.ui.features.sales.components.SalesTopBar
import com.myimdad_por.ui.features.sales.models.CartUiModel
import com.myimdad_por.ui.features.sales.models.ProductUiModel
import com.myimdad_por.ui.theme.AppDimens
import com.myimdad_por.ui.theme.AppShapeTokens
import com.myimdad_por.ui.theme.BrandPrimary
import com.myimdad_por.ui.theme.BrandPrimaryDark
import com.myimdad_por.ui.theme.BrandPrimarySoft
import com.myimdad_por.ui.theme.BrandPrimaryTint
import com.myimdad_por.ui.theme.BorderColor
import com.myimdad_por.ui.theme.IconOnBrandColor
import com.myimdad_por.ui.theme.TextPrimaryColor
import com.myimdad_por.ui.theme.TextSecondaryColor
import com.myimdad_por.ui.theme.WhiteColor
import kotlinx.coroutines.launch
import java.math.BigDecimal

// ---------------------------------------------------------------------------
// SalesScreen Entry Point
// ---------------------------------------------------------------------------

@Composable
fun SalesScreen(
    onNavigateBack: () -> Unit = {},
    onNavigateToReports: () -> Unit = {},
    onNavigateToCustomers: () -> Unit = {},
    onNavigateToProducts: () -> Unit = {},
    onNavigateToSubscription: () -> Unit = {},
    onNavigateToInvoiceDetails: (String) -> Unit = {},
    viewModel: SalesViewModel = hiltViewModel()
) {
    val state by viewModel.salesState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // ── Effect collector ───────────────────────────────────────────────────
    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is SalesEffect.NavigateBack -> onNavigateBack()
                is SalesEffect.NavigateToReports -> onNavigateToReports()
                is SalesEffect.NavigateToCustomers -> onNavigateToCustomers()
                is SalesEffect.NavigateToProducts -> onNavigateToProducts()
                is SalesEffect.NavigateToSubscription -> onNavigateToSubscription()
                is SalesEffect.NavigateToInvoiceDetails ->
                    onNavigateToInvoiceDetails(effect.invoiceId)

                is SalesEffect.ShowSuccess -> scope.launch {
                    snackbarHostState.showSnackbar(effect.message)
                }
                is SalesEffect.ShowFailure -> scope.launch {
                    snackbarHostState.showSnackbar(effect.message)
                }
                is SalesEffect.ShowWarning -> scope.launch {
                    snackbarHostState.showSnackbar(effect.message)
                }
                is SalesEffect.ShowInfo -> scope.launch {
                    snackbarHostState.showSnackbar(effect.message)
                }
                else -> Unit
            }
        }
    }

    SalesScreenContent(
        state = state,
        snackbarHostState = snackbarHostState,
        onEvent = viewModel::onEvent
    )
}

// ---------------------------------------------------------------------------
// SalesScreenContent
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SalesScreenContent(
    state: SalesUiState,
    snackbarHostState: SnackbarHostState,
    onEvent: (SalesUiEvent) -> Unit
) {
    // ── Local UI state ─────────────────────────────────────────────────────
    var showClearCartDialog by rememberSaveable { mutableStateOf(false) }
    var showConfirmSaleDialog by rememberSaveable { mutableStateOf(false) }

    // Product sheet
    var showProductsSheet by rememberSaveable { mutableStateOf(false) }
    val productsSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Payment sheet
    val paymentSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Customer sheet
    val customerSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Local payment inputs (kept locally to avoid re-triggering events on every keystroke)
    var paidAmountInput by rememberSaveable { mutableStateOf("") }
    var referenceNumber by rememberSaveable { mutableStateOf("") }
    var creditSaleEnabled by rememberSaveable { mutableStateOf(false) }
    var customerQuery by rememberSaveable { mutableStateOf("") }
    var discountInput by rememberSaveable { mutableStateOf("") }

    // ── BackHandler ────────────────────────────────────────────────────────
    BackHandler(enabled = state.isPaymentSheetVisible || state.isCustomerSheetVisible || showProductsSheet) {
        when {
            state.isPaymentSheetVisible -> onEvent(SalesUiEvent.TogglePaymentSheet)
            state.isCustomerSheetVisible -> onEvent(SalesUiEvent.ToggleCustomerSheet)
            showProductsSheet -> showProductsSheet = false
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            SalesTopBar(
                title = "المبيعات",
                subtitle = "إدارة الفواتير والمبيعات اليومية",
                onBackClick = { onEvent(SalesUiEvent.NavigateBack) },
                actions = {
                    // Scanner button
                    IconButton(
                        onClick = { onEvent(SalesUiEvent.OpenScanner) },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.White.copy(alpha = 0.14f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = "مسح الباركود",
                            tint = IconOnBrandColor
                        )
                    }
                    Spacer(modifier = Modifier.width(AppDimens.Spacing.small))
                    // Cart badge
                    BadgedBox(
                        badge = {
                            if (state.totalItemsCount > 0) {
                                Badge(
                                    containerColor = WhiteColor,
                                    contentColor = BrandPrimaryDark
                                ) {
                                    Text(
                                        text = state.totalItemsCount.toString(),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                        }
                    ) {
                        IconButton(
                            onClick = { showProductsSheet = true },
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color.White.copy(alpha = 0.14f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.ShoppingCart,
                                contentDescription = "السلة",
                                tint = IconOnBrandColor
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            AnimatedVisibility(
                visible = !state.isCartEmpty,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                SalesBottomBar(
                    subtotalAmount = state.subtotalAmount,
                    discountAmount = state.discountAmount,
                    taxAmount = state.taxAmount,
                    totalAmount = state.totalAmount,
                    paidAmount = state.paidAmount,
                    remainingAmount = state.remainingAmount,
                    enabled = !state.isSubmittingSale,
                    loading = state.isSubmittingSale,
                    onPayClick = { onEvent(SalesUiEvent.TogglePaymentSheet) },
                    onSaveDraftClick = { onEvent(SalesUiEvent.CreateDraftInvoice) },
                    onClearCartClick = { showClearCartDialog = true }
                )
            }
        }
    ) { paddingValues ->

        // ── Main body ──────────────────────────────────────────────────────
        AnimatedContent(
            targetState = state.uiState,
            transitionSpec = {
                fadeIn(tween(220)) togetherWith fadeOut(tween(180))
            },
            label = "sales_content_anim"
        ) { uiState ->

            when (uiState) {

                is UiState.Loading,
                UiState.Idle -> {
                    SalesLoadingBody(
                        paddingValues = paddingValues
                    )
                }

                is UiState.Error -> {
                    SalesErrorBody(
                        message = uiState.message,
                        paddingValues = paddingValues,
                        onRetry = {
                            onEvent(SalesUiEvent.Retry)
                        }
                    )
                }

                is UiState.Success -> {
                    SalesMainBody(
                        state = state,
                        paddingValues = paddingValues,
                        discountInput = discountInput,
                        onDiscountInputChange = {
                            discountInput = it
                        },
                        onEvent = onEvent,
                        onOpenProductsSheet = {
                            showProductsSheet = true
                        },
                        onOpenCustomerSheet = {
                            onEvent(SalesUiEvent.ToggleCustomerSheet)
                        },
                        onOpenPaymentSheet = {
                            onEvent(SalesUiEvent.TogglePaymentSheet)
                        },
                        onConfirmSale = {
                            showConfirmSaleDialog = true
                        },
                        onClearCart = {
                            showClearCartDialog = true
                        }
                    )
                }

                else -> Unit
            }
        }
    }

    // ── Products Bottom Sheet ──────────────────────────────────────────────
   // if (showProductsSheet) {

    // ── Products Bottom Sheet ──────────────────────────────────────────────
    if (showProductsSheet) {
    ModalBottomSheet(
        onDismissRequest = { showProductsSheet = false },
        sheetState = productsSheetState,
        // إزالة الحواف المستديرة الافتراضية لأن الهيدر الجديد له خلفية متدرجة تغطي المساحة
        shape = RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp), 
        // جعل لون الحاوية شفافاً ليظهر التدرج اللوني الخاص بالمكون الجديد بوضوح
        containerColor = Color.Transparent, 
        // إلغاء المقبض الافتراضي تماماً لأننا نريد مظهراً نظيفاً يشبه التطبيقات الحديثة
        dragHandle = null, 
    ) {
        // استدعاء المكون مباشرة، هو سيتكفل بالعنوان والبحث والقائمة
        ProductsSheetContent(
            state = state,
            onEvent = onEvent
        )
    }
}


    // ── Payment Bottom Sheet ───────────────────────────────────────────────
    if (state.isPaymentSheetVisible) {
        ModalBottomSheet(
            onDismissRequest = { onEvent(SalesUiEvent.TogglePaymentSheet) },
            sheetState = paymentSheetState,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            containerColor = WhiteColor,
            dragHandle = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    BottomSheetDefaults.DragHandle()
                    PaymentSheetHeader(
                        onClose = { onEvent(SalesUiEvent.TogglePaymentSheet) }
                    )
                    HorizontalDivider(color = BorderColor)
                }
            }
        ) {
            PaymentSheetContent(
                state = state,
                paidAmountInput = paidAmountInput,
                onPaidAmountInputChange = { paidAmountInput = it },
                referenceNumber = referenceNumber,
                onReferenceNumberChange = { referenceNumber = it },
                creditSaleEnabled = creditSaleEnabled,
                onCreditSaleToggle = { creditSaleEnabled = it },
                onEvent = onEvent,
                onConfirm = {
                    onEvent(SalesUiEvent.UpdatePaidAmount(
                        paidAmountInput.toBigDecimalOrNull() ?: BigDecimal.ZERO
                    ))
                    onEvent(SalesUiEvent.TogglePaymentSheet)
                    showConfirmSaleDialog = true
                }
            )
        }
    }

    // ── Customer Bottom Sheet ──────────────────────────────────────────────
    if (state.isCustomerSheetVisible) {
        ModalBottomSheet(
            onDismissRequest = { onEvent(SalesUiEvent.ToggleCustomerSheet) },
            sheetState = customerSheetState,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            containerColor = WhiteColor,
            dragHandle = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    BottomSheetDefaults.DragHandle()
                    CustomerSheetHeader(
                        hasCustomer = state.hasCustomer,
                        onClose = { onEvent(SalesUiEvent.ToggleCustomerSheet) }
                    )
                    HorizontalDivider(color = BorderColor)
                }
            }
        ) {
            CustomerSheetContent(
                state = state,
                customerQuery = customerQuery,
                onCustomerQueryChange = {
                    customerQuery = it
                    onEvent(SalesUiEvent.SearchCustomers(it))
                },
                onEvent = onEvent
            )
        }
    }

    // ── Clear Cart Dialog ──────────────────────────────────────────────────
    AppConfirmDialog(
        visible = showClearCartDialog,
        onDismissRequest = { showClearCartDialog = false },
        title = "تفريغ السلة",
        message = "هل أنت متأكد من حذف جميع المنتجات من السلة؟ لا يمكن التراجع عن هذا الإجراء.",
        confirmText = "تفريغ",
        dismissText = "إلغاء",
        variant = AppDialogVariant.Warning,
        onConfirm = {
            onEvent(SalesUiEvent.ClearCart)
            showClearCartDialog = false
        }
    )

    // ── Confirm Sale Dialog ────────────────────────────────────────────────
    AppConfirmDialog(
        visible = showConfirmSaleDialog,
        onDismissRequest = { showConfirmSaleDialog = false },
        title = "تأكيد عملية البيع",
        message = "هل تريد إتمام عملية البيع وإصدار الفاتورة؟",
        confirmText = "إتمام البيع",
        dismissText = "مراجعة",
        variant = AppDialogVariant.Success,
        loading = state.isSubmittingSale,
        onConfirm = {
            onEvent(SalesUiEvent.ConfirmSale)
            showConfirmSaleDialog = false
        }
    )
}

// ---------------------------------------------------------------------------
// Loading Body
// ---------------------------------------------------------------------------

@Composable
private fun SalesLoadingBody(paddingValues: PaddingValues) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
        contentPadding = PaddingValues(AppDimens.Layout.screenPadding),
        verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing.large)
    ) {
        item { SalesLoading() }
    }
}

// ---------------------------------------------------------------------------
// Error Body
// ---------------------------------------------------------------------------

@Composable
private fun SalesErrorBody(
    message: String,
    paddingValues: PaddingValues,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
        contentAlignment = Alignment.Center
    ) {
        SalesError(
            visible = true,
            message = message,
            onRetry = onRetry
        )
    }
}

// ---------------------------------------------------------------------------
// Main Body
// ---------------------------------------------------------------------------

@Composable
private fun SalesMainBody(
    state: SalesUiState,
    paddingValues: PaddingValues,
    discountInput: String,
    onDiscountInputChange: (String) -> Unit,
    onEvent: (SalesUiEvent) -> Unit,
    onOpenProductsSheet: () -> Unit,
    onOpenCustomerSheet: () -> Unit,
    onOpenPaymentSheet: () -> Unit,
    onConfirmSale: () -> Unit,
    onClearCart: () -> Unit
) {
    val listState = rememberLazyListState()

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
        contentPadding = PaddingValues(
            start = AppDimens.Layout.screenPadding,
            end = AppDimens.Layout.screenPadding,
            top = AppDimens.Spacing.large,
            bottom = if (state.isCartEmpty) {
                AppDimens.Spacing.large
            } else {
                200.dp
            }
        ),
        verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing.large)
    ) {

        // ─────────────────────────────────────────────────────────────
        // Search Bar
        // ─────────────────────────────────────────────────────────────
        item(key = "search_bar") {
            SalesSearchBar(
                query = state.searchQuery,
                isSearching = state.isSearching,
                onQueryChange = {
                    onEvent(SalesUiEvent.SearchProducts(it))
                },
                onClear = {
                    onEvent(SalesUiEvent.ClearSearch)
                },
                onOpenScanner = {
                    onEvent(SalesUiEvent.OpenScanner)
                }
            )
        }

        // ─────────────────────────────────────────────────────────────
        // Quick Actions
        // ─────────────────────────────────────────────────────────────
        item(key = "quick_actions") {
            SalesQuickActions(
                cartCount = state.totalItemsCount,
                hasCustomer = state.hasCustomer,
                customerName = state.selectedCustomer?.displayName,
                onAddProducts = onOpenProductsSheet,
                onAddCustomer = onOpenCustomerSheet,
                onPay = onOpenPaymentSheet
            )
        }

        // ─────────────────────────────────────────────────────────────
        // Error Banner
        // ─────────────────────────────────────────────────────────────
        if (state.hasError) {
            item(key = "error_banner") {
                SalesError(
                    visible = true,
                    title = "حدث خطأ",
                    message = state.errorMessage ?: "تعذر إكمال العملية",
                    onRetry = {
                        onEvent(SalesUiEvent.Retry)
                    }
                )
            }
        }

        // ─────────────────────────────────────────────────────────────
        // Cart Content
        // ─────────────────────────────────────────────────────────────
        if (!state.isCartEmpty) {

            // Header
            item(key = "cart_header") {
                CartSectionHeader(
                    itemCount = state.cartItems.size,
                    totalQuantity = state.totalQuantity
                )
            }

            // Cart Items
            items(
                items = state.cartItems,
                key = { it.id }
            ) { cartItem ->

                CartItem(
                    item = cartItem,

                    onIncreaseClick = {
                        onEvent(
                            SalesUiEvent.IncreaseCartItemQuantity(
                                cartItem.id
                            )
                        )
                    },

                    onDecreaseClick = {
                        onEvent(
                            SalesUiEvent.DecreaseCartItemQuantity(
                                cartItem.id
                            )
                        )
                    },

                    onRemoveClick = {
                        onEvent(
                            SalesUiEvent.RemoveCartItem(
                                cartItem.id
                            )
                        )
                    },

                    showBarcode = true,
                    showNote = true,
                    showSubtotal = true
                )
            }

            // ─────────────────────────────────────────────────────────
            // Discount Section
            // ─────────────────────────────────────────────────────────
            item(key = "discount_section") {

                DiscountSection(
                    discountInput = discountInput,

                    onDiscountInputChange = onDiscountInputChange,

                    subtotalAmount = state.subtotalAmount,

                    enabled = !state.isSubmittingSale,

                    onApplyDiscount = {

                        val discount =
                            discountInput.toBigDecimalOrNull()
                                ?: BigDecimal.ZERO

                        if (state.cartItems.isNotEmpty()) {

                            state.cartItems.firstOrNull()?.let { firstItem ->

                                onEvent(
                                    SalesUiEvent.UpdateCartItemDiscount(
                                        firstItem.id,
                                        discount
                                    )
                                )
                            }
                        }
                    },

                    onClearDiscount = {
                        onDiscountInputChange("")
                    },

                    onPresetDiscountClick = { preset ->
                        onDiscountInputChange(
                            preset.toPlainString()
                        )
                    }
                )
            }

            // ─────────────────────────────────────────────────────────
            // Professional Transaction Panel
            // ─────────────────────────────────────────────────────────
            item(key = "sale_transaction_panel") {

                SaleTransactionPanel(

                    // Financial Data
                    subtotalAmount = state.subtotalAmount,
                    totalAmount = state.totalAmount,
                    discountAmount = state.discountAmount,
                    taxAmount = state.taxAmount,
                    paidAmount = state.paidAmount,
                    remainingAmount = state.remainingAmount,

                   // Customer Info
customerName = state.selectedCustomer?.displayName,
customerPhone = state.selectedCustomer?.phoneNumber, // تم التغيير من phone إلى phoneNumber

                    // Invoice Info
                    invoiceNumber = state.invoiceNumber,

                    // Payment
                    paymentMethodLabel =
                        state.selectedPaymentMethod?.name
                            ?: "نقدًا",

                    // Credit Sale
                    creditSaleEnabled = state.isCreditSale,

                    // Notes
                    notes = state.notes.orEmpty(),

                    onNotesChange = { notes ->
                        onEvent(
                            SalesUiEvent.UpdateNotes(
                                notes
                            )
                        )
                    },

                    // Actions
                    onConfirmSale = onConfirmSale,

                    onSaveDraft = {
                        onEvent(
                            SalesUiEvent.CreateDraftInvoice
                        )
                    },

                    onClearTransaction = onClearCart,

                    // States
                    loading = state.isSubmittingSale,

                    enabled =
                        !state.isSubmittingSale &&
                        state.isSaleReady
                )
            }

        } else {

            // ─────────────────────────────────────────────────────────
            // Empty State
            // ─────────────────────────────────────────────────────────
            item(key = "empty_state") {

                EmptySales(
                    onPrimaryActionClick = onOpenProductsSheet,

                    onSecondaryActionClick = {
                        onEvent(
                            SalesUiEvent.RefreshData
                        )
                    }
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Search Bar
// ---------------------------------------------------------------------------

@Composable
private fun SalesSearchBar(
    query: String,
    isSearching: Boolean,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    onOpenScanner: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing.small)
    ) {
        AppTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.weight(1f),
            label = "بحث المنتجات",
            placeholder = "اسم المنتج أو الباركود...",
            helperText = if (isSearching) "جارٍ البحث..." else null,
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = BrandPrimary
                )
            },
            trailingIcon = if (query.isNotBlank()) {
                {
                    IconButton(onClick = onClear) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "مسح",
                            tint = TextSecondaryColor
                        )
                    }
                }
            } else null,
            variant = AppTextFieldVariant.Outlined,
            size = AppTextFieldSize.Large,
            contentDescription = "بحث في المنتجات"
        )
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = BrandPrimary,
            contentColor = IconOnBrandColor,
            modifier = Modifier.size(56.dp)
        ) {
            IconButton(onClick = onOpenScanner) {
                Icon(
                    imageVector = Icons.Default.QrCodeScanner,
                    contentDescription = "مسح باركود",
                    tint = IconOnBrandColor
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Quick Action Chips
// ---------------------------------------------------------------------------

@Composable
private fun SalesQuickActions(
    cartCount: Int,
    hasCustomer: Boolean,
    customerName: String?,
    onAddProducts: () -> Unit,
    onAddCustomer: () -> Unit,
    onPay: () -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing.small),
        contentPadding = PaddingValues(horizontal = 2.dp)
    ) {
        item {
            QuickActionChip(
                label = "إضافة منتجات",
                icon = Icons.Rounded.AddShoppingCart,
                badge = if (cartCount > 0) cartCount.toString() else null,
                onClick = onAddProducts
            )
        }
        item {
            QuickActionChip(
                label = if (hasCustomer) (customerName ?: "العميل") else "إضافة عميل",
                icon = Icons.Rounded.Person,
                selected = hasCustomer,
                onClick = onAddCustomer
            )
        }
        item {
            QuickActionChip(
                label = "الدفع والاعتماد",
                icon = Icons.Rounded.Payment,
                highlighted = cartCount > 0,
                onClick = onPay
            )
        }
        item {
            QuickActionChip(
                label = "الفواتير",
                icon = Icons.Rounded.Receipt,
                onClick = {}
            )
        }
        item {
            QuickActionChip(
                label = "نقطة البيع",
                icon = Icons.Rounded.PointOfSale,
                onClick = {}
            )
        }
    }
}

@Composable
private fun QuickActionChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    badge: String? = null,
    selected: Boolean = false,
    highlighted: Boolean = false,
    onClick: () -> Unit
) {
    val bgColor = when {
        highlighted -> BrandPrimary
        selected -> BrandPrimaryDark
        else -> BrandPrimarySoft
    }
    val contentColor = when {
        highlighted || selected -> IconOnBrandColor
        else -> BrandPrimaryDark
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(999.dp),
        color = bgColor,
        contentColor = contentColor,
        border = if (!highlighted && !selected) BorderStroke(1.dp, BrandPrimaryTint) else null
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = AppDimens.Spacing.medium,
                vertical = AppDimens.Spacing.small
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing.extraSmall)
        ) {
            BadgedBox(
                badge = {
                    if (!badge.isNullOrBlank()) {
                        Badge(
                            containerColor = if (highlighted) WhiteColor else BrandPrimaryDark,
                            contentColor = if (highlighted) BrandPrimaryDark else WhiteColor
                        ) {
                            Text(
                                text = badge,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(18.dp)
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Cart Section Header
// ---------------------------------------------------------------------------

@Composable
private fun CartSectionHeader(
    itemCount: Int,
    totalQuantity: BigDecimal
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing.small)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(BrandPrimary, BrandPrimaryDark)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ShoppingCart,
                    contentDescription = null,
                    tint = IconOnBrandColor,
                    modifier = Modifier.size(18.dp)
                )
            }
            Column {
                Text(
                    text = "سلة المشتريات",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = TextPrimaryColor
                )
                Text(
                    text = "$itemCount منتج · الكمية: ${totalQuantity.toPlainString()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondaryColor
                )
            }
        }
        Surface(
            shape = RoundedCornerShape(999.dp),
            color = BrandPrimarySoft,
            border = BorderStroke(1.dp, BrandPrimaryTint)
        ) {
            Text(
                text = "$itemCount عنصر",
                modifier = Modifier.padding(
                    horizontal = AppDimens.Spacing.medium,
                    vertical = AppDimens.Spacing.extraSmall
                ),
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = BrandPrimaryDark
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Products Sheet
// ---------------------------------------------------------------------------

@Composable
private fun ProductsSheetHeader(
    cartCount: Int,
    onClose: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppDimens.Layout.screenPadding)
            .padding(bottom = AppDimens.Spacing.medium),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = "قائمة المنتجات",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.ExtraBold
                ),
                color = TextPrimaryColor
            )
            Text(
                text = if (cartCount > 0) "$cartCount منتج في السلة" else "اختر منتجًا لإضافته",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondaryColor
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing.small)
        ) {
            if (cartCount > 0) {
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = BrandPrimarySoft,
                    border = BorderStroke(1.dp, BrandPrimaryTint)
                ) {
                    Text(
                        text = "$cartCount",
                        modifier = Modifier.padding(
                            horizontal = AppDimens.Spacing.medium,
                            vertical = AppDimens.Spacing.extraSmall
                        ),
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.ExtraBold
                        ),
                        color = BrandPrimaryDark
                    )
                }
            }
            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "إغلاق",
                    tint = TextSecondaryColor
                )
            }
        }
    }
}
@Composable
private fun ProductsSheetContent(
    state: SalesUiState,
    onEvent: (SalesUiEvent) -> Unit
) {
    // نستخدم المكون الاحترافي الذي صممته في الملف السابق
    SaleProductList(
        state = state,
        onEvent = onEvent,
        modifier = Modifier
            .fillMaxWidth()
            .height(650.dp), // تحديد ارتفاع مناسب داخل الـ BottomSheet
        onProductClick = { product ->
            // هنا يمكنك إما إضافة المنتج مباشرة أو فتح تفاصيل
            onEvent(SalesUiEvent.AddProductToCart(product))
        }
    )
}

// ---------------------------------------------------------------------------
// Payment Sheet
// ---------------------------------------------------------------------------

@Composable
private fun PaymentSheetHeader(onClose: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppDimens.Layout.screenPadding)
            .padding(bottom = AppDimens.Spacing.medium),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = "الدفع والاعتماد",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.ExtraBold
                ),
                color = TextPrimaryColor
            )
            Text(
                text = "أكمل عملية الدفع وأصدر الفاتورة",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondaryColor
            )
        }
        IconButton(onClick = onClose) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "إغلاق",
                tint = TextSecondaryColor
            )
        }
    }
}

@Composable
private fun PaymentSheetContent(
    state: SalesUiState,
    paidAmountInput: String,
    onPaidAmountInputChange: (String) -> Unit,
    referenceNumber: String,
    onReferenceNumberChange: (String) -> Unit,
    creditSaleEnabled: Boolean,
    onCreditSaleToggle: (Boolean) -> Unit,
    onEvent: (SalesUiEvent) -> Unit,
    onConfirm: () -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(
            horizontal = AppDimens.Layout.screenPadding,
            vertical = AppDimens.Spacing.medium
        ),
        verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing.large),
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        item {
            PaymentSection(
                selectedPaymentMethod = state.selectedPaymentMethod,
                onPaymentMethodSelected = { onEvent(SalesUiEvent.SelectPaymentMethod(it)) },
                paidAmountInput = paidAmountInput,
                onPaidAmountInputChange = onPaidAmountInputChange,
                subtotalAmount = state.subtotalAmount,
                totalAmount = state.totalAmount,
                remainingAmount = state.remainingAmount,
                referenceNumber = referenceNumber,
                onReferenceNumberChange = onReferenceNumberChange,
                creditSaleEnabled = creditSaleEnabled,
                onCreditSaleToggle = onCreditSaleToggle,
                enabled = !state.isSubmittingSale,
                loading = state.isSubmittingSale,
                paymentMethods = PaymentMethod.values().toList(),
                onApplyPayment = onConfirm
            )
        }
        item { Spacer(modifier = Modifier.height(AppDimens.Spacing.large)) }
    }
}

// ---------------------------------------------------------------------------
// Customer Sheet
// ---------------------------------------------------------------------------

@Composable
private fun CustomerSheetHeader(
    hasCustomer: Boolean,
    onClose: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppDimens.Layout.screenPadding)
            .padding(bottom = AppDimens.Spacing.medium),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = "العملاء",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.ExtraBold
                ),
                color = TextPrimaryColor
            )
            Text(
                text = if (hasCustomer) "تم اختيار العميل" else "ابحث عن عميل أو أضف جديدًا",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondaryColor
            )
        }
        IconButton(onClick = onClose) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "إغلاق",
                tint = TextSecondaryColor
            )
        }
    }
}

@Composable
private fun CustomerSheetContent(
    state: SalesUiState,
    customerQuery: String,
    onCustomerQueryChange: (String) -> Unit,
    onEvent: (SalesUiEvent) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(
            horizontal = AppDimens.Layout.screenPadding,
            vertical = AppDimens.Spacing.medium
        ),
        verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing.large),
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        item {
            CustomerSection(
                customerQuery = customerQuery,
                onCustomerQueryChange = onCustomerQueryChange,
                searchResults = emptyList(),
                selectedCustomer = state.selectedCustomer,
                onCustomerSelected = { customer: Customer ->
                    onEvent(SalesUiEvent.SelectCustomer(customer))
                    onEvent(SalesUiEvent.ToggleCustomerSheet)
                },
                onRemoveCustomer = { onEvent(SalesUiEvent.RemoveCustomer) },
                onAddNewCustomerClick = { onEvent(SalesUiEvent.NavigateToCustomers) },
                enabled = !state.isLoadingCustomers,
                loading = state.isLoadingCustomers
            )
        }
        item { Spacer(modifier = Modifier.height(AppDimens.Spacing.extraLarge)) }
    }
}