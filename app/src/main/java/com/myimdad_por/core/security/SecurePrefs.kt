package com.myimdad_por.core.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Encrypted shared preferences helper for small sensitive values.
 *
 * Good for:
 * - session flags
 * - tokens
 * - identifiers
 * - security settings
 *
 * Not intended for large or structured data.
 */
object SecurePrefs {

    private const val PREFS_NAME = "secure_prefs"
    private const val MASTER_KEY_ALIAS = "secure_prefs_master_key"

    private var prefs: SharedPreferences? = null

    @Synchronized
    fun init(context: Context) {
        if (prefs == null) {
            prefs = createEncryptedPrefs(context.applicationContext)
        }
    }

    private fun requirePrefs(): SharedPreferences {
        return prefs ?: throw IllegalStateException(
            "SecurePrefs is not initialized. Call SecurePrefs.init(context) first."
        )
    }

    fun putString(key: String, value: String?) {
        require(key.isNotBlank()) { "key must not be blank." }
        requirePrefs().edit().putString(key, value).apply()
    }

    fun getString(key: String, defaultValue: String? = null): String? {
        require(key.isNotBlank()) { "key must not be blank." }
        return requirePrefs().getString(key, defaultValue)
    }

    fun putBoolean(key: String, value: Boolean) {
        require(key.isNotBlank()) { "key must not be blank." }
        requirePrefs().edit().putBoolean(key, value).apply()
    }

    fun getBoolean(key: String, defaultValue: Boolean = false): Boolean {
        require(key.isNotBlank()) { "key must not be blank." }
        return requirePrefs().getBoolean(key, defaultValue)
    }

    fun putInt(key: String, value: Int) {
        require(key.isNotBlank()) { "key must not be blank." }
        requirePrefs().edit().putInt(key, value).apply()
    }

    fun getInt(key: String, defaultValue: Int = 0): Int {
        require(key.isNotBlank()) { "key must not be blank." }
        return requirePrefs().getInt(key, defaultValue)
    }

    fun putLong(key: String, value: Long) {
        require(key.isNotBlank()) { "key must not be blank." }
        requirePrefs().edit().putLong(key, value).apply()
    }

    fun getLong(key: String, defaultValue: Long = 0L): Long {
        require(key.isNotBlank()) { "key must not be blank." }
        return requirePrefs().getLong(key, defaultValue)
    }

    fun putFloat(key: String, value: Float) {
        require(key.isNotBlank()) { "key must not be blank." }
        requirePrefs().edit().putFloat(key, value).apply()
    }

    fun getFloat(key: String, defaultValue: Float = 0f): Float {
        require(key.isNotBlank()) { "key must not be blank." }
        return requirePrefs().getFloat(key, defaultValue)
    }

    fun contains(key: String): Boolean {
        require(key.isNotBlank()) { "key must not be blank." }
        return requirePrefs().contains(key)
    }

    fun remove(key: String) {
        require(key.isNotBlank()) { "key must not be blank." }
        requirePrefs().edit().remove(key).apply()
    }

    fun clear() {
        requirePrefs().edit().clear().apply()
    }

    fun getAllKeys(): Set<String> {
        return requirePrefs().all.keys
    }

    private fun createEncryptedPrefs(context: Context): SharedPreferences {
        val masterKey = MasterKey.Builder(context, MASTER_KEY_ALIAS)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        return EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }
}