package com.myimdad_por.domain.usecase

import com.myimdad_por.domain.model.Report
import com.myimdad_por.domain.model.ReportExportFormat
import com.myimdad_por.domain.model.ReportPeriod
import com.myimdad_por.domain.model.ReportType
import com.myimdad_por.domain.repository.ReportRequest
import com.myimdad_por.domain.repository.ReportsRepository
import java.math.BigDecimal
import javax.inject.Inject

data class GetReportsRequest(
    val reportId: String? = null,
    val type: ReportType? = null,
    val fromMillis: Long? = null,
    val toMillis: Long? = null,
    val period: ReportPeriod? = null,
    val filters: Map<String, String> = emptyMap(),
    val exportFormat: ReportExportFormat? = null,
    val generatedByUserId: String? = null,
    val generateIfEmpty: Boolean = false,
    val useGeneratorWhenPossible: Boolean = false
) {
    init {
        reportId?.let {
            require(it.isNotBlank()) { "reportId cannot be blank when provided." }
        }
        generatedByUserId?.let {
            require(it.isNotBlank()) { "generatedByUserId cannot be blank when provided." }
        }
    }
}

data class GetReportsResult(
    val reports: List<Report>,
    val totalNumericValue: BigDecimal,
    val count: Int,
    val requestedType: ReportType? = null,
    val period: ReportPeriod? = null,
    val source: Source,
    val notes: List<String> = emptyList()
) {
    enum class Source {
        SINGLE_LOOKUP,
        QUERY,
        GENERATED
    }

    val hasData: Boolean
        get() = reports.isNotEmpty()
}

class GetReportsUseCase @Inject constructor(
    private val reportsRepository: ReportsRepository
) {

    suspend operator fun invoke(request: GetReportsRequest): Result<GetReportsResult> {
        return runCatching {
            val notes = mutableListOf<String>()
            val period = request.period ?: buildPeriod(request.fromMillis, request.toMillis)
            val reports = when {
                !request.reportId.isNullOrBlank() -> {
                    val report = reportsRepository.getReportById(request.reportId.trim())
                    if (report == null) {
                        emptyList()
                    } else {
                        listOf(report)
                    }
                }

                request.useGeneratorWhenPossible && request.type != null && period != null -> {
                    val generated = generateReport(
                        type = request.type,
                        period = period,
                        filters = request.filters,
                        exportFormat = request.exportFormat,
                        generatedByUserId = request.generatedByUserId
                    )
                    notes += "generated"
                    listOf(generated)
                }

                else -> {
                    reportsRepository.getReports(
                        from = request.fromMillis,
                        to = request.toMillis,
                        type = request.type
                    )
                }
            }

            val sortedReports = reports.sortedByDescending { it.generatedAtMillis }
            val source = when {
                !request.reportId.isNullOrBlank() -> GetReportsResult.Source.SINGLE_LOOKUP
                request.useGeneratorWhenPossible && request.type != null && period != null -> GetReportsResult.Source.GENERATED
                else -> GetReportsResult.Source.QUERY
            }

            GetReportsResult(
                reports = sortedReports,
                totalNumericValue = sortedReports.fold(BigDecimal.ZERO) { acc, report ->
                    acc + report.totalNumericValue
                },
                count = sortedReports.size,
                requestedType = request.type,
                period = period,
                source = source,
                notes = notes
            )
        }
    }

    private suspend fun generateReport(
        type: ReportType,
        period: ReportPeriod,
        filters: Map<String, String>,
        exportFormat: ReportExportFormat?,
        generatedByUserId: String?
    ): Report {
        return when (type) {
            ReportType.SALES -> reportsRepository.generateSalesReport(period, filters).getOrThrow()
            ReportType.INVENTORY -> reportsRepository.generateInventoryReport(period, filters).getOrThrow()
            ReportType.FINANCIAL -> reportsRepository.generateFinancialReport(period, filters).getOrThrow()
            ReportType.PERFORMANCE -> reportsRepository.generatePerformanceReport(
                period = period,
                employeeId = filters["employeeId"],
                filters = filters
            ).getOrThrow()

            ReportType.CUSTOM -> reportsRepository.generateReport(
                ReportRequest(
                    title = buildTitle(type, period),
                    type = type,
                    period = period,
                    filters = filters,
                    exportFormat = exportFormat,
                    generatedByUserId = generatedByUserId
                )
            ).getOrThrow()

            ReportType.TAX,
            ReportType.OPERATIONAL -> reportsRepository.generateReport(
                ReportRequest(
                    title = buildTitle(type, period),
                    type = type,
                    period = period,
                    filters = filters,
                    exportFormat = exportFormat,
                    generatedByUserId = generatedByUserId
                )
            ).getOrThrow()
        }
    }

    private fun buildPeriod(fromMillis: Long?, toMillis: Long?): ReportPeriod? {
        return when {
            fromMillis == null && toMillis == null -> null
            else -> ReportPeriod(
                fromMillis = fromMillis,
                toMillis = toMillis
            )
        }
    }

    private fun buildTitle(type: ReportType, period: ReportPeriod): String {
        val fromPart = period.fromMillis?.toString() ?: "open"
        val toPart = period.toMillis?.toString() ?: "open"
        return "${type.name.lowercase().replaceFirstChar { it.uppercase() }} Report [$fromPart → $toPart]"
    }
}