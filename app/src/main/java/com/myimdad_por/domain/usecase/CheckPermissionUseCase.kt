package com.myimdad_por.domain.usecase

import com.myimdad_por.domain.model.Permission
import com.myimdad_por.domain.model.User
import javax.inject.Inject

data class PermissionContext(
    val shiftOpen: Boolean = true,
    val registerOpen: Boolean = true,
    val subscription: SubscriptionContext = SubscriptionContext()
)

data class SubscriptionContext(
    val planName: String = "basic",
    val maxWarehousesAllowed: Int = 1,
    val currentWarehouseCount: Int = 1,
    val canViewReports: Boolean = true,
    val canUseHardware: Boolean = true,
    val canManageMultipleBranches: Boolean = false
) {
    init {
        require(maxWarehousesAllowed > 0) { "maxWarehousesAllowed must be greater than zero" }
        require(currentWarehouseCount >= 0) { "currentWarehouseCount cannot be negative" }
    }
}

data class PermissionDecision(
    val allowed: Boolean,
    val permission: Permission,
    val reasons: List<String> = emptyList()
) {
    val isDenied: Boolean
        get() = !allowed
}

/**
 * Hilt-friendly use case:
 * - لا يُنشئ أي dependency يدويًا
 * - يمكن حقنه مباشرة في ViewModel أو Use Case آخر
 */
class CheckPermissionUseCase @Inject constructor() {

    operator fun invoke(
        user: User?,
        permission: Permission,
        context: PermissionContext = PermissionContext()
    ): PermissionDecision {
        val reasons = mutableListOf<String>()

        if (user == null) {
            reasons += "المستخدم غير موجود"
            return PermissionDecision(
                allowed = false,
                permission = permission,
                reasons = reasons
            )
        }

        if (!user.isActive) {
            reasons += "الحساب غير نشط"
        }

        if (!user.hasPermission(permission)) {
            reasons += "لا يملك الصلاحية المطلوبة: ${permission.label}"
        }

        if (!context.subscription.canUseHardware &&
            permission in hardwarePermissions
        ) {
            reasons += "الاشتراك الحالي لا يسمح باستخدام العتاد"
        }

        if (!context.subscription.canViewReports &&
            permission == Permission.VIEW_REPORTS
        ) {
            reasons += "الاشتراك الحالي لا يسمح بعرض التقارير"
        }

        if (context.subscription.currentWarehouseCount > context.subscription.maxWarehousesAllowed &&
            permission == Permission.MANAGE_INVENTORY
        ) {
            reasons += "تجاوزت الباقة الحالية الحد المسموح للمخازن"
        }

        if (!context.shiftOpen && permission in shiftRequiredPermissions) {
            reasons += "الوردية مغلقة"
        }

        if (!context.registerOpen && permission in registerRequiredPermissions) {
            reasons += "الصندوق مغلق"
        }

        if (permission == Permission.MANAGE_USERS && !user.role.isManager()) {
            reasons += "إدارة المستخدمين تتطلب رتبة أعلى"
        }

        if (permission == Permission.MANAGE_SETTINGS && !user.role.isManager()) {
            reasons += "إدارة الإعدادات تتطلب رتبة أعلى"
        }

        if (permission == Permission.VIEW_AUDIT_LOGS && !user.role.isManager()) {
            reasons += "عرض سجل التدقيق يتطلب رتبة أعلى"
        }

        return PermissionDecision(
            allowed = reasons.isEmpty(),
            permission = permission,
            reasons = reasons
        )
    }

    fun canAccessAll(
        user: User?,
        permissions: Set<Permission>,
        context: PermissionContext = PermissionContext()
    ): PermissionDecision {
        val deniedReasons = mutableListOf<String>()

        permissions.forEach { permission ->
            val decision = invoke(user = user, permission = permission, context = context)
            if (decision.isDenied) {
                deniedReasons += decision.reasons.map { "$permission: $it" }
            }
        }

        return PermissionDecision(
            allowed = deniedReasons.isEmpty(),
            permission = permissions.firstOrNull() ?: Permission.VIEW_DASHBOARD,
            reasons = deniedReasons
        )
    }

    private val shiftRequiredPermissions = setOf(
        Permission.MANAGE_SALES,
        Permission.MANAGE_PURCHASES,
        Permission.MANAGE_RETURNS,
        Permission.MANAGE_EXPENSES,
        Permission.MANAGE_PAYMENTS,
        Permission.PROCESS_REFUND,
        Permission.APPROVE_PAYMENT
    )

    private val registerRequiredPermissions = setOf(
        Permission.MANAGE_SALES,
        Permission.MANAGE_PAYMENTS,
        Permission.PROCESS_REFUND,
        Permission.APPROVE_PAYMENT
    )

    private val hardwarePermissions = setOf(
        Permission.ACCESS_HARDWARE,
        Permission.MANAGE_PRINTER,
        Permission.MANAGE_SCANNER
    )
}