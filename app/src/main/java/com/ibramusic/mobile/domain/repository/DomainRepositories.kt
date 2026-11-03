package com.ibramusic.mobile.domain.repository

import com.ibramusic.mobile.domain.model.Album
import com.ibramusic.mobile.domain.model.Artist
import com.ibramusic.mobile.domain.model.Track
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface TrackRepository {
    suspend fun searchTracks(query: String): List<Track>
    suspend fun searchAlbums(query: String): List<Album>
    suspend fun searchArtists(query: String): List<Artist>
    suspend fun getHomeRecommendations(): List<Track>
    suspend fun getTrending(): List<Track>
    suspend fun getAlbumTracks(albumId: String): List<Track>
    suspend fun getArtistTracks(artistId: String): List<Track>
    fun getLikedTracks(): Flow<List<Track>>
    suspend fun toggleLike(track: Track): Boolean
    suspend fun isLiked(trackId: String): Boolean
}

interface AuthRepository {
    val currentUserId: Flow<String?>
    val currentUserEmail: Flow<String?>
    val currentUserAvatar: Flow<String?>
    val currentUserName: Flow<String?>
    val isGuestMode: Flow<Boolean>

    suspend fun continueAsGuest()
    suspend fun signIn(email: String, password: String): Result<Unit>
    suspend fun signUp(email: String, password: String): Result<Unit>
    suspend fun signOut()
    suspend fun syncWithCloud(): Result<Unit>
    suspend fun getValidAccessToken(): String?
    suspend fun refreshAccessToken(): Result<String>
    suspend fun linkGuestAccountToPermanent(email: String, password: String): Result<Unit>
    suspend fun refreshUserProfile(): Result<Unit>
}

interface DownloadRepository {
    val downloadedTracks: Flow<List<Track>>
    val activeDownloads: StateFlow<Map<String, Int>>

    suspend fun downloadTrack(track: Track)
    suspend fun removeDownload(trackId: String)
    suspend fun isDownloaded(trackId: String): Boolean
    suspend fun getDownloadProgress(trackId: String): Flow<Int>
}

interface SettingsRepository {
    val audioQuality: Flow<String>
    val crossfadeEnabled: Flow<Boolean>
    val crossfadeDurationSeconds: Flow<Int>
    val theme: Flow<String> // "dark", "noir", "bright"

    suspend fun setAudioQuality(quality: String)
    suspend fun setCrossfadeEnabled(enabled: Boolean)
    suspend fun setCrossfadeDuration(seconds: Int)
    suspend fun setTheme(theme: String)
}

