package com.myimdad_por.domain.usecase

import com.myimdad_por.core.security.SessionManager
import com.myimdad_por.domain.repository.AuthRepository
import javax.inject.Inject

data class LogoutRequest(
    val clearLocalSession: Boolean = true,
    val revokeRemoteSession: Boolean = true
)

data class LogoutResult(
    val wasLoggedIn: Boolean,
    val remoteLogoutSucceeded: Boolean,
    val localSessionCleared: Boolean,
    val executedAtMillis: Long = System.currentTimeMillis(),
    val remoteErrorMessage: String? = null
) {
    val isFullySuccessful: Boolean
        get() = remoteLogoutSucceeded && localSessionCleared
}

class LogoutUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {

    suspend operator fun invoke(request: LogoutRequest = LogoutRequest()): Result<LogoutResult> {
        return runCatching {
            val wasLoggedIn = SessionManager.isLoggedIn()
            var remoteLogoutSucceeded = false
            var remoteErrorMessage: String? = null

            if (request.revokeRemoteSession) {
                runCatching {
                    authRepository.logout().getOrThrow()
                    remoteLogoutSucceeded = true
                }.onFailure { error ->
                    remoteErrorMessage = error.message ?: error::class.java.simpleName
                }
            } else {
                remoteLogoutSucceeded = true
            }

            var localSessionCleared = false
            if (request.clearLocalSession) {
                runCatching {
                    SessionManager.logout()
                    localSessionCleared = true
                }.getOrElse {
                    localSessionCleared = false
                }
            } else {
                localSessionCleared = true
            }

            LogoutResult(
                wasLoggedIn = wasLoggedIn,
                remoteLogoutSucceeded = remoteLogoutSucceeded,
                localSessionCleared = localSessionCleared,
                remoteErrorMessage = remoteErrorMessage
            )
        }
    }
}