package com.myimdad_por.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.myimdad_por.data.local.entity.ReturnEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReturnDao {

    @Query(
        """
        SELECT * 
        FROM returns
        WHERE is_deleted = 0
        ORDER BY return_date_millis DESC, updated_at_millis DESC
        """
    )
    fun observeAll(): Flow<List<ReturnEntity>>

    @Query(
        """
        SELECT * 
        FROM returns
        WHERE is_deleted = 0
          AND return_type = :returnType
        ORDER BY return_date_millis DESC, updated_at_millis DESC
        """
    )
    fun observeByType(returnType: String): Flow<List<ReturnEntity>>

    @Query(
        """
        SELECT * 
        FROM returns
        WHERE is_deleted = 0
          AND status = :status
        ORDER BY return_date_millis DESC, updated_at_millis DESC
        """
    )
    fun observeByStatus(status: String): Flow<List<ReturnEntity>>

    @Query(
        """
        SELECT * 
        FROM returns
        WHERE is_deleted = 0
          AND refund_status = :refundStatus
        ORDER BY return_date_millis DESC, updated_at_millis DESC
        """
    )
    fun observeByRefundStatus(refundStatus: String): Flow<List<ReturnEntity>>

    @Query(
        """
        SELECT * 
        FROM returns
        WHERE is_deleted = 0
          AND party_id = :partyId
        ORDER BY return_date_millis DESC, updated_at_millis DESC
        """
    )
    fun observeByPartyId(partyId: String): Flow<List<ReturnEntity>>

    @Query(
        """
        SELECT * 
        FROM returns
        WHERE is_deleted = 0
          AND processed_by_employee_id = :employeeId
        ORDER BY return_date_millis DESC, updated_at_millis DESC
        """
    )
    fun observeByEmployeeId(employeeId: String): Flow<List<ReturnEntity>>

    @Query(
        """
        SELECT * 
        FROM returns
        WHERE is_deleted = 0
          AND refund_status IN ('PENDING', 'PARTIAL')
        ORDER BY return_date_millis DESC, updated_at_millis DESC
        """
    )
    fun observePendingRefunds(): Flow<List<ReturnEntity>>

    @Query(
        """
        SELECT * 
        FROM returns
        WHERE is_deleted = 0
          AND sync_state != 'SYNCED'
        ORDER BY updated_at_millis DESC, return_date_millis DESC
        """
    )
    fun observePendingSync(): Flow<List<ReturnEntity>>

    @Query(
        """
        SELECT * 
        FROM returns
        WHERE is_deleted = 0
          AND (
                LOWER(return_number) LIKE '%' || LOWER(:query) || '%'
             OR LOWER(COALESCE(original_document_number, '')) LIKE '%' || LOWER(:query) || '%'
             OR LOWER(COALESCE(party_name, '')) LIKE '%' || LOWER(:query) || '%'
             OR LOWER(COALESCE(reason, '')) LIKE '%' || LOWER(:query) || '%'
             OR LOWER(COALESCE(note, '')) LIKE '%' || LOWER(:query) || '%'
             OR LOWER(return_type) LIKE '%' || LOWER(:query) || '%'
             OR LOWER(status) LIKE '%' || LOWER(:query) || '%'
             OR LOWER(refund_status) LIKE '%' || LOWER(:query) || '%'
          )
        ORDER BY return_date_millis DESC, updated_at_millis DESC
        """
    )
    fun search(query: String): Flow<List<ReturnEntity>>

    @Query(
        """
        SELECT * 
        FROM returns
        WHERE id = :id
        LIMIT 1
        """
    )
    suspend fun getById(id: String): ReturnEntity?

    @Query(
        """
        SELECT * 
        FROM returns
        WHERE server_id = :serverId
        LIMIT 1
        """
    )
    suspend fun getByServerId(serverId: String): ReturnEntity?

    @Query(
        """
        SELECT * 
        FROM returns
        WHERE return_number = :returnNumber
        LIMIT 1
        """
    )
    suspend fun getByReturnNumber(returnNumber: String): ReturnEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ReturnEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<ReturnEntity>): List<Long>

    @Update
    suspend fun update(entity: ReturnEntity): Int

    @Delete
    suspend fun delete(entity: ReturnEntity): Int

    @Query(
        """
        UPDATE returns
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
        UPDATE returns
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
        UPDATE returns
        SET status = :status,
            updated_at_millis = :updatedAtMillis,
            sync_state = :syncState
        WHERE id = :id
        """
    )
    suspend fun updateStatus(
        id: String,
        status: String,
        updatedAtMillis: Long = System.currentTimeMillis(),
        syncState: String = "PENDING"
    ): Int

    @Query(
        """
        UPDATE returns
        SET refund_status = :refundStatus,
            refunded_amount = :refundedAmount,
            remaining_refund_amount = :remainingRefundAmount,
            updated_at_millis = :updatedAtMillis,
            sync_state = :syncState
        WHERE id = :id
        """
    )
    suspend fun updateRefundSnapshot(
        id: String,
        refundStatus: String,
        refundedAmount: String,
        remainingRefundAmount: String,
        updatedAtMillis: Long = System.currentTimeMillis(),
        syncState: String = "PENDING"
    ): Int

    @Query(
        """
        UPDATE returns
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
        FROM returns
        WHERE is_deleted = 0
        """
    )
    suspend fun countActive(): Int

    @Query(
        """
        SELECT COUNT(*) 
        FROM returns
        WHERE is_deleted = 1
        """
    )
    suspend fun countDeleted(): Int

    @Query(
        """
        DELETE FROM returns
        WHERE is_deleted = 1
        """
    )
    suspend fun purgeDeleted(): Int
}