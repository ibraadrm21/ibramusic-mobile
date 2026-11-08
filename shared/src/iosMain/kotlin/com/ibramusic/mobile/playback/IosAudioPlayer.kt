package com.ibramusic.mobile.playback

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryPlayback
import platform.AVFAudio.setActive
import platform.AVFoundation.AVPlayer
import platform.AVFoundation.AVPlayerItem
import platform.AVFoundation.AVPlayerItemDidPlayToEndTimeNotification
import platform.AVFoundation.currentTime
import platform.AVFoundation.duration
import platform.AVFoundation.pause
import platform.AVFoundation.play
import platform.AVFoundation.seekToTime
import platform.CoreMedia.CMTimeGetSeconds
import platform.CoreMedia.CMTimeMakeWithSeconds
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSURL
import platform.MediaPlayer.MPMediaItemPropertyArtist
import platform.MediaPlayer.MPMediaItemPropertyPlaybackDuration
import platform.MediaPlayer.MPMediaItemPropertyTitle
import platform.MediaPlayer.MPNowPlayingInfoCenter
import platform.MediaPlayer.MPNowPlayingInfoPropertyElapsedPlaybackTime
import platform.MediaPlayer.MPNowPlayingInfoPropertyPlaybackRate
import platform.MediaPlayer.MPRemoteCommandCenter
import platform.MediaPlayer.MPRemoteCommandHandlerStatusSuccess

data class IosPlaybackState(
    val title: String? = null,
    val artist: String? = null,
    val artworkUrl: String? = null,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L
)

class IosAudioPlayer {

    private var player: AVPlayer? = null
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var tickerJob: Job? = null

    private val _state = MutableStateFlow(IosPlaybackState())
    val state: StateFlow<IosPlaybackState> = _state.asStateFlow()

    var onTrackEnded: (() -> Unit)? = null
    var onNextTrack: (() -> Unit)? = null
    var onPrevTrack: (() -> Unit)? = null

    init {
        setupAudioSession()
        setupRemoteCommands()
    }

    private fun setupAudioSession() {
        try {
            val session = AVAudioSession.sharedInstance()
            session.setCategory(AVAudioSessionCategoryPlayback, error = null)
            session.setActive(true, error = null)
        } catch (e: Exception) {
            // Audio session setup
        }
    }

    private fun setupRemoteCommands() {
        val commandCenter = MPRemoteCommandCenter.sharedCommandCenter()

        commandCenter.playCommand.addTargetWithHandler {
            play()
            MPRemoteCommandHandlerStatusSuccess
        }

        commandCenter.pauseCommand.addTargetWithHandler {
            pause()
            MPRemoteCommandHandlerStatusSuccess
        }

        commandCenter.togglePlayPauseCommand.addTargetWithHandler {
            if (_state.value.isPlaying) pause() else play()
            MPRemoteCommandHandlerStatusSuccess
        }

        commandCenter.nextTrackCommand.addTargetWithHandler {
            onNextTrack?.invoke()
            MPRemoteCommandHandlerStatusSuccess
        }

        commandCenter.previousTrackCommand.addTargetWithHandler {
            onPrevTrack?.invoke()
            MPRemoteCommandHandlerStatusSuccess
        }

        commandCenter.changePlaybackPositionCommand.addTargetWithHandler { event ->
            val positionEvent = event as? platform.MediaPlayer.MPChangePlaybackPositionCommandEvent
            if (positionEvent != null) {
                seekTo((positionEvent.positionTime * 1000.0).toLong())
            }
            MPRemoteCommandHandlerStatusSuccess
        }
    }

    fun prepareAndPlay(
        url: String,
        title: String,
        artist: String,
        artworkUrl: String? = null,
        durationMs: Long = 0L,
        startPositionMs: Long = 0L
    ) {
        val nsUrl = NSURL.URLWithString(url) ?: return
        val playerItem = AVPlayerItem(uRL = nsUrl)

        NSNotificationCenter.defaultCenter.addObserverForName(
            name = AVPlayerItemDidPlayToEndTimeNotification,
            `object` = playerItem,
            queue = null
        ) { _ ->
            _state.value = _state.value.copy(isPlaying = false)
            stopTicker()
            onTrackEnded?.invoke()
        }

        player = AVPlayer(playerItem = playerItem)

        if (startPositionMs > 0L) {
            val targetTime = CMTimeMakeWithSeconds(startPositionMs / 1000.0, 1000)
            player?.seekToTime(targetTime)
        }

        player?.play()

        _state.value = IosPlaybackState(
            title = title,
            artist = artist,
            artworkUrl = artworkUrl,
            isPlaying = true,
            isBuffering = false,
            positionMs = startPositionMs,
            durationMs = durationMs
        )

        updateNowPlayingInfo(title, artist, durationMs, startPositionMs, isPlaying = true)
        startTicker()
    }

    fun play() {
        player?.play()
        _state.value = _state.value.copy(isPlaying = true)
        updateNowPlayingInfo(
            _state.value.title.orEmpty(),
            _state.value.artist.orEmpty(),
            _state.value.durationMs,
            _state.value.positionMs,
            isPlaying = true
        )
        startTicker()
    }

    fun pause() {
        player?.pause()
        _state.value = _state.value.copy(isPlaying = false)
        updateNowPlayingInfo(
            _state.value.title.orEmpty(),
            _state.value.artist.orEmpty(),
            _state.value.durationMs,
            _state.value.positionMs,
            isPlaying = false
        )
        stopTicker()
    }

    fun seekTo(positionMs: Long) {
        val safePos = positionMs.coerceAtLeast(0L)
        val targetTime = CMTimeMakeWithSeconds(safePos / 1000.0, 1000)
        player?.seekToTime(targetTime)
        _state.value = _state.value.copy(positionMs = safePos)
        updateNowPlayingInfo(
            _state.value.title.orEmpty(),
            _state.value.artist.orEmpty(),
            _state.value.durationMs,
            safePos,
            isPlaying = _state.value.isPlaying
        )
    }

    private fun updateNowPlayingInfo(
        title: String,
        artist: String,
        durationMs: Long,
        positionMs: Long,
        isPlaying: Boolean
    ) {
        val info = mutableMapOf<Any?, Any?>()
        info[MPMediaItemPropertyTitle] = title
        info[MPMediaItemPropertyArtist] = artist
        info[MPMediaItemPropertyPlaybackDuration] = durationMs / 1000.0
        info[MPNowPlayingInfoPropertyElapsedPlaybackTime] = positionMs / 1000.0
        info[MPNowPlayingInfoPropertyPlaybackRate] = if (isPlaying) 1.0 else 0.0

        MPNowPlayingInfoCenter.defaultCenter().nowPlayingInfo = info
    }

    private fun startTicker() {
        stopTicker()
        tickerJob = scope.launch {
            while (isActive) {
                val p = player
                if (p != null) {
                    val currentSec = CMTimeGetSeconds(p.currentTime())
                    val durSec = CMTimeGetSeconds(p.currentItem?.duration ?: p.currentTime())
                    val posMs = if (currentSec.isFinite()) (currentSec * 1000.0).toLong() else 0L
                    val durMs = if (durSec.isFinite() && durSec > 0.0) (durSec * 1000.0).toLong() else _state.value.durationMs

                    _state.value = _state.value.copy(
                        positionMs = posMs,
                        durationMs = durMs
                    )
                }
                delay(500L)
            }
        }
    }

    private fun stopTicker() {
        tickerJob?.cancel()
        tickerJob = null
    }

    fun release() {
        stopTicker()
        player?.pause()
        player = null
    }
}

