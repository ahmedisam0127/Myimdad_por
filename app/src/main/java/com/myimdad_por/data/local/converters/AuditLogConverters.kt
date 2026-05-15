package com.myimdad_por.data.local.converters

import androidx.room.TypeConverter
import com.myimdad_por.domain.model.AuditAction
import com.myimdad_por.domain.model.AuditActor
import com.myimdad_por.domain.model.AuditContext
import com.myimdad_por.domain.model.AuditSeverity
import com.myimdad_por.domain.model.AuditTarget
import org.json.JSONArray
import org.json.JSONObject

class AuditLogConverters {

    @TypeConverter
    fun fromAuditAction(value: AuditAction?): String? = value?.name

    @TypeConverter
    fun toAuditAction(value: String?): AuditAction? {
        if (value.isNullOrBlank()) return null
        return runCatching { enumValueOf<AuditAction>(value) }.getOrNull()
    }

    @TypeConverter
    fun fromAuditSeverity(value: AuditSeverity?): String? = value?.name

    @TypeConverter
    fun toAuditSeverity(value: String?): AuditSeverity? {
        if (value.isNullOrBlank()) return null
        return runCatching { enumValueOf<AuditSeverity>(value) }.getOrNull()
    }

    @TypeConverter
    fun fromAuditActor(value: AuditActor?): String? = value?.toJsonString()

    @TypeConverter
    fun toAuditActor(value: String?): AuditActor? {
        if (value.isNullOrBlank()) return null
        return runCatching { value.toAuditActor() }.getOrNull()
    }

    @TypeConverter
    fun fromAuditTarget(value: AuditTarget?): String? = value?.toJsonString()

    @TypeConverter
    fun toAuditTarget(value: String?): AuditTarget? {
        if (value.isNullOrBlank()) return null
        return runCatching { value.toAuditTarget() }.getOrNull()
    }

    @TypeConverter
    fun fromAuditContext(value: AuditContext?): String? = value?.toJsonString()

    @TypeConverter
    fun toAuditContext(value: String?): AuditContext? {
        if (value.isNullOrBlank()) return null
        return runCatching { value.toAuditContext() }.getOrNull()
    }

    @TypeConverter
    fun fromAuditChanges(value: Map<String, Pair<String, String>>?): String? = value?.toJsonString()

    @TypeConverter
    fun toAuditChanges(value: String?): Map<String, Pair<String, String>> {
        if (value.isNullOrBlank()) return emptyMap()
        return runCatching { value.toAuditChangesMap() }.getOrDefault(emptyMap())
    }
}

private fun AuditActor.toJsonString(): String {
    return JSONObject().apply {
        put("actorId", actorId)
        put("actorName", actorName)
        put("role", role)
        put("deviceId", deviceId)
        put("email", email)
        put("metadata", JSONObject(metadata))
    }.toString()
}

private fun String.toAuditActor(): AuditActor {
    val json = JSONObject(this)
    val metadata = json.optJSONObject("metadata")?.toStringMap().orEmpty()
    return AuditActor(
        actorId = json.optString("actorId"),
        actorName = json.optString("actorName"),
        role = json.optString("role"),
        deviceId = json.optString("deviceId").takeUnless { it.isBlank() },
        email = json.optString("email").takeUnless { it.isBlank() },
        metadata = metadata
    )
}

private fun AuditTarget.toJsonString(): String {
    return JSONObject().apply {
        put("targetType", targetType)
        put("targetId", targetId)
        put("targetName", targetName)
        put("module", module)
        put("metadata", JSONObject(metadata))
    }.toString()
}

private fun String.toAuditTarget(): AuditTarget {
    val json = JSONObject(this)
    val metadata = json.optJSONObject("metadata")?.toStringMap().orEmpty()
    return AuditTarget(
        targetType = json.optString("targetType"),
        targetId = json.optString("targetId"),
        targetName = json.optString("targetName").takeUnless { it.isBlank() },
        module = json.optString("module").takeUnless { it.isBlank() },
        metadata = metadata
    )
}

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

private fun String.toAuditContext(): AuditContext {
    val json = JSONObject(this)
    val metadata = json.optJSONObject("metadata")?.toStringMap().orEmpty()
    return AuditContext(
        ipAddress = json.optString("ipAddress").takeUnless { it.isBlank() },
        userAgent = json.optString("userAgent").takeUnless { it.isBlank() },
        location = json.optString("location").takeUnless { it.isBlank() },
        sessionId = json.optString("sessionId").takeUnless { it.isBlank() },
        requestId = json.optString("requestId").takeUnless { it.isBlank() },
        deviceModel = json.optString("deviceModel").takeUnless { it.isBlank() },
        appVersion = json.optString("appVersion").takeUnless { it.isBlank() },
        isOffline = json.optBoolean("isOffline", false),
        metadata = metadata
    )
}

private fun Map<String, Pair<String, String>>.toJsonString(): String {
    return JSONObject().apply {
        forEach { (field, change) ->
            put(
                field,
                JSONObject().apply {
                    put("old", change.first)
                    put("new", change.second)
                }
            )
        }
    }.toString()
}

private fun String.toAuditChangesMap(): Map<String, Pair<String, String>> {
    val json = JSONObject(this)
    return buildMap {
        json.keys().forEach { key ->
            val changeObject = json.optJSONObject(key)
            if (changeObject != null) {
                put(
                    key,
                    changeObject.optString("old") to changeObject.optString("new")
                )
            }
        }
    }
}

private fun JSONObject.toStringMap(): Map<String, String> {
    return buildMap {
        keys().forEach { key ->
            put(key, optString(key))
        }
    }
}