package com.myimdad_por.data.mapper

import com.myimdad_por.core.utils.toBigDecimalOrZero
import com.myimdad_por.data.remote.dto.CustomerDto
import com.myimdad_por.domain.model.Customer
import org.json.JSONObject
import java.math.BigDecimal
import java.math.RoundingMode

fun Customer.toDto(
    serverId: String? = null,
    syncState: String = "PENDING",
    isDeleted: Boolean = false,
    syncedAtMillis: Long? = null,
    createdAtMillis: Long = this.createdAtMillis,
    updatedAtMillis: Long = this.updatedAtMillis
): CustomerDto {
    return CustomerDto(
        id = id,
        serverId = serverId,
        code = code,
        fullName = fullName,
        tradeName = tradeName,
        phoneNumber = phoneNumber,
        email = email,
        address = address,
        city = city,
        country = country,
        taxNumber = taxNumber,
        nationalId = nationalId,
        creditLimit = creditLimit.money(),
        outstandingBalance = outstandingBalance.money(),
        isActive = isActive,
        lastPurchaseAtMillis = lastPurchaseAtMillis,
        metadataJson = metadata.toJsonString(),
        syncState = syncState,
        isDeleted = isDeleted,
        syncedAtMillis = syncedAtMillis,
        createdAtMillis = createdAtMillis,
        updatedAtMillis = updatedAtMillis
    )
}

fun CustomerDto.toDomain(): Customer {
    return Customer(
        id = id,
        code = code,
        fullName = fullName,
        tradeName = tradeName,
        phoneNumber = phoneNumber,
        email = email,
        address = address,
        city = city,
        country = country,
        taxNumber = taxNumber,
        nationalId = nationalId,
        creditLimit = creditLimit.toBigDecimalOrZero(),
        outstandingBalance = outstandingBalance.toBigDecimalOrZero(),
        isActive = isActive,
        createdAtMillis = createdAtMillis,
        updatedAtMillis = updatedAtMillis,
        lastPurchaseAtMillis = lastPurchaseAtMillis,
        metadata = metadataJson.toStringMap()
    )
}

fun List<Customer>.toDtoList(): List<CustomerDto> = map { it.toDto() }

fun List<CustomerDto>.toDomainList(): List<Customer> = map { it.toDomain() }

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

private fun BigDecimal.money(): String =
    setScale(2, RoundingMode.HALF_UP).toPlainString()