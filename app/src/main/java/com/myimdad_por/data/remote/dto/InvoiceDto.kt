package com.myimdad_por.data.remote.dto

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

/**
 * كبسولة البيانات (DTO) - تم التعديل لاستخدام Gson لتفادي أخطاء الترجمة (Build Errors)
 */
@Keep
data class SaleInvoiceDto(
    @SerializedName("id") val id: String? = null,
    @SerializedName("invoice_number") val invoiceNumber: String? = null,
    @SerializedName("sale_id") val saleId: String? = null,
    @SerializedName("status") val status: String? = null,
    @SerializedName("issue_date") val issueDate: String? = null,
    @SerializedName("due_date") val dueDate: String? = null,
    @SerializedName("tax_reference") val taxReference: String? = null,
    @SerializedName("customer") val customerSnapshot: CustomerSnapshotDto? = null,
    @SerializedName("employee_id") val employeeId: String? = null,
    @SerializedName("items") val items: List<SaleItemDto>? = null,
    @SerializedName("paid_amount") val paidAmount: String? = null,
    @SerializedName("notes") val notes: String? = null,
    @SerializedName("terms_conditions") val termsAndConditions: String? = null,
    @SerializedName("qr_payload") val qrPayload: String? = null
)

@Keep
data class CustomerSnapshotDto(
    @SerializedName("name") val name: String? = null,
    @SerializedName("address") val address: String? = null,
    @SerializedName("tax_number") val taxNumber: String? = null,
    @SerializedName("phone") val phone: String? = null
)

@Keep
data class SaleItemDto(
    @SerializedName("id") val id: String? = null,
    @SerializedName("product_id") val productId: String? = null, // تمت إضافة هذا الحقل المفقود
    @SerializedName("product_name") val productName: String? = null,
    @SerializedName("quantity") val quantity: String? = null,
    @SerializedName("unit_price") val unitPrice: String? = null,
    @SerializedName("tax_amount") val taxAmount: String? = null,
    @SerializedName("discount_amount") val discountAmount: String? = null
)
