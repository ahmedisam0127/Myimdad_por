package com.myimdad_por.domain.usecase

import com.myimdad_por.core.security.CryptoManager
import javax.inject.Inject

enum class SensitiveDataPurpose {
    PASSWORD,
    ACCESS_TOKEN,
    REFRESH_TOKEN,
    PHONE_NUMBER,
    BANK_REFERENCE,
    CUSTOMER_SECRET,
    API_KEY,
    PAYMENT_REFERENCE,
    OTHER
}

data class EncryptedSensitiveData(
    val cipherText: String,
    val purpose: SensitiveDataPurpose,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val algorithm: String = "AES-256-GCM + HMAC-SHA256"
)

class EncryptSensitiveDataUseCase @Inject constructor() {

    operator fun invoke(
        plainText: String,
        purpose: SensitiveDataPurpose = SensitiveDataPurpose.OTHER,
        encryptionAlias: String = DEFAULT_ENCRYPTION_ALIAS,
        hmacAlias: String = DEFAULT_HMAC_ALIAS
    ): Result<EncryptedSensitiveData> {
        return runCatching {
            require(plainText.isNotBlank()) { "plainText cannot be blank" }

            val cipherText = CryptoManager
                .encryptAuthenticatedString(
                    plainText = plainText,
                    encryptionAlias = encryptionAlias,
                    hmacAlias = hmacAlias
                )
                .getOrThrow()

            EncryptedSensitiveData(
                cipherText = cipherText,
                purpose = purpose
            )
        }
    }

    fun decrypt(
        encrypted: EncryptedSensitiveData,
        encryptionAlias: String = DEFAULT_ENCRYPTION_ALIAS,
        hmacAlias: String = DEFAULT_HMAC_ALIAS
    ): Result<String> {
        return CryptoManager.decryptAuthenticatedString(
            encodedPayload = encrypted.cipherText,
            encryptionAlias = encryptionAlias,
            hmacAlias = hmacAlias
        )
    }

    fun encryptBytes(
        plainBytes: ByteArray,
        purpose: SensitiveDataPurpose = SensitiveDataPurpose.OTHER,
        encryptionAlias: String = DEFAULT_ENCRYPTION_ALIAS,
        hmacAlias: String = DEFAULT_HMAC_ALIAS
    ): Result<EncryptedSensitiveData> {
        return runCatching {
            require(plainBytes.isNotEmpty()) { "plainBytes cannot be empty" }

            val cipherText = CryptoManager
                .encryptAuthenticated(
                    plainText = plainBytes,
                    encryptionAlias = encryptionAlias,
                    hmacAlias = hmacAlias
                )
                .getOrThrow()
                .let { CryptoManager.toBase64(it).getOrThrow() }

            EncryptedSensitiveData(
                cipherText = cipherText,
                purpose = purpose
            )
        }
    }

    fun decryptBytes(
        encrypted: EncryptedSensitiveData,
        encryptionAlias: String = DEFAULT_ENCRYPTION_ALIAS,
        hmacAlias: String = DEFAULT_HMAC_ALIAS
    ): Result<ByteArray> {
        return runCatching {
            val raw = CryptoManager.fromBase64(encrypted.cipherText).getOrThrow()
            CryptoManager.decryptAuthenticated(
                payload = raw,
                encryptionAlias = encryptionAlias,
                hmacAlias = hmacAlias
            ).getOrThrow()
        }
    }

    fun generateSecureToken(byteCount: Int = 32): String {
        return CryptoManager.generateRandomToken(byteCount)
    }

    fun encryptForStorage(
        value: String,
        purpose: SensitiveDataPurpose,
        encryptionAlias: String = DEFAULT_ENCRYPTION_ALIAS,
        hmacAlias: String = DEFAULT_HMAC_ALIAS
    ): String {
        return invoke(
            plainText = value,
            purpose = purpose,
            encryptionAlias = encryptionAlias,
            hmacAlias = hmacAlias
        ).getOrThrow().cipherText
    }

    fun decryptFromStorage(
        cipherText: String,
        purpose: SensitiveDataPurpose = SensitiveDataPurpose.OTHER,
        encryptionAlias: String = DEFAULT_ENCRYPTION_ALIAS,
        hmacAlias: String = DEFAULT_HMAC_ALIAS
    ): String {
        return decrypt(
            encrypted = EncryptedSensitiveData(
                cipherText = cipherText,
                purpose = purpose
            ),
            encryptionAlias = encryptionAlias,
            hmacAlias = hmacAlias
        ).getOrThrow()
    }

    private companion object {
        const val DEFAULT_ENCRYPTION_ALIAS = "imdad_por_sensitive_aes_key"
        const val DEFAULT_HMAC_ALIAS = "imdad_por_sensitive_hmac_key"
    }
}