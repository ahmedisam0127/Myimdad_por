package com.myimdad_por.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.myimdad_por.data.local.entity.InvoiceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InvoiceDao {

    @Query(
        """
        SELECT * 
        FROM invoices
        WHERE is_deleted = 0
        ORDER BY issue_date_millis DESC, created_at_millis DESC
        """
    )
    fun observeAll(): Flow<List<InvoiceEntity>>

    @Query(
        """
        SELECT * 
        FROM invoices
        WHERE is_deleted = 0
          AND invoice_type = :invoiceType
        ORDER BY issue_date_millis DESC, created_at_millis DESC
        """
    )
    fun observeByType(invoiceType: String): Flow<List<InvoiceEntity>>

    @Query(
        """
        SELECT * 
        FROM invoices
        WHERE is_deleted = 0
          AND status = :status
        ORDER BY issue_date_millis DESC, created_at_millis DESC
        """
    )
    fun observeByStatus(status: String): Flow<List<InvoiceEntity>>

    @Query(
        """
        SELECT * 
        FROM invoices
        WHERE is_deleted = 0
          AND payment_status = :paymentStatus
        ORDER BY issue_date_millis DESC, created_at_millis DESC
        """
    )
    fun observeByPaymentStatus(paymentStatus: String): Flow<List<InvoiceEntity>>

    @Query(
        """
        SELECT * 
        FROM invoices
        WHERE is_deleted = 0
          AND party_id = :partyId
        ORDER BY issue_date_millis DESC, created_at_millis DESC
        """
    )
    fun observeByPartyId(partyId: String): Flow<List<InvoiceEntity>>

    @Query(
        """
        SELECT * 
        FROM invoices
        WHERE is_deleted = 0
          AND issued_by_employee_id = :employeeId
        ORDER BY issue_date_millis DESC, created_at_millis DESC
        """
    )
    fun observeByEmployeeId(employeeId: String): Flow<List<InvoiceEntity>>

    @Query(
        """
        SELECT * 
        FROM invoices
        WHERE is_deleted = 0
          AND due_date_millis IS NOT NULL
          AND due_date_millis < :nowMillis
          AND payment_status != 'PAID'
        ORDER BY due_date_millis ASC, issue_date_millis DESC
        """
    )
    fun observeOverdue(nowMillis: Long = System.currentTimeMillis()): Flow<List<InvoiceEntity>>

    @Query(
        """
        SELECT * 
        FROM invoices
        WHERE is_deleted = 0
          AND sync_state != 'SYNCED'
        ORDER BY updated_at_millis DESC, issue_date_millis DESC
        """
    )
    fun observePendingSync(): Flow<List<InvoiceEntity>>

    @Query(
        """
        SELECT * 
        FROM invoices
        WHERE is_deleted = 0
          AND (
                LOWER(invoice_number) LIKE '%' || LOWER(:query) || '%'
             OR LOWER(party_name) LIKE '%' || LOWER(:query) || '%'
             OR LOWER(party_tax_number) LIKE '%' || LOWER(:query) || '%'
             OR LOWER(notes) LIKE '%' || LOWER(:query) || '%'
             OR LOWER(terms_and_conditions) LIKE '%' || LOWER(:query) || '%'
             OR LOWER(invoice_type) LIKE '%' || LOWER(:query) || '%'
             OR LOWER(status) LIKE '%' || LOWER(:query) || '%'
          )
        ORDER BY issue_date_millis DESC, created_at_millis DESC
        """
    )
    fun search(query: String): Flow<List<InvoiceEntity>>

    @Query(
        """
        SELECT * 
        FROM invoices
        WHERE id = :id
        LIMIT 1
        """
    )
    suspend fun getById(id: String): InvoiceEntity?

    @Query(
        """
        SELECT * 
        FROM invoices
        WHERE server_id = :serverId
        LIMIT 1
        """
    )
    suspend fun getByServerId(serverId: String): InvoiceEntity?

    @Query(
        """
        SELECT * 
        FROM invoices
        WHERE invoice_number = :invoiceNumber
        LIMIT 1
        """
    )
    suspend fun getByInvoiceNumber(invoiceNumber: String): InvoiceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: InvoiceEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<InvoiceEntity>): List<Long>

    @Update
    suspend fun update(entity: InvoiceEntity): Int

    @Delete
    suspend fun delete(entity: InvoiceEntity): Int

    @Query(
        """
        UPDATE invoices
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
        UPDATE invoices
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
        UPDATE invoices
        SET payment_status = :paymentStatus,
            paid_amount = :paidAmount,
            updated_at_millis = :updatedAtMillis,
            sync_state = :syncState
        WHERE id = :id
        """
    )
    suspend fun updatePaymentSnapshot(
        id: String,
        paymentStatus: String,
        paidAmount: String,
        updatedAtMillis: Long = System.currentTimeMillis(),
        syncState: String = "PENDING"
    ): Int

    @Query(
        """
        UPDATE invoices
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
        UPDATE invoices
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
        FROM invoices
        WHERE is_deleted = 0
        """
    )
    suspend fun countActive(): Int

    @Query(
        """
        SELECT COUNT(*) 
        FROM invoices
        WHERE is_deleted = 1
        """
    )
    suspend fun countDeleted(): Int

    @Query(
        """
        DELETE FROM invoices
        WHERE is_deleted = 1
        """
    )
    suspend fun purgeDeleted(): Int
}