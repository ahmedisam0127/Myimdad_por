package com.myimdad_por.data.repository

import com.myimdad_por.core.dispatchers.AppDispatchers
import com.myimdad_por.core.dispatchers.DefaultAppDispatchers
import com.myimdad_por.core.network.ApiException
import com.myimdad_por.core.network.NetworkResult
import com.myimdad_por.data.local.dao.SupplierDao
import com.myimdad_por.data.local.entity.SupplierEntity
import com.myimdad_por.data.local.entity.toDomain as entityToDomain
import com.myimdad_por.data.mapper.toDomain
import com.myimdad_por.data.mapper.toDomainList
import com.myimdad_por.data.mapper.toDto
import com.myimdad_por.data.mapper.toDtoList
import com.myimdad_por.data.remote.datasource.SupplierRemoteDataSource
import com.myimdad_por.domain.model.Purchase
import com.myimdad_por.domain.model.Supplier
import com.myimdad_por.domain.repository.SupplierPerformanceSummary
import com.myimdad_por.domain.repository.SupplierRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import javax.inject.Inject
import java.time.LocalDateTime
import java.time.ZoneId

class SupplierRepositoryImpl @Inject constructor(
    private val supplierDao: SupplierDao,
    private val remoteDataSource: SupplierRemoteDataSource,
    private val appDispatchers: AppDispatchers = DefaultAppDispatchers
) : SupplierRepository {

    override fun observeAllSuppliers(): Flow<List<Supplier>> {
        return supplierDao.observeAll().map { it.toSupplierListSafely() }
    }

    override fun observeSupplierById(id: String): Flow<Supplier?> {
        if (id.isBlank()) return flowOf(null)
        return supplierDao.observeAll().map { entities ->
            entities.firstOrNull { it.id == id }?.safeToDomain()
        }
    }

    override fun observeSupplierByCode(supplierCode: String): Flow<Supplier?> {
        val normalized = supplierCode.trim()
        if (normalized.isBlank()) return flowOf(null)
        return supplierDao.observeAll().map { entities ->
            entities.firstOrNull { it.supplierCode.equals(normalized, ignoreCase = true) }?.safeToDomain()
        }
    }

    override fun observeActiveSuppliers(): Flow<List<Supplier>> {
        return supplierDao.observeActive().map { it.toSupplierListSafely() }
    }

    override fun observePreferredSuppliers(): Flow<List<Supplier>> {
        return supplierDao.observePreferred().map { it.toSupplierListSafely() }
    }

    override fun observeSuppliersWithDebt(): Flow<List<Supplier>> {
        return supplierDao.observeAll().map { entities ->
            entities.toSupplierListSafely().filter { it.outstandingBalance > BigDecimal.ZERO }
        }
    }

    override suspend fun getSupplierById(id: String): Supplier? = withContext(appDispatchers.io) {
        require(id.isNotBlank()) { "id cannot be blank." }

        supplierDao.getById(id)?.safeToDomain() ?: runCatching {
            val domain = remoteDataSource.getSupplierById(id).unwrapOrThrow().toDomain()
            upsertLocal(domain, serverId = domain.id)
            domain
        }.getOrNull()
    }

    override suspend fun getSupplierByCode(supplierCode: String): Supplier? = withContext(appDispatchers.io) {
        require(supplierCode.isNotBlank()) { "supplierCode cannot be blank." }

        supplierDao.getByCode(supplierCode)?.safeToDomain() ?: runCatching {
            val domain = remoteDataSource.getSupplierByCode(supplierCode).unwrapOrThrow().toDomain()
            upsertLocal(domain, serverId = domain.id)
            domain
        }.getOrNull()
    }

    override suspend fun getSupplierByEmail(email: String): Supplier? = withContext(appDispatchers.io) {
        val needle = email.trim()
        if (needle.isBlank()) return@withContext null

        supplierDao.observeAll().first()
            .firstOrNull { it.email.equals(needle, ignoreCase = true) }
            ?.safeToDomain()
    }

    override suspend fun searchSuppliers(query: String): List<Supplier> = withContext(appDispatchers.io) {
        val needle = query.trim()
        if (needle.isBlank()) return@withContext emptyList()

        val local = supplierDao.search(needle).first().toSupplierListSafely()
        if (local.isNotEmpty()) return@withContext local

        runCatching {
            val domainList = remoteDataSource.searchSuppliers(needle).unwrapOrThrow().toDomainList()
            upsertLocal(domainList, serverIds = domainList.map { it.id })
            domainList
        }.getOrDefault(emptyList())
    }

    override suspend fun getSuppliersWithDebt(): List<Supplier> = withContext(appDispatchers.io) {
        val local = supplierDao.observeAll().first()
            .toSupplierListSafely()
            .filter { it.outstandingBalance > BigDecimal.ZERO }

        if (local.isNotEmpty()) return@withContext local

        runCatching {
            val domainList = remoteDataSource.getSuppliersWithDebt().unwrapOrThrow().toDomainList()
            upsertLocal(domainList, serverIds = domainList.map { it.id })
            domainList
        }.getOrDefault(emptyList())
    }

    override suspend fun getSupplierBalance(supplierId: String): BigDecimal = withContext(appDispatchers.io) {
        require(supplierId.isNotBlank()) { "supplierId cannot be blank." }

        supplierDao.getById(supplierId)?.outstandingBalanceBigDecimal() ?: runCatching {
            remoteDataSource.getSupplierBalance(supplierId).unwrapOrThrow()
        }.getOrDefault(BigDecimal.ZERO)
    }

    override suspend fun getSupplierBalanceByCode(supplierCode: String): BigDecimal = withContext(appDispatchers.io) {
        require(supplierCode.isNotBlank()) { "supplierCode cannot be blank." }

        supplierDao.getByCode(supplierCode)?.outstandingBalanceBigDecimal() ?: runCatching {
            remoteDataSource.getSupplierBalanceByCode(supplierCode).unwrapOrThrow()
        }.getOrDefault(BigDecimal.ZERO)
    }

    override suspend fun observeSupplierPurchases(supplierId: String): Flow<List<Purchase>> {
        return emptyFlow() // TODO: Implement when Purchase logic is ready
    }

    override suspend fun getSupplierPurchases(
        supplierId: String,
        from: LocalDateTime?,
        to: LocalDateTime?
    ): List<Purchase> {
        return emptyList() // TODO: Implement when Purchase logic is ready
    }

    override suspend fun saveSupplier(supplier: Supplier): Result<Supplier> = withContext(appDispatchers.io) {
        runCatching {
            val request = supplier.toDto()
            val saved = remoteDataSource.saveSupplier(request).unwrapOrThrow().toDomain()
            upsertLocal(saved, serverId = request.id)
            saved
        }
    }

    override suspend fun saveSuppliers(suppliers: List<Supplier>): Result<List<Supplier>> = withContext(appDispatchers.io) {
        runCatching {
            if (suppliers.isEmpty()) return@runCatching emptyList()

            val requests = suppliers.toDtoList()
            val saved = remoteDataSource.saveSuppliers(requests).unwrapOrThrow().toDomainList()
            
            upsertLocal(saved, serverIds = saved.map { it.id })
            saved
        }
    }

    override suspend fun updateSupplier(supplier: Supplier): Result<Supplier> = withContext(appDispatchers.io) {
        runCatching {
            val request = supplier.toDto()
            val updated = remoteDataSource.updateSupplier(supplier.id, request).unwrapOrThrow().toDomain()
            upsertLocal(updated, serverId = request.id)
            updated
        }
    }

    override suspend fun updateSupplierDebt(
        supplierId: String,
        balanceDelta: BigDecimal
    ): Result<Supplier> = withContext(appDispatchers.io) {
        runCatching {
            val updated = remoteDataSource.updateSupplierDebt(supplierId, balanceDelta).unwrapOrThrow().toDomain()
            upsertLocal(updated, serverId = updated.id)
            updated
        }
    }

    override suspend fun markSupplierPreferred(
        supplierId: String,
        preferred: Boolean
    ): Result<Supplier> = withContext(appDispatchers.io) {
        runCatching {
            val updated = remoteDataSource.markSupplierPreferred(supplierId, preferred).unwrapOrThrow().toDomain()
            upsertLocal(updated, serverId = updated.id)
            updated
        }
    }

    override suspend fun markSupplierActive(
        supplierId: String,
        active: Boolean
    ): Result<Supplier> = withContext(appDispatchers.io) {
        runCatching {
            val updated = remoteDataSource.markSupplierActive(supplierId, active).unwrapOrThrow().toDomain()
            upsertLocal(updated, serverId = updated.id)
            updated
        }
    }

    override suspend fun deleteSupplier(id: String): Result<Unit> = withContext(appDispatchers.io) {
        runCatching {
            val entity = supplierDao.getById(id)
            val remoteId = entity?.serverId ?: id
            
            remoteDataSource.deleteSupplier(remoteId).unwrapOrThrow()

            if (entity != null) {
                supplierDao.softDelete(
                    id = entity.id,
                    updatedAtMillis = System.currentTimeMillis(),
                    syncState = "SYNCED"
                )
            }
        }
    }

    override suspend fun deleteSuppliers(ids: List<String>): Result<Int> = withContext(appDispatchers.io) {
        runCatching {
            if (ids.isEmpty()) return@runCatching 0

            val deletedCount = remoteDataSource.deleteSuppliers(ids).unwrapOrThrow()

            ids.forEach { id ->
                supplierDao.getById(id)?.let { entity ->
                    supplierDao.softDelete(
                        id = entity.id,
                        updatedAtMillis = System.currentTimeMillis(),
                        syncState = "SYNCED"
                    )
                }
            }
            deletedCount
        }
    }

    override suspend fun clearAll(): Result<Unit> = withContext(appDispatchers.io) {
        runCatching {
            remoteDataSource.clearAll().unwrapOrThrow()
            supplierDao.observeAll().first().forEach { entity ->
                supplierDao.delete(entity)
            }
        }
    }

    override suspend fun countSuppliers(): Long = withContext(appDispatchers.io) {
        supplierDao.observeAll().first().size.toLong()
    }

    override suspend fun countActiveSuppliers(): Long = withContext(appDispatchers.io) {
        supplierDao.observeActive().first().size.toLong()
    }

    override suspend fun countPreferredSuppliers(): Long = withContext(appDispatchers.io) {
        supplierDao.observePreferred().first().size.toLong()
    }

    override suspend fun countSuppliersWithDebt(): Long = withContext(appDispatchers.io) {
        supplierDao.observeAll().first()
            .mapNotNull { it.safeToDomain() }
            .count { it.outstandingBalance > BigDecimal.ZERO }
            .toLong()
    }

    override suspend fun getSupplierPerformance(
        supplierId: String,
        from: LocalDateTime?,
        to: LocalDateTime?
    ): SupplierPerformanceSummary = withContext(appDispatchers.io) {
        remoteDataSource.getSupplierPerformance(
            supplierId = supplierId,
            fromMillis = from?.toMillis(),
            toMillis = to?.toMillis()
        ).unwrapOrThrow()
    }

    // --- Helper Functions ---

    /**
     * Unwraps a NetworkResult. Returns the data on success, throws the exception on Error/Loading.
     * This dramatically reduces boilerplate in the repository methods.
     */
    private fun <T> NetworkResult<T>.unwrapOrThrow(): T {
        return when (this) {
            is NetworkResult.Success -> data
            is NetworkResult.Error -> throw exception
            NetworkResult.Loading -> throw ApiException.unexpected("Unexpected loading state")
        }
    }

    private fun SupplierEntity.safeToDomain(): Supplier? {
        return runCatching { entityToDomain() }.getOrNull()
    }

    private fun List<SupplierEntity>.toSupplierListSafely(): List<Supplier> {
        return mapNotNull { it.safeToDomain() }
    }

    private suspend fun upsertLocal(supplier: Supplier, serverId: String? = null) {
        val existing = supplierDao.getById(supplier.id)
        val entity = SupplierEntity.fromDomain(
            supplier = supplier,
            serverId = serverId ?: existing?.serverId ?: supplier.id,
            syncState = "SYNCED",
            isDeleted = false,
            syncedAtMillis = System.currentTimeMillis(),
            createdAtMillis = existing?.createdAtMillis ?: supplier.createdAtMillis,
            updatedAtMillis = System.currentTimeMillis()
        )
        supplierDao.insert(entity)
    }

    private suspend fun upsertLocal(suppliers: List<Supplier>, serverIds: List<String> = emptyList()) {
        val entities = suppliers.mapIndexed { index, supplier ->
            val existing = supplierDao.getById(supplier.id)
            SupplierEntity.fromDomain(
                supplier = supplier,
                serverId = serverIds.getOrNull(index) ?: existing?.serverId ?: supplier.id,
                syncState = "SYNCED",
                isDeleted = false,
                syncedAtMillis = System.currentTimeMillis(),
                createdAtMillis = existing?.createdAtMillis ?: supplier.createdAtMillis,
                updatedAtMillis = System.currentTimeMillis()
            )
        }
        supplierDao.insertAll(entities)
    }

    private fun SupplierEntity.outstandingBalanceBigDecimal(): BigDecimal {
        return runCatching { BigDecimal(outstandingBalance) }.getOrDefault(BigDecimal.ZERO)
    }

    private fun LocalDateTime.toMillis(): Long {
        return atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }
}
