package com.myimdad_por.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.myimdad_por.domain.model.StockItem
import com.myimdad_por.domain.model.UnitOfMeasure
import org.json.JSONObject
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

@Entity(
    tableName = "stocks",
    indices = [
        Index(value = ["server_id"], unique = true),
        Index(value = ["normalized_barcode"]),
        Index(value = ["normalized_location"]),
        Index(value = ["product_name"]),
        Index(value = ["unit_of_measure"]),
        Index(value = ["expiry_date_millis"]),
        Index(value = ["sync_state"]),
        Index(value = ["normalized_barcode", "normalized_location"], unique = true)
    ]
)
data class StockEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "server_id")
    val serverId: String? = null,

    @ColumnInfo(name = "product_barcode")
    val productBarcode: String,

    @ColumnInfo(name = "normalized_barcode")
    val normalizedBarcode: String,

    @ColumnInfo(name = "product_name")
    val productName: String,

    @ColumnInfo(name = "display_name")
    val displayName: String? = null,

    @ColumnInfo(name = "location")
    val location: String,

    @ColumnInfo(name = "normalized_location")
    val normalizedLocation: String,

    @ColumnInfo(name = "unit_of_measure")
    val unitOfMeasure: String = UnitOfMeasure.UNIT.name,

    @ColumnInfo(name = "quantity")
    val quantity: Double,

    @ColumnInfo(name = "expiry_date_millis")
    val expiryDateMillis: Long? = null,

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
        require(productBarcode.isNotBlank()) { "productBarcode cannot be blank." }
        require(normalizedBarcode.isNotBlank()) { "normalizedBarcode cannot be blank." }
        require(productName.isNotBlank()) { "productName cannot be blank." }
        require(location.isNotBlank()) { "location cannot be blank." }
        require(normalizedLocation.isNotBlank()) { "normalizedLocation cannot be blank." }
        require(quantity.isFinite()) { "quantity must be finite." }
        require(quantity >= 0.0) { "quantity cannot be negative." }
    }

    val effectiveName: String
        get() = displayName?.trim().takeUnless { it.isNullOrBlank() } ?: productName.trim()

    companion object {
        fun fromDomain(
            stockItem: StockItem,
            serverId: String? = null,
            id: String = defaultStockId(stockItem.productBarcode, stockItem.location),
            syncState: String = "PENDING",
            isDeleted: Boolean = false,
            syncedAtMillis: Long? = null,
            createdAtMillis: Long = System.currentTimeMillis(),
            updatedAtMillis: Long = System.currentTimeMillis()
        ): StockEntity {
            val normalizedBarcode = stockItem.normalizedBarcode
            val normalizedLocation = stockItem.normalizedLocation

            return StockEntity(
                id = id,
                serverId = serverId,
                productBarcode = stockItem.productBarcode,
                normalizedBarcode = normalizedBarcode,
                productName = stockItem.productName,
                displayName = stockItem.displayName,
                location = stockItem.location,
                normalizedLocation = normalizedLocation,
                unitOfMeasure = stockItem.unitOfMeasure.name,
                quantity = stockItem.quantity,
                expiryDateMillis = stockItem.expiryDate?.toMillis(),
                metadataJson = "{}",
                syncState = syncState,
                isDeleted = isDeleted,
                syncedAtMillis = syncedAtMillis,
                createdAtMillis = createdAtMillis,
                updatedAtMillis = updatedAtMillis
            )
        }

        private fun defaultStockId(barcode: String, location: String): String {
            return "${barcode.trim().lowercase()}@${location.trim().lowercase()}"
        }
    }
}

fun StockEntity.toDomain(): StockItem {
    return StockItem(
        productBarcode = productBarcode,
        productName = productName,
        quantity = quantity,
        location = location,
        unitOfMeasure = runCatching { enumValueOf<UnitOfMeasure>(unitOfMeasure) }
            .getOrDefault(UnitOfMeasure.UNIT),
        displayName = displayName,
        expiryDate = expiryDateMillis?.toLocalDate()
    )
}

private fun LocalDate.toMillis(): Long {
    return this.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
}

private fun Long.toLocalDate(): LocalDate {
    return java.time.Instant.ofEpochMilli(this)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
}