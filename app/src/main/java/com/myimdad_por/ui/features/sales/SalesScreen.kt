@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.myimdad_por.ui.features.sales

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.myimdad_por.domain.model.Customer
import com.myimdad_por.domain.model.Product
import com.myimdad_por.domain.model.Sale
import com.myimdad_por.domain.model.SaleItem
import com.myimdad_por.ui.components.AppButton
import com.myimdad_por.ui.components.AppButtonSize
import com.myimdad_por.ui.components.AppButtonVariant
import com.myimdad_por.ui.components.AppTextField
import com.myimdad_por.ui.components.AppTextFieldSize
import com.myimdad_por.ui.components.ErrorState
import com.myimdad_por.ui.components.ErrorStateStyle
import com.myimdad_por.ui.components.LoadingIndicator
import com.myimdad_por.ui.components.LoadingIndicatorSize
import com.myimdad_por.ui.components.LoadingIndicatorVariant
import com.myimdad_por.ui.components.LoadingOverlay
import com.myimdad_por.ui.theme.AppDimens
import com.myimdad_por.ui.theme.AppTypography
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale

@Composable
fun SalesScreen(
    state: SalesUiState,
    onEvent: (SalesUiEvent) -> Unit,
    modifier: Modifier = Modifier,
    onNavigateBack: (() -> Unit)? = null
) {
    val currencyFormatter = remember(state.currencyCode) {
        createCurrencyFormatter(state.currencyCode)
    }

    var invoiceTouched by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(state.errorMessage) {
        // جاهز للربط مع Snackbar من الحاوية الأعلى عند الحاجة
    }

    LoadingOverlay(
        modifier = modifier,
        visible = state.isSubmitting,
        title = "جاري حفظ الفاتورة",
        message = "يرجى الانتظار...",
        variant = LoadingIndicatorVariant.Circular,
        size = LoadingIndicatorSize.Medium
    ) {
        Scaffold(
            topBar = {
                SalesTopBar(
                    state = state,
                    onEvent = onEvent,
                    onNavigateBack = onNavigateBack
                )
            },
            floatingActionButton = {
                if (state.canSubmit) {
                    FloatingActionButton(
                        onClick = { onEvent(SalesUiEvent.SubmitSale) },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "إتمام البيع"
                        )
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when {
                    state.isLoading && !state.hasContent -> {
                        LoadingIndicator(
                            modifier = Modifier.fillMaxSize(),
                            variant = LoadingIndicatorVariant.Circular,
                            size = LoadingIndicatorSize.Large,
                            title = "جاري تحميل البيانات",
                            message = "يتم تجهيز العملاء والمنتجات..."
                        )
                    }

                    state.hasError && !state.hasContent -> {
                        ErrorState(
                            message = state.errorMessage.orEmpty(),
                            style = ErrorStateStyle.FullScreen,
                            onRetry = { onEvent(SalesUiEvent.Retry) },
                            onDismiss = { onEvent(SalesUiEvent.ClearError) },
                            contentDescription = "خطأ في شاشة المبيعات"
                        )
                    }

                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                horizontal = AppDimens.Layout.screenPadding,
                                vertical = AppDimens.Spacing.medium
                            ),
                            verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing.medium)
                        ) {
                            item {
                                if (!state.errorMessage.isNullOrBlank()) {
                                    ErrorState(
                                        message = state.errorMessage,
                                        style = ErrorStateStyle.Inline,
                                        onRetry = { onEvent(SalesUiEvent.Retry) },
                                        onDismiss = { onEvent(SalesUiEvent.ClearError) },
                                        contentDescription = "رسالة خطأ"
                                    )
                                }
                            }

                            item {
                                SalesSearchCard(
                                    query = state.searchQuery,
                                    onQueryChange = { onEvent(SalesUiEvent.ChangeSearchQuery(it)) },
                                    onClear = { onEvent(SalesUiEvent.ChangeSearchQuery("")) },
                                    onRefresh = { onEvent(SalesUiEvent.Refresh) },
                                    loading = state.isRefreshing,
                                    totalCustomers = state.customers.size,
                                    totalProducts = state.availableProducts.size,
                                    totalSales = state.sales.size
                                )
                            }

                            item {
                                SalesSummaryCard(
                                    subtotal = state.subtotalAmount,
                                    tax = state.taxAmount,
                                    discount = state.discountAmount,
                                    total = state.totalAmount,
                                    paid = state.paidAmount,
                                    remaining = state.remainingAmount,
                                    change = state.changeAmount,
                                    itemCount = state.itemCount,
                                    totalQuantity = state.totalQuantity,
                                    currencyFormatter = currencyFormatter
                                )
                            }

                            item {
                                SalesDraftFormCard(
                                    state = state,
                                    currencyFormatter = currencyFormatter,
                                    onEvent = onEvent,
                                    invoiceTouched = invoiceTouched,
                                    onInvoiceTouched = { invoiceTouched = true }
                                )
                            }

                            item {
                                SalesCustomerAndPaymentCard(
                                    state = state,
                                    currencyFormatter = currencyFormatter
                                )
                            }

                            item {
                                SalesDraftItemsCard(
                                    items = state.draftItems,
                                    currencyFormatter = currencyFormatter,
                                    onIncrement = { item ->
                                        onEvent(
                                            SalesUiEvent.UpdateDraftItem(
                                                item.copy(quantity = (item.quantity + BigDecimal.ONE).money())
                                            )
                                        )
                                    },
                                    onDecrement = { item ->
                                        val next = item.quantity - BigDecimal.ONE
                                        if (next > BigDecimal.ZERO) {
                                            onEvent(
                                                SalesUiEvent.UpdateDraftItem(
                                                    item.copy(quantity = next.money())
                                                )
                                            )
                                        } else {
                                            onEvent(SalesUiEvent.RemoveDraftItem(item.id))
                                        }
                                    },
                                    onRemove = { item ->
                                        onEvent(SalesUiEvent.RemoveDraftItem(item.id))
                                    }
                                )
                            }

                            item {
                                SalesProductsCard(
                                    products = state.availableProducts,
                                    currencyFormatter = currencyFormatter,
                                    onAdd = { product ->
                                        onEvent(SalesUiEvent.AddProductToDraft(product, 1))
                                    }
                                )
                            }

                            item {
                                SalesCustomersCard(
                                    customers = state.customers,
                                    selectedCustomer = state.selectedCustomer,
                                    onSelect = { onEvent(SalesUiEvent.SelectCustomer(it)) }
                                )
                            }

                            item {
                                SalesHistoryCard(
                                    sales = state.sales,
                                    selectedSale = state.selectedSale,
                                    currencyFormatter = currencyFormatter,
                                    onSelect = { onEvent(SalesUiEvent.SelectSale(it.id)) }
                                )
                            }

                            item {
                                SalesActionsCard(
                                    state = state,
                                    onEvent = onEvent,
                                    onNavigateBack = onNavigateBack
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SalesTopBar(
    state: SalesUiState,
    onEvent: (SalesUiEvent) -> Unit,
    onNavigateBack: (() -> Unit)?
) {
    TopAppBar(
        title = {
            Column {
                Text(
                    text = "المبيعات",
                    style = AppTypography.titleLarge
                )
                Text(
                    text = when {
                        state.isSubmitting -> "جاري الحفظ"
                        state.selectedSale != null -> "فاتورة محددة"
                        state.draftItems.isNotEmpty() -> "فاتورة جديدة"
                        else -> "ابدأ بإدخال الفاتورة"
                    },
                    style = AppTypography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        navigationIcon = {
            if (onNavigateBack != null) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.Filled.ArrowBack,
                        contentDescription = "رجوع"
                    )
                }
            }
        },
        actions = {
            IconButton(onClick = { onEvent(SalesUiEvent.Refresh) }) {
                Icon(Icons.Filled.Refresh, contentDescription = "تحديث")
            }
            IconButton(onClick = { onEvent(SalesUiEvent.ResetForm) }) {
                Icon(Icons.Filled.DeleteOutline, contentDescription = "إعادة ضبط")
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
            actionIconContentColor = MaterialTheme.colorScheme.onSurface
        )
    )
}

@Composable
private fun SalesSearchCard(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    onRefresh: () -> Unit,
    loading: Boolean,
    totalCustomers: Int,
    totalProducts: Int,
    totalSales: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(AppDimens.Layout.screenPadding),
            verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing.medium)
        ) {
            Text("البحث والتحديث", style = AppTypography.titleMedium)

            AppTextField(
                value = query,
                onValueChange = onQueryChange,
                label = "ابحث عن عميل",
                placeholder = "اكتب اسم العميل",
                leadingIcon = {
                    Icon(Icons.Filled.Search, contentDescription = null)
                },
                size = AppTextFieldSize.Medium
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing.small)
            ) {
                AppButton(
                    text = "تحديث",
                    onClick = onRefresh,
                    variant = AppButtonVariant.Secondary,
                    size = AppButtonSize.Small,
                    loading = loading
                )
                AppButton(
                    text = "مسح البحث",
                    onClick = onClear,
                    variant = AppButtonVariant.Outlined,
                    size = AppButtonSize.Small
                )
            }

            Divider()

            StatLine(label = "العملاء", value = totalCustomers.toString())
            StatLine(label = "المنتجات", value = totalProducts.toString())
            StatLine(label = "الفواتير", value = totalSales.toString())
        }
    }
}

@Composable
private fun SalesSummaryCard(
    subtotal: BigDecimal,
    tax: BigDecimal,
    discount: BigDecimal,
    total: BigDecimal,
    paid: BigDecimal,
    remaining: BigDecimal,
    change: BigDecimal,
    itemCount: Int,
    totalQuantity: BigDecimal,
    currencyFormatter: (BigDecimal) -> String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(AppDimens.Layout.screenPadding),
            verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing.medium)
        ) {
            Text("ملخص الفاتورة", style = AppTypography.titleMedium)

            StatLine(label = "عدد الأصناف", value = itemCount.toString())
            StatLine(label = "الكمية", value = totalQuantity.stripTrailingZeros().toPlainString())
            StatLine(label = "المدفوع", value = currencyFormatter(paid))

            Divider()

            SummaryRow(label = "الإجمالي الفرعي", value = currencyFormatter(subtotal))
            SummaryRow(label = "الضريبة", value = currencyFormatter(tax))
            SummaryRow(label = "الخصم", value = currencyFormatter(discount))
            SummaryRow(label = "الإجمالي النهائي", value = currencyFormatter(total), emphasized = true)
            SummaryRow(label = "المتبقي", value = currencyFormatter(remaining))
            SummaryRow(label = "الباقي", value = currencyFormatter(change))
        }
    }
}

@Composable
private fun SalesDraftFormCard(
    state: SalesUiState,
    currencyFormatter: (BigDecimal) -> String,
    onEvent: (SalesUiEvent) -> Unit,
    invoiceTouched: Boolean,
    onInvoiceTouched: () -> Unit
) {
    val draftError = state.validationErrors["draftItems"]
    val invoiceError = state.validationErrors["invoiceNumber"]
    val paymentError = state.validationErrors["paymentMethod"]
    val creditError = state.validationErrors["credit"]

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(AppDimens.Layout.screenPadding),
            verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing.medium)
        ) {
            Text("بيانات الفاتورة", style = AppTypography.titleMedium)

            AppTextField(
                value = state.invoiceNumber,
                onValueChange = {
                    onInvoiceTouched()
                    onEvent(SalesUiEvent.UpdateInvoiceNumber(it))
                },
                label = "رقم الفاتورة",
                placeholder = "أدخل الرقم المرجعي",
                errorText = invoiceError,
                helperText = if (invoiceTouched || state.invoiceNumber.isNotBlank()) null else "أدخل رقمًا واضحًا",
                size = AppTextFieldSize.Medium,
                contentDescription = "رقم الفاتورة"
            )

            AppTextField(
                value = state.note,
                onValueChange = { onEvent(SalesUiEvent.UpdateNote(it)) },
                label = "ملاحظات",
                placeholder = "ملاحظات داخلية",
                singleLine = false,
                minLines = 2,
                maxLines = 4,
                size = AppTextFieldSize.Large,
                contentDescription = "ملاحظات الفاتورة"
            )

            AppTextField(
                value = currencyFormatter(state.paidAmount),
                onValueChange = { onEvent(SalesUiEvent.UpdatePaidAmount(it)) },
                label = "المبلغ المدفوع",
                placeholder = "0.00",
                helperText = "يمكن إدخال الرقم مباشرة",
                size = AppTextFieldSize.Medium,
                contentDescription = "المبلغ المدفوع"
            )

            if (!draftError.isNullOrBlank() || !paymentError.isNullOrBlank() || !creditError.isNullOrBlank()) {
                Column(verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing.extraSmall)) {
                    listOfNotNull(draftError, paymentError, creditError).forEach { message ->
                        Text(
                            text = "• $message",
                            style = AppTypography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SalesCustomerAndPaymentCard(
    state: SalesUiState,
    currencyFormatter: (BigDecimal) -> String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(AppDimens.Layout.screenPadding),
            verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing.medium)
        ) {
            Text("العميل والدفع", style = AppTypography.titleMedium)

            SummaryField(
                label = "العميل المختار",
                value = state.customerName.ifBlank { "لم يتم اختيار عميل" },
                trailing = if (state.canUseCredit) "يسمح بالآجل" else "نقدي"
            )

            SummaryField(
                label = "طريقة الدفع",
                value = state.paymentMethodName.ifBlank { "غير محددة" },
                trailing = if (state.selectedPaymentMethod == null) "اختر طريقة الدفع" else "جاهزة"
            )

            SummaryField(
                label = "الرصيد المتبقي",
                value = currencyFormatter(state.remainingAmount),
                trailing = when {
                    state.remainingAmount > BigDecimal.ZERO && state.canUseCredit -> "آجل متاح"
                    state.remainingAmount > BigDecimal.ZERO -> "لا يسمح بالآجل"
                    else -> "مكتمل"
                }
            )
        }
    }
}

@Composable
private fun SalesDraftItemsCard(
    items: List<SaleItem>,
    currencyFormatter: (BigDecimal) -> String,
    onIncrement: (SaleItem) -> Unit,
    onDecrement: (SaleItem) -> Unit,
    onRemove: (SaleItem) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(AppDimens.Layout.screenPadding),
            verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing.medium)
        ) {
            Text("الأصناف المختارة", style = AppTypography.titleMedium)

            if (items.isEmpty()) {
                Text(
                    text = "لم تتم إضافة أي صنف بعد.",
                    style = AppTypography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                items.forEach { item ->
                    DraftItemRow(
                        item = item,
                        currencyFormatter = currencyFormatter,
                        onIncrement = { onIncrement(item) },
                        onDecrement = { onDecrement(item) },
                        onRemove = { onRemove(item) }
                    )
                }
            }
        }
    }
}

@Composable
private fun DraftItemRow(
    item: SaleItem,
    currencyFormatter: (BigDecimal) -> String,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onRemove: () -> Unit
) {
    val lineTotal = item.calculateSubtotal().money()

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
    ) {
        Column(
            modifier = Modifier.padding(AppDimens.Spacing.medium),
            verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing.small)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.fillMaxWidth(0.85f)) {
                    Text(
                        text = item.productName,
                        style = AppTypography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = listOfNotNull(
                            item.productId.takeIf { it.isNotBlank() },
                            item.unit?.toString(),
                            if (item.isReturn) "مرتجع" else null
                        ).joinToString(" • ").ifBlank { "عنصر فاتورة" },
                        style = AppTypography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(onClick = onRemove) {
                    Icon(
                        imageVector = Icons.Filled.DeleteOutline,
                        contentDescription = "حذف الصنف"
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${item.quantity.stripTrailingZeros().toPlainString()} × ${currencyFormatter(item.unitPrice)}",
                    style = AppTypography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = currencyFormatter(lineTotal),
                    style = AppTypography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing.small)) {
                AppButton(
                    text = "-",
                    onClick = onDecrement,
                    variant = AppButtonVariant.Outlined,
                    size = AppButtonSize.Small
                )
                AppButton(
                    text = "+",
                    onClick = onIncrement,
                    variant = AppButtonVariant.Primary,
                    size = AppButtonSize.Small
                )
            }
        }
    }
}

@Composable
private fun SalesProductsCard(
    products: List<Product>,
    currencyFormatter: (BigDecimal) -> String,
    onAdd: (Product) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(AppDimens.Layout.screenPadding),
            verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing.medium)
        ) {
            Text("المنتجات المتاحة", style = AppTypography.titleMedium)

            if (products.isEmpty()) {
                Text(
                    text = "لا توجد منتجات متاحة حالياً.",
                    style = AppTypography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                products.take(8).forEach { product ->
                    ProductRow(
                        product = product,
                        currencyFormatter = currencyFormatter,
                        onAdd = { onAdd(product) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ProductRow(
    product: Product,
    currencyFormatter: (BigDecimal) -> String,
    onAdd: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onAdd),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
    ) {
        Row(
            modifier = Modifier.padding(AppDimens.Spacing.medium),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.fillMaxWidth(0.75f)) {
                Text(
                    text = product.effectiveName,
                    style = AppTypography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = listOfNotNull(
                        product.normalizedBarcode.takeIf { it.isNotBlank() },
                        product.unitOfMeasure?.toString()
                    ).joinToString(" • "),
                    style = AppTypography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = currencyFormatter(product.price.money()),
                    style = AppTypography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "إضافة",
                    style = AppTypography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SalesCustomersCard(
    customers: List<Customer>,
    selectedCustomer: Customer?,
    onSelect: (Customer) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(AppDimens.Layout.screenPadding),
            verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing.medium)
        ) {
            Text("العملاء", style = AppTypography.titleMedium)

            if (customers.isEmpty()) {
                Text(
                    text = "لا توجد نتائج عملاء.",
                    style = AppTypography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                customers.take(8).forEach { customer ->
                    CustomerRow(
                        customer = customer,
                        selected = selectedCustomer?.id == customer.id,
                        onSelect = { onSelect(customer) }
                    )
                }
            }
        }
    }
}

@Composable
private fun CustomerRow(
    customer: Customer,
    selected: Boolean,
    onSelect: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect),
        shape = MaterialTheme.shapes.medium,
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
        }
    ) {
        Column(
            modifier = Modifier.padding(AppDimens.Spacing.medium),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = customer.displayName,
                style = AppTypography.titleSmall,
                color = if (selected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (!customer.taxNumber.isNullOrBlank()) {
                Text(
                    text = "الرقم الضريبي: ${customer.taxNumber}",
                    style = AppTypography.bodySmall,
                    color = if (selected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }
    }
}

@Composable
private fun SalesHistoryCard(
    sales: List<Sale>,
    selectedSale: Sale?,
    currencyFormatter: (BigDecimal) -> String,
    onSelect: (Sale) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(AppDimens.Layout.screenPadding),
            verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing.medium)
        ) {
            Text("الفواتير السابقة", style = AppTypography.titleMedium)

            if (sales.isEmpty()) {
                Text(
                    text = "لا توجد فواتير محفوظة بعد.",
                    style = AppTypography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                sales.take(6).forEach { sale ->
                    SaleRow(
                        sale = sale,
                        selected = selectedSale?.id == sale.id,
                        currencyFormatter = currencyFormatter,
                        onSelect = { onSelect(sale) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SaleRow(
    sale: Sale,
    selected: Boolean,
    currencyFormatter: (BigDecimal) -> String,
    onSelect: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect),
        shape = MaterialTheme.shapes.medium,
        color = if (selected) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
        }
    ) {
        Column(
            modifier = Modifier.padding(AppDimens.Spacing.medium),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = sale.invoiceNumber.ifBlank { "فاتورة بدون رقم" },
                style = AppTypography.titleSmall,
                color = if (selected) {
                    MaterialTheme.colorScheme.onSecondaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "عدد الأصناف: ${sale.items.size}",
                style = AppTypography.bodySmall,
                color = if (selected) {
                    MaterialTheme.colorScheme.onSecondaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            Text(
                text = "المدفوع: ${currencyFormatter(sale.paidAmount.money())}",
                style = AppTypography.bodySmall,
                color = if (selected) {
                    MaterialTheme.colorScheme.onSecondaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            if (!sale.note.isNullOrBlank()) {
                Text(
                    text = sale.note,
                    style = AppTypography.bodySmall,
                    color = if (selected) {
                        MaterialTheme.colorScheme.onSecondaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun SalesActionsCard(
    state: SalesUiState,
    onEvent: (SalesUiEvent) -> Unit,
    onNavigateBack: (() -> Unit)?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(AppDimens.Layout.screenPadding),
            verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing.medium)
        ) {
            Text("الإجراءات", style = AppTypography.titleMedium)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing.small)
            ) {
                AppButton(
                    text = "مسح النموذج",
                    onClick = { onEvent(SalesUiEvent.ResetForm) },
                    variant = AppButtonVariant.Outlined,
                    size = AppButtonSize.Medium
                )
                AppButton(
                    text = "حفظ كمسودة",
                    onClick = { onEvent(SalesUiEvent.SaveDraft) },
                    variant = AppButtonVariant.Secondary,
                    size = AppButtonSize.Medium
                )
            }

            AppButton(
                text = if (state.isSubmitting) "جاري الحفظ..." else "إتمام البيع",
                onClick = { onEvent(SalesUiEvent.SubmitSale) },
                variant = AppButtonVariant.Primary,
                size = AppButtonSize.Large,
                enabled = state.canSubmit,
                loading = state.isSubmitting,
                fullWidth = true
            )

            if (onNavigateBack != null) {
                AppButton(
                    text = "الرجوع",
                    onClick = onNavigateBack,
                    variant = AppButtonVariant.Text,
                    size = AppButtonSize.Medium,
                    fullWidth = true
                )
            }
        }
    }
}

@Composable
private fun SummaryRow(
    label: String,
    value: String,
    emphasized: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = if (emphasized) AppTypography.titleSmall else AppTypography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = if (emphasized) AppTypography.titleSmall else AppTypography.bodyMedium,
            color = if (emphasized) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun StatLine(
    label: String,
    value: String
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppDimens.Radius.medium),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
    ) {
        Row(
            modifier = Modifier.padding(AppDimens.Spacing.medium),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = AppTypography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = AppTypography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun SummaryField(
    label: String,
    value: String,
    trailing: String
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
    ) {
        Row(
            modifier = Modifier.padding(AppDimens.Spacing.medium),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.fillMaxWidth(0.75f)) {
                Text(
                    text = label,
                    style = AppTypography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = value,
                    style = AppTypography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Text(
                text = trailing,
                style = AppTypography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun createCurrencyFormatter(currencyCode: String): (BigDecimal) -> String {
    val code = currencyCode.uppercase()
    val locale = when (code) {
        "SDG", "SAR", "AED", "EGP", "QAR", "KWD", "BHD", "OMR" -> Locale("ar", "SA")
        else -> Locale.getDefault()
    }

    val formatter = NumberFormat.getNumberInstance(locale).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }

    return { amount: BigDecimal ->
        "${formatter.format(amount.money())} $code"
    }
}

private fun BigDecimal.money(): BigDecimal = setScale(2, RoundingMode.HALF_UP)