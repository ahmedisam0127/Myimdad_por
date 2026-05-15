package com.myimdad_por.ui.features.dashboard

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import com.myimdad_por.core.base.BaseViewModel
import com.myimdad_por.core.base.UiState
import com.myimdad_por.core.dispatchers.AppDispatchers
import com.myimdad_por.domain.model.Dashboard
import com.myimdad_por.domain.model.DashboardMetric
import com.myimdad_por.domain.model.money
import com.myimdad_por.domain.repository.DashboardRepository
import com.myimdad_por.domain.usecase.dashboard.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.math.BigDecimal

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: DashboardRepository,
    dispatchers: AppDispatchers,// تأكد من وجود @Inject في الـ Dispatcher module
    private val refreshDashboardDataUseCase: RefreshDashboardData,
    private val cacheDashboardDataUseCase: CacheDashboardData,
    private val realtimeDashboardUpdatesUseCase: GetRealtimeDashboardUpdates,
    private val getAnalyticsData: GetAnalyticsData,
    private val calculateKpis: CalculateKpis,
    private val manageDashboardWidgets: ManageDashboardWidgets,
    private val checkDashboardAccess: CheckDashboardAccess,
) : BaseViewModel<DashboardUiState>(dispatchers) {

    private val dashboardPermissions = DashboardPermissions()
    private val widgetConfig = DashboardWidgetConfig()

    private var latestDashboard: Dashboard? = null
    private var selectedPeriod: DashboardPeriod = DashboardPeriod.Today
    private var observationJob: Job? = null

    private val _snapshot = MutableStateFlow<DashboardSnapshot?>(null)
    val snapshot: StateFlow<DashboardSnapshot?> = _snapshot.asStateFlow()

    init {
        restartObservation()
        loadDashboard(forceRefresh = false)
    }

    fun onEvent(event: DashboardUiEvent) {
        when (event) {
            DashboardUiEvent.Load -> loadDashboard(forceRefresh = false)
            DashboardUiEvent.Refresh -> refreshDashboard()
            is DashboardUiEvent.ChangePeriod -> changePeriod(event.period)
            is DashboardUiEvent.OnSaleClick -> handleSaleClick(event.saleId)
            is DashboardUiEvent.OnExpenseClick -> handleExpenseClick(event.expenseId)
            is DashboardUiEvent.OnReportClick -> handleReportClick(event.reportId)
            is DashboardUiEvent.OnTransactionClick -> handleTransactionClick(event.transactionId)
            DashboardUiEvent.NavigateToSales -> selectSalesSection()
            DashboardUiEvent.NavigateToExpenses -> selectExpensesSection()
            DashboardUiEvent.NavigateToReports -> selectReportsSection()
            DashboardUiEvent.Retry -> retry()
        }
    }

    private fun loadDashboard(forceRefresh: Boolean) {
        launchStateful(dispatcher = dispatchers.io) {
            val result = if (forceRefresh) {
                refreshDashboardDataUseCase(forceRefresh = true, filter = currentFilter())
            } else {
                cacheDashboardDataUseCase(forceRefresh = false, filter = currentFilter())
            }

            result.fold(
                onSuccess = { dashboard ->
                    publishDashboard(dashboard)
                    UiState.Success(requireSnapshot().uiState)
                },
                onFailure = { throwable ->
                    val cached = latestDashboard
                    if (cached != null) {
                        publishDashboard(cached, throwable.message ?: DEFAULT_ERROR_MESSAGE)
                        UiState.Success(requireSnapshot().uiState)
                    } else {
                        UiState.Error(
                            message = throwable.message ?: DEFAULT_ERROR_MESSAGE,
                            throwable = throwable
                        )
                    }
                }
            )
        }
    }

    private fun refreshDashboard() {
        loadDashboard(forceRefresh = true)
    }

    private fun changePeriod(period: DashboardPeriod) {
        if (selectedPeriod == period) return
        selectedPeriod = period
        restartObservation()
        latestDashboard?.let { publishDashboard(it) }
    }

    private fun retry() {
        if (latestDashboard != null) refreshDashboard() else loadDashboard(false)
    }

    private fun restartObservation() {
        observationJob?.cancel()
        observationJob = viewModelScope.launch(dispatchers.io) {
            realtimeDashboardUpdatesUseCase(currentFilter()).collectLatest { dashboard ->
                publishDashboard(dashboard)
            }
        }
    }

    private fun publishDashboard(dashboard: Dashboard, errorMessage: String? = null) {
        latestDashboard = dashboard
        val snapshot = buildSnapshot(dashboard, errorMessage)
        _snapshot.value = snapshot
        setStateFromSnapshot(snapshot)
    }

    private fun setStateFromSnapshot(snapshot: DashboardSnapshot) {
        updateState { _ -> UiState.Success(snapshot.uiState) }
    }

    private fun buildSnapshot(dashboard: Dashboard, errorMessage: String? = null): DashboardSnapshot {
        val accessResult = checkDashboardAccess(dashboard, dashboardPermissions)
        return DashboardSnapshot(
            uiState = buildUiState(dashboard, accessResult, errorMessage),
            analyticsData = getAnalyticsData(dashboard),
            kpis = calculateKpis(dashboard),
            widgetsState = manageDashboardWidgets(dashboard, widgetConfig),
            accessResult = accessResult
        )
    }

    private fun buildUiState(
        dashboard: Dashboard,
        accessResult: DashboardAccessResult,
        errorMessage: String? = null
    ): DashboardUiState {
        return DashboardUiState(
            isLoading = false,
            isRefreshing = false,
            errorMessage = errorMessage,
            selectedPeriod = selectedPeriod,
            currencyCode = dashboard.financial.currencyCode.ifBlank { DEFAULT_CURRENCY_CODE },
            salesCount = resolveSalesCount(dashboard),
            expensesCount = resolveExpensesCount(dashboard),
            reportsCount = resolveReportsCount(dashboard, accessResult),
            paymentTransactionsCount = resolvePaymentTransactionsCount(dashboard),
            pendingPaymentsCount = resolvePendingPaymentsCount(dashboard),
            paidPaymentsCount = resolvePaidPaymentsCount(dashboard),
            totalSalesAmount = resolveSalesAmount(dashboard),
            totalExpensesAmount = dashboard.financial.totalCashOut.money(),
            netAmount = dashboard.financial.netBalance.money(),
            totalPaidAmount = dashboard.financial.totalCashIn.money(),
            totalPendingAmount = dashboard.financial.receivables.money(),
            canAccessReports = accessResult.canAccessDashboard,
            canUsePaidFeatures = dashboard.financial.payables > BigDecimal.ZERO,
            isReadOnlyMode = !accessResult.canAccessDashboard,
            lastUpdatedAtMillis = dashboard.lastUpdatedAtEpochMillis
        )
    }

    private fun currentFilter(): DashboardDataFilter = DashboardDataFilter(
        includeOverview = true, includeSales = true, includeInventory = true,
        includeCustomers = true, includeFinancial = true, includeAlerts = true,
        includeQuickActions = true, includeLastUpdatedAt = true,
        maxAlertsCount = 5, maxQuickActionsCount = 5
    )

    private fun resolveSalesCount(dashboard: Dashboard): Int = when (selectedPeriod) {
        DashboardPeriod.Today -> dashboard.sales.todaySalesCount
        else -> dashboard.sales.monthSalesCount
    }

    private fun resolveSalesAmount(dashboard: Dashboard): BigDecimal = when (selectedPeriod) {
        DashboardPeriod.Today -> dashboard.sales.todayRevenue.money()
        else -> dashboard.sales.monthRevenue.money()
    }

    private fun resolveExpensesCount(dashboard: Dashboard): Int =
        if (dashboard.financial.totalCashOut > BigDecimal.ZERO) 1 else 0

    private fun resolveReportsCount(dashboard: Dashboard, accessResult: DashboardAccessResult): Int =
        if (accessResult.allowedQuickActions.isNotEmpty()) accessResult.allowedQuickActions.size 
        else if (dashboard.hasAlerts) dashboard.alerts.size else 0

    private fun resolvePaymentTransactionsCount(dashboard: Dashboard): Int =
        if (dashboard.quickActions.isNotEmpty()) dashboard.quickActions.size else 0

    private fun resolvePendingPaymentsCount(dashboard: Dashboard): Int =
        if (dashboard.financial.receivables > BigDecimal.ZERO) 1 else 0

    private fun resolvePaidPaymentsCount(dashboard: Dashboard): Int =
        if (dashboard.financial.totalCashIn > BigDecimal.ZERO) 1 else 0

    private fun handleSaleClick(saleId: String) {}
    private fun handleExpenseClick(expenseId: String) {}
    private fun handleReportClick(reportId: String) {}
    private fun handleTransactionClick(transactionId: String) {}
    private fun selectSalesSection() = Unit
    private fun selectExpensesSection() = Unit
    private fun selectReportsSection() = Unit

    private fun requireSnapshot(): DashboardSnapshot = _snapshot.value ?: error("Dashboard snapshot is not available")

    override fun onCleared() {
        observationJob?.cancel()
        super.onCleared()
    }

    data class DashboardSnapshot(
        val uiState: DashboardUiState,
        val analyticsData: AnalyticsData,
        val kpis: List<DashboardMetric>,
        val widgetsState: DashboardWidgetsState,
        val accessResult: DashboardAccessResult
    )

    private companion object {
        const val DEFAULT_CURRENCY_CODE = "SDG"
        const val DEFAULT_ERROR_MESSAGE = "حدث خطأ غير متوقع"
    }
}
