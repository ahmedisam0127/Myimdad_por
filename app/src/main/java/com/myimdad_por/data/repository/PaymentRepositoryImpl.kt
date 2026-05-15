package com.myimdad_por.data.repository

import com.myimdad_por.core.dispatchers.AppDispatchers
import com.myimdad_por.core.dispatchers.DefaultAppDispatchers
import com.myimdad_por.data.local.dao.PaymentDao
import com.myimdad_por.data.local.entity.PaymentTransactionEntity
import com.myimdad_por.data.local.entity.toDomain
import com.myimdad_por.domain.model.CurrencyCode
import com.myimdad_por.domain.model.PaymentMethod
import com.myimdad_por.domain.model.PaymentMethodType
import com.myimdad_por.domain.model.PaymentRecord
import com.myimdad_por.domain.model.PaymentStatus
import com.myimdad_por.domain.model.PaymentVerification
import com.myimdad_por.domain.model.VerificationStatus
import com.myimdad_por.domain.repository.PaymentRepository
import com.myimdad_por.domain.repository.PaymentSummary
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class PaymentRepositoryImpl(
    private val paymentDao: PaymentDao,
    private val dispatchers: AppDispatchers = DefaultAppDispatchers
) : PaymentRepository {

    override fun observeAllPayments(): Flow<List<PaymentRecord>> {
        return paymentDao.observeAll()
            .map { entities -> entities.map { it.toDomainRecord() } }
            .flowOn(dispatchers.io)
    }

    override fun observePaymentsByInvoice(invoiceId: String): Flow<List<PaymentRecord>> {
        val normalized = invoiceId.trim()
        if (normalized.isBlank()) return flowOf(emptyList())

        return observeAllPayments().map { payments ->
            payments.filter { it.invoiceId?.trim() == normalized }
        }
    }

    override fun observePaymentsByCustomer(customerId: String): Flow<List<PaymentRecord>> {
        val normalized = customerId.trim()
        if (normalized.isBlank()) return flowOf(emptyList())

        return observeAllPayments().map { payments ->
            payments.filter { it.customerId?.trim() == normalized }
        }
    }

    override fun observePaymentsByStatus(status: PaymentStatus): Flow<List<PaymentRecord>> {
        return paymentDao.observeByStatus(status.name)
            .map { entities -> entities.map { it.toDomainRecord() } }
            .flowOn(dispatchers.io)
    }

    override fun observeRefunds(): Flow<List<PaymentRecord>> {
        return observeAllPayments().map { payments ->
            payments.filter {
                it.status == PaymentStatus.REFUNDED ||
                    it.status == PaymentStatus.PARTIALLY_REFUNDED
            }
        }
    }

    override fun observeUnallocatedPayments(customerId: String?): Flow<List<PaymentRecord>> {
        val normalized = customerId?.trim()?.takeIf { it.isNotBlank() }

        return observeAllPayments().map { payments ->
            payments.filter { payment ->
                payment.invoiceId.isNullOrBlank() &&
                    (normalized == null || payment.customerId?.trim() == normalized)
            }
        }
    }

    override suspend fun getPaymentById(id: String): PaymentRecord? = withContext(dispatchers.io) {
        val normalized = id.trim()
        if (normalized.isBlank()) return@withContext null

        paymentDao.getById(normalized)?.toDomainRecord()
            ?: paymentDao.getByServerId(normalized)?.toDomainRecord()
    }

    override suspend fun getPaymentByTransactionId(transactionId: String): PaymentRecord? =
        withContext(dispatchers.io) {
            val normalized = transactionId.trim()
            if (normalized.isBlank()) return@withContext null

            paymentDao.getByTransactionId(normalized)?.toDomainRecord()
        }

    override suspend fun getPayments(
        from: LocalDateTime?,
        to: LocalDateTime?,
        invoiceId: String?,
        customerId: String?,
        status: PaymentStatus?,
        paymentMethod: PaymentMethod?,
        currency: CurrencyCode?
    ): List<PaymentRecord> = withContext(dispatchers.io) {
        paymentDao.observeAll()
            .first()
            .asSequence()
            .map { it.toDomainRecord() }
            .filter { payment -> from == null || payment.createdAtMillis >= from.toMillis() }
            .filter { payment -> to == null || payment.createdAtMillis <= to.toMillis() }
            .filter { payment -> invoiceId == null || payment.invoiceId?.trim() == invoiceId.trim() }
            .filter { payment -> customerId == null || payment.customerId?.trim() == customerId.trim() }
            .filter { payment -> status == null || payment.status == status }
            .filter { payment -> paymentMethod == null || payment.paymentMethod?.id == paymentMethod.id }
            .filter { payment -> currency == null || payment.currency == currency }
            .toList()
    }

    override suspend fun processPayment(payment: PaymentRecord): Result<PaymentRecord> {
        return withContext(dispatchers.io) {
            runCatching {
                val normalized = payment.normalizeForStorage()
                val processed = when {
                    normalized.status == PaymentStatus.PENDING && normalized.paymentMethod?.isElectronic() == true ->
                        normalized.copy(status = PaymentStatus.PROCESSING)

                    normalized.status == PaymentStatus.PENDING && normalized.paymentMethod?.type == PaymentMethodType.CASH ->
                        normalized.copy(status = PaymentStatus.PAID)

                    else -> normalized
                }

                savePayment(processed).getOrThrow()
            }
        }
    }

    override suspend fun savePayment(payment: PaymentRecord): Result<PaymentRecord> {
        return withContext(dispatchers.io) {
            runCatching {
                val normalized = payment.normalizeForStorage()

                val existing = paymentDao.getById(normalized.recordId)
                    ?: paymentDao.getByTransactionId(normalized.transactionId)
                    ?: paymentDao.getByPaymentIntentId(
                        normalized.metadata[METADATA_PAYMENT_INTENT_ID].orEmpty()
                    )
                    ?: paymentDao.getByReferenceNumber(
                        normalized.metadata[METADATA_REFERENCE_NUMBER].orEmpty()
                    )

                val entity = normalized.toEntity(
                    existing = existing,
                    syncState = existing?.syncState ?: "PENDING",
                    isDeleted = existing?.isDeleted ?: false,
                    syncedAtMillis = existing?.syncedAtMillis,
                    createdAtMillis = existing?.createdAtMillis ?: System.currentTimeMillis(),
                    updatedAtMillis = System.currentTimeMillis()
                )

                paymentDao.insert(entity)
                entity.toDomainRecord()
            }
        }
    }

    override suspend fun savePayments(payments: List<PaymentRecord>): Result<List<PaymentRecord>> {
        return withContext(dispatchers.io) {
            runCatching {
                if (payments.isEmpty()) return@runCatching emptyList()

                val saved = mutableListOf<PaymentRecord>()
                for (payment in payments) {
                    saved += savePayment(payment).getOrThrow()
                }
                saved
            }
        }
    }

    override suspend fun updatePayment(payment: PaymentRecord): Result<PaymentRecord> {
        return savePayment(payment)
    }

    override suspend fun linkPaymentToInvoice(
        paymentId: String,
        invoiceId: String
    ): Result<PaymentRecord> {
        return withContext(dispatchers.io) {
            runCatching {
                val current = getPaymentById(paymentId.trim())
                    ?: error("Payment not found: $paymentId")

                val updatedMetadata = current.metadata.toMutableMap().apply {
                    this[METADATA_INVOICE_ID] = invoiceId.trim()
                }

                savePayment(
                    current.copy(
                        invoiceId = invoiceId.trim().takeIf { it.isNotBlank() },
                        metadata = updatedMetadata
                    )
                ).getOrThrow()
            }
        }
    }

    override suspend fun unlinkPaymentFromInvoice(paymentId: String): Result<PaymentRecord> {
        return withContext(dispatchers.io) {
            runCatching {
                val current = getPaymentById(paymentId.trim())
                    ?: error("Payment not found: $paymentId")

                val updatedMetadata = current.metadata.toMutableMap().apply {
                    remove(METADATA_INVOICE_ID)
                }

                savePayment(
                    current.copy(
                        invoiceId = null,
                        metadata = updatedMetadata
                    )
                ).getOrThrow()
            }
        }
    }

    override suspend fun refundPayment(
        paymentId: String,
        amount: BigDecimal?,
        reason: String?
    ): Result<PaymentRecord> {
        return withContext(dispatchers.io) {
            runCatching {
                val current = getPaymentById(paymentId.trim())
                    ?: error("Payment not found: $paymentId")

                val refundAmount = amount?.takeIf { it > BigDecimal.ZERO } ?: current.amount
                val refundStatus = if (refundAmount >= current.amount) {
                    PaymentStatus.REFUNDED
                } else {
                    PaymentStatus.PARTIALLY_REFUNDED
                }

                val updatedMetadata = current.metadata.toMutableMap().apply {
                    this[METADATA_REFUND_AMOUNT] = refundAmount.moneyText()
                    if (!reason.isNullOrBlank()) {
                        this[METADATA_REFUND_REASON] = reason.trim()
                    }
                }

                savePayment(
                    current.copy(
                        status = refundStatus,
                        note = mergeNotes(current.note, reason),
                        metadata = updatedMetadata
                    )
                ).getOrThrow()
            }
        }
    }

    override suspend fun verifyElectronicPayment(transactionId: String): Result<PaymentVerification> {
        return withContext(dispatchers.io) {
            runCatching {
                val payment = getPaymentByTransactionId(transactionId.trim())
                    ?: error("Payment not found: $transactionId")

                val isVerified = payment.status == PaymentStatus.PAID ||
                    payment.status == PaymentStatus.PARTIALLY_PAID ||
                    payment.status == PaymentStatus.REFUNDED ||
                    payment.status == PaymentStatus.PARTIALLY_REFUNDED

                PaymentVerification(
                    verificationId = UUID.randomUUID().toString(),
                    transactionId = payment.transactionId,
                    authorizationId = payment.providerReference,
                    amount = payment.amount,
                    currency = payment.currency,
                    status = if (isVerified) VerificationStatus.VERIFIED else VerificationStatus.PENDING,
                    verified = isVerified,
                    signatureValid = !payment.providerReference.isNullOrBlank(),
                    amountMatched = true,
                    backendConfirmed = isVerified,
                    gatewayName = payment.providerName,
                    providerReference = payment.providerReference,
                    verifiedAtMillis = System.currentTimeMillis(),
                    reason = if (isVerified) null else "payment is not confirmed yet",
                    metadata = payment.metadata + mapOf(
                        METADATA_PAYMENT_STATUS to payment.status.name
                    )
                )
            }
        }
    }

    override suspend fun recordVerification(
        paymentId: String,
        verification: PaymentVerification
    ): Result<PaymentRecord> {
        return withContext(dispatchers.io) {
            runCatching {
                val current = getPaymentById(paymentId.trim())
                    ?: error("Payment not found: $paymentId")

                val updatedStatus = when {
                    verification.verified && verification.status == VerificationStatus.VERIFIED ->
                        PaymentStatus.PAID

                    verification.status == VerificationStatus.REJECTED ->
                        PaymentStatus.FAILED

                    verification.status == VerificationStatus.FAILED ->
                        PaymentStatus.FAILED

                    else -> current.status
                }

                val updatedMetadata = current.metadata.toMutableMap().apply {
                    this[METADATA_VERIFICATION_ID] = verification.verificationId
                    this[METADATA_VERIFIED] = verification.verified.toString()
                    this[METADATA_VERIFICATION_STATUS] = verification.status.name
                    this[METADATA_VERIFIED_AT] = verification.verifiedAtMillis.toString()
                    verification.reason?.takeIf { it.isNotBlank() }?.let {
                        this[METADATA_VERIFICATION_REASON] = it
                    }
                    verification.gatewayName?.takeIf { it.isNotBlank() }?.let {
                        this[METADATA_GATEWAY_NAME] = it
                    }
                    verification.providerReference?.takeIf { it.isNotBlank() }?.let {
                        this[METADATA_PROVIDER_REFERENCE] = it
                    }
                }

                savePayment(
                    current.copy(
                        status = updatedStatus,
                        providerName = verification.gatewayName ?: current.providerName,
                        providerReference = verification.providerReference ?: current.providerReference,
                        metadata = updatedMetadata
                    )
                ).getOrThrow()
            }
        }
    }

    override suspend fun getPaymentSummary(
        from: LocalDateTime?,
        to: LocalDateTime?,
        currency: CurrencyCode?
    ): PaymentSummary {
        return withContext(dispatchers.io) {
            val payments = getPayments(from = from, to = to, currency = currency)
            val summaryCurrency = currency ?: payments.firstOrNull()?.currency ?: CurrencyCode.SDG

            val totalReceived = payments
                .filter { it.status in RECEIVED_STATUSES }
                .fold(BigDecimal.ZERO) { acc, payment -> acc + payment.amount }
                .money()

            val totalRefunded = payments
                .filter { it.status in REFUNDED_STATUSES }
                .fold(BigDecimal.ZERO) { acc, payment -> acc + payment.amount }
                .money()

            val totalPending = payments
                .filter { it.status in PENDING_STATUSES }
                .fold(BigDecimal.ZERO) { acc, payment -> acc + payment.amount }
                .money()

            PaymentSummary(
                currency = summaryCurrency,
                totalReceived = totalReceived,
                totalRefunded = totalRefunded,
                totalPending = totalPending,
                cashTotal = payments.filter { it.paymentMethod?.type == PaymentMethodType.CASH }
                    .fold(BigDecimal.ZERO) { acc, payment -> acc + payment.amount }
                    .money(),
                bankTotal = payments.filter { it.paymentMethod?.type == PaymentMethodType.BANK_TRANSFER }
                    .fold(BigDecimal.ZERO) { acc, payment -> acc + payment.amount }
                    .money(),
                walletTotal = payments.filter { it.paymentMethod?.type == PaymentMethodType.WALLET }
                    .fold(BigDecimal.ZERO) { acc, payment -> acc + payment.amount }
                    .money(),
                cardTotal = payments.filter { it.paymentMethod?.type == PaymentMethodType.POS }
                    .fold(BigDecimal.ZERO) { acc, payment -> acc + payment.amount }
                    .money(),
                paymentCount = payments.size.toLong(),
                refundedCount = payments.count { it.status in REFUNDED_STATUSES }.toLong(),
                pendingCount = payments.count { it.status in PENDING_STATUSES }.toLong()
            )
        }
    }

    override suspend fun getTotalReceived(
        from: LocalDateTime?,
        to: LocalDateTime?,
        currency: CurrencyCode?
    ): BigDecimal {
        return getPaymentSummary(from = from, to = to, currency = currency).totalReceived
    }

    override suspend fun getUnallocatedPayments(customerId: String): List<PaymentRecord> {
        return getPayments(customerId = customerId)
            .filter { it.invoiceId.isNullOrBlank() }
    }

    override suspend fun getPendingElectronicPayments(): List<PaymentRecord> {
        return getPayments()
            .filter { payment ->
                payment.paymentMethod?.isElectronic() == true &&
                    payment.status in setOf(
                        PaymentStatus.PENDING,
                        PaymentStatus.PROCESSING,
                        PaymentStatus.REQUIRES_ACTION,
                        PaymentStatus.AUTHORIZED
                    )
            }
    }

    override suspend fun deletePayment(id: String): Result<Unit> {
        return withContext(dispatchers.io) {
            runCatching {
                val normalized = id.trim()
                val current = paymentDao.getById(normalized)
                    ?: paymentDao.getByServerId(normalized)
                    ?: error("Payment not found: $id")

                paymentDao.softDelete(
                    id = current.id,
                    updatedAtMillis = System.currentTimeMillis(),
                    syncState = "PENDING"
                )
                Unit
            }
        }
    }

    override suspend fun deletePayments(ids: List<String>): Result<Int> {
        return withContext(dispatchers.io) {
            runCatching {
                var count = 0
                ids.mapNotNull { it.trim().takeIf(String::isNotBlank) }
                    .distinct()
                    .forEach { paymentId ->
                        if (deletePayment(paymentId).isSuccess) count++
                    }
                count
            }
        }
    }

    override suspend fun clearAll(): Result<Unit> {
        return withContext(dispatchers.io) {
            runCatching {
                val allPayments = paymentDao.observeAll().first()
                allPayments.forEach {
                    paymentDao.softDelete(
                        id = it.id,
                        updatedAtMillis = System.currentTimeMillis(),
                        syncState = "PENDING"
                    )
                }
                paymentDao.purgeDeleted()
                Unit
            }
        }
    }

    override suspend fun countPayments(): Long {
        return withContext(dispatchers.io) {
            paymentDao.countActive().toLong()
        }
    }

    override suspend fun countPaymentsByStatus(status: PaymentStatus): Long {
        return withContext(dispatchers.io) {
            paymentDao.observeByStatus(status.name).first().size.toLong()
        }
    }

    override suspend fun countRefundedPayments(): Long {
        return withContext(dispatchers.io) {
            getPayments().count { it.status in REFUNDED_STATUSES }.toLong()
        }
    }

    override suspend fun getPaymentsPendingSync(): List<PaymentRecord> {
        return withContext(dispatchers.io) {
            paymentDao.observePendingSync().first().map { it.toDomainRecord() }
        }
    }

    override suspend fun markPaymentSynced(paymentId: String): Result<PaymentRecord> {
        return withContext(dispatchers.io) {
            runCatching {
                val normalized = paymentId.trim()
                val rows = paymentDao.markSynced(normalized)
                if (rows == 0) {
                    error("Payment not found: $paymentId")
                }
                paymentDao.getById(normalized)?.toDomainRecord()
                    ?: error("Payment not found after sync mark: $paymentId")
            }
        }
    }

    private fun PaymentRecord.normalizeForStorage(): PaymentRecord {
        return copy(
            recordId = recordId.trim().ifBlank { UUID.randomUUID().toString() },
            transactionId = transactionId.trim().ifBlank { "TXN-${System.currentTimeMillis()}" },
            invoiceId = invoiceId?.trim()?.takeIf { it.isNotBlank() },
            customerId = customerId?.trim()?.takeIf { it.isNotBlank() },
            providerName = providerName?.trim()?.takeIf { it.isNotBlank() },
            providerReference = providerReference?.trim()?.takeIf { it.isNotBlank() },
            receiptNumber = receiptNumber?.trim()?.takeIf { it.isNotBlank() },
            note = note?.trim()?.takeIf { it.isNotBlank() }
        )
    }

    private fun PaymentRecord.toEntity(
        existing: PaymentTransactionEntity? = null,
        syncState: String = "PENDING",
        isDeleted: Boolean = false,
        syncedAtMillis: Long? = null,
        createdAtMillis: Long = System.currentTimeMillis(),
        updatedAtMillis: Long = System.currentTimeMillis()
    ): PaymentTransactionEntity {
        val meta = mutableMapOf<String, String>()

        invoiceId?.trim()?.takeIf { it.isNotBlank() }?.let { meta[METADATA_INVOICE_ID] = it }
        customerId?.trim()?.takeIf { it.isNotBlank() }?.let { meta[METADATA_CUSTOMER_ID] = it }
        note?.trim()?.takeIf { it.isNotBlank() }?.let { meta[METADATA_NOTE] = it }

        paymentMethod?.id?.takeIf { it.isNotBlank() }?.let { meta[METADATA_PAYMENT_METHOD_ID] = it }
        paymentMethod?.name?.takeIf { it.isNotBlank() }?.let { meta[METADATA_PAYMENT_METHOD_NAME] = it }
        paymentMethod?.type?.name?.takeIf { it.isNotBlank() }?.let { meta[METADATA_PAYMENT_METHOD_TYPE] = it }
        paymentMethod?.providerName?.takeIf { it.isNotBlank() }?.let { meta[METADATA_PROVIDER_NAME] = it }
        paymentMethod?.requiresReference?.let { meta[METADATA_PAYMENT_METHOD_REQUIRES_REFERENCE] = it.toString() }

        metadata[METADATA_PAYMENT_INTENT_ID]?.takeIf { it.isNotBlank() }?.let {
            meta[METADATA_PAYMENT_INTENT_ID] = it
        }
        metadata[METADATA_REFERENCE_NUMBER]?.takeIf { it.isNotBlank() }?.let {
            meta[METADATA_REFERENCE_NUMBER] = it
        }
        metadata[METADATA_GATEWAY_NAME]?.takeIf { it.isNotBlank() }?.let {
            meta[METADATA_GATEWAY_NAME] = it
        }
        metadata[METADATA_PROVIDER_REFERENCE]?.takeIf { it.isNotBlank() }?.let {
            meta[METADATA_PROVIDER_REFERENCE] = it
        }

        return PaymentTransactionEntity(
            id = recordId,
            serverId = existing?.serverId,
            transactionId = transactionId,
            paymentIntentId = meta[METADATA_PAYMENT_INTENT_ID],
            referenceNumber = meta[METADATA_REFERENCE_NUMBER],
            paymentMethodId = paymentMethod?.id,
            paymentMethodName = paymentMethod?.displayName ?: paymentMethod?.name,
            paymentMethodType = paymentMethod?.type?.name,
            paymentMethodRequiresReference = paymentMethod?.requiresReference ?: false,
            paymentMethodExtraFees = paymentMethod?.extraFees?.moneyText() ?: "0.00",
            paymentMethodSupportedCurrenciesJson = paymentMethod?.supportedCurrencies
                ?.map { it.name }
                .toJsonArrayString(),
            paymentMethodIsActive = paymentMethod?.isActive ?: true,
            amount = amount.moneyText(),
            currencyCode = currency.name,
            status = status.name,
            providerName = providerName,
            providerReference = providerReference,
            receiptNumber = receiptNumber,
            authorizedAtMillis = null,
            capturedAtMillis = null,
            refundedAtMillis = null,
            failureReason = note,
            metadataJson = meta.toJsonObjectString(),
            syncState = syncState,
            isDeleted = isDeleted,
            syncedAtMillis = syncedAtMillis,
            createdAtMillis = existing?.createdAtMillis ?: createdAtMillis,
            updatedAtMillis = updatedAtMillis
        )
    }
private fun PaymentTransactionEntity.toDomainRecord(): PaymentRecord {
    val meta = metadataJson.toStringMap()
    return PaymentRecord(
        recordId = id,
        transactionId = transactionId,
        invoiceId = meta[METADATA_INVOICE_ID]?.takeIf { it.isNotBlank() },
        customerId = meta[METADATA_CUSTOMER_ID]?.takeIf { it.isNotBlank() },
        paymentMethod = toPaymentMethodOrNull(),
        amount = amount.toBigDecimalOrZero(),
        currency = runCatching { CurrencyCode.valueOf(currencyCode) }.getOrDefault(CurrencyCode.SDG),
        status = runCatching { PaymentStatus.valueOf(status) }.getOrDefault(PaymentStatus.PENDING),
        providerName = providerName,
        providerReference = providerReference,
        receiptNumber = receiptNumber,
        note = meta[METADATA_NOTE]?.takeIf { it.isNotBlank() } ?: failureReason,
        createdAtMillis = createdAtMillis,
        updatedAtMillis = updatedAtMillis,
        metadata = meta
            .minus(METADATA_INVOICE_ID)
            .minus(METADATA_CUSTOMER_ID)
            .minus(METADATA_NOTE)
            .minus(METADATA_PAYMENT_METHOD_ID)
            .minus(METADATA_PAYMENT_METHOD_NAME)
            .minus(METADATA_PAYMENT_METHOD_TYPE)
            .minus(METADATA_PROVIDER_NAME)
            .minus(METADATA_PAYMENT_METHOD_REQUIRES_REFERENCE)
            .minus(METADATA_PAYMENT_INTENT_ID)
            .minus(METADATA_REFERENCE_NUMBER)
            .minus(METADATA_GATEWAY_NAME)
            .minus(METADATA_PROVIDER_REFERENCE)
    )
}
    
    private fun PaymentTransactionEntity.toPaymentMethodOrNull(): PaymentMethod? {
        val methodId = paymentMethodId?.trim().orEmpty()
        if (methodId.isBlank()) return null

        val resolvedType = paymentMethodType?.let {
            runCatching { PaymentMethodType.valueOf(it) }.getOrDefault(PaymentMethodType.OTHER)
        } ?: PaymentMethodType.OTHER

        val extraFees = paymentMethodExtraFees.toBigDecimalOrZero()
        val supportedCurrencies = paymentMethodSupportedCurrenciesJson.toCurrencyCodeSet()

        return when (methodId) {
            PaymentMethod.CASH.id -> PaymentMethod.CASH
            PaymentMethod.BANK_TRANSFER.id -> PaymentMethod.BANK_TRANSFER
            PaymentMethod.WALLET.id -> PaymentMethod.WALLET
            PaymentMethod.POS.id -> PaymentMethod.POS
            PaymentMethod.CHEQUE.id -> PaymentMethod.CHEQUE
            else -> PaymentMethod.Custom(
                id = methodId,
                name = paymentMethodName?.takeIf { it.isNotBlank() } ?: methodId,
                type = resolvedType,
                providerName = providerName,
                requiresReference = paymentMethodRequiresReference,
                extraFees = extraFees,
                supportedCurrencies = supportedCurrencies.ifEmpty { setOf(CurrencyCode.SDG, CurrencyCode.EGP) },
                isActive = paymentMethodIsActive
            )
        }
    }

    private fun PaymentMethod.isElectronic(): Boolean {
        return type != PaymentMethodType.CASH
    }

    private fun Map<String, String>.toJsonObjectString(): String {
        return JSONObject(this).toString()
    }

    private fun String.toStringMap(): Map<String, String> {
        if (isBlank()) return emptyMap()
        return runCatching {
            val json = JSONObject(this)
            buildMap {
                json.keys().forEach { key ->
                    put(key, json.optString(key))
                }
            }
        }.getOrDefault(emptyMap())
    }

    private fun Collection<String>?.toJsonArrayString(): String {
        if (this.isNullOrEmpty()) return "[]"
        return JSONArray().apply {
            forEach { put(it) }
        }.toString()
    }

    private fun String.toCurrencyCodeSet(): Set<CurrencyCode> {
        if (isBlank()) return emptySet()
        return runCatching {
            val array = JSONArray(this)
            buildSet {
                for (i in 0 until array.length()) {
                    val value = array.optString(i)
                    runCatching { add(CurrencyCode.valueOf(value)) }
                }
            }
        }.getOrDefault(emptySet())
    }

    private fun BigDecimal.money(): BigDecimal {
        return setScale(2, RoundingMode.HALF_UP)
    }

    private fun BigDecimal.moneyText(): String {
        return setScale(2, RoundingMode.HALF_UP).toPlainString()
    }

    private fun String.toBigDecimalOrZero(): BigDecimal {
        return runCatching { BigDecimal(this) }
            .getOrDefault(BigDecimal.ZERO)
            .setScale(2, RoundingMode.HALF_UP)
    }

    private fun mergeNotes(existingNote: String?, reason: String?): String? {
        val parts = buildList {
            existingNote?.trim()?.takeIf { it.isNotBlank() }?.let { add(it) }
            reason?.trim()?.takeIf { it.isNotBlank() }?.let { add("REFUND REASON: $it") }
        }
        return parts.takeIf { it.isNotEmpty() }?.joinToString(separator = "\n")
    }

    private fun LocalDateTime.toMillis(): Long {
        return atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    private fun BigDecimal.coerceAtLeast(min: BigDecimal): BigDecimal {
        return if (this < min) min else this
    }

    private companion object {
        private const val METADATA_INVOICE_ID = "__invoice_id"
        private const val METADATA_CUSTOMER_ID = "__customer_id"
        private const val METADATA_NOTE = "__note"
        private const val METADATA_PAYMENT_INTENT_ID = "__payment_intent_id"
        private const val METADATA_REFERENCE_NUMBER = "__reference_number"
        private const val METADATA_PAYMENT_METHOD_ID = "__payment_method_id"
        private const val METADATA_PAYMENT_METHOD_NAME = "__payment_method_name"
        private const val METADATA_PAYMENT_METHOD_TYPE = "__payment_method_type"
        private const val METADATA_PAYMENT_METHOD_REQUIRES_REFERENCE = "__payment_method_requires_reference"
        private const val METADATA_PROVIDER_NAME = "__provider_name"
        private const val METADATA_PROVIDER_REFERENCE = "__provider_reference"
        private const val METADATA_VERIFICATION_ID = "__verification_id"
        private const val METADATA_VERIFICATION_STATUS = "__verification_status"
        private const val METADATA_VERIFIED = "__verified"
        private const val METADATA_VERIFIED_AT = "__verified_at_millis"
        private const val METADATA_VERIFICATION_REASON = "__verification_reason"
        private const val METADATA_GATEWAY_NAME = "__gateway_name"
        private const val METADATA_REFUND_AMOUNT = "__refund_amount"
        private const val METADATA_REFUND_REASON = "__refund_reason"
        private const val METADATA_PAYMENT_STATUS = "__payment_status"

        private val RECEIVED_STATUSES = setOf(
            PaymentStatus.PAID,
            PaymentStatus.PARTIALLY_PAID,
            PaymentStatus.AUTHORIZED
        )

        private val REFUNDED_STATUSES = setOf(
            PaymentStatus.REFUNDED,
            PaymentStatus.PARTIALLY_REFUNDED
        )

        private val PENDING_STATUSES = setOf(
            PaymentStatus.PENDING,
            PaymentStatus.PROCESSING,
            PaymentStatus.REQUIRES_ACTION,
            PaymentStatus.AUTHORIZED
        )
    }
}