package com.myimdad_por.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.myimdad_por.data.local.entity.PendingSyncEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingSyncDao {

    @Query(
        """
        SELECT * 
        FROM pending_sync
        ORDER BY priority DESC, created_at_millis ASC, updated_at_millis ASC
        """
    )
    fun observeAll(): Flow<List<PendingSyncEntity>>

    @Query(
        """
        SELECT * 
        FROM pending_sync
        WHERE status = :status
        ORDER BY priority DESC, created_at_millis ASC, updated_at_millis ASC
        """
    )
    fun observeByStatus(status: String): Flow<List<PendingSyncEntity>>

    @Query(
        """
        SELECT * 
        FROM pending_sync
        WHERE entity_type = :entityType
        ORDER BY priority DESC, created_at_millis ASC, updated_at_millis ASC
        """
    )
    fun observeByEntityType(entityType: String): Flow<List<PendingSyncEntity>>

    @Query(
        """
        SELECT * 
        FROM pending_sync
        WHERE operation = :operation
        ORDER BY priority DESC, created_at_millis ASC, updated_at_millis ASC
        """
    )
    fun observeByOperation(operation: String): Flow<List<PendingSyncEntity>>

    @Query(
        """
        SELECT * 
        FROM pending_sync
        WHERE status IN ('PENDING', 'RETRY')
          AND (next_attempt_at_millis IS NULL OR next_attempt_at_millis <= :nowMillis)
        ORDER BY priority DESC, COALESCE(next_attempt_at_millis, 0) ASC, created_at_millis ASC
        """
    )
    fun observeReadyToProcess(nowMillis: Long = System.currentTimeMillis()): Flow<List<PendingSyncEntity>>

    @Query(
        """
        SELECT * 
        FROM pending_sync
        WHERE locked_at_millis IS NOT NULL
        ORDER BY locked_at_millis DESC, updated_at_millis DESC
        """
    )
    fun observeLocked(): Flow<List<PendingSyncEntity>>

    @Query(
        """
        SELECT * 
        FROM pending_sync
        WHERE attempt_count < max_retry_count
          AND status IN ('PENDING', 'RETRY', 'FAILED')
        ORDER BY priority DESC, created_at_millis ASC
        """
    )
    fun observeRetryable(): Flow<List<PendingSyncEntity>>

    @Query(
        """
        SELECT * 
        FROM pending_sync
        WHERE id = :id
        LIMIT 1
        """
    )
    suspend fun getById(id: String): PendingSyncEntity?

    @Query(
        """
        SELECT * 
        FROM pending_sync
        WHERE entity_type = :entityType
          AND entity_id = :entityId
          AND operation = :operation
        LIMIT 1
        """
    )
    suspend fun getByUniqueKey(
        entityType: String,
        entityId: String,
        operation: String
    ): PendingSyncEntity?

    @Query(
        """
        SELECT * 
        FROM pending_sync
        WHERE entity_type = :entityType
          AND entity_id = :entityId
        ORDER BY created_at_millis DESC
        LIMIT 1
        """
    )
    suspend fun getLatestForEntity(
        entityType: String,
        entityId: String
    ): PendingSyncEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: PendingSyncEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<PendingSyncEntity>): List<Long>

    @Update
    suspend fun update(entity: PendingSyncEntity): Int

    @Query(
        """
        DELETE FROM pending_sync
        WHERE id = :id
        """
    )
    suspend fun deleteById(id: String): Int

    @Query(
        """
        DELETE FROM pending_sync
        WHERE entity_type = :entityType
          AND entity_id = :entityId
          AND operation = :operation
        """
    )
    suspend fun deleteByUniqueKey(
        entityType: String,
        entityId: String,
        operation: String
    ): Int

    @Query(
        """
        UPDATE pending_sync
        SET status = 'IN_PROGRESS',
            locked_at_millis = :nowMillis,
            updated_at_millis = :nowMillis
        WHERE id = :id
        """
    )
    suspend fun markInProgress(
        id: String,
        nowMillis: Long = System.currentTimeMillis()
    ): Int

    @Query(
        """
        UPDATE pending_sync
        SET status = 'RETRY',
            attempt_count = attempt_count + 1,
            last_error_message = :errorMessage,
            next_attempt_at_millis = :nextAttemptAtMillis,
            locked_at_millis = NULL,
            updated_at_millis = :nowMillis
        WHERE id = :id
        """
    )
    suspend fun markRetry(
        id: String,
        nextAttemptAtMillis: Long,
        errorMessage: String? = null,
        nowMillis: Long = System.currentTimeMillis()
    ): Int

    @Query(
        """
        UPDATE pending_sync
        SET status = 'COMPLETED',
            completed_at_millis = :nowMillis,
            locked_at_millis = NULL,
            updated_at_millis = :nowMillis
        WHERE id = :id
        """
    )
    suspend fun markCompleted(
        id: String,
        nowMillis: Long = System.currentTimeMillis()
    ): Int

    @Query(
        """
        UPDATE pending_sync
        SET status = 'FAILED',
            last_error_message = :errorMessage,
            locked_at_millis = NULL,
            updated_at_millis = :nowMillis
        WHERE id = :id
        """
    )
    suspend fun markFailed(
        id: String,
        errorMessage: String? = null,
        nowMillis: Long = System.currentTimeMillis()
    ): Int

    @Query(
        """
        UPDATE pending_sync
        SET status = 'CANCELED',
            locked_at_millis = NULL,
            updated_at_millis = :nowMillis
        WHERE id = :id
        """
    )
    suspend fun cancel(
        id: String,
        nowMillis: Long = System.currentTimeMillis()
    ): Int

    @Query(
        """
        UPDATE pending_sync
        SET locked_at_millis = NULL,
            updated_at_millis = :nowMillis
        WHERE locked_at_millis IS NOT NULL
          AND locked_at_millis < :thresholdMillis
        """
    )
    suspend fun releaseStaleLocks(
        thresholdMillis: Long,
        nowMillis: Long = System.currentTimeMillis()
    ): Int

    @Query(
        """
        UPDATE pending_sync
        SET attempt_count = 0,
            status = 'PENDING',
            next_attempt_at_millis = NULL,
            last_error_message = NULL,
            locked_at_millis = NULL,
            completed_at_millis = NULL,
            updated_at_millis = :nowMillis
        WHERE entity_type = :entityType
          AND entity_id = :entityId
        """
    )
    suspend fun resetForEntity(
        entityType: String,
        entityId: String,
        nowMillis: Long = System.currentTimeMillis()
    ): Int

    @Query(
        """
        SELECT COUNT(*) 
        FROM pending_sync
        """
    )
    suspend fun countAll(): Int

    @Query(
        """
        SELECT COUNT(*) 
        FROM pending_sync
        WHERE status = 'PENDING'
        """
    )
    suspend fun countPending(): Int

    @Query(
        """
        SELECT COUNT(*) 
        FROM pending_sync
        WHERE status = 'RETRY'
        """
    )
    suspend fun countRetry(): Int

    @Query(
        """
        DELETE FROM pending_sync
        WHERE status IN ('COMPLETED', 'CANCELED')
        """
    )
    suspend fun purgeFinished(): Int
}