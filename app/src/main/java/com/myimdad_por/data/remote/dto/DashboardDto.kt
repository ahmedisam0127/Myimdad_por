package com.myimdad_por.data.remote.dto

import com.google.gson.annotations.SerializedName

data class DashboardDto(
    @SerializedName("overview")
    val overview: DashboardOverviewDto? = null,

    @SerializedName("sales")
    val sales: DashboardSalesSummaryDto? = null,

    @SerializedName("inventory")
    val inventory: DashboardInventorySummaryDto? = null,

    @SerializedName("customers")
    val customers: DashboardCustomerSummaryDto? = null,

    @SerializedName("financial")
    val financial: DashboardFinancialSummaryDto? = null,

    @SerializedName("alerts")
    val alerts: List<DashboardAlertDto>? = null,

    @SerializedName("quick_actions")
    val quickActions: List<DashboardQuickActionDto>? = null,

    @SerializedName("last_updated_at_epoch_millis")
    val lastUpdatedAtEpochMillis: Long? = null
)

data class DashboardSummaryDto(
    @SerializedName("overview")
    val overview: DashboardOverviewDto? = null,

    @SerializedName("sales")
    val sales: DashboardSalesSummaryDto? = null,

    @SerializedName("customers")
    val customers: DashboardCustomerSummaryDto? = null,

    @SerializedName("financial")
    val financial: DashboardFinancialSummaryDto? = null,

    @SerializedName("alerts")
    val alerts: List<DashboardAlertDto>? = null,

    @SerializedName("last_updated_at_epoch_millis")
    val lastUpdatedAtEpochMillis: Long? = null
)

data class DashboardAnalyticsDto(
    @SerializedName("overview")
    val overview: DashboardOverviewDto? = null,

    @SerializedName("sales")
    val sales: DashboardSalesSummaryDto? = null,

    @SerializedName("financial")
    val financial: DashboardFinancialSummaryDto? = null,

    @SerializedName("trend_points")
    val trendPoints: List<DashboardTrendPointDto>? = null,

    @SerializedName("last_updated_at_epoch_millis")
    val lastUpdatedAtEpochMillis: Long? = null
)

data class DashboardTrendPointDto(
    @SerializedName("label")
    val label: String? = null,

    @SerializedName("value")
    val value: String? = null,

    @SerializedName("timestamp_epoch_millis")
    val timestampEpochMillis: Long? = null
)

data class DashboardOverviewDto(
    @SerializedName("title")
    val title: String? = null,

    @SerializedName("subtitle")
    val subtitle: String? = null,

    @SerializedName("greeting")
    val greeting: String? = null,

    @SerializedName("total_metrics")
    val totalMetrics: Int? = null,

    @SerializedName("positive_metrics")
    val positiveMetrics: Int? = null,

    @SerializedName("negative_metrics")
    val negativeMetrics: Int? = null
)

data class DashboardSalesSummaryDto(
    @SerializedName("today_sales_count")
    val todaySalesCount: Int? = null,

    @SerializedName("today_revenue")
    val todayRevenue: String? = null,

    @SerializedName("month_sales_count")
    val monthSalesCount: Int? = null,

    @SerializedName("month_revenue")
    val monthRevenue: String? = null,

    @SerializedName("pending_invoices_count")
    val pendingInvoicesCount: Int? = null,

    @SerializedName("returns_count")
    val returnsCount: Int? = null,

    @SerializedName("top_selling_product_name")
    val topSellingProductName: String? = null,

    @SerializedName("growth_rate_percent")
    val growthRatePercent: String? = null
)

data class DashboardInventorySummaryDto(
    @SerializedName("products_count")
    val productsCount: Int? = null,

    @SerializedName("low_stock_count")
    val lowStockCount: Int? = null,

    @SerializedName("out_of_stock_count")
    val outOfStockCount: Int? = null,

    @SerializedName("total_stock_value")
    val totalStockValue: String? = null,

    @SerializedName("reserved_items_count")
    val reservedItemsCount: Int? = null,

    @SerializedName("most_critical_product_name")
    val mostCriticalProductName: String? = null
)

data class DashboardCustomerSummaryDto(
    @SerializedName("customers_count")
    val customersCount: Int? = null,

    @SerializedName("new_customers_count")
    val newCustomersCount: Int? = null,

    @SerializedName("active_customers_count")
    val activeCustomersCount: Int? = null,

    @SerializedName("due_customers_count")
    val dueCustomersCount: Int? = null,

    @SerializedName("top_customer_name")
    val topCustomerName: String? = null,

    @SerializedName("average_order_value")
    val averageOrderValue: String? = null
)

data class DashboardFinancialSummaryDto(
    @SerializedName("total_cash_in")
    val totalCashIn: String? = null,

    @SerializedName("total_cash_out")
    val totalCashOut: String? = null,

    @SerializedName("net_balance")
    val netBalance: String? = null,

    @SerializedName("receivables")
    val receivables: String? = null,

    @SerializedName("payables")
    val payables: String? = null,

    @SerializedName("profit_estimate")
    val profitEstimate: String? = null,

    @SerializedName("currency_code")
    val currencyCode: String? = null
)

data class DashboardAlertDto(
    @SerializedName("id")
    val id: String? = null,

    @SerializedName("title")
    val title: String? = null,

    @SerializedName("message")
    val message: String? = null,

    @SerializedName("level")
    val level: String? = null,

    @SerializedName("action_label")
    val actionLabel: String? = null,

    @SerializedName("target_route")
    val targetRoute: String? = null,

    @SerializedName("is_dismissible")
    val isDismissible: Boolean? = null
)

data class DashboardQuickActionDto(
    @SerializedName("id")
    val id: String? = null,

    @SerializedName("title")
    val title: String? = null,

    @SerializedName("description")
    val description: String? = null,

    @SerializedName("icon_name")
    val iconName: String? = null,

    @SerializedName("route")
    val route: String? = null,

    @SerializedName("is_enabled")
    val isEnabled: Boolean? = null,

    @SerializedName("requires_permission")
    val requiresPermission: String? = null
)