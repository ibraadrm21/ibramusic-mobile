package com.ibramusic.mobile.feature.playlist

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ibramusic.mobile.domain.model.Playlist
import com.ibramusic.mobile.domain.model.Track
import com.ibramusic.mobile.domain.repository.DownloadRepository
import com.ibramusic.mobile.domain.usecase.AddTrackToPlaylistUseCase
import com.ibramusic.mobile.domain.usecase.CreatePlaylistUseCase
import com.ibramusic.mobile.domain.usecase.DeletePlaylistUseCase
import com.ibramusic.mobile.domain.usecase.UpdatePlaylistUseCase
import com.ibramusic.mobile.domain.usecase.GetPlaylistTracksUseCase
import com.ibramusic.mobile.domain.usecase.GetPlaylistsUseCase
import com.ibramusic.mobile.domain.usecase.LikedTracksUseCase
import com.ibramusic.mobile.domain.usecase.ManageQueueUseCase
import com.ibramusic.mobile.domain.usecase.PlayTrackUseCase
import com.ibramusic.mobile.domain.usecase.RemoveTrackFromPlaylistUseCase
import com.ibramusic.mobile.domain.usecase.ToggleShuffleUseCase
import com.ibramusic.mobile.playback.resolver.UnifiedPlaybackResolver
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class PlaylistViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    getPlaylistsUseCase: GetPlaylistsUseCase,
    private val getPlaylistTracksUseCase: GetPlaylistTracksUseCase,
    private val createPlaylistUseCase: CreatePlaylistUseCase,
    private val updatePlaylistUseCase: UpdatePlaylistUseCase,
    private val deletePlaylistUseCase: DeletePlaylistUseCase,
    private val addTrackToPlaylistUseCase: AddTrackToPlaylistUseCase,
    private val removeTrackFromPlaylistUseCase: RemoveTrackFromPlaylistUseCase,
    private val playTrackUseCase: PlayTrackUseCase,
    private val manageQueueUseCase: ManageQueueUseCase,
    private val likedTracksUseCase: LikedTracksUseCase,
    private val toggleShuffleUseCase: ToggleShuffleUseCase,
    private val resolver: UnifiedPlaybackResolver,
    private val downloadRepository: DownloadRepository
) : ViewModel() {

    val playlists: StateFlow<List<Playlist>> = getPlaylistsUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val activeDownloads: StateFlow<Map<String, Int>> = downloadRepository.activeDownloads

    private val initialPlaylistId: String? = savedStateHandle.get<String>("playlistId")
    private val _selectedPlaylistId = MutableStateFlow<String?>(initialPlaylistId)
    val selectedPlaylistId: StateFlow<String?> = _selectedPlaylistId.asStateFlow()

    val currentPlaylistTracks: StateFlow<List<Track>> = _selectedPlaylistId
        .flatMapLatest { id ->
            if (id.isNullOrEmpty()) {
                flowOf(emptyList())
            } else {
                getPlaylistTracksUseCase(id)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        viewModelScope.launch {
            currentPlaylistTracks.collect { tracks ->
                if (tracks.isNotEmpty()) {
                    delay(500) // Don't compete with screen transition animation
                    resolver.preResolveList(tracks, limit = 2)
                }
            }
        }
    }

    fun selectPlaylist(playlistId: String) {
        _selectedPlaylistId.value = playlistId
    }

    fun createPlaylist(name: String, description: String? = null, coverUrl: String? = null, onCreated: ((String) -> Unit)? = null) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val id = createPlaylistUseCase(name.trim(), description?.trim(), coverUrl)
            onCreated?.invoke(id)
        }
    }

    fun updatePlaylist(playlistId: String, name: String, coverUrl: String?) {
        if (name.isBlank()) return
        viewModelScope.launch {
            updatePlaylistUseCase(playlistId, name.trim(), coverUrl?.takeIf { it.isNotBlank() }?.trim())
        }
    }

    fun importSpotifyPlaylist(url: String, onComplete: ((Boolean) -> Unit)? = null) {
        if (url.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val encodedUrl = URLEncoder.encode(url.trim(), "UTF-8")
                val client = OkHttpClient()
                val request = Request.Builder()
                    .url("https://open.spotify.com/oembed?url=$encodedUrl")
                    .build()
                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: ""
                    val json = JSONObject(body)
                    val title = json.optString("title", "Spotify Playlist")
                    val thumbnail = json.optString("thumbnail_url").takeIf { it.isNotEmpty() }
                    createPlaylist(name = title, description = "Importada desde Spotify", coverUrl = thumbnail)
                    withContext(Dispatchers.Main) {
                        onComplete?.invoke(true)
                    }
                } else {
                    createPlaylist(name = "Spotify Playlist", description = "Importada desde $url")
                    withContext(Dispatchers.Main) {
                        onComplete?.invoke(true)
                    }
                }
            } catch (e: Exception) {
                createPlaylist(name = "Spotify Playlist", description = "Importada desde $url")
                withContext(Dispatchers.Main) {
                    onComplete?.invoke(true)
                }
            }
        }
    }

    fun deletePlaylist(playlistId: String) {
        viewModelScope.launch {
            deletePlaylistUseCase(playlistId)
            if (_selectedPlaylistId.value == playlistId) {
                _selectedPlaylistId.value = null
            }
        }
    }

    fun addTrackToPlaylist(playlistId: String, track: Track) {
        viewModelScope.launch {
            addTrackToPlaylistUseCase(playlistId, track)
        }
    }

    fun removeTrackFromPlaylist(playlistId: String, trackId: String) {
        viewModelScope.launch {
            removeTrackFromPlaylistUseCase(playlistId, trackId)
        }
    }

    fun downloadTrack(track: Track) {
        viewModelScope.launch {
            downloadRepository.downloadTrack(track)
        }
    }

    fun deleteDownload(trackId: String) {
        viewModelScope.launch {
            downloadRepository.removeDownload(trackId)
        }
    }

    fun downloadPlaylist(tracks: List<Track>) {
        viewModelScope.launch {
            tracks.forEach { track ->
                downloadRepository.downloadTrack(track)
            }
        }
    }

    fun playTrack(track: Track, tracks: List<Track>) {
        playTrackUseCase(track, tracks)
    }

    fun playAll(tracks: List<Track>, shuffle: Boolean = false) {
        if (tracks.isEmpty()) return
        val listToPlay = if (shuffle) tracks.shuffled() else tracks
        playTrackUseCase(listToPlay.first(), listToPlay)
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
}

