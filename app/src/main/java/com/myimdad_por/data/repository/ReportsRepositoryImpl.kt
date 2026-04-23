package com.myimdad_por.data.repository

import com.myimdad_por.core.network.NetworkResult
import com.myimdad_por.data.local.dao.ReportDao
import com.myimdad_por.data.mapper.ReportMapper
import com.myimdad_por.data.remote.datasource.ReportRemoteDataSource
import com.myimdad_por.data.remote.dto.ReportDataPointDto
import com.myimdad_por.data.remote.dto.ReportDto
import com.myimdad_por.data.remote.dto.ReportExportRequestDto
import com.myimdad_por.data.remote.dto.ReportGenerateRequestDto
import com.myimdad_por.domain.model.Report
import com.myimdad_por.domain.model.ReportDataPoint
import com.myimdad_por.domain.model.ReportExportFormat
import com.myimdad_por.domain.model.ReportPeriod
import com.myimdad_por.domain.model.ReportType
import com.myimdad_por.domain.repository.ReportExportResult
import com.myimdad_por.domain.repository.ReportRequest
import com.myimdad_por.domain.repository.ReportSchedule
import com.myimdad_por.domain.repository.ReportsRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import javax.inject.Inject

class ReportsRepositoryImpl @Inject constructor(
    private val reportDao: ReportDao,
    private val reportRemoteDataSource: ReportRemoteDataSource
) : ReportsRepository {

    override fun observeGeneratedReports(): Flow<List<Report>> {
        return reportDao.observeAll().map(ReportMapper::toDomainList)
    }

    override fun observeLatestReports(limit: Int): Flow<List<Report>> {
        val safeLimit = limit.coerceAtLeast(0)
        return reportDao.observeAll().map { entities ->
            ReportMapper.toDomainList(entities).take(safeLimit)
        }
    }

    override suspend fun getReportById(reportId: String): Report? = withContext(Dispatchers.IO) {
        reportDao.getById(reportId)?.let(ReportMapper::toDomain)
            ?: when (val remote = reportRemoteDataSource.getReportById(reportId)) {
                is NetworkResult.Success -> {
                    val report = remote.data.toDomain()
                    reportDao.insert(ReportMapper.toEntity(report))
                    report
                }
                is NetworkResult.Error -> null
                NetworkResult.Loading -> null
            }
    }

    override suspend fun getReports(
        from: Long?,
        to: Long?,
        type: ReportType?
    ): List<Report> = withContext(Dispatchers.IO) {
        when (val remote = reportRemoteDataSource.getReports(
            fromMillis = from,
            toMillis = to,
            type = type?.name
        )) {
            is NetworkResult.Success -> {
                val reports = remote.data.reports.mapNotNull { it.toDomainOrNull() }
                cacheReports(reports)
                reports.filterReports(from = from, to = to, type = type)
            }

            is NetworkResult.Error -> {
                val localReports = ReportMapper.toDomainList(reportDao.observeAll().first())
                localReports.filterReports(from = from, to = to, type = type)
            }

            NetworkResult.Loading -> emptyList()
        }
    }

    override suspend fun generateReport(request: ReportRequest): Result<Report> {
        return safeCall {
            val remoteRequest = request.toGenerateRequestDto()

            when (val remote = reportRemoteDataSource.createReport(remoteRequest)) {
                is NetworkResult.Success -> {
                    val report = remote.data.toDomain()
                    reportDao.insert(ReportMapper.toEntity(report))
                    report
                }

                is NetworkResult.Error -> throw remote.exception
                NetworkResult.Loading -> error("Unexpected loading state")
            }
        }
    }

    override suspend fun generateScheduledReport(schedule: ReportSchedule): Result<Report> {
        val request = ReportRequest(
            title = schedule.title,
            type = schedule.type,
            period = schedule.period,
            exportFormat = schedule.exportFormat,
            metadata = buildMap {
                putAll(schedule.metadata)
                put("schedule_id", schedule.scheduleId)
                put("is_active", schedule.isActive.toString())
                schedule.cronExpression?.let { put("cron_expression", it) }
                schedule.nextRunAtMillis?.let { put("next_run_at_millis", it.toString()) }
            }
        )
        return generateReport(request)
    }

    override suspend fun generateSalesReport(
        period: ReportPeriod,
        filters: Map<String, String>
    ): Result<Report> {
        return generateReport(
            ReportRequest(
                title = "Sales Report",
                type = ReportType.SALES,
                period = period,
                filters = filters
            )
        )
    }

    override suspend fun generateInventoryReport(
        period: ReportPeriod,
        filters: Map<String, String>
    ): Result<Report> {
        return generateReport(
            ReportRequest(
                title = "Inventory Report",
                type = ReportType.INVENTORY,
                period = period,
                filters = filters
            )
        )
    }

    override suspend fun generateFinancialReport(
        period: ReportPeriod,
        filters: Map<String, String>
    ): Result<Report> {
        return generateReport(
            ReportRequest(
                title = "Financial Report",
                type = ReportType.FINANCIAL,
                period = period,
                filters = filters
            )
        )
    }

    override suspend fun generatePerformanceReport(
        period: ReportPeriod,
        employeeId: String?,
        filters: Map<String, String>
    ): Result<Report> {
        return generateReport(
            ReportRequest(
                title = "Performance Report",
                type = ReportType.PERFORMANCE,
                period = period,
                filters = buildMap {
                    putAll(filters)
                    employeeId?.let { put("employee_id", it) }
                }
            )
        )
    }

    override suspend fun generateDeadStockReport(
        period: ReportPeriod,
        deadDaysThreshold: Int,
        filters: Map<String, String>
    ): Result<Report> {
        return generateReport(
            ReportRequest(
                title = "Dead Stock Report",
                type = ReportType.INVENTORY,
                period = period,
                filters = buildMap {
                    putAll(filters)
                    put("dead_days_threshold", deadDaysThreshold.toString())
                },
                metadata = mapOf("report_scope" to "dead_stock")
            )
        )
    }

    override suspend fun generateProfitAndLossReport(
        period: ReportPeriod,
        filters: Map<String, String>
    ): Result<Report> {
        return generateReport(
            ReportRequest(
                title = "Profit and Loss Report",
                type = ReportType.FINANCIAL,
                period = period,
                filters = filters,
                metadata = mapOf("report_scope" to "profit_and_loss")
            )
        )
    }

    override suspend fun generateCashFlowReport(
        period: ReportPeriod,
        filters: Map<String, String>
    ): Result<Report> {
        return generateReport(
            ReportRequest(
                title = "Cash Flow Report",
                type = ReportType.FINANCIAL,
                period = period,
                filters = filters,
                metadata = mapOf("report_scope" to "cash_flow")
            )
        )
    }

    override suspend fun exportReport(
        reportId: String,
        format: ReportExportFormat
    ): Result<ReportExportResult> {
        return safeCall {
            when (val remote = reportRemoteDataSource.exportReport(
                reportId = reportId,
                request = ReportExportRequestDto(format = format.name)
            )) {
                is NetworkResult.Success -> {
                    val report = remote.data.toDomain()
                    reportDao.insert(ReportMapper.toEntity(report))
                    reportDao.markAsExported(reportId)

                    ReportExportResult(
                        reportId = report.reportId,
                        format = format,
                        fileName = buildExportFileName(
                            title = report.title,
                            reportId = report.reportId,
                            format = format
                        ),
                        filePath = null,
                        exportedAtMillis = report.generatedAtMillis,
                        metadata = report.metadata
                    )
                }

                is NetworkResult.Error -> throw remote.exception
                NetworkResult.Loading -> error("Unexpected loading state")
            }
        }
    }

    override suspend fun deleteReport(reportId: String): Result<Unit> {
        return safeCall {
            when (val remote = reportRemoteDataSource.deleteReport(reportId)) {
                is NetworkResult.Success -> {
                    reportDao.deleteById(reportId)
                    Unit
                }

                is NetworkResult.Error -> throw remote.exception
                NetworkResult.Loading -> error("Unexpected loading state")
            }
        }
    }

    override suspend fun deleteReports(reportIds: List<String>): Result<Int> {
        return safeCall {
            if (reportIds.isEmpty()) return@safeCall 0

            when (val remote = reportRemoteDataSource.deleteReportsBulk(reportIds)) {
                is NetworkResult.Success -> {
                    var deletedCount = 0
                    reportIds.forEach { id ->
                        deletedCount += reportDao.deleteById(id)
                    }
                    deletedCount
                }

                is NetworkResult.Error -> throw remote.exception
                NetworkResult.Loading -> error("Unexpected loading state")
            }
        }
    }

    override suspend fun clearAll(): Result<Unit> {
        return safeCall {
            reportDao.deleteAll()
            Unit
        }
    }

    override suspend fun countReports(): Long {
        return withContext(Dispatchers.IO) {
            reportDao.observeCount().first().toLong()
        }
    }

    override suspend fun countReportsByType(type: ReportType): Long {
        return withContext(Dispatchers.IO) {
            reportDao.observeCountByType(type.name).first().toLong()
        }
    }

    override suspend fun getRevenueSummary(period: ReportPeriod): BigDecimal {
        return withContext(Dispatchers.IO) {
            val reports = getReports(
                from = period.fromMillis,
                to = period.toMillis,
                type = ReportType.SALES
            )
            reports.fold(BigDecimal.ZERO) { acc, report -> acc.add(report.totalNumericValue) }
        }
    }

    override suspend fun getInventoryValueSummary(period: ReportPeriod): BigDecimal {
        return withContext(Dispatchers.IO) {
            val reports = getReports(
                from = period.fromMillis,
                to = period.toMillis,
                type = ReportType.INVENTORY
            )
            reports.fold(BigDecimal.ZERO) { acc, report -> acc.add(report.totalNumericValue) }
        }
    }

    private suspend fun cacheReports(reports: List<Report>) {
        if (reports.isNotEmpty()) {
            reportDao.insertAll(ReportMapper.toEntityList(reports))
        }
    }

    private fun ReportDto.toDomain(): Report {
        return Report(
            reportId = reportId,
            title = title,
            type = type.toReportType(),
            generatedAtMillis = generatedAtMillis,
            period = period?.toDomain(),
            filters = filters.orEmpty(),
            dataPoints = dataPoints.orEmpty().mapNotNull { it.toDomainOrNull() },
            summary = summary,
            exported = exported,
            exportFormat = exportFormat.toReportExportFormat(),
            generatedByUserId = generatedByUserId,
            metadata = metadata.orEmpty()
        )
    }

    private fun ReportDto.toDomainOrNull(): Report? {
        return runCatching { toDomain() }.getOrNull()
    }

    private fun com.myimdad_por.data.remote.dto.ReportPeriodDto.toDomain(): ReportPeriod {
        return ReportPeriod(
            fromMillis = fromMillis,
            toMillis = toMillis
        )
    }

    private fun ReportDataPointDto.toDomainOrNull(): ReportDataPoint? {
        return when (kind.trim().uppercase()) {
            "COUNT" -> runCatching {
                ReportDataPoint.Count(
                    label = label,
                    value = value.trim().toLong()
                )
            }.getOrNull()

            "MONEY" -> runCatching {
                ReportDataPoint.Money(
                    label = label,
                    value = value.trim().toBigDecimal(),
                    currencyCode = currencyCode.orEmpty()
                )
            }.getOrNull()

            "RATIO" -> runCatching {
                ReportDataPoint.Ratio(
                    label = label,
                    value = value.trim().toDouble()
                )
            }.getOrNull()

            "TEXT" -> {
                val textValue = value.trim()
                if (label.isBlank() || textValue.isBlank()) null
                else ReportDataPoint.Text(label = label, value = textValue)
            }

            else -> null
        }
    }

    private fun String.toReportType(): ReportType {
        return runCatching {
            ReportType.valueOf(trim().uppercase())
        }.getOrDefault(ReportType.CUSTOM)
    }

    private fun String?.toReportExportFormat(): ReportExportFormat? {
        if (this.isNullOrBlank()) return null
        return runCatching {
            ReportExportFormat.valueOf(trim().uppercase())
        }.getOrNull()
    }

    private fun ReportRequest.toGenerateRequestDto(): ReportGenerateRequestDto {
        return ReportGenerateRequestDto(
            title = title,
            type = type.name,
            fromMillis = period.fromMillis,
            toMillis = period.toMillis,
            filters = filters,
            exportFormat = exportFormat?.name,
            metadata = metadata
        )
    }

    private fun buildExportFileName(
        title: String,
        reportId: String,
        format: ReportExportFormat
    ): String {
        val safeTitle = title
            .trim()
            .lowercase()
            .replace(Regex("[^a-z0-9\\u0600-\\u06FF]+"), "_")
            .trim('_')
            .ifBlank { "report" }

        return "${safeTitle}_${reportId}.${format.name.lowercase()}"
    }

    private fun List<Report>.filterReports(
        from: Long?,
        to: Long?,
        type: ReportType?
    ): List<Report> {
        return asSequence()
            .filter { report ->
                val matchesFrom = from == null || report.generatedAtMillis >= from
                val matchesTo = to == null || report.generatedAtMillis <= to
                val matchesType = type == null || report.type == type
                matchesFrom && matchesTo && matchesType
            }
            .sortedByDescending { it.generatedAtMillis }
            .toList()
    }

    private suspend fun <T> safeCall(block: suspend () -> T): Result<T> {
        return withContext(Dispatchers.IO) {
            try {
                Result.success(block())
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                Result.failure(e)
            }
        }
    }
}