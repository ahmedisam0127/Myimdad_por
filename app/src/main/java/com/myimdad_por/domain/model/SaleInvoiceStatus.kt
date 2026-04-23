package com.myimdad_por.domain.model

/**
 * حالة الفاتورة ضمن سير العمل.
 */
enum class SaleInvoiceStatus(
    val isEditable: Boolean,
    val isFinal: Boolean,
    val reversesInventoryOnCancel: Boolean
) {
    DRAFT(true, false, false),
    ISSUED(false, false, true),
    OPEN(false, false, true),
    PAID(false, true, true),
    PARTIALLY_PAID(false, false, true),
    OVERDUE(false, false, true),
    VOID(false, true, true),
    CANCELLED(false, true, true);

    val isActive: Boolean
        get() = this != VOID && this != CANCELLED
}