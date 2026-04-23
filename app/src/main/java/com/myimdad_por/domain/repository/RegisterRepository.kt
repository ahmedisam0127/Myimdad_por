package com.myimdad_por.domain.repository

import java.math.BigDecimal
import java.time.LocalDateTime
import kotlinx.coroutines.flow.Flow

/**
 * Contract for cash drawer / cashier shift management.
 *
 * The register controls:
 * - opening shift
 * - closing shift
 * - cash movements
 * - reconciliation
 */
interface RegisterRepository {

    fun observeCurrentRegister(): Flow<RegisterSession?>

    fun observeRegisterHistory(): Flow<List<RegisterSession>>

    fun observeOpenRegisters(): Flow<List<RegisterSession>>

    suspend fun getCurrentRegister(): RegisterSession?

    suspend fun getRegisterById(registerId: String): RegisterSession?

    suspend fun openRegister(request: OpenRegisterRequest): Result<RegisterSession>

    suspend fun closeRegister(request: CloseRegisterRequest): Result<RegisterSession>

    suspend fun addCash(request: CashMovementRequest): Result<CashMovement>

    suspend fun payout(request: CashMovementRequest): Result<CashMovement>

    suspend fun recordCashMovement(request: CashMovementRequest): Result<CashMovement>

    suspend fun getCashMovements(registerId: String): List<CashMovement>

    suspend fun getCashSummary(registerId: String): CashSummary

    suspend fun reconcile(registerId: String): Result<CashReconciliationResult>

    suspend fun calculateExpectedCash(registerId: String): BigDecimal

    suspend fun calculateActualCash(registerId: String): BigDecimal

    suspend fun calculateDifference(registerId: String): BigDecimal

    suspend fun markRegisterSynced(registerId: String): Result<RegisterSession>

    suspend fun getRegistersPendingSync(): List<RegisterSession>

    suspend fun deleteRegister(registerId: String): Result<Unit>

    suspend fun clearAll(): Result<Unit>

    suspend fun countRegisters(): Long

    suspend fun countOpenRegisters(): Long

    suspend fun countClosedRegisters(): Long
}

/**
 * Cash drawer shift snapshot.
 */
data class RegisterSession(
    val registerId: String,
    val openedByEmployeeId: String,
    val openedAtMillis: Long,
    val openingBalance: BigDecimal,
    val expectedClosingBalance: BigDecimal? = null,
    val actualClosingBalance: BigDecimal? = null,
    val closedByEmployeeId: String? = null,
    val closedAtMillis: Long? = null,
    val status: RegisterStatus = RegisterStatus.OPEN,
    val note: String? = null,
    val metadata: Map<String, String> = emptyMap()
) {
    init {
        require(registerId.isNotBlank()) { "registerId cannot be blank." }
        require(openedByEmployeeId.isNotBlank()) { "openedByEmployeeId cannot be blank." }
        require(openedAtMillis > 0L) { "openedAtMillis must be greater than zero." }
        require(openingBalance >= BigDecimal.ZERO) { "openingBalance cannot be negative." }

        expectedClosingBalance?.let {
            require(it >= BigDecimal.ZERO) { "expectedClosingBalance cannot be negative when provided." }
        }
        actualClosingBalance?.let {
            require(it >= BigDecimal.ZERO) { "actualClosingBalance cannot be negative when provided." }
        }
        closedByEmployeeId?.let {
            require(it.isNotBlank()) { "closedByEmployeeId cannot be blank when provided." }
        }
        closedAtMillis?.let {
            require(it > 0L) { "closedAtMillis must be greater than zero when provided." }
        }
        note?.let {
            require(it.isNotBlank()) { "note cannot be blank when provided." }
        }
    }

    val isOpen: Boolean
        get() = status == RegisterStatus.OPEN

    val isClosed: Boolean
        get() = status == RegisterStatus.CLOSED
}

/**
 * Register state.
 */
enum class RegisterStatus {
    OPEN,
    CLOSED,
    RECONCILED,
    SUSPENDED
}

/**
 * Register cash movement type.
 */
enum class CashMovementType {
    ADD,
    PAYOUT,
    ADJUSTMENT,
    OPENING_FLOAT,
    CLOSING_BALANCE
}

/**
 * Request for opening a register.
 */
data class OpenRegisterRequest(
    val employeeId: String,
    val openingBalance: BigDecimal,
    val openedAtMillis: Long = System.currentTimeMillis(),
    val note: String? = null,
    val metadata: Map<String, String> = emptyMap()
) {
    init {
        require(employeeId.isNotBlank()) { "employeeId cannot be blank." }
        require(openingBalance >= BigDecimal.ZERO) { "openingBalance cannot be negative." }
        require(openedAtMillis > 0L) { "openedAtMillis must be greater than zero." }
        note?.let {
            require(it.isNotBlank()) { "note cannot be blank when provided." }
        }
    }
}

/**
 * Request for closing a register.
 */
data class CloseRegisterRequest(
    val employeeId: String,
    val actualClosingBalance: BigDecimal,
    val closedAtMillis: Long = System.currentTimeMillis(),
    val note: String? = null,
    val metadata: Map<String, String> = emptyMap()
) {
    init {
        require(employeeId.isNotBlank()) { "employeeId cannot be blank." }
        require(actualClosingBalance >= BigDecimal.ZERO) { "actualClosingBalance cannot be negative." }
        require(closedAtMillis > 0L) { "closedAtMillis must be greater than zero." }
        note?.let {
            require(it.isNotBlank()) { "note cannot be blank when provided." }
        }
    }
}

/**
 * Request for adding or removing cash.
 */
data class CashMovementRequest(
    val registerId: String,
    val amount: BigDecimal,
    val type: CashMovementType,
    val reason: String,
    val referenceId: String? = null,
    val employeeId: String? = null,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val metadata: Map<String, String> = emptyMap()
) {
    init {
        require(registerId.isNotBlank()) { "registerId cannot be blank." }
        require(amount >= BigDecimal.ZERO) { "amount cannot be negative." }
        require(reason.isNotBlank()) { "reason cannot be blank." }
        require(createdAtMillis > 0L) { "createdAtMillis must be greater than zero." }
        referenceId?.let {
            require(it.isNotBlank()) { "referenceId cannot be blank when provided." }
        }
        employeeId?.let {
            require(it.isNotBlank()) { "employeeId cannot be blank when provided." }
        }
    }
}

/**
 * Individual cash movement record.
 */
data class CashMovement(
    val movementId: String,
    val registerId: String,
    val amount: BigDecimal,
    val type: CashMovementType,
    val reason: String,
    val referenceId: String? = null,
    val employeeId: String? = null,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val metadata: Map<String, String> = emptyMap()
) {
    init {
        require(movementId.isNotBlank()) { "movementId cannot be blank." }
        require(registerId.isNotBlank()) { "registerId cannot be blank." }
        require(amount >= BigDecimal.ZERO) { "amount cannot be negative." }
        require(reason.isNotBlank()) { "reason cannot be blank." }
    }
}

/**
 * Cash summary for shift reconciliation.
 */
data class CashSummary(
    val registerId: String,
    val openingBalance: BigDecimal,
    val salesCash: BigDecimal,
    val cashIn: BigDecimal,
    val cashOut: BigDecimal,
    val expectedClosingBalance: BigDecimal,
    val actualClosingBalance: BigDecimal? = null,
    val difference: BigDecimal? = null
) {
    init {
        require(registerId.isNotBlank()) { "registerId cannot be blank." }
        require(openingBalance >= BigDecimal.ZERO) { "openingBalance cannot be negative." }
        require(salesCash >= BigDecimal.ZERO) { "salesCash cannot be negative." }
        require(cashIn >= BigDecimal.ZERO) { "cashIn cannot be negative." }
        require(cashOut >= BigDecimal.ZERO) { "cashOut cannot be negative." }
        require(expectedClosingBalance >= BigDecimal.ZERO) { "expectedClosingBalance cannot be negative." }
        actualClosingBalance?.let {
            require(it >= BigDecimal.ZERO) { "actualClosingBalance cannot be negative when provided." }
        }
        difference?.let {
            require(it >= BigDecimal.ZERO) { "difference cannot be negative when provided." }
        }
    }
}

/**
 * Reconciliation result after closing the register.
 */
data class CashReconciliationResult(
    val registerId: String,
    val expectedCash: BigDecimal,
    val actualCash: BigDecimal,
    val difference: BigDecimal,
    val reconciledAtMillis: Long = System.currentTimeMillis(),
    val note: String? = null
) {
    init {
        require(registerId.isNotBlank()) { "registerId cannot be blank." }
        require(expectedCash >= BigDecimal.ZERO) { "expectedCash cannot be negative." }
        require(actualCash >= BigDecimal.ZERO) { "actualCash cannot be negative." }
        require(difference >= BigDecimal.ZERO) { "difference cannot be negative." }
        note?.let {
            require(it.isNotBlank()) { "note cannot be blank when provided." }
        }
    }
}