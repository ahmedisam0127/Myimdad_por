package com.myimdad_por.data.remote.dto

/**
 * [SupplierDto] هو وعاء البيانات الآمن للتواصل مع الـ API الخارجي.
 * يتميز بـ (Strict Validation) لضمان عدم تسرب "بيانات ملوثة" إلى داخل التطبيق.
 */
data class SupplierDto(
    val id: String,
    val supplierCode: String? = null,
    val companyName: String,
    val contactPerson: String? = null,
    val phoneNumber: String? = null,
    val email: String? = null,
    val address: String? = null,
    val city: String? = null,
    val country: String? = null,
    val taxNumber: String? = null,
    val commercialRegisterNumber: String? = null,
    val bankAccountNumber: String? = null,
    val creditLimit: String = "0.00",
    val outstandingBalance: String = "0.00",
    val paymentTermsDays: Int = 0,
    val isPreferred: Boolean = false,
    val isActive: Boolean = true,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
    val lastSupplyAtMillis: Long? = null,
    val metadataJson: String = "{}"
) {
    init {
        require(id.isNotBlank()) { "Supplier ID cannot be blank." }
        require(companyName.isNotBlank()) { "Company name is strictly required." }
        require(paymentTermsDays >= 0) { "Payment terms cannot be negative days." }
        require(createdAtMillis > 0L) { "Invalid creation timestamp." }
        require(updatedAtMillis >= createdAtMillis) { "Update time cannot precede creation time." }
        
        // منع القيم النصية الفارغة (Blank Strings) للخصائص الاختيارية
        supplierCode?.let { require(it.isNotBlank()) { "supplierCode must be null or non-blank" } }
        contactPerson?.let { require(it.isNotBlank()) { "contactPerson must be null or non-blank" } }
        email?.let { require(it.isNotBlank()) { "email must be null or non-blank" } }
    }
}
