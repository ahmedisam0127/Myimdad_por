package com.myimdad_por.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.myimdad_por.data.local.entity.PaymentTransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PaymentDao {

    @Query(
        """
        SELECT * 
        FROM payment_transactions
        WHERE is_deleted = 0
        ORDER BY created_at_millis DESC, updated_at_millis DESC
        """
    )
    fun observeAll(): Flow<List<PaymentTransactionEntity>>

    @Query(
        """
        SELECT * 
        FROM payment_transactions
        WHERE is_deleted = 0
          AND status = :status
        ORDER BY created_at_millis DESC, updated_at_millis DESC
        """
    )
    fun observeByStatus(status: String): Flow<List<PaymentTransactionEntity>>

    @Query(
        """
        SELECT * 
        FROM payment_transactions
        WHERE is_deleted = 0
          AND currency_code = :currencyCode
        ORDER BY created_at_millis DESC, updated_at_millis DESC
        """
    )
    fun observeByCurrencyCode(currencyCode: String): Flow<List<PaymentTransactionEntity>>

    @Query(
        """
        SELECT * 
        FROM payment_transactions
        WHERE is_deleted = 0
          AND payment_method_id = :paymentMethodId
        ORDER BY created_at_millis DESC, updated_at_millis DESC
        """
    )
    fun observeByPaymentMethodId(paymentMethodId: String): Flow<List<PaymentTransactionEntity>>

    @Query(
        """
        SELECT * 
        FROM payment_transactions
        WHERE is_deleted = 0
          AND sync_state != 'SYNCED'
        ORDER BY updated_at_millis DESC, created_at_millis DESC
        """
    )
    fun observePendingSync(): Flow<List<PaymentTransactionEntity>>

    @Query(
        """
        SELECT * 
        FROM payment_transactions
        WHERE is_deleted = 0
          AND (
                LOWER(transaction_id) LIKE '%' || LOWER(:query) || '%'
             OR LOWER(COALESCE(reference_number, '')) LIKE '%' || LOWER(:query) || '%'
             OR LOWER(COALESCE(payment_intent_id, '')) LIKE '%' || LOWER(:query) || '%'
             OR LOWER(COALESCE(payment_method_name, '')) LIKE '%' || LOWER(:query) || '%'
             OR LOWER(COALESCE(provider_name, '')) LIKE '%' || LOWER(:query) || '%'
             OR LOWER(COALESCE(provider_reference, '')) LIKE '%' || LOWER(:query) || '%'
             OR LOWER(status) LIKE '%' || LOWER(:query) || '%'
             OR LOWER(currency_code) LIKE '%' || LOWER(:query) || '%'
          )
        ORDER BY created_at_millis DESC, updated_at_millis DESC
        """
    )
    fun search(query: String): Flow<List<PaymentTransactionEntity>>

    @Query(
        """
        SELECT * 
        FROM payment_transactions
        WHERE id = :id
        LIMIT 1
        """
    )
    suspend fun getById(id: String): PaymentTransactionEntity?

    @Query(
        """
        SELECT * 
        FROM payment_transactions
        WHERE server_id = :serverId
        LIMIT 1
        """
    )
    suspend fun getByServerId(serverId: String): PaymentTransactionEntity?

    @Query(
        """
        SELECT * 
        FROM payment_transactions
        WHERE transaction_id = :transactionId
        LIMIT 1
        """
    )
    suspend fun getByTransactionId(transactionId: String): PaymentTransactionEntity?

    @Query(
        """
        SELECT * 
        FROM payment_transactions
        WHERE payment_intent_id = :paymentIntentId
        LIMIT 1
        """
    )
    suspend fun getByPaymentIntentId(paymentIntentId: String): PaymentTransactionEntity?

    @Query(
        """
        SELECT * 
        FROM payment_transactions
        WHERE reference_number = :referenceNumber
        LIMIT 1
        """
    )
    suspend fun getByReferenceNumber(referenceNumber: String): PaymentTransactionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: PaymentTransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<PaymentTransactionEntity>): List<Long>

    @Update
    suspend fun update(entity: PaymentTransactionEntity): Int

    @Delete
    suspend fun delete(entity: PaymentTransactionEntity): Int

    @Query(
        """
        UPDATE payment_transactions
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
        UPDATE payment_transactions
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
        UPDATE payment_transactions
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
        UPDATE payment_transactions
        SET provider_name = :providerName,
            provider_reference = :providerReference,
            receipt_number = :receiptNumber,
            updated_at_millis = :updatedAtMillis,
            sync_state = :syncState
        WHERE id = :id
        """
    )
    suspend fun updateProviderSnapshot(
        id: String,
        providerName: String?,
        providerReference: String?,
        receiptNumber: String?,
        updatedAtMillis: Long = System.currentTimeMillis(),
        syncState: String = "PENDING"
    ): Int

    @Query(
        """
        UPDATE payment_transactions
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
        FROM payment_transactions
        WHERE is_deleted = 0
        """
    )
    suspend fun countActive(): Int

    @Query(
        """
        SELECT COUNT(*) 
        FROM payment_transactions
        WHERE is_deleted = 1
        """
    )
    suspend fun countDeleted(): Int

    @Query(
        """
        DELETE FROM payment_transactions
        WHERE is_deleted = 1
        """
    )
    suspend fun purgeDeleted(): Int
}