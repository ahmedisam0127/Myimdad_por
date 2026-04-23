package com.myimdad_por.domain.usecase.dashboard

import com.myimdad_por.domain.model.Dashboard
import com.myimdad_por.domain.model.DashboardQuickAction

/**
 * يحدد ما إذا كانت لوحة التحكم قابلة للوصول، وما هي الأجزاء/الإجراءات المتاحة داخلها.
 *
 * الفكرة هنا أن طبقة الـ UI أو الـ ViewModel لا تضطر إلى تنفيذ منطق الصلاحيات بنفسها.
 * يمكن لاحقًا ربط هذا الاستخدام بمصدر صلاحيات حقيقي مثل:
 * - المستخدم الحالي
 * - الدور الوظيفي
 * - الفروع / المخازن المسموح بها
 * - حالة الترخيص
 */
class CheckDashboardAccess {

    operator fun invoke(
        dashboard: Dashboard?,
        permissions: DashboardPermissions = DashboardPermissions()
    ): DashboardAccessResult {
        if (dashboard == null) {
            return DashboardAccessResult(
                canAccessDashboard = false,
                allowedQuickActions = emptyList(),
                deniedQuickActions = emptyList(),
                reason = "لا توجد بيانات لوحة تحكم متاحة"
            )
        }

        val allowedActions = dashboard.quickActions.filter { action ->
            action.isEnabled && isActionAllowed(action, permissions)
        }

        val deniedActions = dashboard.quickActions.filterNot { action ->
            action.isEnabled && isActionAllowed(action, permissions)
        }

        val canAccessDashboard = permissions.canViewDashboard

        return DashboardAccessResult(
            canAccessDashboard = canAccessDashboard,
            allowedQuickActions = allowedActions,
            deniedQuickActions = deniedActions,
            reason = when {
                !canAccessDashboard -> "ليس لديك صلاحية عرض لوحة التحكم"
                dashboard.quickActions.isEmpty() -> "لوحة التحكم متاحة لكن لا توجد إجراءات سريعة"
                else -> null
            }
        )
    }

    private fun isActionAllowed(
        action: DashboardQuickAction,
        permissions: DashboardPermissions
    ): Boolean {
        val requiredPermission = action.requiresPermission?.trim().orEmpty()
        return when {
            requiredPermission.isBlank() -> true
            requiredPermission.equals(PermissionNames.VIEW_SALES, ignoreCase = true) -> permissions.canViewSales
            requiredPermission.equals(PermissionNames.CREATE_SALES, ignoreCase = true) -> permissions.canCreateSales
            requiredPermission.equals(PermissionNames.VIEW_CUSTOMERS, ignoreCase = true) -> permissions.canViewCustomers
            requiredPermission.equals(PermissionNames.VIEW_INVENTORY, ignoreCase = true) -> permissions.canViewInventory
            requiredPermission.equals(PermissionNames.VIEW_REPORTS, ignoreCase = true) -> permissions.canViewReports
            requiredPermission.equals(PermissionNames.VIEW_FINANCE, ignoreCase = true) -> permissions.canViewFinance
            else -> permissions.customPermissions.contains(requiredPermission)
        }
    }
}

data class DashboardPermissions(
    val canViewDashboard: Boolean = true,
    val canViewSales: Boolean = true,
    val canCreateSales: Boolean = true,
    val canViewCustomers: Boolean = true,
    val canViewInventory: Boolean = true,
    val canViewReports: Boolean = true,
    val canViewFinance: Boolean = true,
    val customPermissions: Set<String> = emptySet()
)

data class DashboardAccessResult(
    val canAccessDashboard: Boolean,
    val allowedQuickActions: List<DashboardQuickAction>,
    val deniedQuickActions: List<DashboardQuickAction>,
    val reason: String? = null
) {
    val hasAllowedActions: Boolean
        get() = allowedQuickActions.isNotEmpty()

    val hasDeniedActions: Boolean
        get() = deniedQuickActions.isNotEmpty()
}

private object PermissionNames {
    const val VIEW_SALES = "VIEW_SALES"
    const val CREATE_SALES = "CREATE_SALES"
    const val VIEW_CUSTOMERS = "VIEW_CUSTOMERS"
    const val VIEW_INVENTORY = "VIEW_INVENTORY"
    const val VIEW_REPORTS = "VIEW_REPORTS"
    const val VIEW_FINANCE = "VIEW_FINANCE"
}
