package com.myimdad_por.data.remote.dto

import java.util.Locale

data class EmployeeDto(
    val id: String,
    val serverId: String? = null,
    val employeeCode: String,
    val fullName: String,
    val role: String,
    val jobTitle: String? = null,
    val department: String? = null,
    val email: String? = null,
    val phoneNumber: String? = null,
    val nationalId: String? = null,
    val salary: String = "0.00",
    val commissionRate: String = "0.00",
    val isActive: Boolean = true,
    val userId: String? = null,
    val hiredAtMillis: Long = System.currentTimeMillis(),
    val terminatedAtMillis: Long? = null,
    val lastShiftAtMillis: Long? = null,
    val permissionsJson: String = "[]",
    val metadataJson: String = "{}",
    val syncState: String = "PENDING",
    val isDeleted: Boolean = false,
    val syncedAtMillis: Long? = null,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis()
) {
    init {
        require(id.isNotBlank()) { "id cannot be blank." }
        require(employeeCode.isNotBlank()) { "employeeCode cannot be blank." }
        require(fullName.isNotBlank()) { "fullName cannot be blank." }
        require(role.isNotBlank()) { "role cannot be blank." }
        require(salary.isNotBlank()) { "salary cannot be blank." }
        require(commissionRate.isNotBlank()) { "commissionRate cannot be blank." }
        require(hiredAtMillis > 0L) { "hiredAtMillis must be greater than zero." }
        require(createdAtMillis > 0L) { "createdAtMillis must be greater than zero." }
        require(updatedAtMillis > 0L) { "updatedAtMillis must be greater than zero." }

        jobTitle?.let { require(it.isNotBlank()) { "jobTitle cannot be blank when provided." } }
        department?.let { require(it.isNotBlank()) { "department cannot be blank when provided." } }
        email?.let { require(it.isNotBlank()) { "email cannot be blank when provided." } }
        phoneNumber?.let { require(it.isNotBlank()) { "phoneNumber cannot be blank when provided." } }
        nationalId?.let { require(it.isNotBlank()) { "nationalId cannot be blank when provided." } }
        userId?.let { require(it.isNotBlank()) { "userId cannot be blank when provided." } }
        terminatedAtMillis?.let { require(it > 0L) { "terminatedAtMillis must be greater than zero when provided." } }
        lastShiftAtMillis?.let { require(it > 0L) { "lastShiftAtMillis must be greater than zero when provided." } }
        require(syncState.isNotBlank()) { "syncState cannot be blank." }
    }

    val normalizedEmployeeCode: String
        get() = employeeCode.trim().uppercase(Locale.ROOT)

    val displayName: String
        get() = fullName.trim()
}