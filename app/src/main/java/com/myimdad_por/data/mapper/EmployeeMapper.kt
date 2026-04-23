package com.myimdad_por.data.mapper

import com.myimdad_por.core.utils.toBigDecimalOrZero
import com.myimdad_por.data.remote.dto.EmployeeDto
import com.myimdad_por.domain.model.Employee
import com.myimdad_por.domain.model.Permission
import com.myimdad_por.domain.model.Role
import org.json.JSONArray
import org.json.JSONObject
import java.math.BigDecimal
import java.math.RoundingMode

fun Employee.toDto(
    serverId: String? = null,
    syncState: String = "PENDING",
    isDeleted: Boolean = false,
    syncedAtMillis: Long? = null,
    createdAtMillis: Long = this.hiredAtMillis,
    updatedAtMillis: Long = System.currentTimeMillis()
): EmployeeDto {
    return EmployeeDto(
        id = id,
        serverId = serverId,
        employeeCode = employeeCode,
        fullName = fullName,
        role = role.name,
        jobTitle = jobTitle,
        department = department,
        email = email,
        phoneNumber = phoneNumber,
        nationalId = nationalId,
        salary = salary.money(),
        commissionRate = commissionRate.money(),
        isActive = isActive,
        userId = userId,
        hiredAtMillis = hiredAtMillis,
        terminatedAtMillis = terminatedAtMillis,
        lastShiftAtMillis = lastShiftAtMillis,
        permissionsJson = permissions.toJsonString(),
        metadataJson = metadata.toJsonString(),
        syncState = syncState,
        isDeleted = isDeleted,
        syncedAtMillis = syncedAtMillis,
        createdAtMillis = createdAtMillis,
        updatedAtMillis = updatedAtMillis
    )
}

fun EmployeeDto.toDomain(): Employee {
    return Employee(
        id = id,
        employeeCode = employeeCode,
        fullName = fullName,
        role = role.toRoleOrDefault(),
        jobTitle = jobTitle,
        department = department,
        email = email,
        phoneNumber = phoneNumber,
        nationalId = nationalId,
        salary = salary.toBigDecimalOrZero(),
        commissionRate = commissionRate.toBigDecimalOrZero(),
        isActive = isActive,
        userId = userId,
        hiredAtMillis = hiredAtMillis,
        terminatedAtMillis = terminatedAtMillis,
        lastShiftAtMillis = lastShiftAtMillis,
        permissions = permissionsJson.toPermissionSet(),
        metadata = metadataJson.toStringMap()
    )
}

fun List<Employee>.toDtoList(): List<EmployeeDto> = map { it.toDto() }

fun List<EmployeeDto>.toDomainList(): List<Employee> = map { it.toDomain() }

private fun String.toRoleOrDefault(): Role {
    val normalized = trim().uppercase()
    return runCatching { Role.valueOf(normalized) }.getOrDefault(Role.EMPLOYEE)
}

private fun Set<Permission>.toJsonString(): String {
    return JSONArray().apply {
        forEach { put(it.name) }
    }.toString()
}

private fun String.toPermissionSet(): Set<Permission> {
    if (isBlank()) return emptySet()
    return runCatching {
        val array = JSONArray(this)
        buildSet {
            for (i in 0 until array.length()) {
                val value = array.optString(i)
                runCatching { add(Permission.valueOf(value)) }
            }
        }
    }.getOrDefault(emptySet())
}

private fun Map<String, String>.toJsonString(): String {
    return JSONObject(this).toString()
}

private fun String.toStringMap(): Map<String, String> {
    if (isBlank()) return emptyMap()
    return runCatching {
        val json = JSONObject(this)
        buildMap {
            json.keys().forEach { key ->
                put(key, json.optString(key))
            }
        }
    }.getOrDefault(emptyMap())
}

private fun BigDecimal.money(): String =
    setScale(2, RoundingMode.HALF_UP).toPlainString()