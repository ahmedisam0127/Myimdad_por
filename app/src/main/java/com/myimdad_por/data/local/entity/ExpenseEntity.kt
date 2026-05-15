package com.myimdad_por.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.myimdad_por.domain.model.Expense
import com.myimdad_por.domain.model.ExpenseCategory
import com.myimdad_por.domain.model.ExpenseStatus
import com.myimdad_por.domain.model.PaymentMethod
import com.myimdad_por.domain.model.PaymentMethodType
import com.myimdad_por.domain.model.PaymentStatus
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.UUID

@Entity(
    tableName = "expenses",
    indices = [
        Index(value = ["server_id"], unique = true),
        Index(value = ["expense_number"], unique = true),
        Index(value = ["category"]),
        Index(value = ["status"]),
        Index(value = ["payment_status"]),
        Index(value = ["expense_date_millis"]),
        Index(value = ["employee_id"]),
        Index(value = ["sync_state"])
    ]
)
data class ExpenseEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "server_id")
    val serverId: String? = null,

    @ColumnInfo(name = "expense_number")
    val expenseNumber: String,

    @ColumnInfo(name = "category")
    val category: String,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "amount")
    val amount: String,

    @ColumnInfo(name = "expense_date_millis")
    val expenseDateMillis: Long,

    @ColumnInfo(name = "paid_amount")
    val paidAmount: String = "0.00",

    @ColumnInfo(name = "payment_method")
    val paymentMethod: String = PaymentMethod.CASH.id,

    @ColumnInfo(name = "status")
    val status: String = ExpenseStatus.PENDING.name,

    @ColumnInfo(name = "payment_status")
    val paymentStatus: String = PaymentStatus.PENDING.name,

    @ColumnInfo(name = "reference_number")
    val referenceNumber: String? = null,

    @ColumnInfo(name = "supplier_name")
    val supplierName: String? = null,

    @ColumnInfo(name = "employee_id")
    val employeeId: String? = null,

    @ColumnInfo(name = "note")
    val note: String? = null,

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
        require(expenseNumber.isNotBlank()) { "expenseNumber cannot be blank." }
        require(title.isNotBlank()) { "title cannot be blank." }
        require(expenseDateMillis > 0L) { "expenseDateMillis must be greater than zero." }
    }

    companion object {
        fun fromDomain(
            expense: Expense,
            serverId: String? = null,
            syncState: String = "PENDING",
            isDeleted: Boolean = false,
            syncedAtMillis: Long? = null,
            createdAtMillis: Long = System.currentTimeMillis(),
            updatedAtMillis: Long = System.currentTimeMillis()
        ): ExpenseEntity {
            return ExpenseEntity(
                id = expense.id,
                serverId = serverId,
                expenseNumber = expense.expenseNumber,
                category = expense.category.name,
                title = expense.title,
                amount = expense.amount.money(),
                expenseDateMillis = expense.expenseDate.toMillis(),
                paidAmount = expense.paidAmount.money(),
                paymentMethod = expense.paymentMethod.id,
                status = expense.status.name,
                paymentStatus = expense.paymentStatus.name,
                referenceNumber = expense.referenceNumber,
                supplierName = expense.supplierName,
                employeeId = expense.employeeId,
                note = expense.note,
                syncState = syncState,
                isDeleted = isDeleted,
                syncedAtMillis = syncedAtMillis,
                createdAtMillis = createdAtMillis,
                updatedAtMillis = updatedAtMillis
            )
        }
    }
}

fun ExpenseEntity.toDomain(): Expense {
    return Expense(
        id = id,
        expenseNumber = expenseNumber,
        category = runCatching { enumValueOf<ExpenseCategory>(category) }
            .getOrDefault(ExpenseCategory.OTHER),
        title = title,
        amount = amount.toBigDecimalOrZero(),
        expenseDate = expenseDateMillis.toLocalDateTime(),
        paidAmount = paidAmount.toBigDecimalOrZero(),
        paymentMethod = paymentMethod.toPaymentMethod(),
        status = runCatching { enumValueOf<ExpenseStatus>(status) }
            .getOrDefault(ExpenseStatus.PENDING),
        referenceNumber = referenceNumber,
        supplierName = supplierName,
        employeeId = employeeId,
        note = note
    )
}

private fun String.toPaymentMethod(): PaymentMethod {
    return when (trim().lowercase()) {
        PaymentMethod.CASH.id -> PaymentMethod.CASH
        PaymentMethod.BANK_TRANSFER.id -> PaymentMethod.BANK_TRANSFER
        PaymentMethod.WALLET.id -> PaymentMethod.WALLET
        PaymentMethod.POS.id -> PaymentMethod.POS
        PaymentMethod.CHEQUE.id -> PaymentMethod.CHEQUE
        else -> PaymentMethod.Custom(
            name = takeIf { it.isNotBlank() } ?: PaymentMethod.CASH.name,
            type = PaymentMethodType.OTHER
        )
    }
}

private fun BigDecimal.money(): String = this.setScale(2, RoundingMode.HALF_UP).toPlainString()

private fun String.toBigDecimalOrZero(): BigDecimal {
    return runCatching { BigDecimal(this) }
        .getOrDefault(BigDecimal.ZERO)
        .setScale(2, RoundingMode.HALF_UP)
}

private fun LocalDateTime.toMillis(): Long {
    return this.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
}

private fun Long.toLocalDateTime(): LocalDateTime {
    return LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(this), ZoneId.systemDefault())
}