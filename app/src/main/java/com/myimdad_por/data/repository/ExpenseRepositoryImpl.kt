package com.myimdad_por.data.repository

import com.myimdad_por.core.dispatchers.AppDispatchers
import com.myimdad_por.core.dispatchers.DefaultAppDispatchers
import com.myimdad_por.core.network.NetworkResult
import com.myimdad_por.data.local.dao.ExpenseDao
import com.myimdad_por.data.local.entity.ExpenseEntity
import com.myimdad_por.data.local.entity.toDomain as entityToDomain
import com.myimdad_por.data.mapper.toDomain as dtoToDomain
import com.myimdad_por.data.mapper.toDto
import com.myimdad_por.data.remote.datasource.ExpenseRemoteDataSource
import com.myimdad_por.data.remote.dto.ExpenseDto
import com.myimdad_por.domain.model.Expense
import com.myimdad_por.domain.model.ExpenseCategory
import com.myimdad_por.domain.model.ExpenseStatus
import com.myimdad_por.domain.model.PaymentMethod
import com.myimdad_por.domain.repository.ExpenseBudgetUsage
import com.myimdad_por.domain.repository.ExpenseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDateTime
import java.util.Locale
import java.util.UUID

class ExpenseRepositoryImpl(
    private val expenseDao: ExpenseDao,
    private val remoteDataSource: ExpenseRemoteDataSource? = null,
    private val dispatchers: AppDispatchers = DefaultAppDispatchers
) : ExpenseRepository {

    override fun observeAllExpenses(): Flow<List<Expense>> {
        return expenseDao.observeAll()
            .map { entities -> entities.map { it.entityToDomain() } }
            .flowOn(dispatchers.io)
    }

    override fun observeExpensesByCategory(category: ExpenseCategory): Flow<List<Expense>> {
        return expenseDao.observeByCategory(category.name)
            .map { entities -> entities.map { it.entityToDomain() } }
            .flowOn(dispatchers.io)
    }

    override fun observeExpensesByStatus(status: ExpenseStatus): Flow<List<Expense>> {
        return expenseDao.observeByStatus(status.name)
            .map { entities -> entities.map { it.entityToDomain() } }
            .flowOn(dispatchers.io)
    }

    override fun observePendingExpenses(): Flow<List<Expense>> {
        return expenseDao.observePendingSync()
            .map { entities -> entities.map { it.entityToDomain() } }
            .flowOn(dispatchers.io)
    }

    override fun observeExpensesByEmployee(employeeId: String): Flow<List<Expense>> {
        val normalizedEmployeeId = employeeId.trim()
        if (normalizedEmployeeId.isBlank()) return flowOf(emptyList())

        return expenseDao.observeByEmployee(normalizedEmployeeId)
            .map { entities -> entities.map { it.entityToDomain() } }
            .flowOn(dispatchers.io)
    }

    override suspend fun getExpenseById(id: String): Expense? = withContext(dispatchers.io) {
        val normalizedId = id.trim()
        if (normalizedId.isBlank()) return@withContext null

        expenseDao.getById(normalizedId)?.entityToDomain()
            ?: expenseDao.getByServerId(normalizedId)?.entityToDomain()
            ?: fetchRemoteById(normalizedId)
    }

    override suspend fun getExpenseByNumber(expenseNumber: String): Expense? = withContext(dispatchers.io) {
        val normalizedNumber = expenseNumber.trim()
        if (normalizedNumber.isBlank()) return@withContext null

        expenseDao.getByExpenseNumber(normalizedNumber)?.entityToDomain()
            ?: fetchRemoteByExpenseNumber(normalizedNumber)
    }

    override suspend fun searchExpenses(query: String): List<Expense> = withContext(dispatchers.io) {
        val normalizedQuery = query.trim()
        if (normalizedQuery.isBlank()) return@withContext emptyList()

        val localExpenses = expenseDao.search(normalizedQuery)
            .first()
            .map { it.entityToDomain() }

        if (localExpenses.isNotEmpty() || remoteDataSource == null) {
            return@withContext localExpenses
        }

        val remoteExpenses = when (
            val result = remoteDataSource.listExpenses(
                filters = mapOf("query" to normalizedQuery)
            )
        ) {
            is NetworkResult.Success -> result.data.map { it.dtoToDomain() }
            is NetworkResult.Error,
            NetworkResult.Loading -> emptyList()
        }

        if (remoteExpenses.isNotEmpty()) {
            expenseDao.insertAll(
                remoteExpenses.map { expense ->
                    ExpenseEntity.fromDomain(expense, syncState = "SYNCED")
                }
            )
        }

        remoteExpenses
    }

    override suspend fun getExpenses(
        from: LocalDateTime?,
        to: LocalDateTime?,
        category: ExpenseCategory?,
        status: ExpenseStatus?,
        employeeId: String?,
        supplierName: String?
    ): List<Expense> = withContext(dispatchers.io) {
        val localExpenses = loadAllLocalExpenses().filterByCriteria(
            from = from,
            to = to,
            category = category,
            status = status,
            employeeId = employeeId,
            supplierName = supplierName
        )

        if (localExpenses.isNotEmpty() || remoteDataSource == null) {
            return@withContext localExpenses
        }

        val remoteExpenses = when (
            val result = remoteDataSource.listExpenses(
                status = status?.name,
                category = category?.name,
                supplierName = supplierName?.trimToNull(),
                filters = buildMap {
                    employeeId?.trimToNull()?.let { put("employeeId", it) }
                }
            )
        ) {
            is NetworkResult.Success -> result.data.map { it.dtoToDomain() }
            is NetworkResult.Error,
            NetworkResult.Loading -> emptyList()
        }

        if (remoteExpenses.isNotEmpty()) {
            expenseDao.insertAll(
                remoteExpenses.map { expense ->
                    ExpenseEntity.fromDomain(expense, syncState = "SYNCED")
                }
            )
        }

        remoteExpenses.filterByCriteria(
            from = from,
            to = to,
            category = category,
            status = status,
            employeeId = employeeId,
            supplierName = supplierName
        )
    }

    override suspend fun getExpensesByPaymentMethod(paymentMethod: PaymentMethod): List<Expense> = withContext(dispatchers.io) {
        loadAllLocalExpenses()
            .filter { it.paymentMethod.matches(paymentMethod) }
    }

    override suspend fun saveExpense(expense: Expense): Result<Expense> = withContext(dispatchers.io) {
        runCatching {
            val normalized = expense.normalize()
            val existing = getLocalExpenseForWrite(normalized)

            val entity = ExpenseEntity.fromDomain(
                expense = normalized,
                serverId = existing?.serverId.trimToNull(),
                syncState = "PENDING",
                isDeleted = existing?.isDeleted == true,
                syncedAtMillis = existing?.syncedAtMillis,
                createdAtMillis = existing?.createdAtMillis ?: normalized.expenseDate.toMillis(),
                updatedAtMillis = System.currentTimeMillis()
            )

            expenseDao.insert(entity)

            syncExpenseToRemote(normalized, existing) ?: normalized
        }
    }

    override suspend fun saveExpenses(expenses: List<Expense>): Result<List<Expense>> {
        return withContext(dispatchers.io) {
            runCatching {
                if (expenses.isEmpty()) return@runCatching emptyList()

                expenses.map { expense ->
                    saveExpense(expense).getOrElse { expense }
                }
            }
        }
    }

    override suspend fun updateExpense(expense: Expense): Result<Expense> {
        return saveExpense(expense)
    }

    override suspend fun markExpensePaid(
        expenseId: String,
        paidAmount: BigDecimal?
    ): Result<Expense> = withContext(dispatchers.io) {
        runCatching {
            val current = getLocalExpenseById(expenseId.trim())
                ?: error("Expense not found.")

            val currentDomain = current.entityToDomain()
            val updatedPaidAmount = paidAmount ?: currentDomain.paidAmount

            val updatedDomain = currentDomain.copy(
                paidAmount = updatedPaidAmount,
                status = resolveExpenseStatus("PAID", currentDomain.status)
            )

            expenseDao.insert(
                ExpenseEntity.fromDomain(
                    expense = updatedDomain,
                    serverId = current.serverId.trimToNull(),
                    syncState = "PENDING",
                    isDeleted = current.isDeleted,
                    syncedAtMillis = current.syncedAtMillis,
                    createdAtMillis = current.createdAtMillis,
                    updatedAtMillis = System.currentTimeMillis()
                )
            )

            syncExpenseToRemote(updatedDomain, current) ?: updatedDomain
        }
    }

    override suspend fun markExpenseApproved(expenseId: String): Result<Expense> = withContext(dispatchers.io) {
        runCatching {
            val current = getLocalExpenseById(expenseId.trim())
                ?: error("Expense not found.")

            val currentDomain = current.entityToDomain()
            val updatedDomain = currentDomain.copy(
                status = resolveExpenseStatus("APPROVED", currentDomain.status)
            )

            expenseDao.insert(
                ExpenseEntity.fromDomain(
                    expense = updatedDomain,
                    serverId = current.serverId.trimToNull(),
                    syncState = "PENDING",
                    isDeleted = current.isDeleted,
                    syncedAtMillis = current.syncedAtMillis,
                    createdAtMillis = current.createdAtMillis,
                    updatedAtMillis = System.currentTimeMillis()
                )
            )

            syncExpenseToRemote(updatedDomain, current) ?: updatedDomain
        }
    }

    override suspend fun markExpenseCancelled(expenseId: String): Result<Expense> = withContext(dispatchers.io) {
        runCatching {
            val current = getLocalExpenseById(expenseId.trim())
                ?: error("Expense not found.")

            val currentDomain = current.entityToDomain()
            val updatedDomain = currentDomain.copy(
                status = resolveExpenseStatus("CANCELLED", currentDomain.status)
            )

            expenseDao.insert(
                ExpenseEntity.fromDomain(
                    expense = updatedDomain,
                    serverId = current.serverId.trimToNull(),
                    syncState = "PENDING",
                    isDeleted = current.isDeleted,
                    syncedAtMillis = current.syncedAtMillis,
                    createdAtMillis = current.createdAtMillis,
                    updatedAtMillis = System.currentTimeMillis()
                )
            )

            syncExpenseToRemote(updatedDomain, current) ?: updatedDomain
        }
    }

    override suspend fun deleteExpense(id: String): Result<Unit> = withContext(dispatchers.io) {
        runCatching {
            val normalizedId = id.trim()
            require(normalizedId.isNotBlank()) { "id cannot be blank." }

            val local = getLocalExpenseById(normalizedId)
            val localId = local?.id ?: normalizedId

            expenseDao.softDelete(localId, syncState = "PENDING")

            val remote = remoteDataSource ?: return@runCatching Unit
            val remoteId = local?.serverId.trimToNull() ?: normalizedId

            when (remote.deleteExpense(remoteId)) {
                is NetworkResult.Success -> {
                    expenseDao.markSynced(localId, syncState = "SYNCED")
                }

                is NetworkResult.Error,
                NetworkResult.Loading -> Unit
            }

            Unit
        }
    }

    override suspend fun deleteExpenses(ids: List<String>): Result<Int> = withContext(dispatchers.io) {
        runCatching {
            if (ids.isEmpty()) return@runCatching 0

            var successCount = 0
            ids.asSequence()
                .mapNotNull { it.trimToNull() }
                .distinct()
                .forEach { expenseId ->
                    if (deleteExpense(expenseId).isSuccess) {
                        successCount++
                    }
                }

            successCount
        }
    }

    override suspend fun clearAll(): Result<Unit> = withContext(dispatchers.io) {
        runCatching {
            val localExpenses = expenseDao.observeAll().first()
            localExpenses.forEach { entity ->
                expenseDao.softDelete(entity.id, syncState = "PENDING")
            }
            expenseDao.purgeDeleted()
            Unit
        }
    }

    override suspend fun countExpenses(): Long = withContext(dispatchers.io) {
        expenseDao.countActive().toLong()
    }

    override suspend fun countExpensesByCategory(category: ExpenseCategory): Long = withContext(dispatchers.io) {
        expenseDao.observeByCategory(category.name)
            .first()
            .size
            .toLong()
    }

    override suspend fun countPendingExpenses(): Long = withContext(dispatchers.io) {
        loadAllLocalExpenses()
            .count { it.status.matches(ExpenseStatus.PENDING) }
            .toLong()
    }

    override suspend fun getTotalExpenses(
        from: LocalDateTime?,
        to: LocalDateTime?,
        category: ExpenseCategory?,
        status: ExpenseStatus?,
        employeeId: String?
    ): BigDecimal = withContext(dispatchers.io) {
        loadAllLocalExpenses()
            .filterByCriteria(
                from = from,
                to = to,
                category = category,
                status = status,
                employeeId = employeeId,
                supplierName = null
            )
            .sumOf { it.amount }
            .normalizeMoney()
    }

    override suspend fun getTotalPaidExpenses(
        from: LocalDateTime?,
        to: LocalDateTime?,
        category: ExpenseCategory?
    ): BigDecimal = withContext(dispatchers.io) {
        loadAllLocalExpenses()
            .filterByCriteria(
                from = from,
                to = to,
                category = category,
                status = null,
                employeeId = null,
                supplierName = null
            )
            .sumOf { it.paidAmount }
            .normalizeMoney()
    }

    override suspend fun getBudgetUsage(
        category: ExpenseCategory,
        budgetAmount: BigDecimal,
        from: LocalDateTime?,
        to: LocalDateTime?
    ): ExpenseBudgetUsage = withContext(dispatchers.io) {
        val spentAmount = loadAllLocalExpenses()
            .filterByCriteria(
                from = from,
                to = to,
                category = category,
                status = null,
                employeeId = null,
                supplierName = null
            )
            .sumOf { it.amount }
            .normalizeMoney()

        val safeBudget = budgetAmount.normalizeMoney()
        val remaining = (safeBudget - spentAmount).coerceAtLeast(BigDecimal.ZERO).normalizeMoney()
        val usagePercent = if (safeBudget <= BigDecimal.ZERO) {
            0.0
        } else {
            spentAmount.divide(safeBudget, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal("100"))
                .toDouble()
        }

        ExpenseBudgetUsage(
            category = category,
            budgetAmount = safeBudget,
            spentAmount = spentAmount,
            remainingAmount = remaining,
            usagePercent = usagePercent
        )
    }

    override suspend fun isBudgetExceeded(
        category: ExpenseCategory,
        budgetAmount: BigDecimal,
        from: LocalDateTime?,
        to: LocalDateTime?
    ): Boolean = withContext(dispatchers.io) {
        getBudgetUsage(
            category = category,
            budgetAmount = budgetAmount,
            from = from,
            to = to
        ).isOverBudget
    }

    private suspend fun loadAllLocalExpenses(): List<Expense> {
        return expenseDao.observeAll()
            .first()
            .map { it.entityToDomain() }
    }

    private suspend fun getLocalExpenseById(id: String): ExpenseEntity? {
        return expenseDao.getById(id)
            ?: expenseDao.getByServerId(id)
    }

    private suspend fun getLocalExpenseForWrite(expense: Expense): ExpenseEntity? {
        return expenseDao.getById(expense.id)
            ?: expenseDao.getByServerId(expense.id)
            ?: expenseDao.getByExpenseNumber(expense.expenseNumber)
    }

    private suspend fun fetchRemoteById(id: String): Expense? {
        val remote = remoteDataSource ?: return null

        return when (val result = remote.getExpense(id)) {
            is NetworkResult.Success -> {
                val dto = result.data
                val domain = dto.dtoToDomain()
                expenseDao.insert(ExpenseEntity.fromDomain(domain, syncState = "SYNCED"))
                domain
            }

            is NetworkResult.Error,
            NetworkResult.Loading -> null
        }
    }

    private suspend fun fetchRemoteByExpenseNumber(expenseNumber: String): Expense? {
        val remote = remoteDataSource ?: return null

        return when (
            val result = remote.listExpenses(
                filters = mapOf("expenseNumber" to expenseNumber)
            )
        ) {
            is NetworkResult.Success -> {
                val dto = result.data.firstOrNull { it.expenseNumber == expenseNumber } ?: return null
                val domain = dto.dtoToDomain()
                expenseDao.insert(ExpenseEntity.fromDomain(domain, syncState = "SYNCED"))
                domain
            }

            is NetworkResult.Error,
            NetworkResult.Loading -> null
        }
    }

    private suspend fun syncExpenseToRemote(
        expense: Expense,
        local: ExpenseEntity?
    ): Expense? {
        val remote = remoteDataSource ?: return null
        val remoteId = local?.serverId.trimToNull()

        return if (remoteId == null) {
            when (val result = remote.createExpense(expense.toDto())) {
                is NetworkResult.Success -> {
                    val syncedDto = result.data
                    val syncedDomain = syncedDto.dtoToDomain()
                    expenseDao.insert(
                        ExpenseEntity.fromDomain(
                            expense = syncedDomain,
                            serverId = syncedDto.id,
                            syncState = "SYNCED",
                            isDeleted = false,
                            syncedAtMillis = System.currentTimeMillis()
                        )
                    )
                    syncedDomain
                }

                is NetworkResult.Error,
                NetworkResult.Loading -> null
            }
        } else {
            when (
                val result = remote.updateExpense(
                    id = remoteId,
                    request = expense.toDto()
                )
            ) {
                is NetworkResult.Success -> {
                    val syncedDto = result.data
                    val syncedDomain = syncedDto.dtoToDomain()
                    expenseDao.insert(
                        ExpenseEntity.fromDomain(
                            expense = syncedDomain,
                            serverId = remoteId,
                            syncState = "SYNCED",
                            isDeleted = false,
                            syncedAtMillis = System.currentTimeMillis()
                        )
                    )
                    syncedDomain
                }

                is NetworkResult.Error,
                NetworkResult.Loading -> null
            }
        }
    }

    private fun List<Expense>.filterByCriteria(
        from: LocalDateTime?,
        to: LocalDateTime?,
        category: ExpenseCategory?,
        status: ExpenseStatus?,
        employeeId: String?,
        supplierName: String?
    ): List<Expense> {
        val normalizedEmployeeId = employeeId.trimToNull()
        val normalizedSupplierName = supplierName.trimToNull()

        return asSequence()
            .filter { from == null || !it.expenseDate.isBefore(from) }
            .filter { to == null || !it.expenseDate.isAfter(to) }
            .filter { category == null || it.category.name.equals(category.name, ignoreCase = true) }
            .filter { status == null || it.status.name.equals(status.name, ignoreCase = true) }
            .filter { normalizedEmployeeId == null || it.employeeId.clean() == normalizedEmployeeId }
            .filter {
                normalizedSupplierName == null ||
                    it.supplierName.clean()?.lowercase(Locale.ROOT) == normalizedSupplierName.lowercase(Locale.ROOT)
            }
            .toList()
    }

    private fun Expense.normalize(): Expense {
        return copy(
            id = id.trim(),
            expenseNumber = expenseNumber.trim(),
            title = title.trim(),
            referenceNumber = referenceNumber.trimToNull(),
            supplierName = supplierName.trimToNull(),
            employeeId = employeeId.trimToNull(),
            note = note.trimToNull()
        )
    }

    private fun PaymentMethod.matches(other: PaymentMethod): Boolean {
        return this.id.equals(other.id, ignoreCase = true)
    }

    private fun ExpenseStatus.matches(other: ExpenseStatus): Boolean {
        return this.name.equals(other.name, ignoreCase = true)
    }

    private fun String?.trimToNull(): String? {
        return this?.trim()?.takeIf { it.isNotBlank() }
    }

    private fun String?.clean(): String? {
        return this?.trim()?.takeIf { it.isNotBlank() }
    }

    private fun BigDecimal.normalizeMoney(): BigDecimal {
        return setScale(2, RoundingMode.HALF_UP)
    }

    private fun LocalDateTime.toMillis(): Long {
        return atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    private fun resolveExpenseStatus(name: String, fallback: ExpenseStatus): ExpenseStatus {
        return runCatching {
            enumValueOf<ExpenseStatus>(name.uppercase(Locale.ROOT))
        }.getOrDefault(fallback)
    }
}