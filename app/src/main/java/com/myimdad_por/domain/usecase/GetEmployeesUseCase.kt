package com.myimdad_por.domain.usecase

import com.myimdad_por.domain.model.Employee
import com.myimdad_por.domain.model.Role
import com.myimdad_por.domain.repository.EmployeePerformancePeriod
import com.myimdad_por.domain.repository.EmployeePerformanceSummary
import com.myimdad_por.domain.repository.EmployeeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject

data class EmployeeQuery(
    val id: String? = null,
    val code: String? = null,
    val email: String? = null,
    val role: Role? = null,
    val managerId: String? = null,
    val text: String? = null,
    val activeOnly: Boolean = false,
    val missingUserAccountOnly: Boolean = false
)

class GetEmployeesUseCase @Inject constructor(
    private val employeeRepository: EmployeeRepository
) {

    suspend operator fun invoke(query: EmployeeQuery = EmployeeQuery()): List<Employee> {
        query.id?.trim()?.takeIf { it.isNotBlank() }?.let {
            return listOfNotNull(employeeRepository.getEmployeeById(it))
        }

        query.code?.trim()?.takeIf { it.isNotBlank() }?.let {
            return listOfNotNull(employeeRepository.getEmployeeByCode(it))
        }

        query.email?.trim()?.takeIf { it.isNotBlank() }?.let {
            return listOfNotNull(employeeRepository.getEmployeeByEmail(it))
        }

        if (query.missingUserAccountOnly) {
            val items = employeeRepository.getEmployeesWithMissingUserAccount()
            return items.filterWithText(query.text)
        }

        val base = when {
            query.managerId != null && query.role == null && !query.activeOnly ->
                employeeRepository.observeEmployeesByManager(query.managerId).first()

            query.role != null && query.managerId == null && !query.activeOnly ->
                employeeRepository.observeEmployeesByRole(query.role).first()

            query.activeOnly && query.role == null && query.managerId == null ->
                employeeRepository.observeActiveEmployees().first()

            else ->
                employeeRepository.observeAllEmployees().first()
        }

        return base.filterWithText(query.text)
    }

    fun observeAll(): Flow<List<Employee>> = employeeRepository.observeAllEmployees()

    fun observeByManager(managerId: String): Flow<List<Employee>> {
        require(managerId.isNotBlank()) { "managerId cannot be blank" }
        return employeeRepository.observeEmployeesByManager(managerId)
    }

    fun observeByRole(role: Role): Flow<List<Employee>> {
        return employeeRepository.observeEmployeesByRole(role)
    }

    fun observeActive(): Flow<List<Employee>> = employeeRepository.observeActiveEmployees()

    suspend fun getById(id: String): Employee? {
        require(id.isNotBlank()) { "id cannot be blank" }
        return employeeRepository.getEmployeeById(id)
    }

    suspend fun getByCode(code: String): Employee? {
        require(code.isNotBlank()) { "code cannot be blank" }
        return employeeRepository.getEmployeeByCode(code)
    }

    suspend fun getByEmail(email: String): Employee? {
        require(email.isNotBlank()) { "email cannot be blank" }
        return employeeRepository.getEmployeeByEmail(email)
    }

    suspend fun search(query: String): List<Employee> {
        require(query.isNotBlank()) { "query cannot be blank" }
        return employeeRepository.searchEmployees(query)
    }

    suspend fun countAll(): Long = employeeRepository.countEmployees()

    suspend fun countActive(): Long = employeeRepository.countActiveEmployees()

    suspend fun countByRole(role: Role): Long = employeeRepository.countEmployeesByRole(role)

    suspend fun performance(
        employeeId: String,
        period: EmployeePerformancePeriod
    ): EmployeePerformanceSummary {
        require(employeeId.isNotBlank()) { "employeeId cannot be blank" }
        return employeeRepository.getEmployeePerformance(employeeId, period)
    }

    suspend fun getEmployeesWithMissingUserAccount(): List<Employee> {
        return employeeRepository.getEmployeesWithMissingUserAccount()
    }

    private fun List<Employee>.filterWithText(text: String?): List<Employee> {
        val q = text?.trim().orEmpty()
        if (q.isBlank()) return this

        return asSequence()
            .filter { it.toString().contains(q, ignoreCase = true) }
            .toList()
    }
}