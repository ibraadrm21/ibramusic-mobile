package com.ibramusic.mobile.data.repository

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.ibramusic.mobile.data.local.dao.TrackDao
import com.ibramusic.mobile.data.local.entity.TrackEntity
import com.ibramusic.mobile.domain.model.PlaybackState
import com.ibramusic.mobile.domain.model.RepeatMode
import com.ibramusic.mobile.domain.model.Track
import com.ibramusic.mobile.domain.repository.PlaybackRepository
import com.ibramusic.mobile.playback.engine.ExoPlayerWrapper
import com.ibramusic.mobile.playback.queue.PreviousResult
import com.ibramusic.mobile.playback.queue.QueueManager
import com.ibramusic.mobile.playback.resolver.UnifiedPlaybackResolver
import com.ibramusic.mobile.playback.service.IbraMusicPlaybackService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaybackRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val playerWrapper: ExoPlayerWrapper,
    private val queueManager: QueueManager,
    private val resolver: UnifiedPlaybackResolver,
    private val trackDao: TrackDao
) : PlaybackRepository {

    private val repositoryScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override val playbackState: StateFlow<PlaybackState> = playerWrapper.playbackState
    override val playbackSpeed: StateFlow<Float> = playerWrapper.playbackSpeed
    override val sleepTimerMinutesLeft: StateFlow<Int?> = playerWrapper.sleepTimerMinutesLeft

    private fun ensureServiceStarted() {
        try {
            val intent = Intent(context, IbraMusicPlaybackService::class.java)
            ContextCompat.startForegroundService(context, intent)
        } catch (e: Exception) {
            // Handled
        }
    }

    override fun playTrack(track: Track, newContextQueue: List<Track>?) {
        // Immediate UI feedback (0ms responsiveness)
        playerWrapper.setPreparing(track)
        ensureServiceStarted()

        repositoryScope.launch {
            if (newContextQueue != null) {
                val startIndex = newContextQueue.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
                queueManager.setContext(newContextQueue, startIndex)
                // Kick off parallel background pre-resolution of upcoming tracks immediately
                val upcoming = newContextQueue.drop(startIndex + 1).take(3)
                upcoming.forEach { resolver.preResolve(it) }
            } else {
                queueManager.setContext(listOf(track), 0)
                queueManager.peekNextTrack()?.let { resolver.preResolve(it) }
            }

            val source = resolver.resolve(track)
            if (source != null) {
                playerWrapper.prepareAndPlay(track, source, playWhenReady = true)
                trackDao.recordTrackPlayed(TrackEntity.fromDomain(track))

                // Also trigger pre-resolution of upcoming track from queue state
                queueManager.peekNextTrack()?.let { nextTrack ->
                    resolver.preResolve(nextTrack)
                }
            }
        }
    }

    override fun togglePlayPause() {
        if (playbackState.value.isPlaying) {
            playerWrapper.pause()
        } else {
            playerWrapper.play()
        }
    }

    override fun pause() {
        playerWrapper.pause()
    }

    override fun resume() {
        playerWrapper.play()
    }

    override fun seekTo(positionMs: Long) {
        playerWrapper.seekTo(positionMs)
    }

    override fun skipToNext() {
        repositoryScope.launch {
            val next = queueManager.nextTrack()
            if (next != null) {
                playerWrapper.setPreparing(next)
                val source = resolver.resolve(next)
                if (source != null) {
                    playerWrapper.prepareAndPlay(next, source, playWhenReady = true)
                    trackDao.recordTrackPlayed(TrackEntity.fromDomain(next))

                    // Pre-resolve following track
                    queueManager.peekNextTrack()?.let { following ->
                        resolver.preResolve(following)
                    }
                }
            } else {
                playerWrapper.seekTo(0L)
                playerWrapper.pause()
            }
        }
    }

    override fun skipToPrevious() {
        repositoryScope.launch {
            val currentPosition = playerWrapper.playbackState.value.positionMs
            when (val result = queueManager.previousTrack(currentPosition)) {
                is PreviousResult.RestartCurrent -> {
                    playerWrapper.seekTo(0L)
                }
                is PreviousResult.PlayTrack -> {
                    playerWrapper.setPreparing(result.track)
                    val source = resolver.resolve(result.track)
                    if (source != null) {
                        playerWrapper.prepareAndPlay(result.track, source, playWhenReady = true)
                        trackDao.recordTrackPlayed(TrackEntity.fromDomain(result.track))
                    }
                }
                is PreviousResult.None -> {
                    playerWrapper.seekTo(0L)
                }
            }
        }
    }

    override fun setVolume(volume: Float) {
        playerWrapper.setVolume(volume)
    }

    override fun toggleShuffle() {
        repositoryScope.launch {
            val enabled = queueManager.toggleShuffle()
            playerWrapper.updateShuffleState(enabled)
        }
    }

    override fun setRepeatMode(mode: RepeatMode) {
        repositoryScope.launch {
            queueManager.setRepeatMode(mode)
            playerWrapper.updateRepeatMode(mode)
        }
    }

    override fun setPlaybackSpeed(speed: Float) {
        playerWrapper.setPlaybackSpeed(speed)
    }

    override fun setSleepTimer(minutes: Int) {
        playerWrapper.setSleepTimer(minutes)
    }
}
