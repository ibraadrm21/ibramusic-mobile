package com.ibramusic.mobile.feature.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ibramusic.mobile.core.network.LyricLine
import com.ibramusic.mobile.core.network.LyricsService
import com.ibramusic.mobile.domain.model.PlaybackState
import com.ibramusic.mobile.domain.model.Playlist
import com.ibramusic.mobile.domain.model.RepeatMode
import com.ibramusic.mobile.domain.model.Track
import com.ibramusic.mobile.domain.repository.DownloadRepository
import com.ibramusic.mobile.domain.repository.PlaybackRepository
import com.ibramusic.mobile.domain.usecase.AddTrackToPlaylistUseCase
import com.ibramusic.mobile.domain.usecase.CreatePlaylistUseCase
import com.ibramusic.mobile.domain.usecase.GetPlaylistsUseCase
import com.ibramusic.mobile.domain.usecase.LikedTracksUseCase
import com.ibramusic.mobile.domain.usecase.ManageQueueUseCase
import com.ibramusic.mobile.playback.audiofx.EqualizerManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NowPlayingViewModel @Inject constructor(
    private val playbackRepository: PlaybackRepository,
    private val likedTracksUseCase: LikedTracksUseCase,
    private val manageQueueUseCase: ManageQueueUseCase,
    private val getPlaylistsUseCase: GetPlaylistsUseCase,
    private val addTrackToPlaylistUseCase: AddTrackToPlaylistUseCase,
    private val createPlaylistUseCase: CreatePlaylistUseCase,
    private val lyricsService: LyricsService,
    val equalizerManager: EqualizerManager,
    private val downloadRepository: DownloadRepository
) : ViewModel() {

    val playbackState: StateFlow<PlaybackState> = playbackRepository.playbackState
    val playbackSpeed: StateFlow<Float> = playbackRepository.playbackSpeed
    val sleepTimerMinutesLeft: StateFlow<Int?> = playbackRepository.sleepTimerMinutesLeft

    val playlists: StateFlow<List<Playlist>> = getPlaylistsUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isLiked = MutableStateFlow(false)
    val isLiked: StateFlow<Boolean> = _isLiked.asStateFlow()

    private val _lyrics = MutableStateFlow<List<LyricLine>>(emptyList())
    val lyrics: StateFlow<List<LyricLine>> = _lyrics.asStateFlow()

    private val _isLoadingLyrics = MutableStateFlow(false)
    val isLoadingLyrics: StateFlow<Boolean> = _isLoadingLyrics.asStateFlow()

    private var currentLyricsTrackId: String? = null
    private var lyricsJob: Job? = null

    init {
        viewModelScope.launch {
            playbackState.collect { state ->
                val track = state.currentTrack
                if (track != null) {
                    _isLiked.value = likedTracksUseCase.isLiked(track.id)

                    if (track.id != currentLyricsTrackId) {
                        currentLyricsTrackId = track.id
                        loadLyrics(track)
                    }
                } else {
                    _isLiked.value = false
                    _lyrics.value = emptyList()
                    currentLyricsTrackId = null
                }
            }
        }
    }

    private fun loadLyrics(track: Track) {
        lyricsJob?.cancel()
        lyricsJob = viewModelScope.launch {
            _isLoadingLyrics.value = true
            _lyrics.value = emptyList()
            val lines = lyricsService.fetchLyrics(
                artist = track.artistName,
                title = track.title,
                durationSec = track.durationMs / 1000
            )
            _lyrics.value = lines
            _isLoadingLyrics.value = false
        }
    }

    fun togglePlayPause() {
        playbackRepository.togglePlayPause()
    }

    fun seekTo(positionMs: Long) {
        playbackRepository.seekTo(positionMs)
    }

    fun skipNext() {
        playbackRepository.skipToNext()
    }

    fun skipPrevious() {
        playbackRepository.skipToPrevious()
    }

    fun toggleShuffle() {
        playbackRepository.toggleShuffle()
    }

    fun cycleRepeatMode() {
        val nextMode = when (playbackState.value.repeatMode) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
        playbackRepository.setRepeatMode(nextMode)
    }

    fun setPlaybackSpeed(speed: Float) {
        playbackRepository.setPlaybackSpeed(speed)
    }

    fun setSleepTimer(minutes: Int) {
        playbackRepository.setSleepTimer(minutes)
    }

    fun toggleLike() {
        val current = playbackState.value.currentTrack ?: return
        viewModelScope.launch {
            val newState = likedTracksUseCase.toggleLike(current)
            _isLiked.value = newState
        }
    }

    fun playNext(track: Track) {
        viewModelScope.launch {
            manageQueueUseCase.playNext(track)
        }
    }

    fun addToQueue(track: Track) {
        viewModelScope.launch {
            manageQueueUseCase.addToQueue(track)
        }
    }

    fun addTrackToPlaylist(playlistId: String, track: Track) {
        viewModelScope.launch {
            addTrackToPlaylistUseCase(playlistId, track)
        }
    }

    fun createPlaylist(name: String, onCreated: ((String) -> Unit)? = null) {
        viewModelScope.launch {
            val id = createPlaylistUseCase(name)
            onCreated?.invoke(id)
        }
    }

    val activeDownloads: StateFlow<Map<String, Int>> = downloadRepository.activeDownloads

    fun downloadTrack(track: Track) {
        viewModelScope.launch {
            downloadRepository.downloadTrack(track)
        }
    }

    fun removeDownload(trackId: String) {
        viewModelScope.launch {
            downloadRepository.removeDownload(trackId)
        }
    }
}
