package com.myimdad_por.core.security

import android.content.Context
import com.myimdad_por.core.utils.Constants

object SessionManager {

    private const val SESSION_ENCRYPTION_ALIAS = "imdad_por_session_aes_key"
    private const val SESSION_HMAC_ALIAS = "imdad_por_session_hmac_key"

    private const val KEY_ACCESS_TOKEN = "session_access_token"
    private const val KEY_REFRESH_TOKEN = "session_refresh_token"
    private const val KEY_USER_ID = "session_user_id"
    private const val KEY_LAST_LOGIN_ENV_SECURE = "session_last_login_env_secure"
    private const val KEY_LAST_LOGIN_AT = "session_last_login_at"
    private const val KEY_LAST_LOGIN_REASON = "session_last_login_reason"

    @Volatile
    private var appContext: Context? = null

    /**
     * يجب استدعاؤها مرة واحدة عند تشغيل التطبيق قبل استخدام الجلسة.
     */
    fun init(context: Context) {
        val appCtx = context.applicationContext
        appContext = appCtx
        SecurePrefs.init(appCtx)
    }

    /**
     * حفظ الجلسة بشكل آمن مع تشفير إضافي للرموز.
     */
    fun saveSession(
        token: String,
        userId: String,
        refreshToken: String? = null
    ) {
        require(token.isNotBlank()) { "token must not be blank." }
        require(userId.isNotBlank()) { "userId must not be blank." }

        val context = requireContext()

        val encryptedAccessToken = CryptoManager.encryptAuthenticatedString(
            plainText = token,
            encryptionAlias = SESSION_ENCRYPTION_ALIAS,
            hmacAlias = SESSION_HMAC_ALIAS
        ).getOrElse { token }

        SecurePrefs.putString(KEY_ACCESS_TOKEN, encryptedAccessToken)
        SecurePrefs.putString(KEY_USER_ID, userId)

        if (!refreshToken.isNullOrBlank()) {
            val encryptedRefreshToken = CryptoManager.encryptAuthenticatedString(
                plainText = refreshToken,
                encryptionAlias = SESSION_ENCRYPTION_ALIAS,
                hmacAlias = SESSION_HMAC_ALIAS
            ).getOrElse { refreshToken }

            SecurePrefs.putString(KEY_REFRESH_TOKEN, encryptedRefreshToken)
        } else {
            SecurePrefs.remove(KEY_REFRESH_TOKEN)
        }

        val secureEnv = isTrustedEnvironment(context)
        SecurePrefs.putBoolean(KEY_LAST_LOGIN_ENV_SECURE, secureEnv)
        SecurePrefs.putLong(KEY_LAST_LOGIN_AT, System.currentTimeMillis())
        SecurePrefs.putString(
            KEY_LAST_LOGIN_REASON,
            if (secureEnv) "secure" else "compromised"
        )
    }

    /**
     * استرجاع access token فقط إذا كانت بيئة التشغيل ما زالت موثوقة.
     */
    fun getAuthToken(): String? {
        val context = requireContext()

        if (!isSessionAllowed(context)) {
            forceLogout()
            return null
        }

        val encryptedToken = SecurePrefs.getString(KEY_ACCESS_TOKEN).orEmpty()
        if (encryptedToken.isBlank()) return null

        return CryptoManager.decryptAuthenticatedString(
            encodedPayload = encryptedToken,
            encryptionAlias = SESSION_ENCRYPTION_ALIAS,
            hmacAlias = SESSION_HMAC_ALIAS
        ).getOrNull()
    }

    /**
     * استرجاع refresh token بشكل آمن.
     */
    fun getRefreshToken(): String? {
        val context = requireContext()

        if (!isSessionAllowed(context)) {
            forceLogout()
            return null
        }

        val encryptedToken = SecurePrefs.getString(KEY_REFRESH_TOKEN).orEmpty()
        if (encryptedToken.isBlank()) return null

        return CryptoManager.decryptAuthenticatedString(
            encodedPayload = encryptedToken,
            encryptionAlias = SESSION_ENCRYPTION_ALIAS,
            hmacAlias = SESSION_HMAC_ALIAS
        ).getOrNull()
    }

    fun getUserId(): String? {
        if (!isLoggedIn()) return null
        return SecurePrefs.getString(KEY_USER_ID)
    }

    fun isLoggedIn(): Boolean {
        return !SecurePrefs.getString(KEY_ACCESS_TOKEN).isNullOrBlank() &&
            !SecurePrefs.getString(KEY_USER_ID).isNullOrBlank()
    }

    /**
     * الجلسة صالحة فقط إذا كانت البيانات موجودة والبيئة الحالية لم تتغير بشكل مريب.
     */
    fun hasValidSession(): Boolean {
        val context = requireContext()
        return isLoggedIn() && isSessionAllowed(context)
    }

    /**
     * تسجيل خروج آمن مع تنظيف كامل للجلسة ومفاتيحها.
     */
    fun logout() {
        forceLogout()
    }

    fun clearSession() {
        forceLogout()
    }

    /**
     * قرار سريع لاستخدام الجلسة في العمليات الحساسة.
     */
    fun shouldRestrictSensitiveOperations(): Boolean {
        return !isSessionAllowed(requireContext())
    }

    /**
     * سبب المنع الحالي إن وجد.
     */
    fun getSessionRiskReason(): String {
        val context = requireContext()
        return when {
            DebugDetector.shouldRestrictSensitiveOperations(context) -> "debug"
            RootDetector.shouldRestrictSensitiveFeatures(context) -> "root"
            TamperProtection.shouldRestrictSensitiveOperations(context) -> "tamper"
            !SecurePrefs.getBoolean(KEY_LAST_LOGIN_ENV_SECURE, true) -> "unsafe_login_env"
            else -> "none"
        }
    }

    /**
     * يفضَّل استدعاؤه قبل العمليات الحساسة أو عند وصول التطبيق للخلفية.
     */
    fun refreshSessionSecurityState(): Boolean {
        val context = requireContext()
        val allowed = isSessionAllowed(context)
        if (!allowed) {
            forceLogout()
        }
        return allowed
    }

    private fun isSessionAllowed(context: Context): Boolean {
        if (!isLoggedIn()) return false
        if (!SecurePrefs.getBoolean(KEY_LAST_LOGIN_ENV_SECURE, true)) return false
        return isTrustedEnvironment(context)
    }

    private fun isTrustedEnvironment(context: Context): Boolean {
        return !RootDetector.isSystemCompromised(context) &&
            !DebugDetector.shouldRestrictSensitiveOperations(context) &&
            !TamperProtection.shouldRestrictSensitiveOperations(context)
    }

    private fun forceLogout() {
        runCatching {
            SecurePrefs.remove(KEY_ACCESS_TOKEN)
            SecurePrefs.remove(KEY_REFRESH_TOKEN)
            SecurePrefs.remove(KEY_USER_ID)
            SecurePrefs.remove(KEY_LAST_LOGIN_ENV_SECURE)
            SecurePrefs.remove(KEY_LAST_LOGIN_AT)
            SecurePrefs.remove(KEY_LAST_LOGIN_REASON)
            KeyStoreManager.deleteKey(SESSION_ENCRYPTION_ALIAS)
            KeyStoreManager.deleteKey(SESSION_HMAC_ALIAS)
        }
    }

    private fun requireContext(): Context {
        return appContext ?: throw IllegalStateException(
            "SessionManager is not initialized. Call SessionManager.init(context) first."
        )
    }
}