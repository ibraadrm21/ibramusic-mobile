package com.ibramusic.mobile.data.repository

import com.ibramusic.mobile.data.local.dao.AlbumDao
import com.ibramusic.mobile.data.local.entity.SavedAlbumEntity
import com.ibramusic.mobile.data.provider.DeezerProvider
import com.ibramusic.mobile.domain.model.Album
import com.ibramusic.mobile.domain.model.Track
import com.ibramusic.mobile.domain.repository.AlbumRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlbumRepositoryImpl @Inject constructor(
    private val albumDao: AlbumDao,
    private val deezerProvider: DeezerProvider
) : AlbumRepository {

    override fun getSavedAlbums(): Flow<List<Album>> {
        return albumDao.getSavedAlbums().map { list ->
            list.map { entity ->
                Album(
                    id = entity.id,
                    title = entity.title,
                    artistName = entity.artistName,
                    artworkUrl = entity.artworkUrl,
                    releaseYear = entity.releaseYear,
                    totalTracks = entity.totalTracks
                )
            }
        }
    }

    override fun isAlbumSaved(id: String, title: String): Flow<Boolean> {
        return albumDao.isAlbumSaved(id, title)
    }

    override suspend fun saveAlbum(album: Album) = withContext(Dispatchers.IO) {
        albumDao.saveAlbum(
            SavedAlbumEntity(
                id = album.id.ifEmpty { "${album.title}_${album.artistName}" },
                title = album.title,
                artistName = album.artistName,
                artworkUrl = album.artworkUrl,
                releaseYear = album.releaseYear,
                totalTracks = album.totalTracks
            )
        )
    }

    override suspend fun removeAlbum(id: String, title: String) = withContext(Dispatchers.IO) {
        albumDao.removeAlbum(id, title)
    }

    override suspend fun getAlbumTracks(albumId: String, albumTitle: String): List<Track> = withContext(Dispatchers.IO) {
        deezerProvider.getAlbumTracks(albumId, albumTitle)
    }
}

