package com.myimdad_por.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.myimdad_por.domain.model.RefundStatus
import com.myimdad_por.domain.model.Return
import com.myimdad_por.domain.model.ReturnItem
import com.myimdad_por.domain.model.ReturnStatus
import com.myimdad_por.domain.model.ReturnType
import com.myimdad_por.domain.model.UnitOfMeasure
import org.json.JSONArray
import org.json.JSONObject
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.UUID

@Entity(
    tableName = "returns",
    indices = [
        Index(value = ["server_id"], unique = true),
        Index(value = ["return_number"], unique = true),
        Index(value = ["return_type"]),
        Index(value = ["status"]),
        Index(value = ["refund_status"]),
        Index(value = ["party_id"]),
        Index(value = ["processed_by_employee_id"]),
        Index(value = ["return_date_millis"]),
        Index(value = ["sync_state"])
    ]
)
data class ReturnEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "server_id")
    val serverId: String? = null,

    @ColumnInfo(name = "return_number")
    val returnNumber: String,

    @ColumnInfo(name = "return_type")
    val returnType: String,

    @ColumnInfo(name = "original_document_id")
    val originalDocumentId: String? = null,

    @ColumnInfo(name = "original_document_number")
    val originalDocumentNumber: String? = null,

    @ColumnInfo(name = "party_id")
    val partyId: String? = null,

    @ColumnInfo(name = "party_name")
    val partyName: String? = null,

    @ColumnInfo(name = "processed_by_employee_id")
    val processedByEmployeeId: String,

    @ColumnInfo(name = "items_json")
    val itemsJson: String,

    @ColumnInfo(name = "return_date_millis")
    val returnDateMillis: Long,

    @ColumnInfo(name = "status")
    val status: String = ReturnStatus.DRAFT.name,

    @ColumnInfo(name = "refund_status")
    val refundStatus: String = RefundStatus.PENDING.name,

    @ColumnInfo(name = "subtotal_amount")
    val subtotalAmount: String,

    @ColumnInfo(name = "tax_amount")
    val taxAmount: String,

    @ColumnInfo(name = "discount_amount")
    val discountAmount: String,

    @ColumnInfo(name = "total_refund_amount")
    val totalRefundAmount: String,

    @ColumnInfo(name = "refunded_amount")
    val refundedAmount: String = "0.00",

    @ColumnInfo(name = "remaining_refund_amount")
    val remainingRefundAmount: String,

    @ColumnInfo(name = "reason")
    val reason: String? = null,

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
        require(returnNumber.isNotBlank()) { "returnNumber cannot be blank." }
        require(returnType.isNotBlank()) { "returnType cannot be blank." }
        require(processedByEmployeeId.isNotBlank()) { "processedByEmployeeId cannot be blank." }
        require(itemsJson.isNotBlank()) { "itemsJson cannot be blank." }
        require(returnDateMillis > 0L) { "returnDateMillis must be greater than zero." }
    }

    companion object {
        fun fromDomain(
            returnDoc: Return,
            serverId: String? = null,
            syncState: String = "PENDING",
            isDeleted: Boolean = false,
            syncedAtMillis: Long? = null,
            updatedAtMillis: Long = System.currentTimeMillis()
        ): ReturnEntity {
            val subtotal = returnDoc.subtotalAmount.money()
            val tax = returnDoc.taxAmount.money()
            val discount = returnDoc.discountAmount.money()
            val total = returnDoc.totalRefundAmount.money()
            val refunded = returnDoc.refundedAmount.money()
            val remaining = returnDoc.remainingRefundAmount.money()

            return ReturnEntity(
                id = returnDoc.id,
                serverId = serverId,
                returnNumber = returnDoc.returnNumber,
                returnType = returnDoc.returnType.name,
                originalDocumentId = returnDoc.originalDocumentId,
                originalDocumentNumber = returnDoc.originalDocumentNumber,
                partyId = returnDoc.partyId,
                partyName = returnDoc.partyName,
                processedByEmployeeId = returnDoc.processedByEmployeeId,
                itemsJson = returnDoc.items.toJsonString(returnDoc.id),
                returnDateMillis = returnDoc.returnDate.toMillis(),
                status = returnDoc.status.name,
                refundStatus = returnDoc.refundStatus.name,
                subtotalAmount = subtotal,
                taxAmount = tax,
                discountAmount = discount,
                totalRefundAmount = total,
                refundedAmount = refunded,
                remainingRefundAmount = remaining,
                reason = returnDoc.reason,
                note = returnDoc.note,
                metadataJson = "{}",
                syncState = syncState,
                isDeleted = isDeleted,
                syncedAtMillis = syncedAtMillis,
                updatedAtMillis = updatedAtMillis
            )
        }
    }
}

fun ReturnEntity.toDomain(): Return {
    val returnItems = itemsJson.toReturnItemList(id)
    require(returnItems.isNotEmpty()) { "itemsJson produced an empty items list." }

    return Return(
        id = id,
        returnNumber = returnNumber,
        returnType = runCatching { enumValueOf<ReturnType>(returnType) }.getOrDefault(ReturnType.SALE_RETURN),
        originalDocumentId = originalDocumentId,
        originalDocumentNumber = originalDocumentNumber,
        partyId = partyId,
        partyName = partyName,
        processedByEmployeeId = processedByEmployeeId,
        items = returnItems,
        returnDate = returnDateMillis.toLocalDateTime(),
        status = runCatching { enumValueOf<ReturnStatus>(status) }.getOrDefault(ReturnStatus.DRAFT),
        refundedAmount = refundedAmount.toBigDecimalOrZero(),
        reason = reason,
        note = note
    )
}

private fun List<ReturnItem>.toJsonString(returnId: String): String {
    return JSONArray().apply {
        forEach { item ->
            put(
                JSONObject().apply {
                    put("id", item.id)
                    put("returnId", item.returnId ?: returnId)
                    put("originalItemId", item.originalItemId)
                    put("productId", item.productId)
                    put("productBarcode", item.productBarcode)
                    put("productName", item.productName)
                    put("unit", item.unit.name)
                    put("quantity", item.quantity.money())
                    put("unitPriceAtSource", item.unitPriceAtSource.money())
                    put("taxAmount", item.taxAmount.money())
                    put("discountAmount", item.discountAmount.money())
                    put("isRestockable", item.isRestockable)
                    put("note", item.note)
                }
            )
        }
    }.toString()
}

private fun String.toReturnItemList(defaultReturnId: String): List<ReturnItem> {
    if (isBlank()) return emptyList()
    return runCatching {
        val array = JSONArray(this)
        buildList {
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                add(
                    ReturnItem(
                        id = obj.optString("id").ifBlank { UUID.randomUUID().toString() },
                        returnId = obj.optString("returnId").ifBlank { defaultReturnId },
                        originalItemId = obj.optString("originalItemId").takeUnless { it.isBlank() },
                        productId = obj.optString("productId"),
                        productBarcode = obj.optString("productBarcode").takeUnless { it.isBlank() },
                        productName = obj.optString("productName"),
                        unit = runCatching { enumValueOf<UnitOfMeasure>(obj.optString("unit")) }
                            .getOrDefault(UnitOfMeasure.UNIT),
                        quantity = obj.optString("quantity").toBigDecimalOrZero(),
                        unitPriceAtSource = obj.optString("unitPriceAtSource").toBigDecimalOrZero(),
                        taxAmount = obj.optString("taxAmount").toBigDecimalOrZero(),
                        discountAmount = obj.optString("discountAmount").toBigDecimalOrZero(),
                        isRestockable = obj.optBoolean("isRestockable", true),
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