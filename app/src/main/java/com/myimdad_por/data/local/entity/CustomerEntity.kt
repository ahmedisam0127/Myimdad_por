package com.myimdad_por.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "customers",
    indices = [
        Index(value = ["server_id"], unique = true),
        Index(value = ["code"], unique = true),
        Index(value = ["full_name"]),
        Index(value = ["trade_name"]),
        Index(value = ["phone_number"]),
        Index(value = ["email"]),
        Index(value = ["national_id"]),
        Index(value = ["tax_number"]),
        Index(value = ["is_active"]),
        Index(value = ["sync_state"])
    ]
)
data class CustomerEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "server_id")
    val serverId: String? = null,

    @ColumnInfo(name = "code")
    val code: String? = null,

    @ColumnInfo(name = "full_name")
    val fullName: String,

    @ColumnInfo(name = "trade_name")
    val tradeName: String? = null,

    @ColumnInfo(name = "phone_number")
    val phoneNumber: String? = null,

    @ColumnInfo(name = "email")
    val email: String? = null,

    @ColumnInfo(name = "address")
    val address: String? = null,

    @ColumnInfo(name = "city")
    val city: String? = null,

    @ColumnInfo(name = "country")
    val country: String? = null,

    @ColumnInfo(name = "tax_number")
    val taxNumber: String? = null,

    @ColumnInfo(name = "national_id")
    val nationalId: String? = null,

    @ColumnInfo(name = "credit_limit")
    val creditLimit: String,

    @ColumnInfo(name = "outstanding_balance")
    val outstandingBalance: String,

    @ColumnInfo(name = "is_active")
    val isActive: Boolean = true,

    @ColumnInfo(name = "created_at_millis")
    val createdAtMillis: Long,

    @ColumnInfo(name = "updated_at_millis")
    val updatedAtMillis: Long,

    @ColumnInfo(name = "last_purchase_at_millis")
    val lastPurchaseAtMillis: Long? = null,

    @ColumnInfo(name = "sync_state")
    val syncState: String = "PENDING",

    @ColumnInfo(name = "is_deleted")
    val isDeleted: Boolean = false,

    @ColumnInfo(name = "synced_at_millis")
    val syncedAtMillis: Long? = null,

    @ColumnInfo(name = "metadata_json")
    val metadataJson: String? = null
)