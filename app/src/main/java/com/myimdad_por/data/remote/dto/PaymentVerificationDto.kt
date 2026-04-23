package com.myimdad_por.data.remote.dto

import com.myimdad_por.domain.model.PaymentStatus
import java.util.Locale

data class PaymentVerificationDto(
    val serverId: String? = null,
    val transactionId: String,
    val verified: Boolean,
    val amount: String,
    val currency: String,
    val status: String = PaymentStatus.PENDING.name,
    val providerReference: String? = null,
    val signatureValid: Boolean = false,
    val amountMatched: Boolean = false,
    val backendConfirmed: Boolean = false,
    val verifiedAtMillis: Long = System.currentTimeMillis(),
    val reason: String? = null,
    val metadataJson: String = "{}",
    val syncState: String = "PENDING",
    val isDeleted: Boolean = false,
    val syncedAtMillis: Long? = null,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis()
) {
    init {
        require(transactionId.isNotBlank()) { "transactionId cannot be blank." }
        require(amount.isNotBlank()) { "amount cannot be blank." }
        require(currency.isNotBlank()) { "currency cannot be blank." }
        require(status.isNotBlank()) { "status cannot be blank." }
        require(syncState.isNotBlank()) { "syncState cannot be blank." }
        require(currency.trim().length == 3) { "currency must be exactly 3 characters." }
        require(currency == currency.uppercase(Locale.ROOT)) { "currency must be uppercase." }
        require(verifiedAtMillis > 0L) { "verifiedAtMillis must be greater than zero." }
        require(createdAtMillis > 0L) { "createdAtMillis must be greater than zero." }
        require(updatedAtMillis > 0L) { "updatedAtMillis must be greater than zero." }
    }
}