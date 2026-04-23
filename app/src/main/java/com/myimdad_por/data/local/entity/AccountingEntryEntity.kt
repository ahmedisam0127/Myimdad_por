package com.myimdad_por.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "accounting_entries",
    indices = [
        Index(value = ["server_id"], unique = true),
        Index(value = ["reference_id"]),
        Index(value = ["transaction_date_millis"]),
        Index(value = ["debit_account"]),
        Index(value = ["credit_account"]),
        Index(value = ["source"]),
        Index(value = ["status"]),
        Index(value = ["sync_state"])
    ]
)
data class AccountingEntryEntity(
    @PrimaryKey
    @ColumnInfo(name = "entry_id")
    val id: String,

    @ColumnInfo(name = "server_id")
    val serverId: String? = null,

    @ColumnInfo(name = "transaction_date_millis")
    val transactionDateMillis: Long,

    @ColumnInfo(name = "reference_id")
    val referenceId: String? = null,

    @ColumnInfo(name = "description")
    val description: String,

    @ColumnInfo(name = "debit_account")
    val debitAccount: String,

    @ColumnInfo(name = "credit_account")
    val creditAccount: String,

    @ColumnInfo(name = "amount")
    val amount: String,

    @ColumnInfo(name = "currency_code")
    val currencyCode: String,

    @ColumnInfo(name = "payment_method_id")
    val paymentMethodId: String? = null,

    @ColumnInfo(name = "source")
    val source: String,

    @ColumnInfo(name = "status")
    val status: String,

    @ColumnInfo(name = "created_by_employee_id")
    val createdByEmployeeId: String? = null,

    @ColumnInfo(name = "note")
    val note: String? = null,

    @ColumnInfo(name = "is_deleted")
    val isDeleted: Boolean = false,

    @ColumnInfo(name = "sync_state")
    val syncState: String = "PENDING",

    @ColumnInfo(name = "created_at_millis")
    val createdAtMillis: Long = transactionDateMillis,

    @ColumnInfo(name = "updated_at_millis")
    val updatedAtMillis: Long = transactionDateMillis,

    @ColumnInfo(name = "synced_at_millis")
    val syncedAtMillis: Long? = null
)