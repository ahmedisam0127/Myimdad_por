package com.myimdad_por.data.mapper

import com.myimdad_por.core.payment.models.PaymentStatus
import com.myimdad_por.core.payment.models.PaymentVerification
import com.myimdad_por.data.remote.dto.PaymentVerificationDto
import org.json.JSONObject
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale

fun PaymentVerificationDto.toDomain(): PaymentVerification {
    return PaymentVerification(
        verified = verified,
        transactionId = transactionId,
        amount = amount.toBigDecimalSafe(),
        currency = currency.trim().uppercase(Locale.ROOT),
        status = status.toPaymentStatusOrDefault(),
        providerReference = providerReference,
        signatureValid = signatureValid,
        amountMatched = amountMatched,
        backendConfirmed = backendConfirmed,
        verifiedAtMillis = verifiedAtMillis,
        reason = reason,
        metadata = metadataJson.toStringMap()
    )
}

fun PaymentVerification.toDto(
    serverId: String? = null,
    syncState: String = "PENDING",
    isDeleted: Boolean = false,
    syncedAtMillis: Long? = null,
    createdAtMillis: Long = verifiedAtMillis,
    updatedAtMillis: Long = System.currentTimeMillis()
): PaymentVerificationDto {
    return PaymentVerificationDto(
        serverId = serverId,
        transactionId = transactionId,
        verified = verified,
        amount = amount.money(),
        currency = currency.trim().uppercase(Locale.ROOT),
        status = status.name,
        providerReference = providerReference,
        signatureValid = signatureValid,
        amountMatched = amountMatched,
        backendConfirmed = backendConfirmed,
        verifiedAtMillis = verifiedAtMillis,
        reason = reason,
        metadataJson = metadata.toJsonString(),
        syncState = syncState,
        isDeleted = isDeleted,
        syncedAtMillis = syncedAtMillis,
        createdAtMillis = createdAtMillis,
        updatedAtMillis = updatedAtMillis
    )
}

fun List<PaymentVerificationDto>.toDomainList(): List<PaymentVerification> = map { it.toDomain() }

fun List<PaymentVerification>.toDtoList(): List<PaymentVerificationDto> = map { it.toDto() }

private fun String.toPaymentStatusOrDefault(): PaymentStatus {
    return runCatching {
        PaymentStatus.valueOf(trim().uppercase(Locale.ROOT))
    }.getOrDefault(PaymentStatus.PENDING)
}

private fun String.toBigDecimalSafe(): BigDecimal {
    return runCatching { BigDecimal(trim()) }
        .getOrDefault(BigDecimal.ZERO)
        .setScale(2, RoundingMode.HALF_UP)
}

private fun BigDecimal.money(): String =
    setScale(2, RoundingMode.HALF_UP).toPlainString()

private fun Map<String, String>.toJsonString(): String {
    return JSONObject(this).toString()
}

private fun String.toStringMap(): Map<String, String> {
    if (isBlank()) return emptyMap()
    return runCatching {
        val json = JSONObject(this)
        buildMap {
            json.keys().forEach { key ->
                put(key, json.optString(key))
            }
        }
    }.getOrDefault(emptyMap())
}