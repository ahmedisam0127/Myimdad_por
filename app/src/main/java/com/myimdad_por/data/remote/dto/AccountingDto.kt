package com.myimdad_por.data.remote.dto

import com.myimdad_por.domain.model.AccountingEntryStatus
import com.myimdad_por.domain.model.AccountingSource
import java.util.Locale

data class AccountingDto(
    val id: String,
    val serverId: String? = null,
    val transactionDateMillis: Long,
    val referenceId: String? = null,
    val description: String,
    val debitAccount: String,
    val creditAccount: String,
    val amount: String,
    val currencyCode: String = "SDG",
    val paymentMethodId: String? = null,
    val paymentMethodName: String? = null,
    val paymentMethodType: String? = null,
    val paymentMethodRequiresReference: Boolean = false,
    val paymentMethodExtraFees: String = "0.00",
    val paymentMethodSupportedCurrenciesJson: String = "[]",
    val paymentMethodIsActive: Boolean = true,
    val source: String = AccountingSource.MANUAL.name,
    val status: String = AccountingEntryStatus.POSTED.name,
    val createdByEmployeeId: String? = null,
    val note: String? = null,
    val metadataJson: String = "{}",
    val syncState: String = "PENDING",
    val isDeleted: Boolean = false,
    val syncedAtMillis: Long? = null,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis()
) {
    init {
        require(id.isNotBlank()) { "id cannot be blank." }
        require(transactionDateMillis > 0L) { "transactionDateMillis must be greater than zero." }
        require(description.isNotBlank()) { "description cannot be blank." }
        require(debitAccount.isNotBlank()) { "debitAccount cannot be blank." }
        require(creditAccount.isNotBlank()) { "creditAccount cannot be blank." }
        require(amount.isNotBlank()) { "amount cannot be blank." }
        require(currencyCode.trim().length == 3) { "currencyCode must be exactly 3 characters." }
        require(currencyCode == currencyCode.uppercase(Locale.ROOT)) {
            "currencyCode must be uppercase."
        }
        require(createdAtMillis > 0L) { "createdAtMillis must be greater than zero." }
        require(updatedAtMillis > 0L) { "updatedAtMillis must be greater than zero." }
    }
}