package com.myimdad_por.domain.model

import java.math.BigDecimal
import java.util.UUID

/**
 * Represents an employee profile in the domain layer.
 *
 * This model is separate from the login user to keep HR data
 * and application access data independently manageable.
 */
data class Employee(
    val id: String = UUID.randomUUID().toString(),
    val employeeCode: String,
    val fullName: String,
    val role: Role,
    val jobTitle: String? = null,
    val department: String? = null,
    val email: String? = null,
    val phoneNumber: String? = null,
    val nationalId: String? = null,
    val salary: BigDecimal = BigDecimal.ZERO,
    val commissionRate: BigDecimal = BigDecimal.ZERO,
    val isActive: Boolean = true,
    val userId: String? = null,
    val hiredAtMillis: Long = System.currentTimeMillis(),
    val terminatedAtMillis: Long? = null,
    val lastShiftAtMillis: Long? = null,
    val permissions: Set<Permission> = emptySet(),
    val metadata: Map<String, String> = emptyMap()
) {
    init {
        require(id.isNotBlank()) { "id cannot be blank." }
        require(employeeCode.isNotBlank()) { "employeeCode cannot be blank." }
        require(fullName.isNotBlank()) { "fullName cannot be blank." }
        require(salary >= BigDecimal.ZERO) { "salary cannot be negative." }
        require(commissionRate >= BigDecimal.ZERO) { "commissionRate cannot be negative." }
        require(hiredAtMillis > 0L) { "hiredAtMillis must be greater than zero." }

        jobTitle?.let {
            require(it.isNotBlank()) { "jobTitle cannot be blank when provided." }
        }
        department?.let {
            require(it.isNotBlank()) { "department cannot be blank when provided." }
        }
        email?.let {
            require(it.isNotBlank()) { "email cannot be blank when provided." }
        }
        phoneNumber?.let {
            require(it.isNotBlank()) { "phoneNumber cannot be blank when provided." }
        }
        nationalId?.let {
            require(it.isNotBlank()) { "nationalId cannot be blank when provided." }
        }
        userId?.let {
            require(it.isNotBlank()) { "userId cannot be blank when provided." }
        }
        terminatedAtMillis?.let {
            require(it > 0L) { "terminatedAtMillis must be greater than zero when provided." }
        }
        lastShiftAtMillis?.let {
            require(it > 0L) { "lastShiftAtMillis must be greater than zero when provided." }
        }
    }

    val displayName: String
        get() = fullName.trim()

    val effectivePermissions: Set<Permission>
        get() = if (role == Role.MANAGER) {
            Permission.values().toSet()
        } else {
            role.defaultPermissions + permissions
        }

    val isManager: Boolean
        get() = role.isManager()

    val isEmployee: Boolean
        get() = role.isEmployee()

    val isTerminated: Boolean
        get() = terminatedAtMillis != null

    fun hasPermission(permission: Permission): Boolean {
        if (!isActive || isTerminated) return false
        return permission in effectivePermissions
    }

    fun canApprovePayments(): Boolean {
        return hasPermission(Permission.APPROVE_PAYMENT)
    }

    fun canAccessAuditTrail(): Boolean {
        return hasPermission(Permission.VIEW_AUDIT_LOGS)
    }

    fun canManageUsers(): Boolean {
        return hasPermission(Permission.MANAGE_USERS)
    }
}