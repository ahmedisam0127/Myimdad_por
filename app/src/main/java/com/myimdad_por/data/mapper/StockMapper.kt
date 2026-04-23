package com.myimdad_por.data.mapper

import com.myimdad_por.data.remote.dto.StockDto
import com.myimdad_por.domain.model.StockItem
import com.myimdad_por.domain.model.UnitOfMeasure
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

fun StockDto.toDomain(): StockItem {
    return StockItem(
        productBarcode = productBarcode,
        productName = productName,
        // تحويل النص إلى Double مباشرة ليتوافق مع الـ Domain Model عندك
        quantity = quantity.toDoubleSafe(), 
        location = location,
        unitOfMeasure = runCatching {
            UnitOfMeasure.valueOf(unitOfMeasure.trim().uppercase(Locale.ROOT))
        }.getOrDefault(UnitOfMeasure.UNIT),
        displayName = displayName,
        expiryDate = expiryDateMillis?.toLocalDate()
    )
}

fun StockItem.toDto(
    stockId: String? = null,
    serverId: String? = null,
    movementType: String = "ADJUSTMENT",
    movementQuantity: Double? = null, // تغيير النوع ليتوافق مع Double
    movementReferenceId: String? = null,
    movementReason: String? = null,
    note: String? = null,
    syncState: String = "PENDING",
    isDeleted: Boolean = false
): StockDto {
    return StockDto(
        stockId = stockId ?: this.defaultStockId(),
        serverId = serverId,
        productBarcode = productBarcode,
        normalizedBarcode = normalizedBarcode,
        productName = productName,
        displayName = displayName,
        location = location,
        normalizedLocation = normalizedLocation,
        unitOfMeasure = unitOfMeasure.name,
        quantity = quantity.toMoneyString(), // استخدام الدالة المحدثة للـ Double
        expiryDateMillis = expiryDate?.toMillis(),
        movementType = movementType,
        movementQuantity = (movementQuantity ?: quantity).toMoneyString(),
        movementReferenceId = movementReferenceId,
        movementReason = movementReason,
        note = note,
        syncState = syncState,
        isDeleted = isDeleted
    )
}

// دالات مساعدة محدثة للتعامل مع Double
private fun String.toDoubleSafe(): Double = this.toDoubleOrNull() ?: 0.0

private fun Double.toMoneyString(): String = 
    BigDecimal.valueOf(this).setScale(3, RoundingMode.HALF_UP).toPlainString()

private fun StockItem.defaultStockId(): String {
    return "${normalizedBarcode.lowercase(Locale.ROOT)}@${normalizedLocation.lowercase(Locale.ROOT)}"
}

private fun LocalDate.toMillis(): Long = atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

private fun Long.toLocalDate(): LocalDate = Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate()
