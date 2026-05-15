package com.myimdad_por.core.payment.processor

import android.content.Context
import com.myimdad_por.core.payment.contracts.IPaymentGateway
import com.myimdad_por.core.payment.models.PaymentResult
import com.myimdad_por.core.payment.models.RefundRequest
import com.myimdad_por.core.payment.security.PaymentFraudGuard
import com.myimdad_por.core.payment.security.PaymentTokenVault
import com.myimdad_por.core.security.SessionManager
import com.myimdad_por.core.security.TamperProtection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RefundService(
    context: Context,
    private val gateway: IPaymentGateway
) {
    private val appContext = context.applicationContext

    init {
        PaymentTokenVault.init(appContext)
        PaymentFraudGuard.init(appContext)
    }

    suspend fun refund(
        refundRequest: RefundRequest,
        idempotencyKey: String? = null,
        expectedProductId: String? = null
    ): Result<PaymentResult> {
        return withContext(Dispatchers.IO) {
            if (!canIssueRefund(expectedProductId)) {
                return@withContext Result.failure(
                    IllegalStateException(buildSecurityReason(expectedProductId))
                )
            }

            gateway.refund(
                refundRequest = refundRequest,
                idempotencyKey = idempotencyKey
            ).fold(
                onSuccess = { result ->
                    when (result) {
                        is PaymentResult.Success -> {
                            PaymentTokenVault.clear()
                        }

                        is PaymentResult.Failure -> {
                            if (result.errorCode?.contains("refunded", ignoreCase = true) == true) {
                                PaymentTokenVault.clear()
                            }
                        }

                        else -> Unit
                    }
                    Result.success(result)
                },
                onFailure = { error ->
                    Result.failure(error)
                }
            )
        }
    }

    fun canIssueRefund(expectedProductId: String? = null): Boolean {
        return SessionManager.hasValidSession() &&
            PaymentFraudGuard.canProceed(appContext, expectedProductId) &&
            !TamperProtection.shouldRestrictSensitiveOperations(appContext)
    }

    fun getRefundRiskReason(expectedProductId: String? = null): String {
        return buildSecurityReason(expectedProductId)
    }

    fun clearLocalPaymentState() {
        PaymentTokenVault.clear()
    }

    private fun buildSecurityReason(expectedProductId: String?): String {
        val reasons = PaymentFraudGuard.getRiskReasons(appContext, expectedProductId)
        return when {
            reasons.isNotEmpty() -> reasons.joinToString(separator = ",")
            !SessionManager.hasValidSession() -> "no_session"
            TamperProtection.shouldRestrictSensitiveOperations(appContext) -> "tamper"
            else -> "refund_blocked"
        }
    }
}