package com.myimdad_por.core.payment.processor

import android.content.Context
import com.myimdad_por.core.payment.contracts.IPaymentGateway
import com.myimdad_por.core.payment.models.PaymentVerification
import com.myimdad_por.core.payment.security.PaymentFraudGuard
import com.myimdad_por.core.payment.security.PaymentTokenVault
import com.myimdad_por.core.security.TamperProtection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PaymentVerificationService(
    context: Context,
    private val gateway: IPaymentGateway
) {
    private val appContext = context.applicationContext

    init {
        PaymentTokenVault.init(appContext)
        PaymentFraudGuard.init(appContext)
    }

    suspend fun verifyTransaction(
        transactionId: String,
        expectedProductId: String? = null
    ): Result<PaymentVerification> {
        require(transactionId.isNotBlank()) { "transactionId must not be blank." }

        return withContext(Dispatchers.IO) {
            if (!isEnvironmentTrusted(expectedProductId)) {
                return@withContext Result.failure(
                    IllegalStateException(buildSecurityReason(expectedProductId))
                )
            }

            gateway.verifyTransaction(transactionId).fold(
                onSuccess = { verification ->
                    if (TamperProtection.shouldRestrictSensitiveOperations(appContext)) {
                        return@withContext Result.failure(
                            IllegalStateException("Payment verification blocked by tamper protection.")
                        )
                    }
                    Result.success(verification)
                },
                onFailure = { error ->
                    Result.failure(error)
                }
            )
        }
    }

    suspend fun verifyAndCache(
        transactionId: String,
        purchaseToken: String,
        productId: String,
        orderId: String? = null,
        expiresAtMillis: Long? = null,
        serverTimeMillis: Long? = null,
        expectedProductId: String? = null
    ): Result<PaymentVerification> {
        require(purchaseToken.isNotBlank()) { "purchaseToken must not be blank." }
        require(productId.isNotBlank()) { "productId must not be blank." }

        return withContext(Dispatchers.IO) {
            verifyTransaction(
                transactionId = transactionId,
                expectedProductId = expectedProductId ?: productId
            ).fold(
                onSuccess = { verification ->
                    PaymentTokenVault.saveVerifiedToken(
                        purchaseToken = purchaseToken,
                        productId = productId,
                        orderId = orderId,
                        expiresAtMillis = expiresAtMillis,
                        serverTimeMillis = serverTimeMillis
                    )
                    Result.success(verification)
                },
                onFailure = { error ->
                    Result.failure(error)
                }
            )
        }
    }

    fun cacheServerVerificationPayload(jsonPayload: String) {
        require(jsonPayload.isNotBlank()) { "jsonPayload must not be blank." }
        if (!PaymentFraudGuard.canProceed(appContext)) {
            PaymentTokenVault.clear()
            throw IllegalStateException("Verification payload rejected by security policy.")
        }
        PaymentTokenVault.storeVerifiedPayment(jsonPayload)
    }

    fun hasTrustedEntitlement(expectedProductId: String? = null): Boolean {
        if (!isEnvironmentTrusted(expectedProductId)) return false

        val record = PaymentTokenVault.getStoredRecord() ?: return false
        if (record.isExpired) return false

        return expectedProductId.isNullOrBlank() || record.productId == expectedProductId.trim()
    }

    fun clearCachedVerification() {
        PaymentTokenVault.clear()
    }

    fun getRiskReason(expectedProductId: String? = null): String {
        return buildSecurityReason(expectedProductId)
    }

    private fun isEnvironmentTrusted(expectedProductId: String?): Boolean {
        return PaymentFraudGuard.canProceed(appContext, expectedProductId) &&
            !TamperProtection.shouldRestrictSensitiveOperations(appContext)
    }

    private fun buildSecurityReason(expectedProductId: String?): String {
        val reasons = PaymentFraudGuard.getRiskReasons(appContext, expectedProductId)
        return when {
            reasons.isNotEmpty() -> reasons.joinToString(separator = ",")
            TamperProtection.shouldRestrictSensitiveOperations(appContext) -> "tamper"
            else -> "verification_blocked"
        }
    }
}