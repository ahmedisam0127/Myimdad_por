package com.myimdad_por.ui.features.dashboard

import androidx.lifecycle.viewModelScope
import com.myimdad_por.core.base.BaseViewModel
import com.myimdad_por.core.dispatchers.AppDispatchers
import com.myimdad_por.core.security.SessionManager
import com.myimdad_por.domain.model.Dashboard
import com.myimdad_por.domain.model.Expense
import com.myimdad_por.domain.model.PaymentTransaction
import com.myimdad_por.domain.model.Report
import com.myimdad_por.domain.model.Sale
import com.myimdad_por.domain.repository.DashboardRepository
import com.myimdad_por.domain.usecase.dashboard.GetAnalyticsData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.math.BigDecimal
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: DashboardRepository,
    private val getAnalyticsData: GetAnalyticsData,
    dispatchers: AppDispatchers
) : BaseViewModel<DashboardUiState>(dispatchers) {

    private var observeJob: Job? = null
    private var actionJob: Job? = null

    init {
        setSuccess(DashboardUiState(isLoading = true))
        observeDashboard()
        loadDashboard(forceRefresh = false)
    }

    fun onEvent(event: DashboardUiEvent) {
        when (event) {
            DashboardUiEvent.Load -> loadDashboard(forceRefresh = false)
            DashboardUiEvent.Refresh -> refreshDashboard()
            DashboardUiEvent.Retry -> loadDashboard(forceRefresh = true)
            is DashboardUiEvent.ChangePeriod -> changePeriod(event.period)
            DashboardUiEvent.NavigateToSales -> Unit
            DashboardUiEvent.NavigateToExpenses -> Unit
            DashboardUiEvent.NavigateToReports -> Unit
            is DashboardUiEvent.OnSaleClick -> Unit
            is DashboardUiEvent.OnExpenseClick -> Unit
            is DashboardUiEvent.OnReportClick -> Unit
            is DashboardUiEvent.OnTransactionClick -> Unit
        }
    }

    private fun observeDashboard() {
        if (observeJob?.isActive == true) return

        observeJob = repository.observeDashboard()
            .onEach { dashboard ->
                applyDashboard(dashboard, preserveSelection = true)
            }
            .catch { throwable ->
                applyError(throwable, keepContent = true)
            }
            .launchIn(viewModelScope)
    }

    private fun loadDashboard(forceRefresh: Boolean) {
        actionJob?.cancel()
        actionJob = launch(dispatcher = dispatchers.main) {
            setBusy(isRefreshing = false)

            repository.getDashboard(forceRefresh = forceRefresh)
                .onSuccess { dashboard ->
                    applyDashboard(dashboard, preserveSelection = true)
                }
                .onFailure { throwable ->
                    applyError(throwable, keepContent = true)
                }
        }
    }

    private fun refreshDashboard() {
        actionJob?.cancel()
        actionJob = launch(dispatcher = dispatchers.main) {
            setBusy(isRefreshing = true)

            repository.refreshDashboard()
                .onSuccess { dashboard ->
                    applyDashboard(dashboard, preserveSelection = true)
                }
                .onFailure { throwable ->
                    applyError(throwable, keepContent = true)
                }
        }
    }

    private fun changePeriod(period: DashboardPeriod) {
        updateDashboardState { current ->
            current.copy(
                selectedPeriod = period,
                errorMessage = null
            )
        }
    }

    private fun applyDashboard(
        dashboard: Dashboard,
        preserveSelection: Boolean
    ) {
        val snapshot = dashboard.extractSnapshot()
        val analytics = runCatching { getAnalyticsData(dashboard) }.getOrNull()
        val security = resolveSecuritySnapshot()

        updateDashboardState { current ->
            current.copy(
                isLoading = false,
                isRefreshing = false,
                errorMessage = null,
                selectedPeriod = if (preserveSelection) current.selectedPeriod else DashboardPeriod.Today,

                currencyCode = snapshot.currencyCode
                    ?: analytics?.summary?.currencyCode
                    ?: current.currencyCode,

                salesCount = snapshot.salesCount ?: current.salesCount,
                expensesCount = snapshot.expensesCount ?: current.expensesCount,
                reportsCount = snapshot.reportsCount ?: current.reportsCount,
                paymentTransactionsCount = snapshot.paymentTransactionsCount ?: current.paymentTransactionsCount,
                pendingPaymentsCount = snapshot.pendingPaymentsCount ?: current.pendingPaymentsCount,
                paidPaymentsCount = snapshot.paidPaymentsCount ?: current.paidPaymentsCount,

                totalSalesAmount = snapshot.totalSalesAmount ?: current.totalSalesAmount,
                totalExpensesAmount = snapshot.totalExpensesAmount ?: current.totalExpensesAmount,
                netAmount = snapshot.netAmount ?: current.netAmount,
                totalPaidAmount = snapshot.totalPaidAmount ?: current.totalPaidAmount,
                totalPendingAmount = snapshot.totalPendingAmount ?: current.totalPendingAmount,

                recentSales = snapshot.recentSales.ifNotEmptyElse(current.recentSales),
                recentExpenses = snapshot.recentExpenses.ifNotEmptyElse(current.recentExpenses),
                recentReports = snapshot.recentReports.ifNotEmptyElse(current.recentReports),
                recentTransactions = snapshot.recentTransactions.ifNotEmptyElse(current.recentTransactions),

                canAccessReports = snapshot.canAccessReports ?: security.canAccessReports,
                canUsePaidFeatures = snapshot.canUsePaidFeatures ?: security.canUsePaidFeatures,
                isReadOnlyMode = snapshot.isReadOnlyMode ?: security.isReadOnlyMode,
                lastUpdatedAtMillis = snapshot.lastUpdatedAtMillis ?: System.currentTimeMillis()
            )
        }
    }

    private fun applyError(
        throwable: Throwable,
        keepContent: Boolean
    ) {
        val message = throwable.message?.takeIf { it.isNotBlank() } ?: "حدث خطأ غير متوقع"

        updateDashboardState { current ->
            if (keepContent) {
                current.copy(
                    isLoading = false,
                    isRefreshing = false,
                    errorMessage = message
                )
            } else {
                DashboardUiState(
                    isLoading = false,
                    isRefreshing = false,
                    errorMessage = message,
                    selectedPeriod = current.selectedPeriod,
                    currencyCode = current.currencyCode,
                    salesCount = current.salesCount,
                    expensesCount = current.expensesCount,
                    reportsCount = current.reportsCount,
                    paymentTransactionsCount = current.paymentTransactionsCount,
                    pendingPaymentsCount = current.pendingPaymentsCount,
                    paidPaymentsCount = current.paidPaymentsCount,
                    totalSalesAmount = current.totalSalesAmount,
                    totalExpensesAmount = current.totalExpensesAmount,
                    netAmount = current.netAmount,
                    totalPaidAmount = current.totalPaidAmount,
                    totalPendingAmount = current.totalPendingAmount,
                    recentSales = current.recentSales,
                    recentExpenses = current.recentExpenses,
                    recentReports = current.recentReports,
                    recentTransactions = current.recentTransactions,
                    canAccessReports = current.canAccessReports,
                    canUsePaidFeatures = current.canUsePaidFeatures,
                    isReadOnlyMode = current.isReadOnlyMode,
                    lastUpdatedAtMillis = current.lastUpdatedAtMillis
                )
            }
        }
    }

    private fun setBusy(isRefreshing: Boolean) {
        updateDashboardState { current ->
            current.copy(
                isLoading = !current.hasContent && !isRefreshing,
                isRefreshing = isRefreshing || current.hasContent,
                errorMessage = null
            )
        }
    }

    private fun resolveSecuritySnapshot(): SecuritySnapshot {
        val hasValidSession = runCatching { SessionManager.hasValidSession() }.getOrDefault(false)
        val restricted = runCatching { SessionManager.shouldRestrictSensitiveOperations() }.getOrDefault(true)

        return SecuritySnapshot(
            canAccessReports = hasValidSession && !restricted,
            canUsePaidFeatures = hasValidSession && !restricted,
            isReadOnlyMode = !hasValidSession || restricted
        )
    }

    private fun updateDashboardState(transform: (DashboardUiState) -> DashboardUiState) {
        val current = currentData ?: DashboardUiState()
        setSuccess(transform(current))
    }

    private fun Dashboard.extractSnapshot(): DashboardSnapshot {
        return DashboardSnapshot(
            currencyCode = financial.currencyCode,
            salesCount = sales.todaySalesCount,
            expensesCount = 0,
            reportsCount = 0,
            paymentTransactionsCount = 0,
            pendingPaymentsCount = sales.pendingInvoicesCount,
            paidPaymentsCount = 0,
            totalSalesAmount = sales.todayRevenue,
            totalExpensesAmount = BigDecimal.ZERO,
            netAmount = financial.netBalance,
            totalPaidAmount = financial.totalCashIn,
            totalPendingAmount = financial.payables,
            recentSales = emptyList(),
            recentExpenses = emptyList(),
            recentReports = emptyList(),
            recentTransactions = emptyList(),
            canAccessReports = null,
            canUsePaidFeatures = null,
            isReadOnlyMode = null,
            lastUpdatedAtMillis = lastUpdatedAtEpochMillis
        )
    }

    private fun <T> List<T>.ifNotEmptyElse(fallback: List<T>): List<T> {
        return if (isNotEmpty()) this else fallback
    }

    private data class DashboardSnapshot(
        val currencyCode: String? = null,
        val salesCount: Int? = null,
        val expensesCount: Int? = null,
        val reportsCount: Int? = null,
        val paymentTransactionsCount: Int? = null,
        val pendingPaymentsCount: Int? = null,
        val paidPaymentsCount: Int? = null,
        val totalSalesAmount: BigDecimal? = null,
        val totalExpensesAmount: BigDecimal? = null,
        val netAmount: BigDecimal? = null,
        val totalPaidAmount: BigDecimal? = null,
        val totalPendingAmount: BigDecimal? = null,
        val recentSales: List<Sale> = emptyList(),
        val recentExpenses: List<Expense> = emptyList(),
        val recentReports: List<Report> = emptyList(),
        val recentTransactions: List<PaymentTransaction> = emptyList(),
        val canAccessReports: Boolean? = null,
        val canUsePaidFeatures: Boolean? = null,
        val isReadOnlyMode: Boolean? = null,
        val lastUpdatedAtMillis: Long? = null
    )

    private data class SecuritySnapshot(
        val canAccessReports: Boolean,
        val canUsePaidFeatures: Boolean,
        val isReadOnlyMode: Boolean
    )
}