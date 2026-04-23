package com.myimdad_por.data.mapper

import com.myimdad_por.core.utils.toBigDecimalOrZero
import com.myimdad_por.data.remote.dto.PaymentDto
import com.myimdad_por.domain.model.CurrencyCode
import com.myimdad_por.domain.model.PaymentMethod
import com.myimdad_por.domain.model.PaymentRecord
import com.myimdad_por.domain.model.PaymentStatus
import java.util.UUID

/* * ملاحظة احترافية: 
 * لم نقم بكتابة دوال (toEnumOrDefault) و (toPaymentMethod) هنا 
 * لأنها معرفة مسبقاً في (ExpenseMapper.kt) داخل نفس الـ Package.
 * كوتلن تقوم بالتعرف عليها واستخدامها تلقائياً!
 */

/**
 * Mapper: DTO (Data Layer) -> Domain Model (Business Layer)
 * يحول البيانات القادمة من السيرفر إلى كائنات يفهمها تطبيقك بأمان تام.
 */
fun PaymentDto.toDomain(): PaymentRecord {
    return PaymentRecord(
        recordId = this.recordId ?: UUID.randomUUID().toString(),
        transactionId = this.transactionId ?: "TXN-${System.currentTimeMillis()}",
        invoiceId = this.invoiceId,
        customerId = this.customerId,
        // نستخدم الدالة الموجودة مسبقاً في الحزمة!
        paymentMethod = this.paymentMethod.toPaymentMethod(),
        amount = this.amount.toBigDecimalOrZero(),
        // نستخدم الدالة الموجودة مسبقاً في الحزمة!
        currency = this.currency.toEnumOrDefault(CurrencyCode.SDG),
        status = this.status.toEnumOrDefault(PaymentStatus.PENDING),
        providerName = this.providerName,
        providerReference = this.providerReference,
        receiptNumber = this.receiptNumber,
        note = this.note,
        createdAtMillis = this.createdAtMillis ?: System.currentTimeMillis(),
        updatedAtMillis = this.updatedAtMillis ?: System.currentTimeMillis(),
        metadata = this.metadata ?: emptyMap()
    )
}

/**
 * Mapper: Domain Model (Business Layer) -> DTO (Data Layer / Network)
 * يحول كائنات تطبيقك إلى نصوص وأرقام جاهزة للإرسال إلى السيرفر.
 */
fun PaymentRecord.toDto(): PaymentDto {
    return PaymentDto(
        recordId = this.recordId,
        transactionId = this.transactionId,
        invoiceId = this.invoiceId,
        customerId = this.customerId,
        paymentMethod = this.paymentMethod?.type?.name ?: PaymentMethod.CASH.type.name,
        amount = this.amount.toPlainString(),
        currency = this.currency.name,
        status = this.status.name,
        providerName = this.providerName,
        providerReference = this.providerReference,
        receiptNumber = this.receiptNumber,
        note = this.note,
        createdAtMillis = this.createdAtMillis,
        updatedAtMillis = this.updatedAtMillis,
        metadata = this.metadata
    )
}
