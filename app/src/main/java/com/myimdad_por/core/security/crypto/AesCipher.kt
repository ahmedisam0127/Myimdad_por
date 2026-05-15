package com.myimdad_por.core.security.crypto

import android.util.Base64
import java.security.GeneralSecurityException
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.IvParameterSpec

/**
 * AES helper using authenticated encryption (GCM).
 *
 * Format:
 * - IV is generated per encryption
 * - Ciphertext includes the GCM authentication tag
 */
object AesCipher {

    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val IV_SIZE_BYTES = 12
    private const val TAG_SIZE_BITS = 128

    data class EncryptedData(
        val iv: ByteArray,
        val cipherText: ByteArray
    ) {
        init {
            require(iv.isNotEmpty()) { "iv must not be empty." }
            require(cipherText.isNotEmpty()) { "cipherText must not be empty." }
        }
    }

    fun encrypt(plainText: ByteArray, key: SecretKey): EncryptedData {
        require(plainText.isNotEmpty()) { "plainText must not be empty." }
        validateKey(key)

        val iv = SecureRandomProvider.nextBytes(IV_SIZE_BYTES)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(TAG_SIZE_BITS, iv))

        val cipherText = cipher.doFinal(plainText)
        return EncryptedData(
            iv = iv.copyOf(),
            cipherText = cipherText
        )
    }

    fun decrypt(encryptedData: EncryptedData, key: SecretKey): ByteArray {
        validateKey(key)
        require(encryptedData.iv.size == IV_SIZE_BYTES) { "Invalid IV size." }

        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(
            Cipher.DECRYPT_MODE,
            key,
            GCMParameterSpec(TAG_SIZE_BITS, encryptedData.iv)
        )

        return cipher.doFinal(encryptedData.cipherText)
    }

    fun encryptToBase64(plainText: String, key: SecretKey): String {
        require(plainText.isNotEmpty()) { "plainText must not be empty." }
        val encrypted = encrypt(plainText.toByteArray(Charsets.UTF_8), key)
        return encodeToBase64(encrypted.iv + encrypted.cipherText)
    }

    fun decryptFromBase64(encoded: String, key: SecretKey): String {
        require(encoded.isNotEmpty()) { "encoded must not be empty." }

        val combined = decodeFromBase64(encoded)
        require(combined.size > IV_SIZE_BYTES) { "Invalid encrypted payload." }

        val iv = combined.copyOfRange(0, IV_SIZE_BYTES)
        val cipherText = combined.copyOfRange(IV_SIZE_BYTES, combined.size)

        val decrypted = decrypt(
            encryptedData = EncryptedData(iv = iv, cipherText = cipherText),
            key = key
        )
        return decrypted.toString(Charsets.UTF_8)
    }

    fun encryptBytesToBase64(plainText: ByteArray, key: SecretKey): String {
        val encrypted = encrypt(plainText, key)
        return encodeToBase64(encrypted.iv + encrypted.cipherText)
    }

    fun decryptBytesFromBase64(encoded: String, key: SecretKey): ByteArray {
        require(encoded.isNotEmpty()) { "encoded must not be empty." }

        val combined = decodeFromBase64(encoded)
        require(combined.size > IV_SIZE_BYTES) { "Invalid encrypted payload." }

        val iv = combined.copyOfRange(0, IV_SIZE_BYTES)
        val cipherText = combined.copyOfRange(IV_SIZE_BYTES, combined.size)

        return decrypt(
            encryptedData = EncryptedData(iv = iv, cipherText = cipherText),
            key = key
        )
    }

    private fun validateKey(key: SecretKey) {
        require(key.algorithm.equals("AES", ignoreCase = true)) {
            "SecretKey algorithm must be AES."
        }
    }

    private fun encodeToBase64(data: ByteArray): String {
        return Base64.encodeToString(data, Base64.NO_WRAP)
    }

    private fun decodeFromBase64(data: String): ByteArray {
        return try {
            Base64.decode(data, Base64.NO_WRAP)
        } catch (e: IllegalArgumentException) {
            throw GeneralSecurityException("Invalid Base64 input.", e)
        }
    }
}