package com.myimdad_por.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.myimdad_por.domain.model.Employee
import com.myimdad_por.domain.model.Permission
import com.myimdad_por.domain.model.Role
import org.json.JSONArray
import org.json.JSONObject
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.UUID

@Entity(
    tableName = "employees",
    indices = [
        Index(value = ["server_id"], unique = true),
        Index(value = ["employee_code"], unique = true),
        Index(value = ["full_name"]),
        Index(value = ["role"]),
        Index(value = ["department"]),
        Index(value = ["user_id"]),
        Index(value = ["is_active"]),
        Index(value = ["sync_state"])
    ]
)
data class EmployeeEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "server_id")
    val serverId: String? = null,

    @ColumnInfo(name = "employee_code")
    val employeeCode: String,

    @ColumnInfo(name = "full_name")
    val fullName: String,

    @ColumnInfo(name = "role")
    val role: String,

    @ColumnInfo(name = "job_title")
    val jobTitle: String? = null,

    @ColumnInfo(name = "department")
    val department: String? = null,

    @ColumnInfo(name = "email")
    val email: String? = null,

    @ColumnInfo(name = "phone_number")
    val phoneNumber: String? = null,

    @ColumnInfo(name = "national_id")
    val nationalId: String? = null,

    @ColumnInfo(name = "salary")
    val salary: String = "0.00",

    @ColumnInfo(name = "commission_rate")
    val commissionRate: String = "0.00",

    @ColumnInfo(name = "is_active")
    val isActive: Boolean = true,

    @ColumnInfo(name = "user_id")
    val userId: String? = null,

    @ColumnInfo(name = "hired_at_millis")
    val hiredAtMillis: Long,

    @ColumnInfo(name = "terminated_at_millis")
    val terminatedAtMillis: Long? = null,

    @ColumnInfo(name = "last_shift_at_millis")
    val lastShiftAtMillis: Long? = null,

    @ColumnInfo(name = "permissions_json")
    val permissionsJson: String = "[]",

    @ColumnInfo(name = "metadata_json")
    val metadataJson: String = "{}",

    @ColumnInfo(name = "sync_state")
    val syncState: String = "PENDING",

    @ColumnInfo(name = "is_deleted")
    val isDeleted: Boolean = false,

    @ColumnInfo(name = "synced_at_millis")
    val syncedAtMillis: Long? = null,

    @ColumnInfo(name = "created_at_millis")
    val createdAtMillis: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at_millis")
    val updatedAtMillis: Long = System.currentTimeMillis()
) {
    init {
        require(id.isNotBlank()) { "id cannot be blank." }
        require(employeeCode.isNotBlank()) { "employeeCode cannot be blank." }
        require(fullName.isNotBlank()) { "fullName cannot be blank." }
        require(role.isNotBlank()) { "role cannot be blank." }
        require(hiredAtMillis > 0L) { "hiredAtMillis must be greater than zero." }
    }

    companion object {
        fun fromDomain(
            employee: Employee,
            serverId: String? = null,
            syncState: String = "PENDING",
            isDeleted: Boolean = false,
            syncedAtMillis: Long? = null,
            createdAtMillis: Long = System.currentTimeMillis(),
            updatedAtMillis: Long = System.currentTimeMillis()
        ): EmployeeEntity {
            return EmployeeEntity(
                id = employee.id,
                serverId = serverId,
                employeeCode = employee.employeeCode,
                fullName = employee.fullName,
                role = employee.role.name,
                jobTitle = employee.jobTitle,
                department = employee.department,
                email = employee.email,
                phoneNumber = employee.phoneNumber,
                nationalId = employee.nationalId,
                salary = employee.salary.money(),
                commissionRate = employee.commissionRate.money(),
                isActive = employee.isActive,
                userId = employee.userId,
                hiredAtMillis = employee.hiredAtMillis,
                terminatedAtMillis = employee.terminatedAtMillis,
                lastShiftAtMillis = employee.lastShiftAtMillis,
                permissionsJson = employee.permissions.toJsonString(),
                metadataJson = employee.metadata.toJsonString(),
                syncState = syncState,
                isDeleted = isDeleted,
                syncedAtMillis = syncedAtMillis,
                createdAtMillis = createdAtMillis,
                updatedAtMillis = updatedAtMillis
            )
        }
    }
}

fun EmployeeEntity.toDomain(): Employee {
    return Employee(
        id = id,
        employeeCode = employeeCode,
        fullName = fullName,
        role = runCatching { Role.valueOf(role) }.getOrDefault(Role.EMPLOYEE),
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

private fun BigDecimal.money(): String = this.setScale(2, RoundingMode.HALF_UP).toPlainString()

private fun String.toJsonArray(): JSONArray = JSONArray(this)

private fun Set<Permission>.toJsonString(): String {
    return JSONArray().apply {
        forEach { put(it.name) }
    }.toString()
}

private fun String?.toPermissionSet(): Set<Permission> {
    if (this.isNullOrBlank()) return emptySet()
    return runCatching {
        val array = toJsonArray()
        buildSet {
            for (i in 0 until array.length()) {
                val name = array.optString(i)
                runCatching { add(Permission.valueOf(name)) }
            }
        }
    }.getOrDefault(emptySet())
}

private fun Map<String, String>.toJsonString(): String {
    return JSONObject(this).toString()
}

private fun String?.toStringMap(): Map<String, String> {
    if (this.isNullOrBlank()) return emptyMap()
    return runCatching {
        val json = JSONObject(this)
        buildMap {
            json.keys().forEach { key ->
                put(key, json.optString(key))
            }
        }
    }.getOrDefault(emptyMap())
}

private fun String.toBigDecimalOrZero(): BigDecimal {
    return runCatching { BigDecimal(this) }.getOrDefault(BigDecimal.ZERO).moneyBigDecimal()
}

private fun BigDecimal.moneyBigDecimal(): BigDecimal = this.setScale(2, RoundingMode.HALF_UP)