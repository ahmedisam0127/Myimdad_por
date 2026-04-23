package com.myimdad_por.core.security.crypto

import android.util.Base64
import java.security.GeneralSecurityException
import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.SecretKey

/**
 * HMAC signer for integrity and authenticity checks.
 *
 * Default algorithm: HmacSHA256
 */
object HmacSigner {

    private const val HMAC_ALGORITHM = "HmacSHA256"

    fun sign(data: ByteArray, key: SecretKey): ByteArray {
        require(data.isNotEmpty()) { "data must not be empty." }
        validateKey(key)

        return try {
            val mac = Mac.getInstance(HMAC_ALGORITHM)
            mac.init(key)
            mac.doFinal(data)
        } catch (e: Exception) {
            throw GeneralSecurityException("Failed to create HMAC signature.", e)
        }
    }

    fun signToBase64(data: ByteArray, key: SecretKey): String {
        return Base64.encodeToString(sign(data, key), Base64.NO_WRAP)
    }

    fun signStringToBase64(data: String, key: SecretKey): String {
        require(data.isNotEmpty()) { "data must not be empty." }
        return signToBase64(data.toByteArray(Charsets.UTF_8), key)
    }

    fun verify(data: ByteArray, signature: ByteArray, key: SecretKey): Boolean {
        require(data.isNotEmpty()) { "data must not be empty." }
        require(signature.isNotEmpty()) { "signature must not be empty." }
        validateKey(key)

        val expected = sign(data, key)
        return MessageDigest.isEqual(expected, signature)
    }

    fun verifyBase64(data: ByteArray, signatureBase64: String, key: SecretKey): Boolean {
        require(data.isNotEmpty()) { "data must not be empty." }
        require(signatureBase64.isNotEmpty()) { "signatureBase64 must not be empty." }
        validateKey(key)

        val signature = try {
            Base64.decode(signatureBase64, Base64.NO_WRAP)
        } catch (e: IllegalArgumentException) {
            return false
        }

        return verify(data, signature, key)
    }

    fun verifyStringBase64(data: String, signatureBase64: String, key: SecretKey): Boolean {
        require(data.isNotEmpty()) { "data must not be empty." }
        require(signatureBase64.isNotEmpty()) { "signatureBase64 must not be empty." }
        return verifyBase64(data.toByteArray(Charsets.UTF_8), signatureBase64, key)
    }

    private fun validateKey(key: SecretKey) {
        require(key.algorithm.equals("HmacSHA256", ignoreCase = true) || key.algorithm.isNotBlank()) {
            "SecretKey must be valid."
        }
    }
}