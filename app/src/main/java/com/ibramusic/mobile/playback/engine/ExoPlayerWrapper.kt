package com.ibramusic.mobile.playback.engine

import android.content.Context
import android.net.Uri
import android.util.Log
import java.io.File
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.ibramusic.mobile.domain.model.PlaybackSource
import com.ibramusic.mobile.domain.model.PlaybackState
import com.ibramusic.mobile.domain.model.RepeatMode
import com.ibramusic.mobile.domain.model.Track
import com.ibramusic.mobile.playback.audiofx.EqualizerManager
import com.ibramusic.mobile.playback.session.PlaybackSessionManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.pow

@Singleton
class ExoPlayerWrapper @Inject constructor(
    @ApplicationContext private val context: Context,
    val equalizerManager: EqualizerManager,
    private val sessionManager: PlaybackSessionManager
) {
    companion object {
        private const val TAG = "ExoPlayerWrapper"
    }

    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var positionTickerJob: Job? = null
    private var crossfadeJob: Job? = null
    private var sleepTimerJob: Job? = null
    private var lastPersistTimeMs: Long = 0L

    private val _sleepTimerMinutesLeft = MutableStateFlow<Int?>(null)
    val sleepTimerMinutesLeft: StateFlow<Int?> = _sleepTimerMinutesLeft.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    // Dual-Deck Player Architecture (Spotify-Style Seamless Crossfade)
    private var activeDeckIndex: Int = 0
    private var isTransitioning: Boolean = false
    private var preCuedTrack: Track? = null
    private var isStandbyReady: Boolean = false
    private var hasPreCuedForCurrentTrack: Boolean = false
    private var hasTriggeredCrossfadeForCurrentTrack: Boolean = false

    var isCrossfadeEnabled: Boolean = true
        private set
    var crossfadeDurationMs: Long = 4000L
        private set

    fun setCrossfadeSettings(enabled: Boolean, durationSeconds: Int) {
        isCrossfadeEnabled = enabled
        crossfadeDurationMs = durationSeconds.coerceIn(1, 12) * 1000L
        Log.d(TAG, "Crossfade settings updated: enabled=$enabled, duration=${crossfadeDurationMs}ms")
    }

    private fun createPlayerInstance(deckName: String): ExoPlayer {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()

        return ExoPlayer.Builder(context)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .build()
            .apply {
                addListener(createPlayerListener(deckName))
            }
    }

    private val deck0: ExoPlayer by lazy { createPlayerInstance("Deck0") }
    private val deck1: ExoPlayer by lazy { createPlayerInstance("Deck1") }

    val activePlayer: ExoPlayer get() = if (activeDeckIndex == 0) deck0 else deck1
    val standbyPlayer: ExoPlayer get() = if (activeDeckIndex == 0) deck1 else deck0

    // Backward-compatible reference for MediaSession & ForwardingPlayer
    val exoPlayer: ExoPlayer get() = activePlayer

    private var currentTrack: Track? = null
    private var userVolume: Float = 1.0f

    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private var onPlaybackEndedCallback: (() -> Unit)? = null
    private var onPreCueNeededCallback: (() -> Unit)? = null
    private var onCrossfadeTriggerCallback: (() -> Unit)? = null
    private var onActivePlayerChangedCallback: ((Player) -> Unit)? = null

    fun setOnPlaybackEndedListener(listener: () -> Unit) {
        onPlaybackEndedCallback = listener
    }

    fun setOnPreCueNeededListener(listener: () -> Unit) {
        onPreCueNeededCallback = listener
    }

    fun setOnCrossfadeTriggerListener(listener: () -> Unit) {
        onCrossfadeTriggerCallback = listener
    }

    fun setOnActivePlayerChangedListener(listener: (Player) -> Unit) {
        onActivePlayerChangedCallback = listener
    }

    private fun createPlayerListener(deckName: String) = object : Player.Listener {
        override fun onPlaybackStateChanged(state: Int) {
            val isCurrentActive = (deckName == "Deck0" && activeDeckIndex == 0) || (deckName == "Deck1" && activeDeckIndex == 1)
            if (!isCurrentActive && !isTransitioning) {
                if (state == Player.STATE_READY) {
                    isStandbyReady = true
                    Log.d(TAG, "[$deckName] Standby deck primed and ready to play.")
                }
                return
            }

            when (state) {
                Player.STATE_BUFFERING -> {
                    _playbackState.value = _playbackState.value.copy(
                        isBuffering = true,
                        playbackError = null
                    )
                }
                Player.STATE_READY -> {
                    val p = activePlayer
                    val duration = if (p.duration > 0) p.duration else (currentTrack?.durationMs ?: 0L)
                    _playbackState.value = _playbackState.value.copy(
                        isBuffering = false,
                        durationMs = duration,
                        positionMs = p.currentPosition,
                        playbackError = null
                    )
                }
                Player.STATE_ENDED -> {
                    if (!isTransitioning) {
                        _playbackState.value = _playbackState.value.copy(
                            isPlaying = false,
                            isBuffering = false
                        )
                        stopPositionTicker()
                        onPlaybackEndedCallback?.invoke()
                    }
                }
                Player.STATE_IDLE -> {
                    if (isCurrentActive && !isTransitioning) {
                        _playbackState.value = _playbackState.value.copy(
                            isPlaying = false,
                            isBuffering = false
                        )
                        stopPositionTicker()
                    }
                }
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            val isCurrentActive = (deckName == "Deck0" && activeDeckIndex == 0) || (deckName == "Deck1" && activeDeckIndex == 1)
            if (!isCurrentActive) return

            _playbackState.value = _playbackState.value.copy(
                isPlaying = isPlaying,
                positionMs = activePlayer.currentPosition
            )
            if (isPlaying) {
                startPositionTicker()
            } else {
                stopPositionTicker()
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            val isCurrentActive = (deckName == "Deck0" && activeDeckIndex == 0) || (deckName == "Deck1" && activeDeckIndex == 1)
            if (!isCurrentActive) return

            val message = error.message ?: "Playback error: ${error.errorCodeName}"
            _playbackState.value = _playbackState.value.copy(
                isPlaying = false,
                isBuffering = false,
                playbackError = message
            )
            stopPositionTicker()
        }

        override fun onPositionDiscontinuity(
            oldPosition: Player.PositionInfo,
            newPosition: Player.PositionInfo,
            reason: Int
        ) {
            val isCurrentActive = (deckName == "Deck0" && activeDeckIndex == 0) || (deckName == "Deck1" && activeDeckIndex == 1)
            if (!isCurrentActive) return

            _playbackState.value = _playbackState.value.copy(
                positionMs = newPosition.positionMs
            )
        }
    }

    private inline fun runOnMain(crossinline block: () -> Unit) {
        if (android.os.Looper.myLooper() == android.os.Looper.getMainLooper()) {
            block()
        } else {
            scope.launch(Dispatchers.Main) {
                block()
            }
        }
    }

    fun isMediaLoaded(): Boolean {
        val active = activePlayer
        return active.currentMediaItem != null && active.playbackState != Player.STATE_IDLE
    }

    fun restoreSession(
        track: Track,
        positionMs: Long,
        durationMs: Long,
        repeatMode: RepeatMode = RepeatMode.OFF,
        shuffleEnabled: Boolean = false
    ) = runOnMain {
        currentTrack = track
        val safeDuration = if (durationMs > 0) durationMs else track.durationMs
        val safePosition = positionMs.coerceIn(0L, safeDuration)
        _playbackState.value = _playbackState.value.copy(
            currentTrack = track,
            positionMs = safePosition,
            durationMs = safeDuration,
            isPlaying = false,
            isBuffering = false,
            playbackError = null,
            repeatMode = repeatMode,
            shuffleEnabled = shuffleEnabled
        )
    }

    fun setPreparing(track: Track) {
        currentTrack = track
        _playbackState.value = _playbackState.value.copy(
            currentTrack = track,
            isBuffering = true,
            playbackError = null
        )
    }

    /**
     * Prepares and immediately plays a track on the active deck.
     * Cancels any active crossfade transitions and silences standby player.
     */
    fun prepareAndPlay(
        track: Track,
        source: PlaybackSource,
        playWhenReady: Boolean = true,
        startPositionMs: Long = 0L
    ) = runOnMain {
        cancelCrossfade()
        currentTrack = track
        hasPreCuedForCurrentTrack = false
        hasTriggeredCrossfadeForCurrentTrack = false

        // Silence standby deck completely
        val standby = standbyPlayer
        standby.stop()
        standby.clearMediaItems()
        standby.volume = 0f
        preCuedTrack = null
        isStandbyReady = false

        val active = activePlayer
        val metadata = buildMediaMetadata(track)

        val mediaItem = MediaItem.Builder()
            .setMediaId(track.id)
            .setUri(source.uri)
            .setMediaMetadata(metadata)
            .build()

        setVolume(userVolume)
        active.setMediaItem(mediaItem, startPositionMs)
        active.prepare()
        active.playWhenReady = playWhenReady
        equalizerManager.attachAudioSession(active.audioSessionId)

        _playbackState.value = _playbackState.value.copy(
            currentTrack = track,
            positionMs = startPositionMs,
            durationMs = track.durationMs,
            playbackError = null
        )

        sessionManager.saveSession(
            trackId = track.id,
            positionMs = startPositionMs,
            durationMs = track.durationMs,
            shuffle = _playbackState.value.shuffleEnabled,
            repeatMode = _playbackState.value.repeatMode
        )

        onActivePlayerChangedCallback?.invoke(active)
    }

    /**
     * Pre-cues the standby player ahead of time with zero volume so audio buffer is primed.
     */
    fun preCueStandby(track: Track, source: PlaybackSource) = runOnMain {
        if (preCuedTrack?.id == track.id && isStandbyReady) return@runOnMain
        preCuedTrack = track
        isStandbyReady = false

        val standby = standbyPlayer
        val metadata = buildMediaMetadata(track)

        val mediaItem = MediaItem.Builder()
            .setMediaId(track.id)
            .setUri(source.uri)
            .setMediaMetadata(metadata)
            .build()

        standby.volume = 0f
        standby.setMediaItem(mediaItem, 0L)
        standby.prepare()
        standby.playWhenReady = false
        Log.d(TAG, "Pre-cued standby deck with upcoming track: ${track.artistName} - ${track.title}")
    }

    private fun buildMediaMetadata(track: Track): MediaMetadata {
        val artworkUri = resolveArtworkUri(track)
        val artworkBytes = resolveArtworkBytes(track)

        return MediaMetadata.Builder()
            .setTitle(track.title)
            .setArtist(track.artistName)
            .setAlbumTitle(track.album?.title)
            .setArtworkUri(artworkUri)
            .apply {
                if (artworkBytes != null && artworkBytes.isNotEmpty()) {
                    setArtworkData(artworkBytes, MediaMetadata.PICTURE_TYPE_FRONT_COVER)
                }
            }
            .build()
    }

    private fun resolveArtworkUri(track: Track): Uri? {
        val raw = track.artworkUrl ?: return null
        return try {
            if (raw.startsWith("file:")) {
                val path = Uri.parse(raw).path
                if (path != null) {
                    Uri.fromFile(File(path))
                } else {
                    Uri.parse(raw)
                }
            } else {
                Uri.parse(raw)
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun resolveArtworkBytes(track: Track): ByteArray? {
        // 1. Check if track has a local file path in artworkUrl
        val raw = track.artworkUrl
        if (!raw.isNullOrEmpty() && raw.startsWith("file:")) {
            try {
                val path = Uri.parse(raw).path
                if (path != null) {
                    val f = File(path)
                    if (f.exists() && f.length() > 0) {
                        return f.readBytes()
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error reading artwork file: ${e.message}")
            }
        }
        // 2. Check if downloaded thumbnail exists in downloads directory
        try {
            val downloadedThumb = File(context.filesDir, "downloads/${track.id}.jpg")
            if (downloadedThumb.exists() && downloadedThumb.length() > 0) {
                return downloadedThumb.readBytes()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error reading downloaded thumb: ${e.message}")
        }
        return null
    }

    /**
     * Performs a simultaneous crossfade between the outgoing deck and incoming standby deck.
     */
    fun performSimultaneousCrossfade(
        nextTrack: Track,
        nextSource: PlaybackSource,
        durationMs: Long = crossfadeDurationMs,
        onComplete: () -> Unit
    ) = runOnMain {
        if (isTransitioning) return@runOnMain
        isTransitioning = true

        val outgoing = activePlayer
        val incoming = standbyPlayer

        // If not already pre-cued, prime incoming standby deck immediately
        if (preCuedTrack?.id != nextTrack.id) {
            preCueStandby(nextTrack, nextSource)
        }

        incoming.playWhenReady = true
        incoming.play()

        crossfadeJob?.cancel()
        crossfadeJob = scope.launch {
            val totalSteps = (durationMs / 50).toInt().coerceAtLeast(10)
            val stepDelay = durationMs / totalSteps
            val maxVol = userVolume

            for (i in 0..totalSteps) {
                val progress = i.toFloat() / totalSteps
                // Smooth cubic curves matching human perception
                val outgoingVol = (maxVol * (1f - progress).pow(2)).coerceAtLeast(0f)
                val incomingVol = (maxVol * progress.pow(2)).coerceAtLeast(0f)

                outgoing.volume = outgoingVol
                incoming.volume = incomingVol

                delay(stepDelay)
            }

            // Crossfade complete: switch active deck
            outgoing.stop()
            outgoing.clearMediaItems()
            outgoing.volume = 0f

            activeDeckIndex = 1 - activeDeckIndex
            currentTrack = nextTrack
            preCuedTrack = null
            isStandbyReady = false
            hasPreCuedForCurrentTrack = false
            hasTriggeredCrossfadeForCurrentTrack = false
            isTransitioning = false

            // Ensure new active deck is at exact target volume
            val newActive = activePlayer
            newActive.volume = userVolume
            equalizerManager.attachAudioSession(newActive.audioSessionId)

            _playbackState.value = _playbackState.value.copy(
                currentTrack = nextTrack,
                durationMs = nextTrack.durationMs,
                positionMs = newActive.currentPosition,
                isPlaying = true,
                isBuffering = false
            )

            onActivePlayerChangedCallback?.invoke(newActive)
            onComplete()
            Log.d(TAG, "Simultaneous crossfade completed successfully to: ${nextTrack.title}")
        }
    }

    private fun cancelCrossfade() {
        if (isTransitioning) {
            crossfadeJob?.cancel()
            crossfadeJob = null
            isTransitioning = false
            hasTriggeredCrossfadeForCurrentTrack = false
        }
    }

    fun play() = runOnMain {
        activePlayer.play()
    }

    fun pause() = runOnMain {
        activePlayer.pause()
        standbyPlayer.pause()
        currentTrack?.let {
            sessionManager.updatePosition(activePlayer.currentPosition, it.durationMs)
        }
    }

    fun seekTo(positionMs: Long) = runOnMain {
        val safePosition = positionMs.coerceAtLeast(0L)
        if (isMediaLoaded()) {
            activePlayer.seekTo(safePosition)
        }
        _playbackState.value = _playbackState.value.copy(
            positionMs = safePosition
        )
        currentTrack?.let {
            sessionManager.updatePosition(safePosition, it.durationMs)
        }
    }

    fun setVolume(volume: Float) = runOnMain {
        userVolume = volume.coerceIn(0.0f, 1.0f)
        if (!isTransitioning) {
            activePlayer.volume = userVolume
        }
        _playbackState.value = _playbackState.value.copy(volume = userVolume)
    }

    fun fadeOut(durationMs: Long = 400L, onComplete: () -> Unit) {
        crossfadeJob?.cancel()
        crossfadeJob = scope.launch {
            val steps = 20
            val interval = durationMs / steps
            val startVol = userVolume

            for (i in steps downTo 0) {
                val factor = i.toFloat() / steps
                val shaped = (startVol * factor).pow(3).coerceAtLeast(0.001f)
                activePlayer.volume = shaped
                delay(interval)
            }
            onComplete()
            setVolume(userVolume)
        }
    }

    fun setPlaybackSpeed(speed: Float) {
        _playbackSpeed.value = speed
        val params = androidx.media3.common.PlaybackParameters(speed)
        deck0.setPlaybackParameters(params)
        deck1.setPlaybackParameters(params)
    }

    fun setSleepTimer(minutes: Int) {
        sleepTimerJob?.cancel()
        if (minutes <= 0) {
            _sleepTimerMinutesLeft.value = null
            return
        }
        sleepTimerJob = scope.launch {
            var minutesLeft = minutes
            while (minutesLeft > 0 && isActive) {
                _sleepTimerMinutesLeft.value = minutesLeft
                delay(60_000L)
                minutesLeft--
            }
            _sleepTimerMinutesLeft.value = null
            pause()
        }
    }

    fun updateRepeatMode(mode: RepeatMode) {
        _playbackState.value = _playbackState.value.copy(repeatMode = mode)
        sessionManager.saveSession(
            trackId = currentTrack?.id,
            positionMs = _playbackState.value.positionMs,
            durationMs = _playbackState.value.durationMs,
            shuffle = _playbackState.value.shuffleEnabled,
            repeatMode = mode
        )
    }

    fun updateShuffleState(enabled: Boolean) {
        _playbackState.value = _playbackState.value.copy(shuffleEnabled = enabled)
        sessionManager.saveSession(
            trackId = currentTrack?.id,
            positionMs = _playbackState.value.positionMs,
            durationMs = _playbackState.value.durationMs,
            shuffle = enabled,
            repeatMode = _playbackState.value.repeatMode
        )
    }

    private fun startPositionTicker() {
        stopPositionTicker()
        positionTickerJob = scope.launch {
            while (isActive) {
                val p = activePlayer
                if (p.isPlaying) {
                    val pos = p.currentPosition
                    val dur = if (p.duration > 0) p.duration else (currentTrack?.durationMs ?: 0L)
                    _playbackState.value = _playbackState.value.copy(
                        positionMs = pos,
                        durationMs = dur
                    )

                    val now = System.currentTimeMillis()
                    if (now - lastPersistTimeMs >= 1000L) {
                        lastPersistTimeMs = now
                        sessionManager.updatePosition(pos, dur)
                    }

                    // Spotify-style Simultaneous Crossfade Detection:
                    // 1. Pre-cue standby deck 25 seconds before track ends
                    // 2. Trigger crossfade when remaining time <= crossfadeDurationMs
                    if (isCrossfadeEnabled && !isTransitioning && dur > 10_000L && pos > 0L) {
                        val remainingMs = dur - pos
                        if (remainingMs in (crossfadeDurationMs + 500L)..30_000L) {
                            if (!hasPreCuedForCurrentTrack) {
                                hasPreCuedForCurrentTrack = true
                                Log.d(TAG, "Triggering one-time pre-cue for upcoming track (remaining: ${remainingMs}ms)")
                                onPreCueNeededCallback?.invoke()
                            }
                        } else if (remainingMs <= crossfadeDurationMs && remainingMs > 300L) {
                            if (!hasTriggeredCrossfadeForCurrentTrack) {
                                hasTriggeredCrossfadeForCurrentTrack = true
                                Log.d(TAG, "Triggering one-time crossfade transition (remaining: ${remainingMs}ms)")
                                onCrossfadeTriggerCallback?.invoke()
                            }
                        }
                    }
                }
                delay(300L)
            }
        }
    }

    private fun stopPositionTicker() {
        positionTickerJob?.cancel()
        positionTickerJob = null
    }

    fun release() {
        stopPositionTicker()
        crossfadeJob?.cancel()
        deck0.release()
        deck1.release()
    }
}
