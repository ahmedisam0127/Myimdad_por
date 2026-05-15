package com.myimdad_por.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.myimdad_por.domain.model.Invoice
import com.myimdad_por.domain.model.InvoiceLine
import com.myimdad_por.domain.model.InvoiceStatus
import com.myimdad_por.domain.model.InvoiceType
import com.myimdad_por.domain.model.PaymentStatus
import com.myimdad_por.domain.model.UnitOfMeasure
import org.json.JSONArray
import org.json.JSONObject
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.UUID

@Entity(
    tableName = "invoices",
    indices = [
        Index(value = ["server_id"], unique = true),
        Index(value = ["invoice_number"], unique = true),
        Index(value = ["invoice_type"]),
        Index(value = ["status"]),
        Index(value = ["payment_status"]),
        Index(value = ["issue_date_millis"]),
        Index(value = ["due_date_millis"]),
        Index(value = ["party_id"]),
        Index(value = ["issued_by_employee_id"]),
        Index(value = ["sync_state"])
    ]
)
data class InvoiceEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "server_id")
    val serverId: String? = null,

    @ColumnInfo(name = "invoice_number")
    val invoiceNumber: String,

    @ColumnInfo(name = "invoice_type")
    val invoiceType: String,

    @ColumnInfo(name = "status")
    val status: String = InvoiceStatus.DRAFT.name,

    @ColumnInfo(name = "payment_status")
    val paymentStatus: String = PaymentStatus.PENDING.name,

    @ColumnInfo(name = "issue_date_millis")
    val issueDateMillis: Long,

    @ColumnInfo(name = "due_date_millis")
    val dueDateMillis: Long? = null,

    @ColumnInfo(name = "party_id")
    val partyId: String? = null,

    @ColumnInfo(name = "party_name")
    val partyName: String? = null,

    @ColumnInfo(name = "party_tax_number")
    val partyTaxNumber: String? = null,

    @ColumnInfo(name = "issued_by_employee_id")
    val issuedByEmployeeId: String? = null,

    @ColumnInfo(name = "lines_json")
    val linesJson: String,

    @ColumnInfo(name = "tax_amount")
    val taxAmount: String = "0.00",

    @ColumnInfo(name = "discount_amount")
    val discountAmount: String = "0.00",

    @ColumnInfo(name = "paid_amount")
    val paidAmount: String = "0.00",

    @ColumnInfo(name = "notes")
    val notes: String? = null,

    @ColumnInfo(name = "terms_and_conditions")
    val termsAndConditions: String? = null,

    @ColumnInfo(name = "sync_state")
    val syncState: String = "PENDING",

    @ColumnInfo(name = "is_deleted")
    val isDeleted: Boolean = false,

    @ColumnInfo(name = "synced_at_millis")
    val syncedAtMillis: Long? = null,

    @ColumnInfo(name = "created_at_millis")
    val createdAtMillis: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at_millis")
    val updatedAtMillis: Long = System.currentTimeMillis()
) {
    init {
        require(id.isNotBlank()) { "id cannot be blank." }
        require(invoiceNumber.isNotBlank()) { "invoiceNumber cannot be blank." }
        require(invoiceType.isNotBlank()) { "invoiceType cannot be blank." }
        require(linesJson.isNotBlank()) { "linesJson cannot be blank." }
        require(issueDateMillis > 0L) { "issueDateMillis must be greater than zero." }
    }

    companion object {
        fun fromDomain(
            invoice: Invoice,
            serverId: String? = null,
            syncState: String = "PENDING",
            isDeleted: Boolean = false,
            syncedAtMillis: Long? = null,
            createdAtMillis: Long = System.currentTimeMillis(),
            updatedAtMillis: Long = System.currentTimeMillis()
        ): InvoiceEntity {
            return InvoiceEntity(
                id = invoice.id,
                serverId = serverId,
                invoiceNumber = invoice.invoiceNumber,
                invoiceType = invoice.invoiceType.name,
                status = invoice.status.name,
                paymentStatus = invoice.paymentStatus.name,
                issueDateMillis = invoice.issueDate.toMillis(),
                dueDateMillis = invoice.dueDate?.toMillis(),
                partyId = invoice.partyId,
                partyName = invoice.partyName,
                partyTaxNumber = invoice.partyTaxNumber,
                issuedByEmployeeId = invoice.issuedByEmployeeId,
                linesJson = invoice.lines.toJsonString(),
                taxAmount = invoice.taxAmount.money(),
                discountAmount = invoice.discountAmount.money(),
                paidAmount = invoice.paidAmount.money(),
                notes = invoice.notes,
                termsAndConditions = invoice.termsAndConditions,
                syncState = syncState,
                isDeleted = isDeleted,
                syncedAtMillis = syncedAtMillis,
                createdAtMillis = createdAtMillis,
                updatedAtMillis = updatedAtMillis
            )
        }
    }
}

fun InvoiceEntity.toDomain(): Invoice {
    return Invoice(
        id = id,
        invoiceNumber = invoiceNumber,
        invoiceType = runCatching { InvoiceType.valueOf(invoiceType) }.getOrDefault(InvoiceType.SALE),
        status = runCatching { InvoiceStatus.valueOf(status) }.getOrDefault(InvoiceStatus.DRAFT),
        issueDate = issueDateMillis.toLocalDateTime(),
        dueDate = dueDateMillis?.toLocalDateTime(),
        partyId = partyId,
        partyName = partyName,
        partyTaxNumber = partyTaxNumber,
        issuedByEmployeeId = issuedByEmployeeId,
        lines = linesJson.toInvoiceLineList(),
        taxAmount = taxAmount.toBigDecimalOrZero(),
        discountAmount = discountAmount.toBigDecimalOrZero(),
        paidAmount = paidAmount.toBigDecimalOrZero(),
        notes = notes,
        termsAndConditions = termsAndConditions
    )
}

private fun List<InvoiceLine>.toJsonString(): String {
    return JSONArray().apply {
        forEach { line ->
            put(JSONObject().apply {
                put("id", line.id)
                put("barcode", line.barcode)
                put("productName", line.productName)
                put("displayName", line.displayName)
                put("unitOfMeasure", line.unitOfMeasure.name)
                put("quantity", line.quantity.money())
                put("unitPrice", line.unitPrice.money())
                put("location", line.location)
                put("expiryDate", line.expiryDate?.toString())
                put("taxAmount", line.taxAmount.money())
                put("discountAmount", line.discountAmount.money())
                put("note", line.note)
            })
        }
    }.toString()
}

private fun String.toInvoiceLineList(): List<InvoiceLine> {
    if (isBlank()) return emptyList()
    return runCatching {
        val array = JSONArray(this)
        buildList {
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                add(
                    InvoiceLine(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        barcode = obj.optString("barcode").takeUnless { it.isBlank() },
                        productName = obj.optString("productName"),
                        displayName = obj.optString("displayName").takeUnless { it.isBlank() },
                        unitOfMeasure = runCatching {
                            UnitOfMeasure.valueOf(obj.optString("unitOfMeasure"))
                        }.getOrDefault(UnitOfMeasure.UNIT),
                        quantity = obj.optString("quantity").toBigDecimalOrZero(),
                        unitPrice = obj.optString("unitPrice").toBigDecimalOrZero(),
                        location = obj.optString("location").takeUnless { it.isBlank() },
                        expiryDate = obj.optString("expiryDate").takeUnless { it.isBlank() }?.let { LocalDate.parse(it) },
                        taxAmount = obj.optString("taxAmount").toBigDecimalOrZero(),
                        discountAmount = obj.optString("discountAmount").toBigDecimalOrZero(),
                        note = obj.optString("note").takeUnless { it.isBlank() }
                    )
                )
            }
        }
    }.getOrDefault(emptyList())
}

private fun BigDecimal.money(): String = this.setScale(2, RoundingMode.HALF_UP).toPlainString()

private fun String.toBigDecimalOrZero(): BigDecimal {
    return runCatching { BigDecimal(this) }.getOrDefault(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP)
}

private fun LocalDateTime.toMillis(): Long {
    return this.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
}

private fun Long.toLocalDateTime(): LocalDateTime {
    return LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(this), ZoneId.systemDefault())
}