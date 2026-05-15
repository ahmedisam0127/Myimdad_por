package com.myimdad_por.data.remote.dto

import com.myimdad_por.domain.model.ReturnStatus
import com.myimdad_por.domain.model.ReturnType
import java.util.Locale

data class ReturnDto(
    val returnId: String,
    val serverId: String? = null,
    val returnNumber: String,
    val returnType: String,
    val originalDocumentId: String? = null,
    val originalDocumentNumber: String? = null,
    val partyId: String? = null,
    val partyName: String? = null,
    val processedByEmployeeId: String,
    val itemsJson: String = "[]",
    val returnDateMillis: Long,
    val status: String = ReturnStatus.DRAFT.name,
    val refundStatus: String = "PENDING",
    val subtotalAmount: String,
    val taxAmount: String,
    val discountAmount: String,
    val totalRefundAmount: String,
    val refundedAmount: String = "0.00",
    val remainingRefundAmount: String,
    val reason: String? = null,
    val note: String? = null,
    val metadataJson: String = "{}",
    val syncState: String = "PENDING",
    val isDeleted: Boolean = false,
    val syncedAtMillis: Long? = null,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis()
) {
    init {
        require(returnId.isNotBlank()) { "returnId cannot be blank." }
        require(returnNumber.isNotBlank()) { "returnNumber cannot be blank." }
        require(returnType.isNotBlank()) { "returnType cannot be blank." }
        require(processedByEmployeeId.isNotBlank()) { "processedByEmployeeId cannot be blank." }
        require(itemsJson.isNotBlank()) { "itemsJson cannot be blank." }
        require(returnDateMillis > 0L) { "returnDateMillis must be greater than zero." }
        require(subtotalAmount.isNotBlank()) { "subtotalAmount cannot be blank." }
        require(taxAmount.isNotBlank()) { "taxAmount cannot be blank." }
        require(discountAmount.isNotBlank()) { "discountAmount cannot be blank." }
        require(totalRefundAmount.isNotBlank()) { "totalRefundAmount cannot be blank." }
        require(refundedAmount.isNotBlank()) { "refundedAmount cannot be blank." }
        require(remainingRefundAmount.isNotBlank()) { "remainingRefundAmount cannot be blank." }
        require(status.isNotBlank()) { "status cannot be blank." }
        require(refundStatus.isNotBlank()) { "refundStatus cannot be blank." }
        require(syncState.isNotBlank()) { "syncState cannot be blank." }
        require(createdAtMillis > 0L) { "createdAtMillis must be greater than zero." }
        require(updatedAtMillis > 0L) { "updatedAtMillis must be greater than zero." }

        originalDocumentId?.let { require(it.isNotBlank()) { "originalDocumentId cannot be blank when provided." } }
        originalDocumentNumber?.let { require(it.isNotBlank()) { "originalDocumentNumber cannot be blank when provided." } }
        partyId?.let { require(it.isNotBlank()) { "partyId cannot be blank when provided." } }
        partyName?.let { require(it.isNotBlank()) { "partyName cannot be blank when provided." } }
        reason?.let { require(it.isNotBlank()) { "reason cannot be blank when provided." } }
        note?.let { require(it.isNotBlank()) { "note cannot be blank when provided." } }

        require(returnType == returnType.uppercase(Locale.ROOT)) { "returnType must be uppercase." }
        require(status == status.uppercase(Locale.ROOT)) { "status must be uppercase." }
        require(refundStatus == refundStatus.uppercase(Locale.ROOT)) { "refundStatus must be uppercase." }
        require(syncState == syncState.uppercase(Locale.ROOT)) { "syncState must be uppercase." }

        runCatching { ReturnType.valueOf(returnType.trim().uppercase(Locale.ROOT)) }
        runCatching { ReturnStatus.valueOf(status.trim().uppercase(Locale.ROOT)) }
    }

    val normalizedReturnType: String
        get() = returnType.trim().uppercase(Locale.ROOT)
}