package com.myimdad_por.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.myimdad_por.core.network.ApiException
import com.myimdad_por.core.network.NetworkUnavailableException
import com.myimdad_por.core.network.RequestTimeoutException
import com.myimdad_por.data.local.dao.ReportDao
import com.myimdad_por.data.local.entity.ReportEntity
import com.myimdad_por.data.mapper.ReportMapper
import com.myimdad_por.data.remote.datasource.ReportRemoteDataSource
import com.myimdad_por.data.remote.dto.ReportDataPointDto
import com.myimdad_por.data.remote.dto.ReportDto
import com.myimdad_por.data.remote.dto.ReportPeriodDto
import com.myimdad_por.data.remote.dto.ReportSyncRequestDto
import com.myimdad_por.domain.model.Report
import com.myimdad_por.domain.model.ReportDataPoint
import com.myimdad_por.domain.model.ReportExportFormat
import com.myimdad_por.domain.model.ReportPeriod
import com.myimdad_por.domain.model.ReportType
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.net.SocketTimeoutException
import java.net.UnknownHostException

@HiltWorker
class ReportSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val reportDao: ReportDao,
    private val reportRemoteDataSource: ReportRemoteDataSource
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val localReports = reportDao.observeAll().first()
            val request = buildSyncRequest(localReports)

            when (val remote = reportRemoteDataSource.syncReports(request)) {
                is com.myimdad_por.core.network.NetworkResult.Success -> {
                    val syncedReports = remote.data.reports
                        .mapNotNull { it.toDomainOrNull() }

                    if (syncedReports.isNotEmpty()) {
                        reportDao.insertAll(ReportMapper.toEntityList(syncedReports))
                    }

                    Result.success()
                }

                is com.myimdad_por.core.network.NetworkResult.Error -> {
                    remote.exception.toWorkerResult()
                }

                com.myimdad_por.core.network.NetworkResult.Loading -> {
                    Result.retry()
                }
            }
        } catch (t: Throwable) {
            t.toWorkerResult()
        }
    }

    private fun buildSyncRequest(localReports: List<ReportEntity>): ReportSyncRequestDto {
        val lastSyncTimestamp = localReports.maxOfOrNull { it.generatedAtMillis } ?: 0L
        val localReportIds = localReports.map { it.reportId }.distinct()

        return ReportSyncRequestDto(
            lastSyncTimestamp = lastSyncTimestamp,
            localReportIds = localReportIds
        )
    }

    private fun ReportDto.toDomainOrNull(): Report? {
        return runCatching {
            Report(
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
        }.getOrNull()
    }

    private fun ReportPeriodDto.toDomain(): ReportPeriod {
        return ReportPeriod(
            fromMillis = fromMillis,
            toMillis = toMillis
        )
    }

    private fun ReportDataPointDto.toDomainOrNull(): ReportDataPoint? {
        val safeLabel = label.trim()
        return when (kind.trim().uppercase()) {
            "COUNT" -> runCatching {
                ReportDataPoint.Count(
                    label = safeLabel,
                    value = value.trim().toLong()
                )
            }.getOrNull()

            "MONEY" -> runCatching {
                ReportDataPoint.Money(
                    label = safeLabel,
                    value = value.trim().toBigDecimal(),
                    currencyCode = currencyCode.orEmpty().trim()
                )
            }.getOrNull()

            "RATIO" -> runCatching {
                ReportDataPoint.Ratio(
                    label = safeLabel,
                    value = value.trim().toDouble()
                )
            }.getOrNull()

            "TEXT" -> {
                val textValue = value.trim()
                if (safeLabel.isBlank() || textValue.isBlank()) null
                else ReportDataPoint.Text(label = safeLabel, value = textValue)
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

    private fun Throwable.toWorkerResult(): Result {
        return when (this) {
            is ApiException -> {
                when (this) {
                    is NetworkUnavailableException,
                    is RequestTimeoutException -> Result.retry()
                    else -> Result.failure()
                }
            }

            is SocketTimeoutException -> Result.retry()
            is UnknownHostException -> Result.retry()
            else -> Result.failure()
        }
    }

    private fun String.toBigDecimal(): BigDecimal {
        return runCatching {
            BigDecimal(trim())
        }.getOrDefault(BigDecimal.ZERO)
    }
}