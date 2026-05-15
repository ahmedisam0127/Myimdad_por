package com.myimdad_por.domain.model

/**
 * Identifies who performed the action.
 */
data class AuditActor(
    val actorId: String,
    val actorName: String,
    val role: String,
    val deviceId: String? = null,
    val email: String? = null,
    val metadata: Map<String, String> = emptyMap()
) {
    init {
        require(actorId.isNotBlank()) { "actorId cannot be blank." }
        require(actorName.isNotBlank()) { "actorName cannot be blank." }
        require(role.isNotBlank()) { "role cannot be blank." }
        deviceId?.let {
            require(it.isNotBlank()) { "deviceId cannot be blank when provided." }
        }
        email?.let {
            require(it.isNotBlank()) { "email cannot be blank when provided." }
        }
    }

    val displayLabel: String
        get() = "$actorName ($role)"
}