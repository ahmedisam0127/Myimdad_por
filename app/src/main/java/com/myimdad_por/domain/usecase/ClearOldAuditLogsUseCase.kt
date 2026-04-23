package com.myimdad_por.domain.usecase

import com.myimdad_por.domain.repository.AuditLogRepository
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * ينظف سجلات التدقيق الأقدم من مدة احتفاظ محددة.
 */
class ClearOldAuditLogsUseCase @Inject constructor(
    private val auditLogRepository: AuditLogRepository
) {

    suspend operator fun invoke(
        retentionDays: Long,
        currentTimeMillis: Long = System.currentTimeMillis()
    ): Result<Int> {
        require(retentionDays > 0) { "retentionDays must be greater than zero" }

        val cutoffMillis = currentTimeMillis - TimeUnit.DAYS.toMillis(retentionDays)
        return auditLogRepository.deleteLogsOlderThan(cutoffMillis)
    }
}