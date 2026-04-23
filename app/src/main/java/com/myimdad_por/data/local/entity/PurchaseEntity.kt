package com.myimdad_por.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.myimdad_por.domain.model.Purchase
import com.myimdad_por.domain.model.PurchaseItem
import com.myimdad_por.domain.model.PurchasePaymentStatus
import com.myimdad_por.domain.model.PurchaseStatus
import com.myimdad_por.domain.model.UnitOfMeasure
import org.json.JSONArray
import org.json.JSONObject
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.UUID

@Entity(
    tableName = "purchases",
    indices = [
        Index(value = ["server_id"], unique = true),
        Index(value = ["invoice_number"], unique = true),
        Index(value = ["supplier_id"]),
        Index(value = ["supplier_name"]),
        Index(value = ["employee_id"]),
        Index(value = ["status"]),
        Index(value = ["payment_status"]),
        Index(value = ["created_at_millis"]),
        Index(value = ["due_date_millis"]),
        Index(value = ["sync_state"])
    ]
)
data class PurchaseEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "server_id")
    val serverId: String? = null,

    @ColumnInfo(name = "invoice_number")
    val invoiceNumber: String,

    @ColumnInfo(name = "supplier_id")
    val supplierId: String,

    @ColumnInfo(name = "supplier_name")
    val supplierName: String,

    @ColumnInfo(name = "employee_id")
    val employeeId: String,

    @ColumnInfo(name = "items_json")
    val itemsJson: String,

    @ColumnInfo(name = "subtotal_amount")
    val subtotalAmount: String,

    @ColumnInfo(name = "tax_amount")
    val taxAmount: String,

    @ColumnInfo(name = "discount_amount")
    val discountAmount: String,

    @ColumnInfo(name = "total_amount")
    val totalAmount: String,

    @ColumnInfo(name = "paid_amount")
    val paidAmount: String = "0.00",

    @ColumnInfo(name = "remaining_amount")
    val remainingAmount: String,

    @ColumnInfo(name = "status")
    val status: String = PurchaseStatus.DRAFT.name,

    @ColumnInfo(name = "payment_status")
    val paymentStatus: String = PurchasePaymentStatus.PENDING.name,

    @ColumnInfo(name = "created_at_millis")
    val createdAtMillis: Long,

    @ColumnInfo(name = "due_date_millis")
    val dueDateMillis: Long? = null,

    @ColumnInfo(name = "note")
    val note: String? = null,

    @ColumnInfo(name = "metadata_json")
    val metadataJson: String = "{}",

    @ColumnInfo(name = "sync_state")
    val syncState: String = "PENDING",

    @ColumnInfo(name = "is_deleted")
    val isDeleted: Boolean = false,

    @ColumnInfo(name = "synced_at_millis")
    val syncedAtMillis: Long? = null,

    @ColumnInfo(name = "updated_at_millis")
    val updatedAtMillis: Long = System.currentTimeMillis()
) {
    init {
        require(id.isNotBlank()) { "id cannot be blank." }
        require(invoiceNumber.isNotBlank()) { "invoiceNumber cannot be blank." }
        require(supplierId.isNotBlank()) { "supplierId cannot be blank." }
        require(supplierName.isNotBlank()) { "supplierName cannot be blank." }
        require(employeeId.isNotBlank()) { "employeeId cannot be blank." }
        require(itemsJson.isNotBlank()) { "itemsJson cannot be blank." }
        require(createdAtMillis > 0L) { "createdAtMillis must be greater than zero." }
    }

    companion object {
        fun fromDomain(
            purchase: Purchase,
            serverId: String? = null,
            syncState: String = "PENDING",
            isDeleted: Boolean = false,
            syncedAtMillis: Long? = null,
            updatedAtMillis: Long = System.currentTimeMillis()
        ): PurchaseEntity {
            val subtotal = purchase.subtotalAmount.money()
            val tax = purchase.taxAmount.money()
            val discount = purchase.discountAmount.money()
            val total = purchase.totalAmount.money()
            val paid = purchase.paidAmount.money()
            val remaining = purchase.remainingAmount.money()

            return PurchaseEntity(
                id = purchase.id,
                serverId = serverId,
                invoiceNumber = purchase.invoiceNumber,
                supplierId = purchase.supplierId,
                supplierName = purchase.supplierName,
                employeeId = purchase.employeeId,
                itemsJson = purchase.items.toJsonString(purchase.id),
                subtotalAmount = subtotal,
                taxAmount = tax,
                discountAmount = discount,
                totalAmount = total,
                paidAmount = paid,
                remainingAmount = remaining,
                status = purchase.status.name,
                paymentStatus = purchase.paymentStatus.name,
                createdAtMillis = purchase.createdAt.toMillis(),
                dueDateMillis = purchase.dueDate?.toMillis(),
                note = purchase.note,
                metadataJson = "{}",
                syncState = syncState,
                isDeleted = isDeleted,
                syncedAtMillis = syncedAtMillis,
                updatedAtMillis = updatedAtMillis
            )
        }
    }
}

fun PurchaseEntity.toDomain(): Purchase {
    val purchaseItems = itemsJson.toPurchaseItemList(id)
    require(purchaseItems.isNotEmpty()) { "itemsJson produced an empty items list." }

    return Purchase(
        id = id,
        invoiceNumber = invoiceNumber,
        supplierId = supplierId,
        supplierName = supplierName,
        employeeId = employeeId,
        items = purchaseItems,
        createdAt = createdAtMillis.toLocalDateTime(),
        dueDate = dueDateMillis?.toLocalDateTime(),
        paidAmount = paidAmount.toBigDecimalOrZero(),
        status = runCatching { enumValueOf<PurchaseStatus>(status) }.getOrDefault(PurchaseStatus.DRAFT),
        note = note
    )
}

private fun List<PurchaseItem>.toJsonString(purchaseId: String): String {
    return JSONArray().apply {
        forEach { item ->
            put(
                JSONObject().apply {
                    put("id", item.id)
                    put("purchaseId", item.purchaseId ?: purchaseId)
                    put("productId", item.productId)
                    put("productBarcode", item.productBarcode)
                    put("productName", item.productName)
                    put("unit", item.unit.name)
                    put("quantity", item.quantity.money())
                    put("unitCost", item.unitCost.money())
                    put("taxAmount", item.taxAmount.money())
                    put("discountAmount", item.discountAmount.money())
                    put("note", item.note)
                }
            )
        }
    }.toString()
}

private fun String.toPurchaseItemList(defaultPurchaseId: String): List<PurchaseItem> {
    if (isBlank()) return emptyList()
    return runCatching {
        val array = JSONArray(this)
        buildList {
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                add(
                    PurchaseItem(
                        id = obj.optString("id").ifBlank { UUID.randomUUID().toString() },
                        purchaseId = obj.optString("purchaseId").ifBlank { defaultPurchaseId },
                        productId = obj.optString("productId"),
                        productBarcode = obj.optString("productBarcode").takeUnless { it.isBlank() },
                        productName = obj.optString("productName"),
                        unit = runCatching { enumValueOf<UnitOfMeasure>(obj.optString("unit")) }
                            .getOrDefault(UnitOfMeasure.UNIT),
                        quantity = obj.optString("quantity").toBigDecimalOrZero(),
                        unitCost = obj.optString("unitCost").toBigDecimalOrZero(),
                        taxAmount = obj.optString("taxAmount").toBigDecimalOrZero(),
                        discountAmount = obj.optString("discountAmount").toBigDecimalOrZero(),
                        note = obj.optString("note").takeUnless { it.isBlank() }
                    )
                )
            }
        }
    }.getOrDefault(emptyList())
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