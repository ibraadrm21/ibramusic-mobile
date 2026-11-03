package com.ibramusic.mobile.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ibramusic.mobile.data.local.dao.TrackDao
import com.ibramusic.mobile.domain.model.Playlist
import com.ibramusic.mobile.domain.model.Track
import com.ibramusic.mobile.domain.repository.DownloadRepository
import com.ibramusic.mobile.domain.usecase.AddTrackToPlaylistUseCase
import com.ibramusic.mobile.domain.usecase.CreatePlaylistUseCase
import com.ibramusic.mobile.domain.usecase.GetHomeRecommendationsUseCase
import com.ibramusic.mobile.domain.usecase.GetPlaylistsUseCase
import com.ibramusic.mobile.domain.usecase.GetTrendingTracksUseCase
import com.ibramusic.mobile.domain.usecase.LikedTracksUseCase
import com.ibramusic.mobile.domain.usecase.ManageQueueUseCase
import com.ibramusic.mobile.domain.usecase.PlayTrackUseCase
import com.ibramusic.mobile.playback.resolver.UnifiedPlaybackResolver
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val isLoading: Boolean = true,
    val trending: List<Track> = emptyList(),
    val recommendations: List<Track> = emptyList(),
    val recentlyPlayed: List<Track> = emptyList()
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getHomeRecommendationsUseCase: GetHomeRecommendationsUseCase,
    private val getTrendingTracksUseCase: GetTrendingTracksUseCase,
    private val playTrackUseCase: PlayTrackUseCase,
    private val manageQueueUseCase: ManageQueueUseCase,
    private val likedTracksUseCase: LikedTracksUseCase,
    private val getPlaylistsUseCase: GetPlaylistsUseCase,
    private val addTrackToPlaylistUseCase: AddTrackToPlaylistUseCase,
    private val createPlaylistUseCase: CreatePlaylistUseCase,
    private val resolver: UnifiedPlaybackResolver,
    private val trackDao: TrackDao,
    private val downloadRepository: DownloadRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    val playlists: StateFlow<List<Playlist>> = getPlaylistsUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val recs = try {
                getHomeRecommendationsUseCase()
            } catch (e: Exception) {
                emptyList()
            }

            val trending = try {
                getTrendingTracksUseCase()
            } catch (e: Exception) {
                emptyList()
            }

            val history = try {
                trackDao.getListeningHistory().firstOrNull()?.map { it.toDomain() } ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }

            _uiState.value = HomeUiState(
                isLoading = false,
                recommendations = recs,
                trending = trending,
                recentlyPlayed = history.take(5)
            )

            // Pre-resolve top trending, recommended, and recent tracks in background for instant playback
            if (trending.isNotEmpty()) {
                resolver.preResolveList(trending, limit = 2)
            }
            if (recs.isNotEmpty()) {
                resolver.preResolveList(recs, limit = 2)
            }
            if (history.isNotEmpty()) {
                resolver.preResolveList(history.take(5), limit = 2)
            }
        }
    }

    fun playTrack(track: Track, contextQueue: List<Track>) {
        playTrackUseCase(track, contextQueue)
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
}
