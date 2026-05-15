package com.myimdad_por.data.remote.interceptor

import com.myimdad_por.core.security.SessionManager
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import java.util.Locale
import java.util.UUID

/**
 * AuthInterceptor
 *
 * يضيف طبقة مصادقة أنيقة ومحصّنة لطلبات الشبكة:
 * - Authorization Bearer
 * - session hints
 * - device / client metadata
 * - optional request tagging for observability
 *
 * الفكرة هنا أن كل طلب يخرج من التطبيق يحمل بصمة متسقة،
 * بينما تظل طبقة الـ repository نظيفة وبعيدة عن تفاصيل الترويسات.
 */
class AuthInterceptor(
    private val clientName: String = "myimdad-por-android",
    private val clientVersion: String = "1.0.0",
    private val deviceIdProvider: () -> String? = { null },
    private val requestIdProvider: () -> String = { UUID.randomUUID().toString() },
    private val includeDebugHeaders: Boolean = false,
    private val publicPaths: Set<String> = emptySet()
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()

        if (isPublicRequest(original)) {
            return chain.proceed(
                original.newBuilder()
                    .applyCommonHeaders()
                    .applyRequestId()
                    .build()
            )
        }

        val token = runCatching { SessionManager.getAuthToken() }.getOrNull()
        val userId = runCatching { SessionManager.getUserId() }.getOrNull()
        val sessionRisk = runCatching { SessionManager.getSessionRiskReason() }.getOrDefault("none")

        val request = original.newBuilder()
            .applyCommonHeaders()
            .applyAuthorization(token)
            .applySessionHeaders(userId, sessionRisk)
            .applyRequestId()
            .applyDebugHeaders(includeDebugHeaders, token, userId, sessionRisk)
            .build()

        return chain.proceed(request)
    }

    private fun isPublicRequest(request: Request): Boolean {
        val path = request.url.encodedPath.lowercase(Locale.ROOT)
        return publicPaths.any { publicPath ->
            path.endsWith(publicPath.lowercase(Locale.ROOT))
        }
    }

    private fun Request.Builder.applyCommonHeaders(): Request.Builder {
        header("Accept", "application/json")
        header("Content-Type", "application/json")
        header("X-Client-Name", clientName)
        header("X-Client-Version", clientVersion)
        header("X-Client-Platform", "android")
        header("X-Client-Locale", Locale.getDefault().toLanguageTag())
        deviceIdProvider()?.takeIf { it.isNotBlank() }?.let { deviceId ->
            header("X-Device-Id", deviceId)
        }
        return this
    }

    private fun Request.Builder.applyAuthorization(token: String?): Request.Builder {
        if (!token.isNullOrBlank()) {
            header("Authorization", "Bearer $token")
        }
        return this
    }

    private fun Request.Builder.applySessionHeaders(
        userId: String?,
        sessionRisk: String
    ): Request.Builder {
        userId?.takeIf { it.isNotBlank() }?.let { id ->
            header("X-User-Id", id)
        }
        header("X-Session-Risk", sessionRisk)
        return this
    }

    private fun Request.Builder.applyRequestId(): Request.Builder {
        header("X-Request-Id", requestIdProvider())
        return this
    }

    private fun Request.Builder.applyDebugHeaders(
        enabled: Boolean,
        token: String?,
        userId: String?,
        sessionRisk: String
    ): Request.Builder {
        if (!enabled) return this

        header("X-Debug-Auth-Present", (!token.isNullOrBlank()).toString())
        header("X-Debug-User-Present", (!userId.isNullOrBlank()).toString())
        header("X-Debug-Session-Risk", sessionRisk)
        return this
    }
}