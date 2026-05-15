package com.myimdad_por.domain.repository

import com.myimdad_por.domain.model.Employee
import com.myimdad_por.domain.model.Permission
import com.myimdad_por.domain.model.Role
import java.math.BigDecimal
import kotlinx.coroutines.flow.Flow

/**
 * Contract for employee management and performance tracking.
 */
interface EmployeeRepository {

    fun observeAllEmployees(): Flow<List<Employee>>

    fun observeEmployeesByManager(managerId: String): Flow<List<Employee>>

    fun observeEmployeesByRole(role: Role): Flow<List<Employee>>

    fun observeActiveEmployees(): Flow<List<Employee>>

    suspend fun getEmployeeById(id: String): Employee?

    suspend fun getEmployeeByCode(employeeCode: String): Employee?

    suspend fun getEmployeeByEmail(email: String): Employee?

    suspend fun searchEmployees(query: String): List<Employee>

    suspend fun saveEmployee(employee: Employee): Result<Employee>

    suspend fun saveEmployees(employees: List<Employee>): Result<List<Employee>>

    suspend fun updateEmployee(employee: Employee): Result<Employee>

    suspend fun updateEmployeeStatus(
        employeeId: String,
        isActive: Boolean,
        reason: String? = null
    ): Result<Employee>

    suspend fun assignPermissions(
        employeeId: String,
        permissions: Set<Permission>
    ): Result<Employee>

    suspend fun removePermissions(
        employeeId: String,
        permissions: Set<Permission>
    ): Result<Employee>

    suspend fun deleteEmployee(id: String): Result<Unit>

    suspend fun deleteEmployees(ids: List<String>): Result<Int>

    suspend fun clearAll(): Result<Unit>

    suspend fun countEmployees(): Long

    suspend fun countActiveEmployees(): Long

    suspend fun countEmployeesByRole(role: Role): Long

    suspend fun getEmployeePerformance(
        employeeId: String,
        period: EmployeePerformancePeriod
    ): EmployeePerformanceSummary

    suspend fun getEmployeesWithMissingUserAccount(): List<Employee>
}

/**
 * Time window used for performance analytics.
 */
data class EmployeePerformancePeriod(
    val fromMillis: Long,
    val toMillis: Long
) {
    init {
        require(fromMillis > 0L) { "fromMillis must be greater than zero." }
        require(toMillis > 0L) { "toMillis must be greater than zero." }
        require(fromMillis <= toMillis) { "fromMillis must be less than or equal to toMillis." }
    }
}

/**
 * Compact performance snapshot for one employee.
 */
data class EmployeePerformanceSummary(
    val employeeId: String,
    val employeeCode: String,
    val period: EmployeePerformancePeriod,
    val invoicesIssued: Long = 0L,
    val expensesRecorded: Long = 0L,
    val salesProcessed: Long = 0L,
    val returnsProcessed: Long = 0L,
    val paymentsHandled: Long = 0L,
    val totalSalesAmount: BigDecimal = BigDecimal.ZERO,
    val totalExpenseAmount: BigDecimal = BigDecimal.ZERO,
    val lastActivityAtMillis: Long? = null,
    val note: String? = null
) {
    init {
        require(employeeId.isNotBlank()) { "employeeId cannot be blank." }
        require(employeeCode.isNotBlank()) { "employeeCode cannot be blank." }
        require(invoicesIssued >= 0L) { "invoicesIssued cannot be negative." }
        require(expensesRecorded >= 0L) { "expensesRecorded cannot be negative." }
        require(salesProcessed >= 0L) { "salesProcessed cannot be negative." }
        require(returnsProcessed >= 0L) { "returnsProcessed cannot be negative." }
        require(paymentsHandled >= 0L) { "paymentsHandled cannot be negative." }
        require(totalSalesAmount >= BigDecimal.ZERO) { "totalSalesAmount cannot be negative." }
        require(totalExpenseAmount >= BigDecimal.ZERO) { "totalExpenseAmount cannot be negative." }
        lastActivityAtMillis?.let {
            require(it > 0L) { "lastActivityAtMillis must be greater than zero when provided." }
        }
        note?.let {
            require(it.isNotBlank()) { "note cannot be blank when provided." }
        }
    }

    val netContribution: BigDecimal
        get() = totalSalesAmount - totalExpenseAmount
}