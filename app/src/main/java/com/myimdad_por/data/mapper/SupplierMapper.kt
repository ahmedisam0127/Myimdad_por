package com.myimdad_por.data.mapper

import com.myimdad_por.core.utils.toBigDecimalOrZero
import com.myimdad_por.core.utils.toPlainScaledString
import com.myimdad_por.data.remote.dto.SupplierDto
import com.myimdad_por.domain.model.Supplier
import org.json.JSONObject

/**
 * [SupplierMapper] هو الجسر الذكي بين طبقة الشبكة (DTO) وطبقة الأعمال (Domain).
 * يضمن تحويلاً آمناً للأموال (BigDecimal) وصيغ JSON.
 */

fun SupplierDto.toDomain(): Supplier {
    return Supplier(
        id = id,
        supplierCode = supplierCode,
        companyName = companyName,
        contactPerson = contactPerson,
        phoneNumber = phoneNumber,
        email = email,
        address = address,
        city = city,
        country = country,
        taxNumber = taxNumber,
        commercialRegisterNumber = commercialRegisterNumber,
        bankAccountNumber = bankAccountNumber,
        creditLimit = creditLimit.toBigDecimalOrZero(), // استخدام الامتداد الآمن الخاص بك
        outstandingBalance = outstandingBalance.toBigDecimalOrZero(),
        paymentTermsDays = paymentTermsDays,
        isPreferred = isPreferred,
        isActive = isActive,
        createdAtMillis = createdAtMillis,
        updatedAtMillis = updatedAtMillis,
        lastSupplyAtMillis = lastSupplyAtMillis,
        metadata = metadataJson.toMap() // فك تشفير البيانات الوصفية بأمان
    )
}

fun Supplier.toDto(): SupplierDto {
    return SupplierDto(
        id = id,
        supplierCode = supplierCode,
        companyName = companyName,
        contactPerson = contactPerson,
        phoneNumber = phoneNumber,
        email = email,
        address = address,
        city = city,
        country = country,
        taxNumber = taxNumber,
        commercialRegisterNumber = commercialRegisterNumber,
        bankAccountNumber = bankAccountNumber,
        creditLimit = creditLimit.toPlainScaledString(3), // دقة 3 خانات عشرية للأنظمة المالية
        outstandingBalance = outstandingBalance.toPlainScaledString(3),
        paymentTermsDays = paymentTermsDays,
        isPreferred = isPreferred,
        isActive = isActive,
        createdAtMillis = createdAtMillis,
        updatedAtMillis = updatedAtMillis,
        lastSupplyAtMillis = lastSupplyAtMillis,
        metadataJson = metadata.toJsonString() // تغليف البيانات الوصفية
    )
}

fun List<SupplierDto>.toDomainList(): List<Supplier> = map { it.toDomain() }
fun List<Supplier>.toDtoList(): List<SupplierDto> = map { it.toDto() }

// --- أدوات مساعدة خاصة بالتحويل (Private Helpers) ---

private fun String.toMap(): Map<String, String> {
    return runCatching {
        val jsonObject = JSONObject(this.takeIf { it.isNotBlank() } ?: "{}")
        jsonObject.keys().asSequence().associateWith { key -> 
            jsonObject.getString(key) 
        }
    }.getOrDefault(emptyMap())
}

private fun Map<String, String>.toJsonString(): String {
    return runCatching { JSONObject(this).toString() }.getOrDefault("{}")
}
