package com.myimdad_por.domain.model

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDateTime
import java.util.UUID

/**
 * سجل مصروفات.
 * يمثل أي خرج مالي على النشاط التجاري.
 */
data class Expense(
    val id: String = UUID.randomUUID().toString(),
    val expenseNumber: String,
    val category: ExpenseCategory,
    val title: String,
    val amount: BigDecimal,
    val expenseDate: LocalDateTime = LocalDateTime.now(),

    val paidAmount: BigDecimal = BigDecimal.ZERO,
    val paymentMethod: PaymentMethod = PaymentMethod.CASH,
    val status: ExpenseStatus = ExpenseStatus.PENDING,

    val referenceNumber: String? = null,
    val supplierName: String? = null,
    val employeeId: String? = null,
    val note: String? = null
) {

    init {
        require(expenseNumber.isNotBlank()) { "expenseNumber cannot be blank" }
        require(title.isNotBlank()) { "title cannot be blank" }
        require(amount >= BigDecimal.ZERO) { "amount cannot be negative" }
        require(paidAmount >= BigDecimal.ZERO) { "paidAmount cannot be negative" }
        require(expenseDate.isBefore(LocalDateTime.now().plusSeconds(5))) {
            "expenseDate cannot be in the future"
        }
        require(paidAmount <= amount) { "paidAmount cannot exceed amount" }
    }

    val remainingAmount: BigDecimal
        get() = amount.subtract(paidAmount).money()

    val paymentStatus: PaymentStatus
        get() = when {
            amount <= BigDecimal.ZERO -> PaymentStatus.PAID
            remainingAmount == BigDecimal.ZERO && paidAmount > BigDecimal.ZERO -> PaymentStatus.PAID
            paidAmount > BigDecimal.ZERO -> PaymentStatus.PARTIALLY_PAID
            else -> PaymentStatus.PENDING
        }

    fun isFullyPaid(): Boolean = paymentStatus == PaymentStatus.PAID

    fun isPartialPayment(): Boolean = paymentStatus == PaymentStatus.PARTIALLY_PAID

    fun withPayment(amount: BigDecimal): Expense {
        require(amount >= BigDecimal.ZERO) { "amount cannot be negative" }

        val updatedPaidAmount = paidAmount.add(amount)
        require(updatedPaidAmount <= this.amount) {
            "paidAmount cannot exceed expense amount"
        }

        return copy(paidAmount = updatedPaidAmount.money())
    }

    fun markPaid(): Expense = copy(
        status = ExpenseStatus.PAID,
        paidAmount = amount.money()
    )

    fun markCancelled(): Expense = copy(status = ExpenseStatus.CANCELLED)

    fun markApproved(): Expense = copy(status = ExpenseStatus.APPROVED)
}

enum class ExpenseCategory {
    RENT,
    SALARY,
    UTILITIES,
    TRANSPORT,
    MAINTENANCE,
    PURCHASE,
    TAX,
    MARKETING,
    OFFICE,
    OTHER
}

enum class ExpenseStatus {
    PENDING,
    APPROVED,
    PAID,
    CANCELLED
}


private fun BigDecimal.money(): BigDecimal = this.setScale(2, RoundingMode.HALF_UP)
