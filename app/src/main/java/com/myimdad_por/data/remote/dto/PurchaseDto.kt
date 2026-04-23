package com.myimdad_por.data.remote.dto

import com.myimdad_por.domain.model.PurchasePaymentStatus
import com.myimdad_por.domain.model.PurchaseStatus
import java.util.Locale

data class PurchaseDto(
    val id: String,
    val serverId: String? = null,
    val invoiceNumber: String,
    val supplierId: String,
    val supplierName: String,
    val employeeId: String,
    val itemsJson: String = "[]",
    val subtotalAmount: String,
    val taxAmount: String,
    val discountAmount: String,
    val totalAmount: String,
    val paidAmount: String = "0.00",
    val remainingAmount: String,
    val status: String = PurchaseStatus.DRAFT.name,
    val paymentStatus: String = PurchasePaymentStatus.PENDING.name,
    val createdAtMillis: Long,
    val dueDateMillis: Long? = null,
    val note: String? = null,
    val metadataJson: String = "{}",
    val syncState: String = "PENDING",
    val isDeleted: Boolean = false,
    val syncedAtMillis: Long? = null,
    val updatedAtMillis: Long = System.currentTimeMillis()
) {
    init {
        require(id.isNotBlank()) { "id cannot be blank." }
        require(invoiceNumber.isNotBlank()) { "invoiceNumber cannot be blank." }
        require(supplierId.isNotBlank()) { "supplierId cannot be blank." }
        require(supplierName.isNotBlank()) { "supplierName cannot be blank." }
        require(employeeId.isNotBlank()) { "employeeId cannot be blank." }
        require(itemsJson.isNotBlank()) { "itemsJson cannot be blank." }
        require(subtotalAmount.isNotBlank()) { "subtotalAmount cannot be blank." }
        require(taxAmount.isNotBlank()) { "taxAmount cannot be blank." }
        require(discountAmount.isNotBlank()) { "discountAmount cannot be blank." }
        require(totalAmount.isNotBlank()) { "totalAmount cannot be blank." }
        require(paidAmount.isNotBlank()) { "paidAmount cannot be blank." }
        require(remainingAmount.isNotBlank()) { "remainingAmount cannot be blank." }
        require(createdAtMillis > 0L) { "createdAtMillis must be greater than zero." }
        require(updatedAtMillis > 0L) { "updatedAtMillis must be greater than zero." }
        require(status.isNotBlank()) { "status cannot be blank." }
        require(paymentStatus.isNotBlank()) { "paymentStatus cannot be blank." }
        require(syncState.isNotBlank()) { "syncState cannot be blank." }

        require(status == status.uppercase(Locale.ROOT)) { "status must be uppercase." }
        require(paymentStatus == paymentStatus.uppercase(Locale.ROOT)) { "paymentStatus must be uppercase." }
        require(syncState == syncState.uppercase(Locale.ROOT)) { "syncState must be uppercase." }
    }
}