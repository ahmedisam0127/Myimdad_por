package com.myimdad_por.core.payment.crypto

import com.myimdad_por.core.security.crypto.AesCipher
import com.myimdad_por.core.security.crypto.HmacSigner
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.EOFException
import java.io.IOException
import java.security.GeneralSecurityException
import javax.crypto.SecretKey

/**
 * Encrypts payment payloads with AES-GCM and protects them with HMAC.
 *
 * Payload format:
 * [version:1][ivLength:4][iv][cipherLength:4][cipherText][signature:32]
 *
 * The signature is computed over:
 * [version][ivLength][iv][cipherLength][cipherText]
 */
object PaymentPayloadEncryptor {

    private const val PAYLOAD_VERSION: Byte = 1
    private const val HMAC_SIGNATURE_BYTES = 32

    fun encrypt(data: String): String {
        require(data.isNotBlank()) { "data must not be blank." }
        return encrypt(data.toByteArray(Charsets.UTF_8))
    }

    fun encrypt(data: ByteArray): String {
        require(data.isNotEmpty()) { "data must not be empty." }

        val aesKey = PaymentKeyProvider.getAesKey()
        val hmacKey = PaymentKeyProvider.getHmacKey()

        val encrypted = AesCipher.encrypt(data, aesKey)
        val body = buildBody(
            version = PAYLOAD_VERSION,
            iv = encrypted.iv,
            cipherText = encrypted.cipherText
        )
        val signature = HmacSigner.sign(body, hmacKey)

        return encodeToBase64(body + signature)
    }

    fun decrypt(encodedPayload: String): String {
        require(encodedPayload.isNotBlank()) { "encodedPayload must not be blank." }
        return decryptBytes(encodedPayload).toString(Charsets.UTF_8)
    }

    fun decryptBytes(encodedPayload: String): ByteArray {
        require(encodedPayload.isNotBlank()) { "encodedPayload must not be blank." }

        val payload = decodeFromBase64(encodedPayload)
        val parsed = parsePayload(payload)

        val hmacKey = PaymentKeyProvider.getHmacKey()
        val body = buildBody(
            version = parsed.version,
            iv = parsed.iv,
            cipherText = parsed.cipherText
        )

        val isValid = HmacSigner.verify(body, parsed.signature, hmacKey)
        if (!isValid) {
            throw GeneralSecurityException("Payment payload integrity check failed.")
        }

        val aesKey = PaymentKeyProvider.getAesKey()
        return AesCipher.decrypt(
            encryptedData = AesCipher.EncryptedData(
                iv = parsed.iv,
                cipherText = parsed.cipherText
            ),
            key = aesKey
        )
    }

    fun encryptAuthenticated(
        plainText: String,
        metadata: Map<String, String> = emptyMap()
    ): String {
        require(plainText.isNotBlank()) { "plainText must not be blank." }
        val normalized = if (metadata.isEmpty()) plainText else {
            val meta = metadata.entries
                .sortedBy { it.key }
                .joinToString(separator = "&") { (k, v) -> "$k=$v" }
            "$plainText|$meta"
        }
        return encrypt(normalized)
    }

    fun decryptAuthenticated(encodedPayload: String): String {
        return decrypt(encodedPayload)
    }

    private fun buildBody(version: Byte, iv: ByteArray, cipherText: ByteArray): ByteArray {
        require(iv.isNotEmpty()) { "iv must not be empty." }
        require(cipherText.isNotEmpty()) { "cipherText must not be empty." }

        val output = ByteArrayOutputStream()
        DataOutputStream(output).use { stream ->
            stream.writeByte(version.toInt())
            stream.writeInt(iv.size)
            stream.write(iv)
            stream.writeInt(cipherText.size)
            stream.write(cipherText)
            stream.flush()
        }
        return output.toByteArray()
    }

    private data class ParsedPayload(
        val version: Byte,
        val iv: ByteArray,
        val cipherText: ByteArray,
        val signature: ByteArray
    )

    private fun parsePayload(payload: ByteArray): ParsedPayload {
        try {
            DataInputStream(ByteArrayInputStream(payload)).use { input ->
                val version = input.readByte()
                if (version != PAYLOAD_VERSION) {
                    throw GeneralSecurityException("Unsupported payment payload version: $version")
                }

                val ivLength = input.readInt()
                require(ivLength > 0) { "Invalid IV length." }

                val iv = ByteArray(ivLength)
                input.readFully(iv)

                val cipherLength = input.readInt()
                require(cipherLength > 0) { "Invalid cipherText length." }

                val cipherText = ByteArray(cipherLength)
                input.readFully(cipherText)

                val signature = input.readBytes()
                if (signature.size != HMAC_SIGNATURE_BYTES) {
                    throw GeneralSecurityException("Invalid payment payload signature.")
                }

                return ParsedPayload(
                    version = version,
                    iv = iv,
                    cipherText = cipherText,
                    signature = signature
                )
            }
        } catch (e: EOFException) {
            throw GeneralSecurityException("Truncated payment payload.", e)
        } catch (e: IOException) {
            throw GeneralSecurityException("Failed to parse payment payload.", e)
        }
    }

    private fun DataInputStream.readBytes(): ByteArray {
        val buffer = ByteArrayOutputStream()
        val temp = ByteArray(DEFAULT_BUFFER_SIZE)
        while (true) {
            val read = read(temp)
            if (read <= 0) break
            buffer.write(temp, 0, read)
        }
        return buffer.toByteArray()
    }

    private fun encodeToBase64(data: ByteArray): String {
        return android.util.Base64.encodeToString(data, android.util.Base64.NO_WRAP)
    }

    private fun decodeFromBase64(data: String): ByteArray {
        return try {
            android.util.Base64.decode(data, android.util.Base64.NO_WRAP)
        } catch (e: IllegalArgumentException) {
            throw GeneralSecurityException("Invalid Base64 input.", e)
        }
    }
}