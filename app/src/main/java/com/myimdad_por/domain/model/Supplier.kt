package com.myimdad_por.domain.model

import java.math.BigDecimal
import java.util.UUID

/**
 * Represents a supplier or vendor in the business domain.
 */
data class Supplier(
    val id: String = UUID.randomUUID().toString(),
    val supplierCode: String? = null,
    val companyName: String,
    val contactPerson: String? = null,
    val phoneNumber: String? = null,
    val email: String? = null,
    val address: String? = null,
    val city: String? = null,
    val country: String? = null,
    val taxNumber: String? = null,
    val commercialRegisterNumber: String? = null,
    val bankAccountNumber: String? = null,
    val creditLimit: BigDecimal = BigDecimal.ZERO,
    val outstandingBalance: BigDecimal = BigDecimal.ZERO,
    val paymentTermsDays: Int = 0,
    val isPreferred: Boolean = false,
    val isActive: Boolean = true,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis(),
    val lastSupplyAtMillis: Long? = null,
    val metadata: Map<String, String> = emptyMap()
) {
    init {
        require(id.isNotBlank()) { "id cannot be blank." }
        require(companyName.isNotBlank()) { "companyName cannot be blank." }
        require(creditLimit >= BigDecimal.ZERO) { "creditLimit cannot be negative." }
        require(outstandingBalance >= BigDecimal.ZERO) { "outstandingBalance cannot be negative." }
        require(paymentTermsDays >= 0) { "paymentTermsDays cannot be negative." }
        require(createdAtMillis > 0L) { "createdAtMillis must be greater than zero." }
        require(updatedAtMillis > 0L) { "updatedAtMillis must be greater than zero." }
        require(updatedAtMillis >= createdAtMillis) {
            "updatedAtMillis must be greater than or equal to createdAtMillis."
        }

        supplierCode?.let {
            require(it.isNotBlank()) { "supplierCode cannot be blank when provided." }
        }
        contactPerson?.let {
            require(it.isNotBlank()) { "contactPerson cannot be blank when provided." }
        }
        phoneNumber?.let {
            require(it.isNotBlank()) { "phoneNumber cannot be blank when provided." }
        }
        email?.let {
            require(it.isNotBlank()) { "email cannot be blank when provided." }
        }
        address?.let {
            require(it.isNotBlank()) { "address cannot be blank when provided." }
        }
        city?.let {
            require(it.isNotBlank()) { "city cannot be blank when provided." }
        }
        country?.let {
            require(it.isNotBlank()) { "country cannot be blank when provided." }
        }
        taxNumber?.let {
            require(it.isNotBlank()) { "taxNumber cannot be blank when provided." }
        }
        commercialRegisterNumber?.let {
            require(it.isNotBlank()) { "commercialRegisterNumber cannot be blank when provided." }
        }
        bankAccountNumber?.let {
            require(it.isNotBlank()) { "bankAccountNumber cannot be blank when provided." }
        }
        lastSupplyAtMillis?.let {
            require(it > 0L) { "lastSupplyAtMillis must be greater than zero when provided." }
        }
    }

    val displayName: String
        get() = companyName.trim()

    val identifier: String
        get() = supplierCode?.trim().takeUnless { it.isNullOrBlank() } ?: id

    val availableCredit: BigDecimal
        get() = (creditLimit - outstandingBalance).coerceAtLeast(BigDecimal.ZERO)

    val hasPayableDebt: Boolean
        get() = outstandingBalance > BigDecimal.ZERO

    val isOverLimit: Boolean
        get() = outstandingBalance > creditLimit
}