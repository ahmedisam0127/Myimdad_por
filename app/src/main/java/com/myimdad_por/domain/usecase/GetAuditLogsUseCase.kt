package com.myimdad_por.domain.usecase

import com.myimdad_por.domain.model.AuditAction
import com.myimdad_por.domain.model.AuditLog
import com.myimdad_por.domain.model.AuditSeverity
import com.myimdad_por.domain.repository.AuditLogRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

data class AuditLogQuery(
    val fromMillis: Long? = null,
    val toMillis: Long? = null,
    val action: AuditAction? = null,
    val severity: AuditSeverity? = null,
    val actorId: String? = null,
    val targetType: String? = null,
    val targetId: String? = null,
    val correlationId: String? = null
)

/**
 * محرك استعلامات سجلات التدقيق.
 * يدعم القراءة المباشرة والقراءة التفاعلية عبر Flow.
 */
class GetAuditLogsUseCase @Inject constructor(
    private val auditLogRepository: AuditLogRepository
) {

    suspend operator fun invoke(query: AuditLogQuery = AuditLogQuery()): List<AuditLog> {
        return auditLogRepository.getLogs(
            fromMillis = query.fromMillis,
            toMillis = query.toMillis,
            action = query.action,
            severity = query.severity,
            actorId = query.actorId,
            targetType = query.targetType,
            targetId = query.targetId,
            correlationId = query.correlationId
        )
    }

    fun observeAll(): Flow<List<AuditLog>> {
        return auditLogRepository.observeAllLogs()
    }

    fun observeByTarget(
        targetType: String,
        targetId: String? = null
    ): Flow<List<AuditLog>> {
        require(targetType.isNotBlank()) { "targetType cannot be blank" }
        return auditLogRepository.observeLogsByTarget(targetType, targetId)
    }

    fun observeByActor(actorId: String): Flow<List<AuditLog>> {
        require(actorId.isNotBlank()) { "actorId cannot be blank" }
        return auditLogRepository.observeLogsByActor(actorId)
    }

    fun observeCritical(): Flow<List<AuditLog>> {
        return auditLogRepository.observeCriticalLogs()
    }

    suspend fun getById(logId: String): AuditLog? {
        require(logId.isNotBlank()) { "logId cannot be blank" }
        return auditLogRepository.getLogById(logId)
    }

    suspend fun count(query: AuditLogQuery = AuditLogQuery()): Long {
        return auditLogRepository.countLogs(
            fromMillis = query.fromMillis,
            toMillis = query.toMillis,
            action = query.action,
            severity = query.severity
        )
    }
}