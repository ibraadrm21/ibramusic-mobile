package com.ibramusic.mobile.domain.repository

import com.ibramusic.mobile.domain.model.Album
import com.ibramusic.mobile.domain.model.Track
import kotlinx.coroutines.flow.Flow

interface AlbumRepository {
    fun getSavedAlbums(): Flow<List<Album>>
    fun isAlbumSaved(id: String, title: String): Flow<Boolean>
    suspend fun saveAlbum(album: Album)
    suspend fun removeAlbum(id: String, title: String)
    suspend fun getAlbumTracks(albumId: String, albumTitle: String): List<Track>
}

