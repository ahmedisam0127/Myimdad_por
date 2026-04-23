package com.myimdad_por.domain.repository

import com.myimdad_por.domain.model.RefundStatus
import com.myimdad_por.domain.model.Return
import com.myimdad_por.domain.model.ReturnStatus
import com.myimdad_por.domain.model.ReturnType
import java.math.BigDecimal
import java.time.LocalDateTime
import kotlinx.coroutines.flow.Flow

/**
 * Contract for return processing, refund control, and inventory re-entry.
 */
interface ReturnRepository {

    fun observeAllReturns(): Flow<List<Return>>

    fun observeReturnsByInvoice(documentId: String): Flow<List<Return>>

    fun observeReturnsByStatus(status: ReturnStatus): Flow<List<Return>>

    fun observePendingReturns(): Flow<List<Return>>

    suspend fun getReturnById(id: String): Return?

    suspend fun getReturnByNumber(returnNumber: String): Return?

    suspend fun getReturns(
        from: LocalDateTime? = null,
        to: LocalDateTime? = null,
        returnType: ReturnType? = null,
        status: ReturnStatus? = null,
        partyId: String? = null,
        processedByEmployeeId: String? = null
    ): List<Return>

    suspend fun searchReturns(query: String): List<Return>

    suspend fun saveReturn(returnDocument: Return): Result<Return>

    suspend fun saveReturns(returns: List<Return>): Result<List<Return>>

    suspend fun updateReturn(returnDocument: Return): Result<Return>

    suspend fun processReturn(returnId: String): Result<Return>

    suspend fun approveReturn(returnId: String): Result<Return>

    suspend fun completeReturn(returnId: String): Result<Return>

    suspend fun rejectReturn(
        returnId: String,
        reason: String? = null
    ): Result<Return>

    suspend fun cancelReturn(
        returnId: String,
        reason: String? = null
    ): Result<Return>

    suspend fun calculateReturnAmount(returnId: String): BigDecimal

    suspend fun calculateRefundedAmount(returnId: String): BigDecimal

    suspend fun getRefundStatus(returnId: String): RefundStatus

    suspend fun getReturnsPendingSync(): List<Return>

    suspend fun markReturnSynced(returnId: String): Result<Return>

    suspend fun deleteReturn(id: String): Result<Unit>

    suspend fun deleteReturns(ids: List<String>): Result<Int>

    suspend fun clearAll(): Result<Unit>

    suspend fun countReturns(): Long

    suspend fun countReturnsByStatus(status: ReturnStatus): Long

    suspend fun countReturnsByType(returnType: ReturnType): Long

    suspend fun countPendingReturns(): Long
}