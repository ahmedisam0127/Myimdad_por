package com.myimdad_por.domain.repository

import com.myimdad_por.domain.model.Report
import com.myimdad_por.domain.model.ReportExportFormat
import com.myimdad_por.domain.model.ReportPeriod
import com.myimdad_por.domain.model.ReportType
import java.math.BigDecimal
import java.time.LocalDateTime
import kotlinx.coroutines.flow.Flow

/**
 * Analytical contract for business intelligence, scheduled reports,
 * and export-ready datasets.
 */
interface ReportsRepository {

    fun observeGeneratedReports(): Flow<List<Report>>

    fun observeLatestReports(limit: Int = 10): Flow<List<Report>>

    suspend fun getReportById(reportId: String): Report?

    suspend fun getReports(
        from: Long? = null,
        to: Long? = null,
        type: ReportType? = null
    ): List<Report>

    suspend fun generateReport(request: ReportRequest): Result<Report>

    suspend fun generateScheduledReport(
        schedule: ReportSchedule
    ): Result<Report>

    suspend fun generateSalesReport(
        period: ReportPeriod,
        filters: Map<String, String> = emptyMap()
    ): Result<Report>

    suspend fun generateInventoryReport(
        period: ReportPeriod,
        filters: Map<String, String> = emptyMap()
    ): Result<Report>

    suspend fun generateFinancialReport(
        period: ReportPeriod,
        filters: Map<String, String> = emptyMap()
    ): Result<Report>

    suspend fun generatePerformanceReport(
        period: ReportPeriod,
        employeeId: String? = null,
        filters: Map<String, String> = emptyMap()
    ): Result<Report>

    suspend fun generateDeadStockReport(
        period: ReportPeriod,
        deadDaysThreshold: Int,
        filters: Map<String, String> = emptyMap()
    ): Result<Report>

    suspend fun generateProfitAndLossReport(
        period: ReportPeriod,
        filters: Map<String, String> = emptyMap()
    ): Result<Report>

    suspend fun generateCashFlowReport(
        period: ReportPeriod,
        filters: Map<String, String> = emptyMap()
    ): Result<Report>

    suspend fun exportReport(
        reportId: String,
        format: ReportExportFormat
    ): Result<ReportExportResult>

    suspend fun deleteReport(reportId: String): Result<Unit>

    suspend fun deleteReports(reportIds: List<String>): Result<Int>

    suspend fun clearAll(): Result<Unit>

    suspend fun countReports(): Long

    suspend fun countReportsByType(type: ReportType): Long

    suspend fun getRevenueSummary(
        period: ReportPeriod
    ): BigDecimal

    suspend fun getInventoryValueSummary(
        period: ReportPeriod
    ): BigDecimal
}

/**
 * Request object for on-demand analytics.
 */
data class ReportRequest(
    val title: String,
    val type: ReportType,
    val period: ReportPeriod,
    val filters: Map<String, String> = emptyMap(),
    val exportFormat: ReportExportFormat? = null,
    val generatedByUserId: String? = null,
    val metadata: Map<String, String> = emptyMap()
) {
    init {
        require(title.isNotBlank()) { "title cannot be blank." }
        generatedByUserId?.let {
            require(it.isNotBlank()) { "generatedByUserId cannot be blank when provided." }
        }
    }
}

/**
 * Definition for a recurring report job.
 */
data class ReportSchedule(
    val scheduleId: String,
    val title: String,
    val type: ReportType,
    val period: ReportPeriod,
    val exportFormat: ReportExportFormat? = null,
    val cronExpression: String? = null,
    val isActive: Boolean = true,
    val nextRunAtMillis: Long? = null,
    val metadata: Map<String, String> = emptyMap()
) {
    init {
        require(scheduleId.isNotBlank()) { "scheduleId cannot be blank." }
        require(title.isNotBlank()) { "title cannot be blank." }
        cronExpression?.let {
            require(it.isNotBlank()) { "cronExpression cannot be blank when provided." }
        }
        nextRunAtMillis?.let {
            require(it > 0L) { "nextRunAtMillis must be greater than zero when provided." }
        }
    }
}

/**
 * Export operation result.
 */
data class ReportExportResult(
    val reportId: String,
    val format: ReportExportFormat,
    val fileName: String? = null,
    val filePath: String? = null,
    val exportedAtMillis: Long = System.currentTimeMillis(),
    val metadata: Map<String, String> = emptyMap()
) {
    init {
        require(reportId.isNotBlank()) { "reportId cannot be blank." }
        fileName?.let {
            require(it.isNotBlank()) { "fileName cannot be blank when provided." }
        }
        filePath?.let {
            require(it.isNotBlank()) { "filePath cannot be blank when provided." }
        }
    }
}