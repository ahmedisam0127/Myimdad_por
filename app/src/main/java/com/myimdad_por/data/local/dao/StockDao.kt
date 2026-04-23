package com.myimdad_por.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.myimdad_por.data.local.entity.StockEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StockDao {

    @Query(
        """
        SELECT * 
        FROM stocks
        WHERE is_deleted = 0
        ORDER BY product_name ASC, location ASC, created_at_millis DESC
        """
    )
    fun observeAll(): Flow<List<StockEntity>>

    @Query(
        """
        SELECT * 
        FROM stocks
        WHERE is_deleted = 0
          AND normalized_barcode = :normalizedBarcode
        ORDER BY location ASC, updated_at_millis DESC
        """
    )
    fun observeByNormalizedBarcode(normalizedBarcode: String): Flow<List<StockEntity>>

    @Query(
        """
        SELECT * 
        FROM stocks
        WHERE is_deleted = 0
          AND product_barcode = :productBarcode
        ORDER BY location ASC, updated_at_millis DESC
        """
    )
    fun observeByProductBarcode(productBarcode: String): Flow<List<StockEntity>>

    @Query(
        """
        SELECT * 
        FROM stocks
        WHERE is_deleted = 0
          AND normalized_location = :normalizedLocation
        ORDER BY product_name ASC, updated_at_millis DESC
        """
    )
    fun observeByLocation(normalizedLocation: String): Flow<List<StockEntity>>

    @Query(
        """
        SELECT * 
        FROM stocks
        WHERE is_deleted = 0
          AND unit_of_measure = :unitOfMeasure
        ORDER BY product_name ASC, location ASC, updated_at_millis DESC
        """
    )
    fun observeByUnitOfMeasure(unitOfMeasure: String): Flow<List<StockEntity>>

    @Query(
        """
        SELECT * 
        FROM stocks
        WHERE is_deleted = 0
          AND quantity <= :threshold
        ORDER BY quantity ASC, product_name ASC, location ASC
        """
    )
    fun observeLowStock(threshold: Double): Flow<List<StockEntity>>

    @Query(
        """
        SELECT * 
        FROM stocks
        WHERE is_deleted = 0
          AND quantity = 0
        ORDER BY product_name ASC, location ASC
        """
    )
    fun observeEmptyStock(): Flow<List<StockEntity>>

    @Query(
        """
        SELECT * 
        FROM stocks
        WHERE is_deleted = 0
          AND sync_state != 'SYNCED'
        ORDER BY updated_at_millis DESC, created_at_millis DESC
        """
    )
    fun observePendingSync(): Flow<List<StockEntity>>

    @Query(
        """
        SELECT * 
        FROM stocks
        WHERE is_deleted = 0
          AND (
                LOWER(product_barcode) LIKE '%' || LOWER(:query) || '%'
             OR LOWER(normalized_barcode) LIKE '%' || LOWER(:query) || '%'
             OR LOWER(product_name) LIKE '%' || LOWER(:query) || '%'
             OR LOWER(COALESCE(display_name, '')) LIKE '%' || LOWER(:query) || '%'
             OR LOWER(location) LIKE '%' || LOWER(:query) || '%'
             OR LOWER(normalized_location) LIKE '%' || LOWER(:query) || '%'
          )
        ORDER BY product_name ASC, location ASC, updated_at_millis DESC
        """
    )
    fun search(query: String): Flow<List<StockEntity>>

    @Query(
        """
        SELECT * 
        FROM stocks
        WHERE id = :id
        LIMIT 1
        """
    )
    suspend fun getById(id: String): StockEntity?

    @Query(
        """
        SELECT * 
        FROM stocks
        WHERE server_id = :serverId
        LIMIT 1
        """
    )
    suspend fun getByServerId(serverId: String): StockEntity?

    @Query(
        """
        SELECT * 
        FROM stocks
        WHERE normalized_barcode = :normalizedBarcode
          AND normalized_location = :normalizedLocation
        LIMIT 1
        """
    )
    suspend fun getByBarcodeAndLocation(
        normalizedBarcode: String,
        normalizedLocation: String
    ): StockEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: StockEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<StockEntity>): List<Long>

    @Update
    suspend fun update(entity: StockEntity): Int

    @Delete
    suspend fun delete(entity: StockEntity): Int

    @Query(
        """
        UPDATE stocks
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
        UPDATE stocks
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
        UPDATE stocks
        SET quantity = :quantity,
            updated_at_millis = :updatedAtMillis,
            sync_state = :syncState
        WHERE id = :id
        """
    )
    suspend fun updateQuantity(
        id: String,
        quantity: Double,
        updatedAtMillis: Long = System.currentTimeMillis(),
        syncState: String = "PENDING"
    ): Int

    @Query(
        """
        UPDATE stocks
        SET location = :location,
            normalized_location = :normalizedLocation,
            updated_at_millis = :updatedAtMillis,
            sync_state = :syncState
        WHERE id = :id
        """
    )
    suspend fun updateLocation(
        id: String,
        location: String,
        normalizedLocation: String,
        updatedAtMillis: Long = System.currentTimeMillis(),
        syncState: String = "PENDING"
    ): Int

    @Query(
        """
        UPDATE stocks
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
        FROM stocks
        WHERE is_deleted = 0
        """
    )
    suspend fun countActive(): Int

    @Query(
        """
        SELECT COUNT(*) 
        FROM stocks
        WHERE is_deleted = 1
        """
    )
    suspend fun countDeleted(): Int

    @Query(
        """
        DELETE FROM stocks
        WHERE is_deleted = 1
        """
    )
    suspend fun purgeDeleted(): Int
}