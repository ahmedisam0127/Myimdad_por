package com.myimdad_por.domain.model

/**
 * Identifies the entity affected by the action.
 */
data class AuditTarget(
    val targetType: String,
    val targetId: String,
    val targetName: String? = null,
    val module: String? = null,
    val metadata: Map<String, String> = emptyMap()
) {
    init {
        require(targetType.isNotBlank()) { "targetType cannot be blank." }
        require(targetId.isNotBlank()) { "targetId cannot be blank." }
        targetName?.let {
            require(it.isNotBlank()) { "targetName cannot be blank when provided." }
        }
        module?.let {
            require(it.isNotBlank()) { "module cannot be blank when provided." }
        }
    }

    val displayLabel: String
        get() = targetName?.takeIf { it.isNotBlank() } ?: "$targetType#$targetId"
}