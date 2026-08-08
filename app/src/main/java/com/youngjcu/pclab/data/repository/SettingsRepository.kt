package com.youngjcu.pclab.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

data class UserSettings(
    val theme: ThemePreference = ThemePreference.SYSTEM,
    val fontScale: Float = 1f,
    val colourBlindMode: Boolean = false
)

enum class ThemePreference { SYSTEM, LIGHT, DARK }

interface SettingsRepository {
    val settings: Flow<UserSettings>
    suspend fun updateTheme(theme: ThemePreference)
    suspend fun updateFontScale(scale: Float)
    suspend fun updateColourBlindMode(enabled: Boolean)
}

@Singleton
class DataStoreSettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : SettingsRepository {
    override val settings: Flow<UserSettings> = dataStore.data.map { preferences ->
        UserSettings(
            theme = preferences[THEME]?.let { runCatching { ThemePreference.valueOf(it) }.getOrNull() } ?: ThemePreference.SYSTEM,
            fontScale = preferences[FONT_SCALE] ?: 1f,
            colourBlindMode = preferences[COLOUR_BLIND_MODE] ?: false
        )
    }

    override suspend fun updateTheme(theme: ThemePreference) {
        dataStore.edit { it[THEME] = theme.name }
    }

    override suspend fun updateFontScale(scale: Float) {
        dataStore.edit { it[FONT_SCALE] = scale }
    }

    override suspend fun updateColourBlindMode(enabled: Boolean) {
        dataStore.edit { it[COLOUR_BLIND_MODE] = enabled }
    }

    private companion object {
        val THEME = stringPreferencesKey("theme")
        val FONT_SCALE = floatPreferencesKey("font_scale")
        val COLOUR_BLIND_MODE = booleanPreferencesKey("colour_blind_mode")
    }
}
