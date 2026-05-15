package com.myimdad_por.data.local.preference

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.myimdad_por.core.utils.Constants
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsDataStore @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {

    companion object {
        private val KEY_APP_LANGUAGE = stringPreferencesKey("app_language")
        private val KEY_APP_THEME = stringPreferencesKey("app_theme")
        private val KEY_CURRENCY_CODE = stringPreferencesKey("currency_code")
        private val KEY_DATE_FORMAT = stringPreferencesKey("date_format")
        private val KEY_TIME_FORMAT = stringPreferencesKey("time_format")

        private val KEY_AUTO_SYNC = booleanPreferencesKey("auto_sync")
        private val KEY_SYNC_ON_STARTUP = booleanPreferencesKey("sync_on_startup")
        private val KEY_WIFI_ONLY_SYNC = booleanPreferencesKey("wifi_only_sync")
        private val KEY_DARK_MODE = booleanPreferencesKey("dark_mode")
        private val KEY_BIOMETRIC_LOCK = booleanPreferencesKey("biometric_lock")
        private val KEY_NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        private val KEY_ANALYTICS_ENABLED = booleanPreferencesKey("analytics_enabled")
        private val KEY_SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        private val KEY_VIBRATION_ENABLED = booleanPreferencesKey("vibration_enabled")

        private val KEY_DEFAULT_PAGE_SIZE = intPreferencesKey("default_page_size")
        private val KEY_AUTO_SYNC_INTERVAL_MINUTES = intPreferencesKey("auto_sync_interval_minutes")
        private val KEY_LAST_SYNC_MILLIS = longPreferencesKey("last_sync_millis")
        private val KEY_LAST_BACKUP_MILLIS = longPreferencesKey("last_backup_millis")
        private val KEY_FONT_SCALE = floatPreferencesKey("font_scale")

        private const val DEFAULT_THEME = "SYSTEM"
        private const val DEFAULT_CURRENCY = "SDG"
        private const val DEFAULT_DATE_FORMAT = "yyyy-MM-dd"
        private const val DEFAULT_TIME_FORMAT = "HH:mm"
        private const val DEFAULT_AUTO_SYNC_INTERVAL_MINUTES = 15
        private const val CURRENCY_CODE_LENGTH = 3
        private val CURRENCY_CODE_REGEX = Regex("^[A-Z]{3}$")
    }

    fun observeAppLanguage(defaultValue: String = Locale.getDefault().language): Flow<String> =
        observeString(KEY_APP_LANGUAGE, defaultValue)

    fun observeAppTheme(defaultValue: String = DEFAULT_THEME): Flow<String> =
        observeString(KEY_APP_THEME, defaultValue)

    fun observeCurrencyCode(defaultValue: String = DEFAULT_CURRENCY): Flow<String> =
        observeString(KEY_CURRENCY_CODE, defaultValue)

    fun observeDateFormat(defaultValue: String = DEFAULT_DATE_FORMAT): Flow<String> =
        observeString(KEY_DATE_FORMAT, defaultValue)

    fun observeTimeFormat(defaultValue: String = DEFAULT_TIME_FORMAT): Flow<String> =
        observeString(KEY_TIME_FORMAT, defaultValue)

    fun observeAutoSync(defaultValue: Boolean = true): Flow<Boolean> =
        observeBoolean(KEY_AUTO_SYNC, defaultValue)

    fun observeSyncOnStartup(defaultValue: Boolean = true): Flow<Boolean> =
        observeBoolean(KEY_SYNC_ON_STARTUP, defaultValue)

    fun observeWifiOnlySync(defaultValue: Boolean = false): Flow<Boolean> =
        observeBoolean(KEY_WIFI_ONLY_SYNC, defaultValue)

    fun observeDarkMode(defaultValue: Boolean = false): Flow<Boolean> =
        observeBoolean(KEY_DARK_MODE, defaultValue)

    fun observeBiometricLock(defaultValue: Boolean = false): Flow<Boolean> =
        observeBoolean(KEY_BIOMETRIC_LOCK, defaultValue)

    fun observeNotificationsEnabled(defaultValue: Boolean = true): Flow<Boolean> =
        observeBoolean(KEY_NOTIFICATIONS_ENABLED, defaultValue)

    fun observeAnalyticsEnabled(defaultValue: Boolean = false): Flow<Boolean> =
        observeBoolean(KEY_ANALYTICS_ENABLED, defaultValue)

    fun observeSoundEnabled(defaultValue: Boolean = true): Flow<Boolean> =
        observeBoolean(KEY_SOUND_ENABLED, defaultValue)

    fun observeVibrationEnabled(defaultValue: Boolean = true): Flow<Boolean> =
        observeBoolean(KEY_VIBRATION_ENABLED, defaultValue)

    fun observeDefaultPageSize(
        defaultValue: Int = Constants.Pagination.DEFAULT_PAGE_SIZE
    ): Flow<Int> = observeInt(KEY_DEFAULT_PAGE_SIZE, defaultValue)

    fun observeAutoSyncIntervalMinutes(
        defaultValue: Int = DEFAULT_AUTO_SYNC_INTERVAL_MINUTES
    ): Flow<Int> = observeInt(KEY_AUTO_SYNC_INTERVAL_MINUTES, defaultValue)

    fun observeLastSyncMillis(defaultValue: Long = 0L): Flow<Long> =
        observeLong(KEY_LAST_SYNC_MILLIS, defaultValue)

    fun observeLastBackupMillis(defaultValue: Long = 0L): Flow<Long> =
        observeLong(KEY_LAST_BACKUP_MILLIS, defaultValue)

    fun observeFontScale(defaultValue: Float = 1.0f): Flow<Float> =
        observeFloat(KEY_FONT_SCALE, defaultValue)

    suspend fun getAppLanguage(defaultValue: String = Locale.getDefault().language): String =
        readString(KEY_APP_LANGUAGE, defaultValue)

    suspend fun getAppTheme(defaultValue: String = DEFAULT_THEME): String =
        readString(KEY_APP_THEME, defaultValue)

    suspend fun getCurrencyCode(defaultValue: String = DEFAULT_CURRENCY): String =
        readString(KEY_CURRENCY_CODE, defaultValue)

    suspend fun getDateFormat(defaultValue: String = DEFAULT_DATE_FORMAT): String =
        readString(KEY_DATE_FORMAT, defaultValue)

    suspend fun getTimeFormat(defaultValue: String = DEFAULT_TIME_FORMAT): String =
        readString(KEY_TIME_FORMAT, defaultValue)

    suspend fun isAutoSyncEnabled(defaultValue: Boolean = true): Boolean =
        readBoolean(KEY_AUTO_SYNC, defaultValue)

    suspend fun isSyncOnStartupEnabled(defaultValue: Boolean = true): Boolean =
        readBoolean(KEY_SYNC_ON_STARTUP, defaultValue)

    suspend fun isWifiOnlySyncEnabled(defaultValue: Boolean = false): Boolean =
        readBoolean(KEY_WIFI_ONLY_SYNC, defaultValue)

    suspend fun isDarkModeEnabled(defaultValue: Boolean = false): Boolean =
        readBoolean(KEY_DARK_MODE, defaultValue)

    suspend fun isBiometricLockEnabled(defaultValue: Boolean = false): Boolean =
        readBoolean(KEY_BIOMETRIC_LOCK, defaultValue)

    suspend fun isNotificationsEnabled(defaultValue: Boolean = true): Boolean =
        readBoolean(KEY_NOTIFICATIONS_ENABLED, defaultValue)

    suspend fun isAnalyticsEnabled(defaultValue: Boolean = false): Boolean =
        readBoolean(KEY_ANALYTICS_ENABLED, defaultValue)

    suspend fun isSoundEnabled(defaultValue: Boolean = true): Boolean =
        readBoolean(KEY_SOUND_ENABLED, defaultValue)

    suspend fun isVibrationEnabled(defaultValue: Boolean = true): Boolean =
        readBoolean(KEY_VIBRATION_ENABLED, defaultValue)

    suspend fun getDefaultPageSize(
        defaultValue: Int = Constants.Pagination.DEFAULT_PAGE_SIZE
    ): Int = readInt(KEY_DEFAULT_PAGE_SIZE, defaultValue)

    suspend fun getAutoSyncIntervalMinutes(
        defaultValue: Int = DEFAULT_AUTO_SYNC_INTERVAL_MINUTES
    ): Int = readInt(KEY_AUTO_SYNC_INTERVAL_MINUTES, defaultValue)

    suspend fun getLastSyncMillis(defaultValue: Long = 0L): Long =
        readLong(KEY_LAST_SYNC_MILLIS, defaultValue)

    suspend fun getLastBackupMillis(defaultValue: Long = 0L): Long =
        readLong(KEY_LAST_BACKUP_MILLIS, defaultValue)

    suspend fun getFontScale(defaultValue: Float = 1.0f): Float =
        readFloat(KEY_FONT_SCALE, defaultValue)

    suspend fun setAppLanguage(value: String) {
        require(value.isNotBlank()) { "app language cannot be blank." }
        dataStore.edit { it[KEY_APP_LANGUAGE] = value.trim() }
    }

    suspend fun setAppTheme(value: String) {
        require(value.isNotBlank()) { "app theme cannot be blank." }
        dataStore.edit { it[KEY_APP_THEME] = value.trim().uppercase(Locale.ROOT) }
    }

    suspend fun setCurrencyCode(value: String) {
        val normalized = value.trim().uppercase(Locale.ROOT)
        require(normalized.matches(CURRENCY_CODE_REGEX)) {
            "currencyCode must be exactly 3 uppercase ISO letters."
        }
        dataStore.edit { it[KEY_CURRENCY_CODE] = normalized }
    }

    suspend fun setDateFormat(value: String) {
        require(value.isNotBlank()) { "date format cannot be blank." }
        dataStore.edit { it[KEY_DATE_FORMAT] = value.trim() }
    }

    suspend fun setTimeFormat(value: String) {
        require(value.isNotBlank()) { "time format cannot be blank." }
        dataStore.edit { it[KEY_TIME_FORMAT] = value.trim() }
    }

    suspend fun setAutoSync(enabled: Boolean) {
        dataStore.edit { it[KEY_AUTO_SYNC] = enabled }
    }

    suspend fun setSyncOnStartup(enabled: Boolean) {
        dataStore.edit { it[KEY_SYNC_ON_STARTUP] = enabled }
    }

    suspend fun setWifiOnlySync(enabled: Boolean) {
        dataStore.edit { it[KEY_WIFI_ONLY_SYNC] = enabled }
    }

    suspend fun setDarkMode(enabled: Boolean) {
        dataStore.edit { it[KEY_DARK_MODE] = enabled }
    }

    suspend fun setBiometricLock(enabled: Boolean) {
        dataStore.edit { it[KEY_BIOMETRIC_LOCK] = enabled }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_NOTIFICATIONS_ENABLED] = enabled }
    }

    suspend fun setAnalyticsEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_ANALYTICS_ENABLED] = enabled }
    }

    suspend fun setSoundEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_SOUND_ENABLED] = enabled }
    }

    suspend fun setVibrationEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_VIBRATION_ENABLED] = enabled }
    }

    suspend fun setDefaultPageSize(value: Int) {
        require(value > 0) { "default page size must be greater than zero." }
        dataStore.edit { it[KEY_DEFAULT_PAGE_SIZE] = value }
    }

    suspend fun setAutoSyncIntervalMinutes(value: Int) {
        require(value > 0) { "auto sync interval must be greater than zero." }
        dataStore.edit { it[KEY_AUTO_SYNC_INTERVAL_MINUTES] = value }
    }

    suspend fun setLastSyncMillis(value: Long) {
        require(value >= 0L) { "last sync millis cannot be negative." }
        dataStore.edit { it[KEY_LAST_SYNC_MILLIS] = value }
    }

    suspend fun setLastBackupMillis(value: Long) {
        require(value >= 0L) { "last backup millis cannot be negative." }
        dataStore.edit { it[KEY_LAST_BACKUP_MILLIS] = value }
    }

    suspend fun setFontScale(value: Float) {
        require(value > 0f) { "font scale must be greater than zero." }
        dataStore.edit { it[KEY_FONT_SCALE] = value }
    }

    suspend fun updateSettings(
        appLanguage: String? = null,
        appTheme: String? = null,
        currencyCode: String? = null,
        dateFormat: String? = null,
        timeFormat: String? = null,
        autoSync: Boolean? = null,
        syncOnStartup: Boolean? = null,
        wifiOnlySync: Boolean? = null,
        darkMode: Boolean? = null,
        biometricLock: Boolean? = null,
        notificationsEnabled: Boolean? = null,
        analyticsEnabled: Boolean? = null,
        soundEnabled: Boolean? = null,
        vibrationEnabled: Boolean? = null,
        defaultPageSize: Int? = null,
        autoSyncIntervalMinutes: Int? = null,
        lastSyncMillis: Long? = null,
        lastBackupMillis: Long? = null,
        fontScale: Float? = null
    ) {
        dataStore.edit { preferences ->
            appLanguage?.takeIf { it.isNotBlank() }?.let { preferences[KEY_APP_LANGUAGE] = it.trim() }
            appTheme?.takeIf { it.isNotBlank() }?.let { preferences[KEY_APP_THEME] = it.trim().uppercase(Locale.ROOT) }

            currencyCode?.takeIf { it.isNotBlank() }?.let {
                val normalized = it.trim().uppercase(Locale.ROOT)
                require(normalized.matches(CURRENCY_CODE_REGEX)) {
                    "currencyCode must be exactly 3 uppercase ISO letters."
                }
                preferences[KEY_CURRENCY_CODE] = normalized
            }

            dateFormat?.takeIf { it.isNotBlank() }?.let { preferences[KEY_DATE_FORMAT] = it.trim() }
            timeFormat?.takeIf { it.isNotBlank() }?.let { preferences[KEY_TIME_FORMAT] = it.trim() }

            autoSync?.let { preferences[KEY_AUTO_SYNC] = it }
            syncOnStartup?.let { preferences[KEY_SYNC_ON_STARTUP] = it }
            wifiOnlySync?.let { preferences[KEY_WIFI_ONLY_SYNC] = it }
            darkMode?.let { preferences[KEY_DARK_MODE] = it }
            biometricLock?.let { preferences[KEY_BIOMETRIC_LOCK] = it }
            notificationsEnabled?.let { preferences[KEY_NOTIFICATIONS_ENABLED] = it }
            analyticsEnabled?.let { preferences[KEY_ANALYTICS_ENABLED] = it }
            soundEnabled?.let { preferences[KEY_SOUND_ENABLED] = it }
            vibrationEnabled?.let { preferences[KEY_VIBRATION_ENABLED] = it }

            defaultPageSize?.takeIf { it > 0 }?.let { preferences[KEY_DEFAULT_PAGE_SIZE] = it }
            autoSyncIntervalMinutes?.takeIf { it > 0 }?.let { preferences[KEY_AUTO_SYNC_INTERVAL_MINUTES] = it }
            lastSyncMillis?.takeIf { it >= 0L }?.let { preferences[KEY_LAST_SYNC_MILLIS] = it }
            lastBackupMillis?.takeIf { it >= 0L }?.let { preferences[KEY_LAST_BACKUP_MILLIS] = it }
            fontScale?.takeIf { it > 0f }?.let { preferences[KEY_FONT_SCALE] = it }
        }
    }

    suspend fun snapshot(): SettingsSnapshot {
        val prefs = dataStore.data.catch { cause ->
            if (cause is IOException) emit(emptyPreferences()) else throw cause
        }.first()

        return SettingsSnapshot(
            appLanguage = prefs[KEY_APP_LANGUAGE] ?: Locale.getDefault().language,
            appTheme = prefs[KEY_APP_THEME] ?: DEFAULT_THEME,
            currencyCode = prefs[KEY_CURRENCY_CODE] ?: DEFAULT_CURRENCY,
            dateFormat = prefs[KEY_DATE_FORMAT] ?: DEFAULT_DATE_FORMAT,
            timeFormat = prefs[KEY_TIME_FORMAT] ?: DEFAULT_TIME_FORMAT,
            autoSync = prefs[KEY_AUTO_SYNC] ?: true,
            syncOnStartup = prefs[KEY_SYNC_ON_STARTUP] ?: true,
            wifiOnlySync = prefs[KEY_WIFI_ONLY_SYNC] ?: false,
            darkMode = prefs[KEY_DARK_MODE] ?: false,
            biometricLock = prefs[KEY_BIOMETRIC_LOCK] ?: false,
            notificationsEnabled = prefs[KEY_NOTIFICATIONS_ENABLED] ?: true,
            analyticsEnabled = prefs[KEY_ANALYTICS_ENABLED] ?: false,
            soundEnabled = prefs[KEY_SOUND_ENABLED] ?: true,
            vibrationEnabled = prefs[KEY_VIBRATION_ENABLED] ?: true,
            defaultPageSize = prefs[KEY_DEFAULT_PAGE_SIZE] ?: Constants.Pagination.DEFAULT_PAGE_SIZE,
            autoSyncIntervalMinutes = prefs[KEY_AUTO_SYNC_INTERVAL_MINUTES] ?: DEFAULT_AUTO_SYNC_INTERVAL_MINUTES,
            lastSyncMillis = prefs[KEY_LAST_SYNC_MILLIS] ?: 0L,
            lastBackupMillis = prefs[KEY_LAST_BACKUP_MILLIS] ?: 0L,
            fontScale = prefs[KEY_FONT_SCALE] ?: 1.0f
        )
    }

    private fun observeString(
        key: Preferences.Key<String>,
        defaultValue: String
    ): Flow<String> {
        return dataStore.data
            .catch { cause ->
                if (cause is IOException) emit(emptyPreferences()) else throw cause
            }
            .map { it[key] ?: defaultValue }
            .distinctUntilChanged()
    }

    private fun observeBoolean(
        key: Preferences.Key<Boolean>,
        defaultValue: Boolean
    ): Flow<Boolean> {
        return dataStore.data
            .catch { cause ->
                if (cause is IOException) emit(emptyPreferences()) else throw cause
            }
            .map { it[key] ?: defaultValue }
            .distinctUntilChanged()
    }

    private fun observeInt(
        key: Preferences.Key<Int>,
        defaultValue: Int
    ): Flow<Int> {
        return dataStore.data
            .catch { cause ->
                if (cause is IOException) emit(emptyPreferences()) else throw cause
            }
            .map { it[key] ?: defaultValue }
            .distinctUntilChanged()
    }

    private fun observeLong(
        key: Preferences.Key<Long>,
        defaultValue: Long
    ): Flow<Long> {
        return dataStore.data
            .catch { cause ->
                if (cause is IOException) emit(emptyPreferences()) else throw cause
            }
            .map { it[key] ?: defaultValue }
            .distinctUntilChanged()
    }

    private fun observeFloat(
        key: Preferences.Key<Float>,
        defaultValue: Float
    ): Flow<Float> {
        return dataStore.data
            .catch { cause ->
                if (cause is IOException) emit(emptyPreferences()) else throw cause
            }
            .map { it[key] ?: defaultValue }
            .distinctUntilChanged()
    }

    private suspend fun readString(
        key: Preferences.Key<String>,
        defaultValue: String
    ): String = dataStore.data.first()[key] ?: defaultValue

    private suspend fun readBoolean(
        key: Preferences.Key<Boolean>,
        defaultValue: Boolean
    ): Boolean = dataStore.data.first()[key] ?: defaultValue

    private suspend fun readInt(
        key: Preferences.Key<Int>,
        defaultValue: Int
    ): Int = dataStore.data.first()[key] ?: defaultValue

    private suspend fun readLong(
        key: Preferences.Key<Long>,
        defaultValue: Long
    ): Long = dataStore.data.first()[key] ?: defaultValue

    private suspend fun readFloat(
        key: Preferences.Key<Float>,
        defaultValue: Float
    ): Float = dataStore.data.first()[key] ?: defaultValue
}

data class SettingsSnapshot(
    val appLanguage: String,
    val appTheme: String,
    val currencyCode: String,
    val dateFormat: String,
    val timeFormat: String,
    val autoSync: Boolean,
    val syncOnStartup: Boolean,
    val wifiOnlySync: Boolean,
    val darkMode: Boolean,
    val biometricLock: Boolean,
    val notificationsEnabled: Boolean,
    val analyticsEnabled: Boolean,
    val soundEnabled: Boolean,
    val vibrationEnabled: Boolean,
    val defaultPageSize: Int,
    val autoSyncIntervalMinutes: Int,
    val lastSyncMillis: Long,
    val lastBackupMillis: Long,
    val fontScale: Float
)