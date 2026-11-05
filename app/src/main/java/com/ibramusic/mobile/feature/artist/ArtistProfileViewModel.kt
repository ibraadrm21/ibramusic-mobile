package com.ibramusic.mobile.feature.artist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ibramusic.mobile.domain.model.Album
import com.ibramusic.mobile.domain.model.Artist
import com.ibramusic.mobile.domain.model.Playlist
import com.ibramusic.mobile.domain.model.Track
import com.ibramusic.mobile.domain.repository.DownloadRepository
import com.ibramusic.mobile.domain.usecase.AddTrackToPlaylistUseCase
import com.ibramusic.mobile.domain.usecase.CreatePlaylistUseCase
import com.ibramusic.mobile.domain.usecase.GetArtistProfileUseCase
import com.ibramusic.mobile.domain.usecase.GetPlaylistsUseCase
import com.ibramusic.mobile.domain.usecase.IsArtistFollowedUseCase
import com.ibramusic.mobile.domain.usecase.LikedTracksUseCase
import com.ibramusic.mobile.domain.usecase.ManageQueueUseCase
import com.ibramusic.mobile.domain.usecase.PlayTrackUseCase
import com.ibramusic.mobile.domain.usecase.ToggleFollowArtistUseCase
import com.ibramusic.mobile.playback.resolver.UnifiedPlaybackResolver
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ArtistUiState(
    val artistName: String = "",
    val artistId: String = "",
    val artworkUrl: String? = null,
    val isLoading: Boolean = true,
    val topTracks: List<Track> = emptyList(),
    val albums: List<Album> = emptyList(),
    val isFollowed: Boolean = false
)

@HiltViewModel
class ArtistProfileViewModel @Inject constructor(
    private val getArtistProfileUseCase: GetArtistProfileUseCase,
    private val toggleFollowArtistUseCase: ToggleFollowArtistUseCase,
    private val isArtistFollowedUseCase: IsArtistFollowedUseCase,
    private val playTrackUseCase: PlayTrackUseCase,
    private val manageQueueUseCase: ManageQueueUseCase,
    private val likedTracksUseCase: LikedTracksUseCase,
    private val getPlaylistsUseCase: GetPlaylistsUseCase,
    private val addTrackToPlaylistUseCase: AddTrackToPlaylistUseCase,
    private val createPlaylistUseCase: CreatePlaylistUseCase,
    private val resolver: UnifiedPlaybackResolver,
    private val downloadRepository: DownloadRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ArtistUiState())
    val uiState: StateFlow<ArtistUiState> = _uiState.asStateFlow()

    val playlists: StateFlow<List<Playlist>> = getPlaylistsUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeDownloads: StateFlow<Map<String, Int>> = downloadRepository.activeDownloads

    fun loadArtist(name: String, id: String?, artworkUrl: String?) {
        val safeName = name.trim()
        val safeId = id ?: safeName
        _uiState.value = _uiState.value.copy(
            artistName = safeName,
            artistId = safeId,
            artworkUrl = artworkUrl,
            isLoading = true
        )

        viewModelScope.launch {
            isArtistFollowedUseCase(safeId, safeName).collect { followed ->
                _uiState.value = _uiState.value.copy(isFollowed = followed)
            }
        }

        viewModelScope.launch {
            try {
                val profile = getArtistProfileUseCase(safeId, safeName)
                val effectiveArtwork = artworkUrl.takeIf { !it.isNullOrEmpty() }
                    ?: profile.topTracks.firstOrNull()?.artists?.firstOrNull()?.artworkUrl
                    ?: profile.albums.firstOrNull()?.artworkUrl

                _uiState.value = _uiState.value.copy(
                    topTracks = profile.topTracks,
                    albums = profile.albums,
                    artworkUrl = effectiveArtwork,
                    isLoading = false
                )

                if (profile.topTracks.isNotEmpty()) {
                    resolver.preResolveList(profile.topTracks, limit = 2)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun toggleFollow() {
        val current = _uiState.value
        val artist = Artist(id = current.artistId, name = current.artistName, artworkUrl = current.artworkUrl)
        viewModelScope.launch {
            val isNowFollowed = toggleFollowArtistUseCase(artist)
            _uiState.value = _uiState.value.copy(isFollowed = isNowFollowed)
        }
    }

    fun playTrack(track: Track, contextList: List<Track>) {
        playTrackUseCase(track, contextList)
    }

    fun playAll(tracks: List<Track>, shuffle: Boolean = false) {
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

    fun removeDownload(trackId: String) {
        viewModelScope.launch {
            downloadRepository.removeDownload(trackId)
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
}

