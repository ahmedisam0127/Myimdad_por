package com.myimdad_por.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.myimdad_por.domain.model.CurrencyCode
import com.myimdad_por.domain.model.PaymentMethod
import com.myimdad_por.domain.model.PaymentMethodType
import com.myimdad_por.domain.model.PaymentStatus
import com.myimdad_por.domain.model.PaymentTransaction
import org.json.JSONObject
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.UUID

@Entity(
    tableName = "payment_transactions",
    indices = [
        Index(value = ["server_id"], unique = true),
        Index(value = ["transaction_id"], unique = true),
        Index(value = ["payment_intent_id"]),
        Index(value = ["reference_number"]),
        Index(value = ["payment_method_id"]),
        Index(value = ["status"]),
        Index(value = ["currency_code"]),
        Index(value = ["sync_state"])
    ]
)
data class PaymentTransactionEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "server_id")
    val serverId: String? = null,

    @ColumnInfo(name = "transaction_id")
    val transactionId: String,

    @ColumnInfo(name = "payment_intent_id")
    val paymentIntentId: String? = null,

    @ColumnInfo(name = "reference_number")
    val referenceNumber: String? = null,

    @ColumnInfo(name = "payment_method_id")
    val paymentMethodId: String? = null,

    @ColumnInfo(name = "payment_method_name")
    val paymentMethodName: String? = null,

    @ColumnInfo(name = "payment_method_type")
    val paymentMethodType: String? = null,

    @ColumnInfo(name = "payment_method_requires_reference")
    val paymentMethodRequiresReference: Boolean = false,

    @ColumnInfo(name = "payment_method_extra_fees")
    val paymentMethodExtraFees: String = "0.00",

    @ColumnInfo(name = "payment_method_supported_currencies_json")
    val paymentMethodSupportedCurrenciesJson: String = "[]",

    @ColumnInfo(name = "payment_method_is_active")
    val paymentMethodIsActive: Boolean = true,

    @ColumnInfo(name = "amount")
    val amount: String,

    @ColumnInfo(name = "currency_code")
    val currencyCode: String,

    @ColumnInfo(name = "status")
    val status: String = PaymentStatus.PENDING.name,

    @ColumnInfo(name = "provider_name")
    val providerName: String? = null,

    @ColumnInfo(name = "provider_reference")
    val providerReference: String? = null,

    @ColumnInfo(name = "receipt_number")
    val receiptNumber: String? = null,

    @ColumnInfo(name = "authorized_at_millis")
    val authorizedAtMillis: Long? = null,

    @ColumnInfo(name = "captured_at_millis")
    val capturedAtMillis: Long? = null,

    @ColumnInfo(name = "refunded_at_millis")
    val refundedAtMillis: Long? = null,

    @ColumnInfo(name = "failure_reason")
    val failureReason: String? = null,

    @ColumnInfo(name = "metadata_json")
    val metadataJson: String = "{}",

    @ColumnInfo(name = "sync_state")
    val syncState: String = "PENDING",

    @ColumnInfo(name = "is_deleted")
    val isDeleted: Boolean = false,

    @ColumnInfo(name = "synced_at_millis")
    val syncedAtMillis: Long? = null,

    @ColumnInfo(name = "created_at_millis")
    val createdAtMillis: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at_millis")
    val updatedAtMillis: Long = System.currentTimeMillis()
) {
    init {
        require(id.isNotBlank()) { "id cannot be blank." }
        require(transactionId.isNotBlank()) { "transactionId cannot be blank." }
        require(amount.toBigDecimalOrZero() > BigDecimal.ZERO) { "amount must be greater than zero." }
        require(currencyCode.isNotBlank()) { "currencyCode cannot be blank." }
    }

    companion object {
        fun fromDomain(
            transaction: PaymentTransaction,
            serverId: String? = null,
            syncState: String = "PENDING",
            isDeleted: Boolean = false,
            syncedAtMillis: Long? = null,
            createdAtMillis: Long = System.currentTimeMillis(),
            updatedAtMillis: Long = System.currentTimeMillis()
        ): PaymentTransactionEntity {
            val method = transaction.paymentMethod
            return PaymentTransactionEntity(
                id = transaction.transactionId,
                serverId = serverId,
                transactionId = transaction.transactionId,
                paymentIntentId = transaction.paymentIntentId,
                referenceNumber = transaction.referenceNumber,
                paymentMethodId = method?.id,
                paymentMethodName = method?.name,
                paymentMethodType = method?.type?.name,
                paymentMethodRequiresReference = method?.requiresReference ?: false,
                paymentMethodExtraFees = method?.extraFees?.money() ?: "0.00",
                paymentMethodSupportedCurrenciesJson = method?.supportedCurrencies?.toJsonString() ?: "[]",
                paymentMethodIsActive = method?.isActive ?: true,
                amount = transaction.amount.money(),
                currencyCode = transaction.currency.code,
                status = transaction.status.name,
                providerName = transaction.providerName,
                providerReference = transaction.providerReference,
                receiptNumber = transaction.receiptNumber,
                authorizedAtMillis = transaction.authorizedAtMillis,
                capturedAtMillis = transaction.capturedAtMillis,
                refundedAtMillis = transaction.refundedAtMillis,
                failureReason = transaction.failureReason,
                metadataJson = transaction.metadata.toJsonString(),
                syncState = syncState,
                isDeleted = isDeleted,
                syncedAtMillis = syncedAtMillis,
                createdAtMillis = createdAtMillis,
                updatedAtMillis = updatedAtMillis
            )
        }
    }
}

fun PaymentTransactionEntity.toDomain(): PaymentTransaction {
    return PaymentTransaction(
        transactionId = transactionId,
        paymentIntentId = paymentIntentId,
        referenceNumber = referenceNumber,
        paymentMethod = toPaymentMethodOrNull(),
        amount = amount.toBigDecimalOrZero(),
        currency = runCatching { CurrencyCode.valueOf(currencyCode) }.getOrDefault(CurrencyCode.SDG),
        status = runCatching { PaymentStatus.valueOf(status) }.getOrDefault(PaymentStatus.PENDING),
        providerName = providerName,
        providerReference = providerReference,
        receiptNumber = receiptNumber,
        authorizedAtMillis = authorizedAtMillis,
        capturedAtMillis = capturedAtMillis,
        refundedAtMillis = refundedAtMillis,
        failureReason = failureReason,
        metadata = metadataJson.toStringMap()
    )
}

private fun PaymentTransactionEntity.toPaymentMethodOrNull(): PaymentMethod? {
    val id = paymentMethodId?.trim().orEmpty()
    if (id.isBlank()) return null

    val extraFees = paymentMethodExtraFees.toBigDecimalOrZero()
    val supportedCurrencies = paymentMethodSupportedCurrenciesJson.toCurrencyCodeSet()

    val resolvedType = paymentMethodType?.let {
        runCatching { PaymentMethodType.valueOf(it) }.getOrDefault(PaymentMethodType.OTHER)
    } ?: PaymentMethodType.OTHER

    return when (id) {
        PaymentMethod.CASH.id -> PaymentMethod.CASH
        PaymentMethod.BANK_TRANSFER.id -> PaymentMethod.BANK_TRANSFER
        PaymentMethod.WALLET.id -> PaymentMethod.WALLET
        PaymentMethod.POS.id -> PaymentMethod.POS
        PaymentMethod.CHEQUE.id -> PaymentMethod.CHEQUE
        else -> PaymentMethod.Custom(
            id = id,
            name = paymentMethodName?.takeIf { it.isNotBlank() } ?: id,
            type = resolvedType,
            providerName = providerName,
            requiresReference = paymentMethodRequiresReference,
            extraFees = extraFees,
            supportedCurrencies = supportedCurrencies.ifEmpty { setOf(CurrencyCode.SDG, CurrencyCode.EGP) },
            isActive = paymentMethodIsActive
        )
    }
}

private fun Set<CurrencyCode>.toJsonString(): String {
    return org.json.JSONArray().apply {
        forEach { put(it.name) }
    }.toString()
}

private fun String.toCurrencyCodeSet(): Set<CurrencyCode> {
    if (isBlank()) return emptySet()
    return runCatching {
        val array = org.json.JSONArray(this)
        buildSet {
            for (i in 0 until array.length()) {
                val value = array.optString(i)
                runCatching { add(CurrencyCode.valueOf(value)) }
            }
        }
    }.getOrDefault(emptySet())
}

private fun Map<String, String>.toJsonString(): String {
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

private fun BigDecimal.money(): String = this.setScale(2, RoundingMode.HALF_UP).toPlainString()

private fun String.toBigDecimalOrZero(): BigDecimal {
    return runCatching { BigDecimal(this) }
        .getOrDefault(BigDecimal.ZERO)
        .setScale(2, RoundingMode.HALF_UP)
}