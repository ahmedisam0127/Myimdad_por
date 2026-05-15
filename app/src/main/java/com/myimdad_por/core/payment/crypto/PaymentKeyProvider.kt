package com.myimdad_por.core.payment.crypto

import com.myimdad_por.core.security.KeyStoreManager
import javax.crypto.SecretKey

/**
 * Central source for payment cryptographic material.
 *
 * Payment keys are isolated from the rest of the app:
 * - AES key for payload encryption
 * - HMAC key for request signing and tamper detection
 *
 * No feature code should talk to KeyStoreManager directly.
 */
object PaymentKeyProvider {

    private const val PAYMENT_AES_ALIAS = "imdad_por_payment_aes_key_v1"
    private const val PAYMENT_HMAC_ALIAS = "imdad_por_payment_hmac_key_v1"

    fun getAesKey(): SecretKey {
        return KeyStoreManager.getOrCreateAesKey(PAYMENT_AES_ALIAS)
    }

    fun getHmacKey(): SecretKey {
        return KeyStoreManager.getOrCreateHmacKey(PAYMENT_HMAC_ALIAS)
    }

    fun hasKeys(): Boolean {
        return runCatching {
            KeyStoreManager.containsAlias(PAYMENT_AES_ALIAS) &&
                KeyStoreManager.containsAlias(PAYMENT_HMAC_ALIAS)
        }.getOrDefault(false)
    }

    @Synchronized
    fun resetKeys() {
        runCatching { KeyStoreManager.deleteKey(PAYMENT_AES_ALIAS) }
        runCatching { KeyStoreManager.deleteKey(PAYMENT_HMAC_ALIAS) }
    }

    @Synchronized
    fun rotateKeys(): Pair<SecretKey, SecretKey> {
        resetKeys()
        return getAesKey() to getHmacKey()
    }
}