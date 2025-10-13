package com.example.sms_hub_agent.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.sms_hub_agent.model.AppSettings

/**
 * Repository for managing app settings with encrypted storage
 */
class SettingsRepository(context: Context) {

    private val sharedPreferences: SharedPreferences

    init {
        // Create MasterKey for encryption
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        // Create EncryptedSharedPreferences
        sharedPreferences = EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    /**
     * Save app settings to encrypted storage
     */
    fun saveSettings(settings: AppSettings) {
        sharedPreferences.edit().apply {
            putString(KEY_WEBHOOK_URL, settings.webhookUrl)
            putString(KEY_AUTH_USERNAME, settings.authUsername)
            putString(KEY_AUTH_PASSWORD, settings.authPassword)
            putBoolean(KEY_HMAC_ENABLED, settings.hmacEnabled)
            putString(KEY_HMAC_SECRET, settings.hmacSecret)
            putInt(KEY_RETRY_COUNT, settings.retryCount)
            putInt(KEY_TIMEOUT_SECONDS, settings.timeoutSeconds)
            apply()
        }
    }

    /**
     * Load app settings from encrypted storage
     */
    fun loadSettings(): AppSettings {
        return AppSettings(
            webhookUrl = sharedPreferences.getString(KEY_WEBHOOK_URL, "") ?: "",
            authUsername = sharedPreferences.getString(KEY_AUTH_USERNAME, "") ?: "",
            authPassword = sharedPreferences.getString(KEY_AUTH_PASSWORD, "") ?: "",
            hmacEnabled = sharedPreferences.getBoolean(KEY_HMAC_ENABLED, false),
            hmacSecret = sharedPreferences.getString(KEY_HMAC_SECRET, "") ?: "",
            retryCount = sharedPreferences.getInt(KEY_RETRY_COUNT, 3),
            timeoutSeconds = sharedPreferences.getInt(KEY_TIMEOUT_SECONDS, 30)
        )
    }

    /**
     * Save last forwarding timestamp
     */
    fun saveLastForwardTime(timestamp: Long) {
        sharedPreferences.edit().putLong(KEY_LAST_FORWARD_TIME, timestamp).apply()
    }

    /**
     * Get last forwarding timestamp
     */
    fun getLastForwardTime(): Long {
        return sharedPreferences.getLong(KEY_LAST_FORWARD_TIME, 0L)
    }

    /**
     * Clear all settings
     */
    fun clearSettings() {
        sharedPreferences.edit().clear().apply()
    }

    companion object {
        private const val PREFS_NAME = "sms_hub_prefs"
        private const val KEY_WEBHOOK_URL = "webhook_url"
        private const val KEY_AUTH_USERNAME = "auth_username"
        private const val KEY_AUTH_PASSWORD = "auth_password"
        private const val KEY_HMAC_ENABLED = "hmac_enabled"
        private const val KEY_HMAC_SECRET = "hmac_secret"
        private const val KEY_RETRY_COUNT = "retry_count"
        private const val KEY_TIMEOUT_SECONDS = "timeout_seconds"
        private const val KEY_LAST_FORWARD_TIME = "last_forward_time"
    }
}
