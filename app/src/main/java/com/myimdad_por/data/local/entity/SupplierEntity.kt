package com.myimdad_por.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.myimdad_por.domain.model.Supplier
import org.json.JSONObject
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.UUID

@Entity(
    tableName = "suppliers",
    indices = [
        Index(value = ["server_id"], unique = true),
        Index(value = ["supplier_code"], unique = true),
        Index(value = ["company_name"]),
        Index(value = ["contact_person"]),
        Index(value = ["phone_number"]),
        Index(value = ["email"]),
        Index(value = ["city"]),
        Index(value = ["country"]),
        Index(value = ["tax_number"]),
        Index(value = ["is_active"]),
        Index(value = ["sync_state"])
    ]
)
data class SupplierEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "server_id")
    val serverId: String? = null,

    @ColumnInfo(name = "supplier_code")
    val supplierCode: String? = null,

    @ColumnInfo(name = "company_name")
    val companyName: String,

    @ColumnInfo(name = "contact_person")
    val contactPerson: String? = null,

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

    @ColumnInfo(name = "commercial_register_number")
    val commercialRegisterNumber: String? = null,

    @ColumnInfo(name = "bank_account_number")
    val bankAccountNumber: String? = null,

    @ColumnInfo(name = "credit_limit")
    val creditLimit: String = "0.00",

    @ColumnInfo(name = "outstanding_balance")
    val outstandingBalance: String = "0.00",

    @ColumnInfo(name = "payment_terms_days")
    val paymentTermsDays: Int = 0,

    @ColumnInfo(name = "is_preferred")
    val isPreferred: Boolean = false,

    @ColumnInfo(name = "is_active")
    val isActive: Boolean = true,

    @ColumnInfo(name = "last_supply_at_millis")
    val lastSupplyAtMillis: Long? = null,

    @ColumnInfo(name = "metadata_json")
    val metadataJson: String = "{}",

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
        require(companyName.isNotBlank()) { "companyName cannot be blank." }
        require(paymentTermsDays >= 0) { "paymentTermsDays cannot be negative." }
    }

    val displayName: String
        get() = companyName.trim()

    val availableCredit: BigDecimal
        get() = creditLimit.toBigDecimalOrZero()
            .subtract(outstandingBalance.toBigDecimalOrZero())
            .coerceAtLeast(BigDecimal.ZERO)

    companion object {
        fun fromDomain(
            supplier: Supplier,
            serverId: String? = null,
            syncState: String = "PENDING",
            isDeleted: Boolean = false,
            syncedAtMillis: Long? = null,
            createdAtMillis: Long = supplier.createdAtMillis,
            updatedAtMillis: Long = supplier.updatedAtMillis
        ): SupplierEntity {
            return SupplierEntity(
                id = supplier.id,
                serverId = serverId,
                supplierCode = supplier.supplierCode,
                companyName = supplier.companyName,
                contactPerson = supplier.contactPerson,
                phoneNumber = supplier.phoneNumber,
                email = supplier.email,
                address = supplier.address,
                city = supplier.city,
                country = supplier.country,
                taxNumber = supplier.taxNumber,
                commercialRegisterNumber = supplier.commercialRegisterNumber,
                bankAccountNumber = supplier.bankAccountNumber,
                creditLimit = supplier.creditLimit.money(),
                outstandingBalance = supplier.outstandingBalance.money(),
                paymentTermsDays = supplier.paymentTermsDays,
                isPreferred = supplier.isPreferred,
                isActive = supplier.isActive,
                lastSupplyAtMillis = supplier.lastSupplyAtMillis,
                metadataJson = supplier.metadata.toJsonString(),
                syncState = syncState,
                isDeleted = isDeleted,
                syncedAtMillis = syncedAtMillis,
                createdAtMillis = createdAtMillis,
                updatedAtMillis = updatedAtMillis
            )
        }
    }
}

fun SupplierEntity.toDomain(): Supplier {
    return Supplier(
        id = id,
        supplierCode = supplierCode,
        companyName = companyName,
        contactPerson = contactPerson,
        phoneNumber = phoneNumber,
        email = email,
        address = address,
        city = city,
        country = country,
        taxNumber = taxNumber,
        commercialRegisterNumber = commercialRegisterNumber,
        bankAccountNumber = bankAccountNumber,
        creditLimit = creditLimit.toBigDecimalOrZero(),
        outstandingBalance = outstandingBalance.toBigDecimalOrZero(),
        paymentTermsDays = paymentTermsDays,
        isPreferred = isPreferred,
        isActive = isActive,
        createdAtMillis = createdAtMillis,
        updatedAtMillis = updatedAtMillis,
        lastSupplyAtMillis = lastSupplyAtMillis,
        metadata = metadataJson.toStringMap()
    )
}

private fun BigDecimal.money(): String = this.setScale(2, RoundingMode.HALF_UP).toPlainString()

private fun String.toBigDecimalOrZero(): BigDecimal {
    return runCatching { BigDecimal(this) }
        .getOrDefault(BigDecimal.ZERO)
        .setScale(2, RoundingMode.HALF_UP)
}

private fun Map<String, String>.toJsonString(): String {
    return JSONObject(this).toString()
}

private fun String.toStringMap(): Map<String, String> {
    if (isBlank()) return emptyMap()
    return runCatching {
        val json = JSONObject(this)
        buildMap {
            json.keys().forEach { key ->
                put(key, json.optString(key))
            }
        }
    }.getOrDefault(emptyMap())
}