package com.myimdad_por.core.payment.security

import android.content.Context
import com.myimdad_por.core.security.CryptoManager
import com.myimdad_por.core.security.SecurePrefs
import com.myimdad_por.core.utils.HashUtils
import org.json.JSONObject

object PaymentTokenVault {

    private const val VAULT_ENCRYPTION_ALIAS = "imdad_por_payment_token_vault_key"

    private const val KEY_VAULT_BLOB = "payment_token_vault_blob"
    private const val KEY_TOKEN_HASH = "payment_token_hash"
    private const val KEY_PRODUCT_ID = "payment_product_id"
    private const val KEY_ORDER_ID = "payment_order_id"
    private const val KEY_EXPIRES_AT = "payment_expires_at"
    private const val KEY_SERVER_TIME = "payment_server_time"
    private const val KEY_LOCAL_TIME = "payment_local_time"

    @Volatile
    private var appContext: Context? = null

    data class PaymentRecord(
        val productId: String,
        val purchaseToken: String,
        val orderId: String?,
        val expiresAtMillis: Long?,
        val serverTimeMillis: Long?,
        val rawJson: String
    ) {
        val isExpired: Boolean
            get() = expiresAtMillis != null && System.currentTimeMillis() > expiresAtMillis
    }

    fun init(context: Context) {
        val appCtx = context.applicationContext
        appContext = appCtx
        SecurePrefs.init(appCtx)
    }

    /**
     * يحفظ بيانات شراء/اشتراك مؤكدة من السيرفر بشكل مشفّر.
     */
    fun storeVerifiedPayment(jsonPayload: String) {
        require(jsonPayload.isNotBlank()) { "jsonPayload must not be blank." }

        val context = requireContext()
        SecurePrefs.init(context)

        val record = parseRecord(jsonPayload) ?: return
        val encryptedBlob = CryptoManager.encryptAuthenticatedString(
            plainText = jsonPayload,
            encryptionAlias = VAULT_ENCRYPTION_ALIAS
        ).getOrElse { jsonPayload }

        SecurePrefs.putString(KEY_VAULT_BLOB, encryptedBlob)
        SecurePrefs.putString(KEY_PRODUCT_ID, record.productId)
        SecurePrefs.putString(KEY_ORDER_ID, record.orderId)
        SecurePrefs.putString(KEY_TOKEN_HASH, HashUtils.sha256(record.purchaseToken))
        SecurePrefs.putLong(KEY_LOCAL_TIME, System.currentTimeMillis())

        record.expiresAtMillis?.let { SecurePrefs.putLong(KEY_EXPIRES_AT, it) }
        record.serverTimeMillis?.let { SecurePrefs.putLong(KEY_SERVER_TIME, it) }
    }

    /**
     * يحفظ بيانات شراء تم التحقق منها بصيغة مباشرة.
     */
    fun saveVerifiedToken(
        purchaseToken: String,
        productId: String,
        orderId: String? = null,
        expiresAtMillis: Long? = null,
        serverTimeMillis: Long? = null
    ) {
        require(purchaseToken.isNotBlank()) { "purchaseToken must not be blank." }
        require(productId.isNotBlank()) { "productId must not be blank." }

        val json = JSONObject().apply {
            put("purchaseToken", purchaseToken.trim())
            put("productId", productId.trim())
            if (!orderId.isNullOrBlank()) put("orderId", orderId.trim())
            if (expiresAtMillis != null && expiresAtMillis > 0L) put("expiresAtMillis", expiresAtMillis)
            if (serverTimeMillis != null && serverTimeMillis > 0L) put("serverTimeMillis", serverTimeMillis)
        }.toString()

        storeVerifiedPayment(json)
    }

    fun hasVaultData(): Boolean {
        return !SecurePrefs.getString(KEY_VAULT_BLOB).isNullOrBlank()
    }

    fun getStoredRecord(): PaymentRecord? {
        val blob = SecurePrefs.getString(KEY_VAULT_BLOB).orEmpty()
        if (blob.isBlank()) return null

        val decrypted = CryptoManager.decryptAuthenticatedString(
            encodedPayload = blob,
            encryptionAlias = VAULT_ENCRYPTION_ALIAS
        ).getOrElse { blob }

        return parseRecord(decrypted)
    }

    fun getPurchaseToken(productId: String? = null): String? {
        val record = getStoredRecord() ?: return null
        if (productId != null && record.productId != productId.trim()) return null
        return record.purchaseToken
    }

    fun getProductId(): String? {
        return SecurePrefs.getString(KEY_PRODUCT_ID)
    }

    fun getOrderId(): String? {
        return SecurePrefs.getString(KEY_ORDER_ID)
    }

    fun getTokenHash(): String? {
        return SecurePrefs.getString(KEY_TOKEN_HASH)
    }

    fun isTokenExpired(): Boolean {
        return getStoredRecord()?.isExpired ?: true
    }

    fun matchesProduct(productId: String): Boolean {
        require(productId.isNotBlank()) { "productId must not be blank." }
        return SecurePrefs.getString(KEY_PRODUCT_ID) == productId.trim()
    }

    fun clear() {
        SecurePrefs.remove(KEY_VAULT_BLOB)
        SecurePrefs.remove(KEY_TOKEN_HASH)
        SecurePrefs.remove(KEY_PRODUCT_ID)
        SecurePrefs.remove(KEY_ORDER_ID)
        SecurePrefs.remove(KEY_EXPIRES_AT)
        SecurePrefs.remove(KEY_SERVER_TIME)
        SecurePrefs.remove(KEY_LOCAL_TIME)
    }

    fun getRiskReason(): String {
        return when {
            hasVaultData() && isTokenExpired() -> "expired"
            hasVaultData() -> "none"
            else -> "empty"
        }
    }

    private fun parseRecord(jsonPayload: String): PaymentRecord? {
        return runCatching {
            val json = JSONObject(jsonPayload)

            val purchaseToken = json.optString("purchaseToken", "").trim()
            val productId = json.optString("productId", "").trim()
            if (purchaseToken.isBlank() || productId.isBlank()) return null

            val orderId = json.optString("orderId", null)?.takeIf { it.isNotBlank() }

            val expiresAtMillis = when {
                json.has("expiresAtMillis") -> json.optLong("expiresAtMillis", 0L).takeIf { it > 0L }
                json.has("expiryMillis") -> json.optLong("expiryMillis", 0L).takeIf { it > 0L }
                else -> null
            }

            val serverTimeMillis = when {
                json.has("serverTimeMillis") -> json.optLong("serverTimeMillis", 0L).takeIf { it > 0L }
                json.has("issuedAtMillis") -> json.optLong("issuedAtMillis", 0L).takeIf { it > 0L }
                else -> null
            }

            PaymentRecord(
                productId = productId,
                purchaseToken = purchaseToken,
                orderId = orderId,
                expiresAtMillis = expiresAtMillis,
                serverTimeMillis = serverTimeMillis,
                rawJson = jsonPayload
            )
        }.getOrNull()
    }

    private fun requireContext(): Context {
        return appContext ?: throw IllegalStateException(
            "PaymentTokenVault is not initialized. Call init(context) first."
        )
    }
}