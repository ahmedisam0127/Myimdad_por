package com.myimdad_por.data.remote.interceptor

import com.myimdad_por.core.payment.crypto.PaymentKeyProvider
import com.myimdad_por.core.security.SessionManager
import com.myimdad_por.core.security.crypto.HmacSigner
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import okio.Buffer
import java.nio.charset.Charset
import java.security.MessageDigest
import java.util.UUID

/**
 * PaymentAuthInterceptor
 *
 * طبقة حماية خاصة بطلبات الدفع:
 * - تضيف هوية الجلسة إن كانت موجودة
 * - تضيف توقيع HMAC للطلبات الحساسة
 * - تضيف timestamp + nonce لمنع إعادة الإرسال
 * - تحفظ التوافق مع GET/DELETE/POST/PUT/PATCH
 *
 * التصميم هنا يفترض أن الخادم يتحقق من:
 * - X-Payment-Timestamp
 * - X-Payment-Nonce
 * - X-Payment-Signature
 * - X-Payment-Key-Id
 * - Authorization
 */
class PaymentAuthInterceptor(
    private val clock: () -> Long = { System.currentTimeMillis() },
    private val nonceProvider: () -> String = { UUID.randomUUID().toString() },
    private val payloadCharset: Charset = Charsets.UTF_8,
    private val enableBodySigning: Boolean = true
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val timestamp = clock()
        val nonce = nonceProvider().trim()

        val signedRequest = original.newBuilder()
            .applyAuthorization()
            .applyPaymentHeaders(timestamp, nonce)
            .applySignatureIfNeeded(original, timestamp, nonce)
            .build()

        return chain.proceed(signedRequest)
    }

    private fun Request.Builder.applyAuthorization(): Request.Builder {
        val token = runCatching { SessionManager.getAuthToken() }.getOrNull()
        if (!token.isNullOrBlank()) {
            header("Authorization", "Bearer $token")
        }
        return this
    }

    private fun Request.Builder.applyPaymentHeaders(
        timestamp: Long,
        nonce: String
    ): Request.Builder {
        require(nonce.isNotBlank()) { "nonce must not be blank." }
        header("X-Payment-Timestamp", timestamp.toString())
        header("X-Payment-Nonce", nonce)
        header("X-Payment-Key-Id", "payment-hmac-v1")
        header("X-Payment-Client", "myimdad-por-android")
        return this
    }

    private fun Request.Builder.applySignatureIfNeeded(
        original: Request,
        timestamp: Long,
        nonce: String
    ): Request.Builder {
        if (!enableBodySigning || !shouldSign(original)) return this

        val canonical = buildCanonicalPayload(original, timestamp, nonce)
        val key = PaymentKeyProvider.getHmacKey()
        val signature = HmacSigner.signStringToBase64(canonical, key)

        header("X-Payment-Signature", signature)
        header("X-Payment-Signature-Alg", "HmacSHA256")
        return this
    }

    private fun shouldSign(request: Request): Boolean {
        val method = request.method.uppercase()
        return method in setOf("POST", "PUT", "PATCH", "DELETE")
    }

    private fun buildCanonicalPayload(
        request: Request,
        timestamp: Long,
        nonce: String
    ): String {
        val method = request.method.uppercase()
        val path = request.url.encodedPath
        val query = request.url.encodedQuery?.takeIf { it.isNotBlank() } ?: ""
        val body = request.body.toCanonicalBodyString(payloadCharset)

        return buildString {
            append(method).append('\n')
            append(path).append('\n')
            append(query).append('\n')
            append(timestamp).append('\n')
            append(nonce).append('\n')
            append(body)
        }
    }

    private fun RequestBody?.toCanonicalBodyString(charset: Charset): String {
        if (this == null) return ""

        return runCatching {
            val buffer = Buffer()
            writeTo(buffer)
            buffer.readString(charset).trim()
        }.getOrDefault("")
    }
}