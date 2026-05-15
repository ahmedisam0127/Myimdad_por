package com.myimdad_por.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import org.json.JSONObject
import java.util.UUID

@Entity(
    tableName = "pending_sync",
    indices = [
        Index(value = ["entity_type"]),
        Index(value = ["entity_id"]),
        Index(value = ["operation"]),
        Index(value = ["status"]),
        Index(value = ["next_attempt_at_millis"]),
        Index(value = ["priority"]),
        Index(value = ["entity_type", "entity_id", "operation"], unique = true)
    ]
)
data class PendingSyncEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String = UUID.randomUUID().toString(),

    @ColumnInfo(name = "entity_type")
    val entityType: String,

    @ColumnInfo(name = "entity_id")
    val entityId: String,

    @ColumnInfo(name = "operation")
    val operation: String,

    @ColumnInfo(name = "status")
    val status: String = PendingSyncStatus.PENDING.name,

    @ColumnInfo(name = "priority")
    val priority: Int = 0,

    @ColumnInfo(name = "attempt_count")
    val attemptCount: Int = 0,

    @ColumnInfo(name = "max_retry_count")
    val maxRetryCount: Int = 10,

    @ColumnInfo(name = "payload_json")
    val payloadJson: String = "{}",

    @ColumnInfo(name = "last_error_message")
    val lastErrorMessage: String? = null,

    @ColumnInfo(name = "next_attempt_at_millis")
    val nextAttemptAtMillis: Long? = null,

    @ColumnInfo(name = "locked_at_millis")
    val lockedAtMillis: Long? = null,

    @ColumnInfo(name = "completed_at_millis")
    val completedAtMillis: Long? = null,

    @ColumnInfo(name = "metadata_json")
    val metadataJson: String = "{}",

    @ColumnInfo(name = "created_at_millis")
    val createdAtMillis: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at_millis")
    val updatedAtMillis: Long = System.currentTimeMillis()
) {
    init {
        require(entityType.isNotBlank()) { "entityType cannot be blank." }
        require(entityId.isNotBlank()) { "entityId cannot be blank." }
        require(operation.isNotBlank()) { "operation cannot be blank." }
        require(priority >= 0) { "priority cannot be negative." }
        require(attemptCount >= 0) { "attemptCount cannot be negative." }
        require(maxRetryCount > 0) { "maxRetryCount must be greater than zero." }
    }

    fun markInProgress(nowMillis: Long = System.currentTimeMillis()): PendingSyncEntity {
        return copy(
            status = PendingSyncStatus.IN_PROGRESS.name,
            lockedAtMillis = nowMillis,
            updatedAtMillis = nowMillis
        )
    }

    fun markRetry(
        nextAttemptAtMillis: Long,
        errorMessage: String? = null,
        nowMillis: Long = System.currentTimeMillis()
    ): PendingSyncEntity {
        return copy(
            status = PendingSyncStatus.RETRY.name,
            attemptCount = attemptCount + 1,
            lastErrorMessage = errorMessage,
            nextAttemptAtMillis = nextAttemptAtMillis,
            lockedAtMillis = null,
            updatedAtMillis = nowMillis
        )
    }

    fun markCompleted(nowMillis: Long = System.currentTimeMillis()): PendingSyncEntity {
        return copy(
            status = PendingSyncStatus.COMPLETED.name,
            completedAtMillis = nowMillis,
            lockedAtMillis = null,
            updatedAtMillis = nowMillis
        )
    }

    fun markFailed(
        errorMessage: String? = null,
        nowMillis: Long = System.currentTimeMillis()
    ): PendingSyncEntity {
        return copy(
            status = PendingSyncStatus.FAILED.name,
            lastErrorMessage = errorMessage,
            lockedAtMillis = null,
            updatedAtMillis = nowMillis
        )
    }

    fun canRetry(): Boolean {
        return attemptCount < maxRetryCount && status != PendingSyncStatus.COMPLETED.name
    }

    companion object {
        fun create(
            entityType: String,
            entityId: String,
            operation: PendingSyncOperation,
            payloadJson: String = "{}",
            priority: Int = 0,
            metadata: Map<String, String> = emptyMap()
        ): PendingSyncEntity {
            return PendingSyncEntity(
                entityType = entityType,
                entityId = entityId,
                operation = operation.name,
                payloadJson = payloadJson,
                priority = priority,
                metadataJson = metadata.toJsonString()
            )
        }
    }
}

enum class PendingSyncOperation {
    CREATE,
    UPDATE,
    DELETE,
    UPSERT,
    REPAIR
}

enum class PendingSyncStatus {
    PENDING,
    IN_PROGRESS,
    RETRY,
    COMPLETED,
    FAILED,
    CANCELED
}

private fun Map<String, String>.toJsonString(): String {
    return JSONObject(this).toString()
}