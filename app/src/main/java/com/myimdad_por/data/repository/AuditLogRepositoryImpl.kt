package com.myimdad_por.data.repository

import com.myimdad_por.core.dispatchers.AppDispatchers
import com.myimdad_por.core.dispatchers.DefaultAppDispatchers
import com.myimdad_por.core.network.NetworkResult
import com.myimdad_por.data.local.dao.AuditLogDao
import com.myimdad_por.data.local.entity.AuditLogEntity
import com.myimdad_por.data.remote.datasource.AuditLogRemoteDataSource
import com.myimdad_por.data.remote.dto.AuditLogDto
import com.myimdad_por.domain.model.AuditAction
import com.myimdad_por.domain.model.AuditActor
import com.myimdad_por.domain.model.AuditContext
import com.myimdad_por.domain.model.AuditLog
import com.myimdad_por.domain.model.AuditSeverity
import com.myimdad_por.domain.model.AuditTarget
import com.myimdad_por.domain.repository.AuditLogRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.Locale
import java.util.UUID

class AuditLogRepositoryImpl(
    private val auditLogDao: AuditLogDao,
    private val remoteDataSource: AuditLogRemoteDataSource? = null,
    private val dispatchers: AppDispatchers = DefaultAppDispatchers
) : AuditLogRepository {

    override fun observeAllLogs(): Flow<List<AuditLog>> {
        return auditLogDao.observeAll()
            .map { entities -> entities.map { it.toDomain() } }
            .flowOn(dispatchers.io)
    }

    override fun observeLogsByTarget(
        targetType: String,
        targetId: String?
    ): Flow<List<AuditLog>> {
        val normalizedTargetType = targetType.trim()

        return auditLogDao.observeByTargetType(normalizedTargetType)
            .map { entities ->
                entities
                    .map { it.toDomain() }
                    .filter { log ->
                        targetId.isNullOrBlank() || log.matchesTargetId(targetId)
                    }
            }
            .flowOn(dispatchers.io)
    }

    override fun observeLogsByActor(actorId: String): Flow<List<AuditLog>> {
        val normalizedActorId = actorId.trim()

        return auditLogDao.observeAll()
            .map { entities ->
                entities
                    .map { it.toDomain() }
                    .filter { log -> log.matchesActorId(normalizedActorId) }
            }
            .flowOn(dispatchers.io)
    }

    override fun observeCriticalLogs(): Flow<List<AuditLog>> {
        return auditLogDao.observeAll()
            .map { entities ->
                entities
                    .map { it.toDomain() }
                    .filter { it.severity.isCriticalSeverity() }
            }
            .flowOn(dispatchers.io)
    }

    override suspend fun getLogById(logId: String): AuditLog? = withContext(dispatchers.io) {
        val normalizedId = logId.trim()
        if (normalizedId.isBlank()) return@withContext null

        auditLogDao.getById(normalizedId)?.toDomain()
            ?: fetchRemoteById(normalizedId)
    }

    override suspend fun getLogs(
        fromMillis: Long?,
        toMillis: Long?,
        action: AuditAction?,
        severity: AuditSeverity?,
        actorId: String?,
        targetType: String?,
        targetId: String?,
        correlationId: String?
    ): List<AuditLog> = withContext(dispatchers.io) {
        val localLogs = loadAllLocalLogs().filterByCriteria(
            fromMillis = fromMillis,
            toMillis = toMillis,
            action = action,
            severity = severity,
            actorId = actorId,
            targetType = targetType,
            targetId = targetId,
            correlationId = correlationId
        )

        if (localLogs.isNotEmpty() || remoteDataSource == null) {
            return@withContext localLogs
        }

        val remoteLogs = fetchRemoteLogs(
            action = action,
            severity = severity,
            actorId = actorId,
            targetType = targetType,
            targetId = targetId
        )

        if (remoteLogs.isNotEmpty()) {
            auditLogDao.insertAll(remoteLogs.map { it.toEntity() })
        }

        remoteLogs.filterByCriteria(
            fromMillis = fromMillis,
            toMillis = toMillis,
            action = action,
            severity = severity,
            actorId = actorId,
            targetType = targetType,
            targetId = targetId,
            correlationId = correlationId
        )
    }

    override suspend fun recordLog(log: AuditLog): Result<AuditLog> = withContext(dispatchers.io) {
        runCatching {
            auditLogDao.insert(log.toEntity())
            log
        }
    }

    override suspend fun recordLogs(logs: List<AuditLog>): Result<List<AuditLog>> = withContext(dispatchers.io) {
        runCatching {
            if (logs.isNotEmpty()) {
                auditLogDao.insertAll(logs.map { it.toEntity() })
            }
            logs
        }
    }

    override suspend fun logEvent(
        action: AuditAction,
        severity: AuditSeverity,
        actor: AuditActor,
        target: AuditTarget,
        context: AuditContext?,
        changes: Map<String, Pair<String, String>>,
        note: String?,
        correlationId: String?,
        metadata: Map<String, String>
    ): Result<AuditLog> = withContext(dispatchers.io) {
        runCatching {
            val log = AuditLog(
                logId = UUID.randomUUID().toString(),
                timestampMillis = System.currentTimeMillis(),
                action = action,
                severity = severity,
                actor = actor,
                target = target,
                context = context,
                changes = changes,
                note = note.trimToNull(),
                correlationId = correlationId.trimToNull(),
                metadata = metadata
            )

            auditLogDao.insert(log.toEntity())
            log
        }
    }

    override suspend fun deleteLog(logId: String): Result<Unit> = withContext(dispatchers.io) {
        runCatching {
            val normalizedId = logId.trim()
            require(normalizedId.isNotBlank()) { "logId cannot be blank." }

            val local = auditLogDao.getById(normalizedId)
            auditLogDao.deleteById(normalizedId)

            val remoteId = local?.serverId?.takeIf { it.isNotBlank() } ?: normalizedId
            remoteDataSource?.deleteLog(remoteId)

            Unit
        }
    }

    override suspend fun deleteLogsOlderThan(timestampMillis: Long): Result<Int> = withContext(dispatchers.io) {
        runCatching {
            auditLogDao.deleteOlderThan(timestampMillis)
        }
    }

    override suspend fun clearAll(): Result<Unit> = withContext(dispatchers.io) {
        runCatching {
            auditLogDao.deleteOlderThan(Long.MAX_VALUE)
            Unit
        }
    }

    override suspend fun countLogs(): Long = withContext(dispatchers.io) {
        auditLogDao.countAll().toLong()
    }

    override suspend fun countLogs(
        fromMillis: Long?,
        toMillis: Long?,
        action: AuditAction?,
        severity: AuditSeverity?
    ): Long = withContext(dispatchers.io) {
        loadAllLocalLogs()
            .filterByCriteria(
                fromMillis = fromMillis,
                toMillis = toMillis,
                action = action,
                severity = severity,
                actorId = null,
                targetType = null,
                targetId = null,
                correlationId = null
            )
            .size
            .toLong()
    }

    private suspend fun loadAllLocalLogs(): List<AuditLog> {
        return auditLogDao.observeAll().first().map { it.toDomain() }
    }

    private fun List<AuditLog>.filterByCriteria(
        fromMillis: Long?,
        toMillis: Long?,
        action: AuditAction?,
        severity: AuditSeverity?,
        actorId: String?,
        targetType: String?,
        targetId: String?,
        correlationId: String?
    ): List<AuditLog> {
        val normalizedActorId = actorId.trimToNull()
        val normalizedTargetType = targetType.trimToNull()
        val normalizedTargetId = targetId.trimToNull()
        val normalizedCorrelationId = correlationId.trimToNull()

        return asSequence()
            .filter { fromMillis == null || it.timestampMillis >= fromMillis }
            .filter { toMillis == null || it.timestampMillis <= toMillis }
            .filter { action == null || it.action.name.equals(action.name, ignoreCase = true) }
            .filter { severity == null || it.severity.name.equals(severity.name, ignoreCase = true) }
            .filter { normalizedActorId == null || it.matchesActorId(normalizedActorId) }
            .filter { normalizedTargetType == null || it.target.targetType.equals(normalizedTargetType, ignoreCase = true) }
            .filter { normalizedTargetId == null || it.matchesTargetId(normalizedTargetId) }
            .filter { normalizedCorrelationId == null || it.correlationId.equals(normalizedCorrelationId, ignoreCase = true) }
            .toList()
    }

    private suspend fun fetchRemoteById(logId: String): AuditLog? {
        val remote = remoteDataSource ?: return null

        return when (val result = remote.getLogById(logId)) {
            is NetworkResult.Success -> {
                val domain = result.data.toDomain()
                auditLogDao.insert(domain.toEntity())
                domain
            }

            is NetworkResult.Error,
            NetworkResult.Loading -> null
        }
    }

    private suspend fun fetchRemoteLogs(
        action: AuditAction?,
        severity: AuditSeverity?,
        actorId: String?,
        targetType: String?,
        targetId: String?
    ): List<AuditLog> {
        val remote = remoteDataSource ?: return emptyList()

        return when (
            val result = remote.listLogs(
                page = null,
                limit = null,
                action = action?.name,
                severity = severity?.name,
                actorId = actorId.trimToNull(),
                targetType = targetType.trimToNull(),
                targetId = targetId.trimToNull()
            )
        ) {
            is NetworkResult.Success -> result.data.map { it.toDomain() }
            is NetworkResult.Error,
            NetworkResult.Loading -> emptyList()
        }
    }

    private fun AuditLogEntity.toDomain(): AuditLog {
        val metadata = metadataJson.toStringMap()

        val resolvedActorId = metadata["actorId"]
            .trimToNull()
            ?: serverId.trimToNull()
            ?: logId

        val resolvedTargetId = metadata["targetId"]
            .trimToNull()
            ?: targetLabel.trimToNull()
            ?: logId

        return AuditLog(
            logId = logId,
            timestampMillis = timestampMillis,
            action = action.toAuditActionOrDefault(),
            severity = severity.toAuditSeverityOrDefault(),
            actor = AuditActor(
                actorId = resolvedActorId,
                actorName = actorLabel.safeValue("Unknown Actor"),
                role = actorType.safeValue("UNKNOWN"),
                deviceId = null,
                email = null,
                metadata = metadata
            ),
            target = AuditTarget(
                targetType = targetType.safeValue("UNKNOWN"),
                targetId = resolvedTargetId,
                targetName = targetLabel.trimToNull(),
                module = null,
                metadata = metadata
            ),
            context = contextJson.trimToNull()?.toAuditContextOrNull(),
            changes = changesJson.toChangesMap(),
            note = note.trimToNull(),
            correlationId = correlationId.trimToNull(),
            metadata = metadata
        )
    }

    private fun AuditLog.toEntity(): AuditLogEntity {
        return AuditLogEntity(
            logId = logId,
            serverId = null,
            timestampMillis = timestampMillis,
            action = action.name,
            severity = severity.name,
            actorType = actor.role,
            actorLabel = actor.actorName,
            targetType = target.targetType,
            targetLabel = target.targetName ?: target.targetId,
            contextJson = context?.toJsonString(),
            changesJson = changes.toChangesJsonString(),
            note = note.trimToNull(),
            correlationId = correlationId.trimToNull(),
            metadataJson = metadata.toJsonString(),
            syncState = "PENDING",
            createdAtMillis = timestampMillis,
            syncedAtMillis = null
        )
    }

    private fun AuditLogDto.toDomain(): AuditLog {
        val metadata = buildMap<String, String> {
            putAll(metadataJson.toStringMap())
            putAll(actorMetadataJson.toStringMap().mapKeys { "actor.${it.key}" })
            putAll(targetMetadataJson.toStringMap().mapKeys { "target.${it.key}" })
        }

        return AuditLog(
            logId = logId.trimToNull() ?: UUID.randomUUID().toString(),
            timestampMillis = timestampMillis,
            action = action.toAuditActionOrDefault(),
            severity = severity.toAuditSeverityOrDefault(),
            actor = AuditActor(
                actorId = actorId.safeValue("unknown-actor"),
                actorName = actorName.safeValue("Unknown Actor"),
                role = actorRole.safeValue("UNKNOWN"),
                deviceId = actorDeviceId.trimToNull(),
                email = actorEmail.trimToNull(),
                metadata = actorMetadataJson.toStringMap()
            ),
            target = AuditTarget(
                targetType = targetType.safeValue("UNKNOWN"),
                targetId = targetId.safeValue("unknown-target"),
                targetName = targetName.trimToNull(),
                module = targetModule.trimToNull(),
                metadata = targetMetadataJson.toStringMap()
            ),
            context = contextJson.trimToNull()?.toAuditContextOrNull(),
            changes = changesJson.toChangesMap(),
            note = note.trimToNull(),
            correlationId = correlationId.trimToNull(),
            metadata = metadata
        )
    }

    private fun Map<String, String>.toJsonString(): String {
        return runCatching {
            JSONObject().apply {
                forEach { (key, value) -> put(key, value) }
            }.toString()
        }.getOrDefault("{}")
    }

    private fun Map<String, Pair<String, String>>.toChangesJsonString(): String {
        return runCatching {
            JSONObject().apply {
                forEach { (field, change) ->
                    put(
                        field,
                        JSONObject().apply {
                            put("before", change.first)
                            put("after", change.second)
                        }
                    )
                }
            }.toString()
        }.getOrDefault("{}")
    }

    private fun String?.toStringMap(): Map<String, String> {
        if (this.isNullOrBlank()) return emptyMap()

        return runCatching {
            val json = JSONObject(this)
            buildMap {
                json.keys().forEach { key ->
                    put(key, json.optString(key))
                }
            }
        }.getOrDefault(emptyMap())
    }

    private fun String?.toChangesMap(): Map<String, Pair<String, String>> {
        if (this.isNullOrBlank()) return emptyMap()

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

    private fun AuditContext.toJsonString(): String {
        return runCatching {
            JSONObject().apply {
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
        }.getOrDefault("{}")
    }

    private fun String?.toAuditContextOrNull(): AuditContext? {
        if (this.isNullOrBlank()) return null

        return runCatching {
            val json = JSONObject(this)
            AuditContext(
                ipAddress = json.optString("ipAddress").trimToNull(),
                userAgent = json.optString("userAgent").trimToNull(),
                location = json.optString("location").trimToNull(),
                sessionId = json.optString("sessionId").trimToNull(),
                requestId = json.optString("requestId").trimToNull(),
                deviceModel = json.optString("deviceModel").trimToNull(),
                appVersion = json.optString("appVersion").trimToNull(),
                isOffline = json.optBoolean("isOffline", false),
                metadata = json.optJSONObject("metadata")?.toMap() ?: emptyMap()
            )
        }.getOrNull()
    }

    private fun JSONObject.toMap(): Map<String, String> {
        return buildMap {
            keys().forEach { key ->
                put(key, optString(key))
            }
        }
    }

    private fun AuditLog.matchesActorId(actorId: String): Boolean {
        return actor.actorId.equals(actorId, ignoreCase = true) ||
            actor.actorName.equals(actorId, ignoreCase = true) ||
            metadata["actorId"].equals(actorId, ignoreCase = true)
    }

    private fun AuditLog.matchesTargetId(targetId: String): Boolean {
        return target.targetId.equals(targetId, ignoreCase = true) ||
            target.targetName.orEmpty().equals(targetId, ignoreCase = true) ||
            metadata["targetId"].equals(targetId, ignoreCase = true)
    }

    private fun AuditSeverity.isCriticalSeverity(): Boolean {
        return name.uppercase(Locale.ROOT) in setOf("CRITICAL", "HIGH", "SEVERE", "ALERT", "ERROR")
    }

    private fun String?.toAuditActionOrDefault(): AuditAction {
        val normalized = this?.trim()?.uppercase(Locale.ROOT).orEmpty()
        return runCatching { AuditAction.valueOf(normalized) }.getOrDefault(AuditAction.CUSTOM)
    }

    private fun String?.toAuditSeverityOrDefault(): AuditSeverity {
        val normalized = this?.trim()?.uppercase(Locale.ROOT).orEmpty()
        return runCatching { AuditSeverity.valueOf(normalized) }.getOrDefault(AuditSeverity.LOW)
    }

    private fun String?.safeValue(fallback: String): String {
        return this?.trim()?.takeIf { it.isNotBlank() } ?: fallback
    }

    private fun String?.trimToNull(): String? {
        return this?.trim()?.takeIf { it.isNotBlank() }
    }
}