package com.myimdad_por.data.mapper

import com.myimdad_por.core.payment.models.RefundRequest
import com.myimdad_por.data.remote.dto.RefundDto
import com.myimdad_por.data.remote.dto.ReturnDto
import com.myimdad_por.domain.model.Return
import com.myimdad_por.domain.model.ReturnItem
import com.myimdad_por.domain.model.ReturnStatus
import com.myimdad_por.domain.model.ReturnType
import com.myimdad_por.domain.model.UnitOfMeasure
import org.json.JSONArray
import org.json.JSONObject
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Locale
import java.util.UUID

fun Return.toDto(
    serverId: String? = null,
    refundStatus: String = "PENDING",
    syncState: String = "PENDING",
    isDeleted: Boolean = false,
    syncedAtMillis: Long? = null,
    createdAtMillis: Long = returnDate.toMillis(),
    updatedAtMillis: Long = System.currentTimeMillis()
): ReturnDto {
    return ReturnDto(
        returnId = id,
        serverId = serverId,
        returnNumber = returnNumber,
        returnType = returnType.name,
        originalDocumentId = originalDocumentId,
        originalDocumentNumber = originalDocumentNumber,
        partyId = partyId,
        partyName = partyName,
        processedByEmployeeId = processedByEmployeeId,
        itemsJson = items.toJsonString(id),
        returnDateMillis = returnDate.toMillis(),
        status = status.name,
        refundStatus = refundStatus,
        subtotalAmount = subtotalAmount.money(),
        taxAmount = taxAmount.money(),
        discountAmount = discountAmount.money(),
        totalRefundAmount = totalRefundAmount.money(),
        refundedAmount = refundedAmount.money(),
        remainingRefundAmount = remainingRefundAmount.money(),
        reason = reason,
        note = note,
        metadataJson = "{}",
        syncState = syncState,
        isDeleted = isDeleted,
        syncedAtMillis = syncedAtMillis,
        createdAtMillis = createdAtMillis,
        updatedAtMillis = updatedAtMillis
    )
}

fun ReturnDto.toDomain(): Return {
    val parsedItems = itemsJson.toReturnItemList(returnId)
    require(parsedItems.isNotEmpty()) { "itemsJson produced an empty items list." }

    return Return(
        id = returnId,
        returnNumber = returnNumber,
        returnType = runCatching {
            ReturnType.valueOf(returnType.trim().uppercase(Locale.ROOT))
        }.getOrDefault(ReturnType.SALE_RETURN),
        originalDocumentId = originalDocumentId,
        originalDocumentNumber = originalDocumentNumber,
        partyId = partyId,
        partyName = partyName,
        processedByEmployeeId = processedByEmployeeId,
        items = parsedItems,
        returnDate = returnDateMillis.toLocalDateTime(),
        status = runCatching {
            ReturnStatus.valueOf(status.trim().uppercase(Locale.ROOT))
        }.getOrDefault(ReturnStatus.DRAFT),
        refundedAmount = refundedAmount.toBigDecimalOrZero(),
        reason = reason,
        note = note
    )
}

fun RefundRequest.toRefundDto(
    serverId: String? = null,
    returnId: String? = null,
    status: String = "PENDING",
    verified: Boolean = false,
    verifiedAtMillis: Long? = null,
    syncedAtMillis: Long? = null,
    createdAtMillis: Long = System.currentTimeMillis(),
    updatedAtMillis: Long = System.currentTimeMillis()
): RefundDto {
    val normalizedCurrency = currency?.trim()?.uppercase(Locale.ROOT).takeIf { !it.isNullOrBlank() } ?: "SDG"

    return RefundDto(
        refundId = refundId ?: UUID.randomUUID().toString(),
        serverId = serverId,
        returnId = returnId,
        transactionId = transactionId,
        amount = amount?.money() ?: "0.00",
        currencyCode = normalizedCurrency,
        status = status,
        providerReference = null,
        idempotencyKey = idempotencyKey,
        requestedBy = requestedBy,
        verified = verified,
        verifiedAtMillis = verifiedAtMillis,
        reason = reason,
        metadataJson = metadata.toJsonString(),
        syncState = "PENDING",
        isDeleted = false,
        syncedAtMillis = syncedAtMillis,
        createdAtMillis = createdAtMillis,
        updatedAtMillis = updatedAtMillis
    )
}

fun RefundDto.toRefundRequest(): RefundRequest {
    return RefundRequest(
        transactionId = transactionId,
        amount = amount.toBigDecimalOrZero().takeIf { it > BigDecimal.ZERO },
        currency = currencyCode.takeIf { it.isNotBlank() },
        reason = reason,
        refundId = refundId,
        idempotencyKey = idempotencyKey,
        requestedBy = requestedBy,
        metadata = metadataJson.toStringMap()
    )
}

fun List<Return>.toDtoList(): List<ReturnDto> = map { it.toDto() }

fun List<ReturnDto>.toDomainList(): List<Return> = map { it.toDomain() }

private fun List<ReturnItem>.toJsonString(defaultReturnId: String): String {
    return JSONArray().apply {
        forEach { item ->
            put(
                JSONObject().apply {
                    put("id", item.id)
                    put("returnId", item.returnId ?: defaultReturnId)
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
                        originalItemId = obj.optString("originalItemId").takeIf { it.isNotBlank() },
                        productId = obj.optString("productId"),
                        productBarcode = obj.optString("productBarcode").takeIf { it.isNotBlank() },
                        productName = obj.optString("productName"),
                        unit = runCatching {
                            enumValueOf<UnitOfMeasure>(obj.optString("unit"))
                        }.getOrDefault(UnitOfMeasure.UNIT),
                        quantity = obj.optString("quantity").toBigDecimalOrZero(),
                        unitPriceAtSource = obj.optString("unitPriceAtSource").toBigDecimalOrZero(),
                        taxAmount = obj.optString("taxAmount").toBigDecimalOrZero(),
                        discountAmount = obj.optString("discountAmount").toBigDecimalOrZero(),
                        isRestockable = obj.optBoolean("isRestockable", true),
                        note = obj.optString("note").takeIf { it.isNotBlank() }
                    )
                )
            }
        }
    }.getOrDefault(emptyList())
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

private fun String.toBigDecimalOrZero(): BigDecimal {
    return runCatching { BigDecimal(this.trim()) }
        .getOrDefault(BigDecimal.ZERO)
        .setScale(2, RoundingMode.HALF_UP)
}

private fun BigDecimal.money(): String =
    setScale(2, RoundingMode.HALF_UP).toPlainString()

private fun LocalDateTime.toMillis(): Long {
    return atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
}

private fun Long.toLocalDateTime(): LocalDateTime {
    return LocalDateTime.ofInstant(Instant.ofEpochMilli(this), ZoneId.systemDefault())
}