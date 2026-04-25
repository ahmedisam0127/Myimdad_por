package com.myimdad_por.domain.usecase.dashboard

import com.myimdad_por.domain.model.Dashboard
import com.myimdad_por.domain.model.DashboardAlert
import com.myimdad_por.domain.model.DashboardAlertLevel
import com.myimdad_por.domain.model.DashboardMetric
import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject

/**
 * يجمع بيانات التحليلات الجاهزة للعرض من نموذج لوحة التحكم.
 *
 * هذا الـ use case لا يجلب البيانات من المصدر، بل يحول Dashboard إلى بنية عرض
 * أكثر ملاءمة للشاشة أو التقرير.
 */
class GetAnalyticsData @Inject constructor() {

    operator fun invoke(dashboard: Dashboard?): AnalyticsData {
        if (dashboard == null) {
            return AnalyticsData()
        }

        val metrics = buildMetrics(dashboard)
        val alerts = dashboard.alerts
        val summary = buildSummary(dashboard, metrics, alerts)

        return AnalyticsData(
            summary = summary,
            metrics = metrics,
            alerts = alerts,
            kpis = metrics.take(6),
            riskAlerts = alerts.filter { it.level != DashboardAlertLevel.Info },
            salesTrend = buildTrendSeries(
                current = dashboard.sales.todayRevenue,
                monthly = dashboard.sales.monthRevenue,
                positiveLabel = "إيرادات",
                negativeLabel = "انخفاض"
            ),
            inventoryTrend = buildTrendSeries(
                current = dashboard.inventory.productsCount.toBigDecimal(),
                monthly = dashboard.inventory.lowStockCount.toBigDecimal(),
                positiveLabel = "متاح",
                negativeLabel = "منخفض"
            ),
            customerTrend = buildTrendSeries(
                current = dashboard.customers.customersCount.toBigDecimal(),
                monthly = dashboard.customers.newCustomersCount.toBigDecimal(),
                positiveLabel = "عملاء",
                negativeLabel = "جدد"
            )
        )
    }

    private fun buildSummary(
        dashboard: Dashboard,
        metrics: List<DashboardMetric>,
        alerts: List<DashboardAlert>
    ): AnalyticsSummary {
        val totalPositive = metrics.count { it.isPositiveTrend }
        val totalNegative = metrics.count { !it.isPositiveTrend }
        val highPriorityAlerts = alerts.count {
            it.level == DashboardAlertLevel.Warning || it.level == DashboardAlertLevel.Critical
        }

        return AnalyticsSummary(
            title = dashboard.overview.title,
            subtitle = dashboard.overview.subtitle,
            totalMetrics = metrics.size,
            positiveMetrics = totalPositive,
            negativeMetrics = totalNegative,
            totalAlerts = alerts.size,
            criticalAlerts = highPriorityAlerts,
            hasSalesGrowth = dashboard.sales.growthRatePercent?.let { it > BigDecimal.ZERO } == true,
            hasStockRisk = dashboard.inventory.hasStockRisk,
            hasProfitData = dashboard.financial.hasProfitData,
            currencyCode = dashboard.financial.currencyCode
        )
    }

    private fun buildMetrics(dashboard: Dashboard): List<DashboardMetric> {
        return buildList {
            add(
                DashboardMetric(
                    key = "today_sales_count",
                    label = "مبيعات اليوم",
                    value = dashboard.sales.todaySalesCount.toBigDecimal(),
                    formattedValue = dashboard.sales.todaySalesCount.toString(),
                    deltaPercent = dashboard.sales.growthRatePercent,
                    deltaLabel = dashboard.sales.growthRatePercent?.let { "نمو" },
                    isPositiveTrend = (dashboard.sales.growthRatePercent ?: BigDecimal.ZERO) >= BigDecimal.ZERO,
                    accentKey = "sales"
                )
            )

            add(
                DashboardMetric(
                    key = "today_revenue",
                    label = "إيراد اليوم",
                    value = dashboard.sales.todayRevenue.money(),
                    formattedValue = dashboard.sales.todayRevenue.money().toPlainString(),
                    isPositiveTrend = true,
                    accentKey = "revenue"
                )
            )

            add(
                DashboardMetric(
                    key = "month_revenue",
                    label = "إيراد الشهر",
                    value = dashboard.sales.monthRevenue.money(),
                    formattedValue = dashboard.sales.monthRevenue.money().toPlainString(),
                    deltaPercent = dashboard.sales.growthRatePercent,
                    deltaLabel = dashboard.sales.growthRatePercent?.let { "مقارنة بالشهر السابق" },
                    isPositiveTrend = (dashboard.sales.growthRatePercent ?: BigDecimal.ZERO) >= BigDecimal.ZERO,
                    accentKey = "month_revenue"
                )
            )

            add(
                DashboardMetric(
                    key = "pending_invoices",
                    label = "فواتير معلقة",
                    value = dashboard.sales.pendingInvoicesCount.toBigDecimal(),
                    formattedValue = dashboard.sales.pendingInvoicesCount.toString(),
                    isPositiveTrend = false,
                    accentKey = "pending"
                )
            )

            add(
                DashboardMetric(
                    key = "low_stock_count",
                    label = "منخفض المخزون",
                    value = dashboard.inventory.lowStockCount.toBigDecimal(),
                    formattedValue = dashboard.inventory.lowStockCount.toString(),
                    isPositiveTrend = false,
                    accentKey = "low_stock"
                )
            )

            add(
                DashboardMetric(
                    key = "out_of_stock_count",
                    label = "نفد المخزون",
                    value = dashboard.inventory.outOfStockCount.toBigDecimal(),
                    formattedValue = dashboard.inventory.outOfStockCount.toString(),
                    isPositiveTrend = false,
                    accentKey = "out_of_stock"
                )
            )

            add(
                DashboardMetric(
                    key = "customers_count",
                    label = "العملاء",
                    value = dashboard.customers.customersCount.toBigDecimal(),
                    formattedValue = dashboard.customers.customersCount.toString(),
                    isPositiveTrend = true,
                    accentKey = "customers"
                )
            )

            add(
                DashboardMetric(
                    key = "net_balance",
                    label = "صافي الرصيد",
                    value = dashboard.financial.netBalance.money(),
                    formattedValue = dashboard.financial.netBalance.money().toPlainString(),
                    isPositiveTrend = dashboard.financial.netBalance >= BigDecimal.ZERO,
                    accentKey = "finance"
                )
            )

            dashboard.financial.profitEstimate?.let { profit ->
                add(
                    DashboardMetric(
                        key = "profit_estimate",
                        label = "الربح التقديري",
                        value = profit.money(),
                        formattedValue = profit.money().toPlainString(),
                        isPositiveTrend = profit >= BigDecimal.ZERO,
                        accentKey = "profit"
                    )
                )
            }
        }
    }

    private fun buildTrendSeries(
        current: BigDecimal,
        monthly: BigDecimal,
        positiveLabel: String,
        negativeLabel: String
    ): AnalyticsTrend {
        val safeCurrent = current.money()
        val safeMonthly = monthly.money()

        val changePercent = if (safeMonthly > BigDecimal.ZERO) {
            ((safeCurrent - safeMonthly) / safeMonthly * BigDecimal(100))
                .setScale(2, RoundingMode.HALF_UP)
        } else {
            BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
        }

        return AnalyticsTrend(
            current = safeCurrent,
            reference = safeMonthly,
            changePercent = changePercent,
            label = when {
                changePercent > BigDecimal.ZERO -> positiveLabel
                changePercent < BigDecimal.ZERO -> negativeLabel
                else -> "ثابت"
            },
            isPositive = changePercent >= BigDecimal.ZERO
        )
    }
}

data class AnalyticsData(
    val summary: AnalyticsSummary = AnalyticsSummary(),
    val metrics: List<DashboardMetric> = emptyList(),
    val kpis: List<DashboardMetric> = emptyList(),
    val alerts: List<DashboardAlert> = emptyList(),
    val riskAlerts: List<DashboardAlert> = emptyList(),
    val salesTrend: AnalyticsTrend = AnalyticsTrend(),
    val inventoryTrend: AnalyticsTrend = AnalyticsTrend(),
    val customerTrend: AnalyticsTrend = AnalyticsTrend()
) {
    val hasData: Boolean
        get() = metrics.isNotEmpty() || alerts.isNotEmpty()
}

data class AnalyticsSummary(
    val title: String = "لوحة التحليلات",
    val subtitle: String? = null,
    val totalMetrics: Int = 0,
    val positiveMetrics: Int = 0,
    val negativeMetrics: Int = 0,
    val totalAlerts: Int = 0,
    val criticalAlerts: Int = 0,
    val hasSalesGrowth: Boolean = false,
    val hasStockRisk: Boolean = false,
    val hasProfitData: Boolean = false,
    val currencyCode: String = "SDG"
)

data class AnalyticsTrend(
    val current: BigDecimal = BigDecimal.ZERO,
    val reference: BigDecimal = BigDecimal.ZERO,
    val changePercent: BigDecimal = BigDecimal.ZERO,
    val label: String = "ثابت",
    val isPositive: Boolean = true
)

private fun Int.toBigDecimal(): BigDecimal = BigDecimal.valueOf(toLong())

private fun BigDecimal.money(): BigDecimal = setScale(2, RoundingMode.HALF_UP)