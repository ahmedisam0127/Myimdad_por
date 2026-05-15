package com.myimdad_por.core.security

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.GeneralSecurityException
import java.security.KeyStore
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

/**
 * Centralized access to Android Keystore keys.
 *
 * Keeps key management in one place so crypto helpers can stay focused on
 * encryption, decryption, signing, and verification.
 */
object KeyStoreManager {

    private const val ANDROID_KEY_STORE = "AndroidKeyStore"

    private const val AES_KEY_ALGORITHM = KeyProperties.KEY_ALGORITHM_AES
    private const val HMAC_KEY_ALGORITHM = KeyProperties.KEY_ALGORITHM_HMAC_SHA256

    private const val AES_KEY_SIZE = 256
    private const val HMAC_KEY_SIZE = 256

    private const val AES_BLOCK_MODE = KeyProperties.BLOCK_MODE_GCM
    private const val AES_PADDING = KeyProperties.ENCRYPTION_PADDING_NONE
    private const val HMAC_DIGEST = KeyProperties.DIGEST_SHA256

    @Synchronized
    fun containsAlias(alias: String): Boolean {
        require(alias.isNotBlank()) { "alias must not be blank." }
        return keyStore().containsAlias(alias)
    }

    @Synchronized
    fun deleteKey(alias: String) {
        require(alias.isNotBlank()) { "alias must not be blank." }

        val store = keyStore()
        if (store.containsAlias(alias)) {
            store.deleteEntry(alias)
        }
    }

    @Synchronized
    fun getSecretKey(alias: String): SecretKey {
        require(alias.isNotBlank()) { "alias must not be blank." }

        val entry = keyStore().getEntry(alias, null) as? KeyStore.SecretKeyEntry
            ?: throw GeneralSecurityException("No secret key found for alias: $alias")

        return entry.secretKey
    }

    @Synchronized
    fun getOrCreateAesKey(alias: String, keySize: Int = AES_KEY_SIZE): SecretKey {
        require(alias.isNotBlank()) { "alias must not be blank." }
        require(keySize > 0) { "keySize must be greater than 0." }

        return try {
            getSecretKey(alias)
        } catch (_: Exception) {
            deleteKey(alias)
            createAesKey(alias, keySize)
        }
    }

    @Synchronized
    fun getOrCreateHmacKey(alias: String, keySize: Int = HMAC_KEY_SIZE): SecretKey {
        require(alias.isNotBlank()) { "alias must not be blank." }
        require(keySize > 0) { "keySize must be greater than 0." }

        return try {
            getSecretKey(alias)
        } catch (_: Exception) {
            deleteKey(alias)
            createHmacKey(alias, keySize)
        }
    }

    @Synchronized
    fun createAesKey(alias: String, keySize: Int = AES_KEY_SIZE): SecretKey {
        require(alias.isNotBlank()) { "alias must not be blank." }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            throw UnsupportedOperationException(
                "Android Keystore AES keys require API 23 or higher."
            )
        }

        val generator = KeyGenerator.getInstance(AES_KEY_ALGORITHM, ANDROID_KEY_STORE)
        val spec = KeyGenParameterSpec.Builder(
            alias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setKeySize(keySize)
            .setBlockModes(AES_BLOCK_MODE)
            .setEncryptionPaddings(AES_PADDING)
            .setRandomizedEncryptionRequired(true)
            .build()

        generator.init(spec)
        return generator.generateKey()
    }

    @Synchronized
    fun createHmacKey(alias: String, keySize: Int = HMAC_KEY_SIZE): SecretKey {
        require(alias.isNotBlank()) { "alias must not be blank." }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            throw UnsupportedOperationException(
                "Android Keystore HMAC keys require API 23 or higher."
            )
        }

        val generator = KeyGenerator.getInstance(HMAC_KEY_ALGORITHM, ANDROID_KEY_STORE)
        val spec = KeyGenParameterSpec.Builder(
            alias,
            KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
        )
            .setKeySize(keySize)
            .setDigests(HMAC_DIGEST)
            .setUserAuthenticationRequired(false)
            .build()

        generator.init(spec)
        return generator.generateKey()
    }

    private fun keyStore(): KeyStore {
        return KeyStore.getInstance(ANDROID_KEY_STORE).apply {
            load(null)
        }
    }
}