package com.myimdad_por.domain.usecase

import com.myimdad_por.domain.model.RefundStatus
import com.myimdad_por.domain.model.Return
import com.myimdad_por.domain.model.ReturnStatus
import com.myimdad_por.domain.model.ReturnType
import com.myimdad_por.domain.repository.ReturnRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.math.BigDecimal
import java.time.LocalDateTime
import javax.inject.Inject

data class ReturnQuery(
    val from: LocalDateTime? = null,
    val to: LocalDateTime? = null,
    val returnType: ReturnType? = null,
    val status: ReturnStatus? = null,
    val partyId: String? = null,
    val processedByEmployeeId: String? = null,
    val pendingOnly: Boolean = false,
    val text: String? = null
)

class GetReturnsUseCase @Inject constructor(
    private val returnRepository: ReturnRepository
) {

    suspend operator fun invoke(query: ReturnQuery = ReturnQuery()): List<Return> {
        val effectiveStatus = when {
            query.pendingOnly && query.status == null -> ReturnStatus.DRAFT
            else -> query.status
        }

        val base = when {
            query.pendingOnly &&
                query.from == null &&
                query.to == null &&
                query.returnType == null &&
                query.status == null &&
                query.partyId == null &&
                query.processedByEmployeeId == null &&
                query.text.isNullOrBlank() -> returnRepository.observePendingReturns().first()

            query.from == null &&
                query.to == null &&
                query.returnType == null &&
                effectiveStatus == null &&
                query.partyId == null &&
                query.processedByEmployeeId == null &&
                query.text.isNullOrBlank() -> returnRepository.observeAllReturns().first()

            else -> returnRepository.getReturns(
                from = query.from,
                to = query.to,
                returnType = query.returnType,
                status = effectiveStatus,
                partyId = query.partyId,
                processedByEmployeeId = query.processedByEmployeeId
            )
        }

        return base.filter { query.text.isNullOrBlank() || matchesText(it, query.text) }
    }

    fun observeAll(): Flow<List<Return>> = returnRepository.observeAllReturns()

    fun observeByInvoice(documentId: String): Flow<List<Return>> {
        require(documentId.isNotBlank()) { "documentId cannot be blank" }
        return returnRepository.observeReturnsByInvoice(documentId)
    }

    fun observeByStatus(status: ReturnStatus): Flow<List<Return>> =
        returnRepository.observeReturnsByStatus(status)

    fun observePending(): Flow<List<Return>> = returnRepository.observePendingReturns()

    suspend fun getById(id: String): Return? {
        require(id.isNotBlank()) { "id cannot be blank" }
        return returnRepository.getReturnById(id)
    }

    suspend fun getByNumber(returnNumber: String): Return? {
        require(returnNumber.isNotBlank()) { "returnNumber cannot be blank" }
        return returnRepository.getReturnByNumber(returnNumber)
    }

    suspend fun search(query: String): List<Return> {
        require(query.isNotBlank()) { "query cannot be blank" }
        return returnRepository.searchReturns(query)
    }

    suspend fun save(returnDocument: Return) = returnRepository.saveReturn(returnDocument)

    suspend fun saveAll(returns: List<Return>) = returnRepository.saveReturns(returns)

    suspend fun update(returnDocument: Return) = returnRepository.updateReturn(returnDocument)

    suspend fun process(returnId: String) = returnRepository.processReturn(returnId)

    suspend fun approve(returnId: String) = returnRepository.approveReturn(returnId)

    suspend fun complete(returnId: String) = returnRepository.completeReturn(returnId)

    suspend fun reject(returnId: String, reason: String? = null) =
        returnRepository.rejectReturn(returnId, reason)

    suspend fun cancel(returnId: String, reason: String? = null) =
        returnRepository.cancelReturn(returnId, reason)

    suspend fun amount(returnId: String): BigDecimal =
        returnRepository.calculateReturnAmount(returnId)

    suspend fun refundedAmount(returnId: String): BigDecimal =
        returnRepository.calculateRefundedAmount(returnId)

    suspend fun refundStatus(returnId: String): RefundStatus =
        returnRepository.getRefundStatus(returnId)

    suspend fun pendingSync(): List<Return> = returnRepository.getReturnsPendingSync()

    suspend fun markSynced(returnId: String) = returnRepository.markReturnSynced(returnId)

    suspend fun countAll(): Long = returnRepository.countReturns()

    suspend fun countByStatus(status: ReturnStatus): Long =
        returnRepository.countReturnsByStatus(status)

    suspend fun countByType(returnType: ReturnType): Long =
        returnRepository.countReturnsByType(returnType)

    suspend fun countPending(): Long = returnRepository.countPendingReturns()

    private fun matchesText(returnDoc: Return, text: String): Boolean {
        val q = text.trim()
        return returnDoc.id.contains(q, ignoreCase = true) ||
            returnDoc.returnNumber.contains(q, ignoreCase = true) ||
            returnDoc.partyName?.contains(q, ignoreCase = true) == true ||
            returnDoc.reason?.contains(q, ignoreCase = true) == true ||
            returnDoc.note?.contains(q, ignoreCase = true) == true
    }
}