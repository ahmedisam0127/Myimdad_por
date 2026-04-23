package com.myimdad_por.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Data Transfer Object (DTO) for Expense.
 * Designed to safely receive data from the backend API.
 */
data class ExpenseDto(
    @SerializedName("id") val id: String?,
    @SerializedName("expense_number") val expenseNumber: String?,
    @SerializedName("category") val category: String?,
    @SerializedName("title") val title: String?,
    @SerializedName("amount") val amount: String?,
    @SerializedName("expense_date") val expenseDate: String?,
    @SerializedName("paid_amount") val paidAmount: String?,
    @SerializedName("payment_method") val paymentMethod: String?,
    @SerializedName("status") val status: String?,
    @SerializedName("reference_number") val referenceNumber: String?,
    @SerializedName("supplier_name") val supplierName: String?,
    @SerializedName("employee_id") val employeeId: String?,
    @SerializedName("note") val note: String?
)
