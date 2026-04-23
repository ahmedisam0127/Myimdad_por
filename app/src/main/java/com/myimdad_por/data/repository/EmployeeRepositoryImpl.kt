package com.myimdad_por.data.repository

import com.myimdad_por.core.dispatchers.AppDispatchers
import com.myimdad_por.core.dispatchers.DefaultAppDispatchers
import com.myimdad_por.core.network.NetworkResult
import com.myimdad_por.data.local.dao.EmployeeDao
import com.myimdad_por.data.local.entity.EmployeeEntity
import com.myimdad_por.data.local.entity.toDomain as entityToDomain
import com.myimdad_por.data.mapper.toDto
import com.myimdad_por.data.mapper.toDomain as dtoToDomain
import com.myimdad_por.data.remote.datasource.EmployeeRemoteDataSource
import com.myimdad_por.data.remote.dto.EmployeeDto
import com.myimdad_por.domain.model.Employee
import com.myimdad_por.domain.model.Permission
import com.myimdad_por.domain.model.Role
import com.myimdad_por.domain.repository.EmployeePerformancePeriod
import com.myimdad_por.domain.repository.EmployeePerformanceSummary
import com.myimdad_por.domain.repository.EmployeeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale
import java.util.UUID

class EmployeeRepositoryImpl(
    private val employeeDao: EmployeeDao,
    private val remoteDataSource: EmployeeRemoteDataSource? = null,
    private val dispatchers: AppDispatchers = DefaultAppDispatchers
) : EmployeeRepository {

    override fun observeAllEmployees(): Flow<List<Employee>> {
        return employeeDao.observeAll()
            .map { entities -> entities.map { it.entityToDomain() } }
            .flowOn(dispatchers.io)
    }

    override fun observeEmployeesByManager(managerId: String): Flow<List<Employee>> {
        val normalizedManagerId = managerId.trim()
        if (normalizedManagerId.isBlank()) return flowOf(emptyList())

        return employeeDao.observeAll()
            .map { entities ->
                entities
                    .map { it.entityToDomain() }
                    .filter { employee -> employee.matchesManagerId(normalizedManagerId) }
            }
            .flowOn(dispatchers.io)
    }

    override fun observeEmployeesByRole(role: Role): Flow<List<Employee>> {
        return employeeDao.observeByRole(role.name)
            .map { entities -> entities.map { it.entityToDomain() } }
            .flowOn(dispatchers.io)
    }

    override fun observeActiveEmployees(): Flow<List<Employee>> {
        return employeeDao.observeActive()
            .map { entities -> entities.map { it.entityToDomain() } }
            .flowOn(dispatchers.io)
    }

    override suspend fun getEmployeeById(id: String): Employee? = withContext(dispatchers.io) {
        val normalizedId = id.trim()
        if (normalizedId.isBlank()) return@withContext null

        employeeDao.getById(normalizedId)?.entityToDomain()
            ?: employeeDao.getByServerId(normalizedId)?.entityToDomain()
            ?: fetchRemoteById(normalizedId)
    }

    override suspend fun getEmployeeByCode(employeeCode: String): Employee? = withContext(dispatchers.io) {
        val normalizedCode = employeeCode.trim().uppercase(Locale.ROOT)
        if (normalizedCode.isBlank()) return@withContext null

        employeeDao.getByEmployeeCode(normalizedCode)?.entityToDomain()
            ?: fetchRemoteByLookup(normalizedCode) { dto ->
                dto.normalizedEmployeeCode == normalizedCode
            }
    }

    override suspend fun getEmployeeByEmail(email: String): Employee? = withContext(dispatchers.io) {
        val normalizedEmail = email.trim().lowercase(Locale.ROOT)
        if (normalizedEmail.isBlank()) return@withContext null

        employeeDao.observeAll()
            .first()
            .asSequence()
            .map { it.entityToDomain() }
            .firstOrNull { employee ->
                employee.email.clean()?.lowercase(Locale.ROOT) == normalizedEmail
            }
            ?: fetchRemoteByLookup(normalizedEmail) { dto ->
                dto.email.clean()?.lowercase(Locale.ROOT) == normalizedEmail
            }
    }

    override suspend fun searchEmployees(query: String): List<Employee> = withContext(dispatchers.io) {
        val normalizedQuery = query.trim()
        if (normalizedQuery.isBlank()) return@withContext emptyList()

        val localEmployees = employeeDao.search(normalizedQuery)
            .first()
            .map { it.entityToDomain() }

        if (localEmployees.isNotEmpty() || remoteDataSource == null) {
            return@withContext localEmployees
        }

        val remoteEmployees = when (val result = remoteDataSource.listEmployees(search = normalizedQuery)) {
            is NetworkResult.Success -> result.data.map { it.dtoToDomain() }
            is NetworkResult.Error,
            NetworkResult.Loading -> emptyList()
        }

        if (remoteEmployees.isNotEmpty()) {
            employeeDao.insertAll(remoteEmployees.map { it.toEntity(syncState = "SYNCED") })
        }

        remoteEmployees
    }

    override suspend fun saveEmployee(employee: Employee): Result<Employee> = withContext(dispatchers.io) {
        runCatching {
            val normalized = employee.normalize()
            val existing = getLocalEmployeeForWrite(normalized)

            val localEntity = normalized.toEntity(
                serverId = existing?.serverId.clean(),
                syncState = "PENDING",
                isDeleted = existing?.isDeleted == true,
                syncedAtMillis = existing?.syncedAtMillis,
                createdAtMillis = existing?.createdAtMillis ?: normalized.hiredAtMillis,
                updatedAtMillis = System.currentTimeMillis()
            )

            employeeDao.insert(localEntity)

            syncEmployeeToRemote(normalized, existing) ?: normalized
        }
    }

    override suspend fun saveEmployees(employees: List<Employee>): Result<List<Employee>> {
        return withContext(dispatchers.io) {
            runCatching {
                if (employees.isEmpty()) return@runCatching emptyList()

                employees.map { employee ->
                    saveEmployee(employee).getOrElse { employee }
                }
            }
        }
    }

    override suspend fun updateEmployee(employee: Employee): Result<Employee> {
        return saveEmployee(employee)
    }

    override suspend fun updateEmployeeStatus(
        employeeId: String,
        isActive: Boolean,
        reason: String?
    ): Result<Employee> = withContext(dispatchers.io) {
        runCatching {
            val normalizedId = employeeId.trim()
            require(normalizedId.isNotBlank()) { "employeeId cannot be blank." }

            val current = getLocalEmployeeById(normalizedId)
                ?: error("Employee not found.")

            val currentDomain = current.entityToDomain()
            val updatedDomain = currentDomain.copy(
                isActive = isActive,
                terminatedAtMillis = if (isActive) null else System.currentTimeMillis(),
                metadata = currentDomain.metadata + buildMap {
                    reason.clean()?.let { put("statusReason", it) }
                    put("statusChangedAtMillis", System.currentTimeMillis().toString())
                }
            )

            employeeDao.insert(
                updatedDomain.toEntity(
                    serverId = current.serverId.clean(),
                    syncState = "PENDING",
                    isDeleted = current.isDeleted,
                    syncedAtMillis = current.syncedAtMillis,
                    createdAtMillis = current.createdAtMillis,
                    updatedAtMillis = System.currentTimeMillis()
                )
            )

            syncEmployeeToRemote(updatedDomain, current) ?: updatedDomain
        }
    }

    override suspend fun assignPermissions(
        employeeId: String,
        permissions: Set<Permission>
    ): Result<Employee> = withContext(dispatchers.io) {
        runCatching {
            val normalizedId = employeeId.trim()
            require(normalizedId.isNotBlank()) { "employeeId cannot be blank." }

            val current = getLocalEmployeeById(normalizedId)
                ?: error("Employee not found.")

            val currentDomain = current.entityToDomain()
            val updatedDomain = currentDomain.copy(
                permissions = currentDomain.permissions + permissions
            )

            employeeDao.insert(
                updatedDomain.toEntity(
                    serverId = current.serverId.clean(),
                    syncState = "PENDING",
                    isDeleted = current.isDeleted,
                    syncedAtMillis = current.syncedAtMillis,
                    createdAtMillis = current.createdAtMillis,
                    updatedAtMillis = System.currentTimeMillis()
                )
            )

            syncEmployeeToRemote(updatedDomain, current) ?: updatedDomain
        }
    }

    override suspend fun removePermissions(
        employeeId: String,
        permissions: Set<Permission>
    ): Result<Employee> = withContext(dispatchers.io) {
        runCatching {
            val normalizedId = employeeId.trim()
            require(normalizedId.isNotBlank()) { "employeeId cannot be blank." }

            val current = getLocalEmployeeById(normalizedId)
                ?: error("Employee not found.")

            val currentDomain = current.entityToDomain()
            val updatedDomain = currentDomain.copy(
                permissions = currentDomain.permissions - permissions
            )

            employeeDao.insert(
                updatedDomain.toEntity(
                    serverId = current.serverId.clean(),
                    syncState = "PENDING",
                    isDeleted = current.isDeleted,
                    syncedAtMillis = current.syncedAtMillis,
                    createdAtMillis = current.createdAtMillis,
                    updatedAtMillis = System.currentTimeMillis()
                )
            )

            syncEmployeeToRemote(updatedDomain, current) ?: updatedDomain
        }
    }

    override suspend fun deleteEmployee(id: String): Result<Unit> = withContext(dispatchers.io) {
        runCatching {
            val normalizedId = id.trim()
            require(normalizedId.isNotBlank()) { "id cannot be blank." }

            val local = getLocalEmployeeById(normalizedId)
            val localId = local?.id ?: normalizedId

            employeeDao.softDelete(localId, syncState = "PENDING")

            val remote = remoteDataSource ?: return@runCatching Unit
            val remoteId = local?.serverId.clean() ?: normalizedId

            when (remote.deleteEmployee(remoteId)) {
                is NetworkResult.Success -> {
                    employeeDao.markSynced(localId, syncState = "SYNCED")
                }

                is NetworkResult.Error,
                NetworkResult.Loading -> Unit
            }

            Unit
        }
    }

    override suspend fun deleteEmployees(ids: List<String>): Result<Int> = withContext(dispatchers.io) {
        runCatching {
            if (ids.isEmpty()) return@runCatching 0

            var successCount = 0
            ids.asSequence()
                .mapNotNull { it.clean() }
                .distinct()
                .forEach { employeeId ->
                    if (deleteEmployee(employeeId).isSuccess) {
                        successCount++
                    }
                }

            successCount
        }
    }

    override suspend fun clearAll(): Result<Unit> = withContext(dispatchers.io) {
        runCatching {
            val localEmployees = employeeDao.observeAll().first()
            localEmployees.forEach { entity ->
                employeeDao.softDelete(entity.id, syncState = "PENDING")
            }
            employeeDao.purgeDeleted()
            Unit
        }
    }

    override suspend fun countEmployees(): Long = withContext(dispatchers.io) {
        employeeDao.countActive().toLong()
    }

    override suspend fun countActiveEmployees(): Long = withContext(dispatchers.io) {
        employeeDao.observeActive()
            .first()
            .size
            .toLong()
    }

    override suspend fun countEmployeesByRole(role: Role): Long = withContext(dispatchers.io) {
        employeeDao.observeByRole(role.name)
            .first()
            .size
            .toLong()
    }

    override suspend fun getEmployeePerformance(
        employeeId: String,
        period: EmployeePerformancePeriod
    ): EmployeePerformanceSummary = withContext(dispatchers.io) {
        val normalizedId = employeeId.trim()
        val employee = getEmployeeById(normalizedId)

        if (employee == null) {
            return@withContext EmployeePerformanceSummary(
                employeeId = normalizedId.ifBlank { UUID.randomUUID().toString() },
                employeeCode = "UNKNOWN",
                period = period,
                note = "Employee not found"
            )
        }

        EmployeePerformanceSummary(
            employeeId = employee.id,
            employeeCode = employee.employeeCode,
            period = period,
            invoicesIssued = 0L,
            expensesRecorded = 0L,
            salesProcessed = 0L,
            returnsProcessed = 0L,
            paymentsHandled = 0L,
            totalSalesAmount = BigDecimal.ZERO,
            totalExpenseAmount = BigDecimal.ZERO,
            lastActivityAtMillis = employee.lastShiftAtMillis,
            note = "Performance source is not connected yet"
        )
    }

    override suspend fun getEmployeesWithMissingUserAccount(): List<Employee> = withContext(dispatchers.io) {
        employeeDao.observeAll()
            .first()
            .map { it.entityToDomain() }
            .filter { it.userId.clean().isNullOrBlank() }
    }

    private suspend fun fetchRemoteById(id: String): Employee? {
        val remote = remoteDataSource ?: return null

        return when (val result = remote.getEmployee(id)) {
            is NetworkResult.Success -> {
                val dto = result.data
                employeeDao.insert(dto.toEntity(syncState = "SYNCED"))
                dto.dtoToDomain()
            }

            is NetworkResult.Error,
            NetworkResult.Loading -> null
        }
    }

    private suspend fun fetchRemoteByLookup(
        query: String,
        predicate: (EmployeeDto) -> Boolean
    ): Employee? {
        val remote = remoteDataSource ?: return null

        return when (val result = remote.listEmployees(search = query)) {
            is NetworkResult.Success -> {
                val dto = result.data.firstOrNull(predicate) ?: return null
                employeeDao.insert(dto.toEntity(syncState = "SYNCED"))
                dto.dtoToDomain()
            }

            is NetworkResult.Error,
            NetworkResult.Loading -> null
        }
    }

    private suspend fun syncEmployeeToRemote(
        employee: Employee,
        local: EmployeeEntity?
    ): Employee? {
        val remote = remoteDataSource ?: return null
        val remoteId = local?.serverId.clean()

        return when {
            remoteId == null -> {
                when (val result = remote.createEmployee(employee.toDto(serverId = null, syncState = "PENDING"))) {
                    is NetworkResult.Success -> {
                        val syncedDto = result.data
                        employeeDao.insert(syncedDto.toEntity(syncState = "SYNCED"))
                        syncedDto.dtoToDomain()
                    }

                    is NetworkResult.Error,
                    NetworkResult.Loading -> null
                }
            }

            else -> {
                when (
                    val result = remote.updateEmployee(
                        id = remoteId,
                        request = employee.toDto(
                            serverId = remoteId,
                            syncState = "PENDING"
                        )
                    )
                ) {
                    is NetworkResult.Success -> {
                        val syncedDto = result.data
                        employeeDao.insert(syncedDto.toEntity(syncState = "SYNCED"))
                        syncedDto.dtoToDomain()
                    }

                    is NetworkResult.Error,
                    NetworkResult.Loading -> null
                }
            }
        }
    }

    private suspend fun getLocalEmployeeById(id: String): EmployeeEntity? {
        return employeeDao.getById(id)
            ?: employeeDao.getByServerId(id)
    }

    private suspend fun getLocalEmployeeForWrite(employee: Employee): EmployeeEntity? {
        val byId = employeeDao.getById(employee.id)
        if (byId != null) return byId

        val byServerId = employeeDao.getByServerId(employee.id)
        if (byServerId != null) return byServerId

        val byCode = employeeDao.getByEmployeeCode(employee.employeeCode)
        if (byCode != null) return byCode

        val userId = employee.userId.clean()
        return if (userId != null) employeeDao.getByUserId(userId) else null
    }

    private fun EmployeeEntity.toEntity(
        serverId: String? = null,
        syncState: String = "PENDING",
        isDeleted: Boolean = false,
        syncedAtMillis: Long? = null,
        createdAtMillis: Long = this.hiredAtMillis,
        updatedAtMillis: Long = System.currentTimeMillis()
    ): EmployeeEntity {
        return EmployeeEntity(
            id = id.trim(),
            serverId = serverId.clean(),
            employeeCode = employeeCode.trim(),
            fullName = fullName.trim(),
            role = role.trim().ifBlank { Role.EMPLOYEE.name },
            jobTitle = jobTitle.clean(),
            department = department.clean(),
            email = email.clean(),
            phoneNumber = phoneNumber.clean(),
            nationalId = nationalId.clean(),
            salary = salary.trim().ifBlank { "0.00" },
            commissionRate = commissionRate.trim().ifBlank { "0.00" },
            isActive = isActive,
            userId = userId.clean(),
            hiredAtMillis = hiredAtMillis,
            terminatedAtMillis = terminatedAtMillis,
            lastShiftAtMillis = lastShiftAtMillis,
            permissionsJson = permissionsJson.trim().ifBlank { "[]" },
            metadataJson = metadataJson.trim().ifBlank { "{}" },
            syncState = syncState,
            isDeleted = isDeleted,
            syncedAtMillis = syncedAtMillis,
            createdAtMillis = createdAtMillis,
            updatedAtMillis = updatedAtMillis
        )
    }

    private fun Employee.toEntity(
        serverId: String? = null,
        syncState: String = "PENDING",
        isDeleted: Boolean = false,
        syncedAtMillis: Long? = null,
        createdAtMillis: Long = this.hiredAtMillis,
        updatedAtMillis: Long = System.currentTimeMillis()
    ): EmployeeEntity {
        return EmployeeEntity(
            id = id.trim(),
            serverId = serverId.clean(),
            employeeCode = employeeCode.trim(),
            fullName = fullName.trim(),
            role = role.name,
            jobTitle = jobTitle.clean(),
            department = department.clean(),
            email = email.clean(),
            phoneNumber = phoneNumber.clean(),
            nationalId = nationalId.clean(),
            salary = salary.money(),
            commissionRate = commissionRate.money(),
            isActive = isActive,
            userId = userId.clean(),
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

    private fun EmployeeDto.toEntity(
        syncState: String = "SYNCED"
    ): EmployeeEntity {
        return EmployeeEntity(
            id = id.trim(),
            serverId = serverId.clean() ?: id.trim(),
            employeeCode = employeeCode.trim(),
            fullName = fullName.trim(),
            role = role.trim().ifBlank { Role.EMPLOYEE.name },
            jobTitle = jobTitle.clean(),
            department = department.clean(),
            email = email.clean(),
            phoneNumber = phoneNumber.clean(),
            nationalId = nationalId.clean(),
            salary = salary.trim().ifBlank { "0.00" },
            commissionRate = commissionRate.trim().ifBlank { "0.00" },
            isActive = isActive,
            userId = userId.clean(),
            hiredAtMillis = hiredAtMillis,
            terminatedAtMillis = terminatedAtMillis,
            lastShiftAtMillis = lastShiftAtMillis,
            permissionsJson = permissionsJson.trim().ifBlank { "[]" },
            metadataJson = metadataJson.trim().ifBlank { "{}" },
            syncState = syncState,
            isDeleted = isDeleted,
            syncedAtMillis = syncedAtMillis,
            createdAtMillis = createdAtMillis,
            updatedAtMillis = updatedAtMillis
        )
    }

    private fun Employee.normalize(): Employee {
        return copy(
            id = id.trim(),
            employeeCode = employeeCode.trim(),
            fullName = fullName.trim(),
            jobTitle = jobTitle.clean(),
            department = department.clean(),
            email = email.clean(),
            phoneNumber = phoneNumber.clean(),
            nationalId = nationalId.clean(),
            userId = userId.clean()
        )
    }

    private fun Employee.matchesManagerId(managerId: String): Boolean {
        val normalizedManagerId = managerId.trim()
        return metadata["managerId"].clean() == normalizedManagerId ||
            metadata["supervisorId"].clean() == normalizedManagerId ||
            userId.clean() == normalizedManagerId
    }

    private fun Map<String, String>.toJsonString(): String {
        return runCatching {
            JSONObject(this).toString()
        }.getOrDefault("{}")
    }

    private fun Set<Permission>.toJsonString(): String {
        return runCatching {
            JSONArray().apply {
                forEach { put(it.name) }
            }.toString()
        }.getOrDefault("[]")
    }

    private fun String?.clean(): String? {
        return this?.trim()?.takeIf { it.isNotBlank() }
    }

    private fun String?.toBigDecimalOrZero(): BigDecimal {
        return this.clean()?.toBigDecimalOrNull() ?: BigDecimal.ZERO
    }

    private fun BigDecimal.money(): String {
        return setScale(2, RoundingMode.HALF_UP).toPlainString()
    }
}