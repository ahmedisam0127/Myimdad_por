package com.myimdad_por.data.remote.dto

import java.util.Locale

data class CustomerDto(
    val id: String,
    val serverId: String? = null,
    val code: String? = null,
    val fullName: String,
    val tradeName: String? = null,
    val phoneNumber: String? = null,
    val email: String? = null,
    val address: String? = null,
    val city: String? = null,
    val country: String? = null,
    val taxNumber: String? = null,
    val nationalId: String? = null,
    val creditLimit: String = "0.00",
    val outstandingBalance: String = "0.00",
    val isActive: Boolean = true,
    val lastPurchaseAtMillis: Long? = null,
    val metadataJson: String = "{}",
    val syncState: String = "PENDING",
    val isDeleted: Boolean = false,
    val syncedAtMillis: Long? = null,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis()
) {
    init {
        require(id.isNotBlank()) { "id cannot be blank." }
        require(fullName.isNotBlank()) { "fullName cannot be blank." }
        require(creditLimit.isNotBlank()) { "creditLimit cannot be blank." }
        require(outstandingBalance.isNotBlank()) { "outstandingBalance cannot be blank." }
        require(createdAtMillis > 0L) { "createdAtMillis must be greater than zero." }
        require(updatedAtMillis > 0L) { "updatedAtMillis must be greater than zero." }
        code?.let { require(it.isNotBlank()) { "code cannot be blank when provided." } }
        tradeName?.let { require(it.isNotBlank()) { "tradeName cannot be blank when provided." } }
        phoneNumber?.let { require(it.isNotBlank()) { "phoneNumber cannot be blank when provided." } }
        email?.let { require(it.isNotBlank()) { "email cannot be blank when provided." } }
        address?.let { require(it.isNotBlank()) { "address cannot be blank when provided." } }
        city?.let { require(it.isNotBlank()) { "city cannot be blank when provided." } }
        country?.let { require(it.isNotBlank()) { "country cannot be blank when provided." } }
        taxNumber?.let { require(it.isNotBlank()) { "taxNumber cannot be blank when provided." } }
        nationalId?.let { require(it.isNotBlank()) { "nationalId cannot be blank when provided." } }
        require(syncState.isNotBlank()) { "syncState cannot be blank." }
    }

    val displayName: String
        get() = tradeName?.trim().takeUnless { it.isNullOrBlank() } ?: fullName.trim()

    val normalizedCode: String?
        get() = code?.trim()?.takeIf { it.isNotBlank() }?.uppercase(Locale.ROOT)
}