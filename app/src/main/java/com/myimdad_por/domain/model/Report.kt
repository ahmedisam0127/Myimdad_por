package com.myimdad_por.domain.model

import java.math.BigDecimal

/**
 * Represents an aggregated business report.
 *
 * The model is flexible enough to support:
 * - sales reports
 * - inventory reports
 * - financial summaries
 * - tax and compliance snapshots
 */
data class Report(
    val reportId: String,
    val title: String,
    val type: ReportType,
    val generatedAtMillis: Long = System.currentTimeMillis(),
    val period: ReportPeriod? = null,
    val filters: Map<String, String> = emptyMap(),
    val dataPoints: List<ReportDataPoint> = emptyList(),
    val summary: String? = null,
    val exported: Boolean = false,
    val exportFormat: ReportExportFormat? = null,
    val generatedByUserId: String? = null,
    val metadata: Map<String, String> = emptyMap()
) {
    init {
        require(reportId.isNotBlank()) { "reportId cannot be blank." }
        require(title.isNotBlank()) { "title cannot be blank." }
        require(generatedAtMillis > 0L) { "generatedAtMillis must be greater than zero." }

        generatedByUserId?.let {
            require(it.isNotBlank()) { "generatedByUserId cannot be blank when provided." }
        }
    }

    val totalNumericValue: BigDecimal
        get() = dataPoints.fold(BigDecimal.ZERO) { acc, point ->
            acc.add(point.numericValue)
        }

    val hasData: Boolean
        get() = dataPoints.isNotEmpty()
}

enum class ReportType {
    SALES,
    INVENTORY,
    FINANCIAL,
    TAX,
    OPERATIONAL,
    PERFORMANCE,
    CUSTOM
}

enum class ReportExportFormat {
    PDF,
    EXCEL,
    CSV,
    JSON
}

data class ReportPeriod(
    val fromMillis: Long? = null,
    val toMillis: Long? = null
) {
    init {
        fromMillis?.let {
            require(it > 0L) { "fromMillis must be greater than zero when provided." }
        }
        toMillis?.let {
            require(it > 0L) { "toMillis must be greater than zero when provided." }
        }
        if (fromMillis != null && toMillis != null) {
            require(fromMillis <= toMillis) { "fromMillis must be less than or equal to toMillis." }
        }
    }
}

sealed class ReportDataPoint {

    abstract val label: String
    abstract val numericValue: BigDecimal

    data class Count(
        override val label: String,
        val value: Long
    ) : ReportDataPoint() {
        init {
            require(label.isNotBlank()) { "label cannot be blank." }
            require(value >= 0L) { "value cannot be negative." }
        }

        override val numericValue: BigDecimal
            get() = BigDecimal.valueOf(value)
    }

    data class Money(
        override val label: String,
        val value: BigDecimal,
        val currencyCode: String
    ) : ReportDataPoint() {
        init {
            require(label.isNotBlank()) { "label cannot be blank." }
            require(value >= BigDecimal.ZERO) { "value cannot be negative." }
            require(currencyCode.isNotBlank()) { "currencyCode cannot be blank." }
        }

        override val numericValue: BigDecimal
            get() = value
    }

    data class Ratio(
        override val label: String,
        val value: Double
    ) : ReportDataPoint() {
        init {
            require(label.isNotBlank()) { "label cannot be blank." }
            require(value.isFinite()) { "value must be finite." }
        }

        override val numericValue: BigDecimal
            get() = BigDecimal.valueOf(value)
    }

    data class Text(
        override val label: String,
        val value: String
    ) : ReportDataPoint() {
        init {
            require(label.isNotBlank()) { "label cannot be blank." }
            require(value.isNotBlank()) { "value cannot be blank." }
        }

        override val numericValue: BigDecimal
            get() = BigDecimal.ZERO
    }
}