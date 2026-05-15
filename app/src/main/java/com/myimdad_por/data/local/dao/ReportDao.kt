package com.myimdad_por.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.myimdad_por.data.local.entity.ReportEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReportDao {

    @Query(
        """
        SELECT * 
        FROM reports
        ORDER BY generated_at_millis DESC
        """
    )
    fun observeAll(): Flow<List<ReportEntity>>

    @Query(
        """
        SELECT * 
        FROM reports
        WHERE report_id = :reportId
        LIMIT 1
        """
    )
    fun observeById(reportId: String): Flow<ReportEntity?>

    @Query(
        """
        SELECT * 
        FROM reports
        WHERE report_id = :reportId
        LIMIT 1
        """
    )
    suspend fun getById(reportId: String): ReportEntity?

    @Query(
        """
        SELECT * 
        FROM reports
        WHERE type = :type
        ORDER BY generated_at_millis DESC
        """
    )
    fun observeByType(type: String): Flow<List<ReportEntity>>

    @Query(
        """
        SELECT * 
        FROM reports
        WHERE generated_by_user_id = :userId
        ORDER BY generated_at_millis DESC
        """
    )
    fun observeByGeneratedByUserId(userId: String): Flow<List<ReportEntity>>

    @Query(
        """
        SELECT * 
        FROM reports
        WHERE is_exported = :exported
        ORDER BY generated_at_millis DESC
        """
    )
    fun observeByExported(exported: Boolean): Flow<List<ReportEntity>>

    @Query(
        """
        SELECT * 
        FROM reports
        WHERE generated_at_millis BETWEEN :fromMillis AND :toMillis
        ORDER BY generated_at_millis DESC
        """
    )
    fun observeByDateRange(fromMillis: Long, toMillis: Long): Flow<List<ReportEntity>>

    @Query(
        """
        SELECT * 
        FROM reports
        WHERE 
            title LIKE '%' || :query || '%' COLLATE NOCASE
            OR type LIKE '%' || :query || '%' COLLATE NOCASE
            OR IFNULL(summary, '') LIKE '%' || :query || '%' COLLATE NOCASE
            OR IFNULL(generated_by_user_id, '') LIKE '%' || :query || '%' COLLATE NOCASE
            OR IFNULL(filters_json, '') LIKE '%' || :query || '%' COLLATE NOCASE
            OR IFNULL(metadata_json, '') LIKE '%' || :query || '%' COLLATE NOCASE
        ORDER BY generated_at_millis DESC
        """
    )
    fun search(query: String): Flow<List<ReportEntity>>

    @Query("SELECT COUNT(*) FROM reports")
    fun observeCount(): Flow<Int>

    @Query(
        """
        SELECT COUNT(*) 
        FROM reports
        WHERE type = :type
        """
    )
    fun observeCountByType(type: String): Flow<Int>

    @Query(
        """
        SELECT COUNT(*) 
        FROM reports
        WHERE is_exported = :exported
        """
    )
    fun observeCountByExported(exported: Boolean): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(report: ReportEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(reports: List<ReportEntity>): List<Long>

    @Update
    suspend fun update(report: ReportEntity): Int

    @Delete
    suspend fun delete(report: ReportEntity): Int

    @Query(
        """
        DELETE FROM reports
        WHERE report_id = :reportId
        """
    )
    suspend fun deleteById(reportId: String): Int

    @Query("DELETE FROM reports")
    suspend fun deleteAll(): Int

    @Query(
        """
        UPDATE reports
        SET is_exported = 1
        WHERE report_id = :reportId
        """
    )
    suspend fun markAsExported(reportId: String): Int

    @Query(
        """
        UPDATE reports
        SET is_exported = 0
        WHERE report_id = :reportId
        """
    )
    suspend fun markAsNotExported(reportId: String): Int
}