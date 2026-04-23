package com.myimdad_por.core.payment.crypto

import com.myimdad_por.core.security.crypto.HmacSigner
import java.net.URLEncoder
import javax.crypto.SecretKey

/**
 * Signs and validates payment requests with a stable canonical form.
 *
 * The canonical form is deterministic so the same logical request always
 * produces the same signature.
 */
object PaymentSignatureValidator {

    fun signPaymentRequest(params: Map<String, Any>): String {
        require(params.isNotEmpty()) { "params must not be empty." }
        val canonical = canonicalize(params)
        val hmacKey = PaymentKeyProvider.getHmacKey()
        return HmacSigner.signStringToBase64(canonical, hmacKey)
    }

    fun verifyPaymentRequest(
        params: Map<String, Any>,
        signatureBase64: String
    ): Boolean {
        require(params.isNotEmpty()) { "params must not be empty." }
        require(signatureBase64.isNotBlank()) { "signatureBase64 must not be blank." }

        val canonical = canonicalize(params)
        val hmacKey = PaymentKeyProvider.getHmacKey()
        return HmacSigner.verifyStringBase64(canonical, signatureBase64, hmacKey)
    }

    fun signString(value: String): String {
        require(value.isNotBlank()) { "value must not be blank." }
        return HmacSigner.signStringToBase64(value, PaymentKeyProvider.getHmacKey())
    }

    fun verifyString(value: String, signatureBase64: String): Boolean {
        require(value.isNotBlank()) { "value must not be blank." }
        require(signatureBase64.isNotBlank()) { "signatureBase64 must not be blank." }
        return HmacSigner.verifyStringBase64(value, signatureBase64, PaymentKeyProvider.getHmacKey())
    }

    fun signBytes(value: ByteArray): String {
        require(value.isNotEmpty()) { "value must not be empty." }
        return HmacSigner.signToBase64(value, PaymentKeyProvider.getHmacKey())
    }

    fun verifyBytes(value: ByteArray, signatureBase64: String): Boolean {
        require(value.isNotEmpty()) { "value must not be empty." }
        require(signatureBase64.isNotBlank()) { "signatureBase64 must not be blank." }
        return HmacSigner.verifyBase64(value, signatureBase64, PaymentKeyProvider.getHmacKey())
    }

    fun buildSignedEnvelope(params: Map<String, Any>): Pair<String, String> {
        val payload = canonicalize(params)
        val signature = signString(payload)
        return payload to signature
    }

    fun verifySignedEnvelope(payload: String, signatureBase64: String): Boolean {
        require(payload.isNotBlank()) { "payload must not be blank." }
        require(signatureBase64.isNotBlank()) { "signatureBase64 must not be blank." }
        return verifyString(payload, signatureBase64)
    }

    private fun canonicalize(params: Map<String, Any>): String {
        return params.entries
            .sortedBy { it.key }
            .joinToString(separator = "&") { (key, value) ->
                "${escape(key)}=${escape(serialize(value))}"
            }
    }

    private fun serialize(value: Any): String {
        return when (value) {
            is String -> value.trim()
            is Number, is Boolean -> value.toString()
            is Map<*, *> -> value.entries
                .filter { it.key != null }
                .sortedBy { it.key.toString() }
                .joinToString(prefix = "{", postfix = "}") { (k, v) ->
                    "${serialize(k!!)}:${serialize(v ?: "")}"
                }
            is Iterable<*> -> value.joinToString(prefix = "[", postfix = "]") {
                serialize(it ?: "")
            }
            is Array<*> -> value.joinToString(prefix = "[", postfix = "]") {
                serialize(it ?: "")
            }
            else -> value.toString().trim()
        }
    }

    private fun escape(text: String): String {
        return URLEncoder.encode(text, Charsets.UTF_8.name())
    }
}