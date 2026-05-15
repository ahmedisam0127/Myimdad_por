package com.myimdad_por.data.mapper

import com.myimdad_por.data.local.entity.ReportEntity
import com.myimdad_por.domain.model.Report
import com.myimdad_por.domain.model.ReportDataPoint
import com.myimdad_por.domain.model.ReportExportFormat
import com.myimdad_por.domain.model.ReportPeriod
import com.myimdad_por.domain.model.ReportType
import org.json.JSONArray
import org.json.JSONObject
import java.math.BigDecimal

object ReportMapper {

    fun toDomain(entity: ReportEntity): Report {
        return Report(
            reportId = entity.reportId,
            title = entity.title,
            type = entity.type.toReportType(),
            generatedAtMillis = entity.generatedAtMillis,
            period = entity.toReportPeriod(),
            filters = entity.filtersJson.toStringMap(),
            dataPoints = entity.dataPointsJson.toReportDataPoints(),
            summary = entity.summary,
            exported = entity.isExported, // تم التصحيح من exported إلى isExported
            exportFormat = entity.exportFormat.toReportExportFormat(),
            generatedByUserId = entity.generatedByUserId,
            metadata = entity.metadataJson.toStringMap()
        )
    }

    fun toEntity(domain: Report): ReportEntity {
        return ReportEntity(
            reportId = domain.reportId,
            title = domain.title,
            type = domain.type.name,
            generatedAtMillis = domain.generatedAtMillis,
            fromMillis = domain.period?.fromMillis, // تم التصحيح ليطابق الـ Entity الجديد
            toMillis = domain.period?.toMillis,     // تم التصحيح ليطابق الـ Entity الجديد
            filtersJson = domain.filters.toJsonString(),
            dataPointsJson = domain.dataPoints.toJsonString(),
            summary = domain.summary,
            isExported = domain.exported, // تم التصحيح ليطابق الـ Entity الجديد
            exportFormat = domain.exportFormat?.name,
            generatedByUserId = domain.generatedByUserId,
            metadataJson = domain.metadata.toJsonString(),
            syncState = "PENDING" // يفضل إضافتها لضمان حالة المزامنة عند الإنشاء
        )
    }

    fun toDomainList(entities: List<ReportEntity>): List<Report> {
        return entities.map { toDomain(it) }
    }

    fun toEntityList(domains: List<Report>): List<ReportEntity> {
        return domains.map { toEntity(it) }
    }

    private fun ReportEntity.toReportPeriod(): ReportPeriod? {
        // تم التصحيح لاستخدام الأسماء الجديدة fromMillis و toMillis
        if (fromMillis == null && toMillis == null) return null
        return ReportPeriod(
            fromMillis = fromMillis,
            toMillis = toMillis
        )
    }

    // ... (بقية الدوال المساعدة toStringMap و toJsonString تبقى كما هي فهي صحيحة تماماً) ...
    
    private fun String?.toReportType(): ReportType {
        return runCatching {
            ReportType.valueOf(this?.trim().orEmpty().uppercase())
        }.getOrDefault(ReportType.CUSTOM)
    }

    private fun String?.toReportExportFormat(): ReportExportFormat? {
        if (this.isNullOrBlank()) return null
        return runCatching {
            ReportExportFormat.valueOf(trim().uppercase())
        }.getOrNull()
    }

    private fun String.toStringMap(): Map<String, String> {
        if (isBlank() || this == "{}") return emptyMap()
        return runCatching {
            val json = JSONObject(this)
            val keys = json.keys()
            buildMap {
                while (keys.hasNext()) {
                    val key = keys.next()
                    put(key, json.optString(key))
                }
            }
        }.getOrDefault(emptyMap())
    }

    private fun Map<String, String>.toJsonString(): String {
        if (isEmpty()) return "{}"
        val json = JSONObject()
        for ((key, value) in this) {
            json.put(key, value)
        }
        return json.toString()
    }

    private fun List<ReportDataPoint>.toJsonString(): String {
        if (isEmpty()) return "[]"
        val array = JSONArray()
        for (point in this) {
            val item = JSONObject()
            when (point) {
                is ReportDataPoint.Count -> {
                    item.put("kind", "COUNT")
                    item.put("label", point.label)
                    item.put("value", point.value)
                }
                is ReportDataPoint.Money -> {
                    item.put("kind", "MONEY")
                    item.put("label", point.label)
                    item.put("value", point.value.toPlainString())
                    item.put("currencyCode", point.currencyCode)
                }
                is ReportDataPoint.Ratio -> {
                    item.put("kind", "RATIO")
                    item.put("label", point.label)
                    item.put("value", point.value)
                }
                is ReportDataPoint.Text -> {
                    item.put("kind", "TEXT")
                    item.put("label", point.label)
                    item.put("value", point.value)
                }
            }
            array.put(item)
        }
        return array.toString()
    }

    private fun String.toReportDataPoints(): List<ReportDataPoint> {
        if (isBlank() || this == "[]") return emptyList()
        return runCatching {
            val array = JSONArray(this)
            buildList {
                for (i in 0 until array.length()) {
                    val item = array.optJSONObject(i) ?: continue
                    val kind = item.optString("kind").uppercase()
                    val label = item.optString("label").trim()
                    when (kind) {
                        "COUNT" -> add(ReportDataPoint.Count(label, item.optLong("value")))
                        "MONEY" -> add(ReportDataPoint.Money(label, item.optString("value", "0").toBigDecimalOrZero(), item.optString("currencyCode")))
                        "RATIO" -> add(ReportDataPoint.Ratio(label, item.optDouble("value")))
                        "TEXT" -> add(ReportDataPoint.Text(label, item.optString("value")))
                    }
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun String.toBigDecimalOrZero(): BigDecimal {
        return runCatching { BigDecimal(trim()) }.getOrDefault(BigDecimal.ZERO)
    }
}
