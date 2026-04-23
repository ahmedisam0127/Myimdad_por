package com.myimdad_por.domain.model

/**
 * Environmental information around the audited action.
 */
data class AuditContext(
    val ipAddress: String? = null,
    val userAgent: String? = null,
    val location: String? = null,
    val sessionId: String? = null,
    val requestId: String? = null,
    val deviceModel: String? = null,
    val appVersion: String? = null,
    val isOffline: Boolean = false,
    val metadata: Map<String, String> = emptyMap()
) {
    init {
        ipAddress?.let {
            require(it.isNotBlank()) { "ipAddress cannot be blank when provided." }
        }
        userAgent?.let {
            require(it.isNotBlank()) { "userAgent cannot be blank when provided." }
        }
        location?.let {
            require(it.isNotBlank()) { "location cannot be blank when provided." }
        }
        sessionId?.let {
            require(it.isNotBlank()) { "sessionId cannot be blank when provided." }
        }
        requestId?.let {
            require(it.isNotBlank()) { "requestId cannot be blank when provided." }
        }
        deviceModel?.let {
            require(it.isNotBlank()) { "deviceModel cannot be blank when provided." }
        }
        appVersion?.let {
            require(it.isNotBlank()) { "appVersion cannot be blank when provided." }
        }
    }
}