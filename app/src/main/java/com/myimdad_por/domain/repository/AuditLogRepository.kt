package com.myimdad_por.domain.repository

import com.myimdad_por.domain.model.AuditAction
import com.myimdad_por.domain.model.AuditContext
import com.myimdad_por.domain.model.AuditLog
import com.myimdad_por.domain.model.AuditSeverity
import kotlinx.coroutines.flow.Flow

/**
 * Contract for audit trail persistence and retrieval.
 *
 * Keep implementations in the data layer and bind them via Hilt.
 */
interface AuditLogRepository {

    fun observeAllLogs(): Flow<List<AuditLog>>

    fun observeLogsByTarget(targetType: String, targetId: String? = null): Flow<List<AuditLog>>

    fun observeLogsByActor(actorId: String): Flow<List<AuditLog>>

    fun observeCriticalLogs(): Flow<List<AuditLog>>

    suspend fun getLogById(logId: String): AuditLog?

    suspend fun getLogs(
        fromMillis: Long? = null,
        toMillis: Long? = null,
        action: AuditAction? = null,
        severity: AuditSeverity? = null,
        actorId: String? = null,
        targetType: String? = null,
        targetId: String? = null,
        correlationId: String? = null
    ): List<AuditLog>

    suspend fun recordLog(log: AuditLog): Result<AuditLog>

    suspend fun recordLogs(logs: List<AuditLog>): Result<List<AuditLog>>

    suspend fun logEvent(
        action: AuditAction,
        severity: AuditSeverity,
        actor: com.myimdad_por.domain.model.AuditActor,
        target: com.myimdad_por.domain.model.AuditTarget,
        context: AuditContext? = null,
        changes: Map<String, Pair<String, String>> = emptyMap(),
        note: String? = null,
        correlationId: String? = null,
        metadata: Map<String, String> = emptyMap()
    ): Result<AuditLog>

    suspend fun deleteLog(logId: String): Result<Unit>

    suspend fun deleteLogsOlderThan(timestampMillis: Long): Result<Int>

    suspend fun clearAll(): Result<Unit>

    suspend fun countLogs(): Long

    suspend fun countLogs(
        fromMillis: Long? = null,
        toMillis: Long? = null,
        action: AuditAction? = null,
        severity: AuditSeverity? = null
    ): Long
}