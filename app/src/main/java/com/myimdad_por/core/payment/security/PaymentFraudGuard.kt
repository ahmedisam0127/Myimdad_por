package com.myimdad_por.core.payment.security

import android.content.Context
import com.myimdad_por.core.security.DebugDetector
import com.myimdad_por.core.security.RootDetector
import com.myimdad_por.core.security.SessionManager
import com.myimdad_por.core.security.TamperProtection
import com.myimdad_por.core.security.app.hardening.HookDetection

object PaymentFraudGuard {

    private const val CLOCK_SKEW_TOLERANCE_MS = 5 * 60 * 1000L

    @Volatile
    private var appContext: Context? = null

    enum class Signal {
        NO_SESSION,
        ROOT_DETECTED,
        DEBUGGER_DETECTED,
        TAMPER_DETECTED,
        HOOK_DETECTED,
        CLOCK_ROLLED_BACK,
        NO_VAULT_DATA,
        EXPIRED_TOKEN,
        PRODUCT_MISMATCH,
        TOKEN_HASH_MISSING
    }

    data class SecurityStatus(
        val allowed: Boolean,
        val signals: List<Signal>
    ) {
        val riskScore: Int get() = signals.size
    }

    fun init(context: Context) {
        appContext = context.applicationContext
        PaymentTokenVault.init(context)
    }

    /**
     * فحص موحّد قبل أي عملية شراء أو تحقق أو Refund.
     */
    fun inspect(
        context: Context = requireContext(),
        expectedProductId: String? = null
    ): SecurityStatus {
        init(context)

        val signals = linkedSetOf<Signal>()

        if (!SessionManager.hasValidSession()) {
            signals += Signal.NO_SESSION
        }

        if (RootDetector.isSystemCompromised(context)) {
            signals += Signal.ROOT_DETECTED
        }

        if (DebugDetector.shouldRestrictSensitiveOperations(context)) {
            signals += Signal.DEBUGGER_DETECTED
        }

        if (TamperProtection.shouldRestrictSensitiveOperations(context)) {
            signals += Signal.TAMPER_DETECTED
        }

        if (HookDetection.shouldRestrictSensitiveOperations()) {
            signals += Signal.HOOK_DETECTED
        }

        if (isClockRolledBack()) {
            signals += Signal.CLOCK_ROLLED_BACK
        }

        if (!PaymentTokenVault.hasVaultData()) {
            signals += Signal.NO_VAULT_DATA
        }

        val record = PaymentTokenVault.getStoredRecord()
        if (record == null) {
            signals += Signal.NO_VAULT_DATA
        } else {
            if (record.isExpired) {
                signals += Signal.EXPIRED_TOKEN
            }

            if (!expectedProductId.isNullOrBlank() &&
                record.productId != expectedProductId.trim()
            ) {
                signals += Signal.PRODUCT_MISMATCH
            }

            if (PaymentTokenVault.getTokenHash().isNullOrBlank()) {
                signals += Signal.TOKEN_HASH_MISSING
            }
        }

        return SecurityStatus(
            allowed = signals.isEmpty(),
            signals = signals.toList()
        )
    }

    fun canProceed(
        context: Context = requireContext(),
        expectedProductId: String? = null
    ): Boolean {
        return inspect(context, expectedProductId).allowed
    }

    fun shouldBlock(
        context: Context = requireContext(),
        expectedProductId: String? = null
    ): Boolean {
        return !canProceed(context, expectedProductId)
    }

    fun getRiskReasons(
        context: Context = requireContext(),
        expectedProductId: String? = null
    ): List<String> {
        return inspect(context, expectedProductId).signals.map { it.name }
    }

    fun getRiskLevel(
        context: Context = requireContext(),
        expectedProductId: String? = null
    ): Int {
        return inspect(context, expectedProductId).riskScore
    }

    /**
     * فحص بسيط لرجوع الوقت للخلف اعتمادًا على آخر وقت محفوظ محليًا.
     */
    fun isClockRolledBack(): Boolean {
        val now = System.currentTimeMillis()
        val lastLocal = PaymentTokenVault.getStoredRecord()?.serverTimeMillis
            ?: 0L

        if (lastLocal <= 0L) return false
        return now + CLOCK_SKEW_TOLERANCE_MS < lastLocal
    }

    /**
     * تحقق سريع من أن البيانات المخزنة متسقة مع المنتج المطلوب.
     */
    fun isTokenConsistent(expectedProductId: String): Boolean {
        require(expectedProductId.isNotBlank()) { "expectedProductId must not be blank." }
        val record = PaymentTokenVault.getStoredRecord() ?: return false
        return record.productId == expectedProductId.trim() &&
            record.purchaseToken.isNotBlank() &&
            !record.isExpired
    }

    /**
     * يفرغ حالة الدفع إذا أصبحت البيئة غير موثوقة.
     */
    fun purgeIfCompromised(
        context: Context = requireContext(),
        expectedProductId: String? = null
    ): Boolean {
        val status = inspect(context, expectedProductId)
        if (!status.allowed) {
            PaymentTokenVault.clear()
            return true
        }
        return false
    }

    private fun requireContext(): Context {
        return appContext ?: throw IllegalStateException(
            "PaymentFraudGuard is not initialized. Call init(context) first."
        )
    }
}