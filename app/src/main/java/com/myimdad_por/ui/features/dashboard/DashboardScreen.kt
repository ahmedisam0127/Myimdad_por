@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.myimdad_por.ui.features.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.myimdad_por.ui.components.AppTopBar
import com.myimdad_por.ui.components.ErrorState
import com.myimdad_por.ui.components.ErrorStateStyle
import com.myimdad_por.ui.components.LoadingIndicator
import java.math.BigDecimal
import java.text.DecimalFormat
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.abs
import kotlin.math.max

@Composable
fun DashboardRoute(
    modifier: Modifier = Modifier,
    onSaleClick: (String) -> Unit = {},
    onExpenseClick: (String) -> Unit = {},
    onReportClick: (String) -> Unit = {},
    onTransactionClick: (String) -> Unit = {},
    onNavigateToSales: () -> Unit = {},
    onNavigateToExpenses: () -> Unit = {},
    onNavigateToReports: () -> Unit = {}
) {
    val viewModel: DashboardViewModel = hiltViewModel()
    val state by viewModel.uiState.collectAsState()

    DashboardScreen(
        state = state,
        onEvent = viewModel::onEvent,
        modifier = modifier,
        onSaleClick = onSaleClick,
        onExpenseClick = onExpenseClick,
        onReportClick = onReportClick,
        onTransactionClick = onTransactionClick,
        onNavigateToSales = onNavigateToSales,
        onNavigateToExpenses = onNavigateToExpenses,
        onNavigateToReports = onNavigateToReports
    )
}

@Composable
fun DashboardScreen(
    state: DashboardUiState,
    onEvent: (DashboardUiEvent) -> Unit,
    modifier: Modifier = Modifier,
    onSaleClick: (String) -> Unit = {},
    onExpenseClick: (String) -> Unit = {},
    onReportClick: (String) -> Unit = {},
    onTransactionClick: (String) -> Unit = {},
    onNavigateToSales: () -> Unit = {},
    onNavigateToExpenses: () -> Unit = {},
    onNavigateToReports: () -> Unit = {}
) {
    val accent = MaterialTheme.colorScheme.primary
    val success = MaterialTheme.colorScheme.tertiary
    val danger = MaterialTheme.colorScheme.error
    val warning = MaterialTheme.colorScheme.secondary
    val periodItems = remember { DashboardPeriod.entries }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                            MaterialTheme.colorScheme.surface
                        )
                    )
                ),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                AppTopBar(
                    title = "لوحة التحكم",
                    subtitle = "آخر تحديث: ${state.lastUpdatedAtMillis?.toReadableTime() ?: "الآن"}",
                    showDivider = false,
                    actions = {
                        IconButton(onClick = { onEvent(DashboardUiEvent.Refresh) }) {
                            Icon(
                                imageVector = Icons.Filled.Refresh,
                                contentDescription = "تحديث"
                            )
                        }
                    }
                )
            }

            item {
                HeroCard(
                    state = state,
                    accent = accent,
                    onRetry = { onEvent(DashboardUiEvent.Retry) },
                    onNavigateToReports = onNavigateToReports
                )
            }

            item {
                PeriodSelector(
                    selected = state.selectedPeriod,
                    periods = periodItems,
                    onSelected = { onEvent(DashboardUiEvent.ChangePeriod(it)) }
                )
            }

            item {
                MetricsSection(
                    state = state,
                    accent = accent,
                    success = success,
                    warning = warning,
                    danger = danger,
                    onNavigateToSales = onNavigateToSales,
                    onNavigateToExpenses = onNavigateToExpenses,
                    onNavigateToReports = onNavigateToReports
                )
            }

            item {
                ChartsSection(
                    state = state,
                    accent = accent,
                    success = success,
                    warning = warning
                )
            }

            item {
                ActivitySection(
                    state = state,
                    onSaleClick = onSaleClick,
                    onExpenseClick = onExpenseClick,
                    onReportClick = onReportClick,
                    onTransactionClick = onTransactionClick
                )
            }

            item {
                FooterInfoCard(state = state)
            }
        }

        if (state.isLoading && !state.hasContent) {
            LoadingIndicator(
                modifier = Modifier.fillMaxSize(),
                title = "جارِ تجهيز لوحة التحكم",
                message = "نحمل الإحصاءات والمخططات بشكل أنيق"
            )
        }

        if (state.hasError && !state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                ErrorState(
                    message = state.errorMessage ?: "حدث خطأ غير متوقع",
                    title = "تعذر تحميل البيانات",
                    details = "حاول التحديث أو تغيير الفترة الزمنية.",
                    onRetry = { onEvent(DashboardUiEvent.Retry) },
                    style = ErrorStateStyle.Card
                )
            }
        }
    }
}

@Composable
private fun HeroCard(
    state: DashboardUiState,
    accent: Color,
    onRetry: () -> Unit,
    onNavigateToReports: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(
                            accent.copy(alpha = 0.18f),
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
                .padding(18.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "صافي الأداء",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = state.netAmount.prettyMoney(state.currencyCode),
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "المبيعات: ${state.totalSalesAmount.prettyMoney(state.currencyCode)} • المصروفات: ${state.totalExpensesAmount.prettyMoney(state.currencyCode)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    RingProgress(
                        paid = state.totalPaidAmount,
                        pending = state.totalPendingAmount,
                        accent = accent,
                        label = state.paymentTransactionsCount.toString()
                    )
                }

                if (state.hasError) {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.75f)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "هناك مشكلة في جلب بعض البيانات",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Text(
                                    text = state.errorMessage ?: "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.85f),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Text(
                                text = "إعادة",
                                modifier = Modifier
                                    .background(
                                        color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.08f),
                                        shape = RoundedCornerShape(999.dp)
                                    )
                                    .clickable(onClick = onRetry)
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }

                if (state.canAccessReports) {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = accent.copy(alpha = 0.10f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(onClick = onNavigateToReports)
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "التقارير الذكية",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "اضغط لعرض التحليل المتقدم",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = "فتح",
                                color = accent,
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PeriodSelector(
    selected: DashboardPeriod,
    periods: List<DashboardPeriod>,
    onSelected: (DashboardPeriod) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "الفترة الزمنية",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            periods.forEach { period ->
                FilterChip(
                    selected = period == selected,
                    onClick = { onSelected(period) },
                    label = { Text(text = period.label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        }
    }
}

@Composable
private fun MetricsSection(
    state: DashboardUiState,
    accent: Color,
    success: Color,
    warning: Color,
    danger: Color,
    onNavigateToSales: () -> Unit,
    onNavigateToExpenses: () -> Unit,
    onNavigateToReports: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "نبض التشغيل",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            MetricCard(
                modifier = Modifier.weight(1f),
                title = "المبيعات",
                value = state.salesCount.toString(),
                subValue = state.totalSalesAmount.prettyMoney(state.currencyCode),
                accent = accent,
                onClick = onNavigateToSales
            )
            MetricCard(
                modifier = Modifier.weight(1f),
                title = "المصروفات",
                value = state.expensesCount.toString(),
                subValue = state.totalExpensesAmount.prettyMoney(state.currencyCode),
                accent = danger,
                onClick = onNavigateToExpenses
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            MetricCard(
                modifier = Modifier.weight(1f),
                title = "التقارير",
                value = state.reportsCount.toString(),
                subValue = if (state.canAccessReports) "مفتوحة" else "مقيدة",
                accent = success,
                onClick = onNavigateToReports
            )
            MetricCard(
                modifier = Modifier.weight(1f),
                title = "المدفوعات",
                value = state.paymentTransactionsCount.toString(),
                subValue = "مكتملة: ${state.paidPaymentsCount}",
                accent = warning,
                onClick = onNavigateToReports
            )
        }
    }
}

@Composable
private fun MetricCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    subValue: String,
    accent: Color,
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = modifier.then(
            if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
        ),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(accent.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .background(accent, CircleShape)
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subValue,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ChartsSection(
    state: DashboardUiState,
    accent: Color,
    success: Color,
    warning: Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "التحليل البصري",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ElevatedCard(
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1f),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "صافي الحركة",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = state.netAmount.prettyMoney(state.currencyCode),
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    SparkLineChart(
                        values = state.recentReports.map { it.totalNumericValue.toFloatSafe() },
                        lineColor = accent,
                        fillColor = accent.copy(alpha = 0.18f)
                    )
                }
            }

            ElevatedCard(
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1f),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "توزيع المدفوعات",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    PaymentBars(
                        paid = state.totalPaidAmount,
                        pending = state.totalPendingAmount,
                        paidColor = success,
                        pendingColor = warning
                    )
                }
            }
        }
    }
}

@Composable
private fun ActivitySection(
    state: DashboardUiState,
    onSaleClick: (String) -> Unit,
    onExpenseClick: (String) -> Unit,
    onReportClick: (String) -> Unit,
    onTransactionClick: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "آخر النشاط",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )

        if (
            state.recentSales.isEmpty() &&
            state.recentExpenses.isEmpty() &&
            state.recentReports.isEmpty() &&
            state.recentTransactions.isEmpty()
        ) {
            EmptyActivityCard()
            return
        }

        state.recentSales.take(2).forEach { sale ->
            ActivityCard(
                title = "فاتورة بيع",
                subtitle = sale.invoiceNumber,
                amount = sale.totalAmount.prettyMoney(state.currencyCode),
                trailing = sale.saleStatus.name,
                accent = MaterialTheme.colorScheme.primary,
                onClick = { onSaleClick(sale.id) }
            )
        }

        state.recentExpenses.take(2).forEach { expense ->
            ActivityCard(
                title = "مصروف",
                subtitle = expense.expenseNumber,
                amount = expense.amount.prettyMoney(state.currencyCode),
                trailing = expense.status.name,
                accent = MaterialTheme.colorScheme.error,
                onClick = { onExpenseClick(expense.id) }
            )
        }

        state.recentReports.take(2).forEach { report ->
            ActivityCard(
                title = "تقرير",
                subtitle = report.title,
                amount = report.totalNumericValue.prettyMoney(state.currencyCode),
                trailing = report.type.name,
                accent = MaterialTheme.colorScheme.tertiary,
                onClick = { onReportClick(report.reportId) }
            )
        }

        state.recentTransactions.take(2).forEach { tx ->
            ActivityCard(
                title = "معاملة دفع",
                subtitle = tx.transactionId,
                amount = tx.amount.prettyMoney(state.currencyCode),
                trailing = tx.status.name,
                accent = MaterialTheme.colorScheme.secondary,
                onClick = { onTransactionClick(tx.transactionId) }
            )
        }
    }
}

@Composable
private fun ActivityCard(
    title: String,
    subtitle: String,
    amount: String,
    trailing: String,
    accent: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(accent, CircleShape)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = amount,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = trailing,
                    style = MaterialTheme.typography.labelSmall,
                    color = accent
                )
            }
        }
    }
}

@Composable
private fun EmptyActivityCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "لا توجد حركة حديثة بعد",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "ستظهر المبيعات والمصروفات والتقارير هنا فور توفر البيانات.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun FooterInfoCard(state: DashboardUiState) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "مؤشرات سريعة",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "القراءة الحالية: ${state.selectedPeriod.label} • الوضع: ${if (state.isReadOnlyMode) "عرض فقط" else "تفاعلي"} • التقارير: ${if (state.canAccessReports) "مفعلة" else "محدودة"}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun RingProgress(
    paid: BigDecimal,
    pending: BigDecimal,
    accent: Color,
    label: String
) {
    val paidValue = paid.toFloatSafe()
    val pendingValue = pending.toFloatSafe()
    val total = max(paidValue + pendingValue, 1f)
    val paidFraction = (paidValue / total).coerceIn(0f, 1f)

    val trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
    val labelColor = MaterialTheme.colorScheme.onSurface
    val subLabelColor = MaterialTheme.colorScheme.onSurfaceVariant

    Box(modifier = Modifier.size(128.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = size.minDimension * 0.14f
            val diameter = size.minDimension - stroke
            val topLeft = Offset(stroke / 2f, stroke / 2f)

            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = androidx.compose.ui.geometry.Size(diameter, diameter),
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )

            drawArc(
                color = accent,
                startAngle = -90f,
                sweepAngle = 360f * paidFraction,
                useCenter = false,
                topLeft = topLeft,
                size = androidx.compose.ui.geometry.Size(diameter, diameter),
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                color = labelColor
            )
            Text(
                text = "معاملة",
                style = MaterialTheme.typography.labelSmall,
                color = subLabelColor
            )
        }
    }
}

@Composable
private fun PaymentBars(
    paid: BigDecimal,
    pending: BigDecimal,
    paidColor: Color,
    pendingColor: Color
) {
    val paidValue = paid.toFloatSafe()
    val pendingValue = pending.toFloatSafe()
    val total = max(paidValue + pendingValue, 1f)

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        RatioRow(
            label = "مدفوع",
            ratio = paidValue / total,
            value = paid.prettyMoney(""),
            color = paidColor
        )
        RatioRow(
            label = "معلق",
            ratio = pendingValue / total,
            value = pending.prettyMoney(""),
            color = pendingColor
        )
    }
}

@Composable
private fun RatioRow(
    label: String,
    ratio: Float,
    value: String,
    color: Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(999.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(ratio.coerceIn(0f, 1f))
                    .height(12.dp)
                    .background(color, RoundedCornerShape(999.dp))
            )
        }
    }
}

@Composable
private fun SparkLineChart(
    values: List<Float>,
    lineColor: Color,
    fillColor: Color
) {
    val normalizedValues = remember(values) {
        values.takeLast(12).normalize()
    }

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
    ) {
        if (normalizedValues.isEmpty()) return@Canvas

        val chartWidth = size.width
        val chartHeight = size.height
        val stepX = if (normalizedValues.size == 1) {
            chartWidth
        } else {
            chartWidth / (normalizedValues.size - 1)
        }

        val linePath = Path()
        val fillPath = Path()

        normalizedValues.forEachIndexed { index, value ->
            val x = index * stepX
            val y = chartHeight - (value.coerceIn(0f, 1f) * chartHeight)

            if (index == 0) {
                linePath.moveTo(x, y)
                fillPath.moveTo(x, chartHeight)
                fillPath.lineTo(x, y)
            } else {
                linePath.lineTo(x, y)
                fillPath.lineTo(x, y)
            }
        }

        fillPath.lineTo(chartWidth, chartHeight)
        fillPath.close()

        drawPath(
            path = fillPath,
            color = fillColor
        )

        drawPath(
            path = linePath,
            color = lineColor,
            style = Stroke(width = 6f, cap = StrokeCap.Round)
        )

        normalizedValues.forEachIndexed { index, value ->
            val x = index * stepX
            val y = chartHeight - (value.coerceIn(0f, 1f) * chartHeight)

            drawCircle(
                color = lineColor,
                radius = 6f,
                center = Offset(x, y)
            )
            drawCircle(
                color = Color.White,
                radius = 2.5f,
                center = Offset(x, y)
            )
        }
    }
}

private fun List<Float>.normalize(): List<Float> {
    if (isEmpty()) return emptyList()
    val clean = map { abs(it) }
    val minValue = clean.minOrNull() ?: 0f
    val maxValue = clean.maxOrNull() ?: 1f
    if (maxValue == minValue) return List(size) { 0.65f }
    return clean.map { ((it - minValue) / (maxValue - minValue)).coerceIn(0f, 1f) }
}

private fun BigDecimal.prettyMoney(currencyCode: String): String {
    val formatted = DecimalFormat("#,##0.00").format(this)
    return if (currencyCode.isBlank()) formatted else "$formatted $currencyCode"
}

private fun BigDecimal.toFloatSafe(): Float {
    return try {
        this.toFloat()
    } catch (_: Throwable) {
        0f
    }
}

private fun Long.toReadableTime(): String {
    val instant = java.time.Instant.ofEpochMilli(this)
    val local = LocalDateTime.ofInstant(instant, ZoneId.systemDefault())
    return local.format(DateTimeFormatter.ofPattern("yyyy/MM/dd • HH:mm"))
}

private val DashboardPeriod.label: String
    get() = when (this) {
        DashboardPeriod.Today -> "اليوم"
        DashboardPeriod.Week -> "الأسبوع"
        DashboardPeriod.Month -> "الشهر"
        DashboardPeriod.Year -> "السنة"
        DashboardPeriod.AllTime -> "كل الوقت"
    }