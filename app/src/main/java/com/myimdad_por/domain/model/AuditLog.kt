package com.myimdad_por.domain.model

/**
 * Full immutable audit record.
 *
 * Changes are stored as:
 * - key = field name
 * - Pair(first, second) = old value, new value
 */
data class AuditLog(
    val logId: String,
    val timestampMillis: Long = System.currentTimeMillis(),
    val action: AuditAction,
    val severity: AuditSeverity,
    val actor: AuditActor,
    val target: AuditTarget,
    val context: AuditContext? = null,
    val changes: Map<String, Pair<String, String>> = emptyMap(),
    val note: String? = null,
    val correlationId: String? = null,
    val metadata: Map<String, String> = emptyMap()
) {
    init {
        require(logId.isNotBlank()) { "logId cannot be blank." }
        require(timestampMillis > 0L) { "timestampMillis must be greater than zero." }

        changes.forEach { (field, change) ->
            require(field.isNotBlank()) { "change field cannot be blank." }
            require(change.first.isNotBlank() || change.second.isNotBlank()) {
                "change values must not both be blank."
            }
        }

        note?.let {
            require(it.isNotBlank()) { "note cannot be blank when provided." }
        }
        correlationId?.let {
            require(it.isNotBlank()) { "correlationId cannot be blank when provided." }
        }
    }

    val hasChanges: Boolean
        get() = changes.isNotEmpty()

    val isCritical: Boolean
        get() = severity == AuditSeverity.CRITICAL

    val summary: String
        get() = "${action.name} on ${target.displayLabel} by ${actor.displayLabel}"
}