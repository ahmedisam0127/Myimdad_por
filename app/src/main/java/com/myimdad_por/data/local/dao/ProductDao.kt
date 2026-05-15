package com.myimdad_por.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.myimdad_por.data.local.entity.ProductEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {

    @Query(
        """
        SELECT * 
        FROM products
        WHERE is_deleted = 0
        ORDER BY is_active DESC, name ASC, created_at_millis DESC
        """
    )
    fun observeAll(): Flow<List<ProductEntity>>

    @Query(
        """
        SELECT * 
        FROM products
        WHERE is_deleted = 0
          AND is_active = 1
        ORDER BY name ASC, created_at_millis DESC
        """
    )
    fun observeActive(): Flow<List<ProductEntity>>

    @Query(
        """
        SELECT * 
        FROM products
        WHERE is_deleted = 0
          AND is_active = 0
        ORDER BY name ASC, created_at_millis DESC
        """
    )
    fun observeInactive(): Flow<List<ProductEntity>>

    @Query(
        """
        SELECT * 
        FROM products
        WHERE is_deleted = 0
          AND unit_of_measure = :unitOfMeasure
        ORDER BY name ASC, created_at_millis DESC
        """
    )
    fun observeByUnitOfMeasure(unitOfMeasure: String): Flow<List<ProductEntity>>

    @Query(
        """
        SELECT * 
        FROM products
        WHERE is_deleted = 0
          AND sync_state != 'SYNCED'
        ORDER BY updated_at_millis DESC, created_at_millis DESC
        """
    )
    fun observePendingSync(): Flow<List<ProductEntity>>

    @Query(
        """
        SELECT * 
        FROM products
        WHERE is_deleted = 0
          AND (
                LOWER(barcode) LIKE '%' || LOWER(:query) || '%'
             OR LOWER(normalized_barcode) LIKE '%' || LOWER(:query) || '%'
             OR LOWER(name) LIKE '%' || LOWER(:query) || '%'
             OR LOWER(COALESCE(display_name, '')) LIKE '%' || LOWER(:query) || '%'
             OR LOWER(COALESCE(description, '')) LIKE '%' || LOWER(:query) || '%'
             OR LOWER(search_tokens) LIKE '%' || LOWER(:query) || '%'
          )
        ORDER BY is_active DESC, name ASC, created_at_millis DESC
        """
    )
    fun search(query: String): Flow<List<ProductEntity>>

    @Query(
        """
        SELECT * 
        FROM products
        WHERE id = :id
        LIMIT 1
        """
    )
    suspend fun getById(id: String): ProductEntity?

    @Query(
        """
        SELECT * 
        FROM products
        WHERE server_id = :serverId
        LIMIT 1
        """
    )
    suspend fun getByServerId(serverId: String): ProductEntity?

    @Query(
        """
        SELECT * 
        FROM products
        WHERE barcode = :barcode
        LIMIT 1
        """
    )
    suspend fun getByBarcode(barcode: String): ProductEntity?

    @Query(
        """
        SELECT * 
        FROM products
        WHERE normalized_barcode = :normalizedBarcode
        LIMIT 1
        """
    )
    suspend fun getByNormalizedBarcode(normalizedBarcode: String): ProductEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ProductEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<ProductEntity>): List<Long>

    @Update
    suspend fun update(entity: ProductEntity): Int

    @Delete
    suspend fun delete(entity: ProductEntity): Int

    @Query(
        """
        UPDATE products
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
        UPDATE products
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
        UPDATE products
        SET is_active = :isActive,
            updated_at_millis = :updatedAtMillis,
            sync_state = :syncState
        WHERE id = :id
        """
    )
    suspend fun updateActiveState(
        id: String,
        isActive: Boolean,
        updatedAtMillis: Long = System.currentTimeMillis(),
        syncState: String = "PENDING"
    ): Int

    @Query(
        """
        UPDATE products
        SET price = :price,
            updated_at_millis = :updatedAtMillis,
            sync_state = :syncState
        WHERE id = :id
        """
    )
    suspend fun updatePrice(
        id: String,
        price: String,
        updatedAtMillis: Long = System.currentTimeMillis(),
        syncState: String = "PENDING"
    ): Int

    @Query(
        """
        UPDATE products
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
        FROM products
        WHERE is_deleted = 0
        """
    )
    suspend fun countActive(): Int

    @Query(
        """
        SELECT COUNT(*) 
        FROM products
        WHERE is_deleted = 1
        """
    )
    suspend fun countDeleted(): Int

    @Query(
        """
        DELETE FROM products
        WHERE is_deleted = 1
        """
    )
    suspend fun purgeDeleted(): Int
}