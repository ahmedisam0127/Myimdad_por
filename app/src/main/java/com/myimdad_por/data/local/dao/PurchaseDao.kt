package com.myimdad_por.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.myimdad_por.data.local.entity.PurchaseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PurchaseDao {

    @Query(
        """
        SELECT * 
        FROM purchases
        WHERE is_deleted = 0
        ORDER BY created_at_millis DESC, updated_at_millis DESC
        """
    )
    fun observeAll(): Flow<List<PurchaseEntity>>

    @Query(
        """
        SELECT * 
        FROM purchases
        WHERE is_deleted = 0
          AND status = :status
        ORDER BY created_at_millis DESC, updated_at_millis DESC
        """
    )
    fun observeByStatus(status: String): Flow<List<PurchaseEntity>>

    @Query(
        """
        SELECT * 
        FROM purchases
        WHERE is_deleted = 0
          AND payment_status = :paymentStatus
        ORDER BY created_at_millis DESC, updated_at_millis DESC
        """
    )
    fun observeByPaymentStatus(paymentStatus: String): Flow<List<PurchaseEntity>>

    @Query(
        """
        SELECT * 
        FROM purchases
        WHERE is_deleted = 0
          AND supplier_id = :supplierId
        ORDER BY created_at_millis DESC, updated_at_millis DESC
        """
    )
    fun observeBySupplierId(supplierId: String): Flow<List<PurchaseEntity>>

    @Query(
        """
        SELECT * 
        FROM purchases
        WHERE is_deleted = 0
          AND employee_id = :employeeId
        ORDER BY created_at_millis DESC, updated_at_millis DESC
        """
    )
    fun observeByEmployeeId(employeeId: String): Flow<List<PurchaseEntity>>

    @Query(
        """
        SELECT * 
        FROM purchases
        WHERE is_deleted = 0
          AND due_date_millis IS NOT NULL
          AND due_date_millis < :nowMillis
          AND payment_status != 'PAID'
        ORDER BY due_date_millis ASC, created_at_millis DESC
        """
    )
    fun observeOverdue(nowMillis: Long = System.currentTimeMillis()): Flow<List<PurchaseEntity>>

    @Query(
        """
        SELECT * 
        FROM purchases
        WHERE is_deleted = 0
          AND sync_state != 'SYNCED'
        ORDER BY updated_at_millis DESC, created_at_millis DESC
        """
    )
    fun observePendingSync(): Flow<List<PurchaseEntity>>

    @Query(
        """
        SELECT * 
        FROM purchases
        WHERE is_deleted = 0
          AND (
                LOWER(invoice_number) LIKE '%' || LOWER(:query) || '%'
             OR LOWER(supplier_name) LIKE '%' || LOWER(:query) || '%'
             OR LOWER(employee_id) LIKE '%' || LOWER(:query) || '%'
             OR LOWER(COALESCE(note, '')) LIKE '%' || LOWER(:query) || '%'
             OR LOWER(status) LIKE '%' || LOWER(:query) || '%'
             OR LOWER(payment_status) LIKE '%' || LOWER(:query) || '%'
          )
        ORDER BY created_at_millis DESC, updated_at_millis DESC
        """
    )
    fun search(query: String): Flow<List<PurchaseEntity>>

    @Query(
        """
        SELECT * 
        FROM purchases
        WHERE id = :id
        LIMIT 1
        """
    )
    suspend fun getById(id: String): PurchaseEntity?

    @Query(
        """
        SELECT * 
        FROM purchases
        WHERE server_id = :serverId
        LIMIT 1
        """
    )
    suspend fun getByServerId(serverId: String): PurchaseEntity?

    @Query(
        """
        SELECT * 
        FROM purchases
        WHERE invoice_number = :invoiceNumber
        LIMIT 1
        """
    )
    suspend fun getByInvoiceNumber(invoiceNumber: String): PurchaseEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: PurchaseEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<PurchaseEntity>): List<Long>

    @Update
    suspend fun update(entity: PurchaseEntity): Int

    @Delete
    suspend fun delete(entity: PurchaseEntity): Int

    @Query(
        """
        UPDATE purchases
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
        UPDATE purchases
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
        UPDATE purchases
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
        UPDATE purchases
        SET payment_status = :paymentStatus,
            paid_amount = :paidAmount,
            remaining_amount = :remainingAmount,
            updated_at_millis = :updatedAtMillis,
            sync_state = :syncState
        WHERE id = :id
        """
    )
    suspend fun updatePaymentSnapshot(
        id: String,
        paymentStatus: String,
        paidAmount: String,
        remainingAmount: String,
        updatedAtMillis: Long = System.currentTimeMillis(),
        syncState: String = "PENDING"
    ): Int

    @Query(
        """
        UPDATE purchases
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
        FROM purchases
        WHERE is_deleted = 0
        """
    )
    suspend fun countActive(): Int

    @Query(
        """
        SELECT COUNT(*) 
        FROM purchases
        WHERE is_deleted = 1
        """
    )
    suspend fun countDeleted(): Int

    @Query(
        """
        DELETE FROM purchases
        WHERE is_deleted = 1
        """
    )
    suspend fun purgeDeleted(): Int
}