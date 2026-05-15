package com.myimdad_por.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * كيان الكاش المحلي للوحة التحكم.
 *
 * تم تصميمه كصف واحد ثابت داخل الجدول عبر id = 0،
 * حتى يتم استبدال نفس السجل دائمًا عند التحديث.
 *
 * الحقول النقدية مخزنة كنصوص String للحفاظ على دقة BigDecimal
 * وتجنب أي فقدان في القيمة أثناء التخزين المحلي.
 */
@Entity(tableName = "dashboard_cache")
data class DashboardCacheEntity(
    @PrimaryKey val id: Int = 0,

    // Overview
    val overviewTitle: String = "لوحة التحكم",
    val overviewSubtitle: String? = null,
    val overviewGreeting: String? = null,
    val overviewTotalMetrics: Int = 0,
    val overviewPositiveMetrics: Int = 0,
    val overviewNegativeMetrics: Int = 0,

    // Sales
    val salesTodaySalesCount: Int = 0,
    val salesTodayRevenue: String = "0.00",
    val salesMonthSalesCount: Int = 0,
    val salesMonthRevenue: String = "0.00",
    val salesPendingInvoicesCount: Int = 0,
    val salesReturnsCount: Int = 0,
    val salesTopSellingProductName: String? = null,
    val salesGrowthRatePercent: String? = null,

    // Inventory
    val inventoryProductsCount: Int = 0,
    val inventoryLowStockCount: Int = 0,
    val inventoryOutOfStockCount: Int = 0,
    val inventoryTotalStockValue: String = "0.00",
    val inventoryReservedItemsCount: Int = 0,
    val inventoryMostCriticalProductName: String? = null,

    // Customers
    val customersCount: Int = 0,
    val customersNewCustomersCount: Int = 0,
    val customersActiveCustomersCount: Int = 0,
    val customersDueCustomersCount: Int = 0,
    val customersTopCustomerName: String? = null,
    val customersAverageOrderValue: String = "0.00",

    // Financial
    val financialTotalCashIn: String = "0.00",
    val financialTotalCashOut: String = "0.00",
    val financialNetBalance: String = "0.00",
    val financialReceivables: String = "0.00",
    val financialPayables: String = "0.00",
    val financialProfitEstimate: String? = null,
    val financialCurrencyCode: String = "SDG",

    // Collections serialized as JSON
    val alertsJson: String = "[]",
    val quickActionsJson: String = "[]",

    // Meta
    val lastUpdatedAtEpochMillis: Long? = null
)