package com.ibramusic.mobile.playback.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import coil.Coil
import coil.request.ImageRequest
import coil.request.SuccessResult
import java.io.File
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.Player
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.MediaStyleNotificationHelper
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.ibramusic.mobile.MainActivity
import com.ibramusic.mobile.R
import com.ibramusic.mobile.core.common.Constants
import com.ibramusic.mobile.data.local.dao.TrackDao
import com.ibramusic.mobile.data.local.entity.TrackEntity
import com.ibramusic.mobile.domain.model.PlaybackState
import com.ibramusic.mobile.domain.model.Track
import com.ibramusic.mobile.domain.repository.SettingsRepository
import com.ibramusic.mobile.playback.engine.ExoPlayerWrapper
import com.ibramusic.mobile.playback.queue.PreviousResult
import com.ibramusic.mobile.playback.queue.QueueManager
import com.ibramusic.mobile.playback.resolver.UnifiedPlaybackResolver
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class IbraMusicPlaybackService : MediaSessionService() {

    @Inject
    lateinit var playerWrapper: ExoPlayerWrapper

    @Inject
    lateinit var queueManager: QueueManager

    @Inject
    lateinit var resolver: UnifiedPlaybackResolver

    @Inject
    lateinit var trackDao: TrackDao

    @Inject
    lateinit var settingsRepository: SettingsRepository

    @Inject
    lateinit var userStatsManager: com.ibramusic.mobile.data.stats.UserStatsManager

    private var mediaSession: MediaSession? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var currentArtworkBitmap: Bitmap? = null
    private var currentArtworkTrackId: String? = null
    private var imageLoadingJob: kotlinx.coroutines.Job? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        val sessionActivityIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            sessionActivityIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // ForwardingPlayer guarantees Android System Media Controls, Lockscreen and Bluetooth
        // always display and execute the Next Track and Previous Track actions.
        val forwardingPlayer = object : ForwardingPlayer(playerWrapper.exoPlayer) {
            override fun getAvailableCommands(): Player.Commands {
                return super.getAvailableCommands().buildUpon()
                    .add(Player.COMMAND_PLAY_PAUSE)
                    .add(Player.COMMAND_SEEK_TO_NEXT)
                    .add(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
                    .add(Player.COMMAND_SEEK_TO_PREVIOUS)
                    .add(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
                    .add(Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM)
                    .build()
            }

            override fun isCommandAvailable(command: Int): Boolean {
                return when (command) {
                    Player.COMMAND_PLAY_PAUSE,
                    Player.COMMAND_SEEK_TO_NEXT,
                    Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM,
                    Player.COMMAND_SEEK_TO_PREVIOUS,
                    Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM,
                    Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM -> true
                    else -> super.isCommandAvailable(command)
                }
            }

            override fun hasNextMediaItem(): Boolean {
                return true
            }

            override fun hasPreviousMediaItem(): Boolean {
                return true
            }

            override fun play() {
                if (playerWrapper.isMediaLoaded()) {
                    super.play()
                } else {
                    serviceScope.launch { resumeRestoredPlayback() }
                }
            }

            override fun seekToNext() {
                serviceScope.launch { skipToNext() }
            }

            override fun seekToNextMediaItem() {
                serviceScope.launch { skipToNext() }
            }

            override fun seekToPrevious() {
                serviceScope.launch { skipToPrevious() }
            }

            override fun seekToPreviousMediaItem() {
                serviceScope.launch { skipToPrevious() }
            }
        }

        val callback = object : MediaSession.Callback {
            override fun onConnect(
                session: MediaSession,
                controller: MediaSession.ControllerInfo
            ): MediaSession.ConnectionResult {
                val sessionCommands = MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS.buildUpon()
                    .build()
                val playerCommands = MediaSession.ConnectionResult.DEFAULT_PLAYER_COMMANDS.buildUpon()
                    .add(Player.COMMAND_PLAY_PAUSE)
                    .add(Player.COMMAND_SEEK_TO_NEXT)
                    .add(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
                    .add(Player.COMMAND_SEEK_TO_PREVIOUS)
                    .add(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
                    .add(Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM)
                    .build()

                return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                    .setAvailableSessionCommands(sessionCommands)
                    .setAvailablePlayerCommands(playerCommands)
                    .build()
            }

            override fun onPlayerCommandRequest(
                session: MediaSession,
                controller: MediaSession.ControllerInfo,
                playerCommand: Int
            ): Int {
                when (playerCommand) {
                    Player.COMMAND_SEEK_TO_NEXT, Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM -> {
                        serviceScope.launch { skipToNext() }
                        return SessionResult.RESULT_SUCCESS
                    }
                    Player.COMMAND_SEEK_TO_PREVIOUS, Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM -> {
                        serviceScope.launch { skipToPrevious() }
                        return SessionResult.RESULT_SUCCESS
                    }
                }
                return super.onPlayerCommandRequest(session, controller, playerCommand)
            }

            override fun onCustomCommand(
                session: MediaSession,
                controller: MediaSession.ControllerInfo,
                customCommand: SessionCommand,
                args: android.os.Bundle
            ): ListenableFuture<SessionResult> {
                return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }
        }

        mediaSession = MediaSession.Builder(this, forwardingPlayer)
            .setSessionActivity(pendingIntent)
            .setCallback(callback)
            .build()

        // Observe Crossfade settings from user preferences
        serviceScope.launch {
            combine(settingsRepository.crossfadeEnabled, settingsRepository.crossfadeDurationSeconds) { enabled, duration ->
                playerWrapper.setCrossfadeSettings(enabled, duration)
            }.collect()
        }

        // Standard track end fallback (when crossfade is disabled or not triggered)
        playerWrapper.setOnPlaybackEndedListener {
            playerWrapper.playbackState.value.currentTrack?.let { userStatsManager.recordPlayCompleted(it) }
            serviceScope.launch {
                skipToNext()
            }
        }

        // Early Standby Pre-cueing: 25s before current track ends, prime standby deck at volume 0
        playerWrapper.setOnPreCueNeededListener {
            val nextTrack = queueManager.peekNextTrack()
            if (nextTrack != null) {
                serviceScope.launch(Dispatchers.IO) {
                    val source = resolver.resolve(nextTrack)
                    if (source != null) {
                        playerWrapper.preCueStandby(nextTrack, source)
                    }
                }
            }
        }

        // Simultaneous Crossfade Trigger: smoothly ramps outgoing & incoming decks simultaneously
        playerWrapper.setOnCrossfadeTriggerListener {
            val nextTrack = queueManager.peekNextTrack()
            if (nextTrack != null) {
                serviceScope.launch {
                    val source = resolver.resolve(nextTrack)
                    if (source != null) {
                        queueManager.nextTrack()
                        playerWrapper.performSimultaneousCrossfade(nextTrack, source) {
                            serviceScope.launch {
                                trackDao.recordTrackPlayed(TrackEntity.fromDomain(nextTrack))
                                userStatsManager.recordPlayCompleted(nextTrack)
                            }
                            queueManager.peekNextTrack()?.let { resolver.preResolve(it) }
                        }
                    }
                }
            }
        }

        // Reconnect forwarding player whenever active deck flips
        playerWrapper.setOnActivePlayerChangedListener { newActivePlayer ->
            mediaSession?.player = forwardingPlayer
        }

        // Proactive Queue Pre-Resolution Watcher (Matching ibramusic AudioContext.tsx)
        // Continuously resolves upcoming tracks into memory cache so transitions and crossfades are 0ms instant
        serviceScope.launch {
            combine(
                playerWrapper.playbackState.map { it.currentTrack?.id },
                queueManager.queueState.map { q ->
                    (q.userQueue.map { it.track.id } + q.playlistQueue.map { it.track.id }).take(4)
                }
            ) { currentId, upcomingIds ->
                currentId to upcomingIds
            }.distinctUntilChanged().collect { (currentId, _) ->
                if (currentId != null) {
                    val next = queueManager.peekNextTrack()
                    if (next != null) {
                        resolver.preResolve(next)
                    }
                    val queue = queueManager.queueState.value
                    val upcoming = (queue.userQueue.map { it.track } + queue.playlistQueue.map { it.track })
                    upcoming.take(3).forEach { track ->
                        resolver.preResolve(track)
                    }
                }
            }
        }

        // Periodic listening stats tracker (records seconds listened while playing)
        serviceScope.launch {
            while (true) {
                kotlinx.coroutines.delay(5000L)
                val state = playerWrapper.playbackState.value
                val track = state.currentTrack
                if (state.isPlaying && track != null) {
                    userStatsManager.recordListeningTime(track, 5L)
                }
            }
        }

        // Start foreground immediately to satisfy Android background limits
        val initialNotification = buildNotification(playerWrapper.playbackState.value)
        startForeground(Constants.NOTIFICATION_ID, initialNotification)

        serviceScope.launch {
            var lastTrackId: String? = null
            var lastIsPlaying: Boolean? = null
            playerWrapper.playbackState.collect { state ->
                val trackChanged = state.currentTrack?.id != lastTrackId
                val playChanged = state.isPlaying != lastIsPlaying
                if (trackChanged || playChanged) {
                    lastTrackId = state.currentTrack?.id
                    lastIsPlaying = state.isPlaying
                    val notification = buildNotification(state)
                    val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    notificationManager.notify(Constants.NOTIFICATION_ID, notification)
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "ACTION_PLAY_PAUSE" -> {
                if (playerWrapper.playbackState.value.isPlaying) {
                    playerWrapper.pause()
                } else {
                    if (playerWrapper.isMediaLoaded()) {
                        playerWrapper.play()
                    } else {
                        serviceScope.launch { resumeRestoredPlayback() }
                    }
                }
            }
            "ACTION_NEXT" -> {
                serviceScope.launch { skipToNext() }
            }
            "ACTION_PREV" -> {
                serviceScope.launch { skipToPrevious() }
            }
        }

        val notification = buildNotification(playerWrapper.playbackState.value)
        startForeground(Constants.NOTIFICATION_ID, notification)

        return super.onStartCommand(intent, flags, startId)
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = playerWrapper.activePlayer
        if (!player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    private suspend fun resumeRestoredPlayback() {
        val current = playerWrapper.playbackState.value.currentTrack ?: return
        val resumePosition = playerWrapper.playbackState.value.positionMs
        val startPos = if (current.durationMs > 5000L && resumePosition >= current.durationMs - 2000L) {
            0L
        } else {
            resumePosition
        }

        playerWrapper.setPreparing(current)
        val source = resolver.resolve(current)
        if (source != null) {
            playerWrapper.prepareAndPlay(
                track = current,
                source = source,
                playWhenReady = true,
                startPositionMs = startPos
            )
            trackDao.recordTrackPlayed(TrackEntity.fromDomain(current))
            queueManager.peekNextTrack()?.let { resolver.preResolve(it) }
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    private fun updateArtworkForTrack(track: Track?) {
        if (track == null) {
            currentArtworkBitmap = null
            currentArtworkTrackId = null
            imageLoadingJob?.cancel()
            return
        }
        if (track.id == currentArtworkTrackId && currentArtworkBitmap != null) {
            return
        }
        currentArtworkTrackId = track.id
        imageLoadingJob?.cancel()

        // 1. Try local downloaded thumbnail file synchronously
        val localThumbFile = resolveLocalArtworkFile(track)
        if (localThumbFile != null && localThumbFile.exists()) {
            try {
                val decoded = BitmapFactory.decodeFile(localThumbFile.absolutePath)
                if (decoded != null) {
                    currentArtworkBitmap = decoded
                    return
                }
            } catch (e: Exception) {
                // Fall back to async load
            }
        }

        // 2. Fetch remote image asynchronously via Coil
        val artUrl = track.artworkUrl ?: track.album?.artworkUrl
        if (!artUrl.isNullOrEmpty()) {
            imageLoadingJob = serviceScope.launch(Dispatchers.IO) {
                try {
                    val request = ImageRequest.Builder(this@IbraMusicPlaybackService)
                        .data(artUrl)
                        .allowHardware(false)
                        .build()
                    val result = Coil.imageLoader(this@IbraMusicPlaybackService).execute(request)
                    if (result is SuccessResult) {
                        val bitmap = (result.drawable as? BitmapDrawable)?.bitmap
                        if (bitmap != null && currentArtworkTrackId == track.id) {
                            currentArtworkBitmap = bitmap
                            val notification = buildNotification(playerWrapper.playbackState.value)
                            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                            notificationManager.notify(Constants.NOTIFICATION_ID, notification)
                        }
                    }
                } catch (e: Exception) {
                    // Ignore image load failure
                }
            }
        } else {
            currentArtworkBitmap = null
        }
    }

    private fun resolveLocalArtworkFile(track: Track): File? {
        val raw = track.artworkUrl ?: track.album?.artworkUrl
        if (raw != null) {
            if (raw.startsWith("file:")) {
                val path = Uri.parse(raw).path
                if (path != null) {
                    val file = File(path)
                    if (file.exists()) return file
                }
            } else if (raw.startsWith("/")) {
                val file = File(raw)
                if (file.exists()) return file
            }
        }
        val defaultThumb = File(filesDir, "downloads/${track.id}.jpg")
        if (defaultThumb.exists()) {
            return defaultThumb
        }
        return null
    }

    private fun buildNotification(state: PlaybackState): Notification {
        val currentTrack = state.currentTrack
        val isPlaying = state.isPlaying

        updateArtworkForTrack(currentTrack)

        val prevIntent = PendingIntent.getService(
            this, 10,
            Intent(this, IbraMusicPlaybackService::class.java).setAction("ACTION_PREV"),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val playPauseIntent = PendingIntent.getService(
            this, 11,
            Intent(this, IbraMusicPlaybackService::class.java).setAction("ACTION_PLAY_PAUSE"),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val nextIntent = PendingIntent.getService(
            this, 12,
            Intent(this, IbraMusicPlaybackService::class.java).setAction("ACTION_NEXT"),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val contentIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(this, Constants.NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_play)
            .setContentTitle(currentTrack?.title ?: "IbraMusic")
            .setContentText(currentTrack?.artistName ?: "Ready to play")
            .setContentIntent(contentIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(isPlaying)
            .addAction(R.drawable.ic_notification_skip_prev, "Previous", prevIntent)
            .addAction(
                if (isPlaying) R.drawable.ic_notification_pause else R.drawable.ic_notification_play,
                if (isPlaying) "Pause" else "Play",
                playPauseIntent
            )
            .addAction(R.drawable.ic_notification_skip_next, "Next", nextIntent)

        currentArtworkBitmap?.let {
            builder.setLargeIcon(it)
        }

        mediaSession?.let { session ->
            builder.setStyle(
                MediaStyleNotificationHelper.MediaStyle(session)
                    .setShowActionsInCompactView(0, 1, 2)
            )
        }

        return builder.build()
    }

    suspend fun playTrack(track: Track, newContextQueue: List<Track>? = null) {
        playerWrapper.setPreparing(track)
        if (newContextQueue != null) {
            val startIndex = newContextQueue.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
            queueManager.setContext(newContextQueue, startIndex)
        }

        val source = resolver.resolve(track)
        if (source != null) {
            playerWrapper.prepareAndPlay(track, source, playWhenReady = true)
            trackDao.recordTrackPlayed(TrackEntity.fromDomain(track))

            // Pre-resolve next track from queue
            queueManager.peekNextTrack()?.let { resolver.preResolve(it) }
        }
    }

    suspend fun skipToNext() {
        val next = queueManager.nextTrack()
        if (next != null) {
            playerWrapper.setPreparing(next)
            val source = resolver.resolve(next)
            if (source != null) {
                playerWrapper.prepareAndPlay(next, source, playWhenReady = true)
                trackDao.recordTrackPlayed(TrackEntity.fromDomain(next))

                // Pre-resolve track after this one
                queueManager.peekNextTrack()?.let { resolver.preResolve(it) }
            }
        } else {
            playerWrapper.pause()
        }
    }

    suspend fun skipToPrevious() {
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

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                Constants.NOTIFICATION_CHANNEL_ID,
                Constants.NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "IbraMusic background playback controls"
                setShowBadge(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }
}
