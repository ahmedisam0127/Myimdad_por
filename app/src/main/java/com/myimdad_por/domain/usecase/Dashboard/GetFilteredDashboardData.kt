package com.myimdad_por.domain.usecase.dashboard

import com.myimdad_por.domain.model.Dashboard
import com.myimdad_por.domain.model.DashboardAlert
import com.myimdad_por.domain.model.DashboardAlertLevel
import com.myimdad_por.domain.model.DashboardCustomerSummary
import com.myimdad_por.domain.model.DashboardFinancialSummary
import com.myimdad_por.domain.model.DashboardInventorySummary
import com.myimdad_por.domain.model.DashboardOverview
import com.myimdad_por.domain.model.DashboardQuickAction
import com.myimdad_por.domain.model.DashboardSalesSummary

/**
 * فلترة بيانات لوحة التحكم بحسب المعايير المطلوبة.
 *
 * الاستخدام النموذجي:
 * - إخفاء بعض الأقسام
 * - إظهار تنبيهات محددة فقط
 * - تقليص قائمة الإجراءات السريعة
 * - البحث النصي داخل التنبيهات أو الإجراءات
 */
class GetFilteredDashboardData {

    operator fun invoke(
        dashboard: Dashboard?,
        filter: DashboardDataFilter = DashboardDataFilter()
    ): Dashboard {
        if (dashboard == null) return Dashboard()

        return dashboard.copy(
            overview = if (filter.includeOverview) dashboard.overview else DashboardOverview(),
            sales = if (filter.includeSales) dashboard.sales else DashboardSalesSummary(),
            inventory = if (filter.includeInventory) dashboard.inventory else DashboardInventorySummary(),
            customers = if (filter.includeCustomers) dashboard.customers else DashboardCustomerSummary(),
            financial = if (filter.includeFinancial) {
                dashboard.financial
            } else {
                DashboardFinancialSummary(currencyCode = dashboard.financial.currencyCode)
            },
            alerts = if (filter.includeAlerts) {
                dashboard.alerts.filterAlerts(filter)
            } else {
                emptyList()
            },
            quickActions = if (filter.includeQuickActions) {
                dashboard.quickActions.filterQuickActions(filter)
            } else {
                emptyList()
            },
            lastUpdatedAtEpochMillis = if (filter.includeLastUpdatedAt) {
                dashboard.lastUpdatedAtEpochMillis
            } else {
                null
            }
        )
    }

    private fun List<DashboardAlert>.filterAlerts(filter: DashboardDataFilter): List<DashboardAlert> {
        return asSequence()
            .filter { alert ->
                filter.allowedAlertLevels.isNullOrEmpty() || alert.level in filter.allowedAlertLevels
            }
            .filter { alert ->
                filter.allowedAlertIds.isNullOrEmpty() || alert.id in filter.allowedAlertIds
            }
            .filter { alert ->
                filter.searchQuery.isNullOrBlank() ||
                    alert.title.contains(filter.searchQuery, ignoreCase = true) ||
                    alert.message.contains(filter.searchQuery, ignoreCase = true)
            }
            .takeIf { filter.maxAlertsCount != null }
            ?.take(filter.maxAlertsCount!!)
            ?.toList()
            ?: toList()
    }

    private fun List<DashboardQuickAction>.filterQuickActions(
        filter: DashboardDataFilter
    ): List<DashboardQuickAction> {
        return asSequence()
            .filter { action ->
                filter.allowedQuickActionIds.isNullOrEmpty() || action.id in filter.allowedQuickActionIds
            }
            .filter { action ->
                filter.searchQuery.isNullOrBlank() ||
                    action.title.contains(filter.searchQuery, ignoreCase = true) ||
                    (action.description?.contains(filter.searchQuery, ignoreCase = true) == true)
            }
            .filter { action ->
                filter.onlyNavigableQuickActions.not() || action.canNavigate
            }
            .takeIf { filter.maxQuickActionsCount != null }
            ?.take(filter.maxQuickActionsCount!!)
            ?.toList()
            ?: toList()
    }
}

/**
 * معايير فلترة بيانات لوحة التحكم.
 */
data class DashboardDataFilter(
    val includeOverview: Boolean = true,
    val includeSales: Boolean = true,
    val includeInventory: Boolean = true,
    val includeCustomers: Boolean = true,
    val includeFinancial: Boolean = true,
    val includeAlerts: Boolean = true,
    val includeQuickActions: Boolean = true,
    val includeLastUpdatedAt: Boolean = true,
    val allowedAlertLevels: Set<DashboardAlertLevel>? = null,
    val allowedAlertIds: Set<String> = emptySet(),
    val allowedQuickActionIds: Set<String> = emptySet(),
    val searchQuery: String? = null,
    val maxAlertsCount: Int? = null,
    val maxQuickActionsCount: Int? = null,
    val onlyNavigableQuickActions: Boolean = false
)