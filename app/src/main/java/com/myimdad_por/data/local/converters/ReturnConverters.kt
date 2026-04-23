package com.myimdad_por.data.local.converters

import androidx.room.TypeConverter
import com.myimdad_por.domain.model.RefundStatus
import com.myimdad_por.domain.model.ReturnItem
import com.myimdad_por.domain.model.ReturnStatus
import com.myimdad_por.domain.model.ReturnType
import com.myimdad_por.domain.model.UnitOfMeasure
import org.json.JSONArray
import org.json.JSONObject
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.UUID

class ReturnConverters {

    @TypeConverter
    fun fromReturnType(value: ReturnType?): String? = value?.name

    @TypeConverter
    fun toReturnType(value: String?): ReturnType? {
        if (value.isNullOrBlank()) return null
        return runCatching { enumValueOf<ReturnType>(value) }.getOrNull()
    }

    @TypeConverter
    fun fromReturnStatus(value: ReturnStatus?): String? = value?.name

    @TypeConverter
    fun toReturnStatus(value: String?): ReturnStatus? {
        if (value.isNullOrBlank()) return null
        return runCatching { enumValueOf<ReturnStatus>(value) }.getOrNull()
    }

    @TypeConverter
    fun fromRefundStatus(value: RefundStatus?): String? = value?.name

    @TypeConverter
    fun toRefundStatus(value: String?): RefundStatus? {
        if (value.isNullOrBlank()) return null
        return runCatching { enumValueOf<RefundStatus>(value) }.getOrNull()
    }

    @TypeConverter
    fun fromReturnItems(value: List<ReturnItem>?): String? = value?.toJsonString()

    @TypeConverter
    fun toReturnItems(value: String?): List<ReturnItem> {
        if (value.isNullOrBlank()) return emptyList()
        return runCatching { value.toReturnItemList() }.getOrDefault(emptyList())
    }
}

private fun List<ReturnItem>.toJsonString(): String {
    return JSONArray().apply {
        forEach { item ->
            put(
                JSONObject().apply {
                    put("id", item.id)
                    put("returnId", item.returnId)
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

private fun String.toReturnItemList(): List<ReturnItem> {
    val array = JSONArray(this)
    return buildList {
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            add(
                ReturnItem(
                    id = obj.optString("id").ifBlank { UUID.randomUUID().toString() },
                    returnId = obj.optString("returnId").takeUnless { it.isBlank() },
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
}

private fun BigDecimal.money(): String = this.setScale(2, RoundingMode.HALF_UP).toPlainString()

private fun String.toBigDecimalOrZero(): BigDecimal {
    return runCatching { BigDecimal(this) }
        .getOrDefault(BigDecimal.ZERO)
        .setScale(2, RoundingMode.HALF_UP)
}