package com.myimdad_por.ui.features.dashboard

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Summarize
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.myimdad_por.core.utils.CurrencyFormatter
import com.myimdad_por.core.utils.DateTimeUtils
import com.myimdad_por.domain.model.Expense
import com.myimdad_por.domain.model.ExpenseStatus
import com.myimdad_por.domain.model.Sale
import com.myimdad_por.domain.model.SaleStatus
import com.myimdad_por.ui.components.AppButton
import com.myimdad_por.ui.components.AppButtonSize
import com.myimdad_por.ui.components.AppButtonVariant
import com.myimdad_por.ui.components.AppTopBar
import com.myimdad_por.ui.components.ErrorState
import com.myimdad_por.ui.components.ErrorStateStyle
import com.myimdad_por.ui.components.LoadingIndicator
import com.myimdad_por.ui.components.LoadingIndicatorVariant
import com.myimdad_por.ui.components.SubscriptionWarningBanner
import com.myimdad_por.ui.theme.AppDimens
import com.myimdad_por.ui.theme.AppShapeTokens
import com.myimdad_por.ui.theme.AppTextStyles
import com.myimdad_por.ui.theme.ExpenseColor
import com.myimdad_por.ui.theme.ExpenseContainer
import com.myimdad_por.ui.theme.IconOnBrandColor
import com.myimdad_por.ui.theme.IncomeColor
import com.myimdad_por.ui.theme.IncomeContainer
import com.myimdad_por.ui.theme.InfoColor
import com.myimdad_por.ui.theme.InfoContainer
import com.myimdad_por.ui.theme.SuccessColor
import com.myimdad_por.ui.theme.SuccessContainer
import com.myimdad_por.ui.theme.SurfaceColor
import com.myimdad_por.ui.theme.TextPrimaryColor
import com.myimdad_por.ui.theme.TextSecondaryColor
import com.myimdad_por.ui.theme.TextTertiaryColor
import com.myimdad_por.ui.theme.WarningColor
import com.myimdad_por.ui.theme.WarningContainer
import java.math.BigDecimal

/**
 * الشاشة الرئيسية للوحة التحكم.
 *
 * تعرض ملخصًا شاملًا لأداء الأعمال متضمنًا مؤشرات الأداء الرئيسية (KPIs)،
 * المبيعات الأخيرة، والمصروفات الأخيرة، مع إمكانية تغيير الفترة الزمنية.
 *
 * @param state حالة واجهة المستخدم الحالية للوحة التحكم.
 * @param onEvent مستمع الأحداث لمعالجة تفاعلات المستخدم.
 * @param modifier مُعدِّل Compose اختياري.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    state: DashboardUiState,
    onEvent: (DashboardUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val periodLabel = remember(state.selectedPeriod) {
        state.selectedPeriod.toDisplayLabel()
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // شريط العنوان العلوي
            AppTopBar(
                title = "لوحة التحكم",
                subtitle = periodLabel,
                actions = {
                    IconButton(
                        onClick = { onEvent(DashboardUiEvent.Refresh) },
                        enabled = !state.isRefreshing && !state.isLoading,
                        modifier = Modifier.semantics {
                            contentDescription = "تحديث البيانات"
                        },
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                },
                centeredTitle = false,
                showDivider = true,
            )

            // المحتوى الرئيسي حسب الحالة
            when {
                state.isLoading && !state.isRefreshing -> {
                    DashboardLoadingState(
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                state.hasError && !state.hasContent -> {
                    DashboardErrorState(
                        message = state.errorMessage ?: "حدث خطأ غير متوقع",
                        onRetry = { onEvent(DashboardUiEvent.Retry) },
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                else -> {
                    DashboardContent(
                        state = state,
                        onEvent = onEvent,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// حالات التحميل والخطأ
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun DashboardLoadingState(modifier: Modifier = Modifier) {
    LoadingIndicator(
        modifier = modifier,
        variant = LoadingIndicatorVariant.Circular,
        title = "جاري تحميل لوحة التحكم",
        message = "يرجى الانتظار لحظة...",
        centered = true,
    )
}

@Composable
private fun DashboardErrorState(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ErrorState(
        message = message,
        title = "تعذر تحميل لوحة التحكم",
        retryText = "إعادة المحاولة",
        onRetry = onRetry,
        style = ErrorStateStyle.FullScreen,
        modifier = modifier,
    )
}

// ──────────────────────────────────────────────────────────────────────────────
// المحتوى الرئيسي للوحة التحكم
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun DashboardContent(
    state: DashboardUiState,
    onEvent: (DashboardUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = AppDimens.Layout.screenPadding,
            end = AppDimens.Layout.screenPadding,
            top = AppDimens.Spacing.medium,
            bottom = AppDimens.Spacing.huge,
        ),
        verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing.large),
    ) {
        // مؤشر التحديث
        if (state.isRefreshing) {
            item(key = "refreshing_indicator") {
                RefreshingIndicator()
            }
        }

        // بانر تنبيه الاشتراك (إن وجد)
        if (state.isReadOnlyMode) {
            item(key = "subscription_warning") {
                SubscriptionWarningBanner(
                    title = "وضع القراءة فقط",
                    message = "أنت في وضع القراءة فقط. بعض الميزات غير متاحة حاليًا.",
                    actionText = "تجديد الاشتراك",
                    onActionClick = { /* TODO: التنقل إلى شاشة الاشتراك */ },
                    visible = true,
                    showIcon = true,
                )
            }
        }

        // محدد الفترة الزمنية
        item(key = "period_selector") {
            PeriodSelector(
                selectedPeriod = state.selectedPeriod,
                onPeriodSelected = { period ->
                    onEvent(DashboardUiEvent.ChangePeriod(period))
                },
            )
        }

        // بطاقات مؤشرات الأداء الرئيسية (KPI)
        item(key = "kpi_grid") {
            KpiCardsGrid(state = state)
        }

        // أزرار الإجراءات السريعة
        item(key = "quick_actions") {
            QuickActionsRow(
                onNavigateToSales = { onEvent(DashboardUiEvent.NavigateToSales) },
                onNavigateToExpenses = { onEvent(DashboardUiEvent.NavigateToExpenses) },
                onNavigateToReports = { onEvent(DashboardUiEvent.NavigateToReports) },
            )
        }

        // قسم المبيعات الأخيرة
        if (state.recentSales.isNotEmpty()) {
            item(key = "recent_sales_header") {
                SectionHeader(
                    title = "المبيعات الأخيرة",
                    actionText = "عرض الكل",
                    onActionClick = { onEvent(DashboardUiEvent.NavigateToSales) },
                )
            }
            items(
                items = state.recentSales,
                key = { sale -> "sale_${sale.id}" },
            ) { sale ->
                RecentSaleItem(
                    sale = sale,
                    onClick = { onEvent(DashboardUiEvent.OnSaleClick(sale.id)) },
                )
            }
        }

        // قسم المصروفات الأخيرة
        if (state.recentExpenses.isNotEmpty()) {
            item(key = "recent_expenses_header") {
                SectionHeader(
                    title = "المصروفات الأخيرة",
                    actionText = "عرض الكل",
                    onActionClick = { onEvent(DashboardUiEvent.NavigateToExpenses) },
                )
            }
            items(
                items = state.recentExpenses,
                key = { expense -> "expense_${expense.id}" },
            ) { expense ->
                RecentExpenseItem(
                    expense = expense,
                    onClick = { onEvent(DashboardUiEvent.OnExpenseClick(expense.id)) },
                )
            }
        }

        // حالة عدم وجود محتوى
        if (!state.hasContent && !state.isRefreshing) {
            item(key = "empty_state") {
                DashboardEmptyState()
            }
        }

        // تذييل آخر تحديث
        if (state.lastUpdatedAtMillis != null) {
            item(key = "last_updated") {
                LastUpdatedFooter(
                    lastUpdatedAtMillis = state.lastUpdatedAtMillis,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// مؤشر التحديث
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun RefreshingIndicator() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = AppDimens.Spacing.small),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        androidx.compose.material3.CircularProgressIndicator(
            modifier = Modifier.size(18.dp),
            strokeWidth = 2.dp,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.width(AppDimens.Spacing.small))
        Text(
            text = "جاري التحديث...",
            style = AppTextStyles.ArabicBody,
            color = TextSecondaryColor,
        )
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// محدد الفترة الزمنية
// ──────────────────────────────────────────────────────────────────────────────
@Composable
private fun PeriodSelector(
    selectedPeriod: DashboardPeriod,
    onPeriodSelected: (DashboardPeriod) -> Unit,
    modifier: Modifier = Modifier,
) {
    val periods = remember {
        listOf(
            DashboardPeriod.Today,
            DashboardPeriod.Week,
            DashboardPeriod.Month,
            DashboardPeriod.Year,
            DashboardPeriod.AllTime,
        )
    }

    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing.small),
        contentPadding = PaddingValues(horizontal = AppDimens.Spacing.extraSmall),
    ) {
        items(
            items = periods,
            key = { period -> period.name },
        ) { period ->
            val isSelected = period == selectedPeriod
            val containerColor by animateColorAsState(
                targetValue = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
                label = "period_chip_bg",
            )
            val contentColor by animateColorAsState(
                targetValue = if (isSelected) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                label = "period_chip_content",
            )

                        FilterChip(
                selected = isSelected,
                enabled = true, // تم نقلها إلى هنا (المكان الصحيح)
                onClick = { onPeriodSelected(period) },
                label = {
                    Text(
                        text = period.toDisplayLabel(),
                        style = AppTextStyles.ArabicBody,
                        color = contentColor,
                        maxLines = 1
                        // حذفنا enabled من هنا لأن Text لا تدعمه
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = containerColor,
                    labelColor = contentColor,
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true, // أضفنا enabled هنا أيضاً لأنها مطلوبة في Border الإضافي
                    selected = isSelected,
                    borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    selectedBorderColor = MaterialTheme.colorScheme.primary,
                    borderWidth = 1.dp,
                    selectedBorderWidth = 2.dp
                ),
                shape = AppShapeTokens.chip
            )
        }
    }
}



// ──────────────────────────────────────────────────────────────────────────────
// بطاقات مؤشرات الأداء الرئيسية (KPI)
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun KpiCardsGrid(
    state: DashboardUiState,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing.medium),
    ) {
        // الصف الأول: المبيعات والمصروفات
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing.medium),
        ) {
            KpiCard(
                title = "المبيعات",
                value = CurrencyFormatter.formatSDG(state.totalSalesAmount),
                subtitle = "${state.salesCount} عملية",
                icon = Icons.Filled.TrendingUp,
                containerColor = IncomeContainer,
                contentColor = IncomeColor,
                modifier = Modifier.weight(1f),
            )
            KpiCard(
                title = "المصروفات",
                value = CurrencyFormatter.formatSDG(state.totalExpensesAmount),
                subtitle = "${state.expensesCount} مصروف",
                icon = Icons.Filled.TrendingDown,
                containerColor = ExpenseContainer,
                contentColor = ExpenseColor,
                modifier = Modifier.weight(1f),
            )
        }

        // الصف الثاني: الصافي والمدفوعات
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing.medium),
        ) {
            KpiCard(
                title = "الصافي",
                value = CurrencyFormatter.formatSDG(state.netAmount),
                subtitle = if (state.netAmount >= BigDecimal.ZERO) "ربح" else "خسارة",
                icon = Icons.Filled.Summarize,
                containerColor = InfoContainer,
                contentColor = InfoColor,
                modifier = Modifier.weight(1f),
            )
            KpiCard(
                title = "المدفوعات المعلقة",
                value = CurrencyFormatter.formatSDG(state.totalPendingAmount),
                subtitle = "${state.pendingPaymentsCount} معلقة",
                icon = Icons.Filled.Payments,
                containerColor = WarningContainer,
                contentColor = WarningColor,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun KpiCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.animateContentSize(),
        shape = AppShapeTokens.card,
        colors = CardDefaults.cardColors(containerColor = SurfaceColor),
        elevation = CardDefaults.cardElevation(defaultElevation = AppDimens.Elevation.medium),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppDimens.Spacing.normal),
            verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing.small),
        ) {
            // أيقونة مع خلفية ملونة
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(containerColor),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(22.dp),
                )
            }

            Spacer(modifier = Modifier.height(AppDimens.Spacing.extraSmall))

            // العنوان
            Text(
                text = title,
                style = AppTextStyles.ArabicBody,
                color = TextSecondaryColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            // القيمة
            Text(
                text = value,
                style = AppTextStyles.NumberDisplay,
                color = TextPrimaryColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.Bold,
            )

            // العنوان الفرعي
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// أزرار الإجراءات السريعة
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun QuickActionsRow(
    onNavigateToSales: () -> Unit,
    onNavigateToExpenses: () -> Unit,
    onNavigateToReports: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing.small),
    ) {
        QuickActionButton(
            text = "المبيعات",
            icon = Icons.Filled.ShoppingCart,
            onClick = onNavigateToSales,
            modifier = Modifier.weight(1f),
        )
        QuickActionButton(
            text = "المصروفات",
            icon = Icons.Filled.ReceiptLong,
            onClick = onNavigateToExpenses,
            modifier = Modifier.weight(1f),
        )
        QuickActionButton(
            text = "التقارير",
            icon = Icons.Filled.Summarize,
            onClick = onNavigateToReports,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun QuickActionButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .clip(AppShapeTokens.card)
            .clickable(onClick = onClick),
        shape = AppShapeTokens.card,
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = AppDimens.Elevation.low,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppDimens.Spacing.normal),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing.small),
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
            }
            Text(
                text = text,
                style = AppTextStyles.ArabicBody,
                color = TextPrimaryColor,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// رأس القسم
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun SectionHeader(
    title: String,
    actionText: String,
    onActionClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .semantics { heading() },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimaryColor,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = actionText,
            style = AppTextStyles.ArabicBody,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
            modifier = Modifier
                .clip(AppShapeTokens.chip)
                .clickable(onClick = onActionClick)
                .padding(
                    horizontal = AppDimens.Spacing.small,
                    vertical = AppDimens.Spacing.extraSmall,
                ),
        )
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// عنصر مبيعة حديثة
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun RecentSaleItem(
    sale: Sale,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = AppShapeTokens.card,
        colors = CardDefaults.cardColors(containerColor = SurfaceColor),
        elevation = CardDefaults.cardElevation(defaultElevation = AppDimens.Elevation.low),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppDimens.Spacing.normal),
            horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing.medium),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // أيقونة الحالة
            val (statusIcon, statusColor) = sale.saleStatus.toStatusUi()
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(statusColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = statusIcon,
                    contentDescription = null,
                    tint = statusColor,
                    modifier = Modifier.size(20.dp),
                )
            }

            // معلومات المبيعة
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing.extraSmall),
            ) {
                Text(
                    text = sale.invoiceNumber,
                    style = AppTextStyles.ArabicBody,
                    color = TextPrimaryColor,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = DateTimeUtils.formatForDisplay(sale.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextTertiaryColor,
                    maxLines = 1,
                )
            }

            // المبلغ
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = CurrencyFormatter.formatSDG(sale.totalAmount),
                    style = AppTextStyles.NumberBody,
                    color = TextPrimaryColor,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                )
                Text(
                    text = sale.saleStatus.toDisplayLabel(),
                    style = MaterialTheme.typography.labelSmall,
                    color = statusColor,
                    maxLines = 1,
                )
            }

            // سهم التنقل
            Icon(
                imageVector = Icons.Filled.ChevronLeft,
                contentDescription = null,
                tint = TextTertiaryColor,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// عنصر مصروف حديث
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun RecentExpenseItem(
    expense: Expense,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = AppShapeTokens.card,
        colors = CardDefaults.cardColors(containerColor = SurfaceColor),
        elevation = CardDefaults.cardElevation(defaultElevation = AppDimens.Elevation.low),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppDimens.Spacing.normal),
            horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing.medium),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // أيقونة الحالة
            val (statusIcon, statusColor) = expense.status.toStatusUi()
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(statusColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = statusIcon,
                    contentDescription = null,
                    tint = statusColor,
                    modifier = Modifier.size(20.dp),
                )
            }

            // معلومات المصروف
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing.extraSmall),
            ) {
                Text(
                    text = expense.title,
                    style = AppTextStyles.ArabicBody,
                    color = TextPrimaryColor,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing.small),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = expense.category.toDisplayLabel(),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextTertiaryColor,
                        maxLines = 1,
                    )
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextTertiaryColor,
                    )
                    Text(
                        text = DateTimeUtils.formatForDisplay(expense.expenseDate),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextTertiaryColor,
                        maxLines = 1,
                    )
                }
            }

            // المبلغ
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = CurrencyFormatter.formatSDG(expense.amount),
                    style = AppTextStyles.NumberBody,
                    color = ExpenseColor,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                )
                Text(
                    text = expense.status.toDisplayLabel(),
                    style = MaterialTheme.typography.labelSmall,
                    color = statusColor,
                    maxLines = 1,
                )
            }

            // سهم التنقل
            Icon(
                imageVector = Icons.Filled.ChevronLeft,
                contentDescription = null,
                tint = TextTertiaryColor,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// حالة فارغة
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun DashboardEmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = AppDimens.Spacing.huge),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing.medium),
    ) {
        // أيقونة توضيحية
        Box(
            modifier = Modifier
                .size(AppDimens.Layout.emptyStateIconSize)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Summarize,
                contentDescription = null,
                tint = TextTertiaryColor,
                modifier = Modifier.size(36.dp),
            )
        }

        Text(
            text = "لا توجد بيانات متاحة",
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimaryColor,
            textAlign = TextAlign.Center,
        )

        Text(
            text = "ستظهر هنا بيانات المبيعات والمصروفات والتقارير\nعند بدء استخدام التطبيق",
            style = AppTextStyles.ArabicBody,
            color = TextSecondaryColor,
            textAlign = TextAlign.Center,
        )
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// تذييل آخر تحديث
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun LastUpdatedFooter(
    lastUpdatedAtMillis: Long,
    modifier: Modifier = Modifier,
) {
    val timeAgo = remember(lastUpdatedAtMillis) {
        val now = System.currentTimeMillis()
        val diffMinutes = (now - lastUpdatedAtMillis) / 60_000
        when {
            diffMinutes < 1 -> "الآن"
            diffMinutes < 60 -> "منذ $diffMinutes دقيقة"
            else -> {
                val diffHours = diffMinutes / 60
                if (diffHours < 24) "منذ $diffHours ساعة"
                else "منذ ${diffHours / 24} يوم"
            }
        }
    }

    Text(
        text = "آخر تحديث: $timeAgo",
        style = MaterialTheme.typography.labelSmall,
        color = TextTertiaryColor,
        textAlign = TextAlign.Center,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = AppDimens.Spacing.small),
    )
}

// ──────────────────────────────────────────────────────────────────────────────
// دوال مساعدة للإظهار
// ──────────────────────────────────────────────────────────────────────────────

private fun DashboardPeriod.toDisplayLabel(): String = when (this) {
    DashboardPeriod.Today -> "اليوم"
    DashboardPeriod.Week -> "هذا الأسبوع"
    DashboardPeriod.Month -> "هذا الشهر"
    DashboardPeriod.Year -> "هذه السنة"
    DashboardPeriod.AllTime -> "كل الوقت"
}

private fun SaleStatus.toStatusUi(): Pair<ImageVector, Color> = when (this) {
    SaleStatus.COMPLETED -> Icons.Filled.ShoppingCart to SuccessColor
    SaleStatus.CANCELLED -> Icons.Filled.TrendingDown to ExpenseColor
    SaleStatus.REFUNDED -> Icons.Filled.Refresh to WarningColor
}

private fun SaleStatus.toDisplayLabel(): String = when (this) {
    SaleStatus.COMPLETED -> "مكتملة"
    SaleStatus.CANCELLED -> "ملغية"
    SaleStatus.REFUNDED -> "مسترجعة"
}

private fun ExpenseStatus.toStatusUi(): Pair<ImageVector, Color> = when (this) {
    ExpenseStatus.PENDING -> Icons.Filled.ReceiptLong to WarningColor
    ExpenseStatus.APPROVED -> Icons.Filled.TrendingUp to InfoColor
    ExpenseStatus.PAID -> Icons.Filled.Payments to SuccessColor
    ExpenseStatus.CANCELLED -> Icons.Filled.TrendingDown to ExpenseColor
}

private fun ExpenseStatus.toDisplayLabel(): String = when (this) {
    ExpenseStatus.PENDING -> "قيد الانتظار"
    ExpenseStatus.APPROVED -> "معتمد"
    ExpenseStatus.PAID -> "مسدد"
    ExpenseStatus.CANCELLED -> "ملغي"
}

private fun com.myimdad_por.domain.model.ExpenseCategory.toDisplayLabel(): String = when (this) {
    com.myimdad_por.domain.model.ExpenseCategory.RENT -> "إيجار"
    com.myimdad_por.domain.model.ExpenseCategory.SALARY -> "رواتب"
    com.myimdad_por.domain.model.ExpenseCategory.UTILITIES -> "خدمات"
    com.myimdad_por.domain.model.ExpenseCategory.TRANSPORT -> "نقل"
    com.myimdad_por.domain.model.ExpenseCategory.MAINTENANCE -> "صيانة"
    com.myimdad_por.domain.model.ExpenseCategory.PURCHASE -> "مشتريات"
    com.myimdad_por.domain.model.ExpenseCategory.TAX -> "ضرائب"
    com.myimdad_por.domain.model.ExpenseCategory.MARKETING -> "تسويق"
    com.myimdad_por.domain.model.ExpenseCategory.OFFICE -> "مصاريف مكتبية"
    com.myimdad_por.domain.model.ExpenseCategory.OTHER -> "أخرى"
}