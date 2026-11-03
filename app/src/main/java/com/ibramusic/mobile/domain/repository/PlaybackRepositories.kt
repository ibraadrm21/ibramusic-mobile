package com.ibramusic.mobile.domain.repository

import com.ibramusic.mobile.domain.model.PlaybackQueue
import com.ibramusic.mobile.domain.model.PlaybackState
import com.ibramusic.mobile.domain.model.QueueItem
import com.ibramusic.mobile.domain.model.RepeatMode
import com.ibramusic.mobile.domain.model.Track
import kotlinx.coroutines.flow.StateFlow

interface PlaybackRepository {
    val playbackState: StateFlow<PlaybackState>
    val playbackSpeed: StateFlow<Float>
    val sleepTimerMinutesLeft: StateFlow<Int?>

    fun playTrack(track: Track, newContextQueue: List<Track>? = null)
    fun togglePlayPause()
    fun pause()
    fun resume()
    fun seekTo(positionMs: Long)
    fun skipToNext()
    fun skipToPrevious()
    fun setRepeatMode(mode: RepeatMode)
    fun toggleShuffle()
    fun setVolume(volume: Float)
    fun setPlaybackSpeed(speed: Float)
    fun setSleepTimer(minutes: Int)
}

interface QueueRepository {
    val queue: StateFlow<PlaybackQueue>

    suspend fun setContextQueue(tracks: List<Track>, startIndex: Int = 0)
    suspend fun playNext(track: Track)
    suspend fun addToQueue(track: Track)
    suspend fun removeFromQueue(queueEntryId: String)
    suspend fun reorderUserQueue(fromIndex: Int, toIndex: Int)
    suspend fun reorderPlaylistQueue(fromIndex: Int, toIndex: Int)
    suspend fun clearUserQueue()
    suspend fun clearAll()
    suspend fun restorePersistedQueue(): PlaybackQueue
    suspend fun persistQueue()
}

