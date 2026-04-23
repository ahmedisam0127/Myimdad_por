package com.myimdad_por.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * الكائن الأساسي لتمثيل التقرير من واجهة البرمجيات (API).
 * تم فصل منطق التحويل (Mapping) إلى ملف مستقل كما طلبت.
 */
data class ReportDto(
    @SerializedName("report_id")
    val reportId: String,
    
    @SerializedName("title")
    val title: String,
    
    @SerializedName("type")
    val type: String,
    
    @SerializedName("generated_at_millis")
    val generatedAtMillis: Long,
    
    @SerializedName("period")
    val period: ReportPeriodDto?,
    
    @SerializedName("filters")
    val filters: Map<String, String>?,
    
    @SerializedName("data_points")
    val dataPoints: List<ReportDataPointDto>?,
    
    @SerializedName("summary")
    val summary: String,
    
    @SerializedName("exported")
    val exported: Boolean,
    
    @SerializedName("export_format")
    val exportFormat: String?,
    
    @SerializedName("generated_by_user_id")
    val generatedByUserId: String?,
    
    @SerializedName("metadata")
    val metadata: Map<String, String>?
)

/**
 * تمثيل النقاط البيانية داخل التقرير.
 */
data class ReportDataPointDto(
    @SerializedName("kind")
    val kind: String, // COUNT, MONEY, RATIO, TEXT
    
    @SerializedName("label")
    val label: String,
    
    @SerializedName("value")
    val value: String, // يتم استقبالها كـ String لضمان دقة الأرقام الكبيرة
    
    @SerializedName("currency_code")
    val currencyCode: String?
)

/**
 * تمثيل الفترة الزمنية للتقرير.
 */
data class ReportPeriodDto(
    @SerializedName("from_millis")
    val fromMillis: Long?,
    
    @SerializedName("to_millis")
    val toMillis: Long?
)

/**
 * استجابة القائمة (Pagination Wrapper) المطلوبة في ReportApiService.
 */
data class ReportListResponseDto(
    @SerializedName("reports")
    val reports: List<ReportDto>,
    
    @SerializedName("total_count")
    val totalCount: Int,
    
    @SerializedName("current_page")
    val currentPage: Int,
    
    @SerializedName("has_more")
    val hasMore: Boolean
)

/**
 * طلب إنشاء تقرير جديد.
 */
data class ReportGenerateRequestDto(
    @SerializedName("title")
    val title: String,
    
    @SerializedName("type")
    val type: String,
    
    @SerializedName("from_millis")
    val fromMillis: Long?,
    
    @SerializedName("to_millis")
    val toMillis: Long?,
    
    @SerializedName("filters")
    val filters: Map<String, String>,
    
    @SerializedName("export_format")
    val exportFormat: String? = null,
    
    @SerializedName("metadata")
    val metadata: Map<String, String> = emptyMap()
)

/**
 * طلب تصدير تقرير بصيغة معينة.
 */
data class ReportExportRequestDto(
    @SerializedName("format")
    val format: String // PDF, EXCEL, CSV
)

/**
 * طلب مزامنة التقارير.
 */
data class ReportSyncRequestDto(
    @SerializedName("last_sync_timestamp")
    val lastSyncTimestamp: Long,
    
    @SerializedName("local_report_ids")
    val localReportIds: List<String>
)
