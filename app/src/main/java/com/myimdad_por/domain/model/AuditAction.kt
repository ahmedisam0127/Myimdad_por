package com.myimdad_por.domain.model

/**
 * Describes what happened in the audit trail.
 */
enum class AuditAction {
    CREATE,
    UPDATE,
    DELETE,
    LOGIN,
    LOGOUT,
    EXPORT_REPORT,
    VOID_INVOICE,
    AUTHORIZE_PAYMENT,
    CHANGE_PASSWORD,
    APPROVE,
    REJECT,
    REFUND,
    SYNC,
    PRINT,
    CUSTOM;

    val isSecurityRelevant: Boolean
        get() = this in setOf(
            LOGIN,
            LOGOUT,
            CHANGE_PASSWORD,
            AUTHORIZE_PAYMENT,
            VOID_INVOICE,
            REFUND,
            REJECT,
            APPROVE
        )
}