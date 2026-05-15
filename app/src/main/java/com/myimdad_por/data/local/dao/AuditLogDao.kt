package com.myimdad_por.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.myimdad_por.data.local.entity.AuditLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AuditLogDao {

    @Query(
        """
        SELECT * 
        FROM audit_logs
        ORDER BY timestamp_millis DESC, created_at_millis DESC
        """
    )
    fun observeAll(): Flow<List<AuditLogEntity>>

    @Query(
        """
        SELECT * 
        FROM audit_logs
        WHERE action = :action
        ORDER BY timestamp_millis DESC, created_at_millis DESC
        """
    )
    fun observeByAction(action: String): Flow<List<AuditLogEntity>>

    @Query(
        """
        SELECT * 
        FROM audit_logs
        WHERE severity = :severity
        ORDER BY timestamp_millis DESC, created_at_millis DESC
        """
    )
    fun observeBySeverity(severity: String): Flow<List<AuditLogEntity>>

    @Query(
        """
        SELECT * 
        FROM audit_logs
        WHERE actor_type = :actorType
        ORDER BY timestamp_millis DESC, created_at_millis DESC
        """
    )
    fun observeByActorType(actorType: String): Flow<List<AuditLogEntity>>

    @Query(
        """
        SELECT * 
        FROM audit_logs
        WHERE target_type = :targetType
        ORDER BY timestamp_millis DESC, created_at_millis DESC
        """
    )
    fun observeByTargetType(targetType: String): Flow<List<AuditLogEntity>>

    @Query(
        """
        SELECT * 
        FROM audit_logs
        WHERE correlation_id = :correlationId
        ORDER BY timestamp_millis DESC, created_at_millis DESC
        """
    )
    fun observeByCorrelationId(correlationId: String): Flow<List<AuditLogEntity>>

    @Query(
        """
        SELECT * 
        FROM audit_logs
        WHERE timestamp_millis BETWEEN :fromMillis AND :toMillis
        ORDER BY timestamp_millis DESC, created_at_millis DESC
        """
    )
    fun observeBetween(fromMillis: Long, toMillis: Long): Flow<List<AuditLogEntity>>

    @Query(
        """
        SELECT * 
        FROM audit_logs
        WHERE log_id = :id
        LIMIT 1
        """
    )
    suspend fun getById(id: String): AuditLogEntity?

    @Query(
        """
        SELECT * 
        FROM audit_logs
        WHERE server_id = :serverId
        LIMIT 1
        """
    )
    suspend fun getByServerId(serverId: String): AuditLogEntity?

    @Query(
        """
        SELECT * 
        FROM audit_logs
        WHERE (
                LOWER(action) LIKE '%' || LOWER(:query) || '%'
             OR LOWER(severity) LIKE '%' || LOWER(:query) || '%'
             OR LOWER(actor_label) LIKE '%' || LOWER(:query) || '%'
             OR LOWER(target_label) LIKE '%' || LOWER(:query) || '%'
             OR LOWER(note) LIKE '%' || LOWER(:query) || '%'
             OR LOWER(correlation_id) LIKE '%' || LOWER(:query) || '%'
          )
        ORDER BY timestamp_millis DESC, created_at_millis DESC
        """
    )
    fun search(query: String): Flow<List<AuditLogEntity>>

    @Query(
        """
        SELECT * 
        FROM audit_logs
        WHERE action IN (
            'LOGIN',
            'LOGOUT',
            'CHANGE_PASSWORD',
            'AUTHORIZE_PAYMENT',
            'VOID_INVOICE',
            'REFUND',
            'REJECT',
            'APPROVE'
        )
        ORDER BY timestamp_millis DESC, created_at_millis DESC
        """
    )
    fun observeSecurityRelevant(): Flow<List<AuditLogEntity>>

    @Query(
        """
        SELECT * 
        FROM audit_logs
        ORDER BY timestamp_millis DESC, created_at_millis DESC
        LIMIT :limit
        """
    )
    fun observeLatest(limit: Int): Flow<List<AuditLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: AuditLogEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<AuditLogEntity>): List<Long>

    @Update
    suspend fun update(entity: AuditLogEntity): Int

    @Delete
    suspend fun delete(entity: AuditLogEntity): Int

    @Query(
        """
        UPDATE audit_logs
        SET sync_state = :syncState,
            synced_at_millis = :syncedAtMillis
        WHERE log_id = :id
        """
    )
    suspend fun markSynced(
        id: String,
        syncState: String = "SYNCED",
        syncedAtMillis: Long = System.currentTimeMillis()
    ): Int

    @Query(
        """
        SELECT COUNT(*) 
        FROM audit_logs
        """
    )
    suspend fun countAll(): Int

    @Query(
        """
        DELETE FROM audit_logs
        WHERE timestamp_millis < :olderThanMillis
        """
    )
    suspend fun deleteOlderThan(olderThanMillis: Long): Int

    @Query(
        """
        DELETE FROM audit_logs
        WHERE log_id = :id
        """
    )
    suspend fun deleteById(id: String): Int
}