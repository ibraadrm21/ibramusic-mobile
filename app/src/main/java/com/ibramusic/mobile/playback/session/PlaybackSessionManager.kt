package com.ibramusic.mobile.playback.session

import android.content.Context
import android.content.SharedPreferences
import com.ibramusic.mobile.domain.model.RepeatMode
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

data class SavedPlaybackSession(
    val trackId: String?,
    val positionMs: Long,
    val durationMs: Long,
    val shuffle: Boolean,
    val repeatMode: RepeatMode
)

@Singleton
class PlaybackSessionManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val PREFS_NAME = "ibramusic_playback_session"
        private const val KEY_TRACK_ID = "last_track_id"
        private const val KEY_POSITION_MS = "last_position_ms"
        private const val KEY_DURATION_MS = "last_duration_ms"
        private const val KEY_SHUFFLE = "last_shuffle"
        private const val KEY_REPEAT_MODE = "last_repeat_mode"
    }

    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun saveSession(
        trackId: String?,
        positionMs: Long,
        durationMs: Long,
        shuffle: Boolean,
        repeatMode: RepeatMode
    ) {
        if (trackId == null) return
        prefs.edit()
            .putString(KEY_TRACK_ID, trackId)
            .putLong(KEY_POSITION_MS, positionMs.coerceAtLeast(0L))
            .putLong(KEY_DURATION_MS, durationMs.coerceAtLeast(0L))
            .putBoolean(KEY_SHUFFLE, shuffle)
            .putString(KEY_REPEAT_MODE, repeatMode.name)
            .apply()
    }

    fun updatePosition(positionMs: Long, durationMs: Long) {
        prefs.edit()
            .putLong(KEY_POSITION_MS, positionMs.coerceAtLeast(0L))
            .putLong(KEY_DURATION_MS, durationMs.coerceAtLeast(0L))
            .apply()
    }

    fun getSavedSession(): SavedPlaybackSession? {
        val trackId = prefs.getString(KEY_TRACK_ID, null) ?: return null
        val positionMs = prefs.getLong(KEY_POSITION_MS, 0L)
        val durationMs = prefs.getLong(KEY_DURATION_MS, 0L)
        val shuffle = prefs.getBoolean(KEY_SHUFFLE, false)
        val repeatModeStr = prefs.getString(KEY_REPEAT_MODE, RepeatMode.OFF.name) ?: RepeatMode.OFF.name
        val repeatMode = try {
            RepeatMode.valueOf(repeatModeStr)
        } catch (e: Exception) {
            RepeatMode.OFF
        }

        return SavedPlaybackSession(
            trackId = trackId,
            positionMs = positionMs,
            durationMs = durationMs,
            shuffle = shuffle,
            repeatMode = repeatMode
        )
    }

    fun clearSession() {
        prefs.edit().clear().apply()
    }
}

