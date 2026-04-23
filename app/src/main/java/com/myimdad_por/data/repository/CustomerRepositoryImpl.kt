package com.myimdad_por.data.repository

import com.myimdad_por.core.dispatchers.AppDispatchers
import com.myimdad_por.core.dispatchers.DefaultAppDispatchers
import com.myimdad_por.core.network.NetworkResult
import com.myimdad_por.data.local.dao.CustomerDao
import com.myimdad_por.data.local.entity.CustomerEntity
import com.myimdad_por.data.mapper.toDomain
import com.myimdad_por.data.mapper.toDto
import com.myimdad_por.data.remote.datasource.CustomerRemoteDataSource
import com.myimdad_por.data.remote.dto.CustomerDto
import com.myimdad_por.domain.model.Customer
import com.myimdad_por.domain.repository.CustomerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.math.BigDecimal
import javax.inject.Inject
import java.math.RoundingMode
import java.util.Locale


class CustomerRepositoryImpl @Inject constructor(
    private val customerDao: CustomerDao,
    private val remoteDataSource: CustomerRemoteDataSource? = null,
    private val dispatchers: AppDispatchers = DefaultAppDispatchers
) : CustomerRepository {

    override fun observeAllCustomers(): Flow<List<Customer>> {
        return customerDao.observeAll()
            .map { entities -> entities.map { it.asDomain() } }
            .flowOn(dispatchers.io)
    }

    override fun observeCustomerById(id: String): Flow<Customer?> {
        val normalizedId = id.trim()
        if (normalizedId.isBlank()) return flowOf(null)

        return customerDao.observeAll()
            .map { entities ->
                entities.firstOrNull { entity ->
                    entity.id == normalizedId || entity.serverId == normalizedId
                }?.asDomain()
            }
            .flowOn(dispatchers.io)
    }

    override fun observeCustomerByEmail(email: String): Flow<Customer?> {
        val normalizedEmail = email.trim().lowercase(Locale.ROOT)
        if (normalizedEmail.isBlank()) return flowOf(null)

        return customerDao.observeAll()
            .map { entities ->
                entities.firstOrNull { entity ->
                    entity.email.clean()?.lowercase(Locale.ROOT) == normalizedEmail
                }?.asDomain()
            }
            .flowOn(dispatchers.io)
    }

    override suspend fun getCustomerById(id: String): Customer? = withContext(dispatchers.io) {
        val normalizedId = id.trim()
        if (normalizedId.isBlank()) return@withContext null

        customerDao.getById(normalizedId)?.asDomain()
            ?: customerDao.getByServerId(normalizedId)?.asDomain()
            ?: fetchRemoteById(normalizedId)
    }

    override suspend fun getCustomerByCode(code: String): Customer? = withContext(dispatchers.io) {
        val normalizedCode = code.trim().uppercase(Locale.ROOT)
        if (normalizedCode.isBlank()) return@withContext null

        customerDao.getByCode(normalizedCode)?.asDomain()
            ?: fetchRemoteByLookup(normalizedCode) { dto ->
                dto.normalizedCode == normalizedCode
            }
    }

    override suspend fun getCustomerByEmail(email: String): Customer? = withContext(dispatchers.io) {
        val normalizedEmail = email.trim().lowercase(Locale.ROOT)
        if (normalizedEmail.isBlank()) return@withContext null

        customerDao.getByEmail(normalizedEmail)?.asDomain()
            ?: fetchRemoteByLookup(normalizedEmail) { dto ->
                dto.email.clean()?.lowercase(Locale.ROOT) == normalizedEmail
            }
    }

    override suspend fun searchCustomers(query: String): List<Customer> = withContext(dispatchers.io) {
        val normalizedQuery = query.trim()
        if (normalizedQuery.isBlank()) return@withContext emptyList()

        val localCustomers = customerDao.search(normalizedQuery)
            .first()
            .map { it.asDomain() }

        if (localCustomers.isNotEmpty() || remoteDataSource == null) {
            return@withContext localCustomers
        }

        val remoteCustomers = when (val result = remoteDataSource.listCustomers(search = normalizedQuery)) {
            is NetworkResult.Success -> result.data.map { it.asDomain() }
            is NetworkResult.Error,
            NetworkResult.Loading -> emptyList()
        }

        if (remoteCustomers.isNotEmpty()) {
            customerDao.insertAll(remoteCustomers.map { it.asEntity(syncState = "SYNCED") })
        }

        remoteCustomers
    }

    override suspend fun saveCustomer(customer: Customer): Result<Customer> = withContext(dispatchers.io) {
        runCatching {
            val normalized = customer.normalize()
            val local = customerDao.getById(normalized.id) ?: customerDao.getByServerId(normalized.id)

            val entity = normalized.asEntity(
                serverId = local?.serverId.clean(),
                syncState = "PENDING",
                isDeleted = local?.isDeleted == true,
                syncedAtMillis = local?.syncedAtMillis
            )

            customerDao.insert(entity)

            val remote = remoteDataSource ?: return@runCatching normalized

            val remoteResult = if (local?.serverId.clean() == null) {
                remote.createCustomer(normalized.toDto(serverId = null, syncState = "PENDING"))
            } else {
                remote.updateCustomer(
                    id = local!!.serverId!!.trim(),
                    request = normalized.toDto(
                        serverId = local.serverId,
                        syncState = "PENDING"
                    )
                )
            }

            when (remoteResult) {
                is NetworkResult.Success -> {
                    val syncedDto = remoteResult.data
                    customerDao.insert(syncedDto.asEntity(syncState = "SYNCED"))
                    syncedDto.asDomain()
                }

                is NetworkResult.Error,
                NetworkResult.Loading -> normalized
            }
        }
    }

    override suspend fun saveCustomers(customers: List<Customer>): Result<List<Customer>> {
        return withContext(dispatchers.io) {
            runCatching {
                if (customers.isEmpty()) return@runCatching emptyList()

                customers.map { customer ->
                    saveCustomer(customer).getOrElse { customer }
                }
            }
        }
    }

    override suspend fun updateCustomer(customer: Customer): Result<Customer> {
        return saveCustomer(customer)
    }

    override suspend fun deleteCustomer(id: String): Result<Unit> = withContext(dispatchers.io) {
        runCatching {
            val normalizedId = id.trim()
            require(normalizedId.isNotBlank()) { "id cannot be blank." }

            val local = customerDao.getById(normalizedId) ?: customerDao.getByServerId(normalizedId)
            customerDao.softDelete(normalizedId, syncState = "PENDING")

            val remote = remoteDataSource ?: return@runCatching Unit
            val remoteId = local?.serverId.clean() ?: normalizedId

            when (remote.deleteCustomer(remoteId)) {
                is NetworkResult.Success -> {
                    customerDao.markSynced(normalizedId, syncState = "SYNCED")
                }

                is NetworkResult.Error,
                NetworkResult.Loading -> Unit
            }

            Unit
        }
    }

    override suspend fun deleteCustomers(ids: List<String>): Result<Int> = withContext(dispatchers.io) {
        runCatching {
            if (ids.isEmpty()) return@runCatching 0

            var successCount = 0
            ids.asSequence()
                .mapNotNull { it.clean() }
                .distinct()
                .forEach { customerId ->
                    if (deleteCustomer(customerId).isSuccess) {
                        successCount++
                    }
                }

            successCount
        }
    }

    override suspend fun clearAll(): Result<Unit> = withContext(dispatchers.io) {
        runCatching {
            val localCustomers = customerDao.observeAll().first()
            localCustomers.forEach { entity ->
                customerDao.softDelete(entity.id, syncState = "PENDING")
            }
            customerDao.purgeDeleted()
            Unit
        }
    }

    override suspend fun countCustomers(): Long = withContext(dispatchers.io) {
        customerDao.countActive().toLong()
    }

    override suspend fun countActiveCustomers(): Long = withContext(dispatchers.io) {
        customerDao.observeActive()
            .first()
            .size
            .toLong()
    }

    override suspend fun getCustomersWithDebt(): List<Customer> = withContext(dispatchers.io) {
        customerDao.observeAll()
            .first()
            .map { it.asDomain() }
            .filter { it.outstandingBalance > BigDecimal.ZERO }
    }

    override suspend fun getCustomersOverLimit(): List<Customer> = withContext(dispatchers.io) {
        customerDao.observeAll()
            .first()
            .map { it.asDomain() }
            .filter { it.outstandingBalance > it.creditLimit }
    }

    private suspend fun fetchRemoteById(id: String): Customer? {
        val remote = remoteDataSource ?: return null

        return when (val result = remote.getCustomer(id)) {
            is NetworkResult.Success -> {
                val dto = result.data
                customerDao.insert(dto.asEntity(syncState = "SYNCED"))
                dto.asDomain()
            }

            is NetworkResult.Error,
            NetworkResult.Loading -> null
        }
    }

    private suspend fun fetchRemoteByLookup(
        query: String,
        predicate: (CustomerDto) -> Boolean
    ): Customer? {
        val remote = remoteDataSource ?: return null

        return when (val result = remote.listCustomers(search = query)) {
            is NetworkResult.Success -> {
                val dto = result.data.firstOrNull(predicate) ?: return null
                customerDao.insert(dto.asEntity(syncState = "SYNCED"))
                dto.asDomain()
            }

            is NetworkResult.Error,
            NetworkResult.Loading -> null
        }
    }

    private fun CustomerEntity.asDomain(): Customer {
        return Customer(
            id = id.trim(),
            code = code.clean(),
            fullName = fullName.trim(),
            tradeName = tradeName.clean(),
            phoneNumber = phoneNumber.clean(),
            email = email.clean(),
            address = address.clean(),
            city = city.clean(),
            country = country.clean(),
            taxNumber = taxNumber.clean(),
            nationalId = nationalId.clean(),
            creditLimit = creditLimit.toBigDecimalOrZero(),
            outstandingBalance = outstandingBalance.toBigDecimalOrZero(),
            isActive = isActive,
            createdAtMillis = createdAtMillis,
            updatedAtMillis = updatedAtMillis,
            lastPurchaseAtMillis = lastPurchaseAtMillis,
            metadata = metadataJson.toStringMap()
        )
    }

    private fun CustomerDto.asDomain(): Customer {
        return Customer(
            id = id.trim(),
            code = code.clean(),
            fullName = fullName.trim(),
            tradeName = tradeName.clean(),
            phoneNumber = phoneNumber.clean(),
            email = email.clean(),
            address = address.clean(),
            city = city.clean(),
            country = country.clean(),
            taxNumber = taxNumber.clean(),
            nationalId = nationalId.clean(),
            creditLimit = creditLimit.toBigDecimalOrZero(),
            outstandingBalance = outstandingBalance.toBigDecimalOrZero(),
            isActive = isActive,
            createdAtMillis = createdAtMillis,
            updatedAtMillis = updatedAtMillis,
            lastPurchaseAtMillis = lastPurchaseAtMillis,
            metadata = metadataJson.toStringMap()
        )
    }

    private fun Customer.asEntity(
        serverId: String? = null,
        syncState: String = "PENDING",
        isDeleted: Boolean = false,
        syncedAtMillis: Long? = null,
        createdAtMillis: Long = this.createdAtMillis,
        updatedAtMillis: Long = this.updatedAtMillis
    ): CustomerEntity {
        return CustomerEntity(
            id = id.trim(),
            serverId = serverId.clean(),
            code = code.clean(),
            fullName = fullName.trim(),
            tradeName = tradeName.clean(),
            phoneNumber = phoneNumber.clean(),
            email = email.clean(),
            address = address.clean(),
            city = city.clean(),
            country = country.clean(),
            taxNumber = taxNumber.clean(),
            nationalId = nationalId.clean(),
            creditLimit = creditLimit.money(),
            outstandingBalance = outstandingBalance.money(),
            isActive = isActive,
            createdAtMillis = createdAtMillis,
            updatedAtMillis = updatedAtMillis,
            lastPurchaseAtMillis = lastPurchaseAtMillis,
            syncState = syncState,
            isDeleted = isDeleted,
            syncedAtMillis = syncedAtMillis,
            metadataJson = metadata.toJsonString()
        )
    }

    private fun CustomerDto.asEntity(
        syncState: String = "SYNCED"
    ): CustomerEntity {
        return CustomerEntity(
            id = id.trim(),
            serverId = serverId.clean() ?: id.trim(),
            code = code.clean(),
            fullName = fullName.trim(),
            tradeName = tradeName.clean(),
            phoneNumber = phoneNumber.clean(),
            email = email.clean(),
            address = address.clean(),
            city = city.clean(),
            country = country.clean(),
            taxNumber = taxNumber.clean(),
            nationalId = nationalId.clean(),
            creditLimit = creditLimit.trim().ifBlank { "0.00" },
            outstandingBalance = outstandingBalance.trim().ifBlank { "0.00" },
            isActive = isActive,
            createdAtMillis = createdAtMillis,
            updatedAtMillis = updatedAtMillis,
            lastPurchaseAtMillis = lastPurchaseAtMillis,
            syncState = syncState,
            isDeleted = isDeleted,
            syncedAtMillis = syncedAtMillis,
            metadataJson = metadataJson.trim().ifBlank { "{}" }
        )
    }

    private fun Customer.normalize(): Customer {
        return Customer(
            id = id.trim(),
            code = code.clean(),
            fullName = fullName.trim(),
            tradeName = tradeName.clean(),
            phoneNumber = phoneNumber.clean(),
            email = email.clean(),
            address = address.clean(),
            city = city.clean(),
            country = country.clean(),
            taxNumber = taxNumber.clean(),
            nationalId = nationalId.clean(),
            creditLimit = creditLimit,
            outstandingBalance = outstandingBalance,
            isActive = isActive,
            createdAtMillis = createdAtMillis,
            updatedAtMillis = updatedAtMillis,
            lastPurchaseAtMillis = lastPurchaseAtMillis,
            metadata = metadata
        )
    }

    private fun Map<String, String>.toJsonString(): String {
        return runCatching {
            JSONObject(this).toString()
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

    private fun String?.clean(): String? {
        return this?.trim()?.takeIf { it.isNotBlank() }
    }

    private fun String?.toBigDecimalOrZero(): BigDecimal {
        return this.clean()?.toBigDecimalOrNull() ?: BigDecimal.ZERO
    }

    private fun BigDecimal.money(): String {
        return setScale(2, RoundingMode.HALF_UP).toPlainString()
    }
}