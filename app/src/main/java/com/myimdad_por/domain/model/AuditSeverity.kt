package com.myimdad_por.domain.model

/**
 * Indicates how serious the audited event is.
 */
enum class AuditSeverity(val level: Int) {
    LOW(1),
    MEDIUM(2),
    HIGH(3),
    CRITICAL(4);

    val isAlertWorthy: Boolean
        get() = level >= HIGH.level
}