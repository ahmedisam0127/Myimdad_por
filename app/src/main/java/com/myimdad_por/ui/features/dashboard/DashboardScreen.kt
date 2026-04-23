package com.myimdad_por.ui.features.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.myimdad_por.core.base.UiState
import com.myimdad_por.domain.model.Expense
import com.myimdad_por.domain.model.PaymentTransaction
import com.myimdad_por.domain.model.Report
import com.myimdad_por.domain.model.Sale
import com.myimdad_por.ui.components.AppTopBar
import com.myimdad_por.ui.components.ErrorState
import com.myimdad_por.ui.components.ErrorStateStyle
import com.myimdad_por.ui.components.LoadingIndicator
import com.myimdad_por.ui.theme.AppDimens
import com.myimdad_por.ui.theme.BrandPrimary
import com.myimdad_por.ui.theme.ErrorColor
import com.myimdad_por.ui.theme.ExpenseColor
import com.myimdad_por.ui.theme.IncomeColor
import com.myimdad_por.ui.theme.InfoColor
import com.myimdad_por.ui.theme.WarningColor
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DashboardRoute(
    onSaleClick: (String) -> Unit,
    onExpenseClick: (String) -> Unit,
    onReportClick: (String) -> Unit,
    onTransactionClick: (String) -> Unit,
    onNavigateToSales: () -> Unit,
    onNavigateToExpenses: () -> Unit,
    onNavigateToReports: () -> Unit,
    onNavigateToAccounting: (() -> Unit)? = null,
    onNavigateToCustomers: (() -> Unit)? = null,
    onNavigateToInventory: (() -> Unit)? = null,
    onNavigateToPayments: (() -> Unit)? = null,
    onNavigateToPurchases: (() -> Unit)? = null,
    onNavigateToReturns: (() -> Unit)? = null,
    onNavigateToSecurity: (() -> Unit)? = null,
    onNavigateToSettings: (() -> Unit)? = null,
    onNavigateToSubscription: (() -> Unit)? = null,
    onNavigateToSuppliers: (() -> Unit)? = null,
    onLogoutClick: (() -> Unit)? = null,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    DashboardScreen(
        uiState = uiState,
        onRefresh = { viewModel.onEvent(DashboardUiEvent.Refresh) },
        onPeriodChange = { period -> viewModel.onEvent(DashboardUiEvent.ChangePeriod(period)) },
        onSaleClick = onSaleClick,
        onExpenseClick = onExpenseClick,
        onReportClick = onReportClick,
        onTransactionClick = onTransactionClick,
        onNavigateToSales = onNavigateToSales,
        onNavigateToExpenses = onNavigateToExpenses,
        onNavigateToReports = onNavigateToReports,
        onNavigateToAccounting = onNavigateToAccounting,
        onNavigateToCustomers = onNavigateToCustomers,
        onNavigateToInventory = onNavigateToInventory,
        onNavigateToPayments = onNavigateToPayments,
        onNavigateToPurchases = onNavigateToPurchases,
        onNavigateToReturns = onNavigateToReturns,
        onNavigateToSecurity = onNavigateToSecurity,
        onNavigateToSettings = onNavigateToSettings,
        onNavigateToSubscription = onNavigateToSubscription,
        onNavigateToSuppliers = onNavigateToSuppliers,
        onLogoutClick = onLogoutClick
    )
}

@Composable
fun DashboardScreen(
    uiState: UiState<DashboardUiState>,
    onRefresh: () -> Unit,
    onPeriodChange: (DashboardPeriod) -> Unit,
    onSaleClick: (String) -> Unit,
    onExpenseClick: (String) -> Unit,
    onReportClick: (String) -> Unit,
    onTransactionClick: (String) -> Unit,
    onNavigateToSales: () -> Unit,
    onNavigateToExpenses: () -> Unit,
    onNavigateToReports: () -> Unit,
    onNavigateToAccounting: (() -> Unit)? = null,
    onNavigateToCustomers: (() -> Unit)? = null,
    onNavigateToInventory: (() -> Unit)? = null,
    onNavigateToPayments: (() -> Unit)? = null,
    onNavigateToPurchases: (() -> Unit)? = null,
    onNavigateToReturns: (() -> Unit)? = null,
    onNavigateToSecurity: (() -> Unit)? = null,
    onNavigateToSettings: (() -> Unit)? = null,
    onNavigateToSubscription: (() -> Unit)? = null,
    onNavigateToSuppliers: (() -> Unit)? = null,
    onLogoutClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val state = when (uiState) {
        is UiState.Success -> uiState.data
        is UiState.Error -> DashboardUiState(
            isLoading = false,
            errorMessage = uiState.message
        )
        UiState.Loading -> DashboardUiState(isLoading = true)
        UiState.Empty -> DashboardUiState()
        UiState.Idle -> DashboardUiState()
    }

    var drawerOpen by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        val contentScale = if (drawerOpen) 0.94f else 1f
        val contentShift = if (drawerOpen) -36f else 0f

        DashboardContent(
            state = state,
            onOpenDrawer = { drawerOpen = true },
            onRefresh = onRefresh,
            onPeriodChange = onPeriodChange,
            onSaleClick = onSaleClick,
            onExpenseClick = onExpenseClick,
            onReportClick = onReportClick,
            onTransactionClick = onTransactionClick,
            onNavigateToSales = onNavigateToSales,
            onNavigateToExpenses = onNavigateToExpenses,
            onNavigateToReports = onNavigateToReports,
            listState = listState,
            contentScale = contentScale,
            contentShift = contentShift
        )

        if (drawerOpen) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.32f))
                    .clickable { drawerOpen = false }
            )

            DashboardDrawerSheet(
                modifier = Modifier.align(Alignment.CenterEnd),
                onClose = { drawerOpen = false },
                onNavigateToAccounting = onNavigateToAccounting,
                onNavigateToCustomers = onNavigateToCustomers,
                onNavigateToExpenses = onNavigateToExpenses,
                onNavigateToInventory = onNavigateToInventory,
                onNavigateToPayments = onNavigateToPayments,
                onNavigateToPurchases = onNavigateToPurchases,
                onNavigateToReports = onNavigateToReports,
                onNavigateToReturns = onNavigateToReturns,
                onNavigateToSales = onNavigateToSales,
                onNavigateToSecurity = onNavigateToSecurity,
                onNavigateToSettings = onNavigateToSettings,
                onNavigateToSubscription = onNavigateToSubscription,
                onNavigateToSuppliers = onNavigateToSuppliers,
                onLogoutClick = onLogoutClick
            )
        }
    }
}

@Composable
private fun DashboardContent(
    state: DashboardUiState,
    onOpenDrawer: () -> Unit,
    onRefresh: () -> Unit,
    onPeriodChange: (DashboardPeriod) -> Unit,
    onSaleClick: (String) -> Unit,
    onExpenseClick: (String) -> Unit,
    onReportClick: (String) -> Unit,
    onTransactionClick: (String) -> Unit,
    onNavigateToSales: () -> Unit,
    onNavigateToExpenses: () -> Unit,
    onNavigateToReports: () -> Unit,
    listState: LazyListState,
    contentScale: Float,
    contentShift: Float
) {
    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                scaleX = contentScale
                scaleY = contentScale
                translationX = contentShift
            }
    ) {
        item {
            AppTopBar(
                title = "لوحة التحكم",
                subtitle = dashboardSubtitle(state),
                actions = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(
                            imageVector = Icons.Filled.Menu,
                            contentDescription = "القائمة"
                        )
                    }
                    IconButton(onClick = onRefresh) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = "تحديث"
                        )
                    }
                }
            )
        }

        if (state.isReadOnlyMode) {
            item {
                ModeBanner(
                    title = "وضع القراءة فقط",
                    message = "تم تقييد بعض العمليات الحساسة لحماية الجلسة."
                )
            }
        }

        if (!state.errorMessage.isNullOrBlank()) {
            item {
                ErrorState(
                    modifier = Modifier.padding(
                        horizontal = AppDimens.Layout.screenPadding,
                        vertical = AppDimens.Spacing.medium
                    ),
                    message = state.errorMessage,
                    title = "تنبيه",
                    details = "يمكنك الاستمرار بالبيانات المعروضة أو إعادة المحاولة.",
                    retryText = "إعادة المحاولة",
                    onRetry = onRefresh,
                    style = ErrorStateStyle.Inline
                )
            }
        }

        item {
            Column(
                modifier = Modifier.padding(AppDimens.Layout.screenPadding),
                verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing.large)
            ) {
                PeriodSelector(
                    selectedPeriod = state.selectedPeriod,
                    onPeriodChange = onPeriodChange
                )

                SummaryHeaderCard(state = state)

                SectionHeader(title = "المؤشرات السريعة")
                MetricGrid(state = state)

                SectionHeader(title = "الحركة الأخيرة")
            }
        }

        item {
            RecentSalesSection(
                items = state.recentSales,
                onItemClick = onSaleClick
            )
        }

        item {
            RecentExpensesSection(
                items = state.recentExpenses,
                onItemClick = onExpenseClick
            )
        }

        item {
            RecentReportsSection(
                items = state.recentReports,
                onItemClick = onReportClick
            )
        }

        item {
            RecentTransactionsSection(
                items = state.recentTransactions,
                onItemClick = onTransactionClick
            )
        }

        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing.small),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(AppDimens.Layout.screenPadding)
            ) {
                OutlinedButton(
                    onClick = onNavigateToSales,
                    modifier = Modifier.weight(1f)
                ) { Text("المبيعات") }

                OutlinedButton(
                    onClick = onNavigateToExpenses,
                    modifier = Modifier.weight(1f)
                ) { Text("المصاريف") }

                OutlinedButton(
                    onClick = onNavigateToReports,
                    modifier = Modifier.weight(1f)
                ) { Text("التقارير") }
            }
        }

        item {
            if (state.lastUpdatedAtMillis != null) {
                Text(
                    text = "آخر تحديث: ${formatTime(state.lastUpdatedAtMillis)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(
                        horizontal = AppDimens.Layout.screenPadding,
                        vertical = AppDimens.Spacing.small
                    )
                )
            }

            Spacer(modifier = Modifier.height(AppDimens.Spacing.large))
        }
    }
}

@Composable
private fun DashboardDrawerSheet(
    modifier: Modifier = Modifier,
    onClose: () -> Unit,
    onNavigateToAccounting: (() -> Unit)?,
    onNavigateToCustomers: (() -> Unit)?,
    onNavigateToExpenses: (() -> Unit)?,
    onNavigateToInventory: (() -> Unit)?,
    onNavigateToPayments: (() -> Unit)?,
    onNavigateToPurchases: (() -> Unit)?,
    onNavigateToReports: (() -> Unit)?,
    onNavigateToReturns: (() -> Unit)?,
    onNavigateToSales: (() -> Unit)?,
    onNavigateToSecurity: (() -> Unit)?,
    onNavigateToSettings: (() -> Unit)?,
    onNavigateToSubscription: (() -> Unit)?,
    onNavigateToSuppliers: (() -> Unit)?,
    onLogoutClick: (() -> Unit)?
) {
    val drawerItems = listOf(
        DrawerMenuItem("المحاسبة", Icons.Filled.AccountBalance, onNavigateToAccounting),
        DrawerMenuItem("العملاء", Icons.Filled.People, onNavigateToCustomers),
        DrawerMenuItem("المصاريف", Icons.Filled.ReceiptLong, onNavigateToExpenses),
        DrawerMenuItem("المخزون", Icons.Filled.Inventory2, onNavigateToInventory),
        DrawerMenuItem("المدفوعات", Icons.Filled.Payments, onNavigateToPayments),
        DrawerMenuItem("المشتريات", Icons.Filled.Storefront, onNavigateToPurchases),
        DrawerMenuItem("التقارير", Icons.Filled.Description, onNavigateToReports),
        DrawerMenuItem("المرتجعات", Icons.Filled.Replay, onNavigateToReturns),
        DrawerMenuItem("المبيعات", Icons.Filled.ShoppingCart, onNavigateToSales),
        DrawerMenuItem("الأمان", Icons.Filled.Security, onNavigateToSecurity),
        DrawerMenuItem("الإعدادات", Icons.Filled.Settings, onNavigateToSettings),
        DrawerMenuItem("الاشتراكات", Icons.Filled.Payments, onNavigateToSubscription),
        DrawerMenuItem("الموردين", Icons.Filled.Storefront, onNavigateToSuppliers)
    )

    Surface(
        modifier = modifier
            .fillMaxWidth(0.86f)
            .fillMaxHeight()
            .statusBarsPadding()
            .navigationBarsPadding(),
        tonalElevation = AppDimens.Elevation.high,
        shape = RoundedCornerShape(
            topStart = AppDimens.Radius.large,
            bottomStart = AppDimens.Radius.large
        ),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(AppDimens.Layout.screenPadding),
            verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing.medium)
        ) {
            DrawerHeader(onClose = onClose)

            Divider(color = MaterialTheme.colorScheme.outlineVariant)

            DrawerSectionTitle(text = "الانتقال السريع")

            drawerItems.forEach { item ->
                DashboardDrawerRow(
                    title = item.title,
                    icon = item.icon,
                    onClick = item.onClick,
                    onClose = onClose
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Divider(color = MaterialTheme.colorScheme.outlineVariant)

            DashboardDrawerRow(
                title = "تسجيل الخروج",
                icon = Icons.Filled.PowerSettingsNew,
                accentColor = ErrorColor,
                destructive = true,
                onClick = onLogoutClick,
                onClose = onClose
            )
        }
    }
}

@Composable
private fun DrawerHeader(onClose: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing.small)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(AppDimens.Radius.large)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Menu,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.Filled.ArrowBack,
                    contentDescription = "إغلاق القائمة"
                )
            }
        }

        Text(
            text = "القائمة الجانبية",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Text(
            text = "تنقل عربي أنيق وسريع بين أقسام التطبيق",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun DrawerSectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.SemiBold
    )
}

@Composable
private fun DashboardDrawerRow(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: (() -> Unit)?,
    onClose: () -> Unit,
    accentColor: androidx.compose.ui.graphics.Color = BrandPrimary,
    destructive: Boolean = false
) {
    val rowColor = if (destructive) ErrorColor else accentColor
    val rowModifier = Modifier
        .fillMaxWidth()
        .clickable(enabled = onClick != null) {
            onClick?.invoke()
            onClose()
        }

    Card(
        modifier = rowModifier,
        shape = RoundedCornerShape(AppDimens.Radius.medium),
        colors = CardDefaults.cardColors(
            containerColor = if (destructive) {
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Row(
            modifier = Modifier.padding(AppDimens.Layout.screenPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing.medium)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = rowColor.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(AppDimens.Radius.medium)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = rowColor
                )
            }

            Text(
                text = title,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ModeBanner(title: String, message: String) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppDimens.Layout.screenPadding),
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shape = RoundedCornerShape(AppDimens.Radius.medium)
    ) {
        Column(
            modifier = Modifier.padding(AppDimens.Layout.screenPadding),
            verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing.extraSmall)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(horizontal = AppDimens.Layout.screenPadding)
    )
}

@Composable
private fun PeriodSelector(
    selectedPeriod: DashboardPeriod,
    onPeriodChange: (DashboardPeriod) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing.small),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "الفترة الزمنية",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing.small),
            modifier = Modifier.fillMaxWidth()
        ) {
            DashboardPeriod.entries.forEach { period ->
                FilterChip(
                    selected = selectedPeriod == period,
                    onClick = { onPeriodChange(period) },
                    label = { Text(period.arabicLabel()) },
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
private fun SummaryHeaderCard(state: DashboardUiState) {
    ElevatedCard(
        shape = RoundedCornerShape(AppDimens.Radius.large),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = AppDimens.Elevation.low)
    ) {
        Column(
            modifier = Modifier.padding(AppDimens.Layout.screenPadding),
            verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing.medium)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing.medium)
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(AppDimens.Radius.large)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.AccountBalance,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "ملخص الأداء",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "قراءة سريعة ومرتبة للمؤشرات الأساسية",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Divider(color = MaterialTheme.colorScheme.outlineVariant)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing.medium)
            ) {
                SummaryStatCard(
                    modifier = Modifier.weight(1f),
                    title = "المبيعات",
                    value = state.salesCount.toString(),
                    icon = Icons.Filled.ShoppingCart,
                    accentColor = IncomeColor
                )
                SummaryStatCard(
                    modifier = Modifier.weight(1f),
                    title = "المصروفات",
                    value = state.expensesCount.toString(),
                    icon = Icons.Filled.ReceiptLong,
                    accentColor = ExpenseColor
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing.medium)
            ) {
                SummaryStatCard(
                    modifier = Modifier.weight(1f),
                    title = "المدفوعات",
                    value = state.paymentTransactionsCount.toString(),
                    icon = Icons.Filled.Payments,
                    accentColor = InfoColor
                )
                SummaryStatCard(
                    modifier = Modifier.weight(1f),
                    title = "التقارير",
                    value = state.reportsCount.toString(),
                    icon = Icons.Filled.Description,
                    accentColor = WarningColor
                )
            }
        }
    }
}

@Composable
private fun SummaryStatCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(AppDimens.Radius.large),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = AppDimens.Elevation.low)
    ) {
        Row(
            modifier = Modifier.padding(AppDimens.Layout.screenPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing.small)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = accentColor.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(AppDimens.Radius.medium)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun MetricGrid(state: DashboardUiState) {
    val items = listOf(
        DashboardMetricUi("إيراد اليوم", formatMoney(state.totalSalesAmount, state.currencyCode), IncomeColor),
        DashboardMetricUi("إجمالي المصروفات", formatMoney(state.totalExpensesAmount, state.currencyCode), ExpenseColor),
        DashboardMetricUi("صافي الرصيد", formatMoney(state.netAmount, state.currencyCode), InfoColor),
        DashboardMetricUi("المدفوع", formatMoney(state.totalPaidAmount, state.currencyCode), IncomeColor),
        DashboardMetricUi("المتبقي", formatMoney(state.totalPendingAmount, state.currencyCode), WarningColor),
        DashboardMetricUi("الوضع", if (state.isReadOnlyMode) "قراءة فقط" else "نشط", if (state.isReadOnlyMode) WarningColor else IncomeColor)
    )

    Column(verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing.medium)) {
        items.chunked(2).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing.medium)
            ) {
                rowItems.forEach { item ->
                    MetricCard(
                        modifier = Modifier.weight(1f),
                        title = item.title,
                        value = item.value,
                        accentColor = item.accentColor
                    )
                }
                if (rowItems.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun MetricCard(
    title: String,
    value: String,
    accentColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(AppDimens.Radius.large),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = AppDimens.Elevation.low)
    ) {
        Column(
            modifier = Modifier.padding(AppDimens.Layout.screenPadding),
            verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing.small)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(
                        color = accentColor.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(AppDimens.Radius.medium)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(accentColor, shape = RoundedCornerShape(AppDimens.Radius.round))
                )
            }

            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun RecentSalesSection(
    items: List<Sale>,
    onItemClick: (String) -> Unit
) {
    RecentSection(
        title = "المبيعات الأخيرة",
        emptyText = "لا توجد مبيعات حديثة",
        icon = Icons.Filled.ShoppingCart,
        items = items.take(4),
        onItemClick = onItemClick,
        itemRenderer = { sale ->
            sale.safeTitle("عملية بيع") to sale.safeSubtitle("تفاصيل البيع")
        }
    )
}

@Composable
private fun RecentExpensesSection(
    items: List<Expense>,
    onItemClick: (String) -> Unit
) {
    RecentSection(
        title = "المصروفات الأخيرة",
        emptyText = "لا توجد مصروفات حديثة",
        icon = Icons.Filled.ReceiptLong,
        items = items.take(4),
        onItemClick = onItemClick,
        itemRenderer = { expense ->
            expense.safeTitle("مصروف") to expense.safeSubtitle("تفاصيل المصروف")
        }
    )
}

@Composable
private fun RecentReportsSection(
    items: List<Report>,
    onItemClick: (String) -> Unit
) {
    RecentSection(
        title = "التقارير الأخيرة",
        emptyText = "لا توجد تقارير حديثة",
        icon = Icons.Filled.Description,
        items = items.take(4),
        onItemClick = onItemClick,
        itemRenderer = { report ->
            report.safeTitle("تقرير") to report.safeSubtitle("تفاصيل التقرير")
        }
    )
}

@Composable
private fun RecentTransactionsSection(
    items: List<PaymentTransaction>,
    onItemClick: (String) -> Unit
) {
    RecentSection(
        title = "المدفوعات الأخيرة",
        emptyText = "لا توجد مدفوعات حديثة",
        icon = Icons.Filled.Payments,
        items = items.take(4),
        onItemClick = onItemClick,
        itemRenderer = { transaction ->
            transaction.safeTitle("حركة مالية") to transaction.safeSubtitle("تفاصيل الحركة")
        }
    )
}

@Composable
private fun <T : Any> RecentSection(
    title: String,
    emptyText: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    items: List<T>,
    onItemClick: (String) -> Unit,
    itemRenderer: (T) -> Pair<String, String>
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing.small),
        modifier = Modifier.padding(horizontal = AppDimens.Layout.screenPadding)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )

        if (items.isEmpty()) {
            OutlinedCard(shape = RoundedCornerShape(AppDimens.Radius.large)) {
                Text(
                    text = emptyText,
                    modifier = Modifier.padding(AppDimens.Layout.screenPadding),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing.small)) {
                items.forEach { item ->
                    val (itemTitle, itemSubtitle) = itemRenderer(item)
                    RecentRow(
                        title = itemTitle,
                        subtitle = itemSubtitle,
                        leadingIcon = icon,
                        onClick = { item.extractId()?.let(onItemClick) }
                    )
                }
            }
        }
    }
}

@Composable
private fun RecentRow(
    title: String,
    subtitle: String,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: (() -> Unit)? = null
) {
    val rowModifier = if (onClick != null) {
        Modifier.clickable { onClick() }
    } else {
        Modifier
    }

    Card(
        modifier = rowModifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppDimens.Radius.large),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = AppDimens.Elevation.none)
    ) {
        Row(
            modifier = Modifier.padding(AppDimens.Layout.screenPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing.medium)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(AppDimens.Radius.medium)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(horizontal = AppDimens.Layout.screenPadding)
    )
}

private data class DashboardMetricUi(
    val title: String,
    val value: String,
    val accentColor: androidx.compose.ui.graphics.Color
)

private data class DrawerMenuItem(
    val title: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val onClick: (() -> Unit)?
)

private fun dashboardSubtitle(state: DashboardUiState): String {
    return when {
        state.isRefreshing -> "جاري التحديث"
        state.isLoading -> "جاري التحميل"
        state.hasError -> "توجد مشكلة مؤقتة"
        else -> "جاهزة للعرض"
    }
}

private fun DashboardPeriod.arabicLabel(): String {
    return when (this) {
        DashboardPeriod.Today -> "اليوم"
        DashboardPeriod.Week -> "الأسبوع"
        DashboardPeriod.Month -> "الشهر"
        DashboardPeriod.Year -> "السنة"
        DashboardPeriod.AllTime -> "الكل"
    }
}

private fun formatMoney(value: BigDecimal, currencyCode: String): String {
    val cleanValue = value.stripTrailingZeros().toPlainString()
    return if (currencyCode.isBlank()) cleanValue else "$cleanValue $currencyCode"
}

private fun formatTime(epochMillis: Long): String {
    return runCatching {
        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(epochMillis))
    }.getOrElse { epochMillis.toString() }
}

private fun Any.safeTitle(fallback: String): String {
    return readString("title", "name", "label", "code", "number") ?: fallback
}

private fun Any.safeSubtitle(fallback: String): String {
    val amount = readString("amount", "total", "value")
    val date = readString("createdAt", "date", "time", "updatedAt", "timestamp")
    return when {
        !amount.isNullOrBlank() && !date.isNullOrBlank() -> "$amount • $date"
        !amount.isNullOrBlank() -> amount
        !date.isNullOrBlank() -> date
        else -> fallback
    }
}

private fun <T : Any> T.extractId(): String? {
    return readString("id", "saleId", "expenseId", "reportId", "transactionId")
}

private fun Any.readString(vararg names: String): String? {
    for (name in names) {
        val value = runCatching {
            val candidates = listOf(
                name,
                "get${name.capitalizeSafe()}",
                "is${name.capitalizeSafe()}"
            )

            var resolved: Any? = null
            for (methodName in candidates) {
                val method = javaClass.methods.firstOrNull { it.name == methodName && it.parameterCount == 0 }
                if (method != null) {
                    resolved = method.invoke(this)
                    break
                }
            }
            resolved
        }.getOrNull()

        when (value) {
            is String -> if (value.isNotBlank()) return value
            is Number, is Boolean -> return value.toString()
        }
    }
    return null
}

private fun String.capitalizeSafe(): String {
    return replaceFirstChar { first ->
        if (first.isLowerCase()) first.titlecase(Locale.ROOT) else first.toString()
    }
}