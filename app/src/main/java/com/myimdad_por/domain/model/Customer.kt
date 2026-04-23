package com.myimdad_por.domain.model

import java.math.BigDecimal
import java.util.UUID

/**
 * Represents a customer in the business domain.
 *
 * Designed to support retail, wholesale, and credit-based operations.
 */
data class Customer(
    val id: String = UUID.randomUUID().toString(),
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
    val creditLimit: BigDecimal = BigDecimal.ZERO,
    val outstandingBalance: BigDecimal = BigDecimal.ZERO,
    val isActive: Boolean = true,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis(),
    val lastPurchaseAtMillis: Long? = null,
    val metadata: Map<String, String> = emptyMap()
) {
    init {
        require(id.isNotBlank()) { "id cannot be blank." }
        require(fullName.isNotBlank()) { "fullName cannot be blank." }
        require(creditLimit >= BigDecimal.ZERO) { "creditLimit cannot be negative." }
        require(outstandingBalance >= BigDecimal.ZERO) { "outstandingBalance cannot be negative." }
        require(createdAtMillis > 0L) { "createdAtMillis must be greater than zero." }
        require(updatedAtMillis > 0L) { "updatedAtMillis must be greater than zero." }
        require(updatedAtMillis >= createdAtMillis) {
            "updatedAtMillis must be greater than or equal to createdAtMillis."
        }

        code?.let {
            require(it.isNotBlank()) { "code cannot be blank when provided." }
        }
        tradeName?.let {
            require(it.isNotBlank()) { "tradeName cannot be blank when provided." }
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
        nationalId?.let {
            require(it.isNotBlank()) { "nationalId cannot be blank when provided." }
        }
        lastPurchaseAtMillis?.let {
            require(it > 0L) { "lastPurchaseAtMillis must be greater than zero when provided." }
        }
    }

    val displayName: String
        get() = tradeName?.trim().takeUnless { it.isNullOrBlank() } ?: fullName.trim()

    val identifier: String
        get() = code?.trim().takeUnless { it.isNullOrBlank() } ?: id

    val availableCredit: BigDecimal
        get() = (creditLimit - outstandingBalance).coerceAtLeast(BigDecimal.ZERO)

    val hasDebt: Boolean
        get() = outstandingBalance > BigDecimal.ZERO

    val isCreditCustomer: Boolean
        get() = creditLimit > BigDecimal.ZERO

    val isOverLimit: Boolean
        get() = outstandingBalance > creditLimit

    fun canPlaceOnCredit(amount: BigDecimal): Boolean {
        require(amount >= BigDecimal.ZERO) { "amount cannot be negative." }
        return availableCredit >= amount
    }
}