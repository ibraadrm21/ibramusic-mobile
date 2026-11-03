package com.ibramusic.mobile.domain.usecase

import com.ibramusic.mobile.domain.model.PlaybackQueue
import com.ibramusic.mobile.domain.model.PlaybackState
import com.ibramusic.mobile.domain.model.RepeatMode
import com.ibramusic.mobile.domain.model.Track
import com.ibramusic.mobile.domain.repository.PlaybackRepository
import com.ibramusic.mobile.domain.repository.QueueRepository
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

class PlayTrackUseCase @Inject constructor(
    private val playbackRepository: PlaybackRepository
) {
    operator fun invoke(track: Track, contextQueue: List<Track>? = null) {
        playbackRepository.playTrack(track, contextQueue)
    }
}

class TogglePlayPauseUseCase @Inject constructor(
    private val playbackRepository: PlaybackRepository
) {
    operator fun invoke() {
        playbackRepository.togglePlayPause()
    }
}

class SeekToUseCase @Inject constructor(
    private val playbackRepository: PlaybackRepository
) {
    operator fun invoke(positionMs: Long) {
        playbackRepository.seekTo(positionMs)
    }
}

class SkipNextUseCase @Inject constructor(
    private val playbackRepository: PlaybackRepository
) {
    operator fun invoke() {
        playbackRepository.skipToNext()
    }
}

class SkipPreviousUseCase @Inject constructor(
    private val playbackRepository: PlaybackRepository
) {
    operator fun invoke() {
        playbackRepository.skipToPrevious()
    }
}

class ToggleShuffleUseCase @Inject constructor(
    private val playbackRepository: PlaybackRepository
) {
    operator fun invoke() {
        playbackRepository.toggleShuffle()
    }
}

class SetRepeatModeUseCase @Inject constructor(
    private val playbackRepository: PlaybackRepository
) {
    operator fun invoke(mode: RepeatMode) {
        playbackRepository.setRepeatMode(mode)
    }
}

class GetPlaybackStateUseCase @Inject constructor(
    private val playbackRepository: PlaybackRepository
) {
    operator fun invoke(): StateFlow<PlaybackState> {
        return playbackRepository.playbackState
    }
}

class ManageQueueUseCase @Inject constructor(
    private val queueRepository: QueueRepository
) {
    val queue: StateFlow<PlaybackQueue> = queueRepository.queue

    suspend fun playNext(track: Track) {
        queueRepository.playNext(track)
    }

    suspend fun addToQueue(track: Track) {
        queueRepository.addToQueue(track)
    }

    suspend fun removeFromQueue(queueEntryId: String) {
        queueRepository.removeFromQueue(queueEntryId)
    }

    suspend fun reorderUserQueue(fromIndex: Int, toIndex: Int) {
        queueRepository.reorderUserQueue(fromIndex, toIndex)
    }

    suspend fun reorderPlaylistQueue(fromIndex: Int, toIndex: Int) {
        queueRepository.reorderPlaylistQueue(fromIndex, toIndex)
    }

    suspend fun clearUserQueue() {
        queueRepository.clearUserQueue()
    }

    suspend fun persistQueue() {
        queueRepository.persistQueue()
    }

    suspend fun restorePersistedQueue() {
        queueRepository.restorePersistedQueue()
    }
}

