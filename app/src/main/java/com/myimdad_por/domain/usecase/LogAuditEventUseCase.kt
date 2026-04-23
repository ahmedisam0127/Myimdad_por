package com.myimdad_por.domain.usecase

import com.myimdad_por.domain.model.AuditAction
import com.myimdad_por.domain.model.AuditActor
import com.myimdad_por.domain.model.AuditContext
import com.myimdad_por.domain.model.AuditLog
import com.myimdad_por.domain.model.AuditSeverity
import com.myimdad_por.domain.model.AuditTarget
import com.myimdad_por.domain.repository.AuditLogRepository
import javax.inject.Inject

class LogAuditEventUseCase @Inject constructor(
    private val auditLogRepository: AuditLogRepository
) {

    suspend operator fun invoke(
        action: AuditAction,
        severity: AuditSeverity,
        actor: AuditActor,
        target: AuditTarget,
        context: AuditContext? = null,
        changes: Map<String, Pair<String, String>> = emptyMap(),
        note: String? = null,
        correlationId: String? = null,
        metadata: Map<String, String> = emptyMap()
    ): Result<AuditLog> {
        return auditLogRepository.logEvent(
            action = action,
            severity = severity,
            actor = actor,
            target = target,
            context = context,
            changes = changes,
            note = note,
            correlationId = correlationId,
            metadata = metadata
        )
    }
}