package com.banktracker.vn.utils

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import androidx.core.content.edit

class PreferencesManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "bank_tracker_prefs"

        // Keys
        private const val KEY_SOUND_ENABLED = "sound_enabled"
        private const val KEY_VIBRATION_ENABLED = "vibration_enabled"
        private const val KEY_MAX_VOLUME_ENABLED = "max_volume_enabled"
        private const val KEY_TTS_ENABLED = "tts_enabled"
        private const val KEY_CUSTOM_SOUND_URI = "custom_sound_uri"
        private const val KEY_MINIMUM_AMOUNT = "minimum_amount"
        private const val KEY_LARGE_AMOUNT_THRESHOLD = "large_amount_threshold"
        private const val KEY_DARK_MODE = "dark_mode"
        private const val KEY_FIRST_LAUNCH = "first_launch"

        // Bank enable keys
        private const val KEY_BANK_PREFIX = "bank_enabled_"

        // Default values
        private const val DEFAULT_MINIMUM_AMOUNT = 0.0
        private const val DEFAULT_LARGE_AMOUNT = 10_000_000.0 // 10 triệu

        private const val KEY_ALLOW_UNKNOWN_BANKS = "allow_unknown_banks"
        private const val KEY_ALLOW_AUTO_RESTART = "allow_auto_restart"
    }

    // Sound settings
    fun isSoundEnabled(): Boolean = prefs.getBoolean(KEY_SOUND_ENABLED, true)

    fun setSoundEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SOUND_ENABLED, enabled).apply()
    }

    fun isVibrationEnabled(): Boolean = prefs.getBoolean(KEY_VIBRATION_ENABLED, true)

    fun setVibrationEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_VIBRATION_ENABLED, enabled).apply()
    }

    fun isMaxVolumeEnabled(): Boolean = prefs.getBoolean(KEY_MAX_VOLUME_ENABLED, false)

    fun setMaxVolumeEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_MAX_VOLUME_ENABLED, enabled).apply()
    }

    fun isTTSEnabled(): Boolean = prefs.getBoolean(KEY_TTS_ENABLED, false)

    fun setTTSEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_TTS_ENABLED, enabled).apply()
    }

    fun getCustomSoundUri(): Uri? {
        val uriString = prefs.getString(KEY_CUSTOM_SOUND_URI, null)
        return if (uriString != null) Uri.parse(uriString) else null
    }

    fun setCustomSoundUri(uri: Uri?) {
        prefs.edit().putString(KEY_CUSTOM_SOUND_URI, uri?.toString()).apply()
    }

    // Amount settings
    fun getMinimumAmount(): Double {
        return prefs.getFloat(KEY_MINIMUM_AMOUNT, DEFAULT_MINIMUM_AMOUNT.toFloat()).toDouble()
    }

    fun setMinimumAmount(amount: Double) {
        prefs.edit().putFloat(KEY_MINIMUM_AMOUNT, amount.toFloat()).apply()
    }

    fun getLargeAmountThreshold(): Double {
        return prefs.getFloat(KEY_LARGE_AMOUNT_THRESHOLD, DEFAULT_LARGE_AMOUNT.toFloat()).toDouble()
    }

    fun setLargeAmountThreshold(amount: Double) {
        prefs.edit().putFloat(KEY_LARGE_AMOUNT_THRESHOLD, amount.toFloat()).apply()
    }

    // Bank settings
    fun isBankEnabled(bankCode: String): Boolean {
        return prefs.getBoolean(KEY_BANK_PREFIX + bankCode, true)
    }

    fun setBankEnabled(bankCode: String, enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BANK_PREFIX + bankCode, enabled).apply()
    }

    fun getEnabledBanks(): Set<String> {
        val enabledBanks = mutableSetOf<String>()
        prefs.all.forEach { (key, value) ->
            if (key.startsWith(KEY_BANK_PREFIX) && value == true) {
                enabledBanks.add(key.removePrefix(KEY_BANK_PREFIX))
            }
        }
        return enabledBanks
    }

    // Theme settings
    fun isDarkModeEnabled(): Boolean = prefs.getBoolean(KEY_DARK_MODE, false)

    fun setDarkModeEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DARK_MODE, enabled).apply()
    }

    // First launch
    fun isFirstLaunch(): Boolean = prefs.getBoolean(KEY_FIRST_LAUNCH, true)

    fun setFirstLaunchComplete() {
        prefs.edit().putBoolean(KEY_FIRST_LAUNCH, false).apply()
    }

    // Clear all preferences
    fun clearAll() {
        prefs.edit().clear().apply()
    }

    fun isAllowUnknownBanks(): Boolean = prefs.getBoolean(KEY_ALLOW_UNKNOWN_BANKS, false)
    fun setAllowUnknownBanks(v: Boolean) = prefs.edit { putBoolean(KEY_ALLOW_UNKNOWN_BANKS, v) }

    fun isAutoRestartEnabled(): Boolean = prefs.getBoolean(KEY_ALLOW_AUTO_RESTART, false)
    fun setAutoRestartEnabled(v: Boolean) = prefs.edit { putBoolean(KEY_ALLOW_AUTO_RESTART, v) }
}