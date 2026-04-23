package com.myimdad_por.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.myimdad_por.data.local.entity.SupplierEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SupplierDao {

    @Query(
        """
        SELECT * FROM suppliers
        WHERE is_deleted = 0
        ORDER BY is_preferred DESC, company_name COLLATE NOCASE ASC, created_at_millis DESC
        """
    )
    fun observeAll(): Flow<List<SupplierEntity>>

    @Query(
        """
        SELECT * FROM suppliers
        WHERE is_deleted = 0
          AND is_active = 1
        ORDER BY company_name COLLATE NOCASE ASC, created_at_millis DESC
        """
    )
    fun observeActive(): Flow<List<SupplierEntity>>

    @Query(
        """
        SELECT * FROM suppliers
        WHERE is_deleted = 0
          AND is_preferred = 1
        ORDER BY company_name COLLATE NOCASE ASC, created_at_millis DESC
        """
    )
    fun observePreferred(): Flow<List<SupplierEntity>>

    @Query(
        """
        SELECT * FROM suppliers
        WHERE is_deleted = 0
          AND (
                LOWER(company_name) LIKE '%' || LOWER(:query) || '%'
             OR LOWER(COALESCE(contact_person, '')) LIKE '%' || LOWER(:query) || '%'
             OR LOWER(COALESCE(supplier_code, '')) LIKE '%' || LOWER(:query) || '%'
             OR LOWER(COALESCE(phone_number, '')) LIKE '%' || LOWER(:query) || '%'
             OR LOWER(COALESCE(email, '')) LIKE '%' || LOWER(:query) || '%'
             OR LOWER(COALESCE(tax_number, '')) LIKE '%' || LOWER(:query) || '%'
             OR LOWER(COALESCE(city, '')) LIKE '%' || LOWER(:query) || '%'
          )
        ORDER BY is_preferred DESC, company_name COLLATE NOCASE ASC
        """
    )
    fun search(query: String): Flow<List<SupplierEntity>>

    @Query(
        """
        SELECT * FROM suppliers
        WHERE is_deleted = 0
          AND sync_state != 'SYNCED'
        ORDER BY updated_at_millis DESC, created_at_millis DESC
        """
    )
    fun observePendingSync(): Flow<List<SupplierEntity>>

    @Query(
        """
        SELECT * FROM suppliers
        WHERE id = :id
        LIMIT 1
        """
    )
    suspend fun getById(id: String): SupplierEntity?

    @Query(
        """
        SELECT * FROM suppliers
        WHERE server_id = :serverId
        LIMIT 1
        """
    )
    suspend fun getByServerId(serverId: String): SupplierEntity?

    @Query(
        """
        SELECT * FROM suppliers
        WHERE supplier_code = :code
        LIMIT 1
        """
    )
    suspend fun getByCode(code: String): SupplierEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: SupplierEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<SupplierEntity>): List<Long>

    @Update
    suspend fun update(entity: SupplierEntity): Int

    @Delete
    suspend fun delete(entity: SupplierEntity): Int

    @Query(
        """
        UPDATE suppliers
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
        UPDATE suppliers
        SET outstanding_balance = :newBalance,
            last_supply_at_millis = :lastSupplyAt,
            updated_at_millis = :updatedAtMillis,
            sync_state = :syncState
        WHERE id = :id
        """
    )
    suspend fun updateFinancials(
        id: String,
        newBalance: String,
        lastSupplyAt: Long? = System.currentTimeMillis(),
        updatedAtMillis: Long = System.currentTimeMillis(),
        syncState: String = "PENDING"
    ): Int

    @Query(
        """
        UPDATE suppliers
        SET is_preferred = :isPreferred,
            updated_at_millis = :updatedAtMillis,
            sync_state = :syncState
        WHERE id = :id
        """
    )
    suspend fun togglePreferred(
        id: String,
        isPreferred: Boolean,
        updatedAtMillis: Long = System.currentTimeMillis(),
        syncState: String = "PENDING"
    ): Int

    @Query(
        """
        UPDATE suppliers
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
        FROM suppliers
        WHERE is_deleted = 0
        """
    )
    suspend fun countActive(): Int

    @Query(
        """
        DELETE FROM suppliers
        WHERE is_deleted = 1
        """
    )
    suspend fun purgeDeleted(): Int
}
