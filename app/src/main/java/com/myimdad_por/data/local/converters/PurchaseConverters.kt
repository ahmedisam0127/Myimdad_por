package com.myimdad_por.data.local.converters

import androidx.room.TypeConverter
import com.myimdad_por.domain.model.PurchaseItem
import com.myimdad_por.domain.model.PurchasePaymentStatus
import com.myimdad_por.domain.model.PurchaseStatus
import com.myimdad_por.domain.model.UnitOfMeasure
import org.json.JSONArray
import org.json.JSONObject
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.UUID

class PurchaseConverters {

    @TypeConverter
    fun fromPurchaseStatus(value: PurchaseStatus?): String? = value?.name

    @TypeConverter
    fun toPurchaseStatus(value: String?): PurchaseStatus? {
        if (value.isNullOrBlank()) return null
        return runCatching { enumValueOf<PurchaseStatus>(value) }.getOrNull()
    }

    @TypeConverter
    fun fromPurchasePaymentStatus(value: PurchasePaymentStatus?): String? = value?.name

    @TypeConverter
    fun toPurchasePaymentStatus(value: String?): PurchasePaymentStatus? {
        if (value.isNullOrBlank()) return null
        return runCatching { enumValueOf<PurchasePaymentStatus>(value) }.getOrNull()
    }

    @TypeConverter
    fun fromPurchaseItems(value: List<PurchaseItem>?): String? = value?.toJsonString()

    @TypeConverter
    fun toPurchaseItems(value: String?): List<PurchaseItem> {
        if (value.isNullOrBlank()) return emptyList()
        return runCatching { value.toPurchaseItemList() }.getOrDefault(emptyList())
    }
}

private fun List<PurchaseItem>.toJsonString(): String {
    return JSONArray().apply {
        forEach { item ->
            put(
                JSONObject().apply {
                    put("id", item.id)
                    put("purchaseId", item.purchaseId)
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

private fun String.toPurchaseItemList(): List<PurchaseItem> {
    val array = JSONArray(this)
    return buildList {
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            add(
                PurchaseItem(
                    id = obj.optString("id").ifBlank { UUID.randomUUID().toString() },
                    purchaseId = obj.optString("purchaseId").takeUnless { it.isBlank() },
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
}

private fun BigDecimal.money(): String = this.setScale(2, RoundingMode.HALF_UP).toPlainString()

private fun String.toBigDecimalOrZero(): BigDecimal {
    return runCatching { BigDecimal(this) }
        .getOrDefault(BigDecimal.ZERO)
        .setScale(2, RoundingMode.HALF_UP)
}