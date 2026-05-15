package com.myimdad_por.data.mapper

import com.myimdad_por.core.utils.toBigDecimalOrZero
import com.myimdad_por.data.remote.dto.AccountingDto
import com.myimdad_por.domain.model.AccountingEntry
import com.myimdad_por.domain.model.AccountingEntryStatus
import com.myimdad_por.domain.model.AccountingSource
import com.myimdad_por.domain.model.CurrencyCode
import com.myimdad_por.domain.model.PaymentMethod
import com.myimdad_por.domain.model.PaymentMethodType
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import org.json.JSONArray

fun AccountingEntry.toDto(
    serverId: String? = null,
    syncState: String = "PENDING",
    isDeleted: Boolean = false,
    syncedAtMillis: Long? = null,
    createdAtMillis: Long = System.currentTimeMillis(),
    updatedAtMillis: Long = System.currentTimeMillis()
): AccountingDto {
    val method = paymentMethod
    return AccountingDto(
        id = id,
        serverId = serverId,
        transactionDateMillis = transactionDate.toMillis(),
        referenceId = referenceId,
        description = description,
        debitAccount = debitAccount,
        creditAccount = creditAccount,
        amount = amount.money(),
        currencyCode = currency.name,
        paymentMethodId = method?.id,
        paymentMethodName = method?.displayName,
        paymentMethodType = method?.type?.name,
        paymentMethodRequiresReference = method?.requiresReference ?: false,
        paymentMethodExtraFees = method?.extraFees?.money() ?: "0.00",
        // تم التعديل هنا لتعامل مع القيم التي قد تكون Null بشكل آمن
        paymentMethodSupportedCurrenciesJson = method?.supportedCurrencies?.toJsonString() ?: "[]",
        paymentMethodIsActive = method?.isActive ?: true,
        source = source.name,
        status = status.name,
        createdByEmployeeId = createdByEmployeeId,
        note = note,
        metadataJson = "{}",
        syncState = syncState,
        isDeleted = isDeleted,
        syncedAtMillis = syncedAtMillis,
        createdAtMillis = createdAtMillis,
        updatedAtMillis = updatedAtMillis
    )
}

fun AccountingDto.toDomain(): AccountingEntry {
    val resolvedCurrency = runCatching {
        CurrencyCode.valueOf(currencyCode.trim().uppercase())
    }.getOrDefault(CurrencyCode.SDG)

    val resolvedStatus = runCatching {
        AccountingEntryStatus.valueOf(status.trim().uppercase())
    }.getOrDefault(AccountingEntryStatus.POSTED)

    val resolvedSource = runCatching {
        AccountingSource.valueOf(source.trim().uppercase())
    }.getOrDefault(AccountingSource.MANUAL)

    return AccountingEntry(
        id = id,
        transactionDate = transactionDateMillis.toLocalDateTime(),
        referenceId = referenceId,
        description = description,
        debitAccount = debitAccount,
        creditAccount = creditAccount,
        amount = amount.toBigDecimalOrZero(),
        currency = resolvedCurrency,
        paymentMethod = toPaymentMethodOrNull(),
        source = resolvedSource,
        status = resolvedStatus,
        createdByEmployeeId = createdByEmployeeId,
        note = note
    )
}

fun List<AccountingEntry>.toDtoList(): List<AccountingDto> = map { it.toDto() }

fun List<AccountingDto>.toDomainList(): List<AccountingEntry> = map { it.toDomain() }

private fun AccountingDto.toPaymentMethodOrNull(): PaymentMethod? {
    val id = paymentMethodId?.trim().orEmpty()
    if (id.isBlank()) return null

    val extraFees = paymentMethodExtraFees.toBigDecimalOrZero()
    val supportedCurrencies = paymentMethodSupportedCurrenciesJson.toCurrencyCodeSet()
    val resolvedType = paymentMethodType?.let {
        runCatching { PaymentMethodType.valueOf(it.trim().uppercase()) }
            .getOrDefault(PaymentMethodType.OTHER)
    } ?: PaymentMethodType.OTHER

    return when (id) {
        PaymentMethod.CASH.id -> PaymentMethod.CASH
        PaymentMethod.BANK_TRANSFER.id -> PaymentMethod.BANK_TRANSFER
        PaymentMethod.WALLET.id -> PaymentMethod.WALLET
        PaymentMethod.POS.id -> PaymentMethod.POS
        PaymentMethod.CHEQUE.id -> PaymentMethod.CHEQUE
        else -> PaymentMethod.Custom(
            id = id,
            name = paymentMethodName?.takeIf { it.isNotBlank() } ?: id,
            type = resolvedType,
            providerName = null,
            requiresReference = paymentMethodRequiresReference,
            extraFees = extraFees,
            supportedCurrencies = supportedCurrencies.ifEmpty {
                setOf(CurrencyCode.SDG, CurrencyCode.EGP)
            },
            isActive = paymentMethodIsActive
        )
    }
}

private fun Set<CurrencyCode>.toJsonString(): String {
    return JSONArray().apply {
        forEach { put(it.name) }
    }.toString()
}

private fun String.toCurrencyCodeSet(): Set<CurrencyCode> {
    if (isBlank()) return emptySet()
    return runCatching {
        val array = JSONArray(this)
        buildSet {
            for (i in 0 until array.length()) {
                val value = array.optString(i)
                runCatching { add(CurrencyCode.valueOf(value)) }
            }
        }
    }.getOrDefault(emptySet())
}

private fun LocalDateTime.toMillis(): Long {
    return atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
}

private fun Long.toLocalDateTime(): LocalDateTime {
    return LocalDateTime.ofInstant(Instant.ofEpochMilli(this), ZoneId.systemDefault())
}

private fun BigDecimal.money(): String = setScale(2, RoundingMode.HALF_UP).toPlainString()
