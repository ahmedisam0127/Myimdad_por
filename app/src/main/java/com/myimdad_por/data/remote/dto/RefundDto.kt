package com.myimdad_por.data.remote.dto

import java.util.Locale

data class RefundDto(
    val refundId: String,
    val serverId: String? = null,
    val returnId: String? = null,
    val transactionId: String,
    val amount: String = "0.00",
    val currencyCode: String = "SDG",
    val status: String = "PENDING",
    val providerReference: String? = null,
    val idempotencyKey: String? = null,
    val requestedBy: String? = null,
    val verified: Boolean = false,
    val verifiedAtMillis: Long? = null,
    val reason: String? = null,
    val metadataJson: String = "{}",
    val syncState: String = "PENDING",
    val isDeleted: Boolean = false,
    val syncedAtMillis: Long? = null,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis()
) {
    init {
        require(refundId.isNotBlank()) { "refundId cannot be blank." }
        require(transactionId.isNotBlank()) { "transactionId cannot be blank." }
        require(amount.isNotBlank()) { "amount cannot be blank." }
        require(currencyCode.isNotBlank()) { "currencyCode cannot be blank." }
        require(currencyCode.trim().length == 3) { "currencyCode must be exactly 3 characters." }
        require(status.isNotBlank()) { "status cannot be blank." }
        require(syncState.isNotBlank()) { "syncState cannot be blank." }
        require(createdAtMillis > 0L) { "createdAtMillis must be greater than zero." }
        require(updatedAtMillis > 0L) { "updatedAtMillis must be greater than zero." }

        returnId?.let { require(it.isNotBlank()) { "returnId cannot be blank when provided." } }
        providerReference?.let { require(it.isNotBlank()) { "providerReference cannot be blank when provided." } }
        idempotencyKey?.let { require(it.isNotBlank()) { "idempotencyKey cannot be blank when provided." } }
        requestedBy?.let { require(it.isNotBlank()) { "requestedBy cannot be blank when provided." } }
        verifiedAtMillis?.let { require(it > 0L) { "verifiedAtMillis must be greater than zero when provided." } }
        reason?.let { require(it.isNotBlank()) { "reason cannot be blank when provided." } }
    }

    val normalizedCurrencyCode: String
        get() = currencyCode.trim().uppercase(Locale.ROOT)
}