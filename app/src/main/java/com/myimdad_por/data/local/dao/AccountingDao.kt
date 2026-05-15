package com.myimdad_por.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.myimdad_por.data.local.entity.AccountingEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountingDao {

    @Query(
        """
        SELECT * 
        FROM accounting_entries
        WHERE is_deleted = 0
        ORDER BY transaction_date_millis DESC, created_at_millis DESC
        """
    )
    fun observeAll(): Flow<List<AccountingEntryEntity>>

    @Query(
        """
        SELECT * 
        FROM accounting_entries
        WHERE is_deleted = 0
          AND status = :status
        ORDER BY transaction_date_millis DESC, created_at_millis DESC
        """
    )
    fun observeByStatus(status: String): Flow<List<AccountingEntryEntity>>

    @Query(
        """
        SELECT * 
        FROM accounting_entries
        WHERE is_deleted = 0
          AND sync_state != 'SYNCED'
        ORDER BY updated_at_millis DESC, transaction_date_millis DESC
        """
    )
    fun observePendingSync(): Flow<List<AccountingEntryEntity>>

    @Query(
        """
        SELECT * 
        FROM accounting_entries
        WHERE entry_id = :id
        LIMIT 1
        """
    )
    suspend fun getById(id: String): AccountingEntryEntity?

    @Query(
        """
        SELECT * 
        FROM accounting_entries
        WHERE server_id = :serverId
        LIMIT 1
        """
    )
    suspend fun getByServerId(serverId: String): AccountingEntryEntity?

    @Query(
        """
        SELECT * 
        FROM accounting_entries
        WHERE is_deleted = 0
          AND (
                LOWER(description) LIKE '%' || LOWER(:query) || '%'
             OR LOWER(reference_id) LIKE '%' || LOWER(:query) || '%'
             OR LOWER(debit_account) LIKE '%' || LOWER(:query) || '%'
             OR LOWER(credit_account) LIKE '%' || LOWER(:query) || '%'
             OR LOWER(source) LIKE '%' || LOWER(:query) || '%'
             OR LOWER(status) LIKE '%' || LOWER(:query) || '%'
          )
        ORDER BY transaction_date_millis DESC, created_at_millis DESC
        """
    )
    fun search(query: String): Flow<List<AccountingEntryEntity>>

    @Query(
        """
        SELECT * 
        FROM accounting_entries
        WHERE is_deleted = 0
          AND transaction_date_millis BETWEEN :fromMillis AND :toMillis
        ORDER BY transaction_date_millis DESC, created_at_millis DESC
        """
    )
    fun observeBetweenDates(fromMillis: Long, toMillis: Long): Flow<List<AccountingEntryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: AccountingEntryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<AccountingEntryEntity>): List<Long>

    @Update
    suspend fun update(entity: AccountingEntryEntity): Int

    @Delete
    suspend fun delete(entity: AccountingEntryEntity): Int

    @Query(
        """
        UPDATE accounting_entries
        SET is_deleted = 1,
            updated_at_millis = :updatedAtMillis,
            sync_state = :syncState
        WHERE entry_id = :id
        """
    )
    suspend fun softDelete(
        id: String,
        updatedAtMillis: Long = System.currentTimeMillis(),
        syncState: String = "PENDING"
    ): Int

    @Query(
        """
        UPDATE accounting_entries
        SET sync_state = :syncState,
            synced_at_millis = :syncedAtMillis,
            updated_at_millis = :updatedAtMillis
        WHERE entry_id = :id
        """
    )
    suspend fun markSynced(
        id: String,
        syncState: String = "SYNCED",
        syncedAtMillis: Long = System.currentTimeMillis(),
        updatedAtMillis: Long = System.currentTimeMillis()
    ): Int

    @Query(
        """
        DELETE FROM accounting_entries
        WHERE is_deleted = 1
        """
    )
    suspend fun purgeDeleted(): Int

    @Query(
        """
        DELETE FROM accounting_entries
        WHERE entry_id = :id
        """
    )
    suspend fun deleteById(id: String): Int

    @Query(
        """
        SELECT COUNT(*) 
        FROM accounting_entries
        WHERE is_deleted = 0
        """
    )
    suspend fun countActive(): Int
}