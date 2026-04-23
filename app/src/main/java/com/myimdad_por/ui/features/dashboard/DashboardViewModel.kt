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
import java.util.Locale
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

            is DashboardUiEvent.OnSaleClick -> Unit
            is DashboardUiEvent.OnExpenseClick -> Unit
            is DashboardUiEvent.OnReportClick -> Unit
            is DashboardUiEvent.OnTransactionClick -> Unit
            DashboardUiEvent.NavigateToSales -> Unit
            DashboardUiEvent.NavigateToExpenses -> Unit
            DashboardUiEvent.NavigateToReports -> Unit
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
            val base = current.copy()

            base.copy(
                isLoading = false,
                isRefreshing = false,
                errorMessage = null,
                selectedPeriod = if (preserveSelection) base.selectedPeriod else DashboardPeriod.Today,
                currencyCode = snapshot.currencyCode
                    ?: analytics?.summary?.currencyCode
                    ?: base.currencyCode,

                salesCount = snapshot.salesCount ?: base.salesCount,
                expensesCount = snapshot.expensesCount ?: base.expensesCount,
                reportsCount = snapshot.reportsCount ?: base.reportsCount,
                paymentTransactionsCount = snapshot.paymentTransactionsCount ?: base.paymentTransactionsCount,
                pendingPaymentsCount = snapshot.pendingPaymentsCount ?: base.pendingPaymentsCount,
                paidPaymentsCount = snapshot.paidPaymentsCount ?: base.paidPaymentsCount,

                totalSalesAmount = snapshot.totalSalesAmount ?: base.totalSalesAmount,
                totalExpensesAmount = snapshot.totalExpensesAmount ?: base.totalExpensesAmount,
                netAmount = snapshot.netAmount ?: base.netAmount,
                totalPaidAmount = snapshot.totalPaidAmount ?: base.totalPaidAmount,
                totalPendingAmount = snapshot.totalPendingAmount ?: base.totalPendingAmount,

                recentSales = snapshot.recentSales.ifNotEmptyElse(base.recentSales),
                recentExpenses = snapshot.recentExpenses.ifNotEmptyElse(base.recentExpenses),
                recentReports = snapshot.recentReports.ifNotEmptyElse(base.recentReports),
                recentTransactions = snapshot.recentTransactions.ifNotEmptyElse(base.recentTransactions),

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

    private fun currentDashboardState(): DashboardUiState {
        return currentData ?: DashboardUiState()
    }

    private fun updateDashboardState(transform: (DashboardUiState) -> DashboardUiState) {
        val current = currentData ?: DashboardUiState()
        setSuccess(transform(current))
    }

    private fun Dashboard.extractSnapshot(): DashboardSnapshot {
        val root: Any = this

        val recentSales = root.typedList<Sale>("recentSales", "sales", "salesItems", "lastSales")
        val recentExpenses = root.typedList<Expense>("recentExpenses", "expenses", "expenseItems", "lastExpenses")
        val recentReports = root.typedList<Report>("recentReports", "reports", "reportItems", "lastReports")
        val recentTransactions = root.typedList<PaymentTransaction>(
            "recentTransactions",
            "paymentTransactions",
            "transactions",
            "lastTransactions"
        )

        val salesNode = root.propertyValue("sales")
        val expensesNode = root.propertyValue("expenses")
        val reportsNode = root.propertyValue("reports")
        val transactionsNode = root.propertyValue("paymentTransactions")
            ?: root.propertyValue("transactions")
            ?: root.propertyValue("payments")
            ?: root.propertyValue("financialTransactions")

        val analyticsSummary = runCatching { getAnalyticsData(this).summary }.getOrNull()

        return DashboardSnapshot(
            currencyCode = root.stringProperty("currencyCode", "currency", "currencyIsoCode")
                ?: analyticsSummary?.currencyCode,

            salesCount = root.intProperty("salesCount", "totalSalesCount")
                ?: recentSales.size.takeIf { it > 0 }
                ?: salesNode?.intProperty("count", "totalCount", "itemsCount")
                ?: salesNode?.listSize("items", "values"),

            expensesCount = root.intProperty("expensesCount", "totalExpensesCount")
                ?: recentExpenses.size.takeIf { it > 0 }
                ?: expensesNode?.intProperty("count", "totalCount", "itemsCount")
                ?: expensesNode?.listSize("items", "values"),

            reportsCount = root.intProperty("reportsCount", "totalReportsCount")
                ?: recentReports.size.takeIf { it > 0 }
                ?: reportsNode?.intProperty("count", "totalCount", "itemsCount")
                ?: reportsNode?.listSize("items", "values"),

            paymentTransactionsCount = root.intProperty(
                "paymentTransactionsCount",
                "transactionsCount",
                "totalTransactionsCount"
            ) ?: recentTransactions.size.takeIf { it > 0 }
                ?: transactionsNode?.intProperty("count", "totalCount", "itemsCount")
                ?: transactionsNode?.listSize("items", "values"),

            pendingPaymentsCount = root.intProperty("pendingPaymentsCount", "pendingCount")
                ?: transactionsNode?.intProperty("pendingCount", "pendingPaymentsCount"),

            paidPaymentsCount = root.intProperty("paidPaymentsCount", "paidCount")
                ?: transactionsNode?.intProperty("paidCount", "paidPaymentsCount"),

            totalSalesAmount = root.bigDecimalProperty(
                "totalSalesAmount",
                "salesAmount",
                "salesTotalAmount",
                "revenueAmount"
            ) ?: salesNode?.bigDecimalProperty("totalAmount", "amount", "revenue"),

            totalExpensesAmount = root.bigDecimalProperty(
                "totalExpensesAmount",
                "expensesAmount",
                "expensesTotalAmount"
            ) ?: expensesNode?.bigDecimalProperty("totalAmount", "amount"),

            netAmount = root.bigDecimalProperty("netAmount", "balance", "netBalance")
                ?: root.propertyValue("financial")?.bigDecimalProperty("netAmount", "balance", "netBalance"),

            totalPaidAmount = root.bigDecimalProperty("totalPaidAmount", "paidAmount")
                ?: transactionsNode?.bigDecimalProperty("paidAmount", "totalPaidAmount"),

            totalPendingAmount = root.bigDecimalProperty("totalPendingAmount", "pendingAmount")
                ?: transactionsNode?.bigDecimalProperty("pendingAmount", "totalPendingAmount"),

            recentSales = recentSales,
            recentExpenses = recentExpenses,
            recentReports = recentReports,
            recentTransactions = recentTransactions,

            canAccessReports = root.booleanProperty("canAccessReports", "allowReports"),
            canUsePaidFeatures = root.booleanProperty("canUsePaidFeatures", "allowPaidFeatures"),
            isReadOnlyMode = root.booleanProperty("isReadOnlyMode", "readOnlyMode"),

            lastUpdatedAtMillis = root.longProperty("lastUpdatedAtMillis", "lastUpdatedAtEpochMillis", "updatedAtMillis")
        )
    }

    private fun Any?.propertyValue(name: String): Any? {
        val receiver = this ?: return null

        val methodNames = listOf(
            name,
            "get${name.capitalizeSafe()}",
            "is${name.capitalizeSafe()}"
        )

        for (methodName in methodNames) {
            val method = receiver.javaClass.methods.firstOrNull {
                it.name == methodName && it.parameterCount == 0
            }
            if (method != null) {
                return runCatching { method.invoke(receiver) }.getOrNull()
            }
        }

        return runCatching {
            receiver.javaClass.getDeclaredField(name).apply { isAccessible = true }.get(receiver)
        }.getOrNull()
    }

    private fun Any?.stringProperty(vararg names: String): String? {
        for (name in names) {
            when (val value = propertyValue(name)) {
                is String -> if (value.isNotBlank()) return value
                is Number, is Boolean -> return value.toString()
            }
        }
        return null
    }

    private fun Any?.intProperty(vararg names: String): Int? {
        for (name in names) {
            when (val value = propertyValue(name)) {
                is Int -> return value
                is Number -> return value.toInt()
                is String -> value.toIntOrNull()?.let { return it }
            }
        }
        return null
    }

    private fun Any?.longProperty(vararg names: String): Long? {
        for (name in names) {
            when (val value = propertyValue(name)) {
                is Long -> return value
                is Number -> return value.toLong()
                is String -> value.toLongOrNull()?.let { return it }
            }
        }
        return null
    }

    private fun Any?.booleanProperty(vararg names: String): Boolean? {
        for (name in names) {
            when (val value = propertyValue(name)) {
                is Boolean -> return value
                is String -> when (value.lowercase(Locale.ROOT)) {
                    "true", "1", "yes", "y", "on" -> return true
                    "false", "0", "no", "n", "off" -> return false
                }
                is Number -> return value.toInt() != 0
            }
        }
        return null
    }

    private fun Any?.bigDecimalProperty(vararg names: String): BigDecimal? {
        for (name in names) {
            when (val value = propertyValue(name)) {
                is BigDecimal -> return value
                is Number -> return BigDecimal.valueOf(value.toDouble())
                is String -> value.toBigDecimalOrNull()?.let { return it }
            }
        }
        return null
    }

    private inline fun <reified T> Any?.typedList(vararg names: String): List<T> {
        for (name in names) {
            when (val value = propertyValue(name)) {
                is List<*> -> return value.mapNotNull { it as? T }
                is Array<*> -> return value.mapNotNull { it as? T }
                is Collection<*> -> return value.mapNotNull { it as? T }
            }
        }
        return emptyList()
    }

    private fun Any?.listSize(vararg names: String): Int? {
        for (name in names) {
            when (val value = propertyValue(name)) {
                is List<*> -> return value.size
                is Array<*> -> return value.size
                is Collection<*> -> return value.size
            }
        }
        return null
    }

    private fun String.capitalizeSafe(): String {
        return replaceFirstChar { first ->
            if (first.isLowerCase()) first.titlecase(Locale.ROOT) else first.toString()
        }
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