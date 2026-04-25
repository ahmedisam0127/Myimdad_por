package com.myimdad_por.ui.features.dashboard

import android.text.format.DateUtils
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.myimdad_por.domain.model.Expense
import com.myimdad_por.domain.model.PaymentTransaction
import com.myimdad_por.domain.model.Report
import com.myimdad_por.domain.model.Sale
import com.myimdad_por.ui.components.AppTopBar
import com.myimdad_por.ui.components.ErrorState
import com.myimdad_por.ui.components.ErrorStateStyle
import com.myimdad_por.ui.components.LoadingIndicator
import com.myimdad_por.ui.components.SubscriptionWarningBanner
import com.myimdad_por.ui.theme.AppDimens
import com.myimdad_por.ui.theme.AppShapeTokens
import com.myimdad_por.ui.theme.AppTypography
import com.myimdad_por.ui.theme.ErrorColor
import com.myimdad_por.ui.theme.ErrorContainer
import com.myimdad_por.ui.theme.InfoColor
import com.myimdad_por.ui.theme.InfoContainer
import com.myimdad_por.ui.theme.IncomeColor
import com.myimdad_por.ui.theme.IncomeContainer
import com.myimdad_por.ui.theme.TextPrimaryColor
import com.myimdad_por.ui.theme.TextSecondaryColor
import com.myimdad_por.ui.theme.WarningColor
import com.myimdad_por.ui.theme.WarningContainer
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

@Composable
fun DashboardScreen(
    state: DashboardUiState,
    onEvent: (DashboardUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        when {
            state.isLoading && !state.hasContent -> {
                LoadingIndicator(
                    modifier = Modifier.fillMaxSize(),
                    title = "جارٍ تحميل البيانات",
                    message = "يتم تجهيز لوحة التحكم الآن",
                    centered = true,
                )
            }

            state.hasError && !state.hasContent -> {
                ErrorState(
                    modifier = Modifier.fillMaxSize(),
                    title = "تعذر تحميل لوحة التحكم",
                    message = state.errorMessage ?: "حدث خطأ غير متوقع",
                    details = "تحقق من الاتصال ثم أعد المحاولة.",
                    retryText = "إعادة المحاولة",
                    onRetry = { onEvent(DashboardUiEvent.Retry) },
                    style = ErrorStateStyle.FullScreen,
                )
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = AppDimens.Layout.screenPadding),
                    verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing.medium),
                ) {
                    item {
                        AppTopBar(
                            title = "لوحة التحكم",
                            subtitle = state.lastUpdatedAtMillis?.let { formatLastUpdated(it) },
                            actions = {
                                TextButton(onClick = { onEvent(DashboardUiEvent.Refresh) }) {
                                    Text(text = "تحديث")
                                }
                            },
                        )
                    }

                    if (state.isReadOnlyMode || !state.canUsePaidFeatures) {
                        item {
                            SubscriptionWarningBanner(
                                modifier = Modifier.padding(horizontal = AppDimens.Layout.screenPadding),
                                title = "وضع محدود",
                                message = "بعض الميزات غير متاحة في الوضع الحالي.",
                                actionText = "عرض الاشتراك",
                                onActionClick = { onEvent(DashboardUiEvent.NavigateToReports) },
                                onDismissClick = null,
                                visible = true,
                                showIcon = true,
                                backgroundColor = WarningContainer,
                                contentColor = TextPrimaryColor,
                                subtitleColor = TextSecondaryColor,
                            )
                        }
                    }

                    if (state.hasError) {
                        item {
                            ErrorState(
                                modifier = Modifier.padding(horizontal = AppDimens.Layout.screenPadding),
                                title = "هناك مشكلة في التحديث",
                                message = state.errorMessage ?: "حدث خطأ غير متوقع",
                                details = "تم الاحتفاظ بالبيانات الحالية.",
                                retryText = "إعادة المحاولة",
                                dismissText = "إغلاق",
                                onRetry = { onEvent(DashboardUiEvent.Retry) },
                                onDismiss = null,
                                style = ErrorStateStyle.Card,
                            )
                        }
                    }

                    item {
                        PeriodSelector(
                            selected = state.selectedPeriod,
                            onSelected = { onEvent(DashboardUiEvent.ChangePeriod(it)) },
                            modifier = Modifier.padding(horizontal = AppDimens.Layout.screenPadding),
                        )
                    }

                    item {
                        DashboardHeroCard(
                            currencyCode = state.currencyCode,
                            netAmount = state.netAmount,
                            totalSalesAmount = state.totalSalesAmount,
                            totalExpensesAmount = state.totalExpensesAmount,
                            modifier = Modifier.padding(horizontal = AppDimens.Layout.screenPadding),
                        )
                    }

                    item {
                        MetricsGrid(
                            state = state,
                            modifier = Modifier.padding(horizontal = AppDimens.Layout.screenPadding),
                        )
                    }

                    if (state.recentSales.isNotEmpty()) {
                        item {
                            SectionHeader(
                                title = "أحدث المبيعات",
                                subtitle = "آخر العمليات المسجلة",
                                actionText = "عرض الكل",
                                onActionClick = { onEvent(DashboardUiEvent.NavigateToSales) },
                                modifier = Modifier.padding(horizontal = AppDimens.Layout.screenPadding),
                            )
                        }
                        items(state.recentSales.take(5)) { sale ->
                            RecentItemCard(
                                label = "مبيعة",
                                title = sale.safeDashboardTitle(),
                                subtitle = sale.safeDashboardSubtitle(),
                                trailing = sale.safeDashboardAmount(),
                                accentColor = IncomeColor,
                                accentContainer = IncomeContainer,
                                onClick = sale.safeDashboardId()?.let { id ->
                                    { onEvent(DashboardUiEvent.OnSaleClick(id)) }
                                },
                                modifier = Modifier.padding(horizontal = AppDimens.Layout.screenPadding),
                            )
                        }
                    }

                    if (state.recentExpenses.isNotEmpty()) {
                        item {
                            SectionHeader(
                                title = "أحدث المصروفات",
                                subtitle = "المصاريف المضافة مؤخرًا",
                                actionText = "عرض الكل",
                                onActionClick = { onEvent(DashboardUiEvent.NavigateToExpenses) },
                                modifier = Modifier.padding(horizontal = AppDimens.Layout.screenPadding),
                            )
                        }
                        items(state.recentExpenses.take(5)) { expense ->
                            RecentItemCard(
                                label = "مصروف",
                                title = expense.safeDashboardTitle(),
                                subtitle = expense.safeDashboardSubtitle(),
                                trailing = expense.safeDashboardAmount(),
                                accentColor = ErrorColor,
                                accentContainer = ErrorContainer,
                                onClick = expense.safeDashboardId()?.let { id ->
                                    { onEvent(DashboardUiEvent.OnExpenseClick(id)) }
                                },
                                modifier = Modifier.padding(horizontal = AppDimens.Layout.screenPadding),
                            )
                        }
                    }

                    if (state.recentReports.isNotEmpty()) {
                        item {
                            SectionHeader(
                                title = "أحدث التقارير",
                                subtitle = "مؤشرات وتحليلات جاهزة للمراجعة",
                                actionText = "عرض الكل",
                                onActionClick = { onEvent(DashboardUiEvent.NavigateToReports) },
                                modifier = Modifier.padding(horizontal = AppDimens.Layout.screenPadding),
                            )
                        }
                        items(state.recentReports.take(5)) { report ->
                            RecentItemCard(
                                label = "تقرير",
                                title = report.safeDashboardTitle(),
                                subtitle = report.safeDashboardSubtitle(),
                                trailing = report.safeDashboardAmount(),
                                accentColor = InfoColor,
                                accentContainer = InfoContainer,
                                onClick = report.safeDashboardId()?.let { id ->
                                    { onEvent(DashboardUiEvent.OnReportClick(id)) }
                                },
                                modifier = Modifier.padding(horizontal = AppDimens.Layout.screenPadding),
                            )
                        }
                    }

                    if (state.recentTransactions.isNotEmpty()) {
                        item {
                            SectionHeader(
                                title = "آخر الحركات المالية",
                                subtitle = "المدفوعات والمعاملات الحديثة",
                                actionText = "عرض الكل",
                                onActionClick = { onEvent(DashboardUiEvent.NavigateToExpenses) },
                                modifier = Modifier.padding(horizontal = AppDimens.Layout.screenPadding),
                            )
                        }
                        items(state.recentTransactions.take(5)) { transaction ->
                            RecentItemCard(
                                label = "معاملة",
                                title = transaction.safeDashboardTitle(),
                                subtitle = transaction.safeDashboardSubtitle(),
                                trailing = transaction.safeDashboardAmount(),
                                accentColor = WarningColor,
                                accentContainer = WarningContainer,
                                onClick = transaction.safeDashboardId()?.let { id ->
                                    { onEvent(DashboardUiEvent.OnTransactionClick(id)) }
                                },
                                modifier = Modifier.padding(horizontal = AppDimens.Layout.screenPadding),
                            )
                        }
                    }
                }

                if (state.isLoading || state.isRefreshing) {
                    LoadingIndicator(
                        modifier = Modifier.fillMaxSize(),
                        title = if (state.isRefreshing) "جارٍ التحديث" else "جارٍ التحميل",
                        message = if (state.isRefreshing) "يتم تحديث البيانات الحالية" else "يتم جلب البيانات الآن",
                        centered = true,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PeriodSelector(
    selected: DashboardPeriod,
    onSelected: (DashboardPeriod) -> Unit,
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

    Column(modifier = modifier) {
        Text(
            text = "الفترة الزمنية",
            style = AppTypography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.semantics { heading() },
        )
        Spacer(modifier = Modifier.height(AppDimens.Spacing.small))
        Row(horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing.small)) {
            periods.forEach { period ->
                FilterChip(
                    selected = selected == period,
                    onClick = { onSelected(period) },
                    label = { Text(text = period.label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                    shape = AppShapeTokens.chip,
                )
            }
        }
    }
}

@Composable
private fun DashboardHeroCard(
    currencyCode: String,
    netAmount: BigDecimal,
    totalSalesAmount: BigDecimal,
    totalExpensesAmount: BigDecimal,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = AppShapeTokens.card,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = AppDimens.Elevation.medium),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppDimens.Layout.screenPadding),
            verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing.medium),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "صافي الأداء",
                        style = AppTypography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(modifier = Modifier.height(AppDimens.Spacing.extraSmall))
                    Text(
                        text = "ملخص مالي مباشر لآخر البيانات المتاحة",
                        style = AppTypography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                AmountPill(
                    amount = netAmount,
                    currencyCode = currencyCode,
                    accentColor = IncomeColor,
                    accentContainer = IncomeContainer,
                )
            }

            Divider(color = MaterialTheme.colorScheme.outlineVariant)

            Row(horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing.small)) {
                MiniAmountCard(
                    title = "إجمالي المبيعات",
                    amount = totalSalesAmount,
                    currencyCode = currencyCode,
                    accentColor = IncomeColor,
                    accentContainer = IncomeContainer,
                    modifier = Modifier.weight(1f),
                )
                MiniAmountCard(
                    title = "إجمالي المصروفات",
                    amount = totalExpensesAmount,
                    currencyCode = currencyCode,
                    accentColor = ErrorColor,
                    accentContainer = ErrorContainer,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun MetricsGrid(
    state: DashboardUiState,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing.small)) {
        Row(horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing.small)) {
            MetricCard(
                title = "المبيعات",
                value = state.salesCount.toString(),
                subtitle = formatMoney(state.totalSalesAmount, state.currencyCode),
                accentColor = IncomeColor,
                accentContainer = IncomeContainer,
                modifier = Modifier.weight(1f),
            )
            MetricCard(
                title = "المصروفات",
                value = state.expensesCount.toString(),
                subtitle = formatMoney(state.totalExpensesAmount, state.currencyCode),
                accentColor = ErrorColor,
                accentContainer = ErrorContainer,
                modifier = Modifier.weight(1f),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing.small)) {
            MetricCard(
                title = "التقارير",
                value = state.reportsCount.toString(),
                subtitle = if (state.canAccessReports) "مسموح بالوصول" else "الوصول محدود",
                accentColor = InfoColor,
                accentContainer = InfoContainer,
                modifier = Modifier.weight(1f),
            )
            MetricCard(
                title = "المدفوعات",
                value = state.paymentTransactionsCount.toString(),
                subtitle = "مدفوع: ${state.paidPaymentsCount} · معلّق: ${state.pendingPaymentsCount}",
                accentColor = WarningColor,
                accentContainer = WarningContainer,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun MetricCard(
    title: String,
    value: String,
    subtitle: String,
    accentColor: Color,
    accentContainer: Color,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = AppShapeTokens.card,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = AppDimens.Elevation.low),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppDimens.Layout.screenPadding),
            horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing.medium),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MetricAccentDot(
                title = title,
                accentColor = accentColor,
                accentContainer = accentContainer,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = AppTypography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(AppDimens.Spacing.extraSmall))
                Text(
                    text = value,
                    style = AppTypography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(AppDimens.Spacing.extraSmall))
                Text(
                    text = subtitle,
                    style = AppTypography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun MiniAmountCard(
    title: String,
    amount: BigDecimal,
    currencyCode: String,
    accentColor: Color,
    accentContainer: Color,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = AppShapeTokens.card,
        colors = CardDefaults.cardColors(containerColor = accentContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = AppDimens.Elevation.none),
    ) {
        Column(
            modifier = Modifier.padding(AppDimens.Spacing.medium),
            verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing.extraSmall),
        ) {
            Text(
                text = title,
                style = AppTypography.labelMedium,
                color = accentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = formatMoney(amount, currencyCode),
                style = AppTypography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun AmountPill(
    amount: BigDecimal,
    currencyCode: String,
    accentColor: Color,
    accentContainer: Color,
) {
    Surface(
        shape = AppShapeTokens.buttonPill,
        color = accentContainer,
        contentColor = accentColor,
    ) {
        Text(
            text = formatMoney(amount, currencyCode),
            style = AppTypography.titleSmall,
            modifier = Modifier.padding(horizontal = AppDimens.Spacing.medium, vertical = 10.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun MetricAccentDot(
    title: String,
    accentColor: Color,
    accentContainer: Color,
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(accentContainer),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = title.take(1),
            style = AppTypography.titleMedium,
            color = accentColor,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun SectionHeader(
    title: String,
    subtitle: String,
    actionText: String,
    onActionClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = AppTypography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.semantics { heading() },
            )
            Spacer(modifier = Modifier.height(AppDimens.Spacing.extraSmall))
            Text(
                text = subtitle,
                style = AppTypography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        TextButton(onClick = onActionClick) {
            Text(text = actionText)
        }
    }
}

@Composable
private fun RecentItemCard(
    label: String,
    title: String,
    subtitle: String,
    trailing: String?,
    accentColor: Color,
    accentContainer: Color,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .semantics {
                role = Role.Button
                contentDescription = "$label $title"
            },
        shape = AppShapeTokens.card,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = AppDimens.Elevation.low),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppDimens.Layout.screenPadding),
            horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing.medium),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(accentContainer),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label.take(1),
                    style = AppTypography.titleMedium,
                    color = accentColor,
                    textAlign = TextAlign.Center,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = AppTypography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(AppDimens.Spacing.extraSmall))
                Text(
                    text = subtitle,
                    style = AppTypography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (!trailing.isNullOrBlank()) {
                Text(
                    text = trailing,
                    style = AppTypography.labelLarge,
                    color = accentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

private fun DashboardPeriod.toArabicLabel(): String = when (this) {
    DashboardPeriod.Today -> "اليوم"
    DashboardPeriod.Week -> "الأسبوع"
    DashboardPeriod.Month -> "الشهر"
    DashboardPeriod.Year -> "السنة"
    DashboardPeriod.AllTime -> "الكل"
}

private val DashboardPeriod.label: String
    get() = toArabicLabel()

private fun formatMoney(amount: BigDecimal?, currencyCode: String): String {
    val safeAmount = amount ?: BigDecimal.ZERO
    val formatter = NumberFormat.getCurrencyInstance(Locale.getDefault())
    return runCatching {
        formatter.currency = Currency.getInstance(currencyCode)
        formatter.maximumFractionDigits = 2
        formatter.minimumFractionDigits = 0
        formatter.format(safeAmount)
    }.getOrElse {
        "$safeAmount $currencyCode"
    }
}

private fun formatLastUpdated(lastUpdatedAtMillis: Long): String {
    val relative = DateUtils.getRelativeTimeSpanString(
        lastUpdatedAtMillis,
        System.currentTimeMillis(),
        DateUtils.MINUTE_IN_MILLIS,
    )
    return "آخر تحديث منذ $relative"
}

private fun Sale.safeDashboardTitle(): String = toStringSafe("Sale")
private fun Expense.safeDashboardTitle(): String = toStringSafe("Expense")
private fun Report.safeDashboardTitle(): String = toStringSafe("Report")
private fun PaymentTransaction.safeDashboardTitle(): String = toStringSafe("Transaction")

private fun Sale.safeDashboardSubtitle(): String = toStringSafe()
private fun Expense.safeDashboardSubtitle(): String = toStringSafe()
private fun Report.safeDashboardSubtitle(): String = toStringSafe()
private fun PaymentTransaction.safeDashboardSubtitle(): String = toStringSafe()

private fun Sale.safeDashboardAmount(): String? = null
private fun Expense.safeDashboardAmount(): String? = null
private fun Report.safeDashboardAmount(): String? = null
private fun PaymentTransaction.safeDashboardAmount(): String? = null

private fun Sale.safeDashboardId(): String? = null
private fun Expense.safeDashboardId(): String? = null
private fun Report.safeDashboardId(): String? = null
private fun PaymentTransaction.safeDashboardId(): String? = null

private fun Any.toStringSafe(prefix: String? = null): String {
    val raw = toString().trim().ifBlank { this::class.simpleName ?: "عنصر" }
    return prefix?.let { "$it: $raw" } ?: raw
}
