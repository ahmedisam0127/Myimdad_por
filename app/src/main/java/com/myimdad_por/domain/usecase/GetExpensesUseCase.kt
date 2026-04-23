package com.myimdad_por.domain.usecase

import com.myimdad_por.domain.model.Expense
import com.myimdad_por.domain.model.ExpenseCategory
import com.myimdad_por.domain.model.ExpenseStatus
import com.myimdad_por.domain.model.PaymentMethod
import com.myimdad_por.domain.repository.ExpenseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.math.BigDecimal
import java.time.LocalDateTime
import javax.inject.Inject

data class ExpenseQuery(
    val from: LocalDateTime? = null,
    val to: LocalDateTime? = null,
    val category: ExpenseCategory? = null,
    val status: ExpenseStatus? = null,
    val employeeId: String? = null,
    val supplierName: String? = null,
    val paymentMethod: PaymentMethod? = null,
    val pendingOnly: Boolean = false,
    val text: String? = null
)

class GetExpensesUseCase @Inject constructor(
    private val expenseRepository: ExpenseRepository
) {

    suspend operator fun invoke(query: ExpenseQuery = ExpenseQuery()): List<Expense> {
        val effectiveStatus = when {
            query.pendingOnly && query.status == null -> ExpenseStatus.PENDING
            else -> query.status
        }

        val base = if (query.from == null &&
            query.to == null &&
            query.category == null &&
            effectiveStatus == null &&
            query.employeeId == null &&
            query.supplierName == null &&
            query.paymentMethod == null &&
            query.text.isNullOrBlank() &&
            !query.pendingOnly
        ) {
            expenseRepository.observeAllExpenses().first()
        } else {
            expenseRepository.getExpenses(
                from = query.from,
                to = query.to,
                category = query.category,
                status = effectiveStatus,
                employeeId = query.employeeId,
                supplierName = query.supplierName
            )
        }

        return base
            .filter { query.paymentMethod == null || it.paymentMethod == query.paymentMethod }
            .filter { query.text.isNullOrBlank() || matchesText(it, query.text) }
    }

    fun observeAll(): Flow<List<Expense>> = expenseRepository.observeAllExpenses()

    fun observeByCategory(category: ExpenseCategory): Flow<List<Expense>> =
        expenseRepository.observeExpensesByCategory(category)

    fun observeByStatus(status: ExpenseStatus): Flow<List<Expense>> =
        expenseRepository.observeExpensesByStatus(status)

    fun observePending(): Flow<List<Expense>> = expenseRepository.observePendingExpenses()

    fun observeByEmployee(employeeId: String): Flow<List<Expense>> {
        require(employeeId.isNotBlank()) { "employeeId cannot be blank" }
        return expenseRepository.observeExpensesByEmployee(employeeId)
    }

    suspend fun getById(id: String): Expense? {
        require(id.isNotBlank()) { "id cannot be blank" }
        return expenseRepository.getExpenseById(id)
    }

    suspend fun getByNumber(expenseNumber: String): Expense? {
        require(expenseNumber.isNotBlank()) { "expenseNumber cannot be blank" }
        return expenseRepository.getExpenseByNumber(expenseNumber)
    }

    suspend fun search(query: String): List<Expense> {
        require(query.isNotBlank()) { "query cannot be blank" }
        return expenseRepository.searchExpenses(query)
    }

    suspend fun getByPaymentMethod(paymentMethod: PaymentMethod): List<Expense> {
        return expenseRepository.getExpensesByPaymentMethod(paymentMethod)
    }

    suspend fun countAll(): Long = expenseRepository.countExpenses()

    suspend fun countByCategory(category: ExpenseCategory): Long =
        expenseRepository.countExpensesByCategory(category)

    suspend fun countPending(): Long = expenseRepository.countPendingExpenses()

    suspend fun total(query: ExpenseQuery = ExpenseQuery()): BigDecimal {
        return expenseRepository.getTotalExpenses(
            from = query.from,
            to = query.to,
            category = query.category,
            status = if (query.pendingOnly && query.status == null) ExpenseStatus.PENDING else query.status,
            employeeId = query.employeeId
        )
    }

    suspend fun totalPaid(query: ExpenseQuery = ExpenseQuery()): BigDecimal {
        return expenseRepository.getTotalPaidExpenses(
            from = query.from,
            to = query.to,
            category = query.category
        )
    }

    suspend fun budgetUsage(
        category: ExpenseCategory,
        budgetAmount: BigDecimal,
        from: LocalDateTime? = null,
        to: LocalDateTime? = null
    ) = expenseRepository.getBudgetUsage(category, budgetAmount, from, to)

    suspend fun isBudgetExceeded(
        category: ExpenseCategory,
        budgetAmount: BigDecimal,
        from: LocalDateTime? = null,
        to: LocalDateTime? = null
    ): Boolean = expenseRepository.isBudgetExceeded(category, budgetAmount, from, to)

    private fun matchesText(expense: Expense, text: String): Boolean {
        val q = text.trim()
        return expense.id.contains(q, ignoreCase = true) ||
            expense.expenseNumber.contains(q, ignoreCase = true) ||
            expense.title.contains(q, ignoreCase = true) ||
            expense.supplierName?.contains(q, ignoreCase = true) == true ||
            expense.note?.contains(q, ignoreCase = true) == true
    }
}