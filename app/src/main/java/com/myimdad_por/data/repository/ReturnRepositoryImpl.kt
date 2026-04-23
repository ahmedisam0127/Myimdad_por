package com.myimdad_por.data.repository
import javax.inject.Inject
import com.myimdad_por.core.dispatchers.AppDispatchers
import com.myimdad_por.core.dispatchers.DefaultAppDispatchers
import com.myimdad_por.core.network.NetworkResult
import com.myimdad_por.data.local.dao.ReturnDao
import com.myimdad_por.data.local.entity.ReturnEntity
import com.myimdad_por.data.local.entity.toDomain as entityToDomain
import com.myimdad_por.data.mapper.toDto
import com.myimdad_por.data.mapper.toDomain as dtoToDomain
import com.myimdad_por.data.remote.datasource.ReturnRemoteDataSource
import com.myimdad_por.domain.model.RefundStatus
import com.myimdad_por.domain.model.Return
import com.myimdad_por.domain.model.ReturnStatus
import com.myimdad_por.domain.model.ReturnType
import com.myimdad_por.domain.repository.ReturnRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.time.LocalDateTime


class ReturnRepositoryImpl @Inject constructor(
    private val returnDao: ReturnDao,
    private val remoteDataSource: ReturnRemoteDataSource,
    private val appDispatchers: AppDispatchers = DefaultAppDispatchers
) : ReturnRepository {

    override fun observeAllReturns(): Flow<List<Return>> {
        return returnDao.observeAll().map { entities ->
            entities.toDomainListSafely()
        }
    }

    override fun observeReturnsByInvoice(documentId: String): Flow<List<Return>> {
        return returnDao.observeAll().map { entities ->
            entities.toDomainListSafely().filter { it.matchesInvoice(documentId) }
        }
    }

    override fun observeReturnsByStatus(status: ReturnStatus): Flow<List<Return>> {
        return returnDao.observeByStatus(status.name).map { entities ->
            entities.toDomainListSafely()
        }
    }

    override fun observePendingReturns(): Flow<List<Return>> {
        return returnDao.observeAll().map { entities ->
            entities.toDomainListSafely().filter { it.isPendingLike() }
        }
    }

    override suspend fun getReturnById(id: String): Return? = withContext(appDispatchers.io) {
        returnDao.getById(id)?.safeToDomain()
    }

    override suspend fun getReturnByNumber(returnNumber: String): Return? = withContext(appDispatchers.io) {
        returnDao.getByReturnNumber(returnNumber)?.safeToDomain()
    }

    override suspend fun getReturns(
        from: LocalDateTime?,
        to: LocalDateTime?,
        returnType: ReturnType?,
        status: ReturnStatus?,
        partyId: String?,
        processedByEmployeeId: String?
    ): List<Return> = withContext(appDispatchers.io) {
        returnDao.observeAll().first()
            .toDomainListSafely()
            .asSequence()
            .filter { from == null || !it.returnDate.isBefore(from) }
            .filter { to == null || !it.returnDate.isAfter(to) }
            .filter { returnType == null || it.returnType == returnType }
            .filter { status == null || it.status == status }
            .filter { partyId == null || it.partyId == partyId }
            .filter { processedByEmployeeId == null || it.processedByEmployeeId == processedByEmployeeId }
            .toList()
    }

    override suspend fun searchReturns(query: String): List<Return> = withContext(appDispatchers.io) {
        returnDao.search(query).first().toDomainListSafely()
    }

    override suspend fun saveReturn(returnDocument: Return): Result<Return> = withContext(appDispatchers.io) {
        runCatching {
            val now = System.currentTimeMillis()
            val existing = returnDao.getById(returnDocument.id)

            val localEntity = returnDocument.toEntity(
                serverId = existing?.serverId,
                syncState = "PENDING",
                isDeleted = existing?.isDeleted ?: false,
                syncedAtMillis = existing?.syncedAtMillis,
                updatedAtMillis = now
            )

            returnDao.insert(localEntity)

            val syncedEntity = syncToRemote(localEntity)
            syncedEntity.safeToDomain() ?: localEntity.safeToDomain() ?: returnDocument
        }
    }

    override suspend fun saveReturns(returns: List<Return>): Result<List<Return>> = withContext(appDispatchers.io) {
        runCatching {
            val now = System.currentTimeMillis()

            val entities = returns.map { returnDocument ->
                val existing = returnDao.getById(returnDocument.id)
                returnDocument.toEntity(
                    serverId = existing?.serverId,
                    syncState = "PENDING",
                    isDeleted = existing?.isDeleted ?: false,
                    syncedAtMillis = existing?.syncedAtMillis,
                    updatedAtMillis = now
                )
            }

            returnDao.insertAll(entities)
            entities.mapNotNull { it.safeToDomain() }
        }
    }

    override suspend fun updateReturn(returnDocument: Return): Result<Return> = withContext(appDispatchers.io) {
        runCatching {
            val now = System.currentTimeMillis()
            val existing = returnDao.getById(returnDocument.id)

            val localEntity = returnDocument.toEntity(
                serverId = existing?.serverId,
                syncState = "PENDING",
                isDeleted = existing?.isDeleted ?: false,
                syncedAtMillis = existing?.syncedAtMillis,
                updatedAtMillis = now
            )

            returnDao.update(localEntity)

            val syncedEntity = syncToRemote(localEntity)
            syncedEntity.safeToDomain() ?: localEntity.safeToDomain() ?: returnDocument
        }
    }

    override suspend fun processReturn(returnId: String): Result<Return> = withContext(appDispatchers.io) {
        runCatching {
            val now = System.currentTimeMillis()
            val entity = returnDao.getById(returnId)
                ?: throw NoSuchElementException("Return not found: $returnId")

            val updated = entity.copy(
                status = "PROCESSING",
                syncState = "PENDING",
                updatedAtMillis = now
            )

            returnDao.update(updated)
            updated.safeToDomain() ?: throw IllegalStateException("Failed to map return: $returnId")
        }
    }

    override suspend fun approveReturn(returnId: String): Result<Return> = withContext(appDispatchers.io) {
        runCatching {
            val now = System.currentTimeMillis()
            val entity = returnDao.getById(returnId)
                ?: throw NoSuchElementException("Return not found: $returnId")

            val approved = entity.copy(
                status = "APPROVED",
                syncState = "PENDING",
                updatedAtMillis = now
            )

            returnDao.update(approved)

            val serverId = approved.serverId
                ?: throw IllegalStateException("Cannot approve return without serverId: $returnId")

            when (val remoteResult = remoteDataSource.approveReturn(serverId)) {
                is NetworkResult.Success -> {
                    val remoteDto = remoteResult.data
                    val syncedEntity = ReturnEntity.fromDomain(
                        returnDoc = remoteDto.dtoToDomain(),
                        serverId = remoteDto.serverId ?: remoteDto.returnId,
                        syncState = remoteDto.syncState,
                        isDeleted = remoteDto.isDeleted,
                        syncedAtMillis = remoteDto.syncedAtMillis,
                        updatedAtMillis = remoteDto.updatedAtMillis
                    ).copy(
                        id = approved.id,
                        updatedAtMillis = now
                    )

                    returnDao.update(syncedEntity)
                    syncedEntity.safeToDomain() ?: throw IllegalStateException("Failed to map approved return: $returnId")
                }

                is NetworkResult.Error -> throw remoteResult.exception
                NetworkResult.Loading -> approved.safeToDomain()
                    ?: throw IllegalStateException("Failed to map approved return: $returnId")
            }
        }
    }

    override suspend fun completeReturn(returnId: String): Result<Return> = withContext(appDispatchers.io) {
        runCatching {
            val now = System.currentTimeMillis()
            val entity = returnDao.getById(returnId)
                ?: throw NoSuchElementException("Return not found: $returnId")

            val updated = entity.copy(
                status = "COMPLETED",
                syncState = "PENDING",
                updatedAtMillis = now
            )

            returnDao.update(updated)
            updated.safeToDomain() ?: throw IllegalStateException("Failed to map return: $returnId")
        }
    }

    override suspend fun rejectReturn(returnId: String, reason: String?): Result<Return> = withContext(appDispatchers.io) {
        runCatching {
            val now = System.currentTimeMillis()
            val entity = returnDao.getById(returnId)
                ?: throw NoSuchElementException("Return not found: $returnId")

            val updated = entity.copy(
                status = "REJECTED",
                reason = reason?.takeIf { it.isNotBlank() } ?: entity.reason,
                syncState = "PENDING",
                updatedAtMillis = now
            )

            returnDao.update(updated)
            updated.safeToDomain() ?: throw IllegalStateException("Failed to map return: $returnId")
        }
    }

    override suspend fun cancelReturn(returnId: String, reason: String?): Result<Return> = withContext(appDispatchers.io) {
        runCatching {
            val now = System.currentTimeMillis()
            val entity = returnDao.getById(returnId)
                ?: throw NoSuchElementException("Return not found: $returnId")

            val updated = entity.copy(
                status = "CANCELED",
                reason = reason?.takeIf { it.isNotBlank() } ?: entity.reason,
                syncState = "PENDING",
                updatedAtMillis = now
            )

            returnDao.update(updated)
            updated.safeToDomain() ?: throw IllegalStateException("Failed to map return: $returnId")
        }
    }

    override suspend fun calculateReturnAmount(returnId: String): BigDecimal = withContext(appDispatchers.io) {
        returnDao.getById(returnId)?.totalRefundAmount.toBigDecimalOrZero()
    }

    override suspend fun calculateRefundedAmount(returnId: String): BigDecimal = withContext(appDispatchers.io) {
        returnDao.getById(returnId)?.refundedAmount.toBigDecimalOrZero()
    }

    override suspend fun getRefundStatus(returnId: String): RefundStatus = withContext(appDispatchers.io) {
        val raw = returnDao.getById(returnId)?.refundStatus.orEmpty()
        runCatching { enumValueOf<RefundStatus>(raw) }.getOrDefault(RefundStatus.PENDING)
    }

    override suspend fun getReturnsPendingSync(): List<Return> = withContext(appDispatchers.io) {
        returnDao.observePendingSync().first().toDomainListSafely()
    }

    override suspend fun markReturnSynced(returnId: String): Result<Return> = withContext(appDispatchers.io) {
        runCatching {
            val now = System.currentTimeMillis()
            val entity = returnDao.getById(returnId)
                ?: throw NoSuchElementException("Return not found: $returnId")

            val synced = entity.copy(
                syncState = "SYNCED",
                syncedAtMillis = now,
                updatedAtMillis = now
            )

            returnDao.update(synced)
            synced.safeToDomain() ?: throw IllegalStateException("Failed to map return: $returnId")
        }
    }

    override suspend fun deleteReturn(id: String): Result<Unit> = withContext(appDispatchers.io) {
        runCatching {
            val now = System.currentTimeMillis()
            val entity = returnDao.getById(id)

            if (entity != null) {
                returnDao.softDelete(
                    id = id,
                    updatedAtMillis = now,
                    syncState = if (entity.serverId.isNullOrBlank()) "PENDING" else "SYNCED"
                )

                entity.serverId?.let { serverId ->
                    remoteDataSource.deleteReturn(serverId)
                }
            }

            Unit
        }
    }

    override suspend fun deleteReturns(ids: List<String>): Result<Int> = withContext(appDispatchers.io) {
        runCatching {
            ids.count { deleteReturn(it).isSuccess }
        }
    }

    override suspend fun clearAll(): Result<Unit> = withContext(appDispatchers.io) {
        runCatching {
            returnDao.observeAll().first().forEach { entity ->
                returnDao.delete(entity)
            }
            returnDao.purgeDeleted()
            Unit
        }
    }

    override suspend fun countReturns(): Long = withContext(appDispatchers.io) {
        returnDao.countActive().toLong()
    }

    override suspend fun countReturnsByStatus(status: ReturnStatus): Long = withContext(appDispatchers.io) {
        returnDao.observeByStatus(status.name).first().size.toLong()
    }

    override suspend fun countReturnsByType(returnType: ReturnType): Long = withContext(appDispatchers.io) {
        returnDao.observeByType(returnType.name).first().size.toLong()
    }

    override suspend fun countPendingReturns(): Long = withContext(appDispatchers.io) {
        returnDao.observeAll().first().count { it.safeToDomain()?.isPendingLike() == true }.toLong()
    }

    private suspend fun syncToRemote(localEntity: ReturnEntity): ReturnEntity {
        val domain = localEntity.safeToDomain() ?: return localEntity

        val request = domain.toDto(
            serverId = localEntity.serverId,
            syncState = "PENDING",
            isDeleted = localEntity.isDeleted,
            syncedAtMillis = localEntity.syncedAtMillis,
            updatedAtMillis = localEntity.updatedAtMillis
        )

        val remoteResult = if (localEntity.serverId.isNullOrBlank()) {
            remoteDataSource.createReturn(request)
        } else {
            remoteDataSource.updateReturn(localEntity.serverId, request)
        }

        return when (remoteResult) {
            is NetworkResult.Success -> {
                val remoteDto = remoteResult.data
                val syncedEntity = ReturnEntity.fromDomain(
                    returnDoc = remoteDto.dtoToDomain(),
                    serverId = remoteDto.serverId ?: remoteDto.returnId,
                    syncState = remoteDto.syncState,
                    isDeleted = remoteDto.isDeleted,
                    syncedAtMillis = remoteDto.syncedAtMillis ?: System.currentTimeMillis(),
                    updatedAtMillis = remoteDto.updatedAtMillis
                ).copy(
                    id = localEntity.id,
                    updatedAtMillis = System.currentTimeMillis()
                )

                returnDao.update(syncedEntity)
                syncedEntity
            }

            is NetworkResult.Error -> localEntity
            NetworkResult.Loading -> localEntity
        }
    }

    private fun ReturnEntity.safeToDomain(): Return? = runCatching { entityToDomain() }.getOrNull()

    private fun List<ReturnEntity>.toDomainListSafely(): List<Return> {
        return mapNotNull { it.safeToDomain() }
    }

    private fun Return.toEntity(
        serverId: String? = null,
        syncState: String = "PENDING",
        isDeleted: Boolean = false,
        syncedAtMillis: Long? = null,
        updatedAtMillis: Long = System.currentTimeMillis()
    ): ReturnEntity {
        return ReturnEntity.fromDomain(
            returnDoc = this,
            serverId = serverId,
            syncState = syncState,
            isDeleted = isDeleted,
            syncedAtMillis = syncedAtMillis,
            updatedAtMillis = updatedAtMillis
        )
    }

    private fun Return.matchesInvoice(documentId: String): Boolean {
        return originalDocumentId == documentId || originalDocumentNumber == documentId
    }

    private fun Return.isPendingLike(): Boolean {
        return status.name in setOf("DRAFT", "PENDING", "PROCESSING", "IN_PROGRESS") ||
            refundStatus.name in setOf("PENDING", "PARTIAL")
    }

    private fun String?.toBigDecimalOrZero(): BigDecimal {
        return runCatching { BigDecimal(this?.trim().orEmpty()) }.getOrDefault(BigDecimal.ZERO)
    }
}