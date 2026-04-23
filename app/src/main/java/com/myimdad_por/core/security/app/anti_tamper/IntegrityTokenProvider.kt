package com.myimdad_por.core.security.app.anti_tamper

import android.content.Context
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.TaskCompletionSource
import com.google.android.play.core.integrity.IntegrityManager
import com.google.android.play.core.integrity.IntegrityTokenRequest
import com.google.android.play.core.integrity.IntegrityTokenResponse
import com.google.android.play.core.integrity.IntegrityManagerFactory
import com.myimdad_por.core.security.IntegrityChecker
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Google Play Integrity token provider.
 *
 * This class only fetches the token.
 * The token must be verified on your server side.
 */
object IntegrityTokenProvider {

    private const val DEFAULT_MAX_ATTEMPTS = 3
    private const val DEFAULT_INITIAL_BACKOFF_MILLIS = 500L
    private const val DEFAULT_MAX_BACKOFF_MILLIS = 4_000L

    suspend fun fetchIntegrityToken(
        context: Context,
        nonce: String,
        cloudProjectNumber: Long,
        maxAttempts: Int = DEFAULT_MAX_ATTEMPTS,
        initialBackoffMillis: Long = DEFAULT_INITIAL_BACKOFF_MILLIS,
        maxBackoffMillis: Long = DEFAULT_MAX_BACKOFF_MILLIS
    ): Result<String> {
        require(nonce.isNotBlank()) { "nonce must not be blank." }
        require(cloudProjectNumber > 0) { "cloudProjectNumber must be greater than zero." }
        require(maxAttempts > 0) { "maxAttempts must be greater than zero." }
        require(initialBackoffMillis > 0) { "initialBackoffMillis must be greater than zero." }
        require(maxBackoffMillis >= initialBackoffMillis) {
            "maxBackoffMillis must be greater than or equal to initialBackoffMillis."
        }

        return withContext(Dispatchers.IO) {
            var attempt = 0
            var backoff = initialBackoffMillis
            var lastError: Throwable? = null

            while (attempt < maxAttempts) {
                try {
                    val token = requestTokenOnce(context, nonce, cloudProjectNumber)
                    return@withContext Result.success(token)
                } catch (ce: CancellationException) {
                    throw ce
                } catch (t: Throwable) {
                    lastError = t
                    attempt++

                    if (attempt >= maxAttempts || !shouldRetry(t)) {
                        return@withContext Result.failure(t)
                    }

                    delay(backoff)
                    backoff = (backoff * 2).coerceAtMost(maxBackoffMillis)
                }
            }

            Result.failure(lastError ?: IOException("Integrity token request failed."))
        }
    }

    suspend fun fetchIntegrityTokenForOperation(
        context: Context,
        operationId: String,
        cloudProjectNumber: Long,
        userId: String? = null,
        maxAttempts: Int = DEFAULT_MAX_ATTEMPTS
    ): Result<String> {
        require(operationId.isNotBlank()) { "operationId must not be blank." }

        val nonce = IntegrityChecker.prepareIntegrityChallenge(
            operationId = operationId,
            userId = userId
        )

        return fetchIntegrityToken(
            context = context,
            nonce = nonce,
            cloudProjectNumber = cloudProjectNumber,
            maxAttempts = maxAttempts
        )
    }

    private suspend fun requestTokenOnce(
        context: Context,
        nonce: String,
        cloudProjectNumber: Long
    ): String {
        val integrityManager = IntegrityChecker.createIntegrityManager(context)
        val request = IntegrityChecker.buildIntegrityTokenRequest(
            nonce = nonce,
            cloudProjectNumber = cloudProjectNumber
        )

        val response = integrityManager.requestIntegrityToken(request).awaitTask()
        return response.token()
    }

    private suspend fun <T> Task<T>.awaitTask(): T = suspendCancellableCoroutine { cont ->
        addOnSuccessListener { result ->
            if (cont.isActive) cont.resume(result)
        }
        addOnFailureListener { error ->
            if (cont.isActive) cont.resumeWithException(error)
        }
        addOnCanceledListener {
            if (cont.isActive) cont.cancel()
        }
    }

    private fun shouldRetry(throwable: Throwable): Boolean {
        return throwable is IOException ||
            throwable is IllegalStateException ||
            throwable.message?.contains("network", ignoreCase = true) == true ||
            throwable.message?.contains("timeout", ignoreCase = true) == true
    }
}