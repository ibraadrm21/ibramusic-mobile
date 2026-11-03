package com.ibramusic.mobile.domain.usecase

import com.ibramusic.mobile.domain.model.Album
import com.ibramusic.mobile.domain.model.Artist
import com.ibramusic.mobile.domain.model.Track
import com.ibramusic.mobile.domain.repository.ArtistRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class GetFollowedArtistsUseCase @Inject constructor(
    private val artistRepository: ArtistRepository
) {
    operator fun invoke(): Flow<List<Artist>> = artistRepository.getFollowedArtists()
}

class ToggleFollowArtistUseCase @Inject constructor(
    private val artistRepository: ArtistRepository
) {
    suspend operator fun invoke(artist: Artist): Boolean {
        val isCurrentlyFollowed = artistRepository.isArtistFollowed(artist.id, artist.name).first()
        if (isCurrentlyFollowed) {
            artistRepository.unfollowArtist(artist.id, artist.name)
            return false
        } else {
            artistRepository.followArtist(artist)
            return true
        }
    }
}

class IsArtistFollowedUseCase @Inject constructor(
    private val artistRepository: ArtistRepository
) {
    operator fun invoke(id: String, name: String): Flow<Boolean> = artistRepository.isArtistFollowed(id, name)
}

data class ArtistProfileData(
    val topTracks: List<Track>,
    val albums: List<Album>
)

class GetArtistProfileUseCase @Inject constructor(
    private val artistRepository: ArtistRepository
) {
    suspend operator fun invoke(artistId: String, artistName: String): ArtistProfileData {
        val tracks = artistRepository.getArtistTopTracks(artistId, artistName)
        val albums = artistRepository.getArtistAlbums(artistId, artistName)
        return ArtistProfileData(tracks, albums)
    }
}

