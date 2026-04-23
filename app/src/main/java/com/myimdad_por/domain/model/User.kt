package com.myimdad_por.domain.model

import java.util.UUID

/**
 * Domain user model with role-based access control.
 */
data class User(
    val id: String = UUID.randomUUID().toString(),
    val fullName: String,
    val role: Role,
    val username: String? = null,
    val email: String? = null,
    val phoneNumber: String? = null,
    val isActive: Boolean = true,
    val permissions: Set<Permission> = emptySet(),
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis(),
    val lastLoginAtMillis: Long? = null,
    val deviceId: String? = null,
    val metadata: Map<String, String> = emptyMap()
) {
    init {
        require(id.isNotBlank()) { "id cannot be blank." }
        require(fullName.isNotBlank()) { "fullName cannot be blank." }
        require(createdAtMillis > 0L) { "createdAtMillis must be greater than zero." }
        require(updatedAtMillis > 0L) { "updatedAtMillis must be greater than zero." }
        require(updatedAtMillis >= createdAtMillis) {
            "updatedAtMillis must be greater than or equal to createdAtMillis."
        }

        username?.let {
            require(it.isNotBlank()) { "username cannot be blank when provided." }
        }
        email?.let {
            require(it.isNotBlank()) { "email cannot be blank when provided." }
        }
        phoneNumber?.let {
            require(it.isNotBlank()) { "phoneNumber cannot be blank when provided." }
        }
        lastLoginAtMillis?.let {
            require(it > 0L) { "lastLoginAtMillis must be greater than zero when provided." }
        }
        deviceId?.let {
            require(it.isNotBlank()) { "deviceId cannot be blank when provided." }
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

    fun hasPermission(permission: Permission): Boolean {
        if (!isActive) return false
        return role.hasPermission(permission) || permission in permissions
    }

    fun canEditSensitiveData(): Boolean {
        return hasPermission(Permission.MANAGE_USERS) ||
            hasPermission(Permission.MANAGE_SETTINGS) ||
            hasPermission(Permission.MANAGE_ACCOUNTING)
    }

    fun canOperatePayments(): Boolean {
        return hasPermission(Permission.MANAGE_PAYMENTS) ||
            hasPermission(Permission.APPROVE_PAYMENT) ||
            hasPermission(Permission.PROCESS_REFUND)
    }

    fun canAccessAuditTrail(): Boolean {
        return hasPermission(Permission.VIEW_AUDIT_LOGS)
    }
}