package com.myimdad_por.domain.usecase

import android.content.Context
import com.myimdad_por.domain.model.AuditAction
import com.myimdad_por.domain.model.AuditActor
import com.myimdad_por.domain.model.AuditContext
import com.myimdad_por.domain.model.AuditLog
import com.myimdad_por.domain.model.AuditSeverity
import com.myimdad_por.domain.model.AuditTarget
import javax.inject.Inject

data class SecurityAuditSnapshot(
    val operationId: String,
    val userId: String?,
    val integrityResult: AppIntegrityCheckResult,
    val metadata: Map<String, String>
)

class RecordSecurityAuditUseCase @Inject constructor(
    private val checkAppIntegrityUseCase: CheckAppIntegrityUseCase,
    private val logAuditEventUseCase: LogAuditEventUseCase
) {

    suspend operator fun invoke(
        context: Context,
        operationId: String,
        action: AuditAction,
        actor: AuditActor,
        target: AuditTarget,
        severity: AuditSeverity = AuditSeverity.CRITICAL,
        userId: String? = null,
        auditContext: AuditContext? = null,
        changes: Map<String, Pair<String, String>> = emptyMap(),
        note: String? = null,
        correlationId: String? = null,
        metadata: Map<String, String> = emptyMap(),
        cloudProjectNumber: Long? = null,
        expectedApkSizeBytes: Long? = null,
        expectedDexSizeBytes: Long? = null,
        trustedInstallers: Set<String> = emptySet()
    ): Result<AuditLog> {
        val integrityResult = checkAppIntegrityUseCase(
            context = context,
            operationId = operationId,
            userId = userId,
            cloudProjectNumber = cloudProjectNumber,
            expectedApkSizeBytes = expectedApkSizeBytes,
            expectedDexSizeBytes = expectedDexSizeBytes,
            trustedInstallers = if (trustedInstallers.isEmpty()) {
                defaultTrustedInstallers
            } else {
                trustedInstallers
            }
        )

        val mergedMetadata = buildMap {
            putAll(metadata)
            put("operation_id", operationId)
            put("user_id", userId.orEmpty())
            put("integrity_trusted", integrityResult.isTrusted.toString())
            put("integrity_risk_score", integrityResult.riskScore.toString())
            put("integrity_risk_reasons", integrityResult.riskReasons.joinToString(separator = "|"))
            put("integrity_nonce", integrityResult.challengeNonce)
            put("tamper_compromised", integrityResult.tamperStatus.compromised.toString())
        }

        val effectiveNote = buildString {
            append(note?.trim().takeUnless { it.isNullOrBlank() } ?: "Security audit event")
            append(" | trusted=").append(integrityResult.isTrusted)
            append(" | riskScore=").append(integrityResult.riskScore)
        }

        return logAuditEventUseCase(
            action = action,
            severity = severity,
            actor = actor,
            target = target,
            context = auditContext,
            changes = changes,
            note = effectiveNote,
            correlationId = correlationId,
            metadata = mergedMetadata
        )
    }

    fun preview(
        context: Context,
        operationId: String,
        userId: String? = null,
        cloudProjectNumber: Long? = null,
        expectedApkSizeBytes: Long? = null,
        expectedDexSizeBytes: Long? = null,
        trustedInstallers: Set<String> = emptySet(),
        metadata: Map<String, String> = emptyMap()
    ): SecurityAuditSnapshot {
        val integrityResult = checkAppIntegrityUseCase(
            context = context,
            operationId = operationId,
            userId = userId,
            cloudProjectNumber = cloudProjectNumber,
            expectedApkSizeBytes = expectedApkSizeBytes,
            expectedDexSizeBytes = expectedDexSizeBytes,
            trustedInstallers = if (trustedInstallers.isEmpty()) {
                defaultTrustedInstallers
            } else {
                trustedInstallers
            }
        )

        val mergedMetadata = buildMap {
            putAll(metadata)
            put("operation_id", operationId)
            put("user_id", userId.orEmpty())
            put("integrity_trusted", integrityResult.isTrusted.toString())
            put("integrity_risk_score", integrityResult.riskScore.toString())
        }

        return SecurityAuditSnapshot(
            operationId = operationId,
            userId = userId,
            integrityResult = integrityResult,
            metadata = mergedMetadata
        )
    }

    private companion object {
        val defaultTrustedInstallers = setOf(
            "com.android.vending",
            "com.amazon.venezia"
        )
    }
}