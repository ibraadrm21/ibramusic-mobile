package com.ibramusic.mobile.domain.repository

import com.ibramusic.mobile.domain.model.Album
import com.ibramusic.mobile.domain.model.Artist
import com.ibramusic.mobile.domain.model.Track
import kotlinx.coroutines.flow.Flow

interface ArtistRepository {
    fun getFollowedArtists(): Flow<List<Artist>>
    fun isArtistFollowed(id: String, name: String): Flow<Boolean>
    suspend fun followArtist(artist: Artist)
    suspend fun unfollowArtist(id: String, name: String)
    suspend fun getArtistTopTracks(artistId: String, artistName: String): List<Track>
    suspend fun getArtistAlbums(artistId: String, artistName: String): List<Album>
}

