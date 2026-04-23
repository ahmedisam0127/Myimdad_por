package com.myimdad_por.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.myimdad_por.data.local.entity.EmployeeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EmployeeDao {

    @Query(
        """
        SELECT * 
        FROM employees
        WHERE is_deleted = 0
        ORDER BY full_name COLLATE NOCASE ASC, created_at_millis DESC
        """
    )
    fun observeAll(): Flow<List<EmployeeEntity>>

    @Query(
        """
        SELECT * 
        FROM employees
        WHERE is_deleted = 0
          AND is_active = 1
        ORDER BY full_name COLLATE NOCASE ASC, created_at_millis DESC
        """
    )
    fun observeActive(): Flow<List<EmployeeEntity>>

    @Query(
        """
        SELECT * 
        FROM employees
        WHERE is_deleted = 0
          AND role = :role
        ORDER BY full_name COLLATE NOCASE ASC, created_at_millis DESC
        """
    )
    fun observeByRole(role: String): Flow<List<EmployeeEntity>>

    @Query(
        """
        SELECT * 
        FROM employees
        WHERE is_deleted = 0
          AND department = :department
        ORDER BY full_name COLLATE NOCASE ASC, created_at_millis DESC
        """
    )
    fun observeByDepartment(department: String): Flow<List<EmployeeEntity>>

    @Query(
        """
        SELECT * 
        FROM employees
        WHERE is_deleted = 0
          AND (
                LOWER(full_name) LIKE '%' || LOWER(:query) || '%'
             OR LOWER(employee_code) LIKE '%' || LOWER(:query) || '%'
             OR LOWER(job_title) LIKE '%' || LOWER(:query) || '%'
             OR LOWER(department) LIKE '%' || LOWER(:query) || '%'
             OR LOWER(email) LIKE '%' || LOWER(:query) || '%'
             OR LOWER(phone_number) LIKE '%' || LOWER(:query) || '%'
             OR LOWER(national_id) LIKE '%' || LOWER(:query) || '%'
             OR LOWER(user_id) LIKE '%' || LOWER(:query) || '%'
          )
        ORDER BY full_name COLLATE NOCASE ASC, created_at_millis DESC
        """
    )
    fun search(query: String): Flow<List<EmployeeEntity>>

    @Query(
        """
        SELECT * 
        FROM employees
        WHERE id = :id
        LIMIT 1
        """
    )
    suspend fun getById(id: String): EmployeeEntity?

    @Query(
        """
        SELECT * 
        FROM employees
        WHERE server_id = :serverId
        LIMIT 1
        """
    )
    suspend fun getByServerId(serverId: String): EmployeeEntity?

    @Query(
        """
        SELECT * 
        FROM employees
        WHERE employee_code = :employeeCode
        LIMIT 1
        """
    )
    suspend fun getByEmployeeCode(employeeCode: String): EmployeeEntity?

    @Query(
        """
        SELECT * 
        FROM employees
        WHERE user_id = :userId
        LIMIT 1
        """
    )
    suspend fun getByUserId(userId: String): EmployeeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: EmployeeEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<EmployeeEntity>): List<Long>

    @Update
    suspend fun update(entity: EmployeeEntity): Int

    @Delete
    suspend fun delete(entity: EmployeeEntity): Int

    @Query(
        """
        UPDATE employees
        SET is_deleted = 1,
            is_active = 0,
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
        UPDATE employees
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
        UPDATE employees
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
        FROM employees
        WHERE is_deleted = 0
        """
    )
    suspend fun countActive(): Int

    @Query(
        """
        SELECT COUNT(*) 
        FROM employees
        WHERE is_deleted = 1
        """
    )
    suspend fun countDeleted(): Int

    @Query(
        """
        DELETE FROM employees
        WHERE is_deleted = 1
        """
    )
    suspend fun purgeDeleted(): Int
}