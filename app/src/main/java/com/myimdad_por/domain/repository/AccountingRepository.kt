package com.myimdad_por.domain.repository

import com.myimdad_por.domain.model.AccountingEntry
import com.myimdad_por.domain.model.AccountingEntryStatus
import com.myimdad_por.domain.model.AccountingSource
import com.myimdad_por.domain.model.CurrencyCode
import java.math.BigDecimal
import java.time.LocalDateTime
import kotlinx.coroutines.flow.Flow

/**
 * Contract for accounting persistence and queries.
 *
 * Hilt bindings should be provided in the data layer implementation.
 */
interface AccountingRepository {

    fun observeAllEntries(): Flow<List<AccountingEntry>>

    fun observeEntriesByReference(referenceId: String): Flow<List<AccountingEntry>>

    suspend fun getEntryById(id: String): AccountingEntry?

    suspend fun getEntries(
        from: LocalDateTime? = null,
        to: LocalDateTime? = null,
        source: AccountingSource? = null,
        status: AccountingEntryStatus? = null,
        currency: CurrencyCode? = null,
        referenceId: String? = null
    ): List<AccountingEntry>

    suspend fun saveEntry(entry: AccountingEntry): Result<AccountingEntry>

    suspend fun saveEntries(entries: List<AccountingEntry>): Result<List<AccountingEntry>>

    suspend fun updateEntry(entry: AccountingEntry): Result<AccountingEntry>

    suspend fun postEntry(id: String): Result<AccountingEntry>

    suspend fun reverseEntry(
        id: String,
        reversedReferenceId: String? = null,
        reversedDescription: String? = null
    ): Result<AccountingEntry>

    suspend fun voidEntry(id: String, note: String? = null): Result<AccountingEntry>

    suspend fun deleteEntry(id: String): Result<Unit>

    suspend fun deleteEntriesByReference(referenceId: String): Result<Int>

    suspend fun clearAll(): Result<Unit>

    suspend fun countEntries(): Long

    suspend fun countEntries(
        from: LocalDateTime? = null,
        to: LocalDateTime? = null,
        source: AccountingSource? = null,
        status: AccountingEntryStatus? = null
    ): Long

    suspend fun getTotalAmount(
        from: LocalDateTime? = null,
        to: LocalDateTime? = null,
        currency: CurrencyCode? = null,
        source: AccountingSource? = null
    ): BigDecimal
}