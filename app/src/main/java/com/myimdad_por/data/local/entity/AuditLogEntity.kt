package com.myimdad_por.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "audit_logs",
    indices = [
        Index(value = ["server_id"], unique = true),
        Index(value = ["timestamp_millis"]),
        Index(value = ["action"]),
        Index(value = ["severity"]),
        Index(value = ["correlation_id"]),
        Index(value = ["actor_type"]),
        Index(value = ["target_type"]),
        Index(value = ["sync_state"])
    ]
)
data class AuditLogEntity(
    @PrimaryKey
    @ColumnInfo(name = "log_id")
    val logId: String,

    @ColumnInfo(name = "server_id")
    val serverId: String? = null,

    @ColumnInfo(name = "timestamp_millis")
    val timestampMillis: Long,

    @ColumnInfo(name = "action")
    val action: String,

    @ColumnInfo(name = "severity")
    val severity: String,

    @ColumnInfo(name = "actor_type")
    val actorType: String,

    @ColumnInfo(name = "actor_label")
    val actorLabel: String,

    @ColumnInfo(name = "target_type")
    val targetType: String,

    @ColumnInfo(name = "target_label")
    val targetLabel: String,

    @ColumnInfo(name = "context_json")
    val contextJson: String? = null,

    @ColumnInfo(name = "changes_json")
    val changesJson: String? = null,

    @ColumnInfo(name = "note")
    val note: String? = null,

    @ColumnInfo(name = "correlation_id")
    val correlationId: String? = null,

    @ColumnInfo(name = "metadata_json")
    val metadataJson: String? = null,

    @ColumnInfo(name = "sync_state")
    val syncState: String = "PENDING",

    @ColumnInfo(name = "created_at_millis")
    val createdAtMillis: Long = timestampMillis,

    @ColumnInfo(name = "synced_at_millis")
    val syncedAtMillis: Long? = null
)