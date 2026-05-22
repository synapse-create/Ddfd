package com.example.data.settings

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "clipluz_settings")

class DataStoreManager(private val context: Context) {

    companion object {
        private val IS_DARK_MODE = booleanPreferencesKey("is_dark_mode")
        private val IS_PRO_UNLOCKED = booleanPreferencesKey("is_pro_unlocked")
        private val SECURE_PIN = stringPreferencesKey("secure_pin")
        private val IS_PIN_LOCKED = booleanPreferencesKey("is_pin_locked")
        private val IS_WATERMARK_ENABLED = booleanPreferencesKey("is_watermark_enabled")
    }

    val isDarkMode: Flow<Boolean?> = context.dataStore.data.map { preferences ->
        preferences[IS_DARK_MODE]
    }

    val isProUnlocked: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[IS_PRO_UNLOCKED] ?: false
    }

    val securePin: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[SECURE_PIN]
    }

    val isPinLocked: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[IS_PIN_LOCKED] ?: false
    }

    val isWatermarkEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[IS_WATERMARK_ENABLED] ?: true
    }

    suspend fun setDarkMode(enabled: Boolean?) {
        context.dataStore.edit { preferences ->
            if (enabled == null) {
                preferences.remove(IS_DARK_MODE)
            } else {
                preferences[IS_DARK_MODE] = enabled
            }
        }
    }

    suspend fun setProUnlocked(unlocked: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[IS_PRO_UNLOCKED] = unlocked
        }
    }

    suspend fun setSecurePin(pin: String?) {
        context.dataStore.edit { preferences ->
            if (pin.isNullOrEmpty()) {
                preferences.remove(SECURE_PIN)
                preferences[IS_PIN_LOCKED] = false
            } else {
                preferences[SECURE_PIN] = pin
                preferences[IS_PIN_LOCKED] = true
            }
        }
    }

    suspend fun setPinLocked(locked: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[IS_PIN_LOCKED] = locked
        }
    }

    suspend fun setWatermarkEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[IS_WATERMARK_ENABLED] = enabled
        }
    }
}
