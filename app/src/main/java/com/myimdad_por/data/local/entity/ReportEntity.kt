package com.myimdad_por.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "reports",
    indices = [
        Index(value = ["type"]),
        Index(value = ["generated_at_millis"]),
        Index(value = ["generated_by_user_id"]),
        Index(value = ["sync_state"])
    ]
)
data class ReportEntity(
    @PrimaryKey
    @ColumnInfo(name = "report_id")
    val reportId: String = UUID.randomUUID().toString(),

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "type")
    val type: String,

    @ColumnInfo(name = "generated_at_millis")
    val generatedAtMillis: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "from_millis") // تم التعديل ليطابق الميجريشن
    val fromMillis: Long? = null,

    @ColumnInfo(name = "to_millis") // تم التعديل ليطابق الميجريشن
    val toMillis: Long? = null,

    @ColumnInfo(name = "filters_json")
    val filtersJson: String = "{}",

    @ColumnInfo(name = "data_points_json")
    val dataPointsJson: String = "[]",

    @ColumnInfo(name = "summary")
    val summary: String? = null,

    @ColumnInfo(name = "is_exported") // تم التعديل ليطابق الميجريشن
    val isExported: Boolean = false,

    @ColumnInfo(name = "export_format")
    val exportFormat: String? = null,

    @ColumnInfo(name = "generated_by_user_id")
    val generatedByUserId: String? = null,

    @ColumnInfo(name = "metadata_json")
    val metadataJson: String = "{}",

    @ColumnInfo(name = "sync_state") // حقل إضافي موجود في الميجريشن
    val syncState: String = "PENDING",

    @ColumnInfo(name = "synced_at_millis") // حقل إضافي موجود في الميجريشن
    val syncedAtMillis: Long? = null
)
