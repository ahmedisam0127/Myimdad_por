package com.myimdad_por.core.security

import android.util.Base64
import com.myimdad_por.core.security.crypto.AesCipher
import com.myimdad_por.core.security.crypto.HmacSigner
import com.myimdad_por.core.security.crypto.SecureRandomProvider
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.security.GeneralSecurityException
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.CipherOutputStream
import javax.crypto.Mac
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.IvParameterSpec

/**
 * Single entry point for app cryptography.
 */
object CryptoManager {

    private const val DEFAULT_AES_ALIAS = "imdad_por_crypto_aes_key"
    private const val DEFAULT_HMAC_ALIAS = "imdad_por_crypto_hmac_key"

    private const val AES_TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_IV_SIZE_BYTES = 12
    private const val GCM_TAG_SIZE_BITS = 128

    private const val HMAC_ALGORITHM = "HmacSHA256"
    private const val HMAC_SIZE_BYTES = 32

    // تصحيح: تم حذف كلمة const لأن ByteArray يتم إنشاؤه في وقت التشغيل
    private val FILE_MAGIC = "IMD1".toByteArray(Charsets.US_ASCII)
    private const val BUFFER_SIZE = 8 * 1024

    /**
     * Encrypts text and returns Base64 output.
     */
    fun encryptString(
        plainText: String,
        alias: String = DEFAULT_AES_ALIAS
    ): Result<String> = runCatching {
        require(plainText.isNotEmpty()) { "plainText must not be empty." }
        val key = getAesKey(alias)
        AesCipher.encryptToBase64(plainText, key)
    }

    /**
     * Decrypts Base64 text produced by [encryptString].
     */
    fun decryptString(
        encodedCipherText: String,
        alias: String = DEFAULT_AES_ALIAS
    ): Result<String> = runCatching {
        require(encodedCipherText.isNotEmpty()) { "encodedCipherText must not be empty." }
        val key = getAesKey(alias)
        AesCipher.decryptFromBase64(encodedCipherText, key)
    }

    /**
     * Encrypts bytes and returns a Base64 payload.
     */
    fun encryptBytes(
        plainText: ByteArray,
        alias: String = DEFAULT_AES_ALIAS
    ): Result<String> = runCatching {
        require(plainText.isNotEmpty()) { "plainText must not be empty." }
        val key = getAesKey(alias)
        AesCipher.encryptBytesToBase64(plainText, key)
    }

    /**
     * Decrypts Base64 bytes produced by [encryptBytes].
     */
    fun decryptBytes(
        encodedCipherText: String,
        alias: String = DEFAULT_AES_ALIAS
    ): Result<ByteArray> = runCatching {
        require(encodedCipherText.isNotEmpty()) { "encodedCipherText must not be empty." }
        val key = getAesKey(alias)
        AesCipher.decryptBytesFromBase64(encodedCipherText, key)
    }

    /**
     * Authenticated encryption:
     * Base64( MAGIC || IV || CIPHERTEXT || HMAC )
     */
    fun encryptAuthenticatedString(
        plainText: String,
        encryptionAlias: String = DEFAULT_AES_ALIAS,
        hmacAlias: String = DEFAULT_HMAC_ALIAS
    ): Result<String> = runCatching {
        require(plainText.isNotEmpty()) { "plainText must not be empty." }

        // تصحيح: استخراج المصفوفة من Result باستخدام getOrThrow
        val payload = encryptAuthenticated(plainText.toByteArray(Charsets.UTF_8), encryptionAlias, hmacAlias).getOrThrow()
        Base64.encodeToString(payload, Base64.NO_WRAP)
    }

    /**
     * Decrypts payloads created by [encryptAuthenticatedString].
     */
    fun decryptAuthenticatedString(
        encodedPayload: String,
        encryptionAlias: String = DEFAULT_AES_ALIAS,
        hmacAlias: String = DEFAULT_HMAC_ALIAS
    ): Result<String> = runCatching {
        require(encodedPayload.isNotEmpty()) { "encodedPayload must not be empty." }

        val payload = decodeBase64(encodedPayload)
        // تصحيح: استخراج المصفوفة من Result قبل تحويلها لنص
        val plainBytes = decryptAuthenticated(payload, encryptionAlias, hmacAlias).getOrThrow()
        plainBytes.toString(Charsets.UTF_8)
    }

    /**
     * Authenticated encryption for raw bytes.
     */
    fun encryptAuthenticated(
        plainText: ByteArray,
        encryptionAlias: String = DEFAULT_AES_ALIAS,
        hmacAlias: String = DEFAULT_HMAC_ALIAS
    ): Result<ByteArray> = runCatching {
        require(plainText.isNotEmpty()) { "plainText must not be empty." }

        val aesKey = getAesKey(encryptionAlias)
        val hmacKey = getHmacKey(hmacAlias)

        val iv = SecureRandomProvider.nextBytes(GCM_IV_SIZE_BYTES)
        val cipher = Cipher.getInstance(AES_TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, aesKey, GCMParameterSpec(GCM_TAG_SIZE_BITS, iv))

        val cipherText = cipher.doFinal(plainText)

        val headerAndCipherText = FILE_MAGIC + iv + cipherText
        val mac = macFor(hmacKey)
        mac.update(headerAndCipherText)

        headerAndCipherText + mac.doFinal()
    }

    /**
     * Decrypts authenticated payloads produced by [encryptAuthenticated].
     */
    fun decryptAuthenticated(
        payload: ByteArray,
        encryptionAlias: String = DEFAULT_AES_ALIAS,
        hmacAlias: String = DEFAULT_HMAC_ALIAS
    ): Result<ByteArray> = runCatching {
        require(payload.size > FILE_MAGIC.size + GCM_IV_SIZE_BYTES + HMAC_SIZE_BYTES) {
            "Invalid payload."
        }

        val aesKey = getAesKey(encryptionAlias)
        val hmacKey = getHmacKey(hmacAlias)

        val expectedMac = payload.copyOfRange(payload.size - HMAC_SIZE_BYTES, payload.size)
        val body = payload.copyOfRange(0, payload.size - HMAC_SIZE_BYTES)

        val mac = macFor(hmacKey)
        mac.update(body)
        val actualMac = mac.doFinal()

        if (!MessageDigest.isEqual(actualMac, expectedMac)) {
            throw GeneralSecurityException("Payload integrity check failed.")
        }

        require(body.size > FILE_MAGIC.size + GCM_IV_SIZE_BYTES) { "Invalid payload body." }

        val magic = body.copyOfRange(0, FILE_MAGIC.size)
        if (!magic.contentEquals(FILE_MAGIC)) {
            throw GeneralSecurityException("Invalid payload header.")
        }

        val ivStart = FILE_MAGIC.size
        val ivEnd = ivStart + GCM_IV_SIZE_BYTES
        val iv = body.copyOfRange(ivStart, ivEnd)
        val cipherText = body.copyOfRange(ivEnd, body.size)

        val cipher = Cipher.getInstance(AES_TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, aesKey, GCMParameterSpec(GCM_TAG_SIZE_BITS, iv))
        cipher.doFinal(cipherText)
    }

    /**
     * Encrypts a file into another file.
     */
    fun encryptFile(
        inputFile: File,
        outputFile: File,
        encryptionAlias: String = DEFAULT_AES_ALIAS,
        hmacAlias: String = DEFAULT_HMAC_ALIAS
    ): Result<Unit> = runCatching {
        require(inputFile.exists() && inputFile.isFile) { "inputFile must exist and be a file." }

        val aesKey = getAesKey(encryptionAlias)
        val hmacKey = getHmacKey(hmacAlias)
        val iv = SecureRandomProvider.nextBytes(GCM_IV_SIZE_BYTES)

        val cipher = Cipher.getInstance(AES_TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, aesKey, GCMParameterSpec(GCM_TAG_SIZE_BITS, iv))

        val mac = macFor(hmacKey)
        mac.update(FILE_MAGIC)
        mac.update(iv)

        outputFile.parentFile?.mkdirs()

        FileInputStream(inputFile).use { input ->
            FileOutputStream(outputFile).use { output ->
                output.write(FILE_MAGIC)
                output.write(iv)

                val buffer = ByteArray(BUFFER_SIZE)
                while (true) {
                    val read = input.read(buffer)
                    if (read <= 0) break

                    val encrypted = cipher.update(buffer, 0, read)
                    if (encrypted != null && encrypted.isNotEmpty()) {
                        mac.update(encrypted)
                        output.write(encrypted)
                    }
                }

                val finalBytes = cipher.doFinal()
                if (finalBytes.isNotEmpty()) {
                    mac.update(finalBytes)
                    output.write(finalBytes)
                }

                output.write(mac.doFinal())
                output.flush()
            }
        }
    }

    /**
     * Decrypts a file produced by [encryptFile].
     */
    fun decryptFile(
        inputFile: File,
        outputFile: File,
        encryptionAlias: String = DEFAULT_AES_ALIAS,
        hmacAlias: String = DEFAULT_HMAC_ALIAS
    ): Result<Unit> = runCatching {
        require(inputFile.exists() && inputFile.isFile) { "inputFile must exist and be a file." }
        require(inputFile.length() > (FILE_MAGIC.size + GCM_IV_SIZE_BYTES + HMAC_SIZE_BYTES).toLong()) {
            "inputFile is too small."
        }

        val aesKey = getAesKey(encryptionAlias)
        val hmacKey = getHmacKey(hmacAlias)

        outputFile.parentFile?.mkdirs()

        val fileSize = inputFile.length()
        val cipherTextStart = (FILE_MAGIC.size + GCM_IV_SIZE_BYTES).toLong()
        val cipherTextEndExclusive = fileSize - HMAC_SIZE_BYTES

        if (cipherTextEndExclusive <= cipherTextStart) {
            throw GeneralSecurityException("Invalid encrypted file.")
        }

        RandomAccessFile(inputFile, "r").use { raf ->
            val magic = ByteArray(FILE_MAGIC.size)
            raf.readFully(magic)
            if (!magic.contentEquals(FILE_MAGIC)) {
                throw GeneralSecurityException("Invalid file header.")
            }

            val iv = ByteArray(GCM_IV_SIZE_BYTES)
            raf.readFully(iv)

            val expectedMac = ByteArray(HMAC_SIZE_BYTES)
            raf.seek(cipherTextEndExclusive)
            raf.readFully(expectedMac)

            val mac = macFor(hmacKey)
            mac.update(FILE_MAGIC)
            mac.update(iv)

            raf.seek(cipherTextStart)
            val cipher = Cipher.getInstance(AES_TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, aesKey, GCMParameterSpec(GCM_TAG_SIZE_BITS, iv))

            FileOutputStream(outputFile).use { output ->
                val buffer = ByteArray(BUFFER_SIZE)
                var remaining = cipherTextEndExclusive - cipherTextStart

                while (remaining > 0) {
                    val toRead = minOf(buffer.size.toLong(), remaining).toInt()
                    val read = raf.read(buffer, 0, toRead)
                    if (read <= 0) break

                    mac.update(buffer, 0, read)

                    val plainChunk = cipher.update(buffer, 0, read)
                    if (plainChunk != null && plainChunk.isNotEmpty()) {
                        output.write(plainChunk)
                    }

                    remaining -= read.toLong()
                }

                val actualMac = mac.doFinal()
                if (!MessageDigest.isEqual(actualMac, expectedMac)) {
                    throw GeneralSecurityException("File integrity check failed.")
                }

                val finalPlain = cipher.doFinal()
                if (finalPlain.isNotEmpty()) {
                    output.write(finalPlain)
                }

                output.flush()
            }
        }
    }

    fun toBase64(bytes: ByteArray): Result<String> = runCatching {
        require(bytes.isNotEmpty()) { "bytes must not be empty." }
        Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    fun fromBase64(encoded: String): Result<ByteArray> = runCatching {
        require(encoded.isNotEmpty()) { "encoded must not be empty." }
        decodeBase64(encoded)
    }

    fun generateRandomToken(byteCount: Int = 32): String {
        require(byteCount > 0) { "byteCount must be greater than 0." }
        return SecureRandomProvider.token(byteCount)
    }

    private fun getAesKey(alias: String): SecretKey {
        require(alias.isNotBlank()) { "alias must not be blank." }
        return KeyStoreManager.getOrCreateAesKey(alias)
    }

    private fun getHmacKey(alias: String): SecretKey {
        require(alias.isNotBlank()) { "alias must not be blank." }
        return KeyStoreManager.getOrCreateHmacKey(alias)
    }

    private fun macFor(key: SecretKey): Mac {
        if (!key.algorithm.equals(HMAC_ALGORITHM, ignoreCase = true)) {
            throw GeneralSecurityException("Invalid HMAC key algorithm.")
        }

        return Mac.getInstance(HMAC_ALGORITHM).apply {
            init(key)
        }
    }

    private fun decodeBase64(encoded: String): ByteArray {
        return try {
            Base64.decode(encoded, Base64.NO_WRAP)
        } catch (e: IllegalArgumentException) {
            throw GeneralSecurityException("Invalid Base64 input.", e)
        }
    }
}
