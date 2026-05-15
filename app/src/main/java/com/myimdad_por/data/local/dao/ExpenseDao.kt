package com.myimdad_por.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.myimdad_por.data.local.entity.ExpenseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {

    @Query(
        """
        SELECT * 
        FROM expenses
        WHERE is_deleted = 0
        ORDER BY expense_date_millis DESC, created_at_millis DESC
        """
    )
    fun observeAll(): Flow<List<ExpenseEntity>>

    @Query(
        """
        SELECT * 
        FROM expenses
        WHERE is_deleted = 0
          AND status = :status
        ORDER BY expense_date_millis DESC, created_at_millis DESC
        """
    )
    fun observeByStatus(status: String): Flow<List<ExpenseEntity>>

    @Query(
        """
        SELECT * 
        FROM expenses
        WHERE is_deleted = 0
          AND payment_status = :paymentStatus
        ORDER BY expense_date_millis DESC, created_at_millis DESC
        """
    )
    fun observeByPaymentStatus(paymentStatus: String): Flow<List<ExpenseEntity>>

    @Query(
        """
        SELECT * 
        FROM expenses
        WHERE is_deleted = 0
          AND category = :category
        ORDER BY expense_date_millis DESC, created_at_millis DESC
        """
    )
    fun observeByCategory(category: String): Flow<List<ExpenseEntity>>

    @Query(
        """
        SELECT * 
        FROM expenses
        WHERE is_deleted = 0
          AND employee_id = :employeeId
        ORDER BY expense_date_millis DESC, created_at_millis DESC
        """
    )
    fun observeByEmployee(employeeId: String): Flow<List<ExpenseEntity>>

    @Query(
        """
        SELECT * 
        FROM expenses
        WHERE is_deleted = 0
          AND expense_date_millis BETWEEN :fromMillis AND :toMillis
        ORDER BY expense_date_millis DESC, created_at_millis DESC
        """
    )
    fun observeBetweenDates(fromMillis: Long, toMillis: Long): Flow<List<ExpenseEntity>>

    @Query(
        """
        SELECT * 
        FROM expenses
        WHERE is_deleted = 0
          AND sync_state != 'SYNCED'
        ORDER BY updated_at_millis DESC, expense_date_millis DESC
        """
    )
    fun observePendingSync(): Flow<List<ExpenseEntity>>

    @Query(
        """
        SELECT * 
        FROM expenses
        WHERE is_deleted = 0
          AND (
                LOWER(expense_number) LIKE '%' || LOWER(:query) || '%'
             OR LOWER(title) LIKE '%' || LOWER(:query) || '%'
             OR LOWER(category) LIKE '%' || LOWER(:query) || '%'
             OR LOWER(reference_number) LIKE '%' || LOWER(:query) || '%'
             OR LOWER(supplier_name) LIKE '%' || LOWER(:query) || '%'
             OR LOWER(note) LIKE '%' || LOWER(:query) || '%'
             OR LOWER(payment_method) LIKE '%' || LOWER(:query) || '%'
          )
        ORDER BY expense_date_millis DESC, created_at_millis DESC
        """
    )
    fun search(query: String): Flow<List<ExpenseEntity>>

    @Query(
        """
        SELECT * 
        FROM expenses
        WHERE id = :id
        LIMIT 1
        """
    )
    suspend fun getById(id: String): ExpenseEntity?

    @Query(
        """
        SELECT * 
        FROM expenses
        WHERE server_id = :serverId
        LIMIT 1
        """
    )
    suspend fun getByServerId(serverId: String): ExpenseEntity?

    @Query(
        """
        SELECT * 
        FROM expenses
        WHERE expense_number = :expenseNumber
        LIMIT 1
        """
    )
    suspend fun getByExpenseNumber(expenseNumber: String): ExpenseEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ExpenseEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<ExpenseEntity>): List<Long>

    @Update
    suspend fun update(entity: ExpenseEntity): Int

    @Delete
    suspend fun delete(entity: ExpenseEntity): Int

    @Query(
        """
        UPDATE expenses
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
        UPDATE expenses
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
        UPDATE expenses
        SET paid_amount = :paidAmount,
            payment_status = :paymentStatus,
            updated_at_millis = :updatedAtMillis,
            sync_state = :syncState
        WHERE id = :id
        """
    )
    suspend fun updatePaymentSnapshot(
        id: String,
        paidAmount: String,
        paymentStatus: String,
        updatedAtMillis: Long = System.currentTimeMillis(),
        syncState: String = "PENDING"
    ): Int

    @Query(
        """
        UPDATE expenses
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
        FROM expenses
        WHERE is_deleted = 0
        """
    )
    suspend fun countActive(): Int

    @Query(
        """
        SELECT COUNT(*) 
        FROM expenses
        WHERE is_deleted = 1
        """
    )
    suspend fun countDeleted(): Int

    @Query(
        """
        DELETE FROM expenses
        WHERE is_deleted = 1
        """
    )
    suspend fun purgeDeleted(): Int
}