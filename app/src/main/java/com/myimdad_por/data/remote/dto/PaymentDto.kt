package com.myimdad_por.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Data Transfer Object (DTO) for PaymentRecord.
 * Engineered to safely deserialize backend JSON payloads into Kotlin objects.
 */
data class PaymentDto(
    @SerializedName("record_id") val recordId: String?,
    @SerializedName("transaction_id") val transactionId: String?,
    @SerializedName("invoice_id") val invoiceId: String?,
    @SerializedName("customer_id") val customerId: String?,
    @SerializedName("payment_method") val paymentMethod: String?,
    @SerializedName("amount") val amount: String?,
    @SerializedName("currency") val currency: String?,
    @SerializedName("status") val status: String?,
    @SerializedName("provider_name") val providerName: String?,
    @SerializedName("provider_reference") val providerReference: String?,
    @SerializedName("receipt_number") val receiptNumber: String?,
    @SerializedName("note") val note: String?,
    @SerializedName("created_at_millis") val createdAtMillis: Long?,
    @SerializedName("updated_at_millis") val updatedAtMillis: Long?,
    @SerializedName("metadata") val metadata: Map<String, String>?
)
