package com.myimdad_por.domain.repository

import com.myimdad_por.domain.model.Expense
import com.myimdad_por.domain.model.ExpenseCategory
import com.myimdad_por.domain.model.ExpenseStatus
import com.myimdad_por.domain.model.PaymentMethod
import java.math.BigDecimal
import java.time.LocalDateTime
import kotlinx.coroutines.flow.Flow

/**
 * Contract for expense tracking, filtering, and budgeting.
 */
interface ExpenseRepository {

    fun observeAllExpenses(): Flow<List<Expense>>

    fun observeExpensesByCategory(category: ExpenseCategory): Flow<List<Expense>>

    fun observeExpensesByStatus(status: ExpenseStatus): Flow<List<Expense>>

    fun observePendingExpenses(): Flow<List<Expense>>

    fun observeExpensesByEmployee(employeeId: String): Flow<List<Expense>>

    suspend fun getExpenseById(id: String): Expense?

    suspend fun getExpenseByNumber(expenseNumber: String): Expense?

    suspend fun searchExpenses(query: String): List<Expense>

    suspend fun getExpenses(
        from: LocalDateTime? = null,
        to: LocalDateTime? = null,
        category: ExpenseCategory? = null,
        status: ExpenseStatus? = null,
        employeeId: String? = null,
        supplierName: String? = null
    ): List<Expense>

    suspend fun getExpensesByPaymentMethod(
        paymentMethod: PaymentMethod
    ): List<Expense>

    suspend fun saveExpense(expense: Expense): Result<Expense>

    suspend fun saveExpenses(expenses: List<Expense>): Result<List<Expense>>

    suspend fun updateExpense(expense: Expense): Result<Expense>

    suspend fun markExpensePaid(
        expenseId: String,
        paidAmount: BigDecimal? = null
    ): Result<Expense>

    suspend fun markExpenseApproved(expenseId: String): Result<Expense>

    suspend fun markExpenseCancelled(expenseId: String): Result<Expense>

    suspend fun deleteExpense(id: String): Result<Unit>

    suspend fun deleteExpenses(ids: List<String>): Result<Int>

    suspend fun clearAll(): Result<Unit>

    suspend fun countExpenses(): Long

    suspend fun countExpensesByCategory(category: ExpenseCategory): Long

    suspend fun countPendingExpenses(): Long

    suspend fun getTotalExpenses(
        from: LocalDateTime? = null,
        to: LocalDateTime? = null,
        category: ExpenseCategory? = null,
        status: ExpenseStatus? = null,
        employeeId: String? = null
    ): BigDecimal

    suspend fun getTotalPaidExpenses(
        from: LocalDateTime? = null,
        to: LocalDateTime? = null,
        category: ExpenseCategory? = null
    ): BigDecimal

    suspend fun getBudgetUsage(
        category: ExpenseCategory,
        budgetAmount: BigDecimal,
        from: LocalDateTime? = null,
        to: LocalDateTime? = null
    ): ExpenseBudgetUsage

    suspend fun isBudgetExceeded(
        category: ExpenseCategory,
        budgetAmount: BigDecimal,
        from: LocalDateTime? = null,
        to: LocalDateTime? = null
    ): Boolean
}

/**
 * Budget snapshot for a category inside a specific time window.
 */
data class ExpenseBudgetUsage(
    val category: ExpenseCategory,
    val budgetAmount: BigDecimal,
    val spentAmount: BigDecimal,
    val remainingAmount: BigDecimal,
    val usagePercent: Double
) {
    init {
        require(budgetAmount >= BigDecimal.ZERO) { "budgetAmount cannot be negative." }
        require(spentAmount >= BigDecimal.ZERO) { "spentAmount cannot be negative." }
        require(remainingAmount >= BigDecimal.ZERO) { "remainingAmount cannot be negative." }
        require(usagePercent.isFinite()) { "usagePercent must be finite." }
    }

    val isOverBudget: Boolean
        get() = spentAmount > budgetAmount
}