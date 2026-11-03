package com.ibramusic.mobile.feature.album

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ibramusic.mobile.domain.model.Album
import com.ibramusic.mobile.domain.model.Playlist
import com.ibramusic.mobile.domain.model.Track
import com.ibramusic.mobile.domain.repository.DownloadRepository
import com.ibramusic.mobile.domain.usecase.AddAlbumToPlaylistUseCase
import com.ibramusic.mobile.domain.usecase.AddTrackToPlaylistUseCase
import com.ibramusic.mobile.domain.usecase.CreatePlaylistUseCase
import com.ibramusic.mobile.domain.usecase.GetAlbumTracksUseCase
import com.ibramusic.mobile.domain.usecase.GetPlaylistsUseCase
import com.ibramusic.mobile.domain.usecase.IsAlbumSavedUseCase
import com.ibramusic.mobile.domain.usecase.LikedTracksUseCase
import com.ibramusic.mobile.domain.usecase.ManageQueueUseCase
import com.ibramusic.mobile.domain.usecase.PlayTrackUseCase
import com.ibramusic.mobile.domain.usecase.SaveAlbumAsPlaylistUseCase
import com.ibramusic.mobile.domain.usecase.ToggleSaveAlbumUseCase
import com.ibramusic.mobile.playback.resolver.UnifiedPlaybackResolver
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AlbumUiState(
    val albumTitle: String = "",
    val artistName: String = "",
    val albumId: String = "",
    val artworkUrl: String? = null,
    val releaseYear: Int? = null,
    val isLoading: Boolean = true,
    val tracks: List<Track> = emptyList(),
    val isSaved: Boolean = false,
    val userMessage: String? = null
)

@HiltViewModel
class AlbumDetailViewModel @Inject constructor(
    private val getAlbumTracksUseCase: GetAlbumTracksUseCase,
    private val toggleSaveAlbumUseCase: ToggleSaveAlbumUseCase,
    private val isAlbumSavedUseCase: IsAlbumSavedUseCase,
    private val saveAlbumAsPlaylistUseCase: SaveAlbumAsPlaylistUseCase,
    private val addAlbumToPlaylistUseCase: AddAlbumToPlaylistUseCase,
    private val playTrackUseCase: PlayTrackUseCase,
    private val manageQueueUseCase: ManageQueueUseCase,
    private val likedTracksUseCase: LikedTracksUseCase,
    private val getPlaylistsUseCase: GetPlaylistsUseCase,
    private val addTrackToPlaylistUseCase: AddTrackToPlaylistUseCase,
    private val createPlaylistUseCase: CreatePlaylistUseCase,
    private val resolver: UnifiedPlaybackResolver,
    private val downloadRepository: DownloadRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AlbumUiState())
    val uiState: StateFlow<AlbumUiState> = _uiState.asStateFlow()

    val playlists: StateFlow<List<Playlist>> = getPlaylistsUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeDownloads: StateFlow<Map<String, Int>> = downloadRepository.activeDownloads

    fun loadAlbum(title: String, artistName: String, artworkUrl: String?, albumId: String?) {
        val safeTitle = title.trim()
        val safeArtist = artistName.trim()
        val safeId = albumId ?: "${safeTitle}_$safeArtist"

        _uiState.value = _uiState.value.copy(
            albumTitle = safeTitle,
            artistName = safeArtist,
            albumId = safeId,
            artworkUrl = artworkUrl,
            isLoading = true
        )

        viewModelScope.launch {
            isAlbumSavedUseCase(safeId, safeTitle).collect { saved ->
                _uiState.value = _uiState.value.copy(isSaved = saved)
            }
        }

        viewModelScope.launch {
            try {
                val tracks = getAlbumTracksUseCase(safeId, safeTitle)
                val effectiveCover = artworkUrl.takeIf { !it.isNullOrEmpty() }
                    ?: tracks.firstOrNull()?.artworkUrl

                _uiState.value = _uiState.value.copy(
                    tracks = tracks,
                    artworkUrl = effectiveCover,
                    isLoading = false
                )

                if (tracks.isNotEmpty()) {
                    resolver.preResolveList(tracks, limit = 2)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun toggleSaveAlbum() {
        val current = _uiState.value
        val album = Album(
            id = current.albumId,
            title = current.albumTitle,
            artistName = current.artistName,
            artworkUrl = current.artworkUrl
        )
        viewModelScope.launch {
            val isNowSaved = toggleSaveAlbumUseCase(album)
            _uiState.value = _uiState.value.copy(isSaved = isNowSaved)
        }
    }

    fun saveAsPlaylist(onSaved: (String) -> Unit) {
        val current = _uiState.value
        if (current.tracks.isEmpty()) return
        val album = Album(
            id = current.albumId,
            title = current.albumTitle,
            artistName = current.artistName,
            artworkUrl = current.artworkUrl
        )
        viewModelScope.launch {
            val newPlaylistId = saveAlbumAsPlaylistUseCase(album, current.tracks)
            _uiState.value = _uiState.value.copy(userMessage = "Álbum guardado como playlist")
            onSaved(newPlaylistId)
        }
    }

    fun addAlbumToPlaylist(playlistId: String, onAdded: () -> Unit) {
        val current = _uiState.value
        if (current.tracks.isEmpty()) return
        viewModelScope.launch {
            addAlbumToPlaylistUseCase(playlistId, current.tracks)
            _uiState.value = _uiState.value.copy(userMessage = "Canciones añadidas a la playlist")
            onAdded()
        }
    }

    fun downloadAll() {
        val tracks = _uiState.value.tracks
        viewModelScope.launch {
            tracks.forEach { downloadRepository.downloadTrack(it) }
        }
    }

    fun playTrack(track: Track, contextList: List<Track>) {
        playTrackUseCase(track, contextList)
    }

    fun playAll(shuffle: Boolean = false) {
        val tracks = _uiState.value.tracks
        if (tracks.isEmpty()) return
        val list = if (shuffle) tracks.shuffled() else tracks
        playTrackUseCase(list.first(), list)
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

    fun toggleLike(track: Track) {
        viewModelScope.launch {
            likedTracksUseCase.toggleLike(track)
        }
    }

    fun downloadTrack(track: Track) {
        viewModelScope.launch {
            downloadRepository.downloadTrack(track)
        }
    }

    fun addTrackToPlaylist(playlistId: String, track: Track) {
        viewModelScope.launch {
            addTrackToPlaylistUseCase(playlistId, track)
        }
    }

    fun createPlaylistAndAdd(name: String, track: Track) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val id = createPlaylistUseCase(name.trim(), null, track.artworkUrl)
            addTrackToPlaylistUseCase(id, track)
        }
    }

    fun clearUserMessage() {
        _uiState.value = _uiState.value.copy(userMessage = null)
    }
}

