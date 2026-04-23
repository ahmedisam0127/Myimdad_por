package com.myimdad_por.data.mapper

import com.myimdad_por.core.utils.roundTo
import com.myimdad_por.core.utils.toBigDecimalOrZero
import com.myimdad_por.data.remote.dto.DashboardAlertDto
import com.myimdad_por.data.remote.dto.DashboardAnalyticsDto
import com.myimdad_por.data.remote.dto.DashboardCustomerSummaryDto
import com.myimdad_por.data.remote.dto.DashboardDto
import com.myimdad_por.data.remote.dto.DashboardFinancialSummaryDto
import com.myimdad_por.data.remote.dto.DashboardInventorySummaryDto
import com.myimdad_por.data.remote.dto.DashboardOverviewDto
import com.myimdad_por.data.remote.dto.DashboardQuickActionDto
import com.myimdad_por.data.remote.dto.DashboardSalesSummaryDto
import com.myimdad_por.data.remote.dto.DashboardSummaryDto
import com.myimdad_por.domain.model.Dashboard
import com.myimdad_por.domain.model.DashboardAlert
import com.myimdad_por.domain.model.DashboardAlertLevel
import com.myimdad_por.domain.model.DashboardCustomerSummary
import com.myimdad_por.domain.model.DashboardFinancialSummary
import com.myimdad_por.domain.model.DashboardInventorySummary
import com.myimdad_por.domain.model.DashboardOverview
import com.myimdad_por.domain.model.DashboardQuickAction
import com.myimdad_por.domain.model.DashboardSalesSummary
import java.math.BigDecimal
import java.util.Locale

fun DashboardDto.toDomain(): Dashboard {
    return Dashboard(
        overview = overview.toDomain(),
        sales = sales.toDomain(),
        inventory = inventory.toDomain(),
        customers = customers.toDomain(),
        financial = financial.toDomain(),
        alerts = alerts.orEmpty().map { it.toDomain() },
        quickActions = quickActions.orEmpty().map { it.toDomain() },
        lastUpdatedAtEpochMillis = lastUpdatedAtEpochMillis
    )
}

fun DashboardSummaryDto.toDomain(): Dashboard {
    return Dashboard(
        overview = overview.toDomain(),
        sales = sales.toDomain(),
        customers = customers.toDomain(),
        financial = financial.toDomain(),
        alerts = alerts.orEmpty().map { it.toDomain() },
        lastUpdatedAtEpochMillis = lastUpdatedAtEpochMillis
    )
}

fun DashboardAnalyticsDto.toDomain(): Dashboard {
    return Dashboard(
        overview = overview.toDomain(),
        sales = sales.toDomain(),
        financial = financial.toDomain(),
        lastUpdatedAtEpochMillis = lastUpdatedAtEpochMillis
    )
}

fun DashboardOverviewDto?.toDomain(): DashboardOverview {
    return DashboardOverview(
        title = this?.title?.takeIf { it.isNotBlank() } ?: "لوحة التحكم",
        subtitle = this?.subtitle,
        greeting = this?.greeting,
        totalMetrics = this?.totalMetrics.orZero(),
        positiveMetrics = this?.positiveMetrics.orZero(),
        negativeMetrics = this?.negativeMetrics.orZero()
    )
}

fun DashboardSalesSummaryDto?.toDomain(): DashboardSalesSummary {
    return DashboardSalesSummary(
        todaySalesCount = this?.todaySalesCount.orZero(),
        todayRevenue = this?.todayRevenue.toBigDecimalOrZero(),
        monthSalesCount = this?.monthSalesCount.orZero(),
        monthRevenue = this?.monthRevenue.toBigDecimalOrZero(),
        pendingInvoicesCount = this?.pendingInvoicesCount.orZero(),
        returnsCount = this?.returnsCount.orZero(),
        topSellingProductName = this?.topSellingProductName,
        growthRatePercent = this?.growthRatePercent.toBigDecimalOrZero().takeIf { this?.growthRatePercent != null }
    )
}

fun DashboardInventorySummaryDto?.toDomain(): DashboardInventorySummary {
    return DashboardInventorySummary(
        productsCount = this?.productsCount.orZero(),
        lowStockCount = this?.lowStockCount.orZero(),
        outOfStockCount = this?.outOfStockCount.orZero(),
        totalStockValue = this?.totalStockValue.toBigDecimalOrZero(),
        reservedItemsCount = this?.reservedItemsCount.orZero(),
        mostCriticalProductName = this?.mostCriticalProductName
    )
}

fun DashboardCustomerSummaryDto?.toDomain(): DashboardCustomerSummary {
    return DashboardCustomerSummary(
        customersCount = this?.customersCount.orZero(),
        newCustomersCount = this?.newCustomersCount.orZero(),
        activeCustomersCount = this?.activeCustomersCount.orZero(),
        dueCustomersCount = this?.dueCustomersCount.orZero(),
        topCustomerName = this?.topCustomerName,
        averageOrderValue = this?.averageOrderValue.toBigDecimalOrZero()
    )
}

fun DashboardFinancialSummaryDto?.toDomain(): DashboardFinancialSummary {
    return DashboardFinancialSummary(
        totalCashIn = this?.totalCashIn.toBigDecimalOrZero(),
        totalCashOut = this?.totalCashOut.toBigDecimalOrZero(),
        netBalance = this?.netBalance.toBigDecimalOrZero(),
        receivables = this?.receivables.toBigDecimalOrZero(),
        payables = this?.payables.toBigDecimalOrZero(),
        profitEstimate = this?.profitEstimate?.toBigDecimalOrZero(),
        currencyCode = this?.currencyCode?.takeIf { it.isNotBlank() } ?: "---"
    )
}

fun DashboardAlertDto.toDomain(): DashboardAlert {
    return DashboardAlert(
        id = id.orEmpty(),
        title = title.orEmpty(),
        message = message.orEmpty(),
        level = level.toDomainAlertLevel(),
        actionLabel = actionLabel,
        targetRoute = targetRoute,
        isDismissible = isDismissible ?: true
    )
}

fun DashboardQuickActionDto.toDomain(): DashboardQuickAction {
    return DashboardQuickAction(
        id = id.orEmpty(),
        title = title.orEmpty(),
        description = description,
        iconName = iconName,
        route = route,
        isEnabled = isEnabled ?: true,
        requiresPermission = requiresPermission
    )
}

private fun String?.toDomainAlertLevel(): DashboardAlertLevel {
    return when (this?.trim()?.lowercase(Locale.ROOT)) {
        "warning", "warn" -> DashboardAlertLevel.Warning
        "critical", "danger", "error" -> DashboardAlertLevel.Critical
        else -> DashboardAlertLevel.Info
    }
}

private fun Int?.orZero(): Int = this ?: 0

private fun BigDecimal?.asApiString(scale: Int = 2): String? {
    return this?.roundTo(scale)?.toPlainString()
}