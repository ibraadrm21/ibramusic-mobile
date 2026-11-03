package com.ibramusic.mobile.data.repository

import com.ibramusic.mobile.data.local.dao.ArtistDao
import com.ibramusic.mobile.data.local.entity.FollowedArtistEntity
import com.ibramusic.mobile.data.provider.DeezerProvider
import com.ibramusic.mobile.domain.model.Album
import com.ibramusic.mobile.domain.model.Artist
import com.ibramusic.mobile.domain.model.Track
import com.ibramusic.mobile.domain.repository.ArtistRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ArtistRepositoryImpl @Inject constructor(
    private val artistDao: ArtistDao,
    private val deezerProvider: DeezerProvider
) : ArtistRepository {

    override fun getFollowedArtists(): Flow<List<Artist>> {
        return artistDao.getFollowedArtists().map { list ->
            list.map { entity ->
                Artist(
                    id = entity.id,
                    name = entity.name,
                    artworkUrl = entity.artworkUrl
                )
            }
        }
    }

    override fun isArtistFollowed(id: String, name: String): Flow<Boolean> {
        return artistDao.isArtistFollowed(id, name)
    }

    override suspend fun followArtist(artist: Artist) = withContext(Dispatchers.IO) {
        artistDao.followArtist(
            FollowedArtistEntity(
                id = artist.id.ifEmpty { artist.name },
                name = artist.name,
                artworkUrl = artist.artworkUrl
            )
        )
    }

    override suspend fun unfollowArtist(id: String, name: String) = withContext(Dispatchers.IO) {
        artistDao.unfollowArtist(id, name)
    }

    override suspend fun getArtistTopTracks(artistId: String, artistName: String): List<Track> = withContext(Dispatchers.IO) {
        deezerProvider.getArtistTopTracks(artistId, artistName)
    }

    override suspend fun getArtistAlbums(artistId: String, artistName: String): List<Album> = withContext(Dispatchers.IO) {
        deezerProvider.getArtistAlbums(artistId, artistName)
    }
}

