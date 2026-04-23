package com.myimdad_por.domain.usecase

import android.content.Context
import com.google.android.play.core.integrity.IntegrityTokenRequest
import com.myimdad_por.core.security.IntegrityChecker
import com.myimdad_por.core.security.TamperProtection
import javax.inject.Inject

data class AppIntegrityCheckResult(
    val operationId: String,
    val userId: String?,
    val challengeNonce: String,
    val tamperStatus: TamperProtection.SecurityStatus,
    val integrityTokenRequest: IntegrityTokenRequest?,
    val riskReasons: List<String>,
    val shouldRestrictSensitiveOperations: Boolean
) {
    val isTrusted: Boolean
        get() = !shouldRestrictSensitiveOperations

    val riskScore: Int
        get() = tamperStatus.riskScore + riskReasons.size
}

class CheckAppIntegrityUseCase @Inject constructor() {

    operator fun invoke(
        context: Context,
        operationId: String,
        userId: String? = null,
        cloudProjectNumber: Long? = null,
        expectedApkSizeBytes: Long? = null,
        expectedDexSizeBytes: Long? = null,
        trustedInstallers: Set<String> = defaultTrustedInstallers
    ): AppIntegrityCheckResult {
        require(operationId.isNotBlank()) { "operationId must not be blank" }

        val challengeNonce = IntegrityChecker.prepareIntegrityChallenge(
            operationId = operationId,
            userId = userId
        )

        val tamperStatus = TamperProtection.inspect(
            context = context,
            expectedApkSizeBytes = expectedApkSizeBytes,
            expectedDexSizeBytes = expectedDexSizeBytes,
            trustedInstallers = trustedInstallers
        )

        val integrityTokenRequest = cloudProjectNumber?.takeIf { it > 0L }?.let {
            IntegrityChecker.buildIntegrityTokenRequest(
                nonce = challengeNonce,
                cloudProjectNumber = it
            )
        }

        val riskReasons = buildList {
            addAll(tamperStatus.signals.map { it.name })
            if (tamperStatus.compromised) {
                add("APP_ENVIRONMENT_COMPROMISED")
            }
            if (cloudProjectNumber == null || cloudProjectNumber <= 0L) {
                add("PLAY_INTEGRITY_NOT_REQUESTED")
            }
        }

        val shouldRestrictSensitiveOperations =
            tamperStatus.compromised || operationId.isBlank()

        return AppIntegrityCheckResult(
            operationId = operationId,
            userId = userId,
            challengeNonce = challengeNonce,
            tamperStatus = tamperStatus,
            integrityTokenRequest = integrityTokenRequest,
            riskReasons = riskReasons,
            shouldRestrictSensitiveOperations = shouldRestrictSensitiveOperations
        )
    }

    private companion object {
        val defaultTrustedInstallers = setOf(
            "com.android.vending",
            "com.amazon.venezia"
        )
    }
}