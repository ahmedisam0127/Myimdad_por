package com.myimdad_por.domain.usecase.auth

import com.myimdad_por.core.security.SessionManager
import javax.inject.Inject

enum class SessionAccessState {
    EMPTY,
    ACTIVE,
    RESTRICTED,
    COMPROMISED
}

data class GetSessionStatusRequest(
    val refreshSecurityState: Boolean = false,
    val includeUserId: Boolean = true
)

data class GetSessionStatusResult(
    val isLoggedIn: Boolean,
    val hasValidSession: Boolean,
    val shouldRestrictSensitiveOperations: Boolean,
    val accessState: SessionAccessState,
    val userId: String? = null,
    val riskReason: String = "none"
) {
    val isUsable: Boolean
        get() = accessState == SessionAccessState.ACTIVE
}

class GetSessionStatusUseCase @Inject constructor() {

    operator fun invoke(
        request: GetSessionStatusRequest = GetSessionStatusRequest()
    ): GetSessionStatusResult {
        val loggedIn = SessionManager.isLoggedIn()

        val validSession = if (request.refreshSecurityState) {
            SessionManager.refreshSessionSecurityState()
        } else {
            SessionManager.hasValidSession()
        }

        val restricted = SessionManager.shouldRestrictSensitiveOperations()
        val riskReason = SessionManager.getSessionRiskReason()

        val accessState = when {
            !loggedIn -> SessionAccessState.EMPTY
            validSession && !restricted && riskReason == "none" -> SessionAccessState.ACTIVE
            riskReason == "debug" || riskReason == "root" || riskReason == "tamper" -> SessionAccessState.COMPROMISED
            else -> SessionAccessState.RESTRICTED
        }

        return GetSessionStatusResult(
            isLoggedIn = loggedIn,
            hasValidSession = validSession,
            shouldRestrictSensitiveOperations = restricted,
            accessState = accessState,
            userId = if (request.includeUserId && loggedIn) SessionManager.getUserId() else null,
            riskReason = riskReason
        )
    }
}