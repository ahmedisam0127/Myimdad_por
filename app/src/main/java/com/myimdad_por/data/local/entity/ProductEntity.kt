package com.myimdad_por.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.myimdad_por.domain.model.Product
import com.myimdad_por.domain.model.UnitOfMeasure
import org.json.JSONObject
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.UUID

@Entity(
    tableName = "products",
    indices = [
        Index(value = ["server_id"], unique = true),
        Index(value = ["barcode"], unique = true),
        Index(value = ["normalized_barcode"], unique = true),
        Index(value = ["name"]),
        Index(value = ["display_name"]),
        Index(value = ["unit_of_measure"]),
        Index(value = ["is_active"]),
        Index(value = ["sync_state"])
    ]
)
data class ProductEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "server_id")
    val serverId: String? = null,

    @ColumnInfo(name = "barcode")
    val barcode: String,

    @ColumnInfo(name = "normalized_barcode")
    val normalizedBarcode: String,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "display_name")
    val displayName: String? = null,

    @ColumnInfo(name = "description")
    val description: String? = null,

    @ColumnInfo(name = "unit_of_measure")
    val unitOfMeasure: String = UnitOfMeasure.UNIT.name,

    @ColumnInfo(name = "price")
    val price: String,

    @ColumnInfo(name = "is_active")
    val isActive: Boolean = true,

    @ColumnInfo(name = "search_tokens")
    val searchTokens: String = "",

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
        require(barcode.isNotBlank()) { "barcode cannot be blank." }
        require(normalizedBarcode.isNotBlank()) { "normalizedBarcode cannot be blank." }
        require(name.isNotBlank()) { "name cannot be blank." }
    }

    val effectiveName: String
        get() = displayName?.trim().takeUnless { it.isNullOrBlank() } ?: name.trim()

    companion object {
        fun fromDomain(
            product: Product,
            serverId: String? = null,
            syncState: String = "PENDING",
            isDeleted: Boolean = false,
            syncedAtMillis: Long? = null,
            createdAtMillis: Long = System.currentTimeMillis(),
            updatedAtMillis: Long = System.currentTimeMillis()
        ): ProductEntity {
            val normalizedBarcode = product.normalizedBarcode
            return ProductEntity(
                id = UUID.randomUUID().toString(),
                serverId = serverId,
                barcode = product.barcode,
                normalizedBarcode = normalizedBarcode,
                name = product.name,
                displayName = product.displayName,
                description = product.description,
                unitOfMeasure = product.unitOfMeasure.name,
                price = product.price.money(),
                isActive = product.isActive,
                searchTokens = buildSearchTokens(
                    barcode = normalizedBarcode,
                    name = product.name,
                    displayName = product.displayName,
                    description = product.description
                ),
                metadataJson = "{}",
                syncState = syncState,
                isDeleted = isDeleted,
                syncedAtMillis = syncedAtMillis,
                createdAtMillis = createdAtMillis,
                updatedAtMillis = updatedAtMillis
            )
        }

        private fun buildSearchTokens(
            barcode: String,
            name: String,
            displayName: String?,
            description: String?
        ): String {
            return listOfNotNull(
                barcode.trim().lowercase(),
                name.trim().lowercase(),
                displayName?.trim()?.lowercase()?.takeIf { it.isNotBlank() },
                description?.trim()?.lowercase()?.takeIf { it.isNotBlank() }
            ).distinct().joinToString(" ")
        }
    }
}

fun ProductEntity.toDomain(): Product {
    return Product(
        barcode = barcode,
        name = name,
        price = price.toBigDecimalOrZero(),
        unitOfMeasure = runCatching { UnitOfMeasure.valueOf(unitOfMeasure) }.getOrDefault(UnitOfMeasure.UNIT),
        displayName = displayName,
        description = description,
        isActive = isActive
    )
}

private fun BigDecimal.money(): String = this.setScale(2, RoundingMode.HALF_UP).toPlainString()

private fun String.toBigDecimalOrZero(): BigDecimal {
    return runCatching { BigDecimal(this) }
        .getOrDefault(BigDecimal.ZERO)
        .setScale(2, RoundingMode.HALF_UP)
}