package com.myimdad_por.domain.model

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * نموذج شامل لبيانات لوحة التحكم.
 *
 * صُمم ليكون مرنًا وقابلًا للتوسع، بحيث يمكن ربطه لاحقًا مع:
 * - إحصاءات المبيعات
 * - حالة المخزون
 * - العملاء
 * - التنبيهات
 * - الإجراءات السريعة
 */
data class Dashboard(
    val overview: DashboardOverview = DashboardOverview(),
    val sales: DashboardSalesSummary = DashboardSalesSummary(),
    val inventory: DashboardInventorySummary = DashboardInventorySummary(),
    val customers: DashboardCustomerSummary = DashboardCustomerSummary(),
    val financial: DashboardFinancialSummary = DashboardFinancialSummary(),
    val alerts: List<DashboardAlert> = emptyList(),
    val quickActions: List<DashboardQuickAction> = emptyList(),
    val lastUpdatedAtEpochMillis: Long? = null
) {
    val hasAlerts: Boolean
        get() = alerts.isNotEmpty()

    val hasQuickActions: Boolean
        get() = quickActions.isNotEmpty()

    val isFresh: Boolean
        get() = lastUpdatedAtEpochMillis != null
}

data class DashboardOverview(
    val title: String = "لوحة التحكم",
    val subtitle: String? = null,
    val greeting: String? = null,
    val totalMetrics: Int = 0,
    val positiveMetrics: Int = 0,
    val negativeMetrics: Int = 0
) {
    val hasPerformanceData: Boolean
        get() = totalMetrics > 0
}

data class DashboardSalesSummary(
    val todaySalesCount: Int = 0,
    val todayRevenue: BigDecimal = BigDecimal.ZERO,
    val monthSalesCount: Int = 0,
    val monthRevenue: BigDecimal = BigDecimal.ZERO,
    val pendingInvoicesCount: Int = 0,
    val returnsCount: Int = 0,
    val topSellingProductName: String? = null,
    val growthRatePercent: BigDecimal? = null
) {
    val hasGrowthRate: Boolean
        get() = growthRatePercent != null
}

data class DashboardInventorySummary(
    val productsCount: Int = 0,
    val lowStockCount: Int = 0,
    val outOfStockCount: Int = 0,
    val totalStockValue: BigDecimal = BigDecimal.ZERO,
    val reservedItemsCount: Int = 0,
    val mostCriticalProductName: String? = null
) {
    val hasStockRisk: Boolean
        get() = lowStockCount > 0 || outOfStockCount > 0
}

data class DashboardCustomerSummary(
    val customersCount: Int = 0,
    val newCustomersCount: Int = 0,
    val activeCustomersCount: Int = 0,
    val dueCustomersCount: Int = 0,
    val topCustomerName: String? = null,
    val averageOrderValue: BigDecimal = BigDecimal.ZERO
)

data class DashboardFinancialSummary(
    val totalCashIn: BigDecimal = BigDecimal.ZERO,
    val totalCashOut: BigDecimal = BigDecimal.ZERO,
    val netBalance: BigDecimal = BigDecimal.ZERO,
    val receivables: BigDecimal = BigDecimal.ZERO,
    val payables: BigDecimal = BigDecimal.ZERO,
    val profitEstimate: BigDecimal? = null,
    val currencyCode: String = "SDG"
) {
    val hasProfitData: Boolean
        get() = profitEstimate != null
}

data class DashboardMetric(
    val key: String,
    val label: String,
    val value: BigDecimal,
    val formattedValue: String? = null,
    val deltaPercent: BigDecimal? = null,
    val deltaLabel: String? = null,
    val isPositiveTrend: Boolean = true,
    val accentKey: String? = null
) {
    val hasDelta: Boolean
        get() = deltaPercent != null

    fun normalizedValue(scale: Int = 2): BigDecimal = value.setScale(scale, RoundingMode.HALF_UP)
}

enum class DashboardAlertLevel {
    Info,
    Warning,
    Critical
}

data class DashboardAlert(
    val id: String,
    val title: String,
    val message: String,
    val level: DashboardAlertLevel = DashboardAlertLevel.Info,
    val actionLabel: String? = null,
    val targetRoute: String? = null,
    val isDismissible: Boolean = true
) {
    val hasAction: Boolean
        get() = !actionLabel.isNullOrBlank() && !targetRoute.isNullOrBlank()
}

data class DashboardQuickAction(
    val id: String,
    val title: String,
    val description: String? = null,
    val iconName: String? = null,
    val route: String? = null,
    val isEnabled: Boolean = true,
    val requiresPermission: String? = null
) {
    val canNavigate: Boolean
        get() = isEnabled && !route.isNullOrBlank()
}

fun BigDecimal.money(scale: Int = 2): BigDecimal = setScale(scale, RoundingMode.HALF_UP)
