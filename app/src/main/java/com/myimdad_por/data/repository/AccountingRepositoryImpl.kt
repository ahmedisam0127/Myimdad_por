package com.myimdad_por.data.repository

import com.myimdad_por.core.network.NetworkResult
import com.myimdad_por.data.local.dao.AccountingDao
import com.myimdad_por.data.local.entity.AccountingEntryEntity
import com.myimdad_por.data.mapper.toDto as accountingEntryToDto
import com.myimdad_por.data.mapper.toDomain as accountingDtoToDomain
import com.myimdad_por.data.remote.datasource.AccountingRemoteDataSource
import com.myimdad_por.domain.model.AccountingEntry
import com.myimdad_por.domain.model.AccountingEntryStatus
import com.myimdad_por.domain.model.AccountingSource
import com.myimdad_por.domain.model.CurrencyCode
import com.myimdad_por.domain.model.PaymentMethod
import com.myimdad_por.domain.model.PaymentMethodType
import com.myimdad_por.domain.repository.AccountingRepository
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class AccountingRepositoryImpl @Inject constructor(
    private val dao: AccountingDao,
    private val remoteDataSource: AccountingRemoteDataSource? = null
) : AccountingRepository {

    override fun observeAllEntries(): Flow<List<AccountingEntry>> {
        return dao.observeAll().map { list -> list.map { it.toDomainModel() } }
    }

    override fun observeEntriesByReference(referenceId: String): Flow<List<AccountingEntry>> {
        require(referenceId.isNotBlank()) { "referenceId cannot be blank" }
        return observeAllEntries().map { list -> list.filter { it.referenceId == referenceId } }
    }

    override suspend fun getEntryById(id: String): AccountingEntry? {
        require(id.isNotBlank()) { "id cannot be blank" }
        return dao.getById(id)?.toDomainModel()
    }

    override suspend fun getEntries(
        from: LocalDateTime?,
        to: LocalDateTime?,
        source: AccountingSource?,
        status: AccountingEntryStatus?,
        currency: CurrencyCode?,
        referenceId: String?
    ): List<AccountingEntry> {
        val all = dao.observeAll().first().map { it.toDomainModel() }

        return all.asSequence()
            .filter { from == null || !it.transactionDate.isBefore(from) }
            .filter { to == null || !it.transactionDate.isAfter(to) }
            .filter { source == null || it.source == source }
            .filter { status == null || it.status == status }
            .filter { currency == null || it.currency == currency }
            .filter { referenceId == null || it.referenceId == referenceId }
            .toList()
    }

    override suspend fun saveEntry(entry: AccountingEntry): Result<AccountingEntry> {
        require(entry.id.isNotBlank()) { "entry.id cannot be blank" }
        require(entry.amount > BigDecimal.ZERO) { "amount must be greater than zero" }

        return runCatching {
            val now = System.currentTimeMillis()

            dao.insert(
                entry.toEntity(
                    serverId = null,
                    syncState = "PENDING",
                    isDeleted = false,
                    syncedAtMillis = null,
                    createdAtMillis = now,
                    updatedAtMillis = now
                )
            )

            val remoteSaved = syncCreateRemote(entry)
            if (remoteSaved != null) {
                upsertLocalFromDomain(
                    remoteSaved,
                    serverId = remoteSaved.id,
                    syncState = "SYNCED",
                    isDeleted = false,
                    syncedAtMillis = System.currentTimeMillis(),
                    createdAtMillis = now,
                    updatedAtMillis = System.currentTimeMillis()
                )
                remoteSaved
            } else {
                entry
            }
        }
    }

    override suspend fun saveEntries(entries: List<AccountingEntry>): Result<List<AccountingEntry>> {
        if (entries.isEmpty()) return Result.success(emptyList())

        return runCatching {
            entries.map { saveEntry(it).getOrThrow() }
        }
    }

    override suspend fun updateEntry(entry: AccountingEntry): Result<AccountingEntry> {
        require(entry.id.isNotBlank()) { "entry.id cannot be blank" }

        return runCatching {
            val current = dao.getById(entry.id)
                ?: throw IllegalStateException("Accounting entry not found: ${entry.id}")

            dao.update(
                entry.toEntity(
                    serverId = current.serverId,
                    syncState = current.syncState,
                    isDeleted = current.isDeleted,
                    syncedAtMillis = current.syncedAtMillis,
                    createdAtMillis = current.createdAtMillis,
                    updatedAtMillis = System.currentTimeMillis()
                )
            )

            val remoteUpdated = current.serverId?.takeIf { it.isNotBlank() }?.let { serverId ->
                syncUpdateRemote(serverId, entry)
            }

            if (remoteUpdated != null) {
                upsertLocalFromDomain(
                    remoteUpdated,
                    serverId = current.serverId,
                    syncState = "SYNCED",
                    isDeleted = current.isDeleted,
                    syncedAtMillis = System.currentTimeMillis(),
                    createdAtMillis = current.createdAtMillis,
                    updatedAtMillis = System.currentTimeMillis()
                )
                remoteUpdated
            } else {
                entry
            }
        }
    }

    override suspend fun postEntry(id: String): Result<AccountingEntry> {
        require(id.isNotBlank()) { "id cannot be blank" }

        return runCatching {
            val current = dao.getById(id)
                ?: throw IllegalStateException("Accounting entry not found: $id")

            val posted = current.toDomainModel().copy(
                status = AccountingEntryStatus.POSTED
            )

            dao.update(
                posted.toEntity(
                    serverId = current.serverId,
                    syncState = "PENDING",
                    isDeleted = current.isDeleted,
                    syncedAtMillis = current.syncedAtMillis,
                    createdAtMillis = current.createdAtMillis,
                    updatedAtMillis = System.currentTimeMillis()
                )
            )

            val remotePosted = current.serverId?.takeIf { it.isNotBlank() }?.let { serverId ->
                when (val result = remoteDataSource?.postEntry(serverId)) {
                    is NetworkResult.Success -> result.data.accountingDtoToDomain()
                    is NetworkResult.Error -> null
                    NetworkResult.Loading -> null
                    null -> null
                }
            }

            if (remotePosted != null) {
                upsertLocalFromDomain(
                    remotePosted,
                    serverId = current.serverId,
                    syncState = "SYNCED",
                    isDeleted = current.isDeleted,
                    syncedAtMillis = System.currentTimeMillis(),
                    createdAtMillis = current.createdAtMillis,
                    updatedAtMillis = System.currentTimeMillis()
                )
                remotePosted
            } else {
                posted
            }
        }
    }

    override suspend fun reverseEntry(
        id: String,
        reversedReferenceId: String?,
        reversedDescription: String?
    ): Result<AccountingEntry> {
        require(id.isNotBlank()) { "id cannot be blank" }

        return runCatching {
            val current = getEntryById(id)
                ?: throw IllegalStateException("Accounting entry not found: $id")

            val reversed = AccountingEntry(
                id = current.id,
                transactionDate = LocalDateTime.now(),
                referenceId = reversedReferenceId ?: current.referenceId,
                description = reversedDescription ?: "Reversal: ${current.description}",
                debitAccount = current.creditAccount,
                creditAccount = current.debitAccount,
                amount = current.amount,
                currency = current.currency,
                paymentMethod = current.paymentMethod,
                source = AccountingSource.REVERSAL,
                status = AccountingEntryStatus.REVERSED,
                createdByEmployeeId = current.createdByEmployeeId,
                note = current.note
            )

            updateEntry(reversed).getOrThrow()
            reversed
        }
    }

    override suspend fun voidEntry(id: String, note: String?): Result<AccountingEntry> {
        require(id.isNotBlank()) { "id cannot be blank" }

        return runCatching {
            val current = getEntryById(id)
                ?: throw IllegalStateException("Accounting entry not found: $id")

            val voided = current.copy(
                status = AccountingEntryStatus.VOID,
                note = note ?: current.note
            )

            updateEntry(voided).getOrThrow()
        }
    }

    override suspend fun deleteEntry(id: String): Result<Unit> {
        require(id.isNotBlank()) { "id cannot be blank" }

        return runCatching {
            val current = dao.getById(id) ?: return@runCatching
            dao.softDelete(id)

            current.serverId?.takeIf { it.isNotBlank() }?.let { serverId ->
                remoteDataSource?.deleteEntry(serverId)
            }
        }
    }

    override suspend fun deleteEntriesByReference(referenceId: String): Result<Int> {
        require(referenceId.isNotBlank()) { "referenceId cannot be blank" }

        return runCatching {
            val entries = getEntries(referenceId = referenceId)
            var deleted = 0
            entries.forEach { entry ->
                if (deleteEntry(entry.id).isSuccess) deleted++
            }
            deleted
        }
    }

    override suspend fun clearAll(): Result<Unit> {
        return runCatching {
            dao.observeAll().first().forEach { entity ->
                dao.softDelete(entity.id)
            }
            dao.purgeDeleted()
        }
    }

    override suspend fun countEntries(): Long {
        return dao.countActive().toLong()
    }

    override suspend fun countEntries(
        from: LocalDateTime?,
        to: LocalDateTime?,
        source: AccountingSource?,
        status: AccountingEntryStatus?
    ): Long {
        return getEntries(
            from = from,
            to = to,
            source = source,
            status = status
        ).size.toLong()
    }

    override suspend fun getTotalAmount(
        from: LocalDateTime?,
        to: LocalDateTime?,
        currency: CurrencyCode?,
        source: AccountingSource?
    ): BigDecimal {
        return getEntries(
            from = from,
            to = to,
            source = source,
            currency = currency
        ).fold(BigDecimal.ZERO) { acc, entry ->
            acc.add(entry.amount)
        }
    }

    private suspend fun syncCreateRemote(entry: AccountingEntry): AccountingEntry? {
        val remote = remoteDataSource ?: return null
        return when (val result = remote.createEntry(entry.accountingEntryToDto())) {
            is NetworkResult.Success -> result.data.accountingDtoToDomain()
            is NetworkResult.Error -> null
            NetworkResult.Loading -> null
        }
    }

    private suspend fun syncUpdateRemote(serverId: String, entry: AccountingEntry): AccountingEntry? {
        val remote = remoteDataSource ?: return null
        return when (val result = remote.updateEntry(serverId, entry.accountingEntryToDto(serverId = serverId))) {
            is NetworkResult.Success -> result.data.accountingDtoToDomain()
            is NetworkResult.Error -> null
            NetworkResult.Loading -> null
        }
    }

    private suspend fun upsertLocalFromDomain(
        entry: AccountingEntry,
        serverId: String? = null,
        syncState: String = "PENDING",
        isDeleted: Boolean = false,
        syncedAtMillis: Long? = null,
        createdAtMillis: Long = System.currentTimeMillis(),
        updatedAtMillis: Long = System.currentTimeMillis()
    ) {
        dao.insert(
            entry.toEntity(
                serverId = serverId,
                syncState = syncState,
                isDeleted = isDeleted,
                syncedAtMillis = syncedAtMillis,
                createdAtMillis = createdAtMillis,
                updatedAtMillis = updatedAtMillis
            )
        )
    }

    private fun AccountingEntry.toEntity(
        serverId: String? = null,
        syncState: String = "PENDING",
        isDeleted: Boolean = false,
        syncedAtMillis: Long? = null,
        createdAtMillis: Long = System.currentTimeMillis(),
        updatedAtMillis: Long = System.currentTimeMillis()
    ): AccountingEntryEntity {
        return AccountingEntryEntity(
            id = id,
            serverId = serverId,
            transactionDateMillis = transactionDate.toMillis(),
            referenceId = referenceId,
            description = description,
            debitAccount = debitAccount,
            creditAccount = creditAccount,
            amount = amount.moneyString(),
            currencyCode = currency.name,
            paymentMethodId = paymentMethod?.id,
            source = source.name,
            status = status.name,
            createdByEmployeeId = createdByEmployeeId,
            note = note,
            isDeleted = isDeleted,
            syncState = syncState,
            createdAtMillis = createdAtMillis,
            updatedAtMillis = updatedAtMillis,
            syncedAtMillis = syncedAtMillis
        )
    }

    private fun AccountingEntryEntity.toDomainModel(): AccountingEntry {
        val currency = runCatching {
            CurrencyCode.valueOf(currencyCode.trim().uppercase())
        }.getOrDefault(CurrencyCode.SDG)

        val source = runCatching {
            AccountingSource.valueOf(this.source.trim().uppercase())
        }.getOrDefault(AccountingSource.MANUAL)

        val status = runCatching {
            AccountingEntryStatus.valueOf(this.status.trim().uppercase())
        }.getOrDefault(AccountingEntryStatus.POSTED)

        return AccountingEntry(
            id = id,
            transactionDate = transactionDateMillis.toLocalDateTime(),
            referenceId = referenceId,
            description = description,
            debitAccount = debitAccount,
            creditAccount = creditAccount,
            amount = amount.toBigDecimalOrZero(),
            currency = currency,
            paymentMethod = paymentMethodId?.takeIf { it.isNotBlank() }?.let {
                PaymentMethod.Custom(
                    id = it,
                    name = it,
                    type = PaymentMethodType.OTHER,
                    providerName = null,
                    requiresReference = false,
                    extraFees = BigDecimal.ZERO,
                    supportedCurrencies = setOf(CurrencyCode.SDG, CurrencyCode.EGP),
                    isActive = true
                )
            },
            source = source,
            status = status,
            createdByEmployeeId = createdByEmployeeId,
            note = note
        )
    }

    private fun LocalDateTime.toMillis(): Long {
        return atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    private fun Long.toLocalDateTime(): LocalDateTime {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(this), ZoneId.systemDefault())
    }

    private fun BigDecimal.moneyString(): String {
        return setScale(2, RoundingMode.HALF_UP).toPlainString()
    }

    private fun String.toBigDecimalOrZero(): BigDecimal {
        return runCatching { BigDecimal(this) }.getOrElse { BigDecimal.ZERO }
    }
}