package com.myimdad_por.ui.features.sales

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.myimdad_por.domain.model.Product
import com.myimdad_por.domain.model.Sale
import com.myimdad_por.domain.model.SaleItem
import com.myimdad_por.ui.components.AppButton
import com.myimdad_por.ui.components.AppButtonSize
import com.myimdad_por.ui.components.AppButtonVariant
import com.myimdad_por.ui.components.AppTextField
import com.myimdad_por.ui.components.AppTextFieldSize
import com.myimdad_por.ui.components.AppTextFieldVariant
import com.myimdad_por.ui.components.AppTopBar
import com.myimdad_por.ui.components.ErrorState
import com.myimdad_por.ui.components.ErrorStateStyle
import com.myimdad_por.ui.components.LoadingIndicator
import com.myimdad_por.ui.components.LoadingIndicatorSize
import com.myimdad_por.ui.components.LoadingIndicatorVariant
import com.myimdad_por.ui.components.ProductCard
import com.myimdad_por.core.utils.CurrencyFormatter
import com.myimdad_por.ui.theme.AppDimens
import com.myimdad_por.ui.theme.BrandPrimary
import com.myimdad_por.ui.theme.BrandPrimaryDark
import com.myimdad_por.ui.theme.BrandPrimarySoft
import com.myimdad_por.ui.theme.ErrorColor
import com.myimdad_por.ui.theme.InfoColor
import com.myimdad_por.ui.theme.SurfaceColor
import com.myimdad_por.ui.theme.TextPrimaryColor
import com.myimdad_por.ui.theme.TextSecondaryColor
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale

@Composable
fun SalesScreen(
    state: SalesUiState,
    onEvent: (SalesUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val visibleProducts by remember(state.availableProducts, state.searchQuery) {
        derivedStateOf {
            val query = state.searchQuery.trim().lowercase(Locale.getDefault())
            if (query.isBlank()) {
                state.availableProducts
            } else {
                state.availableProducts.filter { product ->
                    product.filterableText().contains(query)
                }
            }
        }
    }

    val visibleSales by remember(state.sales, state.searchQuery) {
        derivedStateOf {
            val query = state.searchQuery.trim().lowercase(Locale.getDefault())
            if (query.isBlank()) {
                state.sales
            } else {
                state.sales.filter { sale ->
                    sale.filterableText().contains(query)
                }
            }
        }
    }

    val selectedCustomerName = state.selectedCustomer?.displayName.orEmpty()
    val selectedPaymentMethodName = state.selectedPaymentMethod?.displayName.orEmpty()
    val hasInitialLoading = state.isLoading && !state.hasContent

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            AppTopBar(
                title = "المبيعات",
                subtitle = buildSalesSubtitle(state),
                onBackClick = null,
                actions = {
                    AppButton(
                        text = "تحديث",
                        onClick = { onEvent(SalesUiEvent.Refresh) },
                        variant = AppButtonVariant.Text,
                        size = AppButtonSize.Small,
                        enabled = !state.isSubmitting,
                        loading = state.isRefreshing,
                        leadingIcon = { Icon(Icons.Filled.Refresh, contentDescription = null) }
                    )
                }
            )
        },
        bottomBar = {
            SalesBottomBar(
                state = state,
                onEvent = onEvent
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
        ) {
            if (hasInitialLoading) {
                LoadingIndicator(
                    modifier = Modifier.fillMaxSize(),
                    variant = LoadingIndicatorVariant.Dots,
                    size = LoadingIndicatorSize.Large,
                    title = "جاري تجهيز شاشة المبيعات",
                    message = "يتم تحميل البيانات الأساسية حالياً"
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = AppDimens.Layout.screenPadding,
                    end = AppDimens.Layout.screenPadding,
                    top = AppDimens.Spacing.medium,
                    bottom = 140.dp
                ),
                verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing.medium)
            ) {
                item {
                    SalesHeroCard(
                        state = state,
                        customerName = selectedCustomerName,
                        paymentMethodName = selectedPaymentMethodName
                    )
                }

                item {
                    AppTextField(
                        value = state.searchQuery,
                        onValueChange = { onEvent(SalesUiEvent.ChangeSearchQuery(it)) },
                        label = "بحث سريع",
                        placeholder = "ابحث في المبيعات أو العملاء أو المنتجات",
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                        trailingIcon = if (state.searchQuery.isNotBlank()) {
                            {
                                AppButton(
                                    text = "",
                                    onClick = { onEvent(SalesUiEvent.ChangeSearchQuery("")) },
                                    variant = AppButtonVariant.Text,
                                    size = AppButtonSize.Small,
                                    enabled = !state.isReadOnlyMode,
                                    leadingIcon = { Icon(Icons.Filled.Clear, contentDescription = null) }
                                )
                            }
                        } else {
                            null
                        },
                        variant = AppTextFieldVariant.Outlined,
                        size = AppTextFieldSize.Medium,
                        enabled = !state.isReadOnlyMode
                    )
                }

                if (state.hasError) {
                    item {
                        ErrorState(
                            message = state.errorMessage.orEmpty(),
                            title = "تعذر متابعة المبيعات",
                            details = validationDetails(state.validationErrors),
                            onRetry = { onEvent(SalesUiEvent.Retry) },
                            onDismiss = { onEvent(SalesUiEvent.ClearError) },
                            style = ErrorStateStyle.Card
                        )
                    }
                }

                if (state.validationErrors.isNotEmpty()) {
                    item {
                        ValidationBanner(errors = state.validationErrors.values.toList())
                    }
                }

                item {
                    SectionHeader(
                        title = "مسودة البيع",
                        subtitle = if (state.hasDraftItems) {
                            "${state.itemCount} صنف • ${formatMoney(state.totalAmount, state.currencyCode)}"
                        } else {
                            "أضف أصناف البيع أولاً"
                        },
                        icon = Icons.Filled.ShoppingCart,
                        accent = BrandPrimary
                    )
                }

                if (state.hasDraftItems) {
                    items(state.draftItems, key = { saleItemKey(it) }) { item ->
                        DraftItemCard(
                            item = item,
                            currencyCode = state.currencyCode,
                            enabled = !state.isReadOnlyMode,
                            onIncrease = {
                                onEvent(SalesUiEvent.UpdateDraftItem(item.copy(quantity = item.quantity + BigDecimal.ONE)))
                            },
                            onDecrease = {
                                val nextQuantity = item.quantity - BigDecimal.ONE
                                if (nextQuantity <= BigDecimal.ZERO) {
                                    onEvent(SalesUiEvent.RemoveDraftItem(saleItemKey(item)))
                                } else {
                                    onEvent(SalesUiEvent.UpdateDraftItem(item.copy(quantity = nextQuantity)))
                                }
                            },
                            // السطر 261: استخدم الدالة المعرفة في أسفل الملف لجلب المفتاح 
                            onRemove = { onEvent(SalesUiEvent.RemoveDraftItem(saleItemKey(item))) }
                        )
                    }
                } else {
                    item {
                        EmptyCard(
                            title = "لا توجد أصناف داخل المسودة",
                            message = "ابدأ بإضافة المنتجات المناسبة إلى عملية البيع الحالية.",
                            icon = Icons.Filled.ReceiptLong
                        )
                    }
                }

                item {
                    TotalsCard(
                        state = state,
                        currencyCode = state.currencyCode
                    )
                }

                item {
                    SectionHeader(
                        title = "تفاصيل العملية",
                        subtitle = "المعلومات الأساسية قبل الإرسال والحفظ",
                        icon = Icons.Filled.Payment,
                        accent = BrandPrimaryDark
                    )
                }

                item {
                    DetailsPanel(
                        state = state,
                        onEvent = onEvent,
                        customerName = selectedCustomerName,
                        paymentMethodName = selectedPaymentMethodName
                    )
                }

                item {
                    SectionHeader(
                        title = "المنتجات المتاحة",
                        subtitle = if (visibleProducts.isEmpty()) {
                            "لا توجد نتائج مطابقة"
                        } else {
                            "${visibleProducts.size} منتج جاهز للبيع"
                        },
                        icon = Icons.Filled.Inventory2,
                        accent = InfoColor
                    )
                }

                if (visibleProducts.isNotEmpty()) {
                    items(visibleProducts.chunked(2)) { rowProducts ->
                        ProductRow(
                            products = rowProducts,
                            enabled = !state.isReadOnlyMode,
                            onProductClick = { product ->
                                onEvent(SalesUiEvent.AddProductToDraft(product, 1))
                            }
                        )
                    }
                } else {
                    item {
                        EmptyCard(
                            title = "لا توجد منتجات ظاهرة الآن",
                            message = "جرّب تغيير البحث أو أضف بيانات المنتجات من المصدر المرتبط.",
                            icon = Icons.Filled.Inventory2
                        )
                    }
                }

                item {
                    SectionHeader(
                        title = "أحدث المبيعات",
                        subtitle = if (visibleSales.isEmpty()) {
                            "لا توجد مبيعات مطابقة للبحث"
                        } else {
                            "${visibleSales.size} عملية بيع ظاهرة"
                        },
                        icon = Icons.Filled.TrendingUp,
                        accent = BrandPrimary
                    )
                }

                if (visibleSales.isNotEmpty()) {
                    items(visibleSales.take(8), key = { it.id }) { sale ->
                        RecentSaleCard(
                            sale = sale,
                            currencyCode = state.currencyCode,
                            onClick = { onEvent(SalesUiEvent.SelectSale(sale.id)) }
                        )
                    }
                } else {
                    item {
                        EmptyCard(
                            title = "لا توجد مبيعات مسجلة",
                            message = "ستظهر أحدث العمليات هنا بمجرد توفرها.",
                            icon = Icons.Filled.TrendingUp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SalesBottomBar(
    state: SalesUiState,
    onEvent: (SalesUiEvent) -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 4.dp,
        shadowElevation = 8.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppDimens.Layout.screenPadding),
            verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing.small)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing.small)
            ) {
                AppButton(
                    text = "إعادة الضبط",
                    onClick = { onEvent(SalesUiEvent.ResetForm) },
                    variant = AppButtonVariant.Outlined,
                    size = AppButtonSize.Medium,
                    enabled = !state.isSubmitting && !state.isLoading,
                    fullWidth = true,
                    leadingIcon = { Icon(Icons.Filled.Clear, contentDescription = null) }
                )
                AppButton(
                    text = "حفظ كمسودة",
                    onClick = { onEvent(SalesUiEvent.SaveDraft) },
                    variant = AppButtonVariant.Secondary,
                    size = AppButtonSize.Medium,
                    enabled = state.hasDraftItems && !state.isSubmitting && !state.isLoading && !state.isReadOnlyMode,
                    loading = state.isSubmitting,
                    fullWidth = true,
                    leadingIcon = { Icon(Icons.Filled.ReceiptLong, contentDescription = null) }
                )
            }
            AppButton(
                text = if (state.isReadOnlyMode) "وضع القراءة فقط" else "إرسال عملية البيع",
                onClick = { onEvent(SalesUiEvent.SubmitSale) },
                variant = AppButtonVariant.Primary,
                size = AppButtonSize.Large,
                enabled = state.canSubmit && !state.isReadOnlyMode,
                loading = state.isSubmitting,
                fullWidth = true,
                leadingIcon = { Icon(Icons.Filled.Payment, contentDescription = null) }
            )
        }
    }
}

@Composable
private fun SalesHeroCard(
    state: SalesUiState,
    customerName: String,
    paymentMethodName: String
) {
    val heroBrush = remember {
        Brush.horizontalGradient(
            colors = listOf(
                BrandPrimary,
                BrandPrimaryDark
            )
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppDimens.Radius.large),
        colors = CardDefaults.cardColors(containerColor = SurfaceColor),
        elevation = CardDefaults.cardElevation(defaultElevation = AppDimens.Elevation.high)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(heroBrush)
                    .padding(AppDimens.Layout.screenPadding)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing.small)) {
                    Text(
                        text = "التحكم الذكي في المبيعات",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White
                    )
                    Text(
                        text = "إدارة المسودة والملخصات والمنتجات مع واجهة خضراء متوازنة واحترافية.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.92f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing.small),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StatusChip(
                            text = if (state.isSubmitting) "جارٍ التنفيذ" else "جاهز",
                            containerColor = Color.White.copy(alpha = 0.16f),
                            contentColor = Color.White,
                            icon = Icons.Filled.Done
                        )
                        if (state.canAccessCreditSales) {
                            StatusChip(
                                text = "بيع آجل متاح",
                                containerColor = Color.White.copy(alpha = 0.16f),
                                contentColor = Color.White,
                                icon = Icons.Filled.AttachMoney
                            )
                        }
                        if (state.canAccessReturns) {
                            StatusChip(
                                text = "مرتجعات مفعلة",
                                containerColor = Color.White.copy(alpha = 0.16f),
                                contentColor = Color.White,
                                icon = Icons.Filled.Refresh
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier.padding(AppDimens.Layout.screenPadding),
                verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing.medium)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing.small)
                ) {
                    MiniStatCard(
                        title = "الإجمالي",
                        value = formatMoney(state.totalAmount, state.currencyCode),
                        icon = Icons.Filled.AttachMoney,
                        accent = BrandPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    MiniStatCard(
                        title = "عدد الأصناف",
                        value = state.itemCount.toString(),
                        icon = Icons.Filled.ShoppingCart,
                        accent = BrandPrimaryDark,
                        modifier = Modifier.weight(1f)
                    )
                    MiniStatCard(
                        title = "المدفوع",
                        value = formatMoney(state.paidAmount, state.currencyCode),
                        icon = Icons.Filled.Payment,
                        accent = InfoColor,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing.small)
                ) {
                    DetailChip(
                        label = "العميل",
                        value = customerName.ifBlank { "غير محدد" },
                        modifier = Modifier.weight(1f)
                    )
                    DetailChip(
                        label = "طريقة الدفع",
                        value = paymentMethodName.ifBlank { "غير محددة" },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailsPanel(
    state: SalesUiState,
    onEvent: (SalesUiEvent) -> Unit,
    customerName: String,
    paymentMethodName: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppDimens.Radius.large),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = AppDimens.Elevation.low)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppDimens.Layout.screenPadding),
            verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing.medium)
        ) {
            DetailChip(
                label = "العميل المحدد",
                value = customerName.ifBlank { "لم يتم الاختيار بعد" },
                accent = BrandPrimary,
                trailingLabel = if (state.selectedCustomer != null) "محدد" else "فارغ"
            )
            DetailChip(
                label = "طريقة الدفع",
                value = paymentMethodName.ifBlank { "لم يتم الاختيار بعد" },
                accent = BrandPrimaryDark,
                trailingLabel = if (state.selectedPaymentMethod != null) "محددة" else "فارغة"
            )

            AppTextField(
                value = state.invoiceNumber,
                onValueChange = { onEvent(SalesUiEvent.UpdateInvoiceNumber(it)) },
                label = "رقم الفاتورة",
                placeholder = "اتركه فارغًا إن كان سيُولد تلقائيًا",
                leadingIcon = { Icon(Icons.Filled.ReceiptLong, contentDescription = null) },
                enabled = !state.isReadOnlyMode,
                variant = AppTextFieldVariant.Outlined,
                size = AppTextFieldSize.Medium
            )

            AppTextField(
                value = state.note,
                onValueChange = { onEvent(SalesUiEvent.UpdateNote(it)) },
                label = "ملاحظات",
                placeholder = "أي ملاحظة إضافية مرتبطة بعملية البيع",
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                enabled = !state.isReadOnlyMode,
                singleLine = false,
                minLines = 3,
                maxLines = 5,
                variant = AppTextFieldVariant.Outlined,
                size = AppTextFieldSize.Large
            )

            AppTextField(
                value = state.paidAmount.stripForInput(),
                onValueChange = { onEvent(SalesUiEvent.UpdatePaidAmount(it)) },
                label = "المبلغ المدفوع",
                placeholder = "0.00",
                leadingIcon = { Icon(Icons.Filled.AttachMoney, contentDescription = null) },
                enabled = !state.isReadOnlyMode,
                variant = AppTextFieldVariant.Outlined,
                size = AppTextFieldSize.Medium
            )

            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing.small)
            ) {
                SummaryTile(
                    title = "المتبقي",
                    value = formatMoney(state.remainingAmount, state.currencyCode),
                    modifier = Modifier.weight(1f),
                    accent = ErrorColor
                )
                SummaryTile(
                    title = "الباقي",
                    value = formatMoney(state.changeAmount, state.currencyCode),
                    modifier = Modifier.weight(1f),
                    accent = BrandPrimaryDark
                )
            }
        }
    }
}

@Composable
private fun TotalsCard(
    state: SalesUiState,
    currencyCode: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppDimens.Radius.large),
        colors = CardDefaults.cardColors(containerColor = BrandPrimarySoft),
        elevation = CardDefaults.cardElevation(defaultElevation = AppDimens.Elevation.low),
        border = BorderStroke(1.dp, BrandPrimary.copy(alpha = 0.20f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppDimens.Layout.screenPadding),
            verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing.small)
        ) {
            Text(
                text = "الملخص المالي",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimaryColor
            )
            MoneyRow("الإجمالي الفرعي", formatMoney(state.subtotalAmount, currencyCode), BrandPrimaryDark)
            MoneyRow("الضريبة", formatMoney(state.taxAmount, currencyCode), BrandPrimaryDark)
            MoneyRow("الخصم", formatMoney(state.discountAmount, currencyCode), ErrorColor)
            Divider(color = BrandPrimary.copy(alpha = 0.18f))
            MoneyRow(
                label = "الإجمالي النهائي",
                amount = formatMoney(state.totalAmount, currencyCode),
                accent = BrandPrimary
            )
        }
    }
}

@Composable
private fun DraftItemCard(
    item: SaleItem,
    currencyCode: String,
    enabled: Boolean,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppDimens.Radius.large),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = AppDimens.Elevation.low),
        border = BorderStroke(1.dp, BrandPrimary.copy(alpha = 0.16f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppDimens.Layout.screenPadding),
            verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing.small)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing.small)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.productName.ifBlank { "صنف غير مسمى" },
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimaryColor,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "الباركود/المرجع: ${item.productId}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondaryColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                AppButton(
                    text = "",
                    onClick = onRemove,
                    variant = AppButtonVariant.Text,
                    size = AppButtonSize.Small,
                    enabled = enabled,
                    leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null, tint = ErrorColor) }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing.small),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CounterButton(
                    text = "−",
                    onClick = onDecrease,
                    enabled = enabled
                )
                CounterButton(
                    text = item.quantity.stripTrailingZeros().toPlainString(),
                    onClick = { },
                    enabled = false,
                    emphasized = true
                )
                CounterButton(
                    text = "+",
                    onClick = onIncrease,
                    enabled = enabled
                )

                Spacer(modifier = Modifier.width(AppDimens.Spacing.small))

                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    MoneyRow(
                        label = "سعر الوحدة",
                        amount = formatMoney(item.unitPrice, currencyCode),
                        accent = BrandPrimaryDark,
                        compact = true
                    )
                    MoneyRow(
                        label = "الإجمالي",
                        amount = formatMoney(item.calculateSubtotal(), currencyCode),
                        accent = BrandPrimary,
                        compact = true
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing.small)
            ) {
                StatusChip(
                    text = "الكمية: ${item.quantity.stripTrailingZeros().toPlainString()}",
                    containerColor = BrandPrimarySoft,
                    contentColor = BrandPrimaryDark,
                    icon = Icons.Filled.ShoppingCart
                )
                item.unit?.toString()?.takeIf { it.isNotBlank() }?.let { unit ->
                    StatusChip(
                        text = unit,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = TextSecondaryColor,
                        icon = Icons.Filled.Inventory2
                    )
                }
            }
        }
    }
}

@Composable
private fun ProductRow(
    products: List<Product>,
    enabled: Boolean,
    onProductClick: (Product) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing.small)
    ) {
        products.forEachIndexed { index, product ->
            Box(modifier = Modifier.weight(1f)) {
                ProductCard(
                    product = product,
                    actionText = if (enabled) "بيع الآن" else "عرض",
                    onActionClick = { if (enabled) onProductClick(product) },
                    onCardClick = { if (enabled) onProductClick(product) }
                )
            }
            if (index == 0 && products.size == 1) {
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun RecentSaleCard(
    sale: Sale,
    currencyCode: String,
    onClick: () -> Unit
) {
    val total = sale.items.sumItemsTotal()
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp),
        shape = RoundedCornerShape(AppDimens.Radius.large),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = AppDimens.Elevation.low),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.10f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppDimens.Layout.screenPadding)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing.small)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = sale.invoiceNumber.ifBlank { "فاتورة غير مرقمة" },
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimaryColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = sale.id,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondaryColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                StatusChip(
                    text = sale.saleStatus.name,
                    containerColor = BrandPrimarySoft,
                    contentColor = BrandPrimaryDark,
                    icon = Icons.Filled.Done
                )
            }

            Spacer(modifier = Modifier.height(AppDimens.Spacing.small))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing.small)
            ) {
                SummaryTile(
                    title = "عدد الأصناف",
                    value = sale.items.size.toString(),
                    modifier = Modifier.weight(1f),
                    accent = BrandPrimaryDark
                )
                SummaryTile(
                    title = "الإجمالي",
                    value = formatMoney(total, currencyCode),
                    modifier = Modifier.weight(1f),
                    accent = BrandPrimary
                )
            }

            Spacer(modifier = Modifier.height(AppDimens.Spacing.small))

            AppButton(
                text = "عرض التفاصيل",
                onClick = onClick,
                variant = AppButtonVariant.Outlined,
                size = AppButtonSize.Small,
                fullWidth = true,
                leadingIcon = { Icon(Icons.Filled.ReceiptLong, contentDescription = null) }
            )
        }
    }
}

@Composable
private fun ValidationBanner(errors: List<String>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppDimens.Radius.large),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF7F7)),
        border = BorderStroke(1.dp, ErrorColor.copy(alpha = 0.20f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppDimens.Layout.screenPadding),
            verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing.small)
        ) {
            Text(
                text = "ملاحظات التحقق",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimaryColor
            )
            errors.filter { it.isNotBlank() }.take(4).forEach { error ->
                Text(
                    text = "• $error",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondaryColor
                )
            }
        }
    }
}

@Composable
private fun EmptyCard(
    title: String,
    message: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppDimens.Radius.large),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppDimens.Layout.screenPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing.small)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(AppDimens.Radius.round))
                    .background(BrandPrimarySoft),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = BrandPrimaryDark)
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimaryColor
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondaryColor,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accent: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing.small)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(AppDimens.Radius.medium))
                .background(accent.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = accent)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimaryColor
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondaryColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun StatusChip(
    text: String,
    containerColor: Color,
    contentColor: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Surface(
        color = containerColor,
        contentColor = contentColor,
        shape = RoundedCornerShape(AppDimens.Radius.round)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun DetailChip(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    accent: Color = BrandPrimary,
    trailingLabel: String? = null
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(AppDimens.Radius.medium),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.18f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondaryColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!trailingLabel.isNullOrBlank()) {
                    StatusChip(
                        text = trailingLabel,
                        containerColor = accent.copy(alpha = 0.12f),
                        contentColor = accent,
                        icon = Icons.Filled.Done
                    )
                }
            }
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                color = TextPrimaryColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun MiniStatCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(AppDimens.Radius.medium),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.16f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(AppDimens.Radius.small))
                    .background(accent.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
            }
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondaryColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimaryColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SummaryTile(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    accent: Color = BrandPrimary
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(AppDimens.Radius.medium),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondaryColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                color = accent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun MoneyRow(
    label: String,
    amount: String,
    accent: Color,
    compact: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = if (compact) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium,
            color = TextSecondaryColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = amount,
            style = if (compact) MaterialTheme.typography.bodySmall else MaterialTheme.typography.titleSmall,
            color = accent,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun CounterButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean,
    emphasized: Boolean = false
) {
    val background = when {
        !enabled -> MaterialTheme.colorScheme.surfaceVariant
        emphasized -> BrandPrimarySoft
        else -> MaterialTheme.colorScheme.surface
    }
    val contentColor = when {
        !enabled -> TextSecondaryColor.copy(alpha = 0.6f)
        emphasized -> BrandPrimaryDark
        else -> TextPrimaryColor
    }

    Surface(
        onClick = onClick,
        enabled = enabled,
        color = background,
        contentColor = contentColor,
        shape = RoundedCornerShape(AppDimens.Radius.medium),
        border = BorderStroke(1.dp, BrandPrimary.copy(alpha = 0.18f))
    ) {
        Box(
            modifier = Modifier.size(width = 44.dp, height = 40.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun buildSalesSubtitle(state: SalesUiState): String {
    return when {
        state.isSubmitting -> "جارٍ الحفظ والإرسال"
        state.isRefreshing -> "جارٍ تحديث البيانات"
        state.hasDraftItems -> "${state.itemCount} صنف في المسودة"
        else -> "إدارة البيع والحفظ والملخصات من مكان واحد"
    }
}

private fun validationDetails(errors: Map<String, String>): String? {
    val combined = errors.values.filter { it.isNotBlank() }.joinToString(separator = "")
    return combined.ifBlank { null }
}

private fun saleItemKey(item: SaleItem): String {
    return item.id.ifBlank { item.productId }
}

private fun formatMoney(amount: BigDecimal, currencyCode: String): String {
    val normalized = amount.setScale(2, RoundingMode.HALF_UP)
    return "$currencyCode ${normalized.toPlainString()}"
}

private fun BigDecimal.stripForInput(): String {
    return setScale(2, RoundingMode.HALF_UP).toPlainString()
}

private fun Product.filterableText(): String {
    return buildString {
        append(filterValue(readText("effectiveName", "displayName", "name", "title")))
        append(' ')
        append(filterValue(readText("barcode", "id", "productId")))
        append(' ')
        append(filterValue(readText("categoryName", "category", "groupName")))
        append(' ')
        append(filterValue(readText("description", "notes")))
    }
}

private fun Sale.filterableText(): String {
    return buildString {
        append(filterValue(readText("invoiceNumber")))
        append(' ')
        append(filterValue(readText("id")))
        append(' ')
        append(filterValue(readText("customerId")))
        append(' ')
        append(filterValue(readText("employeeId")))
        append(' ')
        append(filterValue(readText("saleStatus")))
    }
}


private fun List<SaleItem>.sumItemsTotal(): BigDecimal {
    return fold(BigDecimal.ZERO) { acc, item ->
        acc.add(item.calculateSubtotal())
    }.setScale(2, RoundingMode.HALF_UP)
}

private fun readValueFrom(instance: Any, vararg names: String): Any? {
    val clazz = instance.javaClass
    names.forEach { name ->
        val getter = clazz.methods.firstOrNull { method ->
            method.parameterCount == 0 &&
                (method.name == name || method.name == "get${name.replaceFirstChar { it.uppercase() }}")
        }
        if (getter != null) {
            return runCatching { getter.invoke(instance) }.getOrNull()
        }
    }
    return null
}

private fun Any.readText(vararg names: String): String {
    return readValueFrom(this, *names)?.toString().orEmpty()
}

private fun filterValue(value: String?): String {
    return value.orEmpty().trim().lowercase(Locale.getDefault())
}
