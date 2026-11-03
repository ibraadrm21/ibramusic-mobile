package com.ibramusic.mobile.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.ibramusic.mobile.core.di.SettingsDataStore
import com.ibramusic.mobile.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    @SettingsDataStore private val dataStore: DataStore<Preferences>
) : SettingsRepository {

    companion object {
        val KEY_AUDIO_QUALITY = stringPreferencesKey("audio_quality")
        val KEY_CROSSFADE_ENABLED = booleanPreferencesKey("crossfade_enabled")
        val KEY_CROSSFADE_DURATION = intPreferencesKey("crossfade_duration")
        val KEY_THEME = stringPreferencesKey("theme")
    }

    override val audioQuality: Flow<String> = dataStore.data.map { prefs ->
        prefs[KEY_AUDIO_QUALITY] ?: "high"
    }

    override val crossfadeEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_CROSSFADE_ENABLED] ?: true
    }

    override val crossfadeDurationSeconds: Flow<Int> = dataStore.data.map { prefs ->
        prefs[KEY_CROSSFADE_DURATION] ?: 4
    }

    override val theme: Flow<String> = dataStore.data.map { prefs ->
        prefs[KEY_THEME] ?: "dark"
    }

    override suspend fun setAudioQuality(quality: String) {
        dataStore.edit { prefs ->
            prefs[KEY_AUDIO_QUALITY] = quality
        }
    }

    override suspend fun setCrossfadeEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[KEY_CROSSFADE_ENABLED] = enabled
        }
    }

    override suspend fun setCrossfadeDuration(seconds: Int) {
        dataStore.edit { prefs ->
            prefs[KEY_CROSSFADE_DURATION] = seconds
        }
    }

    override suspend fun setTheme(theme: String) {
        dataStore.edit { prefs ->
            prefs[KEY_THEME] = theme
        }
    }
}
