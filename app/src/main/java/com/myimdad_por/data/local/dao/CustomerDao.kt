package com.myimdad_por.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.myimdad_por.data.local.entity.CustomerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomerDao {

    @Query(
        """
        SELECT * 
        FROM customers
        WHERE is_deleted = 0
        ORDER BY full_name COLLATE NOCASE ASC, created_at_millis DESC
        """
    )
    fun observeAll(): Flow<List<CustomerEntity>>

    @Query(
        """
        SELECT * 
        FROM customers
        WHERE is_deleted = 0
          AND is_active = 1
        ORDER BY full_name COLLATE NOCASE ASC, created_at_millis DESC
        """
    )
    fun observeActive(): Flow<List<CustomerEntity>>

    @Query(
        """
        SELECT * 
        FROM customers
        WHERE is_deleted = 0
          AND (
                LOWER(full_name) LIKE '%' || LOWER(:query) || '%'
             OR LOWER(trade_name) LIKE '%' || LOWER(:query) || '%'
             OR LOWER(phone_number) LIKE '%' || LOWER(:query) || '%'
             OR LOWER(email) LIKE '%' || LOWER(:query) || '%'
             OR LOWER(code) LIKE '%' || LOWER(:query) || '%'
             OR LOWER(national_id) LIKE '%' || LOWER(:query) || '%'
             OR LOWER(tax_number) LIKE '%' || LOWER(:query) || '%'
          )
        ORDER BY full_name COLLATE NOCASE ASC, created_at_millis DESC
        """
    )
    fun search(query: String): Flow<List<CustomerEntity>>

    @Query(
        """
        SELECT * 
        FROM customers
        WHERE is_deleted = 0
          AND sync_state != 'SYNCED'
        ORDER BY updated_at_millis DESC, created_at_millis DESC
        """
    )
    fun observePendingSync(): Flow<List<CustomerEntity>>

    @Query(
        """
        SELECT * 
        FROM customers
        WHERE id = :id
        LIMIT 1
        """
    )
    suspend fun getById(id: String): CustomerEntity?

    @Query(
        """
        SELECT * 
        FROM customers
        WHERE server_id = :serverId
        LIMIT 1
        """
    )
    suspend fun getByServerId(serverId: String): CustomerEntity?

    @Query(
        """
        SELECT * 
        FROM customers
        WHERE code = :code
        LIMIT 1
        """
    )
    suspend fun getByCode(code: String): CustomerEntity?

    @Query(
        """
        SELECT * 
        FROM customers
        WHERE phone_number = :phoneNumber
        LIMIT 1
        """
    )
    suspend fun getByPhoneNumber(phoneNumber: String): CustomerEntity?

    @Query(
        """
        SELECT * 
        FROM customers
        WHERE email = :email
        LIMIT 1
        """
    )
    suspend fun getByEmail(email: String): CustomerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: CustomerEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<CustomerEntity>): List<Long>

    @Update
    suspend fun update(entity: CustomerEntity): Int

    @Delete
    suspend fun delete(entity: CustomerEntity): Int

    @Query(
        """
        UPDATE customers
        SET is_deleted = 1,
            updated_at_millis = :updatedAtMillis,
            sync_state = :syncState
        WHERE id = :id
        """
    )
    suspend fun softDelete(
        id: String,
        updatedAtMillis: Long = System.currentTimeMillis(),
        syncState: String = "PENDING"
    ): Int

    @Query(
        """
        UPDATE customers
        SET is_deleted = 0,
            updated_at_millis = :updatedAtMillis,
            sync_state = :syncState
        WHERE id = :id
        """
    )
    suspend fun restore(
        id: String,
        updatedAtMillis: Long = System.currentTimeMillis(),
        syncState: String = "PENDING"
    ): Int

    @Query(
        """
        UPDATE customers
        SET outstanding_balance = :outstandingBalance,
            last_purchase_at_millis = :lastPurchaseAtMillis,
            updated_at_millis = :updatedAtMillis,
            sync_state = :syncState
        WHERE id = :id
        """
    )
    suspend fun updateFinancialSnapshot(
        id: String,
        outstandingBalance: String,
        lastPurchaseAtMillis: Long? = null,
        updatedAtMillis: Long = System.currentTimeMillis(),
        syncState: String = "PENDING"
    ): Int

    @Query(
        """
        UPDATE customers
        SET sync_state = :syncState,
            synced_at_millis = :syncedAtMillis,
            updated_at_millis = :updatedAtMillis
        WHERE id = :id
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
        SELECT COUNT(*) 
        FROM customers
        WHERE is_deleted = 0
        """
    )
    suspend fun countActive(): Int

    @Query(
        """
        SELECT COUNT(*) 
        FROM customers
        WHERE is_deleted = 1
        """
    )
    suspend fun countDeleted(): Int

    @Query(
        """
        DELETE FROM customers
        WHERE is_deleted = 1
        """
    )
    suspend fun purgeDeleted(): Int
}