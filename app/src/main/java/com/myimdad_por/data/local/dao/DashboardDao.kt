package com.myimdad_por.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.myimdad_por.data.local.entity.DashboardCacheEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO خاص بتخزين لوحة التحكم ككاش محلي بنمط Singleton Row.
 *
 * الفكرة:
 * - صف واحد فقط داخل الجدول.
 * - يتم استبدال نفس الصف عند التحديث عبر id ثابت داخل DashboardCacheEntity.
 * - الواجهة تراقب التغييرات مباشرة عبر Flow.
 */
@Dao
interface DashboardDao {

    @Query("SELECT * FROM dashboard_cache WHERE id = 0 LIMIT 1")
    suspend fun getCachedDashboard(): DashboardCacheEntity?

    @Query("SELECT * FROM dashboard_cache WHERE id = 0 LIMIT 1")
    fun observeCachedDashboard(): Flow<DashboardCacheEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCachedDashboard(entity: DashboardCacheEntity)

    @Delete
    suspend fun deleteCachedDashboard(entity: DashboardCacheEntity)

    @Query("DELETE FROM dashboard_cache WHERE id = 0")
    suspend fun clearCachedDashboard()

    @Query("SELECT EXISTS(SELECT 1 FROM dashboard_cache WHERE id = 0)")
    suspend fun hasCachedDashboard(): Boolean

    @Query("SELECT COUNT(*) FROM dashboard_cache")
    suspend fun getCachedDashboardRowCount(): Int
}