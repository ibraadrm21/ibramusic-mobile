package com.ibramusic.mobile.domain.repository

import com.ibramusic.mobile.domain.model.Playlist
import com.ibramusic.mobile.domain.model.Track
import kotlinx.coroutines.flow.Flow

interface PlaylistRepository {
    fun getPlaylists(): Flow<List<Playlist>>
    fun getPlaylistTracks(playlistId: String): Flow<List<Track>>
    suspend fun createPlaylist(name: String, description: String? = null, coverUrl: String? = null): String
    suspend fun updatePlaylist(playlistId: String, name: String, coverUrl: String?)
    suspend fun deletePlaylist(playlistId: String)
    suspend fun addTrackToPlaylist(playlistId: String, track: Track)
    suspend fun removeTrackFromPlaylist(playlistId: String, trackId: String)
}

