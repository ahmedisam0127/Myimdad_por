package com.myimdad_por.core.security

import android.content.Context
import org.json.JSONObject

object SubscriptionGuard {

    private const val SUB_ENCRYPTION_ALIAS = "imdad_por_sub_key"

    private const val KEY_SUB_DATA = "sub_data_blob"
    private const val KEY_LAST_SEEN_WALL_CLOCK = "sub_last_seen_wall_clock"
    private const val KEY_LAST_GOOD_CHECK = "sub_last_good_check"

    private const val CLOCK_SKEW_TOLERANCE_MS = 5 * 60 * 1000L

    @Volatile
    private var appContext: Context? = null

    data class SubscriptionState(
        val active: Boolean,
        val plan: String,
        val expiresAtMillis: Long,
        val serverIssuedAtMillis: Long? = null
    ) {
        val isExpired: Boolean
            get() = expiresAtMillis > 0L && System.currentTimeMillis() > expiresAtMillis
    }

    fun init(context: Context) {
        val appCtx = context.applicationContext
        appContext = appCtx
        SecurePrefs.init(appCtx)
    }

    /**
     * الحارس النهائي للوصول إلى الميزات المدفوعة.
     */
    fun isPremiumActive(context: Context): Boolean {
        val currentContext = context.applicationContext
        appContext = currentContext
        SecurePrefs.init(currentContext)

        if (isEnvironmentCompromised(currentContext)) return false
        if (isTimeTampered()) return false

        val blob = SecurePrefs.getString(KEY_SUB_DATA).orEmpty()
        if (blob.isBlank()) return false

        val state = decryptAndParseSubscription(blob) ?: return false
        if (!state.active) return false
        if (state.isExpired) return false

        markTrustedClock()
        return true
    }

    /**
     * حفظ الاشتراك القادم من السيرفر بعد التحقق منه.
     * يُفترض أن السيرفر هو مصدر الحقيقة.
     */
    fun updateSubscription(jsonPayload: String) {
        require(jsonPayload.isNotBlank()) { "jsonPayload must not be blank." }

        val context = requireContext()
        SecurePrefs.init(context)

        val parsed = parseSubscriptionJson(jsonPayload) ?: return
        val encryptedBlob = CryptoManager.encryptAuthenticatedString(
            plainText = jsonPayload,
            encryptionAlias = SUB_ENCRYPTION_ALIAS
        ).getOrNull() ?: return

        SecurePrefs.putString(KEY_SUB_DATA, encryptedBlob)
        SecurePrefs.putLong(KEY_LAST_GOOD_CHECK, System.currentTimeMillis())

        if (parsed.expiresAtMillis > 0L) {
            SecurePrefs.putLong(KEY_LAST_SEEN_WALL_CLOCK, maxOf(
                SecurePrefs.getLong(KEY_LAST_SEEN_WALL_CLOCK, 0L),
                System.currentTimeMillis()
            ))
        }
    }

    fun clearSubscription() {
        runCatching {
            SecurePrefs.remove(KEY_SUB_DATA)
            SecurePrefs.remove(KEY_LAST_SEEN_WALL_CLOCK)
            SecurePrefs.remove(KEY_LAST_GOOD_CHECK)
        }
    }

    fun hasSubscriptionData(): Boolean {
        return !SecurePrefs.getString(KEY_SUB_DATA).isNullOrBlank()
    }

    fun getCachedSubscriptionState(): SubscriptionState? {
        val blob = SecurePrefs.getString(KEY_SUB_DATA).orEmpty()
        if (blob.isBlank()) return null
        return decryptAndParseSubscription(blob)
    }

    fun getAccessRiskReason(context: Context): String {
        return when {
            RootDetector.isSystemCompromised(context) -> "root"
            DebugDetector.shouldRestrictSensitiveOperations(context) -> "debug"
            TamperProtection.shouldRestrictSensitiveOperations(context) -> "tamper"
            isTimeTampered() -> "time_tamper"
            else -> "none"
        }
    }

    fun shouldRestrictPremiumOperations(context: Context): Boolean {
        return !isPremiumActive(context)
    }

    private fun isEnvironmentCompromised(context: Context): Boolean {
        return RootDetector.isSystemCompromised(context) ||
            DebugDetector.shouldRestrictSensitiveOperations(context) ||
            TamperProtection.shouldRestrictSensitiveOperations(context)
    }

    /**
     * يكشف رجوع وقت الجهاز للخلف مقارنة بآخر وقت موثوق تم تسجيله.
     */
    private fun isTimeTampered(): Boolean {
        val now = System.currentTimeMillis()
        val lastSeen = SecurePrefs.getLong(KEY_LAST_SEEN_WALL_CLOCK, 0L)

        if (lastSeen <= 0L) {
            SecurePrefs.putLong(KEY_LAST_SEEN_WALL_CLOCK, now)
            return false
        }

        if (now + CLOCK_SKEW_TOLERANCE_MS < lastSeen) {
            return true
        }

        return false
    }

    private fun markTrustedClock() {
        val now = System.currentTimeMillis()
        val lastSeen = SecurePrefs.getLong(KEY_LAST_SEEN_WALL_CLOCK, 0L)
        if (now > lastSeen) {
            SecurePrefs.putLong(KEY_LAST_SEEN_WALL_CLOCK, now)
        }
        SecurePrefs.putLong(KEY_LAST_GOOD_CHECK, now)
    }

    private fun decryptAndParseSubscription(blob: String): SubscriptionState? {
        val decrypted = CryptoManager.decryptAuthenticatedString(
            encodedPayload = blob,
            encryptionAlias = SUB_ENCRYPTION_ALIAS
        ).getOrNull() ?: return null

        return parseSubscriptionJson(decrypted)
    }

    private fun parseSubscriptionJson(jsonPayload: String): SubscriptionState? {
        return runCatching {
            val json = JSONObject(jsonPayload)

            val active = when {
                json.has("active") -> json.optBoolean("active", false)
                json.has("isActive") -> json.optBoolean("isActive", false)
                else -> false
            }

            val plan = when {
                json.has("plan") -> json.optString("plan", "").trim()
                json.has("subscriptionTier") -> json.optString("subscriptionTier", "").trim()
                else -> ""
            }

            val expiresAtMillis = when {
                json.has("expiresAtMillis") -> json.optLong("expiresAtMillis", 0L)
                json.has("expiresAt") -> json.optLong("expiresAt", 0L)
                json.has("expiryMillis") -> json.optLong("expiryMillis", 0L)
                else -> 0L
            }

            val serverIssuedAtMillis = when {
                json.has("issuedAtMillis") -> json.optLong("issuedAtMillis", 0L).takeIf { it > 0L }
                json.has("serverTimeMillis") -> json.optLong("serverTimeMillis", 0L).takeIf { it > 0L }
                else -> null
            }

            if (plan.isBlank() || expiresAtMillis <= 0L) return null

            SubscriptionState(
                active = active,
                plan = plan,
                expiresAtMillis = expiresAtMillis,
                serverIssuedAtMillis = serverIssuedAtMillis
            )
        }.getOrNull()
    }

    private fun requireContext(): Context {
        return appContext ?: throw IllegalStateException(
            "SubscriptionGuard is not initialized. Call SubscriptionGuard.init(context) first."
        )
    }
}