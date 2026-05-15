package com.myimdad_por.data.mapper

import com.myimdad_por.core.utils.DateTimeUtils
import com.myimdad_por.core.utils.toBigDecimalOrZero
import com.myimdad_por.data.remote.dto.CustomerSnapshotDto
import com.myimdad_por.data.remote.dto.SaleInvoiceDto
import com.myimdad_por.data.remote.dto.SaleItemDto
import com.myimdad_por.domain.model.CustomerSnapshot
import com.myimdad_por.domain.model.SaleInvoice
import com.myimdad_por.domain.model.SaleInvoiceStatus
import com.myimdad_por.domain.model.SaleItem
import java.util.UUID

fun SaleInvoiceDto.toDomain(): SaleInvoice {
    return SaleInvoice(
        id = this.id ?: UUID.randomUUID().toString(),
        invoiceNumber = this.invoiceNumber.orEmpty().ifBlank { "INV-UNKNOWN" },
        saleId = this.saleId.orEmpty(),
        status = runCatching { SaleInvoiceStatus.valueOf(this.status ?: "DRAFT") }
            .getOrDefault(SaleInvoiceStatus.DRAFT),
        issueDate = this.issueDate?.let { DateTimeUtils.parseDateTime(it) } ?: DateTimeUtils.now(),
        dueDate = this.dueDate?.let { DateTimeUtils.parseDateTime(it) },
        taxReference = this.taxReference,
        customerSnapshot = this.customerSnapshot?.toDomain(),
        employeeId = this.employeeId.orEmpty(),
        items = this.items?.map { it.toDomain() } ?: emptyList(),
        paidAmount = this.paidAmount.toBigDecimalOrZero(),
        notes = this.notes,
        termsAndConditions = this.termsAndConditions,
        qrPayload = this.qrPayload
    )
}

fun CustomerSnapshotDto.toDomain(): CustomerSnapshot? {
    if (this.name.isNullOrBlank()) return null
    return CustomerSnapshot(
        name = this.name,
        address = this.address,
        taxNumber = this.taxNumber,
        phone = this.phone
    )
}

fun SaleItemDto.toDomain(): SaleItem {
    return SaleItem(
        id = this.id ?: UUID.randomUUID().toString(),
        productId = this.productId.orEmpty(), // تم حل المشكلة: تمرير المعلمة المفقودة
        productName = this.productName.orEmpty().ifBlank { "Unspecified Product" },
        quantity = this.quantity.toBigDecimalOrZero(),
        unitPrice = this.unitPrice.toBigDecimalOrZero(),
        taxAmount = this.taxAmount.toBigDecimalOrZero(),
        discountAmount = this.discountAmount.toBigDecimalOrZero()
    )
}
