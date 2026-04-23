package com.myimdad_por.domain.model

/**
 * Only two roles are used in this system:
 * - MANAGER
 * - EMPLOYEE
 */
enum class Role(
    val displayName: String,
    private val basePermissions: Set<Permission>,
    private val fullAccess: Boolean
) {
    MANAGER(
        displayName = "مدير",
        basePermissions = emptySet(),
        fullAccess = true
    ),
    EMPLOYEE(
        displayName = "موظف",
        basePermissions = setOf(
            Permission.VIEW_DASHBOARD,
            Permission.MANAGE_SALES,
            Permission.MANAGE_INVENTORY,
            Permission.MANAGE_CUSTOMERS,
            Permission.MANAGE_SUPPLIERS,
            Permission.MANAGE_RETURNS,
            Permission.MANAGE_EXPENSES,
            Permission.VIEW_REPORTS,
            Permission.ACCESS_HARDWARE,
            Permission.MANAGE_PRINTER,
            Permission.MANAGE_SCANNER,
            Permission.CHANGE_PASSWORD
        ),
        fullAccess = false
    );

    val defaultPermissions: Set<Permission>
        get() = if (fullAccess) Permission.values().toSet() else basePermissions

    fun hasPermission(permission: Permission): Boolean {
        return fullAccess || permission in basePermissions
    }

    fun canManageUsers(): Boolean {
        return hasPermission(Permission.MANAGE_USERS)
    }

    fun canSeeAuditLogs(): Boolean {
        return hasPermission(Permission.VIEW_AUDIT_LOGS)
    }

    fun canApprovePayments(): Boolean {
        return hasPermission(Permission.APPROVE_PAYMENT)
    }

    fun isManager(): Boolean = this == MANAGER
    fun isEmployee(): Boolean = this == EMPLOYEE
}