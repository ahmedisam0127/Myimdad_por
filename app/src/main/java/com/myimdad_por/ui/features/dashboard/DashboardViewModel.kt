package com.myimdad_por.ui.features.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myimdad_por.domain.model.Report
import com.myimdad_por.domain.model.ReportPeriod
import com.myimdad_por.domain.model.ReportType
import com.myimdad_por.domain.usecase.GetReportsRequest
import com.myimdad_por.domain.usecase.GetReportsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.ZoneId
import java.util.Calendar
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val getReportsUseCase: GetReportsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private val _uiEvent = MutableSharedFlow<DashboardUiEvent>(
        replay = 0,
        extraBufferCapacity = 1
    )
    val uiEvent: SharedFlow<DashboardUiEvent> = _uiEvent.asSharedFlow()

    init {
        onEvent(DashboardUiEvent.Load)
    }

    fun onEvent(event: DashboardUiEvent) {
        when (event) {
            DashboardUiEvent.Load -> loadDashboard(isRefreshing = false)
            DashboardUiEvent.Refresh -> loadDashboard(isRefreshing = true)
            is DashboardUiEvent.ChangePeriod -> changePeriod(event.period)
            is DashboardUiEvent.OnSaleClick -> emitEvent(event)
            is DashboardUiEvent.OnExpenseClick -> emitEvent(event)
            is DashboardUiEvent.OnReportClick -> emitEvent(event)
            is DashboardUiEvent.OnTransactionClick -> emitEvent(event)
            DashboardUiEvent.NavigateToSales -> emitEvent(event)
            DashboardUiEvent.NavigateToExpenses -> emitEvent(event)
            DashboardUiEvent.NavigateToReports -> emitEvent(event)
            DashboardUiEvent.Retry -> loadDashboard(isRefreshing = false)
        }
    }

    private fun changePeriod(period: DashboardPeriod) {
        _uiState.update { it.copy(selectedPeriod = period, errorMessage = null) }
        loadDashboard(isRefreshing = false)
    }

    private fun loadDashboard(isRefreshing: Boolean) {
        viewModelScope.launch {
            try {
                _uiState.update {
                    it.copy(
                        isLoading = !isRefreshing && it.lastUpdatedAtMillis == null,
                        isRefreshing = isRefreshing,
                        errorMessage = null
                    )
                }

                val period = _uiState.value.selectedPeriod.toReportPeriod()
                val request = GetReportsRequest(
                    period = period,
                    type = null,
                    generateIfEmpty = false,
                    useGeneratorWhenPossible = false
                )

                val reportsResult = getReportsUseCase(request)
                    .getOrElse { throw it }

                val recentReports = reportsResult.reports.take(5)
                val reportCount = reportsResult.count
                val reportSummary = buildReportSummary(recentReports)

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        errorMessage = null,
                        reportsCount = reportCount,
                        recentReports = recentReports,
                        totalSalesAmount = reportSummary.totalSalesAmount,
                        totalExpensesAmount = reportSummary.totalExpensesAmount,
                        netAmount = reportSummary.netAmount,
                        lastUpdatedAtMillis = System.currentTimeMillis()
                    )
                }
            } catch (throwable: Throwable) {
                if (throwable is CancellationException) throw throwable
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        errorMessage = throwable.message ?: "حدث خطأ غير متوقع"
                    )
                }
            }
        }
    }

    private fun emitEvent(event: DashboardUiEvent) {
        viewModelScope.launch {
            _uiEvent.emit(event)
        }
    }

    private fun DashboardPeriod.toReportPeriod(): ReportPeriod? {
        val now = LocalDate.now()
        val zoneId = ZoneId.systemDefault()

        fun startOfDayMillis(date: LocalDate): Long = date
            .atStartOfDay(zoneId)
            .toInstant()
            .toEpochMilli()

        return when (this) {
            DashboardPeriod.Today -> ReportPeriod(
                fromMillis = startOfDayMillis(now),
                toMillis = System.currentTimeMillis()
            )

            DashboardPeriod.Week -> {
                val calendar = Calendar.getInstance().apply {
                    timeInMillis = System.currentTimeMillis()
                    set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                ReportPeriod(
                    fromMillis = calendar.timeInMillis,
                    toMillis = System.currentTimeMillis()
                )
            }

            DashboardPeriod.Month -> ReportPeriod(
                fromMillis = startOfDayMillis(now.withDayOfMonth(1)),
                toMillis = System.currentTimeMillis()
            )

            DashboardPeriod.Year -> ReportPeriod(
                fromMillis = startOfDayMillis(now.withDayOfYear(1)),
                toMillis = System.currentTimeMillis()
            )

            DashboardPeriod.AllTime -> null
        }
    }

    private data class ReportSummary(
        val totalSalesAmount: java.math.BigDecimal = java.math.BigDecimal.ZERO,
        val totalExpensesAmount: java.math.BigDecimal = java.math.BigDecimal.ZERO,
        val netAmount: java.math.BigDecimal = java.math.BigDecimal.ZERO
    )

    private fun buildReportSummary(reports: List<Report>): ReportSummary {
        if (reports.isEmpty()) return ReportSummary()

        val total = reports.fold(java.math.BigDecimal.ZERO) { acc, report ->
            acc.add(report.totalNumericValue)
        }

        return ReportSummary(
            totalSalesAmount = total,
            totalExpensesAmount = java.math.BigDecimal.ZERO,
            netAmount = total
        )
    }
}
