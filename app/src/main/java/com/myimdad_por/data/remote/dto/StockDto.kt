package com.myimdad_por.data.remote.dto

import java.util.Locale

/**
 * DTO (Data Transfer Object) 
 * يمثل شكل البيانات القادمة من أو الذاهبة إلى الـ API.
 * تم إبقاء المنطق فيه مقتصرًا على الـ Validation فقط.
 */
data class StockDto(
    val stockId: String,
    val serverId: String? = null,
    val productBarcode: String,
    val normalizedBarcode: String,
    val productName: String,
    val displayName: String? = null,
    val location: String,
    val normalizedLocation: String,
    val unitOfMeasure: String,
    val quantity: String,
    val expiryDateMillis: Long? = null,
    val movementType: String = "ADJUSTMENT",
    val movementQuantity: String = "0.00",
    val movementReferenceId: String? = null,
    val movementReason: String? = null,
    val note: String? = null,
    val metadataJson: String = "{}",
    val syncState: String = "PENDING",
    val isDeleted: Boolean = false,
    val syncedAtMillis: Long? = null,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis()
) {
    init {
        // التحقق من صحة البيانات (Data Integrity)
        require(stockId.isNotBlank()) { "stockId cannot be blank." }
        require(productBarcode.isNotBlank()) { "productBarcode cannot be blank." }
        require(normalizedBarcode.isNotBlank()) { "normalizedBarcode cannot be blank." }
        require(productName.isNotBlank()) { "productName cannot be blank." }
        require(location.isNotBlank()) { "location cannot be blank." }
        require(normalizedLocation.isNotBlank()) { "normalizedLocation cannot be blank." }
        require(quantity.isNotBlank()) { "quantity cannot be blank." }
        require(unitOfMeasure.isNotBlank()) { "unitOfMeasure cannot be blank." }
        
        require(unitOfMeasure == unitOfMeasure.uppercase(Locale.ROOT)) {
            "unitOfMeasure must be uppercase."
        }

        displayName?.let { require(it.isNotBlank()) { "displayName cannot be blank." } }
    }

    val normalizedUnitOfMeasure: String
        get() = unitOfMeasure.trim().uppercase(Locale.ROOT)
}
