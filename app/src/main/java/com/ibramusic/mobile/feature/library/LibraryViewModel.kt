package com.ibramusic.mobile.feature.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ibramusic.mobile.core.common.OfflineModeManager
import com.ibramusic.mobile.data.provider.LocalAudioProvider
import com.ibramusic.mobile.domain.model.Album
import com.ibramusic.mobile.domain.model.Artist
import com.ibramusic.mobile.domain.model.Playlist
import com.ibramusic.mobile.domain.model.Track
import com.ibramusic.mobile.domain.repository.AuthRepository
import com.ibramusic.mobile.domain.repository.DownloadRepository
import com.ibramusic.mobile.domain.usecase.AddTrackToPlaylistUseCase
import com.ibramusic.mobile.domain.usecase.CreatePlaylistUseCase
import com.ibramusic.mobile.domain.usecase.GetFollowedArtistsUseCase
import com.ibramusic.mobile.domain.usecase.GetPlaylistsUseCase
import com.ibramusic.mobile.domain.usecase.GetSavedAlbumsUseCase
import com.ibramusic.mobile.domain.usecase.LikedTracksUseCase
import com.ibramusic.mobile.domain.usecase.ManageQueueUseCase
import com.ibramusic.mobile.domain.usecase.PlayTrackUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class LibraryTab {
    LIKED,
    DOWNLOADS,
    OFFLINE_PLAYLISTS
}

enum class LibraryNormalTab {
    PLAYLISTS,
    ARTISTS,
    ALBUMS
}

enum class LibraryOfflineTab {
    DOWNLOADS,
    OFFLINE_PLAYLISTS
}

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val likedTracksUseCase: LikedTracksUseCase,
    private val downloadRepository: DownloadRepository,
    private val localAudioProvider: LocalAudioProvider,
    private val playTrackUseCase: PlayTrackUseCase,
    private val manageQueueUseCase: ManageQueueUseCase,
    private val getPlaylistsUseCase: GetPlaylistsUseCase,
    private val addTrackToPlaylistUseCase: AddTrackToPlaylistUseCase,
    private val createPlaylistUseCase: CreatePlaylistUseCase,
    private val authRepository: AuthRepository,
    private val getFollowedArtistsUseCase: GetFollowedArtistsUseCase,
    private val getSavedAlbumsUseCase: GetSavedAlbumsUseCase,
    private val offlineModeManager: OfflineModeManager
) : ViewModel() {

    val isOfflineMode: StateFlow<Boolean> = offlineModeManager.isOfflineMode

    val likedTracks: StateFlow<List<Track>> = likedTracksUseCase.getLikedTracks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val downloadedTracks: StateFlow<List<Track>> = downloadRepository.downloadedTracks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeDownloads: StateFlow<Map<String, Int>> = downloadRepository.activeDownloads

    val playlists: StateFlow<List<Playlist>> = getPlaylistsUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val followedArtists: StateFlow<List<Artist>> = getFollowedArtistsUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val savedAlbums: StateFlow<List<Album>> = getSavedAlbumsUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val offlinePlaylists: StateFlow<List<Playlist>> = combine(
        playlists,
        downloadedTracks
    ) { playlistList, downloaded ->
        val downloadedIds = downloaded.map { it.id }.toSet()
        playlistList.filter { p ->
            p.downloadedTrackCount > 0 || (p.tracks.isNotEmpty() && p.tracks.any { downloadedIds.contains(it.id) })
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedNormalTab = MutableStateFlow(LibraryNormalTab.PLAYLISTS)
    val selectedNormalTab: StateFlow<LibraryNormalTab> = _selectedNormalTab.asStateFlow()

    private val _selectedOfflineTab = MutableStateFlow(LibraryOfflineTab.DOWNLOADS)
    val selectedOfflineTab: StateFlow<LibraryOfflineTab> = _selectedOfflineTab.asStateFlow()

    // Backward-compat selectedTab
    private val _selectedTab = MutableStateFlow(LibraryTab.LIKED)
    val selectedTab: StateFlow<LibraryTab> = _selectedTab.asStateFlow()

    init {
        viewModelScope.launch {
            authRepository.syncWithCloud()
        }
    }

    fun selectNormalTab(tab: LibraryNormalTab) {
        _selectedNormalTab.value = tab
    }

    fun selectOfflineTab(tab: LibraryOfflineTab) {
        _selectedOfflineTab.value = tab
    }

    fun selectTab(tab: LibraryTab) {
        _selectedTab.value = tab
    }

    fun playTrack(track: Track, contextList: List<Track>) {
        playTrackUseCase(track, contextList)
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

    fun addTrackToPlaylist(playlistId: String, track: Track) {
        viewModelScope.launch {
            addTrackToPlaylistUseCase(playlistId, track)
        }
    }

    fun playAll(tracks: List<Track>, shuffle: Boolean = false) {
        if (tracks.isEmpty()) return
        val list = if (shuffle) tracks.shuffled() else tracks
        playTrackUseCase(list.first(), list)
    }

    fun createPlaylist(name: String, onCreated: ((String) -> Unit)? = null) {
        viewModelScope.launch {
            val id = createPlaylistUseCase(name)
            onCreated?.invoke(id)
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
}
