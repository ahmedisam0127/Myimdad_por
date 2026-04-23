package com.myimdad_por.domain.usecase.dashboard

import com.myimdad_por.domain.model.Dashboard
import com.myimdad_por.domain.model.DashboardMetric
import com.myimdad_por.domain.model.money
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * يحول بيانات لوحة التحكم إلى مؤشرات أداء رئيسية (KPIs) جاهزة للعرض.
 *
 * الفكرة من هذا الـ use case هي إبقاء منطق الحساب خارج طبقة الـ UI.
 */
class CalculateKpis {

    operator fun invoke(dashboard: Dashboard?): List<DashboardMetric> {
        if (dashboard == null) return emptyList()

        val sales = dashboard.sales
        val inventory = dashboard.inventory
        val customers = dashboard.customers
        val financial = dashboard.financial

        val metrics = buildList {
            add(
                DashboardMetric(
                    key = "today_sales_count",
                    label = "مبيعات اليوم",
                    value = dashboard.sales.todaySalesCount.toBigDecimal(),
                    formattedValue = dashboard.sales.todaySalesCount.toString(),
                    deltaPercent = dashboard.sales.growthRatePercent,
                    deltaLabel = dashboard.sales.growthRatePercent?.let { "نمو المبيعات" },
                    isPositiveTrend = (dashboard.sales.growthRatePercent ?: BigDecimal.ZERO) >= BigDecimal.ZERO,
                    accentKey = "sales"
                )
            )

            add(
                DashboardMetric(
                    key = "today_revenue",
                    label = "إيراد اليوم",
                    value = sales.todayRevenue.money(),
                    formattedValue = sales.todayRevenue.money().toPlainString(),
                    deltaPercent = null,
                    deltaLabel = null,
                    isPositiveTrend = true,
                    accentKey = "revenue"
                )
            )

            add(
                DashboardMetric(
                    key = "month_revenue",
                    label = "إيراد الشهر",
                    value = sales.monthRevenue.money(),
                    formattedValue = sales.monthRevenue.money().toPlainString(),
                    deltaPercent = sales.growthRatePercent,
                    deltaLabel = sales.growthRatePercent?.let { "مقارنة بالشهر السابق" },
                    isPositiveTrend = (sales.growthRatePercent ?: BigDecimal.ZERO) >= BigDecimal.ZERO,
                    accentKey = "month_revenue"
                )
            )

            add(
                DashboardMetric(
                    key = "pending_invoices",
                    label = "فواتير معلقة",
                    value = sales.pendingInvoicesCount.toBigDecimal(),
                    formattedValue = sales.pendingInvoicesCount.toString(),
                    deltaPercent = null,
                    deltaLabel = null,
                    isPositiveTrend = false,
                    accentKey = "pending"
                )
            )

            add(
                DashboardMetric(
                    key = "returns_count",
                    label = "المرتجعات",
                    value = sales.returnsCount.toBigDecimal(),
                    formattedValue = sales.returnsCount.toString(),
                    deltaPercent = null,
                    deltaLabel = null,
                    isPositiveTrend = false,
                    accentKey = "returns"
                )
            )

            add(
                DashboardMetric(
                    key = "products_count",
                    label = "عدد المنتجات",
                    value = inventory.productsCount.toBigDecimal(),
                    formattedValue = inventory.productsCount.toString(),
                    deltaPercent = null,
                    deltaLabel = null,
                    isPositiveTrend = true,
                    accentKey = "inventory"
                )
            )

            add(
                DashboardMetric(
                    key = "low_stock_count",
                    label = "منخفض المخزون",
                    value = inventory.lowStockCount.toBigDecimal(),
                    formattedValue = inventory.lowStockCount.toString(),
                    deltaPercent = null,
                    deltaLabel = null,
                    isPositiveTrend = false,
                    accentKey = "low_stock"
                )
            )

            add(
                DashboardMetric(
                    key = "out_of_stock_count",
                    label = "نفد المخزون",
                    value = inventory.outOfStockCount.toBigDecimal(),
                    formattedValue = inventory.outOfStockCount.toString(),
                    deltaPercent = null,
                    deltaLabel = null,
                    isPositiveTrend = false,
                    accentKey = "out_of_stock"
                )
            )

            add(
                DashboardMetric(
                    key = "customers_count",
                    label = "العملاء",
                    value = customers.customersCount.toBigDecimal(),
                    formattedValue = customers.customersCount.toString(),
                    deltaPercent = null,
                    deltaLabel = null,
                    isPositiveTrend = true,
                    accentKey = "customers"
                )
            )

            add(
                DashboardMetric(
                    key = "new_customers_count",
                    label = "عملاء جدد",
                    value = customers.newCustomersCount.toBigDecimal(),
                    formattedValue = customers.newCustomersCount.toString(),
                    deltaPercent = null,
                    deltaLabel = null,
                    isPositiveTrend = true,
                    accentKey = "new_customers"
                )
            )

            add(
                DashboardMetric(
                    key = "net_balance",
                    label = "صافي الرصيد",
                    value = financial.netBalance.money(),
                    formattedValue = financial.netBalance.money().toPlainString(),
                    deltaPercent = null,
                    deltaLabel = null,
                    isPositiveTrend = financial.netBalance >= BigDecimal.ZERO,
                    accentKey = "finance"
                )
            )

            add(
                DashboardMetric(
                    key = "receivables",
                    label = "الذمم المدينة",
                    value = financial.receivables.money(),
                    formattedValue = financial.receivables.money().toPlainString(),
                    deltaPercent = null,
                    deltaLabel = null,
                    isPositiveTrend = false,
                    accentKey = "receivables"
                )
            )

            add(
                DashboardMetric(
                    key = "payables",
                    label = "الذمم الدائنة",
                    value = financial.payables.money(),
                    formattedValue = financial.payables.money().toPlainString(),
                    deltaPercent = null,
                    deltaLabel = null,
                    isPositiveTrend = false,
                    accentKey = "payables"
                )
            )

            financial.profitEstimate?.let { profit ->
                add(
                    DashboardMetric(
                        key = "profit_estimate",
                        label = "الربح التقديري",
                        value = profit.money(),
                        formattedValue = profit.money().toPlainString(),
                        deltaPercent = null,
                        deltaLabel = null,
                        isPositiveTrend = profit >= BigDecimal.ZERO,
                        accentKey = "profit"
                    )
                )
            }
        }

        return metrics.sortedWith(
            compareBy<DashboardMetric> { metricPriority(it.key) }
                .thenBy { it.label }
        )
    }

    private fun metricPriority(key: String): Int {
        return when (key) {
            "today_sales_count" -> 0
            "today_revenue" -> 1
            "month_revenue" -> 2
            "pending_invoices" -> 3
            "returns_count" -> 4
            "products_count" -> 5
            "low_stock_count" -> 6
            "out_of_stock_count" -> 7
            "customers_count" -> 8
            "new_customers_count" -> 9
            "net_balance" -> 10
            "receivables" -> 11
            "payables" -> 12
            "profit_estimate" -> 13
            else -> 99
        }
    }
}

private fun Int.toBigDecimal(): BigDecimal = BigDecimal.valueOf(toLong())
