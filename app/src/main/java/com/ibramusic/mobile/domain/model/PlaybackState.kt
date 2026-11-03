package com.ibramusic.mobile.domain.model

import android.net.Uri

enum class RepeatMode {
    OFF,
    ALL,
    ONE
}

data class PlaybackState(
    val currentTrack: Track? = null,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val shuffleEnabled: Boolean = false,
    val volume: Float = 1.0f,
    val playbackError: String? = null
)

data class PlaybackSource(
    val uri: Uri,
    val mimeType: String? = null,
    val providerId: String,
    val expiresAt: Long? = null,
    val isSeekable: Boolean = true
)

data class ProviderCapabilities(
    val search: Boolean = true,
    val streaming: Boolean = true,
    val downloads: Boolean = false,
    val playlists: Boolean = false,
    val favorites: Boolean = false,
    val offlinePlayback: Boolean = false
)

data class LyricLine(
    val timeMs: Long,
    val text: String
)

