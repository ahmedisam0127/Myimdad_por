package com.myimdad_por.data.mapper

import com.myimdad_por.core.utils.toBigDecimalOrZero
import com.myimdad_por.data.remote.dto.PurchaseDto
import com.myimdad_por.domain.model.Purchase
import com.myimdad_por.domain.model.PurchaseItem
import com.myimdad_por.domain.model.PurchasePaymentStatus
import com.myimdad_por.domain.model.PurchaseStatus
import com.myimdad_por.domain.model.UnitOfMeasure
import org.json.JSONArray
import org.json.JSONObject
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.UUID
import java.util.Locale

fun Purchase.toDto(
    serverId: String? = null,
    syncState: String = "PENDING",
    isDeleted: Boolean = false,
    syncedAtMillis: Long? = null,
    updatedAtMillis: Long = System.currentTimeMillis()
): PurchaseDto {
    return PurchaseDto(
        id = id,
        serverId = serverId,
        invoiceNumber = invoiceNumber,
        supplierId = supplierId,
        supplierName = supplierName,
        employeeId = employeeId,
        itemsJson = items.toJsonString(id),
        subtotalAmount = subtotalAmount.money(),
        taxAmount = taxAmount.money(),
        discountAmount = discountAmount.money(),
        totalAmount = totalAmount.money(),
        paidAmount = paidAmount.money(),
        remainingAmount = remainingAmount.money(),
        status = status.name,
        paymentStatus = paymentStatus.name,
        createdAtMillis = createdAt.toMillis(),
        dueDateMillis = dueDate?.toMillis(),
        note = note,
        metadataJson = "{}",
        syncState = syncState,
        isDeleted = isDeleted,
        syncedAtMillis = syncedAtMillis,
        updatedAtMillis = updatedAtMillis
    )
}

fun PurchaseDto.toDomain(): Purchase {
    val parsedItems = itemsJson.toPurchaseItemList(id)
    require(parsedItems.isNotEmpty()) { "itemsJson produced an empty items list." }

    return Purchase(
        id = id,
        invoiceNumber = invoiceNumber,
        supplierId = supplierId,
        supplierName = supplierName,
        employeeId = employeeId,
        items = parsedItems,
        createdAt = createdAtMillis.toLocalDateTime(),
        dueDate = dueDateMillis?.toLocalDateTime(),
        paidAmount = paidAmount.toBigDecimalOrZero(),
        status = runCatching { PurchaseStatus.valueOf(status.trim().uppercase(Locale.ROOT)) }
            .getOrDefault(PurchaseStatus.DRAFT),
        note = note
    )
}

fun List<Purchase>.toDtoList(): List<PurchaseDto> = map { it.toDto() }

fun List<PurchaseDto>.toDomainList(): List<Purchase> = map { it.toDomain() }

private fun List<PurchaseItem>.toJsonString(defaultPurchaseId: String): String {
    return JSONArray().apply {
        forEach { item ->
            put(
                JSONObject().apply {
                    put("id", item.id)
                    put("purchaseId", item.purchaseId ?: defaultPurchaseId)
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
                        productBarcode = obj.optString("productBarcode").takeIf { it.isNotBlank() },
                        productName = obj.optString("productName"),
                        unit = runCatching {
                            enumValueOf<UnitOfMeasure>(obj.optString("unit"))
                        }.getOrDefault(UnitOfMeasure.UNIT),
                        quantity = obj.optString("quantity").toBigDecimalOrZero(),
                        unitCost = obj.optString("unitCost").toBigDecimalOrZero(),
                        taxAmount = obj.optString("taxAmount").toBigDecimalOrZero(),
                        discountAmount = obj.optString("discountAmount").toBigDecimalOrZero(),
                        note = obj.optString("note").takeIf { it.isNotBlank() }
                    )
                )
            }
        }
    }.getOrDefault(emptyList())
}

private fun BigDecimal.money(): String =
    setScale(2, RoundingMode.HALF_UP).toPlainString()

private fun LocalDateTime.toMillis(): Long {
    return atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
}

private fun Long.toLocalDateTime(): LocalDateTime {
    return LocalDateTime.ofInstant(Instant.ofEpochMilli(this), ZoneId.systemDefault())
}