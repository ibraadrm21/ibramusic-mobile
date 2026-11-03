package com.ibramusic.mobile.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ibramusic.mobile.core.network.InnerTubeSearchService
import com.ibramusic.mobile.domain.model.Album
import com.ibramusic.mobile.domain.model.Artist
import com.ibramusic.mobile.domain.model.Playlist
import com.ibramusic.mobile.domain.model.Track
import com.ibramusic.mobile.domain.repository.DownloadRepository
import com.ibramusic.mobile.domain.usecase.AddTrackToPlaylistUseCase
import com.ibramusic.mobile.domain.usecase.CreatePlaylistUseCase
import com.ibramusic.mobile.domain.usecase.GetPlaylistsUseCase
import com.ibramusic.mobile.domain.usecase.GetTrendingTracksUseCase
import com.ibramusic.mobile.domain.usecase.LikedTracksUseCase
import com.ibramusic.mobile.domain.usecase.ManageQueueUseCase
import com.ibramusic.mobile.domain.usecase.PlayTrackUseCase
import com.ibramusic.mobile.domain.usecase.SearchMusicUseCase
import com.ibramusic.mobile.playback.resolver.UnifiedPlaybackResolver
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class SearchCategory {
    ALL,
    TRACKS,
    ARTISTS,
    ALBUMS
}

data class SearchUiState(
    val query: String = "",
    val isSearching: Boolean = false,
    val selectedCategory: SearchCategory = SearchCategory.ALL,
    val suggestions: List<String> = emptyList(),
    val tracks: List<Track> = emptyList(),
    val artists: List<Artist> = emptyList(),
    val albums: List<Album> = emptyList(),
    val trendingTracks: List<Track> = emptyList()
)

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchMusicUseCase: SearchMusicUseCase,
    private val getTrendingTracksUseCase: GetTrendingTracksUseCase,
    private val playTrackUseCase: PlayTrackUseCase,
    private val manageQueueUseCase: ManageQueueUseCase,
    private val likedTracksUseCase: LikedTracksUseCase,
    private val getPlaylistsUseCase: GetPlaylistsUseCase,
    private val addTrackToPlaylistUseCase: AddTrackToPlaylistUseCase,
    private val createPlaylistUseCase: CreatePlaylistUseCase,
    private val resolver: UnifiedPlaybackResolver,
    private val innerTubeSearchService: InnerTubeSearchService,
    private val downloadRepository: DownloadRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    val playlists: StateFlow<List<Playlist>> = getPlaylistsUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeDownloads: StateFlow<Map<String, Int>> = downloadRepository.activeDownloads

    private var searchJob: Job? = null
    private var suggestionJob: Job? = null

    init {
        viewModelScope.launch {
            try {
                val trending = getTrendingTracksUseCase()
                _uiState.value = _uiState.value.copy(trendingTracks = trending)
                if (trending.isNotEmpty()) {
                    resolver.preResolveList(trending, limit = 2)
                }
            } catch (e: Exception) {
                // Handled
            }
        }
    }

    fun onQueryChange(newQuery: String) {
        _uiState.value = _uiState.value.copy(query = newQuery)
        suggestionJob?.cancel()
        searchJob?.cancel()

        if (newQuery.isBlank()) {
            _uiState.value = _uiState.value.copy(
                isSearching = false,
                suggestions = emptyList(),
                tracks = emptyList(),
                artists = emptyList(),
                albums = emptyList()
            )
            return
        }

        // Fast autocomplete suggestions from InnerTube WEB_REMIX
        suggestionJob = viewModelScope.launch {
            delay(120L)
            try {
                val suggestions = innerTubeSearchService.getSearchSuggestions(newQuery)
                _uiState.value = _uiState.value.copy(suggestions = suggestions)
            } catch (e: Exception) {
                // Ignore
            }
        }

        // Full search debounce
        searchJob = viewModelScope.launch {
            delay(350L) // Debounce
            executeSearch(newQuery)
        }
    }

    fun onSuggestionSelected(suggestion: String) {
        _uiState.value = _uiState.value.copy(query = suggestion, suggestions = emptyList())
        suggestionJob?.cancel()
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            executeSearch(suggestion)
        }
    }

    fun searchMood(moodName: String, curatedQuery: String) {
        _uiState.value = _uiState.value.copy(query = moodName, suggestions = emptyList(), isSearching = true)
        suggestionJob?.cancel()
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            executeSearch(curatedQuery)
        }
    }

    private suspend fun executeSearch(query: String) {
        _uiState.value = _uiState.value.copy(isSearching = true)
        try {
            val tracks = searchMusicUseCase.execute(query)
            val artists = searchMusicUseCase.searchArtists(query)
            val albums = searchMusicUseCase.searchAlbums(query)

            _uiState.value = _uiState.value.copy(
                isSearching = false,
                tracks = tracks,
                artists = artists,
                albums = albums
            )

            // Pre-resolve top search tracks in background so clicking them plays immediately
            if (tracks.isNotEmpty()) {
                resolver.preResolveList(tracks, limit = 2)
            }
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(isSearching = false)
        }
    }

    fun downloadTrack(track: Track) {
        viewModelScope.launch {
            downloadRepository.downloadTrack(track)
        }
    }

    fun selectCategory(category: SearchCategory) {
        _uiState.value = _uiState.value.copy(selectedCategory = category)
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

    fun createPlaylist(name: String, onCreated: ((String) -> Unit)? = null) {
        viewModelScope.launch {
            val id = createPlaylistUseCase(name)
            onCreated?.invoke(id)
        }
    }
}
