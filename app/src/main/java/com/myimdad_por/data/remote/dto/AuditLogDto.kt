package com.myimdad_por.data.remote.dto

import com.myimdad_por.domain.model.AuditAction
import com.myimdad_por.domain.model.AuditSeverity
import java.util.Locale

data class AuditLogDto(
    val logId: String,
    val serverId: String? = null,
    val timestampMillis: Long,
    val action: String,
    val severity: String,
    val actorId: String,
    val actorName: String,
    val actorRole: String,
    val actorDeviceId: String? = null,
    val actorEmail: String? = null,
    val actorMetadataJson: String = "{}",
    val targetType: String,
    val targetId: String,
    val targetName: String? = null,
    val targetModule: String? = null,
    val targetMetadataJson: String = "{}",
    val contextJson: String? = null,
    val changesJson: String = "{}",
    val note: String? = null,
    val correlationId: String? = null,
    val metadataJson: String = "{}",
    val syncState: String = "PENDING",
    val isDeleted: Boolean = false,
    val syncedAtMillis: Long? = null,
    val createdAtMillis: Long = timestampMillis,
    val updatedAtMillis: Long = timestampMillis
) {
    init {
        require(logId.isNotBlank()) { "logId cannot be blank." }
        require(timestampMillis > 0L) { "timestampMillis must be greater than zero." }
        require(action.isNotBlank()) { "action cannot be blank." }
        require(severity.isNotBlank()) { "severity cannot be blank." }
        require(actorId.isNotBlank()) { "actorId cannot be blank." }
        require(actorName.isNotBlank()) { "actorName cannot be blank." }
        require(actorRole.isNotBlank()) { "actorRole cannot be blank." }
        require(targetType.isNotBlank()) { "targetType cannot be blank." }
        require(targetId.isNotBlank()) { "targetId cannot be blank." }
        require(createdAtMillis > 0L) { "createdAtMillis must be greater than zero." }
        require(updatedAtMillis > 0L) { "updatedAtMillis must be greater than zero." }
        require(syncState.isNotBlank()) { "syncState cannot be blank." }

        runCatching { AuditAction.valueOf(action.trim().uppercase(Locale.ROOT)) }
        runCatching { AuditSeverity.valueOf(severity.trim().uppercase(Locale.ROOT)) }
    }
}