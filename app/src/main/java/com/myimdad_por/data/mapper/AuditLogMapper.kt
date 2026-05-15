package com.myimdad_por.data.mapper

import com.myimdad_por.data.remote.dto.AuditLogDto
import com.myimdad_por.domain.model.AuditAction
import com.myimdad_por.domain.model.AuditActor
import com.myimdad_por.domain.model.AuditContext
import com.myimdad_por.domain.model.AuditLog
import com.myimdad_por.domain.model.AuditSeverity
import com.myimdad_por.domain.model.AuditTarget
import org.json.JSONObject
import java.util.Locale

fun AuditLog.toDto(
    serverId: String? = null,
    syncState: String = "PENDING",
    isDeleted: Boolean = false,
    syncedAtMillis: Long? = null,
    createdAtMillis: Long = timestampMillis,
    updatedAtMillis: Long = System.currentTimeMillis()
): AuditLogDto {
    return AuditLogDto(
        logId = logId,
        serverId = serverId,
        timestampMillis = timestampMillis,
        action = action.name,
        severity = severity.name,
        actorId = actor.actorId,
        actorName = actor.actorName,
        actorRole = actor.role,
        actorDeviceId = actor.deviceId,
        actorEmail = actor.email,
        actorMetadataJson = actor.metadata.toJsonString(),
        targetType = target.targetType,
        targetId = target.targetId,
        targetName = target.targetName,
        targetModule = target.module,
        targetMetadataJson = target.metadata.toJsonString(),
        contextJson = context?.toJsonString(),
        changesJson = changes.toChangesJsonString(),
        note = note,
        correlationId = correlationId,
        metadataJson = metadata.toJsonString(),
        syncState = syncState,
        isDeleted = isDeleted,
        syncedAtMillis = syncedAtMillis,
        createdAtMillis = createdAtMillis,
        updatedAtMillis = updatedAtMillis
    )
}

fun AuditLogDto.toDomain(): AuditLog {
    return AuditLog(
        logId = logId,
        timestampMillis = timestampMillis,
        action = action.toAuditActionOrDefault(),
        severity = severity.toAuditSeverityOrDefault(),
        actor = AuditActor(
            actorId = actorId.safeValue("unknown-actor"),
            actorName = actorName.safeValue("Unknown Actor"),
            role = actorRole.safeValue("UNKNOWN"),
            deviceId = actorDeviceId?.takeIf { it.isNotBlank() },
            email = actorEmail?.takeIf { it.isNotBlank() },
            metadata = actorMetadataJson.toStringMap()
        ),
        target = AuditTarget(
            targetType = targetType.safeValue("UNKNOWN"),
            targetId = targetId.safeValue("unknown-target"),
            targetName = targetName?.takeIf { it.isNotBlank() },
            module = targetModule?.takeIf { it.isNotBlank() },
            metadata = targetMetadataJson.toStringMap()
        ),
        context = contextJson?.takeIf { it.isNotBlank() }?.toAuditContextOrNull(),
        changes = changesJson.toChangesMap(),
        note = note?.takeIf { it.isNotBlank() },
        correlationId = correlationId?.takeIf { it.isNotBlank() },
        metadata = metadataJson.toStringMap()
    )
}

fun List<AuditLog>.toDtoList(): List<AuditLogDto> = map { it.toDto() }

fun List<AuditLogDto>.toDomainList(): List<AuditLog> = map { it.toDomain() }

private fun String.toAuditActionOrDefault(): AuditAction {
    val normalized = trim().uppercase(Locale.ROOT)
    return runCatching { AuditAction.valueOf(normalized) }.getOrDefault(AuditAction.CUSTOM)
}

private fun String.toAuditSeverityOrDefault(): AuditSeverity {
    val normalized = trim().uppercase(Locale.ROOT)
    return runCatching { AuditSeverity.valueOf(normalized) }.getOrDefault(AuditSeverity.LOW)
}

// تعديل: جعلها تقبل Map بخصائص عامة لتجنب تعارض الأنواع
private fun Map<String, *>.toJsonString(): String = JSONObject(this).toString()

// إضافة دالة خاصة بـ AuditContext لأن نوعه كلاس وليس Map
private fun AuditContext.toJsonString(): String {
    return JSONObject().apply {
        put("ipAddress", ipAddress)
        put("userAgent", userAgent)
        put("location", location)
        put("sessionId", sessionId)
        put("requestId", requestId)
        put("deviceModel", deviceModel)
        put("appVersion", appVersion)
        put("isOffline", isOffline)
        put("metadata", JSONObject(metadata))
    }.toString()
}

private fun String.toStringMap(): Map<String, String> {
    if (isBlank()) return emptyMap()
    return runCatching {
        val json = JSONObject(this)
        val map = mutableMapOf<String, String>()
        json.keys().forEach { key ->
            map[key] = json.optString(key)
        }
        map
    }.getOrDefault(emptyMap())
}

// دالة مساعدة لتحويل JSONObject داخلي إلى Map مباشرة
private fun JSONObject.toStringMap(): Map<String, String> {
    val map = mutableMapOf<String, String>()
    this.keys().forEach { key ->
        map[key] = this.optString(key)
    }
    return map
}

private fun Map<String, Pair<String, String>>.toChangesJsonString(): String {
    return runCatching {
        val json = JSONObject()
        forEach { (field, change) ->
            json.put(
                field,
                JSONObject().apply {
                    put("before", change.first)
                    put("after", change.second)
                }
            )
        }
        json.toString()
    }.getOrDefault("{}")
}

private fun String.toChangesMap(): Map<String, Pair<String, String>> {
    if (isBlank()) return emptyMap()
    return runCatching {
        val json = JSONObject(this)
        buildMap {
            json.keys().forEach { key ->
                val item = json.optJSONObject(key)
                val before = item?.optString("before").orEmpty()
                val after = item?.optString("after").orEmpty()
                put(key, before to after)
            }
        }
    }.getOrDefault(emptyMap())
}

private fun String.toAuditContextOrNull(): AuditContext? {
    return runCatching {
        val json = JSONObject(this)
        AuditContext(
            ipAddress = json.optString("ipAddress").takeIf { it.isNotBlank() },
            userAgent = json.optString("userAgent").takeIf { it.isNotBlank() },
            location = json.optString("location").takeIf { it.isNotBlank() },
            sessionId = json.optString("sessionId").takeIf { it.isNotBlank() },
            requestId = json.optString("requestId").takeIf { it.isNotBlank() },
            deviceModel = json.optString("deviceModel").takeIf { it.isNotBlank() },
            appVersion = json.optString("appVersion").takeIf { it.isNotBlank() },
            isOffline = json.optBoolean("isOffline", false),
            // تعديل هنا: استخدام الدالة الجديدة التي تقبل JSONObject مباشرة
            metadata = json.optJSONObject("metadata")?.toStringMap() ?: emptyMap()
        )
    }.getOrNull()
}

private fun String.safeValue(fallback: String): String {
    return trim().takeIf { it.isNotBlank() } ?: fallback
}
