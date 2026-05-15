package com.myimdad_por.core.security.crypto

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.GeneralSecurityException
import java.security.KeyStore
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

/**
 * AES helper backed by Android Keystore.
 *
 * Responsibilities:
 * - Create and store a per-alias AES key inside Android Keystore
 * - Reuse the same key for encryption/decryption
 * - Recreate the key if it is missing or invalid
 */
object KeyStoreCipher {

    private const val ANDROID_KEY_STORE = "AndroidKeyStore"
    private const val KEY_SIZE = 256
    private const val BLOCK_MODE = KeyProperties.BLOCK_MODE_GCM
    private const val PADDING = KeyProperties.ENCRYPTION_PADDING_NONE
    private const val ALGORITHM = KeyProperties.KEY_ALGORITHM_AES

    fun encrypt(plainText: String, alias: String): String {
        require(plainText.isNotEmpty()) { "plainText must not be empty." }
        require(alias.isNotBlank()) { "alias must not be blank." }

        val secretKey = getOrCreateSecretKey(alias)
        return AesCipher.encryptToBase64(plainText, secretKey)
    }

    fun decrypt(encodedCipherText: String, alias: String): String {
        require(encodedCipherText.isNotEmpty()) { "encodedCipherText must not be empty." }
        require(alias.isNotBlank()) { "alias must not be blank." }

        val secretKey = getOrCreateSecretKey(alias)
        return AesCipher.decryptFromBase64(encodedCipherText, secretKey)
    }

    fun encryptBytes(plainText: ByteArray, alias: String): String {
        require(plainText.isNotEmpty()) { "plainText must not be empty." }
        require(alias.isNotBlank()) { "alias must not be blank." }

        val secretKey = getOrCreateSecretKey(alias)
        return AesCipher.encryptBytesToBase64(plainText, secretKey)
    }

    fun decryptBytes(encodedCipherText: String, alias: String): ByteArray {
        require(encodedCipherText.isNotEmpty()) { "encodedCipherText must not be empty." }
        require(alias.isNotBlank()) { "alias must not be blank." }

        val secretKey = getOrCreateSecretKey(alias)
        return AesCipher.decryptBytesFromBase64(encodedCipherText, secretKey)
    }

    fun containsAlias(alias: String): Boolean {
        require(alias.isNotBlank()) { "alias must not be blank." }

        val keyStore = getKeyStore()
        return keyStore.containsAlias(alias)
    }

    fun deleteKey(alias: String) {
        require(alias.isNotBlank()) { "alias must not be blank." }

        val keyStore = getKeyStore()
        if (keyStore.containsAlias(alias)) {
            keyStore.deleteEntry(alias)
        }
    }

    fun getOrCreateSecretKey(alias: String): SecretKey {
        require(alias.isNotBlank()) { "alias must not be blank." }

        return try {
            getSecretKey(alias)
        } catch (_: GeneralSecurityException) {
            deleteKey(alias)
            createSecretKey(alias)
        }
    }

    fun getSecretKey(alias: String): SecretKey {
        require(alias.isNotBlank()) { "alias must not be blank." }

        val keyStore = getKeyStore()
        val entry = keyStore.getEntry(alias, null) as? KeyStore.SecretKeyEntry
            ?: throw GeneralSecurityException("No secret key found for alias: $alias")

        return entry.secretKey
    }

    fun createSecretKey(alias: String): SecretKey {
        require(alias.isNotBlank()) { "alias must not be blank." }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            throw UnsupportedOperationException(
                "Android Keystore AES keys require API 23 or higher."
            )
        }

        val keyGenerator = KeyGenerator.getInstance(ALGORITHM, ANDROID_KEY_STORE)

        val spec = KeyGenParameterSpec.Builder(
            alias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(BLOCK_MODE)
            .setEncryptionPaddings(PADDING)
            .setKeySize(KEY_SIZE)
            .setRandomizedEncryptionRequired(true)
            .build()

        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    private fun getKeyStore(): KeyStore {
        return KeyStore.getInstance(ANDROID_KEY_STORE).apply {
            load(null)
        }
    }
}